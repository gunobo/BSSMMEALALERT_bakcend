package com.bssm.meal.favorite.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // 클래스 레벨 빌더를 사용하여 모든 필드에 대응합니다.
@Table(name = "admin_notifications")
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    private String targetDate;   // 급식 날짜 (예: 2026-02-06)

    private String targetType;   // "ALL" 또는 "TARGET"

    @Column(columnDefinition = "TEXT")
    private String targetEmails; // 수신자 이메일 리스트 (콤마 구분)

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;; // 예약 발송 시간

    @Builder.Default
    private boolean sent = false; // 발송 완료 여부

    // ✅ 통계 필드: 기본값을 0으로 고정하여 DB 500 에러 방지
    @Builder.Default
    @Column(nullable = false)
    private int totalCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int successCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int failureCount = 0;

    // ✅ 관리자 대시보드 및 스케줄러에서 사용하는 생성자/작성자 필드
    private String createdBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        // 데이터 정합성 보장: Builder 사용 시 0 미만 값이 들어오는 것을 방지
        if (this.totalCount < 0) this.totalCount = 0;
        if (this.successCount < 0) this.successCount = 0;
        if (this.failureCount < 0) this.failureCount = 0;
    }
}