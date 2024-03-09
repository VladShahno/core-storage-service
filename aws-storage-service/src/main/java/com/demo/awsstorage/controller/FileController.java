package com.demo.awsstorage.controller;

import com.demo.awsstorage.dto.FileMetadataDto;
import com.demo.awsstorage.dto.FileUploadResponse;
import com.demo.awsstorage.policy.InsufficientBytesInternalErrorRetryPolicy;
import com.demo.awsstorage.service.FileService;
import com.demo.filestoresdk.configutation.MinioProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@Tag(name = "File controller", description = "Provides operations allowed to upload/download files to/from S3/minio bucket")
@RequestMapping(path = {"/v1/files"}, produces = {"application/json"})
public class FileController {

    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final MinioProperties minioProperties;

    @Operation(summary = "Endpoint allows to upload content to S3 bucket", responses = {
        @ApiResponse(responseCode = "200", description = "OK", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = FileUploadResponse.class))}),
        @ApiResponse(responseCode = "400", description = "Invalid or duplicate file ID/name or file metadata doesn't have all required fields"),
        @ApiResponse(responseCode = "500", description = "Internal error or missing part in multipart data")})
    @PutMapping(produces = {"application/json"}, consumes = {"application/json",
        "multipart/form-data"})
    public ResponseEntity<FileUploadResponse> uploadFile(
        @Parameter(description = "File metadata as JSON string with partition_id, file_name and optional file_id", required = true,
            example =
                "{\"partition_id\":\"valid_and_authorized_partition_id\"," +
                    "\"file_name\":\"dump.zip\",\"storage_id\":\"id_for_file\"," +
                    "\"parent\":{\"id\":\"valid_parent_folder_id (optional field)\"}," +
                    "\"hash\":\"sha256_of_the_file_being_uploaded>\"," +
                    "\"size_in_bytes\":\"size_of_the_file\"}")
        @RequestPart("fileMetadataJson") String fileMetadata,
        @Parameter(required = true, description = "The file to be uploaded")
        @RequestPart("upload") final MultipartFile file) throws IOException {

        FileMetadataDto fileMetadataDto = createFileMetadataDto(fileMetadata);

        final String[] uploadedFileId = new String[1];

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(
            new InsufficientBytesInternalErrorRetryPolicy(minioProperties.getRetryMaxAttempts()));
        template.execute(ctx -> {
            InputStream stream = file.getInputStream();
            uploadedFileId[0] = fileService.uploadFile(fileMetadataDto, stream);
            stream.close();
            return true;
        });
        return ResponseEntity.ok(new FileUploadResponse(uploadedFileId[0]));
    }

    private FileMetadataDto createFileMetadataDto(String fileMetadata)
        throws JsonProcessingException {
        return objectMapper.readValue(fileMetadata, FileMetadataDto.class);
    }

    public FileController(final FileService fileService, final ObjectMapper objectMapper,
        final MinioProperties minioProperties) {
        this.fileService = fileService;
        this.objectMapper = objectMapper;
        this.minioProperties = minioProperties;
    }
}
