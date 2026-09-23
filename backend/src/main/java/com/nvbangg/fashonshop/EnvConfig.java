package com.nvbangg.fashonshop;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

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
        setIfPresent("PRODUCT_IMAGES_DIR", dotenv.get("PRODUCT_IMAGES_DIR"));
        setIfPresent("PRODUCT_IMAGE_MAX_SIZE_BYTES", dotenv.get("PRODUCT_IMAGE_MAX_SIZE_BYTES"));
        setIfPresent("PAYOS_CLIENT_ID", dotenv.get("PAYOS_CLIENT_ID"));
        setIfPresent("PAYOS_API_KEY", dotenv.get("PAYOS_API_KEY"));
        setIfPresent("PAYOS_CHECKSUM_KEY", dotenv.get("PAYOS_CHECKSUM_KEY"));
        setIfPresent("FRONTEND_URL", dotenv.get("FRONTEND_URL"));
    }

    private static void setIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            System.setProperty(key, value);
        }
    }
}