package com.nikitarybalko.wargamebroker.support.traceability;

import com.nikitarybalko.wargamebroker.support.traceability.AutomatedTestResult.Outcome;
import com.nikitarybalko.wargamebroker.support.traceability.TraceabilityCatalogue.Requirement;
import com.nikitarybalko.wargamebroker.support.traceability.TraceabilityCatalogue.TestCaseSpec;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Joins the catalogue with automated test results: requirement → test cases → automated tests → verdict.
 */
public record TraceabilityMatrix(List<RequirementRow> rows) {

    public enum Verdict {
        PASSED("Пройдено"),
        FAILED("Не пройдено"),
        PARTIAL("Частково виконано"),
        NOT_RUN("Не виконувалось"),
        MANUAL("Ручна перевірка"),
        NOT_COVERED("Немає тест-кейсів");

        private final String label;

        Verdict(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record TestCaseRow(TestCaseSpec testCase, List<AutomatedTestResult> tests, Verdict verdict) {

        public long passedTests() {
            return tests.stream().filter(test -> test.outcome() == Outcome.PASSED).count();
        }
    }

    public record RequirementRow(Requirement requirement, List<TestCaseRow> testCases, Verdict verdict) {

        public List<AutomatedTestResult> automatedTests() {
            return testCases.stream()
                    .flatMap(row -> row.tests().stream())
                    .distinct()
                    .toList();
        }

        public long manualTestCases() {
            return testCases.stream().filter(row -> !row.testCase().isAutomated()).count();
        }
    }

    public static TraceabilityMatrix build(TraceabilityCatalogue catalogue, Collection<AutomatedTestResult> results) {
        List<RequirementRow> rows = catalogue.requirements().stream()
                .map(requirement -> requirementRow(requirement, catalogue, results))
                .toList();
        return new TraceabilityMatrix(rows);
    }

    public List<TestCaseRow> testCaseRows() {
        return rows.stream()
                .flatMap(row -> row.testCases().stream())
                .filter(distinctById())
                .sorted(Comparator.comparing(row -> row.testCase().id(), TraceabilityMatrix::compareIds))
                .toList();
    }

    private static RequirementRow requirementRow(Requirement requirement, TraceabilityCatalogue catalogue,
                                                 Collection<AutomatedTestResult> results) {
        List<TestCaseRow> testCases = catalogue.testCasesOf(requirement.id()).stream()
                .map(testCase -> testCaseRow(testCase, results))
                .toList();
        return new RequirementRow(requirement, testCases, requirementVerdict(testCases));
    }

    private static TestCaseRow testCaseRow(TestCaseSpec testCase, Collection<AutomatedTestResult> results) {
        List<AutomatedTestResult> tests = results.stream()
                .filter(result -> result.testCaseIds().contains(testCase.id()))
                .sorted(Comparator.comparing(AutomatedTestResult::testName))
                .toList();
        return new TestCaseRow(testCase, tests, testCaseVerdict(testCase, tests));
    }

    private static Verdict testCaseVerdict(TestCaseSpec testCase, List<AutomatedTestResult> tests) {
        if (!testCase.isAutomated()) {
            return Verdict.MANUAL;
        }
        if (tests.stream().anyMatch(test -> test.outcome() == Outcome.FAILED)) {
            return Verdict.FAILED;
        }
        boolean anyPassed = tests.stream().anyMatch(test -> test.outcome() == Outcome.PASSED);
        return anyPassed ? Verdict.PASSED : Verdict.NOT_RUN;
    }

    private static Verdict requirementVerdict(List<TestCaseRow> testCases) {
        if (testCases.isEmpty()) {
            return Verdict.NOT_COVERED;
        }
        List<Verdict> automated = testCases.stream()
                .map(TestCaseRow::verdict)
                .filter(verdict -> verdict != Verdict.MANUAL)
                .toList();
        if (automated.isEmpty()) {
            return Verdict.MANUAL;
        }
        if (automated.contains(Verdict.FAILED)) {
            return Verdict.FAILED;
        }
        if (!automated.contains(Verdict.NOT_RUN)) {
            return Verdict.PASSED;
        }
        return automated.contains(Verdict.PASSED) ? Verdict.PARTIAL : Verdict.NOT_RUN;
    }

    private static Predicate<TestCaseRow> distinctById() {
        Set<String> seen = new HashSet<>();
        return row -> seen.add(row.testCase().id());
    }

    /** Orders TC-FR2-01 before TC-FR10-01 and functional before non-functional requirements. */
    static int compareIds(String left, String right) {
        return Comparator.comparing(TraceabilityMatrix::idSortKey).compare(left, right);
    }

    private static String idSortKey(String id) {
        String[] parts = id.replace("TC-", "").split("-");
        String category = parts[0].replaceAll("\\d", "");
        String number = parts[0].replaceAll("\\D", "");
        String suffix = parts.length > 1 ? parts[1] : "";
        return (category.startsWith("NFR") ? "1" : "0") + category + String.format("%03d", Integer.parseInt(number)) + suffix;
    }
}
