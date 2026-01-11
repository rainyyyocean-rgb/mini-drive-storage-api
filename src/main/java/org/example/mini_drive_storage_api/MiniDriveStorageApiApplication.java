package org.example.mini_drive_storage_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MiniDriveStorageApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniDriveStorageApiApplication.class, args);
    }

}
