package com.nikitarybalko.wargamebroker.support.traceability;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Requirements and test cases kept in docs/testing as the single source for the traceability matrix.
 */
public record TraceabilityCatalogue(List<Requirement> requirements, List<TestCaseSpec> testCases) {

    public static final String CATALOGUE_DIR_PROPERTY = "traceability.catalogue.dir";
    public static final String AUTOMATED = "automated";

    private static final Path DEFAULT_CATALOGUE_DIR = Path.of("..", "docs", "testing");
    private static final String REQUIREMENTS_FILE = "requirements.yaml";
    private static final String TEST_CASES_FILE = "test-cases.yaml";

    public record Requirement(String id, String type, String subsystem, String description) {
    }

    public record TestCaseSpec(String id, String title, List<String> requirements, String level, String automation,
                               String preconditions, List<String> steps, String expected) {

        public boolean isAutomated() {
            return AUTOMATED.equals(automation);
        }
    }

    public static TraceabilityCatalogue load() {
        String configured = System.getProperty(CATALOGUE_DIR_PROPERTY);
        return load(Objects.isNull(configured) ? DEFAULT_CATALOGUE_DIR : Path.of(configured));
    }

    public static TraceabilityCatalogue load(Path directory) {
        List<Requirement> requirements = readList(directory.resolve(REQUIREMENTS_FILE)).stream()
                .map(TraceabilityCatalogue::toRequirement)
                .toList();
        List<TestCaseSpec> testCases = readList(directory.resolve(TEST_CASES_FILE)).stream()
                .map(TraceabilityCatalogue::toTestCase)
                .toList();
        return new TraceabilityCatalogue(requirements, testCases);
    }

    public List<TestCaseSpec> testCasesOf(String requirementId) {
        return testCases.stream()
                .filter(testCase -> testCase.requirements().contains(requirementId))
                .toList();
    }

    private static List<Map<String, Object>> readList(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return new Yaml().load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read traceability catalogue " + file.toAbsolutePath(), e);
        }
    }

    private static Requirement toRequirement(Map<String, Object> entry) {
        return new Requirement(text(entry, "id"), text(entry, "type"), text(entry, "subsystem"), text(entry, "description"));
    }

    private static TestCaseSpec toTestCase(Map<String, Object> entry) {
        return new TestCaseSpec(text(entry, "id"), text(entry, "title"), list(entry, "requirements"), text(entry, "level"),
                text(entry, "automation"), text(entry, "preconditions"), list(entry, "steps"), text(entry, "expected"));
    }

    private static String text(Map<String, Object> entry, String key) {
        return Objects.toString(entry.get(key), "").strip();
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Map<String, Object> entry, String key) {
        Object value = entry.get(key);
        return value instanceof List<?> items ? ((List<Object>) items).stream().map(String::valueOf).toList() : List.of();
    }
}
