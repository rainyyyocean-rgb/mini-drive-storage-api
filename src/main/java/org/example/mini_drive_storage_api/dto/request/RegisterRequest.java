package org.example.mini_drive_storage_api.dto.request;

import lombok.Data;
@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
}
