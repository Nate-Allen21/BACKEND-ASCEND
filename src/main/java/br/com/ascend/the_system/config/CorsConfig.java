package br.com.ascend.the_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    @Bean
    CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of());

        List<String> allowedOriginPatterns = new ArrayList<>(List.of(
                "http://localhost",
                "https://localhost",
                "http://127.0.0.1",
                "http://localhost:[*]",
                "https://localhost:[*]",
                "http://127.0.0.1:[*]",
                "https://ascend22.vercel.app"
        ));

        // Private-network development origins are opt-in, not production defaults.
        if (Boolean.parseBoolean(System.getenv("ASCEND_CORS_ALLOW_DEV_ORIGINS"))) {
            allowedOriginPatterns.addAll(List.of(
                "http://192.168.*:[*]",
                "http://10.*:[*]",
                "http://172.16.*:[*]",
                "http://172.17.*:[*]",
                "http://172.18.*:[*]",
                "http://172.19.*:[*]",
                "http://172.20.*:[*]",
                "http://172.21.*:[*]",
                "http://172.22.*:[*]",
                "http://172.23.*:[*]",
                "http://172.24.*:[*]",
                "http://172.25.*:[*]",
                "http://172.26.*:[*]",
                "http://172.27.*:[*]",
                "http://172.28.*:[*]",
                "http://172.29.*:[*]",
                "http://172.30.*:[*]",
                "http://172.31.*:[*]"
            ));
        }

        String customOrigins = System.getenv("ASCEND_CORS_ALLOWED_ORIGINS");
        if (customOrigins != null && !customOrigins.isBlank()) {
            allowedOriginPatterns.addAll(
                    Arrays.stream(customOrigins.split(","))
                            .map(String::trim)
                            .filter(value -> !value.isEmpty())
                            .collect(Collectors.toList())
            );
        }

        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return new CorsFilter(source);
    }
}
