package com.demo.awsstorage.model;

import java.io.InputStream;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Builder
@Getter
@Value
public class FileAndMetadata {

    ResourceMetadata fileMetadata;
    InputStream inputStream;
}
