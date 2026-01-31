package com.bssm.meal.report.controller;

import com.bssm.meal.report.domain.Report;
import com.bssm.meal.report.domain.ReportType;
import com.bssm.meal.report.repository.ReportRepository;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository; // 👈 추가 필요
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // 👈 추가 필요
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository; // 👈 사용자 조회를 위해 추가

    // 1. 일반 유저: 신고하기 (로그인 필수)
    @PostMapping
    public ResponseEntity<?> createReport(
            @RequestBody Map<String, Object> payload,
            Principal principal // ✅ @AuthenticationPrincipal 대신 Principal 사용
    ) {
        // 토큰이 없거나 유효하지 않으면 principal은 null이 됩니다.
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요한 서비스입니다."));
        }

        try {
            // ✅ principal.getName()을 통해 사용자 이메일(또는 ID)로 유저 엔티티 조회
            User reporter = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            Report report = Report.builder()
                    .reason(payload.get("reason").toString())
                    .content(payload.get("content") != null ? payload.get("content").toString() : "")
                    .targetId(Long.parseLong(payload.get("targetId").toString()))
                    .type(ReportType.valueOf(payload.get("type").toString().toUpperCase()))
                    .reporter(reporter) // 찾은 유저 저장
                    .isReported(true)
                    .build();

            reportRepository.save(report);
            return ResponseEntity.ok(Map.of("message", "신고가 정상적으로 접수되었습니다."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "잘못된 신고 타입입니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    // 2. 관리자 전용: 모든 미처리 신고 내역 가져오기
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportRepository.findByIsReportedTrue());
    }

    // 3. 관리자 전용: 신고 내역 삭제 (처리 완료 시)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id, Authentication authentication) {
        // 권한 체크 로직 (예: 관리자인지 확인)
        if (authentication == null) return ResponseEntity.status(401).build();

        reportRepository.deleteById(id);
        return ResponseEntity.ok("삭제 성공");
    }
}