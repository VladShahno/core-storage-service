package com.demo.awsstorage.dto;

import static com.demo.reststarter.common.Constants.SPECIAL_CHARACTER_VALIDATION_PATTERN;

import com.demo.reststarter.validation.ValidateSpecialCharacters;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "File Metadata Request", description = "Defines Metadata of the uploaded content.")
public class FileMetadataDto {

    @Schema(description = "Id of the resource.", example = "9053e5f0-f9fe-49ef-a49a-695bbaf30aa7")
    String resourceId;

    @Schema(description = "Preferred User Name", example = "lcp_admin_user")
    String sourceId;

    @Schema(description = "Name of the file that is being uploaded.", example = "crashdump.dmp")
    @NotBlank(message = "File name can not be empty")
    @Size(max = 255, message = "File name must be a minimum of 1 and a maximum of 255 characters")
    @ValidateSpecialCharacters(message =
        "File name should not contain following special characters - "
            + SPECIAL_CHARACTER_VALIDATION_PATTERN)
    String name;

    @Schema(description = "External Id of the resource.", example = "9053e5f0-f9fe-49ef-a49a-695bbaf30aa7")
    String storageId;

    @Schema(description = "Computed SHA-256 has of the file.", example = "8e8b95318a10368f5ea1953f9ec231556790f76e0af9030e905ffa4ab5b53fcc")
    String hash;

    @Schema(description = "File size", example = "3000")
    long sizeInBytes;

    @JsonCreator
    public FileMetadataDto(
        @JsonProperty(required = true) String fileName,
        @JsonProperty(required = true) long sizeInBytes,
        @JsonProperty(required = false) String fileId) {
        this.name = fileName;
        this.sizeInBytes = sizeInBytes;
        this.resourceId = fileId;
    }
}
