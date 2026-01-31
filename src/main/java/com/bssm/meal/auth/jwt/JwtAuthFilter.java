package com.bssm.meal.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    // ✅ [중요] SSE 구독 경로는 JWT 검증 필터를 아예 거치지 않게 설정 (403 에러 해결 핵심)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/api/notifications/subscribe/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String token;
        final String email;

        // 1. 헤더 검증 (Bearer 토큰이 없으면 다음 필터로 넘김)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        token = authHeader.substring(7);

        try {
            // 2. 토큰에서 이메일 추출
            email = jwtService.extractEmail(token);

            // SecurityContext에 인증 정보가 없을 때만 진행
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (jwtService.isTokenValid(token)) {
                    String role = jwtService.extractRole(token);

                    // 3. 권한 포맷팅 (ROLE_ 접두사 확인)
                    if (role == null || role.isEmpty()) {
                        role = "USER";
                    }
                    if (!role.startsWith("ROLE_")) {
                        role = "ROLE_" + role;
                    }

                    List<SimpleGrantedAuthority> authorities =
                            Collections.singletonList(new SimpleGrantedAuthority(role));

                    // 4. 인증 객체 생성
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );

                    // 요청 상세 정보 설정
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. SecurityContext에 인증 정보 저장
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.info("✅ [JWT 인증 성공] 이메일: {}, 권한: {}", email, role);
                }
            }
        } catch (Exception e) {
            log.error("❌ [JWT 인증 에러] : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}