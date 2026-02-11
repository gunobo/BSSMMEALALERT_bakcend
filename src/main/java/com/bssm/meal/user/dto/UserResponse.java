package com.bssm.meal.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String googleId;
    private String picture;

    @JsonProperty("isBanned")
    private boolean isBanned;

    private String banReason;
    private LocalDateTime banExpiresAt;

    private List<String> allergies;
    private List<String> favoriteMenus;

    // 1. 기본 급식 알림
    @JsonProperty("allow_notifications")
    private boolean allow_notifications;

    // ✅ 2. 알레르기 알림 (추가)
    @JsonProperty("allow_allergy_notifications")
    private boolean allow_allergy_notifications;

    // ✅ 3. 선호 메뉴 알림 (추가)
    @JsonProperty("allow_favorite_notifications")
    private boolean allow_favorite_notifications;

    /**
     * ✅ UserService 등에서 명시적으로 호출하기 위한 Getter
     * 롬복의 @Getter는 불리언 필드에 언더바가 있으면 이름을 헷갈리게 생성할 수 있으므로 직접 정의합니다.
     */
    public boolean isAllow_notifications() {
        return allow_notifications;
    }

    public boolean isAllow_allergy_notifications() {
        return allow_allergy_notifications;
    }

    public boolean isAllow_favorite_notifications() {
        return allow_favorite_notifications;
    }
}