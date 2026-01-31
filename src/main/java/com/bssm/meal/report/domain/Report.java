package com.bssm.meal.report.domain;

import com.bssm.meal.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reason;

    // ✅ AdminService에서 r.getReportReason()을 쓰므로 별칭 메서드를 만들거나 이름을 맞춥니다.
    public String getReportReason() {
        return this.reason;
    }

    @Column(columnDefinition = "TEXT")
    private String content;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    private ReportType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    // ✅ AdminService에서 r.getUser().getName()을 호출하므로
    // 작성자(reporter)를 리턴하는 getUser 메서드 추가 (필요 시)
    public User getUser() {
        return this.reporter;
    }

    // ✅ 서비스의 findByIsReportedTrue를 지원하기 위한 필드 추가
    // 보통 신고가 들어오면 기본값이 true여야 하므로 @Builder.Default 설정
    @Builder.Default
    private boolean isReported = false;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ✅ Setter를 쓰기 싫다면 아래와 같이 전용 메서드를 만드세요.
    public void setIsReported(boolean status) {
        this.isReported = status;
    }
}