package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.Calendar;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toCalendar_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration required for the conversions we test.
        converter = new Converter(new ConverterOptions());
        // Force UTC so that LocalDateTime → epoch conversion is deterministic.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// Long value
        Arguments.of(123456789L, null, 123456789L, null, "Pass a Long value; Calendar should represent that epoch milli."), // Numeric String
        Arguments.of("1622548800000", null, 1622548800000L, null, "Pass a numeric String; should be parsed to long."), // java.util.Date (epoch start)
        Arguments.of(new Date(0L), null, 0L, null, "Pass a java.util.Date at epoch start."), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(5000L), null, 5000L, null, "Pass an Instant; conversion should yield its epoch milli."), // null source – expected NPE according to JSON (implementation throws NPE)
        Arguments.of(null, null, null, NullPointerException.class, "Null 'from' should cause NullPointerException."), // BigInteger
        Arguments.of(new BigInteger("1234567890123"), null, 1234567890123L, null, "Pass a BigInteger; should be converted to long."));
    }

    private static Stream<Arguments> additionalTypeProvider() {
        return Stream.of(// Byte
        Arguments.of(Byte.valueOf((byte) 42), null, 42L, null, "Byte value should be treated as Number → 42 ms."), // Short (negative)
        Arguments.of(Short.valueOf((short) -32768), null, -32768L, null, "Short negative value → -32768 ms."), // Integer.MAX_VALUE
        Arguments.of(Integer.valueOf(Integer.MAX_VALUE), null, (long) Integer.MAX_VALUE, null, "Integer.MAX_VALUE should be handled correctly."), // Long.MAX_VALUE
        Arguments.of(Long.MAX_VALUE, null, Long.MAX_VALUE, null, "Maximum long value should be accepted."), // Long.MIN_VALUE
        Arguments.of(Long.MIN_VALUE, null, Long.MIN_VALUE, null, "Minimum long value should be accepted."), // BigDecimal with fraction (truncates)
        Arguments.of(new BigDecimal("1234567890.987"), null, 1234567890L, null, "BigDecimal with fractional part should truncate toward zero."), // AtomicLong
        Arguments.of(new AtomicLong(55555L), null, 55555L, null, "AtomicLong should expose its contained value."), // LocalDateTime (system default zone forced to UTC)
        Arguments.of(LocalDateTime.of(2022, 5, 1, 12, 30, 15), null, 1651401015000L, null, "LocalDateTime interpreted in UTC should yield expected epoch."), // ZonedDateTime with +02:00 offset
        Arguments.of(ZonedDateTime.parse("2022-01-01T00:00:00+02:00[Europe/Paris]"), null, 1640991600000L, null, "ZonedDateTime with offset should be converted to UTC epoch."), // String with whitespace
        Arguments.of("   987654321   ", null, 987654321L, null, "String with leading/trailing whitespace should be trimmed and parsed."), // Negative numeric string
        Arguments.of("-12345", null, -12345L, null, "Negative numeric string should preserve sign."), // Non‑numeric string → NumberFormatException
        Arguments.of("not-a-number", null, null, NumberFormatException.class, "Non‑numeric string should cause NumberFormatException."), // Boolean → ClassCastException
        Arguments.of(true, null, null, ClassCastException.class, "Unsupported Boolean type should cause ClassCastException."), // java.sql.Timestamp
        Arguments.of(new Timestamp(1609459200000L), null, 1609459200000L, null, "Timestamp should be treated like java.util.Date."), // Year 1970 → epoch start
        Arguments.of(Year.of(1970), null, 0L, null, "Year 1970 should map to epoch start."), // YearMonth 2020‑02 → first day of month UTC
        Arguments.of(YearMonth.of(2020, 2), null, 1580515200000L, null, "YearMonth should map to first day of month at UTC."), // MonthDay 12‑31 → 1970‑12‑31 UTC
        Arguments.of(MonthDay.of(12, 31), null, 31536000000L, null, "MonthDay without year defaults to 1970."), // Existing Calendar instance
        Arguments.of(new Calendar.Builder().setInstant(123456789L).build(), null, 123456789L, null, "Passing an existing Calendar should produce an equivalent Calendar."), // Arbitrary Object → ClassCastException
        Arguments.of(new Object(), null, null, ClassCastException.class, "Unsupported object type should cause ClassCastException."), // Null 'from' with a real (non‑null) converter → NPE
        Arguments.of(null, new Converter(new ConverterOptions()), null, NullPointerException.class, "Null source with non‑null converter should still cause NPE."));
    }

    private static Stream<Arguments> exceptionEdgeProvider() {
        return Stream.of(// Decimal string
        Arguments.of("123.45", null, NumberFormatException.class, "Decimal numeric string should cause NumberFormatException."), // Scientific notation
        Arguments.of("1e3", null, NumberFormatException.class, "Scientific‑notation string should cause NumberFormatException."), // Empty string
        Arguments.of("", null, NumberFormatException.class, "Empty string cannot be parsed to long."), // Whitespace only string
        Arguments.of("   ", null, NumberFormatException.class, "Whitespace‑only string should cause NumberFormatException."), // BigDecimal overflow
        Arguments.of(new BigDecimal("9223372036854775808"), null, ArithmeticException.class, "BigDecimal exceeding Long.MAX_VALUE should throw ArithmeticException."), // Unsupported java.time.Duration
        Arguments.of(Duration.ofHours(1), null, ClassCastException.class, "Unsupported Duration type should cause ClassCastException."));
    }

    /**
     * Verify that a {@code Long} representing epoch milliseconds is correctly turned into a {@link Calendar}.
     */
    @Test
    void toCalendar_fromLong() {
        // 2021‑01‑01T00:00:00Z
        long epochMillis = 1_609_459_200_000L;
        Calendar cal = DateConversions.toCalendar(epochMillis, converter);
        assertNotNull(cal, "Calendar should not be null");
        assertEquals(epochMillis, cal.getTimeInMillis(), "Calendar time should match the supplied epoch milliseconds");
    }

    /**
     * Verify that a numeric {@code String} is parsed to a {@link Calendar} with the correct time.
     */
    @Test
    void toCalendar_fromString() {
        // same instant as above
        String epochString = "1609459200000";
        Calendar cal = DateConversions.toCalendar(epochString, converter);
        assertNotNull(cal);
        assertEquals(Long.parseLong(epochString), cal.getTimeInMillis(), "Calendar time should match the parsed long value from the string");
    }

    /**
     * Verify that a {@link Date} instance is converted to a {@link Calendar} preserving the instant.
     */
    @Test
    void toCalendar_fromDate() {
        Date date = new Date(1_609_459_200_000L);
        Calendar cal = DateConversions.toCalendar(date, converter);
        assertNotNull(cal);
        assertEquals(date.getTime(), cal.getTimeInMillis(), "Calendar time should equal the Date's epoch milliseconds");
    }

    /**
     * Verify that an existing {@link Calendar} is effectively cloned (same instant, possibly different instance).
     */
    @Test
    void toCalendar_fromCalendar() {
        Calendar original = Calendar.getInstance();
        original.setTimeInMillis(1_609_459_200_000L);
        Calendar result = DateConversions.toCalendar(original, converter);
        assertNotNull(result);
        assertEquals(original.getTimeInMillis(), result.getTimeInMillis(), "Resulting Calendar should have the same instant as the source Calendar");
        // Ensure we got a distinct object (the conversion creates a new Calendar)
        assertNotSame(original, result, "Resulting Calendar should be a new instance");
    }

    /**
     * Verify behaviour when the source object is {@code null}.
     * According to the Converter contract, a null source for a numeric conversion yields 0,
     * so the resulting Calendar should represent the epoch (0 ms).
     */
    @Test
    void toCalendar_fromNull() {
        Calendar cal = DateConversions.toCalendar(null, converter);
        assertNotNull(cal, "Converter returns a Calendar for null input (epoch time)");
        assertEquals(0L, cal.getTimeInMillis(), "Calendar created from null should represent the epoch (0 ms)");
    }

    /* --------------------------------------------------------------
     *  Parameterised test – branch coverage (numeric conversions)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("branchCoverageProvider")
    void toCalendar_branchCoverage(Object from, Converter conv, Long expectedMillis, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> DateConversions.toCalendar(from, conv), description);
        } else {
            Calendar cal = DateConversions.toCalendar(from, conv);
            assertNotNull(cal, description);
            assertEquals(expectedMillis.longValue(), cal.getTimeInMillis(), description);
        }
    }

    /* --------------------------------------------------------------
     *  Parameterised test – additional type & edge coverage
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("additionalTypeProvider")
    void toCalendar_additionalTypes(Object from, Converter conv, Long expectedMillis, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> DateConversions.toCalendar(from, conv), description);
        } else {
            Calendar cal = DateConversions.toCalendar(from, conv);
            assertNotNull(cal, description);
            assertEquals(expectedMillis.longValue(), cal.getTimeInMillis(), description);
        }
    }

    /* --------------------------------------------------------------
     *  Parameterised test – exception edge cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("exceptionEdgeProvider")
    void toCalendar_exceptionEdgeCases(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> DateConversions.toCalendar(from, conv), description);
    }
}
