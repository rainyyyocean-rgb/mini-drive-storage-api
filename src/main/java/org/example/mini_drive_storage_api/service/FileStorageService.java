package org.example.mini_drive_storage_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.example.mini_drive_storage_api.exception.FileStorageException;
import org.example.mini_drive_storage_api.exception.StorageInitializationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootLocation;

    // Constructor: Tạo thư mục uploads nếu chưa có
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageInitializationException("Could not initialize storage", e);
        }
    }

    public String storeFile(MultipartFile file) {
        try {
            // Tạo tên file ngẫu nhiên để không bị trùng (UUID)
            // Ví dụ: bao-cao.pdf -> a1b2c3d4-bao-cao.pdf
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Copy file vào thư mục đích
            Path destinationFile = this.rootLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            return destinationFile.toString(); // Trả về đường dẫn để lưu vào DB
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }
    }
}
