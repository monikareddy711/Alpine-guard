package io.alpineguard.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.alpineguard.hazard.HazardProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({SpatialProperties.class, HazardProperties.class})
public class AppConfig implements WebMvcConfigurer {

    @Value("${alpineguard.cache.telemetry-ttl-seconds:60}")
    private long telemetryTtl;

    @Value("${alpineguard.cache.hazard-ttl-seconds:30}")
    private long hazardTtl;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("telemetry", "hazard", "routes");
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(2_000).expireAfterWrite(Duration.ofSeconds(hazardTtl)));
        manager.registerCustomCache("telemetry", Caffeine.newBuilder()
                .maximumSize(2_000).expireAfterWrite(Duration.ofSeconds(telemetryTtl)).build());
        return manager;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200", "http://localhost:8080")
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
