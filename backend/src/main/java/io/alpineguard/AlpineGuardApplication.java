package io.alpineguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AlpineGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlpineGuardApplication.class, args);
    }
}
