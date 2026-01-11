package org.example.mini_drive_storage_api.exception;

/**
 * Ném khi không tìm thấy User theo tiêu chí truy vấn.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
