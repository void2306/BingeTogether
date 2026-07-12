package com.sakshi.bingetogetherbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GlobalCorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        // 🌐 FIXED: Added localhost:5173 along with your production domains to stop MVC blocks
                        .allowedOrigins(
                                "http://localhost:5173",
                                "http://localhost:3000",
                                "https://bingetogether.vercel.app",
                                "https://mortuary-panda-panning.ngrok-free.dev"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}