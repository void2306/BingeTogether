package com.sakshi.bingetogetherbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(false); // Ngrok dynamic domains ke liye false safe hai
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // 🔑 Browser ko allow karta hai ki woh Ngrok ke warning headers ko bypass kar sake
        config.setExposedHeaders(Arrays.asList("ngrok-skip-browser-warning"));

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}