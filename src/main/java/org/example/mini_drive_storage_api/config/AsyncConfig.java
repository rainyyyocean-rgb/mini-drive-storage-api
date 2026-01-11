package org.example.mini_drive_storage_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling // Bật tính năng hẹn giờ
@EnableAsync      // Bật tính năng bất đồng bộ
public class AsyncConfig {

    // Tạo một hồ chứa thread để xử lý việc xóa file
    @Bean(name = "cleanupExecutor")
    public ExecutorService cleanupExecutor() {
        // Tạo pool có 5 luồng làm việc cùng lúc
        return Executors.newFixedThreadPool(5);
    }
}