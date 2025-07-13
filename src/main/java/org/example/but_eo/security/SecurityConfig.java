package org.example.but_eo.security;

import java.util.List;

import org.example.but_eo.service.CustomOAuth2UserService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .authorizeHttpRequests((authorizeRequests) ->authorizeRequests
                        .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
                        .requestMatchers(PathRequest.toH2Console()).permitAll() //h2-console 기본 경로 접근 허용
                        .requestMatchers("/", "/login/**").permitAll() //로그인 화면이랑 메인 화면 접근 허용
                        .requestMatchers("/api/users/login", "/api/users/register").permitAll() //로그인이랑 회원가입만 허용 나머진 인증필요
                        .anyRequest().authenticated()
                )

                .formLogin(login -> login
                        .loginPage("/login") // 기본 로그인 페이지
                        .defaultSuccessUrl("/", true) // 로그인 성공 시 이동 페이지
                        .permitAll()
                )

                .oauth2Login(oauth2 -> oauth2   //
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // OAuth2UserService 연결
                        )
                        .successHandler(customOAuth2SuccessHandler)
                        .defaultSuccessUrl("/", true) // 로그인 성공 후 이동할 URL
                        .failureUrl("/login?error")   // 실패 시 이동할 URL
                )

                .sessionManagement(session  //서버가 세션을 사용하지 않고 JWT 기반으로 인증하도록
                        -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf((csrfConfig) -> csrfConfig.disable()) //JWT OAuth2사용하기 위해 csrf 비활성화 및 h2-console 사용하기 위함
                .headers((headerConfig) ->
                        headerConfig.frameOptions((frameOptionsConfig) ->
                                frameOptionsConfig.disable())
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        ; //h2가 iframe 사용해야하므로 X-frame-option 비활성화
        return http.build();
    }


    //프론트 엔드 연결 메소드
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        //TODO : 여기 안에 프론트 엔드 주소 넣어야함
        //config.setAllowedOrigins(List.of("")); //허용할 도메인
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH")); //허용할 HTTP 메소드
        config.setAllowedHeaders(List.of("Authorization", "Content-Type")); //허용할 헤더
        config.setAllowCredentials(true); //인증 정보 포함 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
