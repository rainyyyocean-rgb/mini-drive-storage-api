package org.example.mini_drive_storage_api.repository;

import org.example.mini_drive_storage_api.entity.DownloadRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadRequestRepository extends JpaRepository<DownloadRequest, String> {}