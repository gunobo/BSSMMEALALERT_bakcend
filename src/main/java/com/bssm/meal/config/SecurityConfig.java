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
        // 정적 자원들은 보안 필터를 아예 거치지 않도록 설정 (성능 최적화)
        return (web) -> web.ignoring()
                .requestMatchers("/", "/favicon.ico", "/error", "/uploads/**", "/static/**", "/css/**", "/js/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 세션 미사용 (JWT 방식)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS 요청은 모두 허용 (CORS Preflight 대응)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // [공개 경로] 인증 없이 접근 가능
                        .requestMatchers("/api/auth/**", "/auth/**", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/meals/**", "/api/likes/ranking", "/api/main/**").permitAll()
                        .requestMatchers("/api/notifications/latest", "/api/notifications/subscribe/**").permitAll()

                        // ✅ [앱 다운로드 경로] 누구나 클릭해서 다운로드할 수 있어야 함 (통계 집계용)
                        .requestMatchers("/admin/app/download/**").permitAll()

                        // ✅ [관리자 전용 앱 설정] 업로드 및 통계 조회는 ADMIN만 가능하게 설정
                        .requestMatchers("/admin/app/upload", "/admin/app/stats").hasRole("ADMIN")

                        // [관리자 알림 경로] 테스트 및 정상 작동을 위해 permitAll로 임시 개방
                        .requestMatchers("/api/admin/notification/**").permitAll()

                        // [사용자/관리자 공통]
                        .requestMatchers("/api/user/update-info").hasAnyRole("USER", "ADMIN")

                        // [인증 필요 경로]
                        .requestMatchers("/api/fcm/**").authenticated()
                        .requestMatchers("/api/users/fcm-token").authenticated()
                        .requestMatchers("/api/user/**", "/api/likes/toggle", "/api/likes/user/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reports").authenticated()

                        // [기타 관리자 경로]
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 그 외 모든 요청 허용 (성공 확인용)
                        .anyRequest().permitAll()
                )
                // 5. JWT 필터 추가
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 요청 허용 도메인
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