package org.example.mini_drive_storage_api.exception;

/**
 * Lỗi khi khởi tạo hệ thống lưu trữ (tạo thư mục gốc, v.v.).
 */
public class StorageInitializationException extends RuntimeException {
    public StorageInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
