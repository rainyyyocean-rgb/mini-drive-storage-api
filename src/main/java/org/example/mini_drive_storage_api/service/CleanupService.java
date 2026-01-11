package org.example.mini_drive_storage_api.service;

import org.example.mini_drive_storage_api.entity.FileItem;
import org.example.mini_drive_storage_api.repository.FileItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

    private final FileItemRepository fileItemRepository;
    private final ExecutorService cleanupExecutor; // Inject executor đã tạo ở trên

    // Cron expression: Giây Phút Giờ Ngày Tháng Thứ
    // "0 0 2 * * ?" => Chạy lúc 02:00:00 AM mỗi ngày
    @Scheduled(cron = "0 0 2 * * ?")
    // @Scheduled(fixedDelay = 60000) // Dùng dòng này nếu muốn test ngay (chạy mỗi phút 1 lần)
    public void performCleanup() {
        log.info("🧹 Starting daily cleanup task...");

        // 1. Tìm file đã xóa quá 30 ngày
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        // LocalDateTime threshold = LocalDateTime.now().minusMinutes(1); // Để test nhanh

        List<FileItem> trashFiles = fileItemRepository.findFilesReadyForPermanentDelete(threshold);

        if (trashFiles.isEmpty()) {
            log.info("No files to clean.");
            return;
        }

        log.info("Found {} files to delete permanently.", trashFiles.size());

        // 2. Sử dụng Multi-threading để xóa file vật lý
        for (FileItem file : trashFiles) {
            cleanupExecutor.submit(() -> {
                try {
                    deletePhysicalFile(file);
                    // Sau khi xóa vật lý xong mới xóa trong DB
                    fileItemRepository.delete(file);
                    log.info("Deleted file ID: {}", file.getId());
                } catch (Exception e) {
                    log.error("Failed to delete file ID: {}", file.getId(), e);
                }
            });
        }
    }

    private void deletePhysicalFile(FileItem file) {
        if (file.getStoragePath() != null) {
            try {
                Path path = Paths.get(file.getStoragePath());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.error("Could not delete physical file: " + file.getStoragePath());
            }
        }
    }
}