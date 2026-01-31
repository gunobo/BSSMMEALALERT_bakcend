package com.bssm.meal.auth.controller;

import com.bssm.meal.auth.jwt.JwtService;
import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository; // 유저 저장소 필요
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserRepository userRepository; // 추가

    public AuthController(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        String googleToken = body.get("token");

        try {
            // 1. 구글 토큰 검증 및 정보 추출 (임시 객체 혹은 email 추출)
            User googleUser = jwtService.verifyAndGetUser(googleToken);

            if (googleUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 구글 토큰입니다.");
            }

            // 2. DB에서 해당 이메일의 유저가 있는지 확인
            // AuthController.java의 해당 부분
            User user = userRepository.findByEmail(googleUser.getEmail())
                    .orElseGet(() -> {
                        System.out.println("신규 유저 생성 시작: " + googleUser.getEmail()); // 디버깅 로그
                        User newUser = new User();
                        newUser.setEmail(googleUser.getEmail());
                        newUser.setName(googleUser.getName());
                        newUser.setGoogleId(googleUser.getGoogleId());
                        newUser.setPicture(googleUser.getPicture());

                        // ✅ 본인의 이메일 주소를 입력하세요.
                        if ("startea0716@gmail.com".equals(googleUser.getEmail())) {
                            newUser.setRole("ADMIN"); // 관리자 부여
                        } else {
                            newUser.setRole("USER");  // 일반 유저
                        }

                        return userRepository.save(newUser);
                    });
            // 4. 우리 서버 전용 JWT 토큰 발급
            String token = jwtService.createServerToken(user);

            System.out.println("로그인 시도 유저: " + user.getEmail());
            System.out.println("DB에서 가져온 Role: " + user.getRole()); // 👈 여기서 null이 찍히는지 확인!

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", user.getRole() // null일 경우를 대비해 String.valueOf 사용
            ));
        } catch (Exception e) {
            e.printStackTrace(); // 서버 로그에서 에러 원인을 확인하기 위해 추가
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류: " + e.getMessage());
        }
    }
}