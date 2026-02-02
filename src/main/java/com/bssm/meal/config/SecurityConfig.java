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
//                .csrf(csrf -> csrf.disable())
                // CORS 설정을 filterChain 상단에 확실히 명시
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 공개 허용 경로
                        .requestMatchers("/", "/error", "/auth/**", "/oauth2/**", "/login/**", "/api/auth/**").permitAll()
                        .requestMatchers("/api/meals/**", "/api/likes/ranking", "/api/main/**").permitAll()
                        .requestMatchers("/api/notifications/latest", "/api/notifications/subscribe/**").permitAll()

                        // 2. 권한 필요 경로
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/**", "/api/likes/toggle", "/api/likes/user/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reports").authenticated()

                        // 3. 기타 모든 요청 허용 (성공 확인 후 .authenticated()로 변경 권장)
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // [핵심 수정] 실제 요청이 오는 도메인들을 정확하게 명시
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://bssm.imjemin.co.kr",    // 프론트엔드 도메인
                "https://api.imjemin.co.kr"     // 백엔드 도메인
        ));

        // [중요] OPTIONS 메서드가 포함되어야 Preflight 요청을 통과함
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 모든 헤더 허용
        configuration.setAllowedHeaders(List.of("*"));

        // 쿠키 및 세션 인증 허용 (true 필수)
        configuration.setAllowCredentials(true);

        // 브라우저에서 접근 가능한 응답 헤더 설정
        configuration.setExposedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "Last-Event-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로(/**)에 대해 위 설정 적용
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}