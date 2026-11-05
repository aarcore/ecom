package com.ait.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method whose invocations should be written to the
 * operation audit log.
 *
 * <pre>{@code
 * @OperationLog(module = "商品", action = "上架")
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    String module() default "";

    String action() default "";

    /** When false the payload/params are not persisted (use for secrets). */
    boolean saveParams() default true;
}
