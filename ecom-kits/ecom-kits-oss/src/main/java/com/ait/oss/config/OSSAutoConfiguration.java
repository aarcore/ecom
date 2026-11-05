package com.ait.oss.config;

import com.ait.oss.service.LocalStorageService;
import com.ait.oss.service.S3StorageService;
import com.ait.oss.service.StorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses the storage backend from {@code ecom.storage.type}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
public class OSSAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    @ConditionalOnProperty(name = "ecom.storage.type", havingValue = "local", matchIfMissing = true)
    public StorageService localStorageService(StorageProperties properties) {
        return new LocalStorageService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    @ConditionalOnProperty(name = "ecom.storage.type", havingValue = "s3")
    public StorageService s3StorageService(StorageProperties properties) {
        return new S3StorageService(properties);
    }
}
