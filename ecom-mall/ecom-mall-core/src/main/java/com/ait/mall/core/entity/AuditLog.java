package com.ait.mall.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Audit trail written by the logging module aspect. Named {@code AuditLog} to
 * avoid colliding with the {@code @OperationLog} annotation.
 */
@Data
@TableName("sys_operation_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    private String operator;

    private String module;

    private String action;

    private String method;

    private String uri;

    private String params;

    private String ip;

    private Long durationMs;

    private Integer success;

    private String errorMsg;

    private LocalDateTime createdAt;
}
