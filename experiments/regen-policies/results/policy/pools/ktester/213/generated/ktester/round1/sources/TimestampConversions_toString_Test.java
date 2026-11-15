package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toString_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter is not used by the method under test, but a non‑null instance is required for
        // the few cases where we explicitly pass a non‑null value.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Supplies all test cases defined in the JSON specification.
     */
    private static Stream<Arguments> toStringProvider() {
        return Stream.of(// 1. Exact millisecond precision
        Arguments.of(Timestamp.valueOf("2023-01-02 03:04:05.123"), null, "2023-01-02T03:04:05.123Z", null, "Exact millisecond precision (123 000 000 nanos)"), // 2. Nanosecond precision
        Arguments.of(Timestamp.valueOf("2023-01-02 03:04:05.123456789"), null, "2023-01-02T03:04:05.123456789Z", null, "Nanosecond precision (123 456 789 nanos)"), // 3. Zero nanoseconds (still millisecond pattern)
        Arguments.of(Timestamp.valueOf("2023-01-02 03:04:05.000"), null, "2023-01-02T03:04:05.000Z", null, "Zero nanoseconds – three fractional digits required"), // 4. Null `from` argument → NPE
        Arguments.of(null, null, null, NullPointerException.class, "Null `from` argument should cause NullPointerException"), // 5. Wrong type for `from` → ClassCastException
        Arguments.of(new java.util.Date(), null, null, ClassCastException.class, "`from` is not a java.sql.Timestamp, expect ClassCastException"), // 6. Timestamp just before Unix epoch (negative epoch milliseconds)
        Arguments.of(Timestamp.valueOf("1969-12-31 23:59:59.999"), null, "1969-12-31T23:59:59.999Z", null, "Timestamp before Unix epoch, millisecond pattern"), // 7. Smallest non‑zero nanosecond value
        Arguments.of(Timestamp.valueOf("2023-01-02 03:04:05.000000001"), null, "2023-01-02T03:04:05.000000001Z", null, "Minimum non‑zero nanosecond (1) → nanosecond pattern"), // 8. Maximum nanosecond value (all 9 digits)
        Arguments.of(Timestamp.valueOf("2023-01-02 03:04:05.999999999"), null, "2023-01-02T03:04:05.999999999Z", null, "Maximum nanosecond precision (9‑digit fraction)"), // 9. Trailing zeros after nanosecond pattern (123450000)
        Arguments.of(Timestamp.valueOf("2023-01-02 03:04:05.123450000"), null, "2023-01-02T03:04:05.123450000Z", null, "Nanosecond pattern with trailing zeros (123 450 000)"), // 10. Trailing zeros after nanosecond pattern (123456000)
        Arguments.of(Timestamp.valueOf("2023-01-02 03:04:05.123456000"), null, "2023-01-02T03:04:05.123456000Z", null, "Nanosecond pattern with three trailing zeros (123 456 000)"), // 11. Non‑null converter (method ignores it)
        Arguments.of(Timestamp.valueOf("2023-01-02 03:04:05.123"), // any non‑null Converter instance
        converter, "2023-01-02T03:04:05.123Z", null, "Non‑null converter argument – result should be unchanged"), // 12. Nanosecond pattern with three trailing zeros (999000000)
        Arguments.of(Timestamp.valueOf("2023-01-02 03:04:05.999000000"), null, "2023-01-02T03:04:05.999000000Z", null, "Nanosecond pattern preserving three trailing zeros (999 000 000)"));
    }

    /**
     * Verify that timestamps with millisecond precision (nanos divisible by 1 000 000)
     * are formatted using the 3‑decimal‑place pattern.
     */
    @Test
    void testToString_MillisecondPrecision() {
        // 2023‑01‑02T12:34:56.123Z  → 123 000 000 nanos (exact milliseconds)
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-02T12:34:56.123Z"));
        String result = TimestampConversions.toString(ts, converter);
        assertEquals("2023-01-02T12:34:56.123Z", result);
    }

    /**
     * Verify that timestamps with nanosecond precision (nanos not divisible by 1 000 000)
     * are formatted using the 9‑decimal‑place pattern.
     */
    @Test
    void testToString_NanosecondPrecision() {
        // 2023‑01‑02T12:34:56.123456789Z  → 123 456 789 nanos (nanosecond precision)
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-02T12:34:56.123456789Z"));
        String result = TimestampConversions.toString(ts, converter);
        assertEquals("2023-01-02T12:34:56.123456789Z", result);
    }

    /**
     * Ensure that a timestamp whose nanosecond part is exactly on a millisecond boundary
     * (e.g., 500 000 000 nanos) still uses the millisecond pattern.
     */
    @Test
    void testToString_BoundaryCondition() {
        // 2023‑01‑02T12:34:56.500Z  → 500 000 000 nanos (exactly 0.5 s, still divisible by 1 000 000)
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-02T12:34:56.500Z"));
        String result = TimestampConversions.toString(ts, converter);
        assertEquals("2023-01-02T12:34:56.500Z", result);
    }

    /**
     * Parameterised test that exercises the full set of JSON‑driven cases.
     *
     * @param from               the value passed as the {@code from} argument
     * @param conv               the value passed as the {@code converter} argument (may be {@code null})
     * @param expected           the expected formatted string (null when an exception is expected)
     * @param expectedException  the expected exception class (null when a normal result is expected)
     * @param description        a short description of the scenario (used as the display name)
     */
    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("toStringProvider")
    void testToString_VariousScenarios(Object from, Converter conv, String expected, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            // The method should throw the expected exception.
            assertThrows(expectedException, () -> TimestampConversions.toString(from, conv), description);
        } else {
            // Normal execution – verify the formatted string matches exactly.
            String result = TimestampConversions.toString(from, conv);
            assertEquals(expected, result, description);
        }
    }
}
