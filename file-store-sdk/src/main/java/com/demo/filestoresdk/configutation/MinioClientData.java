package com.demo.filestoresdk.configutation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinioClientData {

    private String url;
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucketName;
    private String rootDirectory;
    private int expiryTime;
    private int minPartSize;
    private int retryMaxAttempts;
    private int retryFixedBackOff;
}
