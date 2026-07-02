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

        // 🚀 Force client alignment with dynamic request credentials
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList("https://bingetogether.vercel.app"));
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization", "ngrok-skip-browser-warning"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Expose critical handshake headers
        config.setExposedHeaders(Arrays.asList("ngrok-skip-browser-warning"));

        // This ensures endpoints including web sockets get processed globally
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}