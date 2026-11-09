package com.nikitarybalko.wargamebroker.support.traceability;

import com.nikitarybalko.wargamebroker.support.traceability.AutomatedTestResult.Outcome;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Collects results of tests tagged with test case IDs and writes the traceability matrix
 * to target/traceability after every test run. Registered through META-INF/services.
 */
public class TraceabilityReportListener implements TestExecutionListener {

    public static final String TEST_CASE_TAG_PREFIX = "TC-";

    private static final Logger log = LoggerFactory.getLogger(TraceabilityReportListener.class);
    private static final Path OUTPUT_DIRECTORY = Path.of("target", "traceability");

    private final Map<String, AutomatedTestResult> results = new ConcurrentHashMap<>();

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        results.clear();
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        record(testIdentifier, Outcome.SKIPPED);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        record(testIdentifier, toOutcome(result));
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (results.isEmpty()) {
            return;
        }
        try {
            TraceabilityMatrix matrix = TraceabilityMatrix.build(TraceabilityCatalogue.load(), results.values());
            new TraceabilityReportWriter(OUTPUT_DIRECTORY).write(matrix);
            log.info("Traceability matrix written to {}", OUTPUT_DIRECTORY.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Traceability matrix was not generated: {}", e.getMessage());
        }
    }

    private void record(TestIdentifier testIdentifier, Outcome outcome) {
        Set<String> testCaseIds = testCaseIds(testIdentifier);
        if (!testIdentifier.isTest() || testCaseIds.isEmpty()) {
            return;
        }
        String testName = testName(testIdentifier);
        AutomatedTestResult result = new AutomatedTestResult(testName, testIdentifier.getDisplayName(), testCaseIds, outcome);
        results.merge(testName, result, AutomatedTestResult::merge);
    }

    private static Set<String> testCaseIds(TestIdentifier testIdentifier) {
        return testIdentifier.getTags().stream()
                .map(TestTag::getName)
                .filter(tag -> tag.startsWith(TEST_CASE_TAG_PREFIX))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String testName(TestIdentifier testIdentifier) {
        return testIdentifier.getSource()
                .filter(MethodSource.class::isInstance)
                .map(MethodSource.class::cast)
                .map(source -> simpleClassName(source.getClassName()) + "#" + source.getMethodName())
                .orElse(testIdentifier.getUniqueId());
    }

    private static String simpleClassName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private static Outcome toOutcome(TestExecutionResult result) {
        return switch (result.getStatus()) {
            case SUCCESSFUL -> Outcome.PASSED;
            case ABORTED -> Outcome.SKIPPED;
            case FAILED -> Outcome.FAILED;
        };
    }
}
