package com.severus.finance.configuration;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "app")
public interface AppConfig {

    SingleUser singleUser();
    Jwt jwt();

    interface SingleUser {
        @WithDefault("severus.technology@gmail.com")
        String email();

        @WithDefault("Severus@Finance2026!")
        String password();
    }

    interface Jwt {
        @WithDefault("super_secret_jwt_key_at_least_32_bytes_long_severus_finance_2026")
        String secret();
    }
}
