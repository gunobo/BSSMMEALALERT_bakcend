package com.bssm.meal.admin.controller;

import com.bssm.meal.admin.dto.AdminStatsResponse;
import com.bssm.meal.admin.service.AdminService;
import com.bssm.meal.admin.service.NotificationService;
import com.bssm.meal.service.EmailService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final NotificationService notificationService;
    private final AdminService adminService;
    private final EmailService emailService;

    @PostMapping(value = "/notifications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> sendNotice(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        notificationService.sendGlobalNotice(title, content, file);
        return ResponseEntity.ok("알림 및 이미지 전송 완료");
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        AdminStatsResponse stats = adminService.getOverallStats();
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/reports/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id) {
        // 여기에 실제 DB 삭제 로직을 추가하세요 (예: reportService.delete(id);)
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reports/{id}/process")
    public ResponseEntity<String> processReport(
            @PathVariable Long id,
            @RequestBody ReportProcessRequest request) {

        // 1. DB 상태 변경 (이제 목록 조회 시 나타나지 않음)
        adminService.processReport(id);

        // 2. 메일 발송
        emailService.sendReportResult(
                request.getUserEmail(),
                request.getStatus(),
                request.getMessage()
        );

        return ResponseEntity.ok("신고 처리가 완료되었습니다.");
    }
}

/**
 * ✅ 에러 해결: 데이터를 담을 DTO 클래스 추가
 * 이 클래스가 있어야 request.getUserEmail() 등을 사용할 수 있습니다.
 */
@Getter
@NoArgsConstructor
class ReportProcessRequest {
    private String status;    // RESOLVED 또는 REJECTED
    private String message;   // 관리자 답변 내용
    private String userEmail; // 신고자 이메일
}