package org.example.mini_drive_storage_api.exception;

/**
 * Ném khi người dùng cố gắng chia sẻ tài nguyên cho chính mình.
 */
public class SelfShareNotAllowedException extends RuntimeException {
    public SelfShareNotAllowedException(String message) {
        super(message);
    }
}
