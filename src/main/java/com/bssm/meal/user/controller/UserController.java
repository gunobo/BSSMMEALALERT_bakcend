package com.bssm.meal.user.controller;

import com.bssm.meal.admin.service.EmailService;
import com.bssm.meal.favorite.entity.FcmToken;
import com.bssm.meal.favorite.repository.FcmTokenRepository;
import com.bssm.meal.favorite.service.FcmService;
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

import com.bssm.meal.user.service.UserService;

import java.util.HashMap;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FcmService fcmService;
    private final FcmTokenRepository fcmTokenRepository;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Getter
    @NoArgsConstructor
    public static class UpdateUserRequest {
        private List<String> allergies;
        private List<String> favoriteMenus;
        private boolean allow_notifications;
        private boolean allow_allergy_notifications;
        private boolean allow_favorite_notifications;
    }

    /**
     * ✅ 현재 로그인된 유저 정보 조회
     */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            log.warn("🚨 인증 정보가 없습니다.");
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }

        String email;
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 차단 체크 로직
        if (user.isBanned()) {
            if (user.getBanExpiresAt() != null) {
                if (LocalDateTime.now().isAfter(user.getBanExpiresAt())) {
                    user.updateBannedStatus(false, null, null);
                    userRepository.saveAndFlush(user);
                } else {
                    return ResponseEntity.status(403).body("차단된 계정입니다.");
                }
            } else {
                return ResponseEntity.status(403).body("영구 차단된 계정입니다.");
            }
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "picture", user.getPicture() != null ? user.getPicture() : "",
                "allergies", user.getAllergies() != null ? user.getAllergies() : List.of(),
                "favoriteMenus", user.getFavoriteMenus() != null ? user.getFavoriteMenus() : List.of(),
                "allow_notifications", user.isAllow_notifications(),
                "allow_allergy_notifications", user.isAllow_allergy_notifications(),
                "allow_favorite_notifications", user.isAllow_favorite_notifications()
        ));
    }

    /**
     * ✅ 정보 업데이트 (유저 설정 + 모든 토큰 설정 동기화)
     */
    @PostMapping("/update-info")
    @Transactional
    public ResponseEntity<?> updateUserInfo(@RequestBody UpdateUserRequest request, Authentication authentication) {
        log.info("📢 정보 업데이트 요청 수신 - 알림상태: {}", request.isAllow_notifications());

        if (authentication == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        String email = (authentication.getPrincipal() instanceof OAuth2User oAuth2User)
                ? oAuth2User.getAttribute("email")
                : authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 정보가 존재하지 않습니다."));

        // 1️⃣ User 테이블 업데이트
        user.updateInfo(
                request.getAllergies(),
                request.getFavoriteMenus(),
                request.isAllow_notifications(),
                request.isAllow_allergy_notifications(),
                request.isAllow_favorite_notifications()
        );
        userRepository.saveAndFlush(user);

        // 2️⃣ FcmToken 테이블 업데이트 (해당 유저의 모든 기기 동기화)
        List<FcmToken> userTokens = fcmTokenRepository.findAllByUserId(user.getId());
        for (FcmToken token : userTokens) {
            token.setAllowNotifications(request.isAllow_notifications());
        }
        fcmTokenRepository.saveAll(userTokens);

        log.info("✅ 유저 [{}]와 연결된 모든 토큰의 알림 상태를 {}로 동기화 완료", email, request.isAllow_notifications());

        return ResponseEntity.ok(Map.of("message", "정보가 저장되었습니다."));
    }

    /**
     * ✅ FCM 토큰 업데이트 (saveToken 파라미터 불일치 해결)
     */
    @PostMapping("/fcm/token")
    @Transactional
    public ResponseEntity<?> updateFcmToken(
            Authentication authentication,
            @RequestBody Map<String, String> request) {

        if (authentication == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");

        String token = request.get("token");
        // ✅ [수정] 프론트엔드에서 넘어오는 deviceType 수신 (기본값 WEB)
        String deviceType = request.getOrDefault("deviceType", "WEB");

        String email = (authentication.getPrincipal() instanceof OAuth2User oAuth2User)
                ? oAuth2User.getAttribute("email")
                : authentication.getName();

        userRepository.findByEmail(email).ifPresentOrElse(u -> {
            // ✅ [수정] fcmService.saveToken에 deviceType 인자를 추가하여 메서드 시그니처 일치시킴
            fcmService.saveToken(u.getId(), token, deviceType);

            // 저장된 토큰의 알림 설정을 유저의 현재 설정값으로 맞춤
            fcmTokenRepository.findByToken(token).ifPresent(t -> {
                t.setAllowNotifications(u.isAllow_notifications());
                fcmTokenRepository.saveAndFlush(t);
            });

            log.info("✅ 유저 [{}]의 {} 토큰 저장 및 알림 설정({}) 동기화 완료", email, deviceType, u.isAllow_notifications());
        }, () -> log.error("❌ 유저 찾기 실패"));

        return ResponseEntity.ok(Map.of("message", "토큰 업데이트 성공"));
    }

    /**
     * ✅ 로그아웃 시 토큰 삭제
     */
    @PostMapping("/logout-device")
    @Transactional
    public ResponseEntity<?> logoutDevice(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token != null && !token.isEmpty()) {
            fcmService.deleteToken(token);
        }
        return ResponseEntity.ok(Map.of("message", "기기 로그아웃 성공"));
    }

    /**
     * ✅ 회원 탈퇴 (계정 및 모든 데이터 삭제)
     * DELETE /api/user/delete
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("❌ 인증되지 않은 사용자의 계정 삭제 시도");
                return ResponseEntity.status(401)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            String userEmail = authentication.getName();
            log.info("🗑️ 계정 삭제 요청 - 사용자: {}", userEmail);

            // 사용자 존재 확인
            if (!userService.existsByEmail(userEmail)) {
                log.warn("❌ 존재하지 않는 사용자: {}", userEmail);
                return ResponseEntity.status(404)
                        .body(Map.of("message", "사용자를 찾을 수 없습니다."));
            }

            // 계정 및 모든 데이터 삭제
            userService.deleteUserAndAllData(userEmail);

            log.info("✅ 계정 삭제 완료 - 사용자: {}", userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "계정이 성공적으로 삭제되었습니다.");
            response.put("deletedAt", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 계정 삭제 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(Map.of("message", "계정 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * ✅ 계정 삭제 요청 (이메일로)
     * POST /api/user/delete-request
     */
    @PostMapping("/delete-request")
    public ResponseEntity<?> requestDeleteAccount(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String reason = request.get("reason");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "이메일을 입력해주세요."));
            }

            log.info("📧 계정 삭제 요청 접수 - 이메일: {}, 사유: {}", email, reason);

            // 삭제 요청 기록 저장
            userService.createDeleteRequest(email, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "계정 삭제 요청이 접수되었습니다. 3영업일 이내에 처리됩니다.");
            response.put("email", email);
            response.put("requestedAt", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 계정 삭제 요청 처리 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(Map.of("message", "요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * ✅ 계정 정보 조회 (삭제 전 확인용)
     * GET /api/user/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            String userEmail = authentication.getName();
            Map<String, Object> userInfo = userService.getUserInfo(userEmail);

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            log.error("❌ 사용자 정보 조회 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(Map.of("message", "정보 조회 중 오류가 발생했습니다."));
        }
    }
}