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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        String method = request.getMethod();

        // 1. OPTIONS 요청은 인증 필터를 거치지 않음 (CORS Preflight 지원)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 2. 인증이 필요 없는 경로는 필터 로직 스킵
        return path.startsWith("/auth/") ||
                path.startsWith("/api/auth/") ||
                path.startsWith("/api/notifications/subscribe/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 헤더 검증: Bearer 토큰이 없으면 다음 필터로 이동 (SecurityConfig의 permitAll 경로들을 위함)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final String email = jwtService.extractEmail(token);

            // 이메일이 존재하고 아직 SecurityContext에 인증 정보가 없는 경우
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtService.isTokenValid(token)) {
                    // 토큰에서 Role 추출
                    String role = jwtService.extractRole(token);

                    // 🔍 디버깅 로그: 403 에러 발생 시 서버 콘솔에서 이 로그를 반드시 확인하세요!
                    log.info("🔍 [토큰 검증] Path: {}, User: {}, Extracted Role: {}",
                            request.getServletPath(), email, role);

                    // Role 값이 없을 경우 기본값 부여 및 ROLE_ 접두사 처리
                    if (role == null || role.trim().isEmpty()) {
                        role = "USER";
                    }

                    // Spring Security의 hasRole("ADMIN")은 "ROLE_ADMIN"을 검사하므로 접두사 확인
                    String finalRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(finalRole))
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.info("✅ [인증 완료] 부여된 권한: {}", finalRole);
                } else {
                    log.warn("⚠️ [인증 실패] 유효하지 않은 토큰입니다. Path: {}", request.getServletPath());
                }
            }
        } catch (Exception e) {
            log.error("❌ [인증 에러] Path: {}, Error: {}", request.getServletPath(), e.getMessage());
            // 에러 발생 시 컨텍스트 클리어 (안전 장치)
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}