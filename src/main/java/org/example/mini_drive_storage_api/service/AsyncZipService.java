package org.example.mini_drive_storage_api.service;

import org.example.mini_drive_storage_api.entity.DownloadRequest;
import org.example.mini_drive_storage_api.entity.FileItem;
import org.example.mini_drive_storage_api.entity.enums.DownloadStatus;
import org.example.mini_drive_storage_api.repository.DownloadRequestRepository;
import org.example.mini_drive_storage_api.repository.FileItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class AsyncZipService {

    private final FileItemRepository fileItemRepository;
    private final DownloadRequestRepository downloadRequestRepository;

    @Async // <--- Quan trọng: Chạy method này ở luồng (Thread) khác
    public void processZipFolder(String requestId, Long folderId, Long ownerId) {
        DownloadRequest request = downloadRequestRepository.findById(requestId).orElse(null);
        if (request == null) return;

        try {
            // 1. Cập nhật trạng thái PROCESSING
            request.setStatus(DownloadStatus.PROCESSING);
            downloadRequestRepository.save(request);

            // 2. Tạo file zip tạm
            String zipFileName = "archive-" + requestId + ".zip";
            Path zipPath = Paths.get("uploads").resolve(zipFileName);

            try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 3. Đệ quy thêm file vào zip
                // (Ở đây demo đơn giản: Lấy file cấp 1. Thực tế cần dùng đệ quy nếu folder lồng folder)
                List<FileItem> files = fileItemRepository.findByOwnerIdAndParentIdAndDeletedFalse(ownerId, folderId);

                // Đảm bảo tên entry trong zip là duy nhất để tránh ZipException: duplicate entry
                Set<String> usedNames = new HashSet<>();

                for (FileItem item : files) {
                    try {
                        if (item.getType().toString().equals("FILE")) {
                            String entryName = buildUniqueEntryName(item.getName(), usedNames);
                            addToZip(item, entryName, zos);
                        } else if (item.getType().toString().equals("FOLDER")) {
                            // Đệ quy xử lý các file bên trong folder con
                            zipFolderContents(item.getId(), ownerId, zos, usedNames);
                        }
                    } catch (Exception ex) {
                        // Nếu một file lỗi (mất file vật lý, trùng tên, ...), bỏ qua file đó để không fail toàn bộ job
                        ex.printStackTrace();
                    }
                }
            }

            // 4. Cập nhật trạng thái READY
            request.setStatus(DownloadStatus.READY);
            request.setZipFilePath(zipPath.toString());
            downloadRequestRepository.save(request);

        } catch (Exception e) {
            request.setStatus(DownloadStatus.FAILED);
            downloadRequestRepository.save(request);
            e.printStackTrace();
        }
    }

    // Đệ quy duyệt các file trong folder và các folder con, thêm vào ZIP theo dạng phẳng với tên duy nhất
    private void zipFolderContents(Long folderId, Long ownerId, ZipOutputStream zos, Set<String> usedNames) {
        List<FileItem> children = fileItemRepository.findByOwnerIdAndParentIdAndDeletedFalse(ownerId, folderId);
        for (FileItem child : children) {
            try {
                if (child.getType().toString().equals("FILE")) {
                    String entryName = buildUniqueEntryName(child.getName(), usedNames);
                    addToZip(child, entryName, zos);
                } else if (child.getType().toString().equals("FOLDER")) {
                    zipFolderContents(child.getId(), ownerId, zos, usedNames);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void addToZip(FileItem item, String entryName, ZipOutputStream zos) throws IOException {
        Path path = Paths.get(item.getStoragePath());
        if (!Files.exists(path)) return; // Bỏ qua nếu file vật lý không tồn tại

        // Đảm bảo không có path traversal trong tên file trong zip
        String safeName = entryName.replace("\\", "/").replace("..", "");

        boolean entryOpened = false;
        try (InputStream in = Files.newInputStream(path)) {
            ZipEntry zipEntry = new ZipEntry(safeName);
            zos.putNextEntry(zipEntry);
            entryOpened = true;

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }
        } catch (IOException ioEx) {
            // Ghi nhận lỗi đọc/ghi file đơn lẻ, để service phía ngoài có thể tiếp tục với file khác
            // Không ném ra ngoài để không làm hỏng toàn bộ ZIP
            ioEx.printStackTrace();
        } finally {
            if (entryOpened) {
                try {
                    zos.closeEntry();
                } catch (IOException ignored) {
                    // Nếu closeEntry lỗi, bỏ qua để tiếp tục các entry khác
                }
            }
        }
    }

    // Tạo tên entry duy nhất, nếu trùng thì thêm (1), (2), ... trước phần mở rộng
    private String buildUniqueEntryName(String originalName, Set<String> usedNames) {
        if (originalName == null || originalName.isBlank()) originalName = "file";

        String name = originalName;
        String base = originalName;
        String ext = "";

        int dot = originalName.lastIndexOf('.');
        if (dot > 0 && dot < originalName.length() - 1) {
            base = originalName.substring(0, dot);
            ext = originalName.substring(dot); // gồm dấu chấm
        }

        int counter = 1;
        while (usedNames.contains(name)) {
            name = base + " (" + counter + ")" + ext;
            counter++;
        }
        usedNames.add(name);
        return name;
    }
}