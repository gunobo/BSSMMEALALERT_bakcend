package com.bssm.meal.admin.controller;

import com.bssm.meal.admin.service.AdminNotificationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/app")
@RequiredArgsConstructor
public class AdminAppController {

    private final AdminNotificationService adminNotificationService;

    /**
     * [관리자] 앱 설치 파일 업로드 API
     * POST /admin/app/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAppFile(
            @RequestParam("type") String type,
            @RequestParam("file") MultipartFile file) {

        try {
            // Service에서 구현한 업로드 로직 호출 (기존 로직)
            String savedPath = adminNotificationService.uploadAppFile(type, file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", type.toUpperCase() + " 업로드 완료",
                    "path", savedPath
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류 발생"));
        }
    }

    /**
     * [사용자] 앱 다운로드 API (통계 카운트 포함)
     * GET /admin/app/download/{type}
     * 사용자가 이 링크를 클릭하면 DB 카운트가 올라가고 실제 파일로 리다이렉트됩니다.
     */
    @GetMapping("/download/{type}")
    public void downloadApp(@PathVariable String type, HttpServletResponse response) throws IOException {
        // Service에서 통계 카운트 증가 및 파일 경로 획득
        String fileUrl = adminNotificationService.getDownloadUrlAndCount(type);

        // 실제 파일 경로(/uploads/...)로 리다이렉트
        response.sendRedirect(fileUrl);
    }

    /**
     * [관리자] 앱 다운로드 통계 조회 API
     * GET /admin/app/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getDownloadStats() {
        try {
            return ResponseEntity.ok(adminNotificationService.getAllDownloadStats());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "통계 조회 중 오류 발생"));
        }
    }
}