package com.nikitarybalko.wargamebroker.support.traceability;

import java.util.Set;

/**
 * Outcome of one test method; parameterized invocations are merged into a single result.
 */
public record AutomatedTestResult(String testName, String displayName, Set<String> testCaseIds, Outcome outcome) {

    public enum Outcome {
        PASSED,
        SKIPPED,
        FAILED;

        /** The worse of two outcomes, so one failed invocation fails the whole method. */
        Outcome worst(Outcome other) {
            return compareTo(other) >= 0 ? this : other;
        }
    }

    public AutomatedTestResult merge(AutomatedTestResult other) {
        return new AutomatedTestResult(testName, displayName, testCaseIds, outcome.worst(other.outcome));
    }
}
