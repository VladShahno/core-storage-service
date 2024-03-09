package com.demo.awsstorage.repository;

import com.demo.awsstorage.model.NodeType;
import com.demo.awsstorage.model.ResourceMetadata;
import com.demo.awsstorage.model.UploadStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FileMetadataRepository extends JpaRepository<ResourceMetadata, String> {

    @Modifying
    @Query("Update ResourceMetadata f SET f.scanStatus=:status, f.updatedOn=:updatedOn WHERE f.resourceId=:fileId")
    void updateScanStatus(String fileId, String status, OffsetDateTime updatedOn);

    default void updateScanStatus(String fileId, String status) {
        this.updateScanStatus(fileId, status, OffsetDateTime.now());
    }

    @Transactional
    @Modifying
    @Query("Update ResourceMetadata f SET f.uploadStatus=:status, f.updatedOn=:updatedOn WHERE f.resourceId=:fileId")
    void updateUploadStatus(String fileId, UploadStatus status, OffsetDateTime updatedOn);

    default void updateUploadStatus(String fileId, UploadStatus status) {
        this.updateUploadStatus(fileId, status, OffsetDateTime.now());
    }

    Optional<ResourceMetadata> findByResourceIdAndNodeType(String resourceId, NodeType nodeType);

    List<ResourceMetadata> findByResourceIdInAndNodeType(List<String> resourceId,
        NodeType nodeType);
}
