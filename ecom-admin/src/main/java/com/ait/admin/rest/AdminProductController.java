package com.ait.admin.rest;

import com.ait.common.api.PageResult;
import com.ait.common.api.R;
import com.ait.logging.annotation.OperationLog;
import com.ait.mall.core.dto.ProductSaveRequest;
import com.ait.mall.core.service.ProductService;
import com.ait.mall.core.vo.ProductVo;
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

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Tag(name = "Admin · 商品管理")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "商品列表")
    public R<PageResult<ProductVo>> page(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int limit) {
        return R.ok(productService.pageForAdmin(keyword, categoryId, status, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情（含 SKU 与规格）")
    public R<ProductVo> detail(@PathVariable Long id) {
        return R.ok(productService.detail(id, false));
    }

    @PostMapping
    @OperationLog(module = "商品", action = "新增商品")
    @Operation(summary = "新增商品")
    public R<ProductVo> create(@Valid @RequestBody ProductSaveRequest request) {
        request.setId(null);
        return R.ok(productService.save(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "商品", action = "编辑商品")
    @Operation(summary = "编辑商品")
    public R<ProductVo> update(@PathVariable Long id, @Valid @RequestBody ProductSaveRequest request) {
        request.setId(id);
        return R.ok(productService.save(request));
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "商品", action = "上下架")
    @Operation(summary = "上架 / 下架")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam int status) {
        productService.updateStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "商品", action = "删除商品")
    @Operation(summary = "删除商品")
    public R<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return R.ok();
    }
}
