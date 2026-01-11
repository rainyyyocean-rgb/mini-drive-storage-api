package org.example.mini_drive_storage_api.dto.request;

import org.example.mini_drive_storage_api.entity.enums.PermissionLevel;
import lombok.Data;

@Data
public class ShareRequest {
    private String email; // Email người nhận
    private PermissionLevel permission; // VIEW hoặc EDIT
}
