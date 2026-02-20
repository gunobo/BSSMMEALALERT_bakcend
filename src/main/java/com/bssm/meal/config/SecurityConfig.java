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

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/", "/favicon.ico", "/error", "/uploads/**", "/static/**", "/css/**", "/js/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ============================================
                        // 1. [공개 경로] 최상단 배치
                        // ============================================
                        .requestMatchers("/api/auth/**", "/auth/**", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/meals/**", "/api/likes/ranking", "/api/main/**").permitAll()
                        .requestMatchers("/api/notifications/latest", "/api/notifications/subscribe/**").permitAll()
                        .requestMatchers("/api/admin/app/download/**").permitAll()

                        // ============================================
                        // 2. [관리자 전용] - ADMIN만 접근 가능
                        // ============================================

                        // 👥 사용자 관리 - 관리자만
                        .requestMatchers("/api/admin/users/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // 📱 앱 업로드 - 관리자만
                        .requestMatchers("/api/admin/app/upload", "/api/admin/app/stats")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // ============================================
                        // 3. [관리자 + 운영자] - ADMIN, MODERATOR 접근 가능
                        // ============================================

                        // 🚨 신고 관리
                        .requestMatchers("/api/admin/reports/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_MODERATOR", "MODERATOR")

                        // 🔔 알림 관리
                        .requestMatchers("/api/admin/notification/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_MODERATOR", "MODERATOR")

                        // 📊 통계 조회
                        .requestMatchers("/api/admin/stats/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_MODERATOR", "MODERATOR")

                        // ============================================
                        // 4. [나머지 관리자 경로] - 위에서 매칭 안된 경로
                        // ============================================
                        .requestMatchers("/api/admin/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_MODERATOR", "MODERATOR")

                        // ============================================
                        // 5. [사용자/관리자 공통]
                        // ============================================
                        .requestMatchers("/api/user/update-info").hasAnyRole("USER", "ADMIN")

                        // ============================================
                        // 6. [인증 필요 경로]
                        // ============================================
                        .requestMatchers("/api/fcm/**", "/api/users/fcm-token", "/api/user/**", "/api/likes/toggle", "/api/likes/user/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reports").authenticated()

                        // ============================================
                        // 7. [기타]
                        // ============================================
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://bssm.imjemin.co.kr",
                "https://api.imjemin.co.kr"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "Last-Event-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}