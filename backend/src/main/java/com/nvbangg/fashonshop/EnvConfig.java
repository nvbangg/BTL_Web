package com.nvbangg.fashonshop;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class EnvConfig {
    static {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
        setIfPresent("DB_USERNAME", dotenv.get("DB_USERNAME"));
        setIfPresent("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
        setIfPresent("DB_URL", dotenv.get("DB_URL"));
        setIfPresent("JWT_SECRET", dotenv.get("JWT_SECRET"));
        setIfPresent("JWT_EXPIRATION_SECONDS", dotenv.get("JWT_EXPIRATION_SECONDS"));
    }

    private static void setIfPresent(String key, String value) {
        if (!Objects.isNull(value) && !value.isBlank()) {
            System.setProperty(key, value);
        }
    }
}