package com.cedarsoftware.util.convert;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OffsetDateTimeConversions_toString_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        mocks.close();
    }

    /**
     * Supplies the test data extracted from the JSON specification.
     *
     * @return a stream of {@link Arguments} for the parameterised test
     */
    static Stream<Arguments> provideTestCases() {
        // Helper converter instance for the single case where a non‑null Converter is required.
        Converter realConverter = new Converter();
        return Stream.of(// 1. Standard case – a valid OffsetDateTime is formatted using ISO_OFFSET_DATE_TIME.
        Arguments.of(OffsetDateTime.of(2023, 5, 15, 10, 15, 30, 0, ZoneOffset.ofHours(2)), null, "2023-05-15T10:15:30+02:00", "Standard case – a valid OffsetDateTime is formatted using ISO_OFFSET_DATE_TIME."), // 2. OffsetDateTime with nanosecond precision – verifies that fractional seconds are preserved.
        Arguments.of(OffsetDateTime.of(2023, 5, 15, 10, 15, 30, 123_456_789, ZoneOffset.ofHours(2)), null, "2023-05-15T10:15:30.123456789+02:00", "OffsetDateTime with nanosecond precision – verifies that fractional seconds are preserved."), // 3. Zero offset (UTC) – ensures the formatter prints \"+00:00\" rather than \"Z\".
        Arguments.of(OffsetDateTime.of(2023, 5, 15, 10, 15, 30, 0, ZoneOffset.UTC), null, "2023-05-15T10:15:30+00:00", "Zero offset (UTC) – ensures the formatter prints \"+00:00\" rather than \"Z\"."), // 4. Null `from` argument – casting null to OffsetDateTime yields NPE.
        Arguments.of(null, null, NullPointerException.class, "Null `from` argument – casting null to OffsetDateTime yields NullPointerException."), // 5. Invalid type for `from` – a String cannot be cast to OffsetDateTime.
        Arguments.of("2023-05-15T10:15:30+02:00", null, ClassCastException.class, "Invalid type for `from` – a String cannot be cast to OffsetDateTime."), // 6. Positive offset with hour‑minute granularity (+05:30) and no fractional seconds.
        Arguments.of(OffsetDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHoursMinutes(5, 30)), null, "2023-12-31T23:59:59+05:30", "Positive offset with hour‑minute granularity (+05:30) and no fractional seconds."), // 7. Negative offset with non‑standard minutes (-07:45) and a half‑second fraction.
        Arguments.of(OffsetDateTime.of(2022, 2, 28, 12, 0, 0, 500_000_000, ZoneOffset.ofHoursMinutes(-7, -45)), null, "2022-02-28T12:00:00.5-07:45", "Negative offset with non‑standard minutes (-07:45) and a half‑second fraction."), // 8. Maximum allowed offset (+18:00) with the highest representable year.
        Arguments.of(OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(18)), null, "9999-12-31T23:59:59+18:00", "Maximum allowed offset (+18:00) with the highest representable year."), // 9. Minimum allowed offset (-18:00) with a negative year (BCE).
        Arguments.of(OffsetDateTime.of(-1, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-18)), null, "-0001-01-01T00:00:00-18:00", "Minimum allowed offset (-18:00) with a negative year (BCE)."), // 10. Fractional seconds that end with trailing zeros – formatter trims them.
        Arguments.of(OffsetDateTime.of(2023, 5, 15, 10, 15, 30, 123_000_000, ZoneOffset.ofHours(2)), null, "2023-05-15T10:15:30.123+02:00", "Fractional seconds that end with trailing zeros – verifies trimming."), // 11. Non‑null Converter argument – method ignores the converter.
        Arguments.of(OffsetDateTime.of(2023, 5, 15, 10, 15, 30, 0, ZoneOffset.ofHours(2)), realConverter, "2023-05-15T10:15:30+02:00", "Non‑null Converter argument; method ignores the converter."));
    }

    @Test
    void toString_WithValidOffsetDateTime_ReturnsIsoString() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(// date
        2023, // date
        5, // date
        10, // time with 123 ms (nanoseconds)
        15, // time with 123 ms (nanoseconds)
        30, // time with 123 ms (nanoseconds)
        45, // time with 123 ms (nanoseconds)
        123_000_000, // +02:30 offset
        ZoneOffset.ofHoursMinutes(2, 30));
        String expected = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        // WHEN
        String result = OffsetDateTimeConversions.toString(odt, mockConverter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void toString_WithNullInput_ThrowsNullPointerException() {
        // GIVEN
        Object nullValue = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toString(nullValue, mockConverter));
    }

    @Test
    void toString_WithInvalidType_ThrowsClassCastException() {
        // GIVEN
        Object notAnOffsetDateTime = "2023-05-10T15:30:45+02:30";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toString(notAnOffsetDateTime, mockConverter));
    }

    /**
     * Parameterised test that iterates over all JSON‑specified cases.
     *
     * @param from       the {@code from} argument passed to {@code toString}
     * @param converter  the {@code converter} argument (may be {@code null})
     * @param expected   either the expected ISO‑8601 string or the expected exception class
     * @param description textual description of the test case (used as JUnit display name)
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideTestCases")
    void toString_VariousScenarios(Object from, Converter converter, Object expected, String description) {
        if (expected instanceof String) {
            // Normal successful conversion
            String result = OffsetDateTimeConversions.toString(from, converter);
            assertEquals(expected, result, description);
        } else if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> OffsetDateTimeConversions.toString(from, converter), description);
        } else {
            fail("Test data error: expected must be String or Class<? extends Throwable>");
        }
    }
}
