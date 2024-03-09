package com.demo.awsstorage.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

@Value
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Schema(title = "File Upload Response")
public class FileUploadResponse {

    @Schema(description = "Id of the created resource.")
    String fileId;
}
