package com.bssm.meal.user.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;
    private String picture;
    private String googleId;

    @Builder.Default
    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Builder.Default
    @Column(nullable = false)
    private boolean isBanned = false;

    private String banReason;
    private LocalDateTime banExpiresAt;

    // ✅ 기기별 FCM 토큰 직접 저장 필드
    @Column(name = "fcm_token_mobile", length = 500)
    private String fcmTokenMobile;

    @Column(name = "fcm_token_web", length = 500)
    private String fcmTokenWeb;

    // 1. 기본 알림 설정
    @Column(name = "allow_notifications")
    @JsonProperty("allow_notifications")
    @Builder.Default
    private Boolean allowNotifications = false;

    // 2. 알레르기 알림 설정
    @Column(name = "allow_allergy_notifications", nullable = false)
    @JsonProperty("allow_allergy_notifications")
    @Builder.Default
    private Boolean allowAllergyNotifications = false;

    // 3. 선호 메뉴 알림 설정
    @Column(name = "allow_favorite_notifications", nullable = false)
    @JsonProperty("allow_favorite_notifications")
    @Builder.Default
    private Boolean allowFavoriteNotifications = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_allergies", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "allergy_name")
    @Builder.Default
    private List<String> allergies = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_favorite_menus", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "menu_name")
    @Builder.Default
    private List<String> favoriteMenus = new ArrayList<>();

    /**
     * ✅ 필드로 저장된 토큰들을 리스트 형태로 반환
     */
    public List<String> getFcmTokens() {
        List<String> tokens = new ArrayList<>();
        if (fcmTokenMobile != null && !fcmTokenMobile.trim().isEmpty()) {
            tokens.add(fcmTokenMobile);
        }
        if (fcmTokenWeb != null && !fcmTokenWeb.trim().isEmpty()) {
            tokens.add(fcmTokenWeb);
        }
        return tokens;
    }

    /**
     * ✅ 알림 허용 여부 getter (Boolean 반환)
     */
    public Boolean getAllowNotifications() {
        return allowNotifications;
    }

    /**
     * ✅ 알림 허용 여부 (boolean primitive 반환)
     */
    public boolean isAllowNotifications() {
        return Boolean.TRUE.equals(allowNotifications);
    }

    /**
     * ✅ 알레르기 알림 허용 여부
     */
    public Boolean getAllowAllergyNotifications() {
        return allowAllergyNotifications;
    }

    public boolean isAllowAllergyNotifications() {
        return Boolean.TRUE.equals(allowAllergyNotifications);
    }

    /**
     * ✅ 선호 메뉴 알림 허용 여부
     */
    public Boolean getAllowFavoriteNotifications() {
        return allowFavoriteNotifications;
    }

    public boolean isAllowFavoriteNotifications() {
        return Boolean.TRUE.equals(allowFavoriteNotifications);
    }

    /**
     * ✅ 알림 허용 설정 setter
     */
    public void setAllowNotifications(Boolean allowNotifications) {
        this.allowNotifications = allowNotifications != null ? allowNotifications : false;
    }

    public void setAllowAllergyNotifications(Boolean allowAllergyNotifications) {
        this.allowAllergyNotifications = allowAllergyNotifications != null ? allowAllergyNotifications : false;
    }

    public void setAllowFavoriteNotifications(Boolean allowFavoriteNotifications) {
        this.allowFavoriteNotifications = allowFavoriteNotifications != null ? allowFavoriteNotifications : false;
    }

    /**
     * ✅ 마이페이지 정보 업데이트
     */
    public void updateInfo(List<String> allergies,
                           List<String> favoriteMenus,
                           Boolean allowNotifications,
                           Boolean allowAllergyNotifications,
                           Boolean allowFavoriteNotifications) {

        this.allergies.clear();
        if (allergies != null) this.allergies.addAll(allergies);

        this.favoriteMenus.clear();
        if (favoriteMenus != null) this.favoriteMenus.addAll(favoriteMenus);

        setAllowNotifications(allowNotifications);
        setAllowAllergyNotifications(allowAllergyNotifications);
        setAllowFavoriteNotifications(allowFavoriteNotifications);
    }

    /**
     * ✅ FCM 토큰 업데이트 로직
     */
    public void updateFcmToken(String token, String deviceType) {
        if (token == null || token.trim().isEmpty()) return;

        if ("MOBILE".equalsIgnoreCase(deviceType)) {
            this.fcmTokenMobile = token;
        } else if ("WEB".equalsIgnoreCase(deviceType)) {
            this.fcmTokenWeb = token;
        }
    }

    /**
     * ✅ 역할 설정
     */
    public void setRole(String role) {
        if (role == null) {
            this.role = "ROLE_USER";
            return;
        }
        this.role = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
    }

    /**
     * ✅ 사용자 ID 반환 (email 사용)
     */
    public String getUserId() {
        return this.email;
    }

    /**
     * ✅ 차단 상태 업데이트
     */
    public void updateBannedStatus(boolean status, String reason, LocalDateTime expiresAt) {
        this.isBanned = status;
        this.banReason = status ? reason : null;
        this.banExpiresAt = status ? expiresAt : null;
    }

    /**
     * ✅ 차단 상태 setter
     */
    public void setIsBanned(boolean isBanned) {
        this.isBanned = isBanned;
    }

    // ✅ 하위 호환성을 위한 deprecated 메서드들 (기존 코드가 있을 경우 대비)
    @Deprecated
    public Boolean isAllow_notifications() {
        return this.allowNotifications;
    }

    @Deprecated
    public Boolean isAllow_allergy_notifications() {
        return this.allowAllergyNotifications;
    }

    @Deprecated
    public Boolean isAllow_favorite_notifications() {
        return this.allowFavoriteNotifications;
    }

    @Deprecated
    public void setAllow_notifications(Boolean allow_notifications) {
        setAllowNotifications(allow_notifications);
    }

    @Deprecated
    public void setAllow_allergy_notifications(Boolean allow_allergy_notifications) {
        setAllowAllergyNotifications(allow_allergy_notifications);
    }

    @Deprecated
    public void setAllow_favorite_notifications(Boolean allow_favorite_notifications) {
        setAllowFavoriteNotifications(allow_favorite_notifications);
    }
}