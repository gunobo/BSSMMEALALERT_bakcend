package com.bssm.meal.user.controller;

import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // 추가
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @Getter
    @NoArgsConstructor
    public static class UpdateUserRequest {
        private List<String> allergies;
        private List<String> favoriteMenus;
    }

    // ✅ 현재 로그인된 유저 정보 조회
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        // Principal을 객체 타입에 맞춰 안전하게 가져옴
        if (authentication == null) return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");

        String email = authentication.getName();
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "picture", user.getPicture() != null ? user.getPicture() : "",
                    "allergies", user.getAllergies(),
                    "favoriteMenus", user.getFavoriteMenus()
            ));
        }
        return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
    }

    // ✅ 알레르기 및 선호 메뉴 정보 업데이트
    @PostMapping("/update-info")
    public ResponseEntity<?> updateUserInfo(@RequestBody UpdateUserRequest request, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        String email = authentication.getName();
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 리스트 데이터를 새 데이터로 교체 (JPA가 @ElementCollection 자동 관리)
            if (request.getAllergies() != null) {
                user.setAllergies(request.getAllergies());
            }
            if (request.getFavoriteMenus() != null) {
                user.setFavoriteMenus(request.getFavoriteMenus());
            }

            userRepository.save(user);

            // 저장 후 최신 정보를 함께 보내주면 프론트엔드 상태 관리가 편합니다.
            return ResponseEntity.ok(Map.of(
                    "message", "저장 성공",
                    "allergies", user.getAllergies(),
                    "favoriteMenus", user.getFavoriteMenus(),
                    "role", user.getRole()
            ));
        }
        return ResponseEntity.status(404).body("유저 정보가 존재하지 않습니다.");
    }
}