package com.bssm.meal.favorite.controller;

import com.bssm.meal.favorite.service.FcmService;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;
    private final UserRepository userRepository;

    /**
     * ✅ FCM 토큰 구독/업데이트 API
     */
    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(
            Authentication authentication, // ✅ String 대신 Authentication 객체를 사용하여 인증 정보 추출
            @RequestBody FcmTokenRequest request
    ) {
        // 1. 인증 확인
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("⚠️ 미인증 사용자의 FCM 구독 시도");
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        // Authentication 객체에서 이메일(사용자 식별자) 추출
        String email = authentication.getName();

        // 2. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다."));

        // 3. 토큰 유효성 검사
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            log.warn("⚠️ 빈 토큰 수신: User={}", email);
            return ResponseEntity.badRequest().body("토큰이 비어있습니다.");
        }

        // 4. 장치 타입 설정 (프론트 로그에 따라 MOBILE을 기본값으로 사용)
        String deviceType = (request.getDeviceType() != null)
                ? request.getDeviceType().toUpperCase()
                : "MOBILE";

        log.info("📩 FCM 구독 요청 수신: Email={}, Type={}, Token(..{})",
                email, deviceType, request.getToken().substring(Math.max(0, request.getToken().length() - 8)));

        // 5. 서비스 호출하여 DB 필드 업데이트
        fcmService.saveToken(user.getId(), request.getToken(), deviceType);

        return ResponseEntity.ok("성공");
    }

    /**
     * ✅ 프론트엔드 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    public static class FcmTokenRequest {
        private String token;
        private String deviceType; // MOBILE 또는 WEB
    }
}