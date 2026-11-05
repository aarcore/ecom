package com.ait.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Unified API envelope. Every endpoint returns this shape so that the
 * web storefront, the WeChat mini-program and the admin console can share
 * one client-side interceptor.
 */
@Data
@Schema(description = "Unified response envelope")
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int SUCCESS = 200;

    @Schema(description = "Business status code, 200 means success")
    private int code;

    @Schema(description = "Human readable message")
    private String message;

    @Schema(description = "Payload")
    private T data;

    @Schema(description = "Server timestamp in millis")
    private long timestamp = System.currentTimeMillis();

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(SUCCESS, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(SUCCESS, message, data);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null);
    }

    public boolean isSuccess() {
        return this.code == SUCCESS;
    }
}
