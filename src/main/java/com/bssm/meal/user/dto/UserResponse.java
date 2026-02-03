package com.bssm.meal.user.dto;

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
public class UserResponse {
    private String id;
    private String email;
    private String userName;
    private String googleId; // ✅ 추가
    private String picture;

    // ✅ 에러 해결을 위해 아래 필드들을 반드시 추가해야 합니다!
    private boolean isBanned;      // 차단 여부
    private String banReason;      // 차단 사유
    private LocalDateTime banExpiresAt; // 차단 만료 시간

    private List<String> allergies;
    private List<String> favoriteMenus;
}