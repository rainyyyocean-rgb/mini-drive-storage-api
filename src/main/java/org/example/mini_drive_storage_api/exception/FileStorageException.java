package org.example.mini_drive_storage_api.exception;

/**
 * Lỗi khi lưu trữ (ghi/đọc) file.
 */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
