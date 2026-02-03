package com.bssm.meal.admin.controller;

import com.bssm.meal.admin.dto.AdminStatsResponse;
import com.bssm.meal.admin.dto.UserDetailResponse;
import com.bssm.meal.admin.service.AdminService;
import com.bssm.meal.admin.service.NotificationService;
import com.bssm.meal.admin.service.EmailService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final NotificationService notificationService;
    private final AdminService adminService;
    private final EmailService emailService;

    /**
     * 전체 사용자 목록 조회
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDetailResponse>> getAllUsers() {
        List<UserDetailResponse> users = adminService.getAllUsersForAdmin();
        return ResponseEntity.ok(users);
    }

    /**
     * 전역 알림 전송
     */
    @PostMapping(value = "/notifications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> sendNotice(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        notificationService.saveNoticeWithFile(title, content, file, true, "ALARM");
        return ResponseEntity.ok("실시간 알림 전송 완료");
    }

    /**
     * 관리자 통계 데이터 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        AdminStatsResponse stats = adminService.getOverallStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 신고 게시글 삭제
     */
    @DeleteMapping("/reports/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        adminService.deleteReport(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 신고 처리 및 결과 메일 발송
     */
    @PostMapping("/reports/{id}/process")
    public ResponseEntity<String> processReport(
            @PathVariable Long id,
            @RequestBody ReportProcessRequest request) {

        adminService.processReport(id);

        emailService.sendReportResult(
                request.getUserEmail(),
                request.getStatus(),
                request.getMessage()
        );

        return ResponseEntity.ok("신고 처리가 완료되었습니다.");
    }

    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<String> toggleUserBan(
            @PathVariable String id,
            @RequestParam boolean status,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer min){ // ✅ days 추가

        // ✅ 서비스 호출 시 days 포함 (총 4개 인자)
        adminService.updateUserBannedStatus(id, status, reason, min);

        return ResponseEntity.ok(status ? "차단 완료" : "해제 완료");
    }
}

/**
 * 요청/응답을 위한 DTO 클래스
 */
@Getter
@NoArgsConstructor
class ReportProcessRequest {
    private String status;
    private String message;
    private String userEmail;
}