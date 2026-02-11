package com.bssm.meal.like.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealResponseDto {
    private String mealKey;
    private String name;
    private String date;
    private String type;
    private long likeCount;   // 전체 좋아요 수
    private boolean isLiked;  // ✅ 현재 로그인한 유저의 좋아요 여부
}