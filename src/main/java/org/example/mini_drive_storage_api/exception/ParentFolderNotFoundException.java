package org.example.mini_drive_storage_api.exception;

/**
 * Ném khi không tìm thấy thư mục cha.
 */
public class ParentFolderNotFoundException extends RuntimeException {
    public ParentFolderNotFoundException(String message) {
        super(message);
    }
}
