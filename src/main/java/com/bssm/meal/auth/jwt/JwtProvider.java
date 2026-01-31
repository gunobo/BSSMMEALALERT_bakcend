package com.bssm.meal.auth.jwt;

import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class JwtProvider {

    private final String secret = "supersecretkey12345";
    private final long validity = 1000L * 60 * 60; // 1시간

    public String generateToken(String username, String role) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());

        long exp = System.currentTimeMillis() + validity;

        // ✅ JSON 포맷을 더 명확히 (role 추가)
        String payloadJson = String.format("{\"sub\":\"%s\",\"role\":\"%s\",\"exp\":%d}", username, role, exp);

        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes());

        String signature = sign(header + "." + payload);

        return header + "." + payload + "." + signature;
    }

    // ✅ 더 안전한 role 추출 로직
    public String getRole(String token) {
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));

            // 정규식 대신 수동 파싱 (더 안전함)
            if (payloadJson.contains("\"role\":\"")) {
                int start = payloadJson.indexOf("\"role\":\"") + 8;
                int end = payloadJson.indexOf("\"", start);
                String extractedRole = payloadJson.substring(start, end);

                // 🔍 디버깅 로그 (서버 콘솔에서 확인용)
                System.out.println("JWT 추출 원본 JSON: " + payloadJson);
                System.out.println("JWT 최종 추출 Role: " + extractedRole);

                return extractedRole;
            }
            return "USER";
        } catch (Exception e) {
            System.err.println("Role 추출 실패: " + e.getMessage());
            return "USER";
        }
    }

    // ✅ 만료 시간(exp) 추출도 안전하게 수정
    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String signature = sign(parts[0] + "." + parts[1]);
            if (!signature.equals(parts[2])) return false;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            // 수동으로 exp 숫자 추출
            int start = payloadJson.indexOf("\"exp\":") + 6;
            int end = payloadJson.indexOf(",", start);
            if (end == -1) end = payloadJson.indexOf("}", start);

            long exp = Long.parseLong(payloadJson.substring(start, end).trim());
            return exp > System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsername(String token) {
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            int start = payloadJson.indexOf("\"sub\":\"") + 7;
            int end = payloadJson.indexOf("\"", start);
            return payloadJson.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac.doFinal(data.getBytes()));
    }
}