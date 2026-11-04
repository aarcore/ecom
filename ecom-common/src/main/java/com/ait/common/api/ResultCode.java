package com.ait.common.api;

import lombok.Getter;

/**
 * Business status codes. The first three digits mirror HTTP semantics so the
 * front-end can keep a single map of handlers.
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "登录状态已失效，请重新登录"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方式不支持"),

    ERROR(500, "系统繁忙，请稍后再试"),

    /* ---- account ---- */
    ACCOUNT_NOT_FOUND(1001, "账号不存在"),
    PASSWORD_ERROR(1002, "账号或密码错误"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),
    ACCOUNT_EXISTS(1004, "账号已存在"),
    PHONE_EXISTS(1005, "手机号已被注册"),
    SMS_CODE_ERROR(1006, "验证码错误或已过期"),
    TOKEN_INVALID(1007, "登录凭证无效"),

    /* ---- catalog ---- */
    PRODUCT_NOT_FOUND(2001, "商品不存在或已下架"),
    PRODUCT_OFF_SHELF(2002, "商品已下架"),
    STOCK_NOT_ENOUGH(2003, "商品库存不足"),
    SKU_NOT_FOUND(2004, "商品规格不存在"),
    CATEGORY_HAS_CHILDREN(2005, "请先删除子分类"),
    CATEGORY_HAS_PRODUCT(2006, "分类下还有商品，无法删除"),

    /* ---- trade ---- */
    CART_EMPTY(3001, "购物车中没有选中的商品"),
    ORDER_NOT_FOUND(3002, "订单不存在"),
    ORDER_STATUS_ERROR(3003, "订单当前状态不允许该操作"),
    ADDRESS_NOT_FOUND(3004, "收货地址不存在"),
    COUPON_NOT_AVAILABLE(3005, "优惠券不可用"),
    COUPON_USED(3006, "优惠券已被使用"),
    PAY_FAILED(3007, "支付失败"),
    CART_ITEM_NOT_FOUND(3008, "购物车商品不存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
