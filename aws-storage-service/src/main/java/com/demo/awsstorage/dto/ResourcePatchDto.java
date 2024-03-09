package com.demo.awsstorage.dto;

import static com.demo.reststarter.common.Constants.SPECIAL_CHARACTER_VALIDATION_PATTERN;

import com.demo.reststarter.validation.ValidateSpecialCharacters;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Schema(title = "Resource Patch Request", description = "Defines parameters to update a folder/file")
public class ResourcePatchDto {

    @Schema(description = "Name of the resource", example = "resource-name")
    @ValidateSpecialCharacters(message = "Name should not contain following special characters - "
        + SPECIAL_CHARACTER_VALIDATION_PATTERN)
    String name;
}
