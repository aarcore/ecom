package com.ait.boot.config;

import com.ait.common.api.R;
import com.ait.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * Replaces Spring Boot's whitelabel error page with the unified envelope so a
 * mistyped admin URL returns JSON instead of HTML.
 */
@Slf4j
@RestController
public class JsonErrorController implements ErrorController {

    @RequestMapping("/error")
    public R<Void> handle(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        int code = status instanceof Integer ? (Integer) status : HttpStatus.INTERNAL_SERVER_ERROR.value();
        log.debug("error page for {} status={}", uri, code);

        if (code == HttpStatus.NOT_FOUND.value()) {
            return R.fail(ResultCode.NOT_FOUND);
        }
        if (code == HttpStatus.UNAUTHORIZED.value()) {
            return R.fail(ResultCode.UNAUTHORIZED);
        }
        if (code == HttpStatus.FORBIDDEN.value()) {
            return R.fail(ResultCode.FORBIDDEN);
        }
        if (code == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            return R.fail(ResultCode.METHOD_NOT_ALLOWED);
        }
        return R.fail(ResultCode.ERROR);
    }
}
