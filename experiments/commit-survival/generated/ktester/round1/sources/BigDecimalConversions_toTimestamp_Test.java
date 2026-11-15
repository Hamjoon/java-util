package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.time.*;
import java.util.Calendar;
import java.util.UUID;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toTimestamp_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Direct Instant with nanoseconds
        Arguments.of(Instant.ofEpochSecond(1622548800L, 123_456_789L), 1_622_548_800_123L, 123_456_789), // java.util.Date (millis precision)
        Arguments.of(new Date(1_622_548_800_123L), 1_622_548_800_123L, 0), // java.sql.Timestamp (already a Timestamp)
        Arguments.of(Timestamp.valueOf("2021-06-01 12:00:00.123456789"), 1_622_548_800_123L, 123_456_789), // Long epoch milliseconds
        Arguments.of(Long.valueOf(1_622_548_800_123L), 1_622_548_800_123L, 0), // BigDecimal seconds with fraction
        Arguments.of(new BigDecimal("1622548800.123456789"), 1_622_548_800_123L, 123_456_789), // BigDecimal integer seconds
        Arguments.of(new BigDecimal("1622548800"), 1_622_548_800_000L, 0), // BigDecimal with excess fractional digits (rounding up)
        Arguments.of(new BigDecimal("1622548800.1234567899"), 1_622_548_800_123L, 456_789_900), // LocalDateTime interpreted in system default zone
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 0, 0, 123_456_789), // convert using default zone
        ZonedDateTime.of(LocalDateTime.of(2021, 6, 1, 12, 0, 0, 123_456_789), ZoneId.systemDefault()).toInstant().toEpochMilli(), // nanos are preserved
        123_456_789), // ZonedDateTime UTC
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 123_456_789, ZoneOffset.UTC), 1_622_548_800_123L, 123_456_789), // ZonedDateTime non‑UTC (America/New_York, offset -04:00 during summer)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 123_456_789, ZoneId.of("America/New_York")), // 12:00 EDT = 16:00 UTC
        ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 123_456_789, ZoneId.of("America/New_York")).toInstant().toEpochMilli(), 123_456_789), // OffsetDateTime UTC
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 123_456_789, ZoneOffset.UTC), 1_622_548_800_123L, 123_456_789), // OffsetDateTime non‑UTC (+05:30)
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 123_456_789, ZoneOffset.ofHoursMinutes(5, 30)), OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 123_456_789, ZoneOffset.ofHoursMinutes(5, 30)).toInstant().toEpochMilli(), 123_456_789), // Calendar instance (UTC+02:00)
        Arguments.of(createCalendar(ZoneOffset.ofHours(2), 1_622_548_800_123L), 1_622_548_800_123L, 0), // java.sql.Date (date only)
        Arguments.of(java.sql.Date.valueOf("2021-06-01"), // midnight of 2021‑06‑01 in the default zone
        ZonedDateTime.of(LocalDate.of(2021, 6, 1), LocalTime.MIDNIGHT, ZoneId.systemDefault()).toInstant().toEpochMilli(), 0), // LocalDate (date only)
        Arguments.of(LocalDate.of(2021, 6, 1), ZonedDateTime.of(LocalDate.of(2021, 6, 1), LocalTime.MIDNIGHT, ZoneId.systemDefault()).toInstant().toEpochMilli(), 0), // LocalTime (time only, epoch day 1970‑01‑01)
        Arguments.of(LocalTime.of(12, 34, 56, 123_456_789), ZonedDateTime.of(LocalDate.ofEpochDay(0), LocalTime.of(12, 34, 56, 123_456_789), ZoneId.systemDefault()).toInstant().toEpochMilli(), 123_456_789), // Negative Instant (before 1970)
        Arguments.of(Instant.ofEpochSecond(-12_345L, 987_654_321L), // epoch milli = seconds * 1000 + nanos/1_000_000
        -12_345_000L, 987_654_321));
    }

    /**
     * Helper to create a Calendar set to a given instant and zone.
     */
    private static Calendar createCalendar(ZoneOffset offset, long epochMilli) {
        Calendar cal = Calendar.getInstance(java.util.TimeZone.getTimeZone(offset));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    @Test
    void toTimestamp_fromDate_returnsEquivalentTimestamp() {
        // given
        // 2021‑10‑01T00:00:00Z
        long epochMilli = 1_632_072_800_000L;
        Date source = new Date(epochMilli);
        // when
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        // then
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(epochMilli, result.getTime(), "Timestamp should represent the same instant as the source Date");
    }

    @Test
    void toTimestamp_fromInstant_returnsEquivalentTimestamp() {
        // given
        Instant instant = Instant.parse("2022-03-15T12:34:56.789Z");
        // when
        Timestamp result = BigDecimalConversions.toTimestamp(instant, converter);
        // then
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(instant.toEpochMilli(), result.getTime(), "Timestamp should represent the same instant as the source Instant");
    }

    @Test
    void toTimestamp_fromLongEpochMilli_returnsEquivalentTimestamp() {
        // given
        // 2022‑01‑01T00:00:00Z
        Long epochMilli = 1_640_995_200_000L;
        // when
        Timestamp result = BigDecimalConversions.toTimestamp(epochMilli, converter);
        // then
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(epochMilli.longValue(), result.getTime(), "Timestamp should represent the same instant as the source epoch milliseconds");
    }

    @Test
    void toTimestamp_fromNull_returnsNull() {
        // given
        Object source = null;
        // when
        Timestamp result = BigDecimalConversions.toTimestamp(source, converter);
        // then
        assertNull(result, "Converting a null source should yield a null Timestamp");
    }

    @ParameterizedTest(name = "{index} ⇒ from={0}")
    @MethodSource("validConversionProvider")
    void toTimestamp_validConversions(Object source, long expectedEpochMilli, int expectedNanos) {
        Timestamp ts = BigDecimalConversions.toTimestamp(source, converter);
        assertNotNull(ts, "Result must not be null");
        assertEquals(expectedEpochMilli, ts.getTime(), "Epoch milliseconds do not match");
        assertEquals(expectedNanos, ts.getNanos(), "Nanosecond component does not match");
    }

    @Test
    void toTimestamp_withCustomConverter_parsesStringToInstant() {
        // Source is an ISO‑8601 string; the mock converter returns the parsed Instant.
        String isoString = "2021-06-01T12:00:00.123456789Z";
        Converter mockConverter = mock(Converter.class);
        // Assume Converter has a method: <T> T convert(Object source, Class<T> target);
        when(mockConverter.convert(eq(isoString), eq(Instant.class))).thenReturn(Instant.parse(isoString));
        Timestamp ts = BigDecimalConversions.toTimestamp(isoString, mockConverter);
        assertNotNull(ts);
        assertEquals(Instant.parse(isoString).toEpochMilli(), ts.getTime());
        assertEquals(123_456_789, ts.getNanos());
    }

    @Test
    void toTimestamp_converterReturnsNull_throwsNullPointerException() {
        String source = "2021-06-01T12:00:00Z";
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(source), eq(Instant.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toTimestamp(source, mockConverter), "A null Instant from the converter should cause NPE");
    }

    @Test
    void toTimestamp_converterThrowsException_propagatesIt() {
        String source = "invalid-format";
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(source), eq(Instant.class))).thenThrow(new IllegalArgumentException("cannot parse"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(source, mockConverter));
        assertEquals("cannot parse", ex.getMessage());
    }

    @Test
    void toTimestamp_longMaxValue_overflows() {
        Long max = Long.MAX_VALUE;
        assertThrows(RuntimeException.class, () -> BigDecimalConversions.toTimestamp(max, converter), "Conversion of Long.MAX_VALUE should fail with an exception");
    }

    @Test
    void toTimestamp_longMinValue_underflows() {
        Long min = Long.MIN_VALUE;
        assertThrows(RuntimeException.class, () -> BigDecimalConversions.toTimestamp(min, converter), "Conversion of Long.MIN_VALUE should fail with an exception");
    }

    @Test
    void toTimestamp_bigDecimalNegativeFraction_illegalArgument() {
        BigDecimal bd = new BigDecimal("-0.000000001");
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(bd, converter));
    }

    @Test
    void toTimestamp_doubleNaN_illegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(Double.NaN, converter));
    }

    @Test
    void toTimestamp_doublePositiveInfinity_illegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(Double.POSITIVE_INFINITY, converter));
    }

    @Test
    void toTimestamp_unsupportedString_illegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp("unsupported string", converter));
    }

    @Test
    void toTimestamp_bigIntegerTooLarge_illegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toTimestamp(new java.math.BigInteger("123456789012345678901234567890"), converter));
    }

    @Test
    void toTimestamp_instantMax_illegalArgument() {
        // far beyond Timestamp range
        Instant max = Instant.MAX;
        assertThrows(RuntimeException.class, () -> BigDecimalConversions.toTimestamp(max, converter));
    }

    @Test
    void toTimestamp_instantMin_illegalArgument() {
        Instant min = Instant.MIN;
        assertThrows(RuntimeException.class, () -> BigDecimalConversions.toTimestamp(min, converter));
    }

    @Test
    void toTimestamp_localDateTimeUpperBound_illegalArgument() {
        LocalDateTime ldt = LocalDateTime.of(292278994, 8, 17, 7, 12, 55, 807_000_000);
        assertThrows(RuntimeException.class, () -> BigDecimalConversions.toTimestamp(ldt, converter));
    }

    @Test
    void toTimestamp_localDateTimeLowerBound_illegalArgument() {
        LocalDateTime ldt = LocalDateTime.of(-292277022, 1, 27, 0, 0, 0, 0);
        assertThrows(RuntimeException.class, () -> BigDecimalConversions.toTimestamp(ldt, converter));
    }
}
