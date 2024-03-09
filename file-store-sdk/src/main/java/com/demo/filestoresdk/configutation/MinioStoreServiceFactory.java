package com.demo.filestoresdk.configutation;

import com.demo.filestoresdk.service.MinioStoreService;
import io.minio.MinioClient;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class MinioStoreServiceFactory {

    @Qualifier("minio-com.demo.filestoresdk.configutation.MinioProperties")
    private final MinioProperties properties;
    private final OkHttpClient httpClient;

    public MinioStoreService create(String clientName) {
        var minioClientData = Optional.ofNullable(properties.getClients())
            .orElseThrow()
            .entrySet()
            .stream()
            .filter(entry -> clientName.equals(entry.getKey()))
            .map(Entry::getValue)
            .findFirst()
            .orElseThrow();

        minioClientData = properties.addDefaultValuesIfAbsent(minioClientData);

        var minioClient = MinioClient.builder()
            .endpoint(minioClientData.getUrl())
            .region(minioClientData.getRegion())
            .credentials(minioClientData.getAccessKey(), minioClientData.getSecretKey())
            .httpClient(httpClient)
            .build();

        if (properties.isSkipCertValidation()) {
            ignoreCertCheck(minioClient);
        }

        return new MinioStoreService(minioClient, minioClientData);
    }

    private void ignoreCertCheck(MinioClient minioClient) {
        try {
            minioClient.ignoreCertCheck();
        } catch (Exception ex) {
            log.error("Failed to ignore cert check", ex);
        }
    }
}
