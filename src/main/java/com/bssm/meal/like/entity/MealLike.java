package com.bssm.meal.like.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Table(
        name = "meal_like",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"userId", "mealDate", "mealType", "mealKey"}
                )
        }
)
@Entity
@Getter @Setter
@NoArgsConstructor
public class MealLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50) // 255에서 50 정도로 줄입니다.
    private String userId;
    @Column(length = 20) // 날짜는 20자면 충분합니다. (예: 2026-01-29)
    private String mealDate;
    @Column(length = 20) // 식사 타입도 짧게 (예: LUNCH, DINNER)
    private String mealType;

    @Column(length = 100) // 메뉴 키 값도 적당히 줄입니다.
    private String mealKey;

    private LocalDateTime createdAt = LocalDateTime.now();

    // 토글 구현 시 중복 체크를 위한 생성자
    public MealLike(String userId, String mealDate, String mealType, String mealKey) {
        this.userId = userId;
        this.mealDate = mealDate;
        this.mealType = mealType;
        this.mealKey = mealKey;
    }
}