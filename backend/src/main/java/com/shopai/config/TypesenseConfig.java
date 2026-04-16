package com.shopai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.typesense.api.Client;
import org.typesense.resources.Node;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Typesense arama motoru yapılandırması.
 * application.yml içindeki typesense.* ayarlarını okur ve
 * Typesense Client bean'i oluşturur.
 */
@Configuration
@ConfigurationProperties(prefix = "typesense")
@Getter
@Setter
public class TypesenseConfig {

    private boolean enabled = true;
    private String protocol = "http";
    private String host = "localhost";
    private String port = "8108";
    private String apiKey;
    private int connectionTimeoutSeconds = 5;

    @Bean
    public Client typesenseClient() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(protocol, host, port));

        // Fully qualified name kullanılıyor çünkü Spring @Configuration ile çakışıyor
        org.typesense.api.Configuration tsConfig = new org.typesense.api.Configuration(
                nodes,
                Duration.ofSeconds(connectionTimeoutSeconds),
                apiKey
        );

        return new Client(tsConfig);
    }
}

