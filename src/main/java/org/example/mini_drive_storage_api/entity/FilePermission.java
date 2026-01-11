package org.example.mini_drive_storage_api.entity;

import org.example.mini_drive_storage_api.entity.enums.PermissionLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"file_id", "user_id"}) // 1 User chỉ có 1 quyền trên 1 file
})
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class FilePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileItem file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Người ĐƯỢC chia sẻ

    @Enumerated(EnumType.STRING)
    private PermissionLevel level; // VIEW hoặc EDIT
}