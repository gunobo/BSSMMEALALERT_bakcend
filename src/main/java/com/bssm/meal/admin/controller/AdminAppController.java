package com.bssm.meal.admin.controller;

import com.bssm.meal.admin.service.AdminNotificationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/app")
@RequiredArgsConstructor
public class AdminAppController {

    private final AdminNotificationService adminNotificationService;

    /**
     * [관리자] 앱 설치 파일 업로드 API
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAppFile(
            @RequestParam("type") String type,
            @RequestParam("file") MultipartFile file) {

        log.info("🚀 업로드 요청 유입 - 타입: {}, 파일명: {}, 크기: {} bytes",
                type, file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "파일이 없습니다."));
        }

        try {
            String savedPath = adminNotificationService.uploadAppFile(type, file);
            log.info("✅ 업로드 완료 - 저장 경로: {}", savedPath);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", type.toUpperCase() + " 업로드 완료",
                    "path", savedPath
            ));
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ 서버 오류 발생: ", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "서버 오류: " + e.getMessage()));
        }
    }

    /**
     * ✅ [사용자] 앱 다운로드 API (직접 스트리밍 방식)
     * Mixed Content 문제 해결: 리다이렉트 없이 직접 파일 전송
     */
    @GetMapping("/download/{type}")
    public void downloadApp(
            @PathVariable String type,
            HttpServletResponse response) throws IOException {

        log.info("📥 다운로드 요청 - 타입: {}", type);

        try {
            // 1. 서비스에서 파일 경로 및 다운로드 카운트 증가
            String fileUrl = adminNotificationService.getDownloadUrlAndCount(type);
            log.info("📂 파일 URL: {}", fileUrl);

            // 2. 실제 파일 경로 추출 (URL 또는 로컬 경로)
            File file = getFileFromUrl(fileUrl);

            if (!file.exists() || !file.isFile()) {
                log.error("❌ 파일이 존재하지 않음: {}", file.getAbsolutePath());
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "파일을 찾을 수 없습니다.");
                return;
            }

            log.info("✅ 파일 확인 완료 - 크기: {} bytes, 경로: {}",
                    file.length(), file.getAbsolutePath());

            // 3. 파일 타입에 따른 Content-Type 설정
            String contentType;
            String fileName;

            if ("apk".equalsIgnoreCase(type)) {
                contentType = "application/vnd.android.package-archive";
                fileName = "BSSM_Meal_Latest.apk";
            } else if ("ipa".equalsIgnoreCase(type)) {
                contentType = "application/octet-stream";
                fileName = "BSSM_Meal_Latest.ipa";
            } else {
                contentType = "application/octet-stream";
                fileName = file.getName();
            }

            // 4. 응답 헤더 설정
            response.setContentType(contentType);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + "\"");
            response.setContentLengthLong(file.length());

            // ✅ HTTPS 보안 헤더 추가
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

            log.info("📤 파일 전송 시작 - 이름: {}, 크기: {} bytes", fileName, file.length());

            // 5. 파일 스트리밍 (메모리 효율적)
            try (InputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[8192]; // 8KB 버퍼
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                outputStream.flush();

                log.info("✅✅✅ 파일 전송 완료 - {} bytes 전송됨", totalBytesRead);
            }

        } catch (FileNotFoundException e) {
            log.error("❌ 파일을 찾을 수 없음: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "파일을 찾을 수 없습니다.");
        } catch (IOException e) {
            log.error("❌ 파일 전송 중 오류 발생: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "파일 전송 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("❌ 다운로드 처리 중 예외 발생: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "다운로드 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * ✅ URL 또는 경로에서 실제 파일 객체 추출
     */
    private File getFileFromUrl(String fileUrl) {
        // URL 형식인 경우 (예: http://api.imjemin.co.kr/uploads/apk/file.apk)
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            try {
                // URL에서 경로 부분만 추출
                java.net.URI uri = java.net.URI.create(fileUrl);
                String path = uri.getPath(); // /uploads/apk/file.apk

                // 실제 파일 시스템 경로로 변환
                // 예: /var/www/uploads/apk/file.apk 또는 ./uploads/apk/file.apk
                String basePath = System.getProperty("user.dir"); // 또는 설정된 업로드 경로

                // 경로가 /uploads로 시작하면 현재 디렉토리 기준으로 변환
                if (path.startsWith("/uploads")) {
                    return new File(basePath + path);
                } else {
                    return new File(basePath + "/uploads" + path);
                }

            } catch (Exception e) {
                log.error("URL 파싱 실패: {}", fileUrl, e);
                // 실패 시 URL 전체를 경로로 시도
                return new File(fileUrl);
            }
        }

        // 이미 파일 경로인 경우
        return new File(fileUrl);
    }

    /**
     * ✅ [사용자] 앱 다운로드 URL 조회 API
     * 앱에서 다운로드 URL을 먼저 받아서 처리하는 방식
     */
    @GetMapping("/download-url/{type}")
    public ResponseEntity<?> getDownloadUrl(@PathVariable String type) {
        try {
            log.info("🔗 다운로드 URL 요청 - 타입: {}", type);

            // 카운트 증가 없이 URL만 반환하는 메서드가 필요하면 서비스에 추가
            String downloadUrl = "/api/admin/app/download/" + type;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "type", type,
                    "downloadUrl", downloadUrl,
                    "fullUrl", "https://api.imjemin.co.kr" + downloadUrl
            ));

        } catch (Exception e) {
            log.error("❌ URL 조회 실패: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "URL 조회 중 오류 발생"));
        }
    }

    /**
     * [관리자] 앱 다운로드 통계 조회 API
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getDownloadStats() {
        try {
            log.info("📊 다운로드 통계 조회 요청");
            return ResponseEntity.ok(adminNotificationService.getAllDownloadStats());
        } catch (Exception e) {
            log.error("❌ 통계 조회 실패: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "통계 조회 중 오류 발생"));
        }
    }

    /**
     * ✅ [사용자] 최신 앱 버전 정보 조회 API
     * 앱 업데이트 체크용
     */
    @GetMapping("/version")
    public ResponseEntity<?> getLatestVersion() {
        try {
            log.info("📱 앱 버전 정보 조회 요청");

            // TODO: DB에서 버전 정보 가져오기
            // 현재는 하드코딩된 값 반환
            Map<String, Object> versionInfo = Map.of(
                    "latestVersion", "1.0.0",
                    "latestVersionCode", 1,
                    "forceUpdate", false,
                    "updateMessage", "새로운 기능이 추가되었습니다!",
                    "apkDownloadUrl", "/api/admin/app/download/apk",
                    "releaseDate", "2026-02-11"
            );

            return ResponseEntity.ok(versionInfo);

        } catch (Exception e) {
            log.error("❌ 버전 정보 조회 실패: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "버전 정보 조회 중 오류 발생"));
        }
    }
}