package org.example.mini_drive_storage_api.repository;

import org.example.mini_drive_storage_api.entity.FileItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FileItemRepository extends JpaRepository<FileItem, Long> {

    // Tìm file trong folder cha, của user đó, và chưa bị xóa
    List<FileItem> findByOwnerIdAndParentIdAndDeletedFalse(Long ownerId, Long parentId);

    // Tìm file ở thư mục gốc (parentId is null)
    List<FileItem> findByOwnerIdAndParentIsNullAndDeletedFalse(Long ownerId);

    // Tìm file theo tên (Search)
    List<FileItem> findByOwnerIdAndNameContainingIgnoreCaseAndDeletedFalse(Long ownerId, String name);

    // 1. TÌM KIẾM (Search): Tìm trong cả file của mình VÀ file được share
    // Logic: (Chủ sở hữu là tôi HOẶC tôi có quyền trong bảng permissions) VÀ Tên khớp từ khóa VÀ Chưa xóa
    @Query("SELECT DISTINCT f FROM FileItem f " +
            "LEFT JOIN FilePermission p ON f.id = p.file.id " +
            "WHERE (f.owner.id = :userId OR p.user.id = :userId) " +
            "AND f.deleted = false " +
            "AND (:keywordPattern IS NULL OR LOWER(f.name) LIKE :keywordPattern)")
    List<FileItem> searchByNameAndUser(@Param("userId") Long userId, @Param("keywordPattern") String keywordPattern);

    // 1b. TÌM KIẾM TOÀN CỤC VỚI BỘ LỌC MỞ RỘNG (My Drive + Shared with me) - dùng native SQL để kiểm soát CAST


    @Query(value = """
            SELECT DISTINCT f.* FROM files f
            LEFT JOIN file_permissions fp ON f.id = fp.file_id
            WHERE f.is_deleted = false
            AND (f.owner_id = :userId OR fp.user_id = :userId)
            AND (:keyword IS NULL OR LOWER(f.name) LIKE CAST(:keyword AS TEXT))
            AND (:fileType IS NULL OR f.type = :fileType)
            AND (:mimePattern IS NULL OR f.type_mm LIKE CAST(:mimePattern AS TEXT))
            AND (:fromSize IS NULL OR f.size >= :fromSize)
            AND (:toSize IS NULL OR f.size <= :toSize)
            ORDER BY f.updated_at DESC
            """, nativeQuery = true)
    List<FileItem> searchGlobal(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("fileType") String fileType,  // Changed from FileType to String
            @Param("mimePattern") String mimePattern,
            @Param("fromSize") Long fromSize,
            @Param("toSize") Long toSize
    );
    // 1c. LIỆT KÊ DANH SÁCH CON THEO parentId (My Drive + Shared with me) với các bộ lọc tùy chọn

    @Query(value = """
            SELECT DISTINCT f.* FROM files f
            LEFT JOIN file_permissions fp ON f.id = fp.file_id
            WHERE (f.owner_id = :userId OR fp.user_id = :userId)
            AND f.parent_id = :parentId
            AND f.is_deleted = false
            AND (:fileType IS NULL OR f.type = :fileType)
            AND (:mimePattern IS NULL OR f.type_mm LIKE CAST(:mimePattern AS TEXT))
            AND (:fromSize IS NULL OR f.size >= :fromSize)
            AND (:toSize IS NULL OR f.size <= :toSize)
            ORDER BY f.type DESC, f.name ASC
            """, nativeQuery = true)
    List<FileItem> listChildren(
            @Param("userId") Long userId,
            @Param("parentId") Long parentId,
            @Param("fileType") String fileType,  // 'FILE' or 'FOLDER'
            @Param("mimePattern") String mimePattern,
            @Param("fromSize") Long fromSize,
            @Param("toSize") Long toSize
    );

    // 2. THỐNG KÊ (Analytics): Tính tổng dung lượng file của user
    @Query("SELECT SUM(f.size) FROM FileItem f WHERE f.owner.id = :userId AND f.deleted = false")
    Long sumSizeByUserId(@Param("userId") Long userId);

    // 3. DỌN DẸP (Cleanup): Tìm file đã xóa mềm quá hạn (ví dụ: quá 30 ngày)
    @Query("SELECT f FROM FileItem f WHERE f.deleted = true AND f.updatedAt < :thresholdDate")
    List<FileItem> findFilesReadyForPermanentDelete(@Param("thresholdDate") LocalDateTime thresholdDate);
}