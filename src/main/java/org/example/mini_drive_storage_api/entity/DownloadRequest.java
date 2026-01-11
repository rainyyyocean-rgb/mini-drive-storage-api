package org.example.mini_drive_storage_api.entity;


import org.example.mini_drive_storage_api.entity.enums.DownloadStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "download_requests")
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class DownloadRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // Dùng UUID cho khó đoán

    private Long userId;

    @Enumerated(EnumType.STRING)
    private DownloadStatus status;

    private String zipFilePath; // Đường dẫn file zip sau khi nén xong

    private LocalDateTime createdAt;
}