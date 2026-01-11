package org.example.mini_drive_storage_api.dto.request;

import lombok.Data;
@Data
public class LoginRequest {
    private String email;
    private String password;
}
