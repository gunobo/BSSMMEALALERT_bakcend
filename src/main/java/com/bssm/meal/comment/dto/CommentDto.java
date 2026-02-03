package com.bssm.meal.comment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor  // 기본 생성자
@AllArgsConstructor // 빌더 사용 시 필수
@Builder            // ✅ .builder() 메서드 생성
public class CommentDto {
    private Long id;
    private String content;
    private String mealDate;
    private String mealType;
    private String mealKey;
    private String username;
    private String email;
    private String createdAt;
}