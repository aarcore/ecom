package com.ait.logging.aspect;

import com.ait.common.security.LoginUser;
import com.ait.common.security.LoginUserContext;
import com.ait.common.utils.JsonUtils;
import com.ait.common.utils.RequestUtils;
import com.ait.logging.annotation.OperationLog;
import com.ait.mall.core.entity.AuditLog;
import com.ait.mall.core.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists an audit row for every {@link OperationLog} annotated controller
 * method. Failures are logged but never propagate: auditing must not break the
 * business call.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_PARAM_LENGTH = 2000;

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            success = false;
            error = truncate(ex.getMessage(), 900);
            throw ex;
        } finally {
            try {
                save(joinPoint, operationLog, System.currentTimeMillis() - start, success, error);
            } catch (Exception ex) {
                log.warn("failed to persist operation log: {}", ex.getMessage());
            }
        }
    }

    private void save(ProceedingJoinPoint joinPoint, OperationLog annotation,
                      long durationMs, boolean success, String error) {
        AuditLog row = new AuditLog();
        row.setModule(annotation.module());
        row.setAction(annotation.action());
        row.setDurationMs(durationMs);
        row.setSuccess(success ? 1 : 0);
        row.setErrorMsg(error);
        row.setCreatedAt(LocalDateTime.now());

        LoginUser user = LoginUserContext.get();
        if (user != null) {
            row.setOperatorId(user.getUserId());
            row.setOperator(user.getUsername());
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        row.setMethod(signature.getDeclaringType().getSimpleName() + "." + signature.getName());

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            row.setUri(request.getRequestURI());
            row.setIp(RequestUtils.clientIp(request));
        }

        if (annotation.saveParams()) {
            row.setParams(serialiseArgs(joinPoint.getArgs()));
        }
        auditLogMapper.insert(row);
    }

    private String serialiseArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> safe = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof MultipartFile || arg instanceof HttpServletRequest
                    || arg instanceof javax.servlet.http.HttpServletResponse) {
                continue;
            }
            safe.add(arg);
        }
        return truncate(JsonUtils.toJson(safe), MAX_PARAM_LENGTH);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
