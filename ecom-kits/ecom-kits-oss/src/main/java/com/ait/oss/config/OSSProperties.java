package com.ait.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "oss")
public class OSSProperties {
    private String endpoint;

    private String customDomain;

    private Boolean pathStyleAccess = false;

    private String appID;

    private String region;

    private String accessKey;

    private String secretKey;

    private String bucketName;

    private Integer maxConnection = 100;

    private String defaultName;
}
