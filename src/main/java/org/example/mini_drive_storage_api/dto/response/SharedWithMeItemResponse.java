package org.example.mini_drive_storage_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedWithMeItemResponse {
    private Long fileId;
    private String fileName;
    private String fileType;
    private String ownerEmail;
    private String ownerName;
    private String permissionLevel;
}
