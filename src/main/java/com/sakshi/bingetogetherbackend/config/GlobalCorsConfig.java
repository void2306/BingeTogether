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

        config.setAllowCredentials(true);
        // 🔑 Both variants of your domain are explicitly covered
        config.setAllowedOrigins(Arrays.asList(
                "https://bingetogether.vercel.app",
                "https://bingetogether-bxintnbmf-void0623.vercel.app"
        ));
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization", "ngrok-skip-browser-warning"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setExposedHeaders(Arrays.asList("ngrok-skip-browser-warning"));

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}