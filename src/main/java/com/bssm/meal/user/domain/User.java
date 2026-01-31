package com.bssm.meal.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter // ✅ 수동 Getter 대신 Lombok 사용 (코드 간결화)
@Setter // ✅ 수동 Setter 대신 Lombok 사용 (실수 방지)
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

    // ✅ 필드 초기화와 Builder.Default를 함께 사용해야 Null 에러가 안 납니다.
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

    public String getRole() {
        return this.role; // 이 메서드가 없으면 Map.of에서 null이 들어갑니다.
    }
    // Lombok @Getter/Setter가 아래 수동 메서드들을 자동으로 생성해줍니다.
    public void setRole(String role) {
        if (role != null && !role.startsWith("ROLE_")) {
            this.role = "ROLE_" + role;
        } else {
            this.role = role;
        }
    }
}