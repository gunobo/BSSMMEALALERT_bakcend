package com.bssm.meal.user.domain;

import jakarta.persistence.*;
import lombok.*;

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
    private String role = "USER";

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

    // 스프링 시큐리티나 Map.of 등에서 쓰일 수 있도록 명시적 Getter
    public String getRole() {
        return this.role;
    }

    // 관리자 페이지 DTO 매핑용 메서드 추가
    // 프론트엔드에서 u.id로 표시될 값을 반환합니다.
    public String getUserId() {
        return this.email; // 이메일을 ID로 사용하거나, String.valueOf(this.id)를 사용하세요.
    }

    /**
     * Role 설정 시 "ROLE_" 접두사 자동 처리
     */
    public void setRole(String role) {
        if (role != null && !role.startsWith("ROLE_")) {
            this.role = "ROLE_" + role;
        } else {
            this.role = role;
        }
    }
}