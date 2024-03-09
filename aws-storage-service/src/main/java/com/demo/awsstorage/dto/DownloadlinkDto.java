package com.demo.awsstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Downloadable Link Response", description = "Defines downloadable link of the uploaded content.")
public class DownloadlinkDto {

    @Schema(description = "Downloadable link for the uploaded content.")
    private String uri;
}
