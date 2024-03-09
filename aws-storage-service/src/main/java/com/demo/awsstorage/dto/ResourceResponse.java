package com.demo.awsstorage.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

@Value
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Schema(title = "Folder Create Response")
public class ResourceResponse {

    @Schema(description = "Id of the created/updated folder/file.")
    private String resourceId;
}
