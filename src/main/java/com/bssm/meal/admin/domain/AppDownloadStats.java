package com.bssm.meal.admin.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor // JPA 엔티티는 기본 생성자가 필수입니다.
public class AppDownloadStats {

    @Id // ✅ Primary Key 설정
    private String appType; // "APK" 또는 "IPA"

    private Long downloadCount = 0L;

    private LocalDateTime lastDownloadedAt;

    public AppDownloadStats(String appType) {
        this.appType = appType;
        this.downloadCount = 0L;
    }

    public void increment() {
        this.downloadCount++;
        this.lastDownloadedAt = LocalDateTime.now();
    }
}