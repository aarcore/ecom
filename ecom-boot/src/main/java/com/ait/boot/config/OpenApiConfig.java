package com.ait.boot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * OpenAPI documentation, split into the two audiences so each client team can
 * point its generated SDK at a stable group.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI ecomOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ecom Platform API")
                        .version("1.0.0")
                        .description("单体电商系统接口：后台管理端（/api/admin）与 C 端前台（/api/app），"
                                + "同时服务 Web H5 与微信小程序。")
                        .contact(new Contact().name("Ecom Platform").email("dev@ecom.dev")))
                .servers(Collections.singletonList(new Server().url("/").description("current host")))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("后台与 C 端 token 分别签发，互不通用")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("1-admin")
                .displayName("管理端接口")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi appApi() {
        return GroupedOpenApi.builder()
                .group("2-app")
                .displayName("C 端接口")
                .pathsToMatch("/api/app/**")
                .build();
    }
}
