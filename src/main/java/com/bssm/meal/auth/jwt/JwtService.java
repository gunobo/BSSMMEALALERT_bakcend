package com.bssm.meal.auth.jwt;

import com.bssm.meal.user.domain.User;
import com.bssm.meal.user.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value; // Import 확인!
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Service
public class JwtService {

    private final UserRepository userRepository;

    // yml의 jwt.client-id 값을 가져옵니다.
    @Value("${jwt.client-id}")
    private String GOOGLE_CLIENT_ID;

    // yml의 jwt.secret 값을 가져와서 SecretKey 객체를 만듭니다.
    private final SecretKey SECRET_KEY;

    // 만료 시간 설정도 yml에서 가져오면 좋습니다.
    @Value("${jwt.access-expire-millis}")
    private long ACCESS_TOKEN_EXPIRATION;

    // 생성자 주입 방식으로 SecretKey 초기화
    public JwtService(UserRepository userRepository, @Value("${jwt.secret}") String secret) {
        this.userRepository = userRepository;
        this.SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // [필터용] 토큰에서 이메일 추출
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // [필터용] 토큰 유효성 검사
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // [구글 로그인용] 토큰 검증 및 DB 저장
    public User verifyAndGetUser(String googleToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID)) // 주입받은 ID 사용
                    .build();

            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String picture = (String) payload.get("picture");
                String googleId = payload.getSubject(); // 구글 고유 ID (sub)

                return userRepository.findByEmail(email)
                        .orElseGet(() -> userRepository.save(
                                User.builder()
                                        .email(email)
                                        .name(name)
                                        .picture(picture)
                                        .googleId(googleId)
                                        .build()
                        ));
            }
        } catch (Exception e) {
            System.err.println("구글 검증 에러: " + e.getMessage());
        }
        return null;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // JwtService 내부에 추가
    public String createServerToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 24시간
                .signWith(SECRET_KEY)
                .compact();
    }

    // JwtService.java (또는 JwtProvider)
    public String extractRole(String token) {
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            // "role":"ADMIN" 또는 "role":"ROLE_ADMIN"을 찾는 더 안전한 방법
            if (payloadJson.contains("\"role\":\"ADMIN\"")) return "ROLE_ADMIN";
            if (payloadJson.contains("\"role\":\"ROLE_ADMIN\"")) return "ROLE_ADMIN";
            return "ROLE_USER";
        } catch (Exception e) {
            return "ROLE_USER";
        }
    }
}