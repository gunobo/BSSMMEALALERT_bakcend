package com.bssm.meal.favorite.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PushRequestDto {
    private String title;
    private String body;
    private String targetDate;
    private java.time.LocalDateTime scheduledTime; // 타입을 LocalDateTime으로 변경
    private String targetType;
    private List<String> targetEmails;
}