package com.bssm.meal.favorite.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "admin_notifications")
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String senderEmail;

    private String targetType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    // ✅ 추가: sent 필드 (발송 완료 여부)
    @Column(nullable = false)
    private Boolean sent = true;

    @Column(nullable = false)
    private Integer totalCount = 0;

    @Column(nullable = false)
    private Integer successCount = 0;

    @Column(nullable = false)
    private Integer failureCount = 0;

    @Builder
    public NotificationHistory(String title, String body, String senderEmail, String targetType,
                               Integer totalCount, Integer successCount, Integer failureCount) {
        this.title = title;
        this.body = body;
        this.senderEmail = senderEmail;
        this.targetType = targetType;
        this.totalCount = (totalCount != null) ? totalCount : 0;
        this.successCount = (successCount != null) ? successCount : 0;
        this.failureCount = (failureCount != null) ? failureCount : 0;
        this.sent = true; // ✅ 기본값 설정
        this.sentAt = LocalDateTime.now();
    }

    /**
     * ✅ 영속화 전 기본값 설정
     */
    @PrePersist
    protected void onCreate() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
        if (this.sent == null) {
            this.sent = true;
        }
    }
}