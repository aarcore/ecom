package com.ait.app.rest;

import com.ait.common.api.R;
import com.ait.common.security.LoginUserContext;
import com.ait.mall.core.service.CartService;
import com.ait.mall.core.vo.CartItemVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/cart")
@RequiredArgsConstructor
@Tag(name = "App · 购物车")
public class AppCartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "购物车列表")
    public R<Map<String, Object>> list() {
        Long memberId = LoginUserContext.requiredUserId();
        List<CartItemVo> items = cartService.list(memberId);

        BigDecimal checkedTotal = items.stream()
                .filter(i -> i.getChecked() != null && i.getChecked() == 1)
                .map(CartItemVo::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int checkedCount = items.stream()
                .filter(i -> i.getChecked() != null && i.getChecked() == 1)
                .mapToInt(CartItemVo::getQuantity)
                .sum();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", items);
        payload.put("total", items.size());
        payload.put("checkedCount", checkedCount);
        payload.put("checkedAmount", checkedTotal);
        payload.put("allChecked", !items.isEmpty()
                && items.stream().allMatch(i -> i.getChecked() != null && i.getChecked() == 1));
        return R.ok(payload);
    }

    @GetMapping("/count")
    @Operation(summary = "购物车角标数量")
    public R<Integer> count() {
        return R.ok(cartService.count(LoginUserContext.requiredUserId()));
    }

    @PostMapping
    @Operation(summary = "加入购物车")
    public R<CartItemVo> add(@RequestBody AddCartRequest request) {
        return R.ok(cartService.add(LoginUserContext.requiredUserId(),
                request.getSkuId(), request.getQuantity() == null ? 1 : request.getQuantity()));
    }

    @PutMapping("/{id}/quantity")
    @Operation(summary = "修改数量")
    public R<CartItemVo> updateQuantity(@PathVariable Long id, @RequestParam int quantity) {
        return R.ok(cartService.updateQuantity(LoginUserContext.requiredUserId(), id, quantity));
    }

    @PutMapping("/{id}/checked")
    @Operation(summary = "勾选 / 取消勾选")
    public R<Void> updateChecked(@PathVariable Long id, @RequestParam boolean checked) {
        cartService.updateChecked(LoginUserContext.requiredUserId(), id, checked);
        return R.ok();
    }

    @PutMapping("/checked")
    @Operation(summary = "全选 / 全不选")
    public R<Void> checkAll(@RequestParam boolean checked) {
        cartService.checkAll(LoginUserContext.requiredUserId(), checked);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除购物车商品")
    public R<Void> remove(@PathVariable Long id) {
        cartService.remove(LoginUserContext.requiredUserId(), id);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "清空购物车")
    public R<Void> clear() {
        cartService.clear(LoginUserContext.requiredUserId());
        return R.ok();
    }

    @lombok.Data
    public static class AddCartRequest {
        private Long skuId;
        private Integer quantity;
    }
}
