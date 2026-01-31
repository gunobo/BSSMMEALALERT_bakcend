package com.bssm.meal.user.dto;

import com.bssm.meal.user.domain.User; // ✅ 경로를 domain.User로 수정
import lombok.Getter;
import java.util.List;

@Getter
public class UserResponse {
    private String name;
    private String email;
    private String picture;
    private List<String> allergies;
    private List<String> favoriteMenus;

    public UserResponse(User user) { // ✅ 이제 User 타입을 인식합니다.
        this.name = user.getName();
        this.email = user.getEmail();
        this.picture = user.getPicture();
        this.allergies = user.getAllergies();
        this.favoriteMenus = user.getFavoriteMenus();
    }
}