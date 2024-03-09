package com.demo.filestoresdk.configutation;

import com.demo.filestoresdk.service.MinioStoreService;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.url")
@EnableConfigurationProperties({MinioProperties.class})
public class FileStoreSdkAutoConfiguration {

    @Qualifier("minio-com.demo.filestoresdk.configutation.MinioProperties")
    private final MinioProperties properties;
    private final MessageSource messageSource;

    @Bean
    public MinioStoreService minioStoreService(MinioClient minioClient) {
        return new MinioStoreService(minioClient, properties.getDefaultMinioClientData());
    }

    @Bean
    @ConditionalOnMissingBean(MinioClient.class)
    MinioClient minioClient(OkHttpClient httpClient) {
        return MinioClient.builder()
            .endpoint(properties.getUrl())
            .region(properties.getRegion())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .httpClient(httpClient)
            .build();
    }

    @Bean
    OkHttpClient httpClient() {
        final var client = properties.getHttpClient();

        return new OkHttpClient()
            .newBuilder()
            .connectTimeout(client.getConnectTimeout())
            .writeTimeout(client.getWriteTimeout())
            .readTimeout(client.getReadTimeout())
            .retryOnConnectionFailure(client.getRetryOnConnectionFailure())
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .build();
    }

    @PostConstruct
    public void updateMessageSource() {
        if (messageSource instanceof ResourceBundleMessageSource resourceBundle) {
            resourceBundle.addBasenames("filestore/messages");
        }
    }
}
