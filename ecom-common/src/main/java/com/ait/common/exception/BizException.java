package com.ait.common.exception;

import com.ait.common.api.ResultCode;
import lombok.Getter;

/**
 * Expected business failure. Handled by {@code GlobalExceptionHandler} and
 * rendered as a normal {@code R} payload with a 200 HTTP status so that the
 * mini-program HTTP layer keeps working.
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
