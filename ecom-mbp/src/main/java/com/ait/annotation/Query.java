package com.ait.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative query condition used by {@code QueryHelpPlus} to build a
 * MyBatis-Plus {@code QueryWrapper} from a plain query object.
 *
 * <pre>{@code
 * @Query(type = Query.Type.INNER_LIKE, propName = "name")
 * private String keyword;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {

    /** Database column name; defaults to the underlined field name. */
    String propName() default "";

    /** Match strategy. */
    Type type() default Type.EQUAL;

    /** Optional comma separated list of columns for an OR'ed LIKE search. */
    String blurry() default "";

    enum Type {
        EQUAL,
        NOT_EQUAL,
        GREATER_THAN,
        GREATER_THAN_NQ,
        LESS_THAN,
        LESS_THAN_NQ,
        INNER_LIKE,
        LEFT_LIKE,
        RIGHT_LIKE,
        IN,
        NOT_IN,
        IS_NULL,
        NOT_NULL,
        BETWEEN
    }
}
