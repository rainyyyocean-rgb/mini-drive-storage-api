package org.example.mini_drive_storage_api.controller;

import org.example.mini_drive_storage_api.dto.request.FolderCreateRequest;
import org.example.mini_drive_storage_api.entity.FileItem;
import org.example.mini_drive_storage_api.entity.User;
import org.example.mini_drive_storage_api.service.FileService;
import org.example.mini_drive_storage_api.service.DownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final DownloadService downloadService;

    // --- CASE 1: Upload File (Multipart) - Hỗ trợ 1 hoặc nhiều file trên cùng endpoint ---
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            // Lưu ý: Bắt buộc Client phải gửi key tên là "files"
            @RequestParam(value = "files") List<MultipartFile> files,
            @RequestParam(value = "parentId", required = false) Long parentId) {

        // 1. Validate đầu vào
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("No file(s) provided");
        }

        // 2. Xử lý logic (Stream xử lý từng file trong list)
        List<FileItem> createdFiles = files.stream()
                // Lọc bỏ file rỗng (đề phòng client gửi file 0 byte)
                .filter(f -> !f.isEmpty())
                .map(f -> fileService.uploadFile(f, parentId))
                .collect(Collectors.toList());

        // 3. Trả về danh sách kết quả
        return ResponseEntity.ok(createdFiles);
    }

    // --- CASE 2: Create Folder (JSON) ---
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createFolder(@Valid @RequestBody FolderCreateRequest request) {

        FileItem createdFolder = fileService.createFolder(request.getName(), request.getParentId());
        return ResponseEntity.ok(createdFolder);
    }

    // --- Endpoint Download (Trigger) ---
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadItem(@PathVariable Long id) {
        DownloadService.DownloadTriggerResult result = downloadService.triggerDownload(id);
        if (result.isDirectDownload()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFilename() + "\"")
                    .body(result.getResource());
        }
        return ResponseEntity.ok(Map.of(
                "message", "Folder compression started",
                "requestId", result.getRequestId()
        ));
    }

    // --- Endpoint Polling trạng thái Zip ---
    @GetMapping("/downloads/{requestId}")
    public ResponseEntity<?> checkDownloadStatus(@PathVariable String requestId) {
        return ResponseEntity.ok(downloadService.checkStatusMap(requestId));
    }

    // --- Endpoint Tải file Zip khi đã READY ---
    @GetMapping("/downloads/{requestId}/content")
    public ResponseEntity<?> downloadZipContent(@PathVariable String requestId) {
        Resource resource = downloadService.loadZipResource(requestId);

        // Thêm Content-Type và tên file rõ ràng để client xử lý đúng định dạng ZIP
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"folder-archive.zip\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                .body(resource);
    }

    // 1. API TÌM KIẾM & LIỆT KÊ
    // Endpoint: GET /api/v1/files
    // Query params:
    // - q: từ khóa tìm kiếm tên file (global search khi không có parentId)
    // - type: FILE, FOLDER, hoặc tiền tố MIME (vd: image, application/pdf)
    // - parentId: nếu có -> liệt kê danh sách con trong thư mục; nếu không -> tìm kiếm toàn cục
    // - fromSize, toSize: lọc theo kích thước (bytes)
    @GetMapping
    public ResponseEntity<?> searchFiles(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "parentId", required = false) Long parentId,
            @RequestParam(value = "fromSize", required = false) Long fromSize,
            @RequestParam(value = "toSize", required = false) Long toSize
    ) {
        return ResponseEntity.ok(fileService.search(keyword, type, parentId, fromSize, toSize));
    }

    // 2. API XÓA MỀM (Soft Delete) - Đưa vào thùng rác
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        fileService.softDelete(id);
        return ResponseEntity.ok("File moved to trash bin");
    }

    // 3. API DASHBOARD (Thống kê dung lượng)
    @GetMapping("/analytics/usage")
    public ResponseEntity<?> getStorageUsage() {
        User user = fileService.getCurrentUser();
        long size = fileService.getUsedStorageBytesForCurrentUser();
        return ResponseEntity.ok(java.util.Map.of(
                "userId", user.getId(),
                "usedStorageBytes", size,
                "usedStorageMB", size / (1024.0 * 1024.0) // Quy đổi ra MB
        ));
    }
}
