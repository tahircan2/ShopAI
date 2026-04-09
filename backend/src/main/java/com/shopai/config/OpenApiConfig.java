package com.shopai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopAI E-Commerce API")
                        .version("3.0.0")
                        .description("""
                                ShopAI REST API — Security-Hardened Edition.
                                
                                **Kimlik Doğrulama:** JWT token HttpOnly cookie olarak taşınır.
                                Authorization header kullanılmaz. Tüm isteklerde
                                `credentials: include` (withCredentials: true) gereklidir.
                                
                                **Güvenlik:** Token localStorage'da saklanmaz.
                                Cookie'ler otomatik taşınır — XSS saldırısında token çalınamaz.
                                """)
                        .contact(new Contact().name("ShopAI Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development"),
                        new Server().url("https://api.shopai.com").description("Production")
                ));
    }
}
