package com.ait.admin.rest;

import com.ait.common.api.PageResult;
import com.ait.common.api.R;
import com.ait.logging.annotation.OperationLog;
import com.ait.mall.core.service.MemberService;
import com.ait.mall.core.vo.MemberVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@Tag(name = "Admin · 会员管理")
public class AdminMemberController {

    private final MemberService memberService;

    @GetMapping
    @Operation(summary = "会员列表")
    public R<PageResult<MemberVo>> page(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String platform,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int limit) {
        return R.ok(memberService.pageForAdmin(keyword, status, platform, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "会员详情")
    public R<MemberVo> detail(@PathVariable Long id) {
        return R.ok(memberService.detailForAdmin(id));
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "会员", action = "启用/禁用会员")
    @Operation(summary = "启用 / 禁用会员")
    public R<Void> setStatus(@PathVariable Long id, @RequestParam int status) {
        memberService.setStatus(id, status);
        return R.ok();
    }
}
