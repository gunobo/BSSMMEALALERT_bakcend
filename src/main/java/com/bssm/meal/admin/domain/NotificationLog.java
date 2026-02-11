package com.bssm.meal.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // 알림 제목
    private String type; // 발송 타입 (ALL / TARGET)
    private int totalCount; // 대상 기기 수
    private int successCount; // 성공 수
    private int failureCount; // 실패 수
    private LocalDateTime sentAt; // 발송 시간
    private String createdBy;

    @Builder.Default
    private String status = "COMPLETED"; // 발송 상태
}