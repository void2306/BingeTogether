package com.sakshi.bingetogetherbackend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable standard CSRF since tokens protect us naturally from forge attacks
                .csrf(csrf -> csrf.disable())

                // 2. Enable clean cross-origin resource sharing properties matching your front-end ports
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))

                // 3. Define explicit endpoint access permissions
                .authorizeHttpRequests(auth -> auth
                        // 🔥 CRITICAL SAFETY GATES: Allow open access to your login, registration, and WebSocket handshake endpoint paths!
                        .requestMatchers("/auth/**", "/room/create", "/ws-binge/**", "/error").permitAll()
                        // Any other private operational data endpoint will strictly require token authorization verification
                        .anyRequest().authenticated()
                )

                // 4. Force stateless sessions. Don't create server session database logs!
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. Wire up our custom JWT verification guard scanner right before the main authentication engine
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}