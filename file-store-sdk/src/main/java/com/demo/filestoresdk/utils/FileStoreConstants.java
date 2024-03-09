package com.demo.filestoresdk.utils;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileStoreConstants {
  @UtilityClass
  public static class FileStatus {
    public static final String NO_FILE = "NO_FILE";
    public static final String IN_SCANNING = "IN_SCANNING";
    public static final String CLEAN = "CLEAN";
    public static final String INFECTED = "INFECTED";
    public static final String SCAN_FAILED = "SCAN_FAILED";
    public static final String SCANNING_QUEUED = "SCANNING_QUEUED";
    public static final String NOT_ASSIGNABLE = "NOT_ASSIGNABLE";

    public static final List<String> PROCESSED_FILE_STATUSES = List.of(CLEAN, INFECTED);
    public static final List<String> NOT_PROCESSED_FILE_STATUSES =
        List.of(NO_FILE, IN_SCANNING, SCAN_FAILED, SCANNING_QUEUED, NOT_ASSIGNABLE);
  }

  @UtilityClass
  public static class FileMetadataHeaders {
    public static final String AV_STATUS = "x-amz-meta-av-status";
    public static final String AV_TIMESTAMP = "x-amz-meta-av-timestamp";
  }

  @UtilityClass
  public static class AWSCodes {
    public static final String NO_SUCH_KEY = "NoSuchKey";
    public static final String SLOW_DOWN = "SlowDown";
  }

  @UtilityClass
  public static class ErrorCodes {
    public static final String SAVE_ERROR_CODE = "binary.data.save.error";
    public static final String GET_METADATA_ERROR = "binary.data.get.metadata.error";
    public static final String GET_DATA_ERROR = "binary.data.get.error";
    public static final String COPY_DATA_ERROR = "binary.data.copy.error";
    public static final String REMOVE_ERROR = "binary.data.remove.error";
    public static final String STATUS_ABSENT_ERROR = "binary.data.get.metadata.status.absent";
    public static final String STATUS_INFECTED_ERROR = "binary.data.get.metadata.status.infected";
    public static final String GET_PRESIGNED_ERROR = "binary.data.get.presigned.object.error";
    public static final String SCAN_FILE_NOT_FOUND = "scan.file.not.found";
    public static final String GET_FILES_NAME_FROM_BUCKET = "binary.data.get.files.name.error";
    public static final String SAVE_TAGS_ERROR = "binary.data.save.file.tags.error";
    public static final String GET_TAGS_ERROR = "binary.data.get.file.tags.error";
  }
}
