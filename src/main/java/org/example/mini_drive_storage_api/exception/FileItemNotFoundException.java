package org.example.mini_drive_storage_api.exception;

/**
 * Ném khi không tìm thấy FileItem (file hoặc thư mục).
 */
public class FileItemNotFoundException extends RuntimeException {
    public FileItemNotFoundException(String message) {
        super(message);
    }
}
