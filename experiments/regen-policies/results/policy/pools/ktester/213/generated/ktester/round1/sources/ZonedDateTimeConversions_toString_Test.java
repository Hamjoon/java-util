package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toString_Test {

    /**
     * A mock Converter – the method under test does not use it, but we verify that.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Real Converter instance that could be used in future tests.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter with default options – not required for the current tests
        // but kept for completeness and potential reuse.
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test to keep tests independent.
        reset(mockConverter);
    }

    /**
     * Provides the test data for {@link #toString_variousScenarios(Object, String, Class, String)}.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the {@code from} object (usually a {@link ZonedDateTime})</li>
     *   <li>the expected ISO‑8601 string (or {@code null} when an exception is expected)</li>
     *   <li>the expected exception class (or {@code null} for successful conversion)</li>
     *   <li>a short description used as the test name</li>
     * </ul>
     */
    private static Stream<Arguments> provideToStringCases() {
        return Stream.of(// Normal conversion – UTC zone
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneId.of("UTC")), "2023-05-10T15:30:45.123456789Z", null, "Normal conversion - UTC zone"), // Normal conversion – non‑UTC zone (standard time)
        Arguments.of(ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York")), "2021-12-31T23:59:59-05:00", null, "Normal conversion - non‑UTC zone"), // Null input handling
        Arguments.of(null, null, NullPointerException.class, "Null input handling"), // Invalid type handling – not a ZonedDateTime
        Arguments.of("2023-01-01T00:00Z", null, ClassCastException.class, "Invalid type handling"), // Edge case – minimum representable date
        Arguments.of(ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.UTC), "-999999999-01-01T00:00:00Z", null, "Edge case - minimum representable date"), // Positive offset zone (Asia/Kolkata)
        Arguments.of(ZonedDateTime.of(2022, 3, 15, 9, 0, 0, 0, ZoneId.of("Asia/Kolkata")), "2022-03-15T09:00:00+05:30", null, "Positive offset zone (Asia/Kolkata)"), // Maximum representable date
        Arguments.of(ZonedDateTime.of(LocalDateTime.MAX, ZoneOffset.UTC), "999999999-12-31T23:59:59.999999999Z", null, "Maximum representable date"), // Daylight Saving Time period (America/New_York summer)
        Arguments.of(ZonedDateTime.of(2021, 7, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), "2021-07-01T12:00:00-04:00", null, "Daylight Saving Time period (America/New_York summer)"), // Zero offset with non‑UTC ZoneId (GMT)
        Arguments.of(ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT")), "2020-01-01T00:00:00Z", null, "Zero offset with non‑UTC ZoneId (GMT)"), // Fractional seconds omitted (nanosecond = 0)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("UTC")), "2023-05-10T15:30:45Z", null, "Fractional seconds omitted (nanosecond = 0)"));
    }

    @Test
    void toString_formatsZonedDateTime_usingIsoFormat() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000, ZoneId.of("America/New_York"));
        String expected = zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        // WHEN
        String actual = ZonedDateTimeConversions.toString(zdt, mockConverter);
        // THEN
        assertEquals(expected, actual, "The formatted string should match ISO_ZONED_DATE_TIME output");
        // Verify that the converter is not used at all.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toString_ignoresConverter_argument() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.now();
        // WHEN
        String result = ZonedDateTimeConversions.toString(zdt, mockConverter);
        // THEN
        assertNotNull(result);
        // The converter should never be called.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toString_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toString(nullObj, mockConverter), "Passing null should result in NullPointerException because of the cast to ZonedDateTime");
    }

    /* --------------------------------------------------------------
     *  Parameterised tests – all cases where the Converter argument is null.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideToStringCases")
    void toString_variousScenarios(Object from, String expectedResult, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        if (expectedException != null) {
            // Expect an exception (null input or wrong type)
            assertThrows(expectedException, () -> ZonedDateTimeConversions.toString(from, null), description);
        } else {
            // Normal conversion – the method should return the ISO‑8601 string.
            String actual = ZonedDateTimeConversions.toString(from, null);
            assertEquals(expectedResult, actual, description);
        }
        // The converter (null) is never used, nothing to verify.
    }

    /* --------------------------------------------------------------
     *  Non‑null converter argument – verify it is ignored.
     * -------------------------------------------------------------- */
    @Test
    void toString_nonNullConverterIsIgnored() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2024, 8, 20, 14, 45, 30, 123_000_000, ZoneId.of("UTC"));
        String expected = "2024-08-20T14:45:30.123Z";
        // WHEN
        String actual = ZonedDateTimeConversions.toString(zdt, mockConverter);
        // THEN
        assertEquals(expected, actual, "The method should ignore a non‑null Converter argument");
        verifyNoInteractions(mockConverter);
    }
}
