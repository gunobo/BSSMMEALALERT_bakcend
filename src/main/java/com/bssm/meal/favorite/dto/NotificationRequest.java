package com.bssm.meal.favorite.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRequest {
    private String title;
    private String body;
    private String targetType;      // "ALL" 또는 "SPECIFIC"
    private List<String> targetEmails; // 💡 여기서 List로 받아야 에러가 안 납니다.
    private String targetDate;
    private LocalDateTime scheduledTime;
}