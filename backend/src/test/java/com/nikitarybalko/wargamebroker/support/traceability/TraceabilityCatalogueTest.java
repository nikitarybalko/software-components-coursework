package com.nikitarybalko.wargamebroker.support.traceability;

import com.nikitarybalko.wargamebroker.WargameBrokerApplication;
import com.nikitarybalko.wargamebroker.support.TestCases;
import com.nikitarybalko.wargamebroker.support.traceability.TraceabilityCatalogue.Requirement;
import com.nikitarybalko.wargamebroker.support.traceability.TraceabilityCatalogue.TestCaseSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestTag;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

@DisplayName("Consistency tests: requirements, test cases and test tags")
class TraceabilityCatalogueTest {

    private static TraceabilityCatalogue catalogue;
    private static Set<String> taggedTestCaseIds;

    @BeforeAll
    static void loadCatalogueAndDiscoverTests() {
        catalogue = TraceabilityCatalogue.load();
        TestPlan plan = LauncherFactory.create().discover(LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage(WargameBrokerApplication.class.getPackageName()))
                .build());
        taggedTestCaseIds = plan.getRoots().stream()
                .flatMap(root -> plan.getDescendants(root).stream())
                .filter(TestIdentifier::isTest)
                .flatMap(test -> test.getTags().stream())
                .map(TestTag::getName)
                .filter(tag -> tag.startsWith(TraceabilityReportListener.TEST_CASE_TAG_PREFIX))
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Requirement and test case identifiers are unique")
    void identifiersAreUnique() {
        assertEquals(List.of(), duplicates(catalogue.requirements(), Requirement::id));
        assertEquals(List.of(), duplicates(catalogue.testCases(), TestCaseSpec::id));
    }

    @Test
    @DisplayName("Every test case references only known requirements")
    void testCasesReferenceKnownRequirements() {
        Set<String> requirementIds = ids(catalogue.requirements(), Requirement::id);
        List<String> unknown = catalogue.testCases().stream()
                .flatMap(testCase -> testCase.requirements().stream()
                        .filter(requirement -> !requirementIds.contains(requirement))
                        .map(requirement -> testCase.id() + " -> " + requirement))
                .toList();
        assertEquals(List.of(), unknown);
    }

    @Test
    @DisplayName("Every requirement is covered by at least one test case")
    void everyRequirementHasTestCase() {
        List<String> uncovered = catalogue.requirements().stream()
                .map(Requirement::id)
                .filter(id -> catalogue.testCasesOf(id).isEmpty())
                .toList();
        assertEquals(List.of(), uncovered);
    }

    @Test
    @DisplayName("Test case tag constants match the automated test cases in the catalogue")
    void tagConstantsMatchAutomatedTestCases() {
        assertEquals(automatedTestCaseIds(), tagConstants());
    }

    @Test
    @DisplayName("Every automated test case is executed by at least one tagged test")
    void automatedTestCasesAreTagged() {
        List<String> untagged = automatedTestCaseIds().stream()
                .filter(id -> !taggedTestCaseIds.contains(id))
                .sorted()
                .toList();
        assertEquals(List.of(), untagged);
    }

    @Test
    @DisplayName("Tests are tagged only with automated test cases from the catalogue")
    void testTagsExistInCatalogue() {
        Set<String> automated = automatedTestCaseIds();
        List<String> unknown = taggedTestCaseIds.stream()
                .filter(id -> !automated.contains(id))
                .sorted()
                .toList();
        assertTrue(unknown.isEmpty(), "Unknown or manual test cases used as tags: " + unknown);
    }

    private static Set<String> automatedTestCaseIds() {
        return catalogue.testCases().stream()
                .filter(TestCaseSpec::isAutomated)
                .map(TestCaseSpec::id)
                .collect(Collectors.toSet());
    }

    private static Set<String> tagConstants() {
        return Arrays.stream(TestCases.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
                .map(TraceabilityCatalogueTest::constantValue)
                .collect(Collectors.toSet());
    }

    private static String constantValue(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> Set<String> ids(Collection<T> items, Function<T, String> id) {
        return items.stream().map(id).collect(Collectors.toSet());
    }

    private static <T> List<String> duplicates(Collection<T> items, Function<T, String> id) {
        return items.stream()
                .collect(Collectors.groupingBy(id, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
    }
}
