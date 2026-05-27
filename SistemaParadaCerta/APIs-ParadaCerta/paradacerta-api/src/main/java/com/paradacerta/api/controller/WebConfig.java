package com.paradacerta.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Configurações web globais:
 *
 * <h2>CORS</h2>
 * O painel web roda em http://localhost:5500 (Live Server / python http.server).
 * O app mobile chama por IP local; nesse caso o navegador não está envolvido,
 * então CORS não impede.
 *
 * <h2>Recursos estáticos</h2>
 * As fotos enviadas pelo upload (item 3 do backlog) são gravadas em disco em
 * {@code paradacerta.uploads.dir} e servidas em {@code /uploads/**}.
 *
 * <h2>RestTemplate</h2>
 * Cliente HTTP usado por {@link com.paradacerta.api.service.ModeracaoImagemService}
 * para falar com o Google Vision SafeSearch. Timeout curto (8 s) — falha => fail-closed.
 */
@Configuration
public class WebConfig {

    @Value("${paradacerta.uploads.dir:./uploads}")
    private String uploadsDir;

    @Bean
    public WebMvcConfigurer corsAndStaticConfigurer() {
        // Resolve uma única vez para garantir caminho absoluto consistente entre handlers.
        final Path uploadsPath = Paths.get(uploadsDir).toAbsolutePath().normalize();

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns(
                                "http://localhost:5500",
                                "http://127.0.0.1:5500",
                                "http://localhost:5501",
                                "http://127.0.0.1:5501",
                                "https://parada-certa-murex.vercel.app",
                                "https://*.vercel.app"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false);

                // As fotos servidas em /uploads/** podem ser carregadas por qualquer painel/admin.
                registry.addMapping("/uploads/**")
                        .allowedOriginPatterns(
                                "http://localhost:5500",
                                "http://127.0.0.1:5500",
                                "http://localhost:5501",
                                "http://127.0.0.1:5501",
                                "https://parada-certa-murex.vercel.app",
                                "https://*.vercel.app"
                        )
                        .allowedMethods("GET", "OPTIONS")
                        .allowCredentials(false);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String location = "file:" + uploadsPath.toString().replace("\\", "/") + "/";
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations(location);
            }
        };
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
    }
}
