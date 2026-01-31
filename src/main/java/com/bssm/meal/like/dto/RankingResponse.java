package com.bssm.meal.like.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankingResponse {
    private String mealDate;
    private String mealType;
    private String mealKey;
    private long count; // 좋아요 합계

    public long getLikeCount() {
        return this.count;
    }
}