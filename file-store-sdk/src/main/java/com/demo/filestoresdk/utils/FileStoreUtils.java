package com.demo.filestoresdk.utils;

import static com.demo.filestoresdk.utils.FileStoreConstants.FileStatus.PROCESSED_FILE_STATUSES;
import static com.demo.filestoresdk.utils.FileStoreConstants.FileStatus.SCANNING_QUEUED;

import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileStoreUtils {

    public static boolean isProcessedStatus(String status) {
        return Objects.nonNull(status) && PROCESSED_FILE_STATUSES.contains(status);
    }

    public static boolean isFileStatusChangeAllowed(String currentStatus, String newStatus) {
        return Objects.isNull(currentStatus) || !isProcessedStatus(currentStatus)
            || SCANNING_QUEUED.equals(newStatus);
    }
}
