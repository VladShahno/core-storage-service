package com.demo.awsstorage.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Builder
@Data
@Getter
public class FileStatDto {

    String scanStatus;
    String etag;
    String contentType;
    long length;
}
