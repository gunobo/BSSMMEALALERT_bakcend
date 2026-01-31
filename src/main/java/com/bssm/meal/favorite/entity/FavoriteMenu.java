package com.bssm.meal.favorite.entity;

import com.bssm.meal.user.domain.User;
import jakarta.persistence.*; // ✅ 반드시 jakarta.persistence 패키지를 사용해야 합니다.
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class FavoriteMenu {

    @Id // ✅ 이제 JPA가 이 ID를 인식합니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ✅ IDENTITY 전략을 위해 String에서 Long으로 변경했습니다.

    private String name;
    private String email;

    @ManyToOne // 유저 한 명은 여러 선호 메뉴 설정을 가질 수 있음
    @JoinColumn(name = "user_id")
    private User user;

    @ElementCollection
    private List<String> allergies = new ArrayList<>();

    @ElementCollection
    private List<String> favoriteMenu = new ArrayList<>();
}