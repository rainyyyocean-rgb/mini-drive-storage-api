package org.example.mini_drive_storage_api.entity;

import org.example.mini_drive_storage_api.entity.enums.FileType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private FileType type; // FILE hoặc FOLDER

    private String typeMm; // MIME type (vd: image/png, application/pdf) -> null nếu là Folder

    private Long size; // Kích thước (bytes). Folder thì có thể là 0 hoặc tính tổng sau.

    // Đường dẫn vật lý trên ổ cứng (chỉ dùng cho FILE).
    // Folder thì field này null.
    private String storagePath;

    // --- QUAN TRỌNG: Cấu trúc cây ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FileItem parent;
    // Nếu parent == null -> Đây là thư mục gốc (Root)

    // --- Quan hệ với User ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // --- Audit & Soft Delete ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private boolean deleted = false; // Mặc định chưa xóa

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
