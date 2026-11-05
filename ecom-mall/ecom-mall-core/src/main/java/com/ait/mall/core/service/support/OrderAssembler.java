package com.ait.mall.core.service.support;

import com.ait.mall.core.entity.Order;
import com.ait.mall.core.entity.OrderItem;
import com.ait.common.i18n.LocaleContext;
import com.ait.mall.core.mapper.OrderItemMapper;
import com.ait.mall.core.service.I18nService;
import com.ait.mall.core.vo.OrderItemVo;
import com.ait.mall.core.vo.OrderVo;
import com.ait.mall.common.enums.OrderStatus;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entity → VO mapping for orders, including line enrichment and the unpaid
 * countdown.
 */
@Component
@RequiredArgsConstructor
public class OrderAssembler {

    /** Keep in sync with {@code OrderServiceImpl.PAY_TIMEOUT_MINUTES}. */
    private static final int PAY_TIMEOUT_MINUTES = 45;

    private final OrderItemMapper orderItemMapper;
    private final I18nService i18nService;

    public OrderVo toVo(Order order) {
        if (order == null) {
            return null;
        }
        OrderVo vo = base(order);
        vo.setItems(loadItems(Collections.singletonList(order.getOrderNo()))
                .getOrDefault(order.getOrderNo(), Collections.emptyList()));
        localize(vo.getItems());
        return vo;
    }

    public List<OrderVo> toVoList(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> orderNos = orders.stream().map(Order::getOrderNo).collect(Collectors.toList());
        Map<String, List<OrderItemVo>> items = loadItems(orderNos);
        List<OrderVo> result = new ArrayList<>(orders.size());
        for (Order order : orders) {
            OrderVo vo = base(order);
            vo.setItems(items.getOrDefault(order.getOrderNo(), Collections.emptyList()));
            result.add(vo);
        }
        // one query for the whole page rather than one per order
        localize(result.stream().flatMap(o -> o.getItems().stream()).collect(Collectors.toList()));
        return result;
    }

    /**
     * Translates the order lines at read time instead of freezing the
     * translation into {@code shop_order_item}.
     *
     * <p>The snapshot columns keep the name exactly as it was in the source
     * language, so the admin console stays consistent, and a customer who
     * switches language after ordering sees their history follow along. Prices,
     * quantities and covers are untouched — only display strings move.
     */
    private void localize(List<OrderItemVo> items) {
        if (items == null || items.isEmpty() || LocaleContext.get().isSource()) {
            return;
        }
        var locale = LocaleContext.get();
        List<Long> productIds = items.stream().map(OrderItemVo::getProductId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return;
        }
        var overrides = i18nService.textMap("product", productIds, locale);
        var terms = i18nService.termMap(locale);
        for (OrderItemVo item : items) {
            String name = overrides.getOrDefault(item.getProductId(), Collections.emptyMap()).get("name");
            if (name != null && !name.isEmpty()) {
                item.setProductName(name);
            }
            if (item.getSpecValues() != null && !terms.isEmpty()) {
                item.setSpecValues(Arrays.stream(item.getSpecValues().split(","))
                        .map(part -> terms.getOrDefault(part, part))
                        .collect(Collectors.joining(",")));
            }
        }
    }

    /** Batched line loading avoids the N+1 query on list screens. */
    private Map<String, List<OrderItemVo>> loadItems(List<String> orderNos) {
        if (orderNos == null || orderNos.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderNo, orderNos));
        return items.stream().collect(Collectors.groupingBy(OrderItem::getOrderNo,
                Collectors.mapping(this::toItemVo, Collectors.toList())));
    }

    private OrderItemVo toItemVo(OrderItem item) {
        OrderItemVo vo = new OrderItemVo();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setSkuId(item.getSkuId());
        vo.setProductName(item.getProductName());
        vo.setProductCover(item.getProductCover());
        vo.setSpecValues(item.getSpecValues());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setTotalAmount(item.getTotalAmount());
        return vo;
    }

    private OrderVo base(Order order) {
        OrderVo vo = new OrderVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setMemberId(order.getMemberId());
        vo.setMemberNickname(order.getMemberNickname());
        vo.setStatus(order.getStatus());
        OrderStatus status = OrderStatus.of(order.getStatus());
        vo.setStatusText(status == null ? "" : status.getLabel());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPayType(order.getPayType());
        vo.setPaidAt(order.getPaidAt());
        vo.setReceiver(order.getReceiver());
        vo.setReceiverMobile(order.getReceiverMobile());
        vo.setReceiverProvince(order.getReceiverProvince());
        vo.setReceiverCity(order.getReceiverCity());
        vo.setReceiverDistrict(order.getReceiverDistrict());
        vo.setReceiverDetail(order.getReceiverDetail());
        vo.setReceiverAddress(join(order.getReceiverProvince(), order.getReceiverCity(),
                order.getReceiverDistrict(), order.getReceiverDetail()));
        vo.setRemark(order.getRemark());
        vo.setPlatform(order.getPlatform());
        vo.setExpressCompany(order.getExpressCompany());
        vo.setExpressNo(order.getExpressNo());
        vo.setShippedAt(order.getShippedAt());
        vo.setFinishedAt(order.getFinishedAt());
        vo.setClosedAt(order.getClosedAt());
        vo.setAdminRemark(order.getAdminRemark());
        vo.setCreatedAt(order.getCreatedAt());

        if (status == OrderStatus.PENDING_PAYMENT && order.getCreatedAt() != null) {
            long elapsed = Duration.between(order.getCreatedAt(), LocalDateTime.now()).getSeconds();
            vo.setPayDeadlineSeconds(Math.max(0, PAY_TIMEOUT_MINUTES * 60L - elapsed));
        }
        return vo;
    }

    private String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                sb.append(part);
            }
        }
        return sb.toString();
    }
}
