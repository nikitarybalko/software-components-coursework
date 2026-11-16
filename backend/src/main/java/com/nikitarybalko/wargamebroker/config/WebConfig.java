package com.nikitarybalko.wargamebroker.config;

import com.nikitarybalko.wargamebroker.web.ApiPaths;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;
import com.nikitarybalko.wargamebroker.web.security.RoleAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ApiKeyProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private static final String ALL_PATHS = "/**";
    private static final String ANY_ORIGIN = "*";
    private static final List<String> ALLOWED_HEADERS = List.of(
            HttpHeaders.ORIGIN, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT,
            HttpHeaders.AUTHORIZATION, ApiKeyAuthFilter.API_KEY_HEADER);
    private static final List<String> ALLOWED_METHODS = Stream.of(
                    HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.OPTIONS)
            .map(HttpMethod::name)
            .toList();

    private final RoleAuthorizationInterceptor roleAuthorizationInterceptor;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of(ANY_ORIGIN));
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setAllowedMethods(ALLOWED_METHODS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ALL_PATHS, config);

        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleAuthorizationInterceptor).addPathPatterns(ApiPaths.API_PATTERN);
    }
}
