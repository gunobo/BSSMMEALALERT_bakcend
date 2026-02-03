package com.bssm.meal.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter // ✅ 유지하되, 특정 필드는 주의해서 사용
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
    private String role = "ROLE_USER"; // ✅ 기본값을 ROLE_ 접두사 포함하여 설정

    @Builder.Default
    @Column(nullable = false)
    private boolean isBanned = false;

    private String banReason;

    private LocalDateTime banExpiresAt;

    // ✅ FetchType.EAGER는 잘 설정하셨습니다. (데이터 조회 시 즉시 로딩)
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
     * ✅ Role 설정 시 "ROLE_" 접두사 자동 처리 보완
     */
    public void setRole(String role) {
        if (role == null) {
            this.role = "ROLE_USER";
            return;
        }
        if (!role.startsWith("ROLE_")) {
            this.role = "ROLE_" + role.toUpperCase(); // 대문자 변환 추가
        } else {
            this.role = role;
        }
    }

    // 관리자 페이지 DTO 매핑용 (이메일을 반환하도록 유지)
    public String getUserId() {
        return this.email;
    }

    /**
     * ✅ 차단/해제 메서드 (Dirty Checking 활용)
     */
    public void updateBannedStatus(boolean status, String reason, LocalDateTime expiresAt) {
        this.isBanned = status;
        this.banReason = status ? reason : null;
        this.banExpiresAt = status ? expiresAt : null;
    }

    /**
     * ✅ 리스트 업데이트용 메서드 추가 (직접 리스트를 교체할 때 안전함)
     */
    public void updateAllergies(List<String> newAllergies) {
        this.allergies.clear();
        if (newAllergies != null) {
            this.allergies.addAll(newAllergies);
        }
    }

    public void updateFavoriteMenus(List<String> newMenus) {
        this.favoriteMenus.clear();
        if (newMenus != null) {
            this.favoriteMenus.addAll(newMenus);
        }
    }
    public void setIsBanned(boolean isBanned) {
        this.isBanned = isBanned;
    }

}