package com.demo.filestoresdk.service;


import static com.demo.filestoresdk.utils.FileStoreConstants.AWSCodes.NO_SUCH_KEY;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.COPY_DATA_ERROR;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.GET_DATA_ERROR;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.GET_FILES_NAME_FROM_BUCKET;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.GET_METADATA_ERROR;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.GET_PRESIGNED_ERROR;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.GET_TAGS_ERROR;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.REMOVE_ERROR;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.SAVE_ERROR_CODE;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.SAVE_TAGS_ERROR;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.SCAN_FILE_NOT_FOUND;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.STATUS_ABSENT_ERROR;
import static com.demo.filestoresdk.utils.FileStoreConstants.ErrorCodes.STATUS_INFECTED_ERROR;
import static com.demo.filestoresdk.utils.FileStoreConstants.FileMetadataHeaders.AV_STATUS;
import static com.demo.filestoresdk.utils.FileStoreConstants.FileMetadataHeaders.AV_TIMESTAMP;
import static com.demo.filestoresdk.utils.FileStoreConstants.FileStatus.INFECTED;
import static com.demo.filestoresdk.utils.FileTools.PATH_DELIMITER;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import com.demo.filestoresdk.configutation.MinioClientData;
import com.demo.filestoresdk.exception.BadRequestRestException;
import com.demo.filestoresdk.exception.InternalErrorException;
import com.demo.filestoresdk.exception.NotFoundException;
import com.demo.filestoresdk.model.DownloadFile;
import com.demo.filestoresdk.model.FileDto;
import com.demo.filestoresdk.model.FileStoreDto;
import com.demo.filestoresdk.model.FileUri;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetObjectTagsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.SetObjectTagsArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
@RequiredArgsConstructor
public class MinioStoreService {

    private final Tika tika = new Tika();

    private final MinioClient minioClient;
    private final MinioClientData minioClientData;

    public String getBucketName() {
        return minioClientData.getBucketName();
    }

    public String getRootDirectory() {
        return minioClientData.getRootDirectory();
    }

    public FileStoreDto putObjectToStorage(InputStream data, Long size, FileUri fileUri) {
        return putObjectToStorage(minioClientData.getBucketName(), data, size,
            fileUri.buildFilePath(),
            fileUri.getFullPackageName());
    }

    public FileStoreDto putObjectToStorage(InputStream data, Long size, String filePath) {
        return putObjectToStorage(minioClientData.getBucketName(), data, size, filePath);
    }

    public FileStoreDto putObjectToStorage(String bucketName, InputStream data, Long size,
        String filePath) {
        return putObjectToStorage(bucketName, data, size, filePath, null);
    }

    private FileStoreDto putObjectToStorage(
        String bucketName,
        InputStream data,
        Long size,
        String filePath,
        String fullPackageName) {
        try {
            var bufferedInputStream = new BufferedInputStream(data);
            var contentType = retrieveContentType(bufferedInputStream, filePath);
            var fileStoreDto = new FileStoreDto();

            var headers = new HashMap<String, String>();
            if (StringUtils.isNotBlank(fullPackageName)) {
                headers.put(CONTENT_DISPOSITION,
                    "attachment; fileName=\"" + fullPackageName + "\"");
            }

            var objectSize = Optional.ofNullable(size).orElse(-1L);

            var putObjectArgs = PutObjectArgs.builder().bucket(bucketName).object(filePath)
                .stream(bufferedInputStream, objectSize, minioClientData.getMinPartSize())
                .contentType(contentType)
                .headers(headers)
                .build();

            RetryTemplate template = RetryTemplate.builder()
                .maxAttempts(minioClientData.getRetryMaxAttempts())
                .fixedBackoff(minioClientData.getRetryFixedBackOff())
                .retryOn(IOException.class)
                .build();

            template.execute(ctx -> {
                minioClient.putObject(putObjectArgs);
                fileStoreDto.setDownloadUri(PATH_DELIMITER + filePath);
                fileStoreDto.setFileSize(size);
                fileStoreDto.setContentType(contentType);
                return true;
            });
            return fileStoreDto;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalErrorException(SAVE_ERROR_CODE, e);
        }
    }

    public void setAntivirusTags(String bucketName, String filePath, String status) {
        try {
            var tags = Map.of(AV_STATUS, status, AV_TIMESTAMP, new Date().toString());

            minioClient.setObjectTags(SetObjectTagsArgs.builder()
                .bucket(bucketName)
                .object(getRelativeFilePath(filePath))
                .tags(tags)
                .build());
        } catch (MinioException | GeneralSecurityException | IOException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalErrorException(SAVE_TAGS_ERROR, e);
        }
    }

    public DownloadFile getFileFromStorageForScan(String filePath) {
        return getFileFromStorage(minioClientData.getBucketName(), filePath, null);
    }

    public DownloadFile getFileFromStorageForScan(String bucketName, String filePath) {
        return getFileFromStorage(bucketName, filePath, null);
    }

    private DownloadFile getFileFromStorage(String bucketName, String filePath,
        StatObjectResponse objectStat) {
        try {
            var getObjectArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(getRelativeFilePath(filePath))
                .build();
            return DownloadFile.builder()
                .objectStat(objectStat)
                .inputStream(minioClient.getObject(getObjectArgs))
                .build();
        } catch (MinioException | GeneralSecurityException | IOException ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw new InternalErrorException(GET_DATA_ERROR, ex);
        }
    }

    public DownloadFile getFileFromStorage(String filePath) {
        var objectStat = getFileStat(filePath);
        checkScanStatus(filePath, objectStat.headers());

        return getFileFromStorage(minioClientData.getBucketName(), filePath, objectStat);
    }

    public DownloadFile getFileFromStorage(String bucketName, String filePath) {
        var objectStat = getFileStat(bucketName, filePath);
        checkScanStatus(filePath, objectStat.headers());
        return getFileFromStorage(bucketName, filePath, objectStat);
    }

    public DownloadFile getFileFromStorageWithoutCheckScanStatus(String bucketName,
        String filePath) {
        var objectStat = getFileStat(bucketName, filePath);
        return getFileFromStorage(bucketName, filePath, objectStat);
    }

    private String getRelativeFilePath(String filePath) {
        return filePath != null && filePath.startsWith("/")
            ? filePath.replaceFirst("/", "")
            : filePath;
    }

    private void checkScanStatus(String filePath, Headers headers) {
        try {
            var tags = minioClient.getObjectTags(GetObjectTagsArgs.builder()
                    .bucket(minioClientData.getBucketName())
                    .object(getRelativeFilePath(filePath))
                    .build())
                .get();
            if (StringUtils.isBlank(tags.get(AV_STATUS)) && StringUtils.isBlank(
                headers.get(AV_STATUS))) {
                throw new BadRequestRestException(STATUS_ABSENT_ERROR);
            }
            if (INFECTED.equalsIgnoreCase(tags.get(AV_STATUS)) || INFECTED.equalsIgnoreCase(
                headers.get(AV_STATUS))) {
                throw new BadRequestRestException(STATUS_INFECTED_ERROR);
            }
        } catch (MinioException | GeneralSecurityException | IOException e) {
            throw new InternalErrorException(GET_TAGS_ERROR, e);
        }
    }

    public StatObjectResponse getFileStat(String filePath) {
        return getFileStat(minioClientData.getBucketName(), filePath);
    }

    public StatObjectResponse getFileStat(String bucketName, String filePath) {
        final StatObjectResponse[] response = new StatObjectResponse[1];
        try {
            var statObjectArgs = StatObjectArgs.builder()
                .bucket(bucketName)
                .object(getRelativeFilePath(filePath))
                .build();
            RetryTemplate template = RetryTemplate.builder()
                .maxAttempts(minioClientData.getRetryMaxAttempts())
                .fixedBackoff(minioClientData.getRetryFixedBackOff())
                .retryOn(IOException.class)
                .build();
            template.execute(ctx -> {
                response[0] = minioClient.statObject(statObjectArgs);
                return true;
            });
            return response[0];
        } catch (ErrorResponseException ex) {
            if (NO_SUCH_KEY.equals(ex.errorResponse().code())) {
                log.error(ex.getLocalizedMessage(), ex);
                throw new NotFoundException(ex.getLocalizedMessage(), ex);
            } else {
                log.error(ex.getLocalizedMessage(), ex);
                throw new InternalErrorException(GET_PRESIGNED_ERROR, ex);
            }
        } catch (Exception ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw new InternalErrorException(GET_METADATA_ERROR, ex);
        }
    }

    public void removeObjectFromBucket(String filePath) {
        removeObjectFromBucket(minioClientData.getBucketName(), filePath);
    }

    public void removeObjectFromBucket(String bucketName, String filePath) {
        try {
            var removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(getRelativeFilePath(filePath))
                .build();
            minioClient.removeObject(removeObjectArgs);
        } catch (MinioException | GeneralSecurityException | IOException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalErrorException(REMOVE_ERROR, e);
        }
    }

    public void removeObjectsFromBucket(List<String> objectIds) {
        removeObjectsFromBucket(minioClientData.getBucketName(), objectIds);
    }

    public void removeObjectsFromBucket(String bucketName, List<String> objectIds) {
        try {
            var deleteObjects = objectIds.stream()
                .map(this::getRelativeFilePath)
                .map(DeleteObject::new)
                .toList();
            var removeObjectsArgs = RemoveObjectsArgs.builder()
                .bucket(bucketName)
                .objects(deleteObjects)
                .build();
            for (var errorResult : minioClient.removeObjects(removeObjectsArgs)) {
                var error = errorResult.get();
                log.error("Failed to remove object'" + error.objectName() + "'. Error:"
                    + error.message());
            }
        } catch (MinioException | GeneralSecurityException | IOException ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw new InternalErrorException(REMOVE_ERROR, ex);
        }
    }

    public List<String> scanListOfObjects(String pathPrefix) {
        return scanListOfObjects(pathPrefix, true);
    }

    public List<String> scanListOfObjects(String pathPrefix, boolean isRecursive) {
        try {
            var listObjectsArgs = ListObjectsArgs.builder()
                .bucket(minioClientData.getBucketName())
                .prefix(pathPrefix)
                .recursive(isRecursive)
                .build();
            var objects = minioClient.listObjects(listObjectsArgs);
            if (!objects.iterator().hasNext()) {
                throw new NotFoundException(SCAN_FILE_NOT_FOUND, pathPrefix);
            }
            var filesToScan = new ArrayList<String>();
            for (var result : objects) {
                filesToScan.add(result.get().objectName());
            }

            return filesToScan;
        } catch (MinioException | GeneralSecurityException | IOException e) {
            log.error(e.getLocalizedMessage(), e);
            throw new InternalErrorException(GET_DATA_ERROR, e);
        }
    }

    private String getFileName(FileDto fileDto) {
        return StringUtils.isNotBlank(fileDto.getPreferredName())
            ? fileDto.getPreferredName() + "." + fileDto.getExtension()
            : StringUtils.substringAfterLast(fileDto.getFilePath(), "/");
    }

    public void copyFile(String srcPath, String dstPath) {
        copyFile(srcPath, dstPath, minioClientData.getBucketName());
    }

    public void copyFile(String srcPath, String dstPath, String dstBucket) {
        var srcBucketName = minioClientData.getBucketName();
        copyFile(srcPath, srcBucketName, dstPath, dstBucket);
    }

    public void copyFile(String srcPath, String srcBucket, String dstPath, String dstBucket) {
        try {
            var copySource = CopySource.builder()
                .bucket(srcBucket)
                .object(getRelativeFilePath(srcPath))
                .build();
            var copyObjectArgs = CopyObjectArgs.builder()
                .bucket(dstBucket)
                .object(getRelativeFilePath(dstPath))
                .source(copySource)
                .build();
            minioClient.copyObject(copyObjectArgs);
        } catch (MinioException | GeneralSecurityException | IOException ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw new InternalErrorException(COPY_DATA_ERROR, ex);
        }
    }

    public void moveFile(String srcPath, String dstPath) {
        moveFile(srcPath, dstPath, minioClientData.getBucketName());
    }

    public void moveFile(String srcPath, String dstPath, String dstBucket) {
        copyFile(srcPath, dstPath, dstBucket);
        removeObjectFromBucket(srcPath);
    }

    public String presignedGetObject(String objectName, Map<String, String> reqParams) {
        return getPresignedObject(objectName, reqParams, minioClientData.getExpiryTime(), true);
    }

    public String presignedGetObjectWithOutFileScanCheck(
        String objectName,
        Map<String, String> reqParams,
        int expiryTime) {
        return getPresignedObject(objectName, reqParams, expiryTime, false);
    }

    private String getPresignedObject(
        String objectName,
        Map<String, String> reqParams,
        int expiryTime,
        boolean checkScanStatus) {
        try {
            if (checkScanStatus) {
                checkScanStatus(objectName, getFileStat(objectName).headers());
            }

            var getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(minioClientData.getBucketName())
                .object(getRelativeFilePath(objectName))
                .expiry(expiryTime)
                .extraQueryParams(reqParams)
                .build();
            return minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
        } catch (MinioException | GeneralSecurityException | IOException ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw new InternalErrorException(GET_PRESIGNED_ERROR, ex);
        }
    }

    private String retrieveContentType(InputStream data, String filePath) throws IOException {
        return tika.detect(TikaInputStream.get(data), filePath);
    }

    public List<String> getFilesNamesInBucket() {
        return getFilesNamesInBucket(minioClientData.getBucketName());
    }

    public List<String> getFilesNamesInRootFolder() {
        return getFilesNamesInFolder(minioClientData.getRootDirectory());
    }

    public List<String> getFilesNamesInFolder(String folderName) {
        var prefix = StringUtils.appendIfMissing(folderName, "/");
        var listObjectsArgs = ListObjectsArgs.builder()
            .bucket(minioClientData.getBucketName())
            .prefix(prefix)
            .build();
        return StreamSupport.stream(
                minioClient.listObjects(listObjectsArgs).spliterator(), false)
            .map(this::getItem)
            .filter(Predicate.not(Item::isDir)
                // for some reason root folder has 'isDir' false. so filter out items with 0 size
                .and(item -> item.size() != 0))
            .map(Item::objectName)
            .filter(objectName -> !StringUtils.equals(objectName, folderName))
            .toList();
    }

    public List<String> getFilesNamesInBucket(String bucketName) {
        var listObjectsArgs = ListObjectsArgs.builder()
            .bucket(bucketName)
            .recursive(true)
            .build();
        return StreamSupport.stream(minioClient.listObjects(listObjectsArgs).spliterator(), false)
            .map(this::getItem)
            .map(Item::objectName)
            .toList();
    }

    private Item getItem(Result<Item> result) {
        try {
            return result.get();
        } catch (MinioException | GeneralSecurityException | IOException ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw new InternalErrorException(GET_FILES_NAME_FROM_BUCKET, ex);
        }
    }

    public byte[] getFileFromBucketByFileName(String fileName) {
        try {
            var getObjectArgs = GetObjectArgs.builder()
                .bucket(minioClientData.getBucketName())
                .object(fileName)
                .build();
            return minioClient.getObject(getObjectArgs).readAllBytes();
        } catch (MinioException | GeneralSecurityException | IOException ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw new InternalErrorException(GET_DATA_ERROR, ex);
        }
    }
}
