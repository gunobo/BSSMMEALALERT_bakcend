package com.bssm.meal.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {

    private String id;               // 사용자 고유 ID
    private String email;            // 이메일 주소

    // ✅ 구글 프로필 이름을 담기 위해 추가된 필드
    private String userName;
    private String googleId; // ✅ 추가
    private String picture;

    // ✅ 관리자 페이지 차단 관리를 위한 필드들
    private boolean banned;          // 차단 여부
    private String banReason;        // 차단 사유
    private LocalDateTime banExpiresAt; // 차단 만료 일시

    private List<String> allergies;      // 알레르기 목록
    private List<String> favoriteMenus;   // 선호 메뉴 목록
}