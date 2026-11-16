package com.nikitarybalko.wargamebroker.config;

import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "ApiKeyAuth";

    private static final String API_TITLE = "Wargame Broker REST API";
    private static final String API_VERSION = "1.0.0";
    private static final String API_LICENSE = "Apache 2.0";
    private static final String API_DESCRIPTION = "Microservice broker for routing, spatial visibility filtering (Fog of War) "
            + "and barrier synchronization between independent tactical simulators.";
    private static final String API_KEY_DESCRIPTION = "API key of the Blue force, the Red force or the administrator";

    @Bean
    public OpenAPI wargameOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(API_TITLE)
                        .description(API_DESCRIPTION)
                        .version(API_VERSION)
                        .license(new License().name(API_LICENSE)))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(ApiKeyAuthFilter.API_KEY_HEADER)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description(API_KEY_DESCRIPTION)));
    }
}
