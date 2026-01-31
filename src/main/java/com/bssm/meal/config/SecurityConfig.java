package com.bssm.meal.config;

import com.bssm.meal.auth.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;

    // ✅ [해결책 1] 시큐리티의 모든 필터(JWT 포함)를 아예 거치지 않도록 완전 제외
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/api/notifications/subscribe/**")
                .requestMatchers("/uploads/**", "/favicon.ico");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // ✅ [해결책 2] CORS 설정을 필터 체인 최상단에서 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 로그인 및 인증 관련 경로
                        .requestMatchers("/auth/**", "/oauth2/**", "/login/**", "/api/auth/**").permitAll()
                        // 2. 급식 및 랭킹 (누구나 조회 가능)
                        .requestMatchers("/api/meals/**", "/api/likes/ranking", "/api/main/**").permitAll()
                        // 3. 알림 (최근 공지 조회 등)
                        .requestMatchers("/api/notifications/latest", "/api/notifications/subscribe/**").permitAll()

                        // 4. 관리자 전용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 5. 유저 인증 필수 경로
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/likes/toggle").authenticated()
                        .requestMatchers("/api/likes/user/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reports").authenticated()

                        // 6. 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                // JWT 필터 위치 지정
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드 주소 (정확히 명시)
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        // SSE 통신을 위한 필수 헤더 노출
        configuration.setExposedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "Last-Event-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}