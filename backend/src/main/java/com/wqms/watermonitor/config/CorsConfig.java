package com.wqms.watermonitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration allowing the frontend dashboard (served separately,
 * e.g. via a static file server or a different port) and the ESP32 to
 * call the backend REST APIs without being blocked by the browser's
 * same-origin policy.
 *
 * NOTE: "*" is used here for local development convenience. Restrict
 * this to your actual frontend origin(s) before deploying to production.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
