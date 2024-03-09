package com.demo.filestoresdk.model;

import io.minio.StatObjectResponse;
import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class DownloadFile {

    private InputStream inputStream;
    private StatObjectResponse objectStat;
}
