package com.nikitarybalko.wargamebroker.support.traceability;

import com.nikitarybalko.wargamebroker.support.traceability.AutomatedTestResult.Outcome;
import com.nikitarybalko.wargamebroker.support.traceability.TraceabilityMatrix.RequirementRow;
import com.nikitarybalko.wargamebroker.support.traceability.TraceabilityMatrix.TestCaseRow;
import com.nikitarybalko.wargamebroker.support.traceability.TraceabilityMatrix.Verdict;
import com.nikitarybalko.wargamebroker.support.traceability.TraceabilityCatalogue.TestCaseSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Renders the traceability matrix as Markdown for the report, a TSV ready to paste into the RTM spreadsheet,
 * and a Markdown catalogue of test cases with their automated tests.
 */
public final class TraceabilityReportWriter {

    public static final String MATRIX_MARKDOWN = "traceability-matrix.md";
    public static final String MATRIX_TSV = "traceability-matrix.tsv";
    public static final String TEST_CASES_MARKDOWN = "test-cases.md";
    public static final String TEST_CASES_TSV = "test-cases.tsv";

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String NEWLINE = "\n";
    private static final String CELL_SEPARATOR = " | ";
    private static final String LIST_SEPARATOR = ", ";
    private static final String NO_VALUE = "—";
    private static final String TAB = "\t";
    private static final String ESCAPED_NEWLINE = "\\n";

    private static final Map<String, String> LEVEL_LABELS = Map.of(
            "unit", "Модульний",
            "web", "Веб-шар (MockMvc)",
            "integration", "Інтеграційний (PostgreSQL)",
            "configuration", "Конфігураційний",
            "ui", "Інтерфейс користувача",
            "deployment", "Розгортання");

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "functional", "Функціональна",
            "non-functional", "Нефункціональна");

    private static final Map<Outcome, String> OUTCOME_LABELS = Map.of(
            Outcome.PASSED, "пройдено",
            Outcome.FAILED, "не пройдено",
            Outcome.SKIPPED, "пропущено");

    private static final Map<Verdict, String> IMPLEMENTATION_STATUS = Map.of(
            Verdict.PASSED, "Реалізовано",
            Verdict.FAILED, "Потребує виправлення",
            Verdict.PARTIAL, "Реалізовано, перевірено частково",
            Verdict.NOT_RUN, "Не перевірено",
            Verdict.MANUAL, "Реалізовано, перевіряється вручну",
            Verdict.NOT_COVERED, "Не перевірено");

    private final Path outputDirectory;

    public TraceabilityReportWriter(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void write(TraceabilityMatrix matrix) throws IOException {
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve(MATRIX_MARKDOWN), matrixMarkdown(matrix));
        Files.writeString(outputDirectory.resolve(MATRIX_TSV), matrixTsv(matrix));
        Files.writeString(outputDirectory.resolve(TEST_CASES_MARKDOWN), testCasesMarkdown(matrix));
        Files.writeString(outputDirectory.resolve(TEST_CASES_TSV), testCasesTsv(matrix));
    }

    String matrixMarkdown(TraceabilityMatrix matrix) {
        Stream<String> header = Stream.of(
                "# Requirements Traceability Matrix",
                "",
                "Згенеровано автоматично " + LocalDateTime.now().format(TIMESTAMP) + " за результатами `mvn test`.",
                "",
                summary(matrix),
                "",
                tableRow("Вимога", "Тип", "Підсистема", "Тест-кейси", "Автотести", "Результат"),
                tableRow("---", "---", "---", "---", "---", "---"));
        Stream<String> rows = matrix.rows().stream().map(this::matrixRow);
        return Stream.concat(header, rows).collect(Collectors.joining(NEWLINE)) + NEWLINE;
    }

    String matrixTsv(TraceabilityMatrix matrix) {
        Stream<String> header = Stream.of(String.join("\t",
                "ID", "Опис вимоги", "Підсистема", "ID Тест-кейсу", "Статус реалізації", "Результат тестування"));
        Stream<String> rows = matrix.rows().stream().map(row -> String.join("\t",
                row.requirement().id(),
                row.requirement().description(),
                row.requirement().subsystem(),
                testCaseIds(row),
                IMPLEMENTATION_STATUS.get(row.verdict()),
                resultSummary(row)));
        return Stream.concat(header, rows).collect(Collectors.joining(NEWLINE)) + NEWLINE;
    }

    String testCasesMarkdown(TraceabilityMatrix matrix) {
        Stream<String> header = Stream.of("# Тест-кейси", "");
        Stream<String> body = matrix.testCaseRows().stream().map(this::testCaseSection);
        return Stream.concat(header, body).collect(Collectors.joining(NEWLINE)) + NEWLINE;
    }

    /** Test cases with multi-line cells (steps, automated tests) escaped as \\n so every row stays on one line. */
    String testCasesTsv(TraceabilityMatrix matrix) {
        Stream<String> header = Stream.of(String.join(TAB, "ID", "Назва", "Вимоги", "Рівень", "Виконання",
                "Передумови", "Кроки", "Очікуваний результат", "Автотести", "Результат"));
        Stream<String> rows = matrix.testCaseRows().stream().map(row -> {
            TestCaseSpec testCase = row.testCase();
            return Stream.of(
                            testCase.id(),
                            testCase.title(),
                            String.join(LIST_SEPARATOR, testCase.requirements()),
                            LEVEL_LABELS.getOrDefault(testCase.level(), testCase.level()),
                            testCase.isAutomated() ? "Автоматизоване" : "Ручне",
                            testCase.preconditions(),
                            numberedSteps(testCase),
                            testCase.expected(),
                            row.tests().stream()
                                    .map(test -> test.testName() + " — " + OUTCOME_LABELS.get(test.outcome()))
                                    .collect(Collectors.joining(ESCAPED_NEWLINE)),
                            row.verdict().label())
                    .map(TraceabilityReportWriter::tsvCell)
                    .collect(Collectors.joining(TAB));
        });
        return Stream.concat(header, rows).collect(Collectors.joining(NEWLINE)) + NEWLINE;
    }

    private static String numberedSteps(TestCaseSpec testCase) {
        return IntStream.range(0, testCase.steps().size())
                .mapToObj(index -> (index + 1) + ". " + testCase.steps().get(index))
                .collect(Collectors.joining(ESCAPED_NEWLINE));
    }

    private static String tsvCell(String value) {
        return value.replace(TAB, " ").replace(NEWLINE, ESCAPED_NEWLINE);
    }

    private String summary(TraceabilityMatrix matrix) {
        List<TestCaseRow> testCases = matrix.testCaseRows();
        long automatedTestCases = testCases.stream().filter(row -> row.testCase().isAutomated()).count();
        long tests = matrix.rows().stream().flatMap(row -> row.automatedTests().stream()).distinct().count();
        long passedRequirements = matrix.rows().stream().filter(row -> row.verdict() == Verdict.PASSED).count();
        return "Вимог: %d, з них підтверджено автотестами: %d. Тест-кейсів: %d (автоматизованих %d, ручних %d). Автотестів: %d."
                .formatted(matrix.rows().size(), passedRequirements, testCases.size(), automatedTestCases,
                        testCases.size() - automatedTestCases, tests);
    }

    private String matrixRow(RequirementRow row) {
        return tableRow(
                row.requirement().id(),
                TYPE_LABELS.getOrDefault(row.requirement().type(), row.requirement().type()),
                row.requirement().subsystem(),
                testCaseIds(row),
                automatedSummary(row),
                row.verdict().label());
    }

    private String testCaseSection(TestCaseRow row) {
        TestCaseSpec testCase = row.testCase();
        Stream<String> header = Stream.of(
                "## " + testCase.id() + " — " + testCase.title(),
                "",
                tableRow("Атрибут", "Значення"),
                tableRow("---", "---"),
                tableRow("Вимоги", String.join(LIST_SEPARATOR, testCase.requirements())),
                tableRow("Рівень", LEVEL_LABELS.getOrDefault(testCase.level(), testCase.level())),
                tableRow("Виконання", testCase.isAutomated() ? "Автоматизоване" : "Ручне"),
                tableRow("Передумови", testCase.preconditions()),
                tableRow("Очікуваний результат", testCase.expected()),
                tableRow("Результат", row.verdict().label()),
                "",
                "**Кроки:**",
                "");
        Stream<String> steps = IntStream.range(0, testCase.steps().size())
                .mapToObj(index -> (index + 1) + ". " + testCase.steps().get(index));
        Stream<String> tests = row.tests().isEmpty()
                ? Stream.empty()
                : Stream.concat(Stream.of("", "**Автотести:**", ""),
                row.tests().stream().map(test -> "- `" + test.testName() + "` — " + OUTCOME_LABELS.get(test.outcome())));
        return Stream.of(header, steps, tests, Stream.of(""))
                .flatMap(part -> part)
                .collect(Collectors.joining(NEWLINE));
    }

    private static String testCaseIds(RequirementRow row) {
        return row.testCases().isEmpty()
                ? NO_VALUE
                : row.testCases().stream().map(test -> test.testCase().id()).collect(Collectors.joining(LIST_SEPARATOR));
    }

    private static String automatedSummary(RequirementRow row) {
        List<AutomatedTestResult> tests = row.automatedTests();
        long passed = tests.stream().filter(test -> test.outcome() == Outcome.PASSED).count();
        String automated = tests.isEmpty() ? NO_VALUE : passed + "/" + tests.size() + " пройдено";
        long manual = row.manualTestCases();
        return manual == 0 ? automated : automated + " + ручних тест-кейсів: " + manual;
    }

    private static String resultSummary(RequirementRow row) {
        return row.verdict().label() + " (" + automatedSummary(row) + ")";
    }

    private static String tableRow(String... cells) {
        return "| " + Stream.of(cells).map(TraceabilityReportWriter::escape).collect(Collectors.joining(CELL_SEPARATOR)) + " |";
    }

    private static String escape(String cell) {
        return cell.replace("|", "\\|").replace(NEWLINE, " ");
    }
}
