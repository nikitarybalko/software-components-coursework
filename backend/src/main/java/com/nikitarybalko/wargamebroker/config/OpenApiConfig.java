package com.nikitarybalko.wargamebroker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "ApiKeyAuth";

    @Bean
    public OpenAPI wargameOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wargame Broker REST API")
                        .description("Microservice broker for routing, spatial visibility filtering (Fog of War) " +
                                "and barrier synchronization between independent tactical simulators.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Coursework Project")
                                .email("student@kpi.ua"))
                        .license(new License().name("Apache 2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name("X-API-KEY")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Authentication token (blue-secret-token, red-secret-token, admin-secret-token)")));
    }
}
