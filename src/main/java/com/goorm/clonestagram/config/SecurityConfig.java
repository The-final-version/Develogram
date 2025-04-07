package com.goorm.clonestagram.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.goorm.clonestagram.common.jwt.JwtAuthenticationFilter;
import com.goorm.clonestagram.common.jwt.JwtTokenProvider;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;
import com.goorm.clonestagram.util.CustomUserDetails;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // (선택) @PreAuthorize 등 메서드 보안 사용시
public class SecurityConfig {

    private final UserInternalQueryService userQueryService;
    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(UserInternalQueryService userQueryService,
        JwtTokenProvider jwtTokenProvider) {
        this.userQueryService = userQueryService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 비밀번호 암호화
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager (유저 정보 가져오는 로직과 패스워드 인코더 설정)
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder
            .userDetailsService(email -> new CustomUserDetails(userQueryService.findByEmail(email)))
            .passwordEncoder(bCryptPasswordEncoder());
        return authBuilder.build();
    }

    /**
     * Security Filter Chain 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // [1] CSRF 비활성화
            .csrf(csrf -> csrf.disable())

            // [2] CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // [3] 세션 정책: JWT 사용 시 STATELESS 권장
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)                     // 동시 세션을 1개로 제한
                .maxSessionsPreventsLogin(true)         // true면 새로운 로그인 거부 (동시 로그인 불가)
            )

            // [4] 요청에 대한 권한 체크
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    "/login", "/join",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger.html",
                    "/search/tag/suggestions",
                    "/search/tag",
                    "/me"
                ).permitAll()
                .anyRequest().authenticated()
            )

            // [5] 로그아웃 설정
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                })
            )

            // [6] 폼 로그인 / httpBasic 비활성화 (JWT만 사용하므로)
            .formLogin(Customizer.withDefaults())
            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable());

        // [7] JWT 필터 적용
        http.addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 주소 등 허용 도메인
        // 예: Vite 개발 서버 (localhost:5173)
        configuration.setAllowedOriginPatterns(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // 세션 쿠키 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
