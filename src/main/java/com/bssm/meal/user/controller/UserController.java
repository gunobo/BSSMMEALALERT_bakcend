package com.bssm.meal.user.controller;

import com.bssm.meal.admin.service.EmailService;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Getter
    @NoArgsConstructor
    public static class UpdateUserRequest {
        private List<String> allergies;
        private List<String> favoriteMenus;
    }

    /**
     * ✅ 현재 로그인된 유저 정보 조회 및 자동 가입 처리
     * 💡 신규 가입 시 즉시 메일 발송 로직 추가
     */
    @GetMapping("/me")
    @Transactional
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            log.warn("🚨 인증 정보가 없습니다.");
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }

        String email;
        String finalName = "사용자";
        String finalPicture = "";

        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
            finalName = oAuth2User.getAttribute("name");
            finalPicture = oAuth2User.getAttribute("picture");
        } else {
            email = authentication.getName();
        }

        final String userEmail = email;
        final String userName = finalName;
        final String userPicture = finalPicture;

        // 🚀 핵심 변경: 유저가 없을 때(신규 가입) 가입시킨 후 바로 메일 발송
        User user = userRepository.findByEmail(userEmail).orElseGet(() -> {
            log.info("🆕 신규 유저 자동 가입 처리 시작: {}", userEmail);
            User newUser = User.builder()
                    .email(userEmail)
                    .name(userName)
                    .picture(userPicture)
                    .role("USER")
                    .build();

            User savedUser = userRepository.save(newUser);

            // 📧 가입과 동시에 웰컴 메일 발송 (유저가 메인으로 이동하기 전 실행됨)
            try {
                log.info("📧 신규 가입자 [{}]에게 웰컴 메일을 발송합니다.", userEmail);
                emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());
            } catch (Exception e) {
                log.error("📧 메일 발송 중 오류 발생 (하지만 가입은 유지): {}", e.getMessage());
            }

            return savedUser;
        });

        // ================= [차단 자동 해제 체크 로직] =================
        if (user.isBanned()) {
            if (user.getBanExpiresAt() != null) {
                if (LocalDateTime.now().isAfter(user.getBanExpiresAt())) {
                    user.updateBannedStatus(false, null, null);
                    userRepository.saveAndFlush(user);
                    emailService.sendUnbanNotification(user.getEmail());
                } else {
                    return ResponseEntity.status(403).body("차단된 계정입니다. 만료 예정: " + user.getBanExpiresAt());
                }
            } else {
                return ResponseEntity.status(403).body("영구 차단된 계정입니다. 사유: " + user.getBanReason());
            }
        }
        // ==========================================================

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "picture", user.getPicture() != null ? user.getPicture() : "",
                "allergies", user.getAllergies() != null ? user.getAllergies() : List.of(),
                "favoriteMenus", user.getFavoriteMenus() != null ? user.getFavoriteMenus() : List.of()
        ));
    }

    /**
     * ✅ 알레르기 및 선호 메뉴 정보 업데이트 (기존 메일 로직 제거 가능)
     */
    @PostMapping("/update-info")
    @Transactional
    public ResponseEntity<?> updateUserInfo(@RequestBody UpdateUserRequest request, Authentication authentication) {
        log.info("📢 정보 업데이트 요청 수신");

        if (authentication == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 정보가 존재하지 않습니다."));

        if (request.getAllergies() != null) user.updateAllergies(request.getAllergies());
        if (request.getFavoriteMenus() != null) user.updateFavoriteMenus(request.getFavoriteMenus());

        userRepository.saveAndFlush(user);

        return ResponseEntity.ok(Map.of("message", "정보가 저장되었습니다."));
    }
}