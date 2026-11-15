package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toString_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not influence the toString implementation,
        // but we provide a real instance to satisfy the method signature.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Supplies all JSON‑defined test cases.
     *
     * Each {@link Arguments} contains:
     *   1. the {@code from} value (may be {@code null}),
     *   2. the {@code Converter} instance to pass (may be {@code null}),
     *   3. the expected formatted string (null when an exception is expected),
     *   4. the expected exception type (null when a normal result is expected).
     */
    private static Stream<Arguments> toStringCases() {
        // Helper: a dummy non‑null Converter (Mockito mock is sufficient)
        Converter dummyConverter = mock(Converter.class);
        return Stream.of(// Standard LocalDateTime without fractional seconds
        Arguments.of(LocalDateTime.of(2023, 8, 15, 13, 45, 30), null, "2023-08-15T13:45:30", null), // LocalDateTime containing nanosecond precision
        Arguments.of(LocalDateTime.of(2023, 8, 15, 13, 45, 30, 123_456_789), null, "2023-08-15T13:45:30.123456789", null), // Earliest year supported by ISO_LOCAL_DATE_TIME (year 0)
        Arguments.of(LocalDateTime.of(0, 1, 1, 0, 0), null, "0000-01-01T00:00", null), // Null source should raise NPE
        Arguments.of(null, null, null, NullPointerException.class), // Incompatible source type (String) should raise CCE
        Arguments.of("2023-08-15T13:45:30", null, null, ClassCastException.class), // Maximum supported year and nanosecond value
        Arguments.of(LocalDateTime.of(999_999_999, 12, 31, 23, 59, 59, 999_999_999), null, "999999999-12-31T23:59:59.999999999", null), // Minimum (most negative) supported year
        Arguments.of(LocalDateTime.of(-999_999_999, 1, 1, 0, 0, 0, 0), null, "-999999999-01-01T00:00", null), // Negative year that fits within four digits
        Arguments.of(LocalDateTime.of(-1, 12, 31, 23, 59, 59), null, "-0001-12-31T23:59:59", null), // Fractional seconds with trailing zeros should be trimmed
        Arguments.of(LocalDateTime.of(2023, 8, 15, 13, 45, 30, 123_000_000), null, "2023-08-15T13:45:30.123", null), // Non‑null dummy converter must not affect output
        Arguments.of(LocalDateTime.of(2023, 12, 31, 23, 59, 59), dummyConverter, "2023-12-31T23:59:59", null));
    }

    @Test
    void testToStringFormatsCorrectly() {
        // GIVEN
        LocalDateTime dateTime = LocalDateTime.of(2023, 5, 17, 13, 45, 30, 123_456_789);
        String expected = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        // WHEN
        String actual = LocalDateTimeConversions.toString(dateTime, converter);
        // THEN
        assertEquals(expected, actual, "LocalDateTimeConversions.toString should format using ISO_LOCAL_DATE_TIME");
    }

    @Test
    void testToStringWithNullThrowsNullPointerException() {
        // GIVEN a null source value
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toString(nullSource, converter), "Calling toString with a null LocalDateTime should throw NPE");
    }

    @Test
    void testToStringWithWrongTypeThrowsClassCastException() {
        // GIVEN a source that is not a LocalDateTime
        Object notALocalDateTime = "2023-05-17T13:45:30";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toString(notALocalDateTime, converter), "Calling toString with a non‑LocalDateTime source should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("toStringCases")
    void testToStringParameterized(Object from, Converter conv, String expectedString, Class<? extends Throwable> expectedException) {
        // Use the shared real converter when the test case supplies null,
        // otherwise use the provided (possibly mocked) converter.
        Converter usedConverter = (conv == null) ? converter : conv;
        if (expectedException != null) {
            // Expect an exception (NPE or CCE)
            assertThrows(expectedException, () -> LocalDateTimeConversions.toString(from, usedConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
        } else {
            // Normal successful conversion
            String actual = LocalDateTimeConversions.toString(from, usedConverter);
            assertEquals(expectedString, actual, () -> "Formatted string does not match for input: " + from);
        }
    }
}
