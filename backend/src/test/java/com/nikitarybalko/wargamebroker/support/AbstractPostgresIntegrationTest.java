package com.nikitarybalko.wargamebroker.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a single PostgreSQL container shared by every integration test class in the JVM,
 * so cached Spring contexts keep a live database between test classes.
 */
@ActiveProfiles(TestProfiles.TEST)
public abstract class AbstractPostgresIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final String DATABASE_NAME = "wargamedb";

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                    .withDatabaseName(DATABASE_NAME);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
