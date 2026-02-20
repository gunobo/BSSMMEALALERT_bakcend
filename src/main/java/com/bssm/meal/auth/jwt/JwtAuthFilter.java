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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // getServletPath() 대신 getRequestURI()를 사용하는 것이 Nginx 환경에서 더 정확합니다.
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 인증이 필요 없는 경로들 (업로드 경로는 여기서 스킵하면 안 됩니다. 권한 체크를 해야 하니까요!)
        return path.startsWith("/auth/") ||
                path.startsWith("/api/auth/") ||
                path.startsWith("/api/notifications/subscribe/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();

        // 🔍 로그: 모든 요청의 헤더 상태 확인
        log.info("📩 [요청 유입] URI: {}, Method: {}, AuthHeader 존재여부: {}",
                requestURI, request.getMethod(), (authHeader != null));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtService.isTokenValid(token)) {
                    String role = jwtService.extractRole(token);

                    // ROLE_ 접두사 처리 (매우 중요)
                    if (role == null || role.trim().isEmpty()) {
                        role = "USER";
                    }
                    String finalRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                    log.info("👤 [토큰 검증 성공] User: {}, Role: {}, Path: {}", email, finalRole, requestURI);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(finalRole))
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.warn("⚠️ [토큰 만료/유효하지 않음] Path: {}", requestURI);
                }
            }
        } catch (Exception e) {
            log.error("❌ [인증 내부 에러] Path: {}, Error: {}", requestURI, e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}