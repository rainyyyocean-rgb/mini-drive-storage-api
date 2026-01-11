package org.example.mini_drive_storage_api.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.mini_drive_storage_api.entity.DownloadRequest;
import org.example.mini_drive_storage_api.entity.FileItem;
import org.example.mini_drive_storage_api.entity.User;
import org.example.mini_drive_storage_api.entity.enums.DownloadStatus;
import org.example.mini_drive_storage_api.entity.enums.FileType;
import org.example.mini_drive_storage_api.exception.RequestNotFoundException;
import org.example.mini_drive_storage_api.repository.DownloadRequestRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DownloadRequestRepository downloadRequestRepository;
    private final AsyncZipService asyncZipService;
    private final FileService fileService;

    @Data
    @Builder
    @AllArgsConstructor
    public static class DownloadTriggerResult {
        private boolean directDownload;
        private Resource resource;
        private String filename;
        private String requestId;
    }

    public DownloadTriggerResult triggerDownload(Long itemId) {
        FileItem item = fileService.getFileById(itemId);
        User currentUser = fileService.getCurrentUser();

        if (item.getType() == FileType.FILE) {
            // Trả trực tiếp file
            String storedName = Paths.get(item.getStoragePath()).getFileName().toString();
            Resource resource = fileService.loadFileAsResource(storedName);
            return DownloadTriggerResult.builder()
                    .directDownload(true)
                    .resource(resource)
                    .filename(item.getName())
                    .build();
        }

        // Folder: tạo request và chạy nén async
        DownloadRequest request = DownloadRequest.builder()
                .userId(currentUser.getId())
                .status(DownloadStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        downloadRequestRepository.save(request);
        asyncZipService.processZipFolder(request.getId(), item.getId(), currentUser.getId());

        return DownloadTriggerResult.builder()
                .directDownload(false)
                .requestId(request.getId())
                .build();
    }

    public Map<String, Object> checkStatusMap(String requestId) {
        DownloadRequest request = downloadRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found"));
        if (request.getStatus() == DownloadStatus.READY) {
            return Map.of(
                    "status", "READY",
                    "downloadUrl", "/api/v1/files/downloads/" + requestId + "/content"
            );
        }
        return Map.of("status", request.getStatus());
    }

    public Resource loadZipResource(String requestId) {
        DownloadRequest request = downloadRequestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found"));
        if (request.getStatus() != DownloadStatus.READY) {
            throw new IllegalStateException("File not ready yet");
        }
        Path path = Paths.get(request.getZipFilePath());
        return new FileSystemResource(path);
    }
}
