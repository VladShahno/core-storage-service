package com.demo.awsstorage.model;


import static com.demo.awsstorage.model.UploadStatus.PENDING;
import static com.demo.filestoresdk.utils.FileStoreConstants.FileStatus.NO_FILE;

import com.demo.awsstorage.dto.FileMetadataDto;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@Table(name = "resource_metadata")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceMetadata implements Serializable {

    @Id
    String resourceId;
    String sourceId;
    String name;
    String storageId;
    String hash;
    String contentType;
    long sizeInBytes;
    long folderHeight;
    long fileCount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    NodeType nodeType = NodeType.FILE;

    @CreationTimestamp
    OffsetDateTime createdOn;

    @UpdateTimestamp
    OffsetDateTime updatedOn;

    @Builder.Default
    String scanStatus = NO_FILE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    UploadStatus uploadStatus = PENDING;


    public static ResourceMetadataBuilder builder() {
        return new ResourceMetadataBuilder();
    }

    public static ResourceMetadataBuilder builder(FileMetadataDto fileMetadataDto) {
        ResourceMetadataBuilder builder = new ResourceMetadataBuilder();
        return builder.resourceId(fileMetadataDto.getResourceId())
            .sourceId(fileMetadataDto.getSourceId())
            .name(fileMetadataDto.getName())
            .storageId(fileMetadataDto.getStorageId())
            .hash(fileMetadataDto.getHash())
            .sizeInBytes(fileMetadataDto.getSizeInBytes())
            .nodeType(NodeType.FILE);
    }
}
