package com.demo.awsstorage.service;

import static com.demo.awsstorage.constant.Logging.CONTENT_TYPE;
import static com.demo.awsstorage.constant.Logging.FILE_ID;
import static com.demo.awsstorage.constant.Logging.NAME;
import static com.demo.awsstorage.constant.Logging.SIZE_IN_BYTES;
import static com.demo.awsstorage.constant.Logging.TIME_TAKEN_IN_MILLI_SECONDS;
import static com.demo.awsstorage.constant.Logging.UPLOAD_STATUS;
import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.Validate.notEmpty;

import com.demo.awsstorage.dto.DownloadlinkDto;
import com.demo.awsstorage.dto.FileMetadataDto;
import com.demo.awsstorage.dto.FileStatDto;
import com.demo.awsstorage.dto.ResourcePatchDto;
import com.demo.awsstorage.exception.FileStorageConflictException;
import com.demo.awsstorage.exception.FileStorageException;
import com.demo.awsstorage.exception.FileStorageIOException;
import com.demo.awsstorage.exception.FileStorageNotFoundException;
import com.demo.awsstorage.exception.FileUploadStatusException;
import com.demo.awsstorage.model.FileAndMetadata;
import com.demo.awsstorage.model.NodeType;
import com.demo.awsstorage.model.ResourceMetadata;
import com.demo.awsstorage.model.UploadStatus;
import com.demo.awsstorage.repository.FileMetadataRepository;
import com.demo.filestoresdk.model.DownloadFile;
import com.demo.filestoresdk.service.MinioStoreService;
import com.demo.reststarter.exception.BadRequestRestException;
import com.demo.reststarter.exception.InternalErrorException;
import io.minio.StatObjectResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class FileService {

    public static final String MINIO_FILE_SEPARATOR = "images/";
    private static final String FORMAT_ZIP = "application/zip";
    private final FileMetadataRepository fileMetadataRepository;
    private final MinioStoreService minioStoreService;
    private final Tika tika = new Tika();

    public String uploadFile(@Valid FileMetadataDto fileMetadataDto, final InputStream is) {
        StopWatch stopWatch = new StopWatch();
        try {
            String fileId = fileMetadataDto.getResourceId();
            if (StringUtils.isNotBlank(fileId) && fileMetadataRepository.existsById(fileId)) {
                throw new FileStorageConflictException(
                    MessageFormat.format("File ID {0} is already in use", fileId));
            }

            stopWatch.start();
            ResourceMetadata fileMetadata = addMetadataAndStoreFile(fileMetadataDto, is);
            stopWatch.stop();

            log.debug("Successfully uploaded the file {} {} {} {}",
                keyValue(FILE_ID, fileMetadata.getResourceId()),
                keyValue(CONTENT_TYPE, fileMetadata.getContentType()),
                keyValue(SIZE_IN_BYTES, fileMetadata.getSizeInBytes()),
                keyValue(TIME_TAKEN_IN_MILLI_SECONDS, stopWatch.getTotalTimeMillis() + " ms"));

            return fileMetadata.getResourceId();
        } catch (Exception ex) {
            log.error("Failed to upload the file {}",
                keyValue(FILE_ID, fileMetadataDto.getResourceId()), ex);
            throw ex;
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
        }
    }

    public FileAndMetadata downloadByFileId(final ResourceMetadata fileMetadata) {
        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            DownloadFile file = minioStoreService.getFileFromStorage(
                minioStoreService.getBucketName(), MINIO_FILE_SEPARATOR);
            stopWatch.stop();

            FileAndMetadata fileAndMetadata = FileAndMetadata.builder()
                .inputStream(file.getInputStream())
                .fileMetadata(fileMetadata)
                .build();

            log.debug("Successfully downloaded the file {} {} {}",
                keyValue(FILE_ID, fileMetadata.getResourceId()),
                keyValue(CONTENT_TYPE, fileMetadata.getContentType()),
                keyValue(TIME_TAKEN_IN_MILLI_SECONDS, stopWatch.getTotalTimeMillis() + " ms"));

            return fileAndMetadata;
        } catch (FileUploadStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to download the file {}",
                keyValue(FILE_ID, fileMetadata.getResourceId()));
            throw ex;
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
        }
    }

    public FileAndMetadata bulkDownload(final List<ResourceMetadata> fileMetadataList,
        final String outputFileName) {
        final StopWatch stopWatch = new StopWatch();
        final var bulkFileMetadata = ResourceMetadata.builder()
            .name(outputFileName)
            .contentType(tika.detect(outputFileName))
            .build();

        try {
            var fileMetadataMap = new HashMap<String, ResourceMetadata>();
            for (var fileMetadata : fileMetadataList) {
                fileMetadataMap.put(fileMetadata.getResourceId(), fileMetadata);
            }

            stopWatch.start();
            // Step 01: download & package
            final byte[] bulkFile;
            if (bulkFileMetadata.getContentType().equals(FORMAT_ZIP)) {
                bulkFile = downloadAndZip(fileMetadataMap);
            } else {
                throw new FileStorageException(
                    "Unsupported package-type (sourceId: " + bulkFileMetadata.getSourceId() +
                        ", contentType: " + bulkFileMetadata.getContentType() + ", name: "
                        + bulkFileMetadata.getName() + ")");
            }

            stopWatch.stop();

            return getFileAndMetadata(bulkFileMetadata, bulkFile);
        } catch (FileUploadStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Failed to download bulk files {} {}", fileMetadataList.stream()
                .map(ResourceMetadata::getResourceId)
                .collect(Collectors.toSet()), keyValue(NAME, bulkFileMetadata.getName()));
            throw ex;
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
        }
    }

    private FileAndMetadata getFileAndMetadata(final ResourceMetadata bulkFileMetadata,
        final byte[] bulkFile) {
        try (var inputStream = new ByteArrayInputStream(bulkFile)) {
            bulkFileMetadata.setSizeInBytes(bulkFile.length);
            return FileAndMetadata.builder()
                .inputStream(inputStream)
                .fileMetadata(bulkFileMetadata)
                .build();
        } catch (IOException ex) {
            throw new FileStorageException("IO Exception for resource id: {}", ex,
                bulkFileMetadata.getSourceId());
        }
    }

    public void deleteByFileId(ResourceMetadata fileMetadata) {
        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            minioStoreService.removeObjectFromBucket(minioStoreService.getBucketName(),
                MINIO_FILE_SEPARATOR);
            stopWatch.stop();

            fileMetadataRepository.deleteById(fileMetadata.getResourceId());

            log.debug("Successfully deleted the file {} {} {}",
                keyValue(FILE_ID, fileMetadata.getResourceId()),
                keyValue(CONTENT_TYPE, fileMetadata.getContentType()),
                keyValue(TIME_TAKEN_IN_MILLI_SECONDS, stopWatch.getTotalTimeMillis() + " ms"));
        } catch (FileStorageNotFoundException ex) {
            log.error("Failed to delete the file {}",
                keyValue(FILE_ID, fileMetadata.getResourceId()), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to delete the file {}",
                keyValue(FILE_ID, fileMetadata.getResourceId()), ex);
            throw new InternalErrorException("Unable to delete file");
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
        }
    }

    public DownloadlinkDto presignedGetObject(ResourceMetadata fileMetadata) {
        try {

            String uri = minioStoreService
                .presignedGetObjectWithOutFileScanCheck(MINIO_FILE_SEPARATOR, null, 100000);

            log.debug("Successfully generated presigned link for the file {} {}",
                keyValue(FILE_ID, fileMetadata.getResourceId()),
                keyValue(CONTENT_TYPE, fileMetadata.getContentType()));

            return DownloadlinkDto.builder()
                .uri(uri)
                .build();
        } catch (FileUploadStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to generate the presigned link {}",
                keyValue(FILE_ID, fileMetadata.getResourceId()), ex);
            throw ex;
        }
    }

    public FileStatDto getFileStat(ResourceMetadata fileMetadata) {
        StatObjectResponse statObjectResponse = minioStoreService.getFileStat(MINIO_FILE_SEPARATOR);
        return FileStatDto.builder()
            .etag(statObjectResponse.etag())
            .length(statObjectResponse.size())
            .contentType(statObjectResponse.contentType())
            .scanStatus(fileMetadata.getScanStatus())
            .build();
    }

    public ResourceMetadata getFileMetadataById(String fileId) {
        log.debug("Fetching metadata for File ID {}", fileId);
        return fileMetadataRepository
            .findByResourceIdAndNodeType(fileId, NodeType.FILE)
            .orElseThrow(
                () -> new FileStorageNotFoundException(
                    String.format("File not available for file ID %s", fileId)));
    }

    public List<ResourceMetadata> getFileMetadataById(List<String> fileIds) {
        log.debug("Fetching metadata for File IDs {}", fileIds);
        notEmpty(fileIds);
        var resourceMetadataList = fileMetadataRepository
            .findByResourceIdInAndNodeType(fileIds, NodeType.FILE);
        Set<String> metaDataFileIds = resourceMetadataList.stream()
            .map(ResourceMetadata::getResourceId).collect(Collectors.toSet());
        var fileIdsNotFound = fileIds.stream()
            .filter(fileId -> !metaDataFileIds.contains(fileId))
            .collect(Collectors.toSet());
        if (isNotEmpty(fileIdsNotFound)) {
            throw new FileStorageNotFoundException(
                "Metadata not found for file id " + String.join(", ", fileIdsNotFound));
        }
        return resourceMetadataList;
    }

    private ResourceMetadata addMetadataAndStoreFile(FileMetadataDto fileMetadataDto,
        final InputStream inputStream) {
        String contentType = tika.detect(fileMetadataDto.getName());
        ResourceMetadata fileMetadata = convertToMetadataEntity(fileMetadataDto, contentType);
        try {
            fileMetadata = fileMetadataRepository.save(fileMetadata);
        } catch (DataIntegrityViolationException e) {
            throw new FileStorageConflictException(
                String.format("File with id %s already exists", fileMetadataDto.getResourceId()));
        }

        storeFile(inputStream, fileMetadata);
        updateActualFileSize(fileMetadata);
        updateUploadStatus(fileMetadata.getResourceId(), UploadStatus.COMPLETED);

        return fileMetadata;
    }

    private void storeFile(final InputStream inputStream, ResourceMetadata fileMetadata) {
        String storagePath = MINIO_FILE_SEPARATOR;
        try {
            uploadFile(inputStream, fileMetadata.getSizeInBytes(), storagePath);
        } catch (BadRequestRestException | FileStorageConflictException e) {
            fileMetadataRepository.delete(fileMetadata);
            throw e;
        } catch (InternalErrorException e) {
            fileMetadataRepository.delete(fileMetadata);
            throw new FileStorageIOException(
                "Error while uploading file (sourceId: " + fileMetadata.getSourceId() +
                    ", fileId: " + fileMetadata.getResourceId() + ", name: "
                    + fileMetadata.getName() + ")", e);
        }
    }

    private void updateActualFileSize(final ResourceMetadata fileMetadata) {
        var fileId = fileMetadata.getResourceId();
        var size = getFileStat(fileMetadata).getLength();
        fileMetadata.setSizeInBytes(size);
        fileMetadataRepository.save(fileMetadata);
    }

    private void uploadFile(InputStream inputStream, Long fileSize, String storagePath) {
        minioStoreService.putObjectToStorage(minioStoreService.getBucketName(), inputStream,
            fileSize, storagePath);
    }

    ResourceMetadata convertToMetadataEntity(FileMetadataDto fileMetadataDto, String contentType) {
        ResourceMetadata.ResourceMetadataBuilder fileMetadataBuilder = ResourceMetadata.builder(
            fileMetadataDto);
        if (StringUtils.isBlank(fileMetadataDto.getResourceId())) {
            fileMetadataBuilder = fileMetadataBuilder.resourceId(UUID.randomUUID().toString());
        }
        if (StringUtils.isBlank(fileMetadataDto.getStorageId())) {
            fileMetadataBuilder = fileMetadataBuilder.storageId(UUID.randomUUID().toString());
        }
        return fileMetadataBuilder
            .contentType(contentType)
            .build();
    }

    public void updateUploadStatus(String fileId, UploadStatus uploadStatus) {
        Objects.requireNonNull(fileId, "File ID should not be null");
        try {
            fileMetadataRepository.updateUploadStatus(fileId, uploadStatus);
            log.debug("File upload status updated {} {}",
                keyValue(FILE_ID, fileId),
                keyValue(UPLOAD_STATUS, uploadStatus));
        } catch (Exception e) {
            log.error("Unable to update File upload status {} {}",
                keyValue(FILE_ID, fileId),
                keyValue(UPLOAD_STATUS, uploadStatus));
        }
    }

    public ResourceMetadata update(ResourceMetadata fileMetaData, ResourcePatchDto filePatchDto) {
        try {
            if (filePatchDto.getName() != null) {
                String fileName = filePatchDto.getName();
                fileMetaData.setName(fileName);
            }

            ResourceMetadata savedFileMetadata = fileMetadataRepository.save(fileMetaData);
            log.debug("Successfully updated file metadata {} {}",
                keyValue(FILE_ID, savedFileMetadata.getResourceId()),
                keyValue(NAME, savedFileMetadata.getName()));
            return savedFileMetadata;
        } catch (Exception ex) {
            log.error("Failed to update the file metadata {}",
                keyValue(FILE_ID, fileMetaData.getResourceId()), ex);
            throw ex;
        }
    }

    private byte[] downloadAndZip(final Map<String, ResourceMetadata> resourceMetadataMap) {
        try (var byteArrayOutputStream = new ByteArrayOutputStream();
            var zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            DownloadFile file;
            for (ResourceMetadata fileMetadata : resourceMetadataMap.values()) {
                file = minioStoreService.getFileFromStorage(minioStoreService.getBucketName(),
                    MINIO_FILE_SEPARATOR);

                zipOutputStream.putNextEntry(new ZipEntry(fileMetadata.getName()));
                zipOutputStream.write(file.getInputStream().readAllBytes());
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new FileStorageIOException(
                "Error while zipping file with fileIds: " + resourceMetadataMap.keySet() + ")", e);
        }
    }
}
