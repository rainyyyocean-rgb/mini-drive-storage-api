package org.example.mini_drive_storage_api.exception;

/**
 * Ném khi người dùng không có quyền thực hiện hành động chia sẻ.
 */
public class SharePermissionException extends RuntimeException {
    public SharePermissionException(String message) {
        super(message);
    }
}
