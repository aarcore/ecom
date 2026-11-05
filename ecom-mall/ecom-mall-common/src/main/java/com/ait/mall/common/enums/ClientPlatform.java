package com.ait.mall.common.enums;

/**
 * Client surface a request originated from. Stored on orders so operations can
 * tell mini-program orders apart from H5 ones.
 */
public enum ClientPlatform {

    WEB("web", "H5 / 网页"),
    MP("mp", "微信小程序");

    private final String code;
    private final String label;

    ClientPlatform(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ClientPlatform of(String code) {
        return MP.code.equalsIgnoreCase(code) ? MP : WEB;
    }
}
