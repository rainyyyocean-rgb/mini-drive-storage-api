package org.example.mini_drive_storage_api.controller;

import org.example.mini_drive_storage_api.dto.request.ShareRequest;
import org.example.mini_drive_storage_api.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    // API Chia sẻ file
    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareFile(@PathVariable Long id, @RequestBody ShareRequest request) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        shareService.shareFile(id, currentUserEmail, request.getEmail(), request.getPermission());

        return ResponseEntity.ok("Shared successfully");
    }

    // API Xem danh sách "Shared with me"
    @GetMapping("/shared-with-me")
    public ResponseEntity<?> getSharedWithMe() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(shareService.getSharedWithMe(currentUserEmail));
    }
}
