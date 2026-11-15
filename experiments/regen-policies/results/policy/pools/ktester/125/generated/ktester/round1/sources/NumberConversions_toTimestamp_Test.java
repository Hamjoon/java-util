package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toTimestamp_Test {

    /**
     * The mock Converter used for all test cases.
     */
    @Mock
    Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Force the default time‑zone to UTC so that LocalDate/LocalDateTime tests are deterministic.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to keep tests independent.
        reset(mockConverter);
    }

    /**
     * Stubs the mock so that converting {@code source} to {@code Long} or {@code long}
     *  yields {@code value}.
     */
    private void stubLongConversion(Object source, long value) {
        when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(value);
        when(mockConverter.convert(eq(source), eq(long.class))).thenReturn(value);
    }

    /**
     * Stubs the mock to throw {@code ex} when converting {@code source} to {@code Long}.
     */
    private void stubConversionThrows(Object source, RuntimeException ex) {
        when(mockConverter.convert(eq(source), any())).thenThrow(ex);
    }

    /**
     * Provides valid numeric source objects together with the expected epoch.
     */
    private static List<Arguments> validNumericSources() {
        return List.of(// Long
        Arguments.of(1622548800000L, 1622548800000L, "Long value representing epoch milliseconds"), // Integer
        Arguments.of(12345, 12345L, "Integer value treated as milliseconds"), // String numeric
        Arguments.of("1622548800000", 1622548800000L, "String containing numeric epoch‑millisecond value"), // BigDecimal
        Arguments.of(new BigDecimal("1622548800000"), 1622548800000L, "BigDecimal representing epoch milliseconds"), // BigInteger
        Arguments.of(new BigInteger("1622548800000"), 1622548800000L, "BigInteger representing epoch milliseconds"), // AtomicLong
        Arguments.of(new AtomicLong(1622548800000L), 1622548800000L, "AtomicLong holding epoch milliseconds"), // Byte
        Arguments.of((byte) 100, 100L, "Byte value widened to long"), // Short
        Arguments.of((short) 20000, 20000L, "Short value widened to long"), // AtomicInteger
        Arguments.of(new AtomicInteger(1_234_567_890), 1_234_567_890L, "AtomicInteger unwrapped to long"), // Double (exact)
        Arguments.of(1622548800000.0d, 1622548800000L, "Double representing an exact millisecond count"), // Float (fraction discarded)
        Arguments.of(1_234_567.0f, 1_234_567L, "Float value converted to long (fraction discarded)"), // Negative long
        Arguments.of(-1000L, -1000L, "Negative long value (pre‑1970 instant)"));
    }

    /**
     * Provides valid date/time source objects together with the expected epoch.
     */
    private static List<Arguments> validDateTimeSources() {
        return List.of(// java.util.Date
        Arguments.of(new Date(1622548800000L), 1622548800000L, "Date instance"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2021-06-01 00:00:00.000"), 1622505600000L, "Timestamp valueOf"), // Instant
        Arguments.of(Instant.ofEpochMilli(1622548800000L), 1622548800000L, "Instant"), // LocalDateTime (interpreted in UTC because default zone set to UTC)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 0, 0, 0), 1622505600000L, "LocalDateTime interpreted as UTC"), // ZonedDateTime UTC
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneId.of("UTC")), 1622505600000L, "ZonedDateTime with UTC zone"), // OffsetDateTime UTC
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC), 1622505600000L, "OffsetDateTime with UTC offset"), // Year (start of year)
        Arguments.of(Year.of(2021), 1609459200000L, "Year interpreted as start of year"), // GregorianCalendar (fixed instant)
        Arguments.of(createFixedGregorianCalendar(1622548800000L), 1622548800000L, "GregorianCalendar with fixed timeInMillis"), // java.sql.Date
        Arguments.of(new java.sql.Date(1622548800000L), 1622548800000L, "java.sql.Date"), // LocalDate (start of day UTC)
        Arguments.of(LocalDate.of(2021, 6, 1), 1622505600000L, "LocalDate interpreted as start of day UTC"), // ZonedDateTime America/New_York (UTC‑4 offset)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneId.of("America/New_York")), 1622520000000L, "ZonedDateTime with America/New_York offset"), // OffsetDateTime with -04:00 offset
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-4)), 1622520000000L, "OffsetDateTime with -04:00 offset"), // String with surrounding whitespace
        Arguments.of(" 1622548800000 ", 1622548800000L, "String with surrounding whitespace"));
    }

    /**
     * Helper that creates a {@link GregorianCalendar} fixed to the supplied epoch millis.
     */
    private static GregorianCalendar createFixedGregorianCalendar(long epochMillis) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Provides inputs that should cause an {@link IllegalArgumentException}.
     */
    private static List<Arguments> illegalArgumentSources() {
        return List.of(Arguments.of(true, "Boolean value is not supported"), Arguments.of(new Object(), "Arbitrary Object is not supported"), Arguments.of("abc", "Non‑numeric String"), Arguments.of("123.45", "Decimal String"), Arguments.of(new BigInteger("9223372036854775808"), "BigInteger exceeding Long.MAX_VALUE"));
    }

    @Test
    void toTimestamp_fromLong_returnsTimestampWithSameEpoch() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        Long source = 1_642_548_800_000L;
        when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(source);
        when(mockConverter.convert(eq(source), eq(long.class))).thenReturn(source);
        // WHEN
        Timestamp result = NumberConversions.toTimestamp(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(source.longValue(), result.getTime(), "Timestamp should contain the same epoch milliseconds as the source Long");
    }

    @Test
    void toTimestamp_fromDate_returnsTimestampWithSameEpoch() {
        // GIVEN
        Date source = new Date(123_456_789L);
        when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(source.getTime());
        when(mockConverter.convert(eq(source), eq(long.class))).thenReturn(source.getTime());
        // WHEN
        Timestamp result = NumberConversions.toTimestamp(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "Timestamp should contain the same epoch milliseconds as the source Date");
    }

    @Test
    void toTimestamp_fromStringNumber_returnsTimestampWithParsedEpoch() {
        // GIVEN
        // numeric string
        String source = "1622548800000";
        Long expected = 1_622_548_800_000L;
        when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(expected);
        when(mockConverter.convert(eq(source), eq(long.class))).thenReturn(expected);
        // WHEN
        Timestamp result = NumberConversions.toTimestamp(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expected.longValue(), result.getTime(), "Timestamp should contain the epoch milliseconds parsed from the numeric String");
    }

    @Test
    void toTimestamp_fromNull_returnsZeroTimestamp() {
        // GIVEN
        when(mockConverter.convert(isNull(), eq(Long.class))).thenReturn(0L);
        when(mockConverter.convert(isNull(), eq(long.class))).thenReturn(0L);
        // WHEN
        Timestamp result = NumberConversions.toTimestamp(null, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "When the source value is null, the resulting Timestamp should represent epoch start (0 ms)");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validNumericSources")
    void toTimestamp_validNumericConversions(Object source, long expectedEpoch, String description) {
        // GIVEN
        stubLongConversion(source, expectedEpoch);
        // WHEN
        Timestamp result = NumberConversions.toTimestamp(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(expectedEpoch, result.getTime(), () -> "Timestamp epoch mismatch for source: " + source + " (" + description + ")");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validDateTimeSources")
    void toTimestamp_validDateTimeConversions(Object source, long expectedEpoch, String description) {
        // GIVEN
        stubLongConversion(source, expectedEpoch);
        // WHEN
        Timestamp result = NumberConversions.toTimestamp(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(expectedEpoch, result.getTime(), () -> "Timestamp epoch mismatch for source: " + source + " (" + description + ")");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("illegalArgumentSources")
    void toTimestamp_illegalArgumentThrows(Object source, String description) {
        // GIVEN – configure the mock to throw IllegalArgumentException when conversion is attempted
        stubConversionThrows(source, new IllegalArgumentException("Unsupported type: " + source));
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toTimestamp(source, mockConverter), () -> "Expected IllegalArgumentException for source: " + source + " (" + description + ")");
    }

    @Test
    void toTimestamp_nullSourceThrowsNullPointerException() {
        // GIVEN – the mock is not stubbed for a null source, allowing the method to hit its own NPE path
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> NumberConversions.toTimestamp(null, mockConverter), "Passing null should result in NullPointerException");
    }
}
