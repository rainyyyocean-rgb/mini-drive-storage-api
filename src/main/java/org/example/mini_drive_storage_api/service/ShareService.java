package org.example.mini_drive_storage_api.service;

import org.example.mini_drive_storage_api.entity.*;
import org.example.mini_drive_storage_api.entity.enums.PermissionLevel;
import org.example.mini_drive_storage_api.repository.*;
import org.example.mini_drive_storage_api.dto.response.SharedWithMeItemResponse;
import org.example.mini_drive_storage_api.exception.FileItemNotFoundException;
import org.example.mini_drive_storage_api.exception.SharePermissionException;
import org.example.mini_drive_storage_api.exception.SelfShareNotAllowedException;
import org.example.mini_drive_storage_api.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final FileItemRepository fileItemRepository;
    private final UserRepository userRepository;
    private final FilePermissionRepository permissionRepository;
    private final EmailService emailService;

    @Transactional
    public void shareFile(Long fileId, String ownerEmail, String targetEmail, PermissionLevel level) {
        // 1. Validate
        FileItem file = fileItemRepository.findById(fileId)
                .orElseThrow(() -> new FileItemNotFoundException("File not found"));

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + ownerEmail));
        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new SharePermissionException("Only owner can share this file");
        }

        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new UserNotFoundException("Target user not found with email: " + targetEmail));

        if (owner.getId().equals(targetUser.getId())) {
            throw new SelfShareNotAllowedException("Cannot share with yourself");
        }

        // 2. Thực hiện chia sẻ (Gọi hàm đệ quy)
        applyPermissionRecursive(file, targetUser, level);

        // 3. Gửi mail thông báo
        emailService.sendShareNotification(targetEmail, file.getName(), owner.getFullName());
    }

    // Hàm đệ quy: Gán quyền cho file hiện tại và tất cả con cháu của nó
    private void applyPermissionRecursive(FileItem currentItem, User targetUser, PermissionLevel level) {
        // Lưu hoặc cập nhật quyền cho item hiện tại
        FilePermission permission = permissionRepository.findByFileAndUser(currentItem, targetUser)
                .orElse(FilePermission.builder()
                        .file(currentItem)
                        .user(targetUser)
                        .build());

        permission.setLevel(level);
        permissionRepository.save(permission);

        // Nếu là Folder, tìm các con và gọi tiếp
        // (Lưu ý: Cách này query hơi nhiều, với dự án nhỏ thì OK.
        // Dự án lớn dùng Bulk Insert hoặc Logic check parent ở query)
        if (currentItem.getType().toString().equals("FOLDER")) {
            List<FileItem> children = fileItemRepository.findByOwnerIdAndParentIdAndDeletedFalse(
                    currentItem.getOwner().getId(),
                    currentItem.getId()
            );
            for (FileItem child : children) {
                applyPermissionRecursive(child, targetUser, level);
            }
        }
    }

    public List<SharedWithMeItemResponse> getSharedWithMe(String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + currentUserEmail));

        List<FilePermission> permissions = permissionRepository.findByUserId(currentUser.getId());

        return permissions.stream().map(fp -> {
            FileItem file = fp.getFile();
            User owner = file.getOwner();
            return SharedWithMeItemResponse.builder()
                    .fileId(file.getId())
                    .fileName(file.getName())
                    .fileType(file.getType() != null ? file.getType().name() : null)
                    .ownerEmail(owner != null ? owner.getEmail() : null)
                    .ownerName(owner != null ? owner.getFullName() : null)
                    .permissionLevel(fp.getLevel().name())
                    .build();
        }).collect(Collectors.toList());
    }
}
