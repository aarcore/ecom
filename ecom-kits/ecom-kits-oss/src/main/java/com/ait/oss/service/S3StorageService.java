package com.ait.oss.service;

import com.ait.oss.config.StorageProperties;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * S3 compatible backend; works against AWS S3, MinIO, Ceph and any OSS
 * provider that exposes an S3 endpoint.
 */
@Slf4j
public class S3StorageService implements StorageService {

    private final StorageProperties properties;
    private final AmazonS3 client;

    public S3StorageService(StorageProperties properties) {
        this.properties = properties;
        StorageProperties.S3 s3 = properties.getS3();

        BasicAWSCredentials credentials = new BasicAWSCredentials(s3.getAccessKey(), s3.getSecretKey());
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(s3.isPathStyleAccess());

        if (s3.getEndpoint() != null && !s3.getEndpoint().isEmpty()) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(s3.getEndpoint(), s3.getRegion()));
        } else {
            builder.withRegion(s3.getRegion());
        }
        this.client = builder.build();

        if (!client.doesBucketExistV2(s3.getBucketName())) {
            client.createBucket(s3.getBucketName());
            log.info("created bucket {}", s3.getBucketName());
        }
    }

    @Override
    public String store(String key, InputStream stream, long size, String mimeType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        if (mimeType != null) {
            metadata.setContentType(mimeType);
        }
        client.putObject(new PutObjectRequest(properties.getS3().getBucketName(), key, stream, metadata));
        return publicUrl(key);
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(properties.getS3().getBucketName(), key);
        } catch (Exception ex) {
            log.warn("failed to delete s3 object {}: {}", key, ex.getMessage());
        }
    }

    @Override
    public String publicUrl(String key) {
        String custom = properties.getS3().getCustomDomain();
        if (custom != null && !custom.isEmpty()) {
            return custom.endsWith("/") ? custom + key : custom + "/" + key;
        }
        return client.getUrl(properties.getS3().getBucketName(), key).toString();
    }

    @Override
    public String backend() {
        return "s3:" + properties.getS3().getBucketName();
    }
}
