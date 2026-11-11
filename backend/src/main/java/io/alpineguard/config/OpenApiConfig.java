package io.alpineguard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alpineGuardOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AlpineGuard API")
                .version("1.0.0")
                .description("Snowpack telemetry, weather station proximity and ski tour hazard assessment "
                        + "for the Tyrol and Graubünden test regions.")
                .license(new License().name("CC BY 4.0").url("https://creativecommons.org/licenses/by/4.0/")));
    }
}
