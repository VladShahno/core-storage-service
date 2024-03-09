package com.demo.filestoresdk.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileStoreDto {

    private String downloadUri;
    private Long fileSize;
    private String contentType;
    private String fileName;
}
