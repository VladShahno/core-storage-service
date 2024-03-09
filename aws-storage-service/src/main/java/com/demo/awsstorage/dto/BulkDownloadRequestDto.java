package com.demo.awsstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Bulk download request details", description = "Defines file ids.")
public class BulkDownloadRequestDto {

    @NotEmpty(message = "File ids cannot be null or empty")
    @Schema(description = "File ids to be download as a bulk.")
    private List<String> fileIds;
}
