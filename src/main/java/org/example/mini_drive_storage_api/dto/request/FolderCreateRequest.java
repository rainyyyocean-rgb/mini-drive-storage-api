package org.example.mini_drive_storage_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FolderCreateRequest {
    @NotBlank(message = "Folder name is required")
    private String name;
    private Long parentId; // Có thể null nếu tạo ở thư mục gốc
    private String type;   // FOLDER (để validate nếu cần)
}