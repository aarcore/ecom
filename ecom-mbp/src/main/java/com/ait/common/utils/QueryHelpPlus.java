package com.ait.common.utils;

import com.ait.annotation.Query;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Reflection driven query builder. Turns a populated query object into a
 * {@link QueryWrapper} based on {@link Query} annotations.
 */
@Slf4j
public final class QueryHelpPlus {

    private QueryHelpPlus() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <R, Q> QueryWrapper<R> getPredicate(R root, Q query) {
        QueryWrapper<R> wrapper = new QueryWrapper<>();
        if (query == null) {
            return wrapper;
        }

        for (Field field : getAllFields(query.getClass())) {
            Query annotation = field.getAnnotation(Query.class);
            if (annotation == null) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(query);
                if (isEmpty(value)) {
                    continue;
                }

                String column = humpToUnderline(
                        isBlank(annotation.propName()) ? field.getName() : annotation.propName());

                String blurry = annotation.blurry();
                if (!isBlank(blurry)) {
                    String[] columns = blurry.split(",");
                    wrapper.and(w -> {
                        for (int i = 0; i < columns.length; i++) {
                            if (i > 0) {
                                w.or();
                            }
                            w.like(humpToUnderline(columns[i].trim()), value);
                        }
                    });
                    continue;
                }

                switch (annotation.type()) {
                    case EQUAL:
                        wrapper.eq(column, value);
                        break;
                    case NOT_EQUAL:
                        wrapper.ne(column, value);
                        break;
                    case GREATER_THAN:
                        wrapper.ge(column, value);
                        break;
                    case GREATER_THAN_NQ:
                        wrapper.gt(column, value);
                        break;
                    case LESS_THAN:
                        wrapper.le(column, value);
                        break;
                    case LESS_THAN_NQ:
                        wrapper.lt(column, value);
                        break;
                    case INNER_LIKE:
                        wrapper.like(column, value);
                        break;
                    case LEFT_LIKE:
                        wrapper.likeLeft(column, value);
                        break;
                    case RIGHT_LIKE:
                        wrapper.likeRight(column, value);
                        break;
                    case IN:
                        if (value instanceof Collection) {
                            wrapper.in(column, (Collection<?>) value);
                        }
                        break;
                    case NOT_IN:
                        if (value instanceof Collection) {
                            wrapper.notIn(column, (Collection<?>) value);
                        }
                        break;
                    case IS_NULL:
                        wrapper.isNull(column);
                        break;
                    case NOT_NULL:
                        wrapper.isNotNull(column);
                        break;
                    case BETWEEN:
                        if (value instanceof List && ((List<?>) value).size() == 2) {
                            List<?> range = (List<?>) value;
                            wrapper.between(column, range.get(0), range.get(1));
                        }
                        break;
                    default:
                        break;
                }
            } catch (IllegalAccessException ex) {
                log.warn("cannot read query field {}", field.getName(), ex);
            }
        }
        return wrapper;
    }

    private static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence) {
            return isBlank((CharSequence) value);
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        return false;
    }

    private static boolean isBlank(CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return true;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    /** camelCase -> snake_case */
    public static String humpToUnderline(String name) {
        if (name == null || name.contains("_")) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
