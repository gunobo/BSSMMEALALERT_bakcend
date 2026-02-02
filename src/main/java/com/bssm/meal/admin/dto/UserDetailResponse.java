package com.bssm.meal.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {

    private String id;               // 프론트의 u.id와 매칭
    private String email;            // 프론트의 u.email과 매칭

    private List<String> allergies;  // 알레르기 목록 ["우유", "난류"]
    private List<String> favoriteMenus; // 선호 메뉴 목록 ["마라탕", "치킨"]

    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드 (선택 사항)
     * Service 계층에서 .map(UserDetailResponse::from) 처럼 깔끔하게 사용 가능합니다.
     */
    /*
    public static UserDetailResponse from(User entity) {
        return UserDetailResponse.builder()
                .id(entity.getUserId())
                .email(entity.getEmail())
                .allergies(entity.getAllergies())
                .favoriteMenus(entity.getFavoriteMenus())
                .build();
    }
    */
}