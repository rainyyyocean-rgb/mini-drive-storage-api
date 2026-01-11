package org.example.mini_drive_storage_api.exception;

/**
 * Ném khi không tìm thấy Download/Request liên quan.
 */
public class RequestNotFoundException extends RuntimeException {
    public RequestNotFoundException(String message) {
        super(message);
    }
}
