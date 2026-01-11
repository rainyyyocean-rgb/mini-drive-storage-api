package org.example.mini_drive_storage_api.controller;

import lombok.RequiredArgsConstructor;
import org.example.mini_drive_storage_api.service.DebugDataGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
public class DebugController {

    private final DebugDataGenerationService generator;

    // Endpoint: POST /api/v1/debug/generate-system
    @PostMapping("/generate-system")
    public ResponseEntity<?> generateSystem() {
        DebugDataGenerationService.ResultSummary rs = generator.generateSystemData();

        Map<String, Object> body = new HashMap<>();
        body.put("usersCreated", rs.usersCreated);
        body.put("foldersCreated", rs.foldersCreated);
        body.put("filesCreated", rs.filesCreated);
        body.put("permissionsCreated", rs.permissionsCreated);
        return ResponseEntity.ok(body);
    }
}
