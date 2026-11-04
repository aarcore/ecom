package com.ait.mall.common.enums;

/**
 * Links a banner to its tap target on the storefront.
 */
public enum BannerLinkType {

    NONE("none"),
    PRODUCT("product"),
    CATEGORY("category"),
    URL("url");

    private final String code;

    BannerLinkType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static BannerLinkType of(String code) {
        if (code == null) {
            return NONE;
        }
        for (BannerLinkType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return NONE;
    }
}
