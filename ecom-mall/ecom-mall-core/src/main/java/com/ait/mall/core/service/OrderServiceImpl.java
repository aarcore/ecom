package com.ait.mall.core.service;

import com.ait.common.api.PageResult;
import com.ait.mall.core.dto.CheckoutPreview;
import com.ait.mall.core.dto.OrderCreateRequest;
import com.ait.mall.core.dto.PayLaunch;
import com.ait.pay.service.PayChannelService;
import com.ait.pay.service.PaymentListener;
import com.ait.mall.core.entity.Order;
import com.ait.mall.core.entity.OrderItem;
import com.ait.mall.core.entity.PayRecord;
import com.ait.mall.core.entity.Product;
import com.ait.mall.core.entity.ProductSku;
import com.ait.mall.core.mapper.MemberMapper;
import com.ait.mall.core.mapper.OrderItemMapper;
import com.ait.mall.core.mapper.OrderMapper;
import com.ait.mall.core.mapper.PayRecordMapper;
import com.ait.mall.core.mapper.ProductMapper;
import com.ait.mall.core.mapper.ProductSkuMapper;
import com.ait.mall.core.service.support.CouponCalculator;
import com.ait.mall.core.service.support.OrderAssembler;
import com.ait.mall.core.vo.OrderVo;
import com.ait.mall.common.dto.OrderItemRequest;
import com.ait.mall.common.enums.OrderStatus;
import com.ait.mall.common.enums.PayType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ait.common.api.ResultCode;
import com.ait.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Order orchestration: pricing, stock locking, state transitions.
 *
 * <p>Stock is decremented with conditional UPDATE statements
 * ({@code where stock >= quantity}) rather than read-then-write so two
 * concurrent checkouts can never oversell the last unit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** Unpaid orders are cancelled after this many minutes. */
    private static final int PAY_TIMEOUT_MINUTES = 45;
    /** Free shipping threshold. */
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("99");
    private static final BigDecimal FLAT_FREIGHT = new BigDecimal("8");
    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PayRecordMapper payRecordMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;
    private final MemberMapper memberMapper;
    private final CartService cartService;
    private final AddressService addressService;
    private final CouponService couponService;
    private final CouponCalculator couponCalculator;
    private final OrderAssembler assembler;
    private final com.ait.pay.service.PayGateway payGateway;
    private final com.ait.pay.service.PayChannelService payChannelService;
    private final com.ait.pay.service.PayRecordService payRecordService;
    private final PaymentListener paymentListener;

    // ------------------------------------------------------------------
    //  Checkout preview
    // ------------------------------------------------------------------
    @Override
    public CheckoutPreview preview(Long memberId, List<OrderItemRequest> items,
                                   Long couponId, boolean couponOptOut, Long addressId, String source) {
        List<OrderItemRequest> lines = resolveLines(memberId, items, source);
        CheckoutPreview preview = new CheckoutPreview();
        preview.setItems(cartService.buildItemViews(lines));
        preview.setAddress(addressService.resolveForCheckout(memberId, addressId));

        BigDecimal total = preview.getItems().stream()
                .map(com.ait.mall.core.vo.CartItemVo::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        preview.setTotalAmount(total);
        preview.setFreightAmount(freightFor(total));

        Long usableCouponId = couponId;
        if (usableCouponId == null && !couponOptOut) {
            usableCouponId = couponCalculator.bestCoupon(memberId, total);
        }
        BigDecimal discount = couponCalculator.discountFor(memberId, usableCouponId, total);
        preview.setSelectedCouponId(discount.signum() > 0 ? usableCouponId : null);
        preview.setDiscountAmount(discount);
        preview.setPayAmount(applyFreight(total, discount));
        preview.setAvailableCoupons(couponService.listForMember(memberId, total));
        return preview;
    }

    // ------------------------------------------------------------------
    //  Create
    // ------------------------------------------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo create(Long memberId, String platform, OrderCreateRequest request) {
        List<OrderItemRequest> lines = resolveLines(memberId, request.getItems(), request.getSource());
        if (lines.isEmpty()) {
            throw new BizException(ResultCode.CART_EMPTY);
        }

        // 1. load and validate every SKU up-front
        List<PricedLine> priced = new ArrayList<>(lines.size());
        for (OrderItemRequest line : lines) {
            ProductSku sku = skuMapper.selectById(line.getSkuId());
            if (sku == null || Integer.valueOf(1).equals(sku.getDeleted())) {
                throw new BizException(ResultCode.SKU_NOT_FOUND);
            }
            Product product = productMapper.selectById(sku.getProductId());
            if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
                throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
            }
            priced.add(new PricedLine(product, sku, line.getQuantity()));
        }

        // 2. lock stock, one conditional update per SKU
        for (PricedLine line : priced) {
            int affected = skuMapper.deductStock(line.sku.getId(), line.quantity);
            if (affected == 0) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH,
                        "商品「" + line.product.getName() + "」库存不足");
            }
            productMapper.deductStock(line.product.getId(), line.quantity);
        }

        // 3. money
        BigDecimal total = priced.stream()
                .map(PricedLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal freight = freightFor(total);

        // When the client does not name a coupon, resolve the best one for this
        // basket so the charged amount always matches what the preview showed.
        Long couponId = request.getCouponId();
        if (couponId == null && !request.isCouponOptOut()) {
            couponId = couponCalculator.bestCoupon(memberId, total);
        }
        BigDecimal discount = couponCalculator.discountFor(memberId, couponId, total);
        if (discount.signum() <= 0) {
            couponId = null;
        }
        BigDecimal payAmount = applyFreight(total, discount);

        // 4. shipping snapshot
        var address = addressService.resolveForCheckout(memberId, request.getAddressId());
        if (address == null) {
            throw new BizException(ResultCode.ADDRESS_NOT_FOUND, "请先添加收货地址");
        }

        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setMemberId(memberId);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setTotalAmount(total);
        order.setFreightAmount(freight);
        order.setDiscountAmount(discount);
        order.setPayAmount(payAmount);
        order.setReceiver(address.getReceiver());
        order.setReceiverMobile(address.getMobile());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverDetail(address.getDetail());
        order.setRemark(request.getRemark());
        order.setCouponId(couponId);
        order.setPlatform(platform == null ? "web" : platform);
        order.setAdminRemark(null);
        orderMapper.insert(order);

        var member = memberMapper.selectById(memberId);
        if (member != null) {
            Order patch = new Order();
            patch.setId(order.getId());
            patch.setMemberNickname(member.getNickname());
            orderMapper.updateById(patch);
            order.setMemberNickname(member.getNickname());
        }

        // 5. freeze coupon so it cannot be used twice
        if (order.getCouponId() != null) {
            couponService.lockForOrder(order.getCouponId(), memberId, order.getOrderNo());
        }

        // 6. snapshot lines
        for (PricedLine line : priced) {
            OrderItem item = new OrderItem();
            item.setOrderNo(order.getOrderNo());
            item.setProductId(line.product.getId());
            item.setSkuId(line.sku.getId());
            item.setProductName(line.product.getName());
            item.setProductCover(line.sku.getImage() != null ? line.sku.getImage() : line.product.getCover());
            item.setSpecValues(line.sku.getSpecValues());
            item.setPrice(line.sku.getPrice());
            item.setQuantity(line.quantity);
            item.setTotalAmount(line.lineAmount());
            item.setCreatedAt(LocalDateTime.now());
            orderItemMapper.insert(item);
            line.product.setSales(line.product.getSales());
        }

        // 7. clear purchased cart lines
        if ("cart".equalsIgnoreCase(request.getSource())) {
            cartService.removeSkuIds(memberId, lines.stream()
                    .map(OrderItemRequest::getSkuId).collect(Collectors.toList()));
        }

        log.info("order {} created for member {} amount={}", order.getOrderNo(), memberId, payAmount);
        return assembler.toVo(order);
    }

    // ------------------------------------------------------------------
    //  Payment
    // ------------------------------------------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo pay(Long memberId, String orderNo, String payType) {
        Order order = requireOrder(orderNo);
        if (!order.getMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode()).equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }

        PayType type = PayType.of(payType);
        if (type == PayType.BALANCE) {
            int affected = memberMapper.deductBalance(memberId, order.getPayAmount());
            if (affected == 0) {
                throw new BizException(ResultCode.PAY_FAILED, "账户余额不足");
            }
        }

        // The demo channel still records the same audit trail as a real one,
        // including a pending row and a settlement, so the reconciliation view
        // behaves identically in both environments.
        String tradeNo = type.getCode().toUpperCase()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        payRecordService.recordSettled(type.getCode(),
                com.ait.pay.core.PayRequest.cny(orderNo, orderNo, order.getPayAmount()),
                tradeNo, memberId);
        return settle(order, type.getCode(), tradeNo, memberId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayLaunch launchPayment(Long memberId, String orderNo, String channelCode, String clientIp) {
        Order order = requireOrder(orderNo);
        if (!order.getMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode()).equals(order.getStatus())) {
            // Already paid or already closed: report the truth rather than
            // opening a second charge the customer would have to dispute.
            if (Integer.valueOf(OrderStatus.PENDING_SHIPMENT.getCode()).equals(order.getStatus())) {
                throw new com.ait.pay.core.PayException(
                        com.ait.pay.core.PaymentResultCode.ALREADY_PAID);
            }
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }

        var request = new com.ait.pay.core.PayRequest(
                orderNo,
                "山脊单车 · 订单 " + orderNo,
                order.getPayAmount(),
                "CNY",
                "山地车与骑行装备",
                clientIp,
                paymentListener.returnUrlFor(channelCode, null),
                paymentListener.notifyUrlFor(channelCode));

        // resolve first so a disabled or unconfigured channel fails before we
        // write anything to the payment log
        var channel = payChannelService.resolveIncludingDisabled(channelCode)
                .orElseThrow(() -> com.ait.pay.core.PayException.notConfigured(channelCode));
        if (!channel.enabled()) {
            throw com.ait.pay.core.PayException.channelDisabled(channelCode);
        }

        com.ait.pay.core.PayResult result = payGateway.create(channelCode, request);
        payRecordService.recordAttempt(channelCode, request, result.tradeNo(), memberId);

        PayLaunch launch = new PayLaunch();
        launch.setOrderNo(orderNo);
        launch.setChannelCode(result.channelCode());
        launch.setChannelName(payChannelService.listAll().stream()
                .filter(c -> c.code().equals(result.channelCode()))
                .map(PayChannelService.PayChannelView::name)
                .findFirst()
                .orElse(result.channelCode()));
        launch.setMode(result.mode().name());
        launch.setUrl(result.url());
        launch.setQrContent(result.qrContent());
        launch.setSdkParams(result.sdkParams());
        launch.setTradeNo(result.tradeNo());
        launch.setAmount(order.getPayAmount());
        launch.setCurrency("CNY");
        launch.setMessage(result.message());

        // The demo channel settles inline, so mirror that here to keep the
        // client code identical across environments: it checks `paid` and if
        // true renders the success state instead of a redirect/QR step.
        if (result.paid()) {
            settle(order, channelCode, result.tradeNo(), memberId);
            launch.setPaid(true);
            launch.setOrderStatus(OrderStatus.PENDING_SHIPMENT.getCode());
        } else {
            launch.setOrderStatus(order.getStatus());
        }
        return launch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String settleFromChannel(com.ait.pay.core.PayNotification notification) {
        if (!notification.paid()) {
            paymentListener.onUnpaid(notification);
            return null;
        }
        String orderNo = notification.orderNo();
        if (orderNo == null || orderNo.isEmpty()) {
            log.warn("payment callback without an order reference channel={}",
                    notification.channelCode());
            return null;
        }

        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo).last("limit 1"));
        if (order == null) {
            log.warn("payment callback for unknown order {} channel={}",
                    orderNo, notification.channelCode());
            return null;
        }

        // Amount tampering: a channel that charges a different amount than the
        // order is either a bug on our side or an attack. Refuse either way and
        // leave the order unpaid for a human to look at.
        if (!notification.matchesAmount(order.getPayAmount())) {
            log.error("payment amount mismatch order={} expected={} got={} channel={}",
                    orderNo, order.getPayAmount(), notification.amount(),
                    notification.channelCode());
            throw new com.ait.pay.core.PayException(
                    com.ait.pay.core.PaymentResultCode.AMOUNT_MISMATCH);
        }

        if (!Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode()).equals(order.getStatus())) {
            // Replay or a payment completed after the order expired. Either way
            // the money moved, so say so loudly and let a human reconcile.
            log.warn("payment callback for order {} in status {} — ignored",
                    orderNo, order.getStatus());
            return orderNo;
        }

        // Both guards: the payment record's conditional update, then the order's.
        // Whichever thread wins here is the one that grants the coupon and points.
        if (!payRecordService.markPaid(notification)) {
            return orderNo;
        }
        settle(order, notification.channelCode(), notification.tradeNo(), order.getMemberId());
        return orderNo;
    }

    /**
     * The single place an order becomes paid.
     *
     * <p>Kept separate so the synchronous channels and the callback path cannot
     * drift: both end up here, so both grant the coupon, accumulate spend and
     * write the same audit rows. The {@code status = PENDING_PAYMENT} guard in
     * the UPDATE is what makes a duplicate call harmless.
     */
    private OrderVo settle(Order order, String channelCode, String tradeNo, Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        Order patch = new Order();
        patch.setId(order.getId());
        patch.setStatus(OrderStatus.PENDING_SHIPMENT.getCode());
        patch.setPayType(channelCode);
        patch.setPaidAt(now);
        int updated = orderMapper.update(patch, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.getCode()));
        if (updated == 0) {
            // another path settled it first
            order.setStatus(OrderStatus.PENDING_SHIPMENT.getCode());
            return assembler.toVo(order);
        }

        memberMapper.accumulateSpend(memberId, order.getPayAmount());

        if (order.getCouponId() != null) {
            couponService.markUsed(order.getCouponId(), memberId, order.getOrderNo());
        }

        order.setStatus(OrderStatus.PENDING_SHIPMENT.getCode());
        order.setPayType(channelCode);
        order.setPaidAt(now);
        log.info("order {} settled via {} tradeNo={}", order.getOrderNo(), channelCode, tradeNo);
        return assembler.toVo(order);
    }

    // ------------------------------------------------------------------
    //  State transitions
    // ------------------------------------------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo cancel(Long memberId, String orderNo, String reason) {
        Order order = requireOrder(orderNo);
        if (memberId != null && !order.getMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!Integer.valueOf(OrderStatus.PENDING_PAYMENT.getCode()).equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "只有待付款订单可以取消");
        }
        restoreStock(order);

        Order patch = new Order();
        patch.setId(order.getId());
        patch.setStatus(OrderStatus.CANCELLED.getCode());
        patch.setClosedAt(LocalDateTime.now());
        patch.setAdminRemark(reason);
        orderMapper.updateById(patch);

        if (order.getCouponId() != null) {
            couponService.release(order.getCouponId(), order.getMemberId());
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setClosedAt(patch.getClosedAt());
        return assembler.toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo confirmReceipt(Long memberId, String orderNo) {
        Order order = requireOrder(orderNo);
        if (!order.getMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!Integer.valueOf(OrderStatus.SHIPPED.getCode()).equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单还未发货");
        }
        LocalDateTime now = LocalDateTime.now();
        Order patch = new Order();
        patch.setId(order.getId());
        patch.setStatus(OrderStatus.COMPLETED.getCode());
        patch.setFinishedAt(now);
        orderMapper.updateById(patch);
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setFinishedAt(now);
        return assembler.toVo(order);
    }

    @Override
    public OrderVo ship(String orderNo, String expressCompany, String expressNo) {
        Order order = requireOrder(orderNo);
        if (!Integer.valueOf(OrderStatus.PENDING_SHIPMENT.getCode()).equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "只有待发货订单可以发货");
        }
        LocalDateTime now = LocalDateTime.now();
        Order patch = new Order();
        patch.setId(order.getId());
        patch.setStatus(OrderStatus.SHIPPED.getCode());
        patch.setExpressCompany(expressCompany);
        patch.setExpressNo(expressNo);
        patch.setShippedAt(now);
        orderMapper.updateById(patch);

        order.setStatus(OrderStatus.SHIPPED.getCode());
        order.setExpressCompany(expressCompany);
        order.setExpressNo(expressNo);
        order.setShippedAt(now);
        return assembler.toVo(order);
    }

    @Override
    public OrderVo adminRemark(String orderNo, String remark) {
        Order order = requireOrder(orderNo);
        Order patch = new Order();
        patch.setId(order.getId());
        patch.setAdminRemark(remark);
        orderMapper.updateById(patch);
        order.setAdminRemark(remark);
        return assembler.toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo refund(String orderNo, String reason) {
        Order order = requireOrder(orderNo);
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == null || !(status == OrderStatus.PENDING_SHIPMENT
                || status == OrderStatus.SHIPPED || status == OrderStatus.COMPLETED)) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "当前状态不支持退款");
        }
        restoreStock(order);

        // Money that actually left the customer's account goes back through the
        // same channel. The store balance is internal, so it is credited here.
        if (PayType.BALANCE.getCode().equals(order.getPayType())) {
            memberMapper.addBalance(order.getMemberId(), order.getPayAmount());
        } else if (order.getPayType() != null
                && !PayType.MOCK.getCode().equals(order.getPayType())) {
            refundThroughChannel(order, reason);
        }

        Order patch = new Order();
        patch.setId(order.getId());
        patch.setStatus(OrderStatus.REFUNDED.getCode());
        patch.setClosedAt(LocalDateTime.now());
        patch.setAdminRemark(reason);
        orderMapper.updateById(patch);

        PayRecord record = new PayRecord();
        record.setOrderNo(order.getOrderNo());
        record.setMemberId(order.getMemberId());
        record.setPayType(order.getPayType());
        record.setAmount(order.getPayAmount().negate());
        record.setTradeNo("REFUND" + order.getOrderNo());
        record.setStatus(1);
        record.setPaidAt(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        payRecordMapper.insert(record);

        order.setStatus(OrderStatus.REFUNDED.getCode());
        order.setAdminRemark(reason);
        return assembler.toVo(order);
    }

    // ------------------------------------------------------------------
    //  Queries
    // ------------------------------------------------------------------
    @Override
    public com.ait.common.api.PageResult<OrderVo> pageForMember(Long memberId, Integer status, int page, int limit) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
                .eq(Order::getMemberId, memberId)
                .orderByDesc(Order::getId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        Page<Order> result = orderMapper.selectPage(new Page<>(page, limit), wrapper);
        List<OrderVo> records = assembler.toVoList(result.getRecords());
        return com.ait.common.api.PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public OrderVo detailForMember(Long memberId, String orderNo) {
        Order order = requireOrder(orderNo);
        if (!order.getMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return assembler.toVo(order);
    }

    @Override
    public com.ait.common.api.PageResult<OrderVo> pageForAdmin(String keyword, Integer status, String platform,
                                                               String startDate, String endDate,
                                                               int page, int limit) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery().orderByDesc(Order::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Order::getOrderNo, kw)
                    .or().like(Order::getReceiver, kw)
                    .or().like(Order::getReceiverMobile, kw)
                    .or().like(Order::getMemberNickname, kw));
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (platform != null && !platform.isEmpty()) {
            wrapper.eq(Order::getPlatform, platform);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Order::getCreatedAt, LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Order::getCreatedAt, LocalDateTime.parse(endDate + "T23:59:59"));
        }
        Page<Order> result = orderMapper.selectPage(new Page<>(page, limit), wrapper);
        return com.ait.common.api.PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(),
                assembler.toVoList(result.getRecords()));
    }

    @Override
    public OrderVo detailForAdmin(String orderNo) {
        return assembler.toVo(requireOrder(orderNo));
    }

    @Override
    public Map<String, Long> statusCounts(Long memberId) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
                .select(Order::getStatus)
                .eq(Order::getMemberId, memberId);
        List<Order> orders = orderMapper.selectList(wrapper);
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("pendingPayment", 0L);
        counts.put("pendingShipment", 0L);
        counts.put("shipped", 0L);
        counts.put("completed", 0L);
        counts.put("refunding", 0L);
        for (Order order : orders) {
            OrderStatus status = OrderStatus.of(order.getStatus());
            if (status == null) {
                continue;
            }
            switch (status) {
                case PENDING_PAYMENT:
                    counts.merge("pendingPayment", 1L, Long::sum);
                    break;
                case PENDING_SHIPMENT:
                    counts.merge("pendingShipment", 1L, Long::sum);
                    break;
                case SHIPPED:
                    counts.merge("shipped", 1L, Long::sum);
                    break;
                case COMPLETED:
                    counts.merge("completed", 1L, Long::sum);
                    break;
                case REFUNDING:
                    counts.merge("refunding", 1L, Long::sum);
                    break;
                default:
                    break;
            }
        }
        return counts;
    }

    // ------------------------------------------------------------------
    //  Scheduled jobs
    // ------------------------------------------------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders() {
        List<Order> expired = orderMapper.findExpiredUnpaid();
        int closed = 0;
        for (Order order : expired) {
            restoreStock(order);
            Order patch = new Order();
            patch.setId(order.getId());
            patch.setStatus(OrderStatus.CANCELLED.getCode());
            patch.setClosedAt(LocalDateTime.now());
            patch.setAdminRemark("超时未支付，系统自动取消");
            orderMapper.updateById(patch);
            if (order.getCouponId() != null) {
                couponService.release(order.getCouponId(), order.getMemberId());
            }
            closed++;
        }
        if (closed > 0) {
            log.info("auto cancelled {} unpaid orders", closed);
        }
        return closed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoCompleteShipped() {
        List<Order> orders = orderMapper.findAutoCompletable();
        for (Order order : orders) {
            Order patch = new Order();
            patch.setId(order.getId());
            patch.setStatus(OrderStatus.COMPLETED.getCode());
            patch.setFinishedAt(LocalDateTime.now());
            orderMapper.updateById(patch);
        }
        return orders.size();
    }

    /**
     * Refunds through the channel that took the money.
     *
     * <p>Looks up the settled payment record for the transaction id, because the
     * channel APIs refund against their own id, not ours. A failure here means
     * the customer has not been refunded yet — the stock is already restored and
     * the order is already marked refunded, so this throws rather than
     * pretending: an operator needs to see it and retry.
     */
    private void refundThroughChannel(Order order, String reason) {
        String channelCode = order.getPayType();
        if (!payChannelService.resolveIncludingDisabled(channelCode).isPresent()) {
            log.warn("order {} uses channel {} which is no longer configured; refund needs manual handling",
                    order.getOrderNo(), channelCode);
            throw new BizException(ResultCode.ERROR,
                    "原支付渠道已不可用，请在该渠道后台手动退款");
        }
        var tradeNo = payRecordMapper.selectOne(Wrappers.<PayRecord>lambdaQuery()
                .eq(PayRecord::getOrderNo, order.getOrderNo())
                .eq(PayRecord::getStatus, 1)
                .orderByDesc(PayRecord::getId)
                .last("limit 1"));
        if (tradeNo == null || tradeNo.getTradeNo() == null) {
            throw new BizException(ResultCode.ERROR, "找不到可退款的支付流水");
        }
        payGateway.refund(channelCode, tradeNo.getTradeNo(), order.getPayAmount(), reason);
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private Order requireOrder(String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private void restoreStock(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderNo, order.getOrderNo()));
        for (OrderItem item : items) {
            skuMapper.restoreStock(item.getSkuId(), item.getQuantity());
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
    }

    private List<OrderItemRequest> resolveLines(Long memberId, List<OrderItemRequest> items, String source) {
        if (items != null && !items.isEmpty()) {
            return items;
        }
        if ("cart".equalsIgnoreCase(source)) {
            return cartService.checkedLines(memberId);
        }
        return Collections.emptyList();
    }

    private BigDecimal freightFor(BigDecimal total) {
        return total.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : FLAT_FREIGHT;
    }

    private BigDecimal applyFreight(BigDecimal total, BigDecimal discount) {
        BigDecimal amount = total.subtract(discount).add(freightFor(total));
        return amount.signum() < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Order number: {@code EC + yyyyMMddHHmmss + 4 random digits}. Unique index
     * on the column makes a collision a retryable insert failure rather than a
     * silent overwrite.
     */
    private String nextOrderNo() {
        return "EC" + LocalDateTime.now().format(ORDER_NO_DATE)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /** Internal priced line holder. */
    private static final class PricedLine {
        private final Product product;
        private final ProductSku sku;
        private final int quantity;

        private PricedLine(Product product, ProductSku sku, int quantity) {
            this.product = product;
            this.sku = sku;
            this.quantity = quantity;
        }

        private BigDecimal lineAmount() {
            return sku.getPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
