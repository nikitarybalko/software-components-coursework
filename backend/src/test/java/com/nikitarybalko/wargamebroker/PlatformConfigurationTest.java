package com.nikitarybalko.wargamebroker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.nikitarybalko.wargamebroker.support.TestCases.TC_NFR1_01;
import static com.nikitarybalko.wargamebroker.support.TestCases.TC_NFR2_01;
import static com.nikitarybalko.wargamebroker.support.TestCases.TC_NFR4_02;
import static com.nikitarybalko.wargamebroker.support.TestCases.TC_NFR6_01;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static checks of the platform and deployment configuration; paths are relative to the backend module.
 */
@DisplayName("Configuration tests: platform and deployment")
class PlatformConfigurationTest {

    private static final Path COMPOSE_FILE = Path.of("..", "docker-compose.yml");
    private static final Path BACKEND_DOCKERFILE = Path.of("Dockerfile");
    private static final Path APPLICATION_CONFIG = Path.of("src", "main", "resources", "application.yml");

    private static final int REQUIRED_JAVA_VERSION = 21;
    private static final String REQUIRED_SPRING_BOOT_LINE = "3.4.";
    private static final List<String> REQUIRED_SERVICES = List.of("postgres", "backend", "dashboard", "simulators");
    private static final String BACKEND = "backend";
    private static final String POSTGRES = "postgres";
    private static final String HEALTHY_CONDITION = "service_healthy";
    private static final String JVM_CONTAINER_MEMORY_FLAG = "-XX:MaxRAMPercentage";
    private static final String POSTGRES_DRIVER = "org.postgresql.Driver";
    private static final String POSTGRES_JDBC_PREFIX = "jdbc:postgresql://";

    @Test
    @Tag(TC_NFR1_01)
    @DisplayName("Backend runs on Java 21")
    void runsOnJava21() {
        assertEquals(REQUIRED_JAVA_VERSION, Runtime.version().feature());
    }

    @Test
    @Tag(TC_NFR1_01)
    @DisplayName("Backend is built on Spring Boot 3.4")
    void usesSpringBoot34() {
        assertTrue(SpringBootVersion.getVersion().startsWith(REQUIRED_SPRING_BOOT_LINE),
                "Unexpected Spring Boot version: " + SpringBootVersion.getVersion());
    }

    @Test
    @Tag(TC_NFR2_01)
    @DisplayName("Application datasource is configured for PostgreSQL")
    void datasourceUsesPostgresql() throws IOException {
        Map<String, Object> datasource = section(readYaml(APPLICATION_CONFIG), "spring", "datasource");

        assertEquals(POSTGRES_DRIVER, datasource.get("driver-class-name"));
        assertTrue(String.valueOf(datasource.get("url")).contains(POSTGRES_JDBC_PREFIX));
    }

    @Test
    @Tag(TC_NFR4_02)
    @DisplayName("docker-compose defines every service and starts the backend after PostgreSQL is healthy")
    void composeDefinesAllServices() throws IOException {
        Map<String, Object> services = section(readYaml(COMPOSE_FILE), "services");

        assertTrue(services.keySet().containsAll(REQUIRED_SERVICES), "Services in compose file: " + services.keySet());
        Map<String, Object> postgresDependency = section(services, BACKEND, "depends_on", POSTGRES);
        assertEquals(HEALTHY_CONDITION, postgresDependency.get("condition"));
    }

    @Test
    @Tag(TC_NFR6_01)
    @DisplayName("Backend container has a memory limit")
    void backendContainerHasMemoryLimit() throws IOException {
        Map<String, Object> limits = section(readYaml(COMPOSE_FILE), "services", BACKEND, "deploy", "resources", "limits");

        assertFalse(String.valueOf(limits.get("memory")).isBlank());
    }

    @Test
    @Tag(TC_NFR6_01)
    @DisplayName("Backend JVM sizes its heap from the container memory limit")
    void backendJvmRespectsContainerMemoryLimit() throws IOException {
        assertTrue(Files.readString(BACKEND_DOCKERFILE).contains(JVM_CONTAINER_MEMORY_FLAG));
    }

    private static Map<String, Object> readYaml(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return new Yaml().load(reader);
        }
    }

    private static Map<String, Object> section(Map<String, Object> root, String... keys) {
        return Arrays.stream(keys).reduce(root, (current, key) -> child(current, key, keys), (left, right) -> right);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> child(Map<String, Object> parent, String key, String... path) {
        Object next = parent.get(key);
        assertTrue(next instanceof Map, "Missing configuration section: " + String.join(".", path));
        return (Map<String, Object>) next;
    }
}
