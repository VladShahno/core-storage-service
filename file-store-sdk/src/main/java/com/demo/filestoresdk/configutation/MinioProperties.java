package com.demo.filestoresdk.configutation;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.time.Duration;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "minio")
@Primary
@Component
public class MinioProperties {

    private String url;
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucketName;
    private String rootDirectory;
    private int expiryTime;
    private int minPartSize;
    private boolean skipCertValidation;
    private int retryMaxAttempts;
    private int retryFixedBackOff;

    @NestedConfigurationProperty
    private HttpClientProperties httpClient = new HttpClientProperties();

    private Map<String, MinioClientData> clients;

    public MinioClientData getDefaultMinioClientData() {
        MinioClientData minioClientData = new MinioClientData();
        minioClientData.setUrl(url);
        minioClientData.setAccessKey(accessKey);
        minioClientData.setSecretKey(secretKey);
        minioClientData.setRegion(region);
        minioClientData.setBucketName(bucketName);
        minioClientData.setRootDirectory(rootDirectory);
        minioClientData.setExpiryTime(expiryTime);
        minioClientData.setMinPartSize(minPartSize);
        minioClientData.setRetryMaxAttempts(retryMaxAttempts);
        minioClientData.setRetryFixedBackOff(retryFixedBackOff);
        return minioClientData;
    }

    public MinioClientData addDefaultValuesIfAbsent(MinioClientData minioClientData) {
        return MinioClientData.builder()
            .url(defaultIfBlank(minioClientData.getUrl(), url))
            .accessKey(defaultIfBlank(minioClientData.getAccessKey(), accessKey))
            .secretKey(defaultIfBlank(minioClientData.getSecretKey(), secretKey))
            .region(defaultIfBlank(minioClientData.getRegion(), region))
            .bucketName(defaultIfBlank(minioClientData.getBucketName(), bucketName))
            .rootDirectory(defaultIfBlank(minioClientData.getRootDirectory(), rootDirectory))
            .expiryTime(
                minioClientData.getExpiryTime() == 0 ? expiryTime : minioClientData.getExpiryTime())
            .retryMaxAttempts(retryMaxAttempts)
            .retryFixedBackOff(retryFixedBackOff)
            .build();
    }

    @Data
    public static class HttpClientProperties {

        private Duration connectTimeout;
        private Duration writeTimeout;
        private Duration readTimeout;
        private Boolean retryOnConnectionFailure;
    }
}
