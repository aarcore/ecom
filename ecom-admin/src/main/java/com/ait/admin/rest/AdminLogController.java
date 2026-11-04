package com.ait.admin.rest;

import com.ait.common.api.PageResult;
import com.ait.common.api.R;
import com.ait.mall.core.entity.AuditLog;
import com.ait.mall.core.mapper.AuditLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@Tag(name = "Admin · 操作日志")
public class AdminLogController {

    private final AuditLogMapper auditLogMapper;

    @GetMapping
    @Operation(summary = "操作日志列表")
    public R<PageResult<AuditLog>> page(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String module,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int limit) {
        LambdaQueryWrapper<AuditLog> wrapper = Wrappers.<AuditLog>lambdaQuery()
                .orderByDesc(AuditLog::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AuditLog::getOperator, kw)
                    .or().like(AuditLog::getAction, kw)
                    .or().like(AuditLog::getUri, kw));
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq(AuditLog::getModule, module);
        }
        Page<AuditLog> result = auditLogMapper.selectPage(new Page<>(page, limit), wrapper);
        return R.ok(PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords()));
    }
}
