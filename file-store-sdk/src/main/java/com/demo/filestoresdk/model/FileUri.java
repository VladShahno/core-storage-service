package com.demo.filestoresdk.model;

import static com.demo.filestoresdk.utils.FileTools.buildUri;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultString;

import com.demo.filestoresdk.utils.FileTools;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUri {

    private String orgId;
    private String folderName;
    private String fileName;
    private String fullPackageName;
    private String extension;

    public String buildFilePath() {
        if (StringUtils.isNoneBlank(extension) && !StringUtils.startsWith(extension, ".")) {
            extension = "." + extension;
        }
        return buildUri(constructFolderName(), constructFileName());
    }

    private String constructFolderName() {
        return ofNullable(folderName)
            .filter(StringUtils::isNotBlank)
            .orElseGet(() -> FileTools.getFolderName(orgId));
    }

    private String constructFileName() {
        return defaultIfBlank(fileName, UUID.randomUUID().toString()) + defaultString(extension);
    }
}
