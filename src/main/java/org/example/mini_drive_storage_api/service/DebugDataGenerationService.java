package org.example.mini_drive_storage_api.service;

import lombok.RequiredArgsConstructor;
import org.example.mini_drive_storage_api.entity.FileItem;
import org.example.mini_drive_storage_api.entity.FilePermission;
import org.example.mini_drive_storage_api.entity.User;
import org.example.mini_drive_storage_api.entity.enums.FileType;
import org.example.mini_drive_storage_api.entity.enums.PermissionLevel;
import org.example.mini_drive_storage_api.repository.FileItemRepository;
import org.example.mini_drive_storage_api.repository.FilePermissionRepository;
import org.example.mini_drive_storage_api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebugDataGenerationService {

    private final UserRepository userRepository;
    private final FileItemRepository fileItemRepository;
    private final FilePermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public static class ResultSummary {
        public int usersCreated;
        public long foldersCreated;
        public long filesCreated;
        public long permissionsCreated;
    }

    @Transactional
    public ResultSummary generateSystemData() {
        ResultSummary summary = new ResultSummary();
        Random random = new Random();

        // 1) Tạo 10 users ngẫu nhiên (nếu đã tồn tại thì bỏ qua)
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String email = "debug_user_" + i + "@example.com";
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                user = User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("Password123!"))
                        .fullName("Debug User " + i)
                        .build();
                userRepository.save(user);
                summary.usersCreated++;
            }
            users.add(user);
        }

        // Chuẩn bị thư mục uploads
        Path uploads = Paths.get("uploads");
        try {
            Files.createDirectories(uploads);
        } catch (Exception ignored) {}

        long totalFilesCreated = 0;
        long totalFoldersCreated = 0;
        List<FileItem> allCreatedFiles = new ArrayList<>(); // chỉ type FILE

        // 2) Mỗi user tạo cấu trúc thư mục/file riêng ~1000 files/user
        // Cấu trúc: 10 root folders -> mỗi root có 10 subfolders -> mỗi subfolder có 10 files => 1000 files
        for (User u : users) {
            // 2.a Tạo 10 thư mục gốc
            List<FileItem> roots = new ArrayList<>();
            for (int r = 1; r <= 10; r++) {
                FileItem root = FileItem.builder()
                        .name("root_" + r)
                        .type(FileType.FOLDER)
                        .owner(u)
                        .build();
                roots.add(root);
            }
            roots = fileItemRepository.saveAll(roots);
            totalFoldersCreated += roots.size();

            // 2.b Với mỗi root, tạo 10 subfolders và trong mỗi subfolder tạo 10 files
            for (FileItem root : roots) {
                List<FileItem> subs = new ArrayList<>();
                for (int s = 1; s <= 10; s++) {
                    FileItem sub = FileItem.builder()
                            .name("folder_" + s)
                            .type(FileType.FOLDER)
                            .owner(u)
                            .parent(root)
                            .build();
                    subs.add(sub);
                }
                subs = fileItemRepository.saveAll(subs);
                totalFoldersCreated += subs.size();

                for (FileItem sub : subs) {
                    List<FileItem> files = new ArrayList<>();
                    for (int f = 1; f <= 10; f++) {
                        // Tạo file vật lý nhỏ (để có storagePath), khoảng 128–2048 bytes
                        int sizeBytes = 128 + random.nextInt(1921);
                        String uniqueName = UUID.randomUUID() + ".txt";
                        Path phys = uploads.resolve(uniqueName);
                        try {
                            byte[] content = ("Debug file for user " + u.getEmail() + ", folder " + sub.getName() + ", index " + f)
                                    .getBytes(StandardCharsets.UTF_8);
                            // lặp để đạt kích thước mong muốn
                            while (content.length < sizeBytes) {
                                content = Arrays.copyOf(content, Math.min(sizeBytes, content.length * 2));
                            }
                            Files.write(phys, Arrays.copyOf(content, sizeBytes));
                        } catch (Exception e) {
                            // Nếu ghi file lỗi, vẫn tiếp tục với storagePath null
                        }

                        FileItem fi = FileItem.builder()
                                .name("file_" + f + ".txt")
                                .type(FileType.FILE)
                                .typeMm("text/plain")
                                .size((long) sizeBytes)
                                .storagePath(phys.toString())
                                .owner(u)
                                .parent(sub)
                                .build();
                        files.add(fi);
                    }
                    files = fileItemRepository.saveAll(files);
                    totalFilesCreated += files.size();
                    allCreatedFiles.addAll(files);
                }
            }
        }

        // 3) Random Sharing: Chọn ngẫu nhiên 10% số lượng file để chia sẻ giữa các user
        long shareCount = Math.max(1, Math.round(allCreatedFiles.size() * 0.1));
        Collections.shuffle(allCreatedFiles, random);
        List<FileItem> filesToShare = allCreatedFiles.stream().limit(shareCount).collect(Collectors.toList());

        long permissionsCreated = 0;
        List<FilePermission> toSavePerms = new ArrayList<>();
        for (FileItem file : filesToShare) {
            // Chọn ngẫu nhiên 1-3 người nhận khác owner
            List<User> candidates = users.stream()
                    .filter(x -> !Objects.equals(x.getId(), file.getOwner().getId()))
                    .collect(Collectors.toList());
            Collections.shuffle(candidates, random);
            int receivers = 1 + random.nextInt(3); // 1..3
            for (int i = 0; i < Math.min(receivers, candidates.size()); i++) {
                User target = candidates.get(i);
                // Kiểm tra trùng để tránh UniqueConstraint lỗi khi chạy nhiều lần
                if (permissionRepository.findByFileAndUser(file, target).isEmpty()) {
                    PermissionLevel level = random.nextBoolean() ? PermissionLevel.VIEW : PermissionLevel.EDIT;
                    FilePermission perm = FilePermission.builder()
                            .file(file)
                            .user(target)
                            .level(level)
                            .build();
                    toSavePerms.add(perm);
                }
            }
        }
        if (!toSavePerms.isEmpty()) {
            permissionRepository.saveAll(toSavePerms);
            permissionsCreated = toSavePerms.size();
        }

        summary.filesCreated = totalFilesCreated;
        summary.foldersCreated = totalFoldersCreated;
        summary.permissionsCreated = permissionsCreated;
        return summary;
    }
}
