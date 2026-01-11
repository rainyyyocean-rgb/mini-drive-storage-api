package org.example.mini_drive_storage_api.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j // Dùng để in log thay vì gửi mail thật
public class EmailService {

    @Async // Gửi mail cũng nên chạy ngầm để không block request
    public void sendShareNotification(String toEmail, String fileName, String ownerName) {
        // Giả lập delay gửi mail 1 giây
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        log.info("📧 [MOCK EMAIL] Sending to: {}", toEmail);
        log.info("   Subject: {} has shared '{}' with you", ownerName, fileName);
        log.info("   Body: Click here to view...");
    }
}