package org.example.mini_drive_storage_api.repository;

import org.example.mini_drive_storage_api.entity.FilePermission;
import org.example.mini_drive_storage_api.entity.FileItem;
import org.example.mini_drive_storage_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface FilePermissionRepository extends JpaRepository<FilePermission, Long> {
    // Kiểm tra xem user có quyền trên file này chưa
    Optional<FilePermission> findByFileAndUser(FileItem file, User user);

    // Tìm tất cả file được chia sẻ cho user này
    List<FilePermission> findByUserId(Long userId);
}
