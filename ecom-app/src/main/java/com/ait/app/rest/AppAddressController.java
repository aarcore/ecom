package com.ait.app.rest;

import com.ait.common.api.R;
import com.ait.common.security.LoginUserContext;
import com.ait.mall.core.service.AddressService;
import com.ait.mall.core.vo.AddressVo;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/app/addresses")
@RequiredArgsConstructor
@Tag(name = "App · 收货地址")
public class AppAddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "地址列表")
    public R<List<AddressVo>> list() {
        return R.ok(addressService.list(LoginUserContext.requiredUserId()));
    }

    @PostMapping
    @Operation(summary = "新增地址")
    public R<AddressVo> create(@RequestBody AddressVo request) {
        request.setId(null);
        return R.ok(addressService.save(LoginUserContext.requiredUserId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑地址")
    public R<AddressVo> update(@PathVariable Long id, @RequestBody AddressVo request) {
        request.setId(id);
        return R.ok(addressService.save(LoginUserContext.requiredUserId(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址")
    public R<Void> delete(@PathVariable Long id) {
        addressService.delete(LoginUserContext.requiredUserId(), id);
        return R.ok();
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认地址")
    public R<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(LoginUserContext.requiredUserId(), id);
        return R.ok();
    }
}
