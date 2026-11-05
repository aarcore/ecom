package com.ait.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Object storage settings. Defaults to local disk so a fresh clone runs without
 * any cloud credentials; switch {@code ecom.storage.type=s3} to use any
 * S3-compatible bucket (AWS S3, MinIO, Aliyun OSS via S3 gateway, ...).
 */
@Data
@ConfigurationProperties(prefix = "ecom.storage")
public class StorageProperties {

    /** local | s3 */
    private String type = "local";

    /**
     * Public URL prefix prepended to the stored object key. Behind the bundled
     * nginx this is {@code /uploads}; in a CDN setup it becomes the CDN host.
     */
    private String publicBaseUrl = "/uploads";

    private Local local = new Local();

    private S3 s3 = new S3();

    @Data
    public static class Local {
        /** Absolute or relative directory that receives uploaded files. */
        private String root = "./data/uploads";
    }

    @Data
    public static class S3 {
        private String endpoint;
        private String region = "us-east-1";
        private String accessKey;
        private String secretKey;
        private String bucketName;
        /** Use path-style addressing (required by MinIO). */
        private boolean pathStyleAccess = true;
        /** Optional CDN domain replacing the bucket URL in returned links. */
        private String customDomain;
    }

    /** Allowed upload extensions, lowercase without the dot. */
    private List<String> allowedExtensions = new ArrayList<>(
            java.util.Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg", "pdf"));

    /** Maximum accepted upload size in bytes (default 10 MiB). */
    private long maxSize = 10L * 1024 * 1024;
}
