package com.bssm.meal.like.service;

import com.bssm.meal.like.repository.LikeRepository;
import com.bssm.meal.meal.dto.MealResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // ✅ 조회 성능 향상
public class MealService {

    private final LikeRepository likeRepository;

    /**
     * 식단 상세 정보와 좋아요 상태를 함께 반환
     */
    public MealResponseDto getMealDetail(String userId, String date, String type, String mealKey, String mealName) {

        // 1. 해당 메뉴의 총 좋아요 수 조회
        long likeCount = likeRepository.countByMealKey(mealKey);

        // 2. 유저별 좋아요 여부 판단
        // userId가 "null"(문자열)이거나 null객체인 경우를 모두 체크
        boolean isLiked = false;
        if (userId != null && !userId.trim().isEmpty() && !"null".equals(userId)) {
            isLiked = likeRepository.existsByUserIdAndMealDateAndMealTypeAndMealKey(
                    userId, date, type, mealKey);
        }

        return MealResponseDto.builder()
                .mealKey(mealKey)
                .name(mealName)
                .date(date)
                .type(type)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }
}