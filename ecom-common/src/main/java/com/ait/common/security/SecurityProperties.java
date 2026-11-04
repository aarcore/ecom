package com.ait.common.security;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "ecom.security")
public class SecurityProperties {

    @Schema(description = "Paths that bypass the C-end interceptor")
    private List<String> appPublicPaths = new ArrayList<>();

    @Schema(description = "Paths that bypass the admin interceptor")
    private List<String> adminPublicPaths = new ArrayList<>();

    /** Origins allowed to call the API from a browser (CORS). */
    private List<String> allowedOrigins = new ArrayList<>();
}
