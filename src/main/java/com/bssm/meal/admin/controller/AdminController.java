package com.bssm.meal.admin.controller;

import com.bssm.meal.admin.dto.AdminStatsResponse;
import com.bssm.meal.admin.dto.UserDetailResponse;
import com.bssm.meal.admin.service.AdminService;
import com.bssm.meal.admin.service.AdminNotificationService;
import com.bssm.meal.admin.service.EmailService;
import com.bssm.meal.favorite.entity.AdminNotification;
import com.bssm.meal.favorite.entity.NotificationHistory; // ✅ 기존 엔티티 임포트
import com.bssm.meal.favorite.repository.AdminNotificationRepository;
import com.bssm.meal.favorite.repository.NotificationHistoryRepository; // ✅ 기존 레포지토리 임포트
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminNotificationService adminNotificationService;
    private final AdminService adminService;
    private final EmailService emailService;
    private final AdminNotificationRepository adminNotificationRepository; // ✅ 레포지토리 교체

    /**
     * 전체 사용자 목록 조회 및 검색
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDetailResponse>> getAllUsers(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        List<UserDetailResponse> users = adminService.getUsersByFilter(type, keyword);
        return ResponseEntity.ok(users);
    }

    /**
     * 전역 알림 전송 (공지사항 형태)
     */
    @PostMapping(value = "/notifications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> sendNotice(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        adminNotificationService.saveNoticeWithFile(title, content, file, true, "ALARM");
        return ResponseEntity.ok("실시간 알림 전송 완료");
    }

    /**
     * 관리자 대시보드 요약 통계 데이터 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        AdminStatsResponse stats = adminService.getOverallStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * ✅ FCM 알림 발송 이력 및 통계 조회
     * /api/admin/notifications/stats
     */
    @GetMapping("/notifications/stats")
    public ResponseEntity<?> getNotificationStats() {
        // 1. 최신 10건의 알림 내역 조회
        List<AdminNotification> logs = adminNotificationRepository.findTop10ByOrderByCreatedAtDesc();
        long totalSent = adminNotificationRepository.count();

        // 2. 리액트 필드명(sentAt, senderEmail)에 맞춰서 데이터 매핑
        List<Map<String, Object>> mappedLogs = logs.stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("sentAt", log.getCreatedAt());      // createdAt -> sentAt
            map.put("senderEmail", log.getCreatedBy()); // createdBy -> senderEmail
            map.put("title", log.getTitle());
            map.put("targetType", log.getTargetType());
            map.put("totalCount", log.getTotalCount());
            map.put("successCount", log.getSuccessCount());
            map.put("failureCount", log.getFailureCount());
            return map;
        }).collect(Collectors.toList());

        // 3. 최종 응답
        return ResponseEntity.ok(Map.of(
                "recentLogs", mappedLogs,
                "totalSentCount", totalSent
        ));
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

    /**
     * 유저 차단/해제
     */
    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<String> toggleUserBan(
            @PathVariable String id,
            @RequestParam boolean status,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer min){

        adminService.updateUserBannedStatus(id, status, reason, min);
        return ResponseEntity.ok(status ? "차단 완료" : "해제 완료");
    }

    /**
     * 유저 강제 로그아웃
     */
    @PostMapping("/users/{userId}/logout")
    public ResponseEntity<?> forceLogout(@PathVariable Long userId) {
        adminService.forceLogoutUser(userId);
        return ResponseEntity.ok("해당 사용자가 강제 로그아웃 되었습니다.");
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