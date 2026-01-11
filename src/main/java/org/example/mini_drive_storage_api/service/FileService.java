package org.example.mini_drive_storage_api.service;

import org.example.mini_drive_storage_api.entity.FileItem;
import org.example.mini_drive_storage_api.entity.User;
import org.example.mini_drive_storage_api.entity.enums.FileType;
import org.example.mini_drive_storage_api.repository.FileItemRepository;
import org.example.mini_drive_storage_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileItemRepository fileItemRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // Helper: Lấy User đang đăng nhập từ Token
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new org.example.mini_drive_storage_api.exception.UserNotFoundException("User not found"));
    }

    // --- 1. Tạo Thư mục ---
    public FileItem createFolder(String folderName, Long parentId) {
        User user = getCurrentUser();

        FileItem parent = null;
        if (parentId != null) {
            parent = fileItemRepository.findById(parentId)
                    .orElseThrow(() -> new org.example.mini_drive_storage_api.exception.ParentFolderNotFoundException("Parent folder not found"));
            // Check: parent phải thuộc về current user
            if (parent.getOwner() == null || parent.getOwner().getId() == null ||
                    !parent.getOwner().getId().equals(user.getId())) {
                throw new org.example.mini_drive_storage_api.exception.SharePermissionException("You are not the owner of the parent folder");
            }
        }

        FileItem folder = FileItem.builder()
                .name(folderName)
                .type(FileType.FOLDER)
                .owner(user)
                .parent(parent)
                .build();

        return fileItemRepository.save(folder);
    }

    // --- 2. Upload File ---
    @Transactional // Nếu lưu DB lỗi thì rollback, không lưu rác
    public FileItem uploadFile(MultipartFile file, Long parentId) {
        User user = getCurrentUser();

        FileItem parent = null;
        if (parentId != null) {
            parent = fileItemRepository.findById(parentId)
                    .orElseThrow(() -> new org.example.mini_drive_storage_api.exception.ParentFolderNotFoundException("Parent folder not found"));
        }

        // 1. Lưu vật lý
        String storagePath = fileStorageService.storeFile(file);

        // 2. Lưu Metadata vào DB
        FileItem fileItem = FileItem.builder()
                .name(file.getOriginalFilename())
                .type(FileType.FILE)
                .typeMm(file.getContentType())
                .size(file.getSize())
                .storagePath(storagePath)
                .owner(user)
                .parent(parent)
                .build();

        return fileItemRepository.save(fileItem);
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = Paths.get("uploads").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new org.example.mini_drive_storage_api.exception.FileItemNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new org.example.mini_drive_storage_api.exception.FileStorageException("File not found " + fileName, e);
        }
    }

    public FileItem getFileById(Long id) {
        return fileItemRepository.findById(id)
                .orElseThrow(() -> new org.example.mini_drive_storage_api.exception.FileItemNotFoundException("File not found"));
    }

    // --- Search logic di chuyển từ Controller về Service ---
    public List<FileItem> search(String keyword, String type, Long parentId, Long fromSize, Long toSize) {
        User user = getCurrentUser();

        String fileTypeStr = null;
        String mimePattern = null;
        if (type != null && !type.isBlank()) {
            String t = type.trim();
            if (t.equalsIgnoreCase("FILE")) {
                fileTypeStr = "FILE";
            } else if (t.equalsIgnoreCase("FOLDER")) {
                fileTypeStr = "FOLDER";
            } else {
                mimePattern = t + "%";
            }
        }

        if (parentId != null) {
            return fileItemRepository.listChildren(user.getId(), parentId, fileTypeStr, mimePattern, fromSize, toSize);
        }
        String kw = (keyword == null || keyword.isBlank()) ? null : ("%" + keyword.toLowerCase() + "%");
        return fileItemRepository.searchGlobal(user.getId(), kw, fileTypeStr, mimePattern, fromSize, toSize);
    }

    // --- Soft delete với kiểm tra quyền ---
    @Transactional
    public void softDelete(Long fileId) {
        User user = getCurrentUser();
        FileItem file = getFileById(fileId);
        if (!file.getOwner().getId().equals(user.getId())) {
            throw new org.example.mini_drive_storage_api.exception.SharePermissionException("Only owner can delete");
        }
        file.setDeleted(true);
        fileItemRepository.save(file);
    }

    // --- Analytics: tổng dung lượng đã dùng của current user ---
    @Transactional(readOnly = true)
    public long getUsedStorageBytesForCurrentUser() {
        User user = getCurrentUser();
        Long usedBytes = fileItemRepository.sumSizeByUserId(user.getId());
        return usedBytes != null ? usedBytes : 0L;
    }
}