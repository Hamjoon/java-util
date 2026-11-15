package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toLong_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference (helps GC in large test suites).
        converter = null;
    }

    /**
     * Supplies the successful conversion test data.
     */
    static Stream<Arguments> validInputs() {
        // Helper constants
        // 2023‑01‑01T12:34:56.789Z
        long epochMillis = 1_672_576_496_789L;
        // same instant shifted +5h (UTC‑5)
        long epochMillisMinus5 = 1_672_594_496_789L;
        // Calendar in UTC – set directly by epoch millis
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(epochMillis);
        // Calendar in America/New_York – set fields to represent local time 2023‑01‑01 12:34:56.789
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCal.clear();
        nyCal.set(2023, Calendar.JANUARY, 1, 12, 34, 56);
        nyCal.set(Calendar.MILLISECOND, 789);
        return Stream.of(// Instant
        Arguments.of(Instant.parse("2023-01-01T12:34:56.789Z"), epochMillis), // java.util.Date
        Arguments.of(new Date(epochMillis), epochMillis), // java.sql.Timestamp
        Arguments.of(Timestamp.from(Instant.parse("2023-01-01T12:34:56.789Z")), epochMillis), // Long (epoch millis)
        Arguments.of(epochMillis, epochMillis), // String (numeric)
        Arguments.of("1672576496789", epochMillis), // String with surrounding whitespace
        Arguments.of("  1672576496789  ", epochMillis), // String with leading plus sign
        Arguments.of("+1672576496789", epochMillis), // String with explicit minus sign
        Arguments.of("-1234567890", -1_234_567_890L), // LocalDateTime (treated as UTC)
        Arguments.of(LocalDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000), epochMillis), // ZonedDateTime UTC
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneOffset.UTC), epochMillis), // OffsetDateTime with -05:00 offset (should normalize to UTC)
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneOffset.ofHours(-5)), epochMillisMinus5), // ZonedDateTime America/New_York (UTC‑5 in winter)
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneId.of("America/New_York")), epochMillisMinus5), // Calendar UTC
        Arguments.of(utcCal, epochMillis), // Calendar non‑UTC (America/New_York)
        Arguments.of(nyCal, epochMillisMinus5), // LocalDate (start of day UTC)
        Arguments.of(LocalDate.of(2023, 1, 1), 1_672_531_200_000L), // YearMonth (first day of month UTC)
        Arguments.of(YearMonth.of(2023, 1), 1_672_531_200_000L), // Year (1‑Jan‑2023 UTC)
        Arguments.of(Year.of(2023), 1_672_531_200_000L), // AtomicLong
        Arguments.of(new AtomicLong(epochMillis), epochMillis), // BigDecimal (whole number)
        Arguments.of(new BigDecimal("1672576496789"), epochMillis), // BigInteger
        Arguments.of(new BigInteger("1672576496789"), epochMillis), // Epoch zero
        Arguments.of(Instant.ofEpochMilli(0L), 0L), // Negative epoch (Long)
        Arguments.of(-1000L, -1000L), // Long.MAX_VALUE
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    static Stream<Arguments> illegalArgumentInputs() {
        return Stream.of(// Unsupported Boolean
        Arguments.of(Boolean.TRUE), // Unsupported LocalTime
        Arguments.of(LocalTime.of(12, 34, 56, 789_000_000)), // Unsupported MonthDay
        Arguments.of(MonthDay.of(1, 1)), // Empty string
        Arguments.of(""), // Non‑numeric string
        Arguments.of("notANumber"), // Whitespace‑only string
        Arguments.of("   "));
    }

    @Test
    void toLongFromInstant() {
        // GIVEN
        long expected = 1_623_456_789L;
        Instant instant = Instant.ofEpochMilli(expected);
        // WHEN
        long actual = LocalDateConversions.toLong(instant, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from Instant should yield the same epoch milli value");
    }

    @Test
    void toLongFromDate() {
        // GIVEN
        long expected = 1_623_456_789L;
        Date date = new Date(expected);
        // WHEN
        long actual = LocalDateConversions.toLong(date, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from java.util.Date should yield its time in millis");
    }

    @Test
    void toLongFromMapWithTimeKey() {
        // GIVEN
        long expected = 1_623_456_789L;
        Map<String, Object> map = new HashMap<>();
        // the Converter treats the "time" key as epoch millis
        map.put("time", expected);
        // WHEN
        long actual = LocalDateConversions.toLong(map, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from a Map containing a \"time\" entry should yield that value");
    }

    @Test
    void toLongFromSqlTimestamp() {
        // GIVEN
        long expected = 1_623_456_789L;
        Timestamp ts = new Timestamp(expected);
        // WHEN
        long actual = LocalDateConversions.toLong(ts, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from java.sql.Timestamp should yield its epoch milli value");
    }

    @Test
    void toLongFromNullReturnsZero() {
        // GIVEN
        Object source = null;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(0L, actual, "Conversion of null should return 0 (the default for primitive long)");
    }

    /**
     * All inputs that should successfully convert to a {@code long}
     * representing epoch‑milliseconds.
     */
    @ParameterizedTest(name = "{index}: toLong({0}) ⇒ {1}")
    @MethodSource("validInputs")
    void toLongValid(Object source, long expected) {
        long actual = LocalDateConversions.toLong(source, converter);
        assertEquals(expected, actual, () -> "Conversion of " + source.getClass().getSimpleName() + " should yield " + expected);
    }

    /**
     * Inputs that should result in an {@link IllegalArgumentException}.
     */
    @ParameterizedTest(name = "{index}: toLong({0}) throws IllegalArgumentException")
    @MethodSource("illegalArgumentInputs")
    void toLongIllegalArgument(Object source) {
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toLong(source, converter), () -> "Expected IllegalArgumentException for source: " + source);
    }

    /**
     * Null source object should cause a {@link NullPointerException}.
     */
    @Test
    void toLongNullSourceThrows() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toLong(null, converter), "Conversion of a null source should throw NullPointerException");
    }

    /**
     * Null {@link Converter} argument should cause a {@link NullPointerException}.
     */
    @Test
    void toLongNullConverterThrows() {
        // any non‑null source
        Object source = Instant.now();
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toLong(source, null), "Passing a null Converter should throw NullPointerException");
    }
}
