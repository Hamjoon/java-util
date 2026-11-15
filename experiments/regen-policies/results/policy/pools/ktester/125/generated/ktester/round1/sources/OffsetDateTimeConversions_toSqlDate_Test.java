package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OffsetDateTimeConversions_toSqlDate_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared Converter instance used by most tests.
     */
    private static Converter defaultConverter;

    /**
     * Converter that is explicitly used for cases that assume UTC (same instance after forcing UTC TZ).
     */
    private static Converter utcConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Force UTC to make date‑only / time‑only conversions deterministic.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        defaultConverter = new Converter(new ConverterOptions());
        // after the TZ change both behave the same.
        utcConverter = defaultConverter;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// ---- Group: OffsetDateTimeConversions.toSqlDate (basic) ----
        arguments(1622548800000L, defaultConverter, 1622548800000L, "Long epoch millis should be passed through unchanged"), arguments(Integer.valueOf(1622548800), defaultConverter, 1622548800L, "Integer numeric value should be widened to long"), arguments("1622548800000", defaultConverter, 1622548800000L, "String containing a numeric timestamp should be parsed to long"), arguments(new Date(1622548800000L), defaultConverter, 1622548800000L, "java.util.Date instance should be converted using its time value"), arguments(Timestamp.valueOf("2021-06-01 00:00:00"), defaultConverter, 1622548800000L, "java.sql.Timestamp should be treated like a Date"), arguments(LocalDateTime.of(2021, 6, 1, 0, 0), utcConverter, 1622548800000L, "LocalDateTime interpreted as UTC midnight"), arguments(OffsetDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2)), defaultConverter, 1622541600000L, "OffsetDateTime +02:00 should be converted to UTC epoch millis"), // ---- Group: OffsetDateTimeConversions.toSqlDate_additional ----
        arguments(Instant.ofEpochMilli(1622548800000L), defaultConverter, 1622548800000L, "Instant should yield its epoch millis directly"), arguments(createUtcCalendar(1622548800000L), defaultConverter, 1622548800000L, "Calendar set to a specific UTC epoch should be converted correctly"), arguments(BigDecimal.valueOf(1622548800000L), defaultConverter, 1622548800000L, "BigDecimal representing a timestamp should be converted"), arguments(BigInteger.valueOf(1622548800000L), defaultConverter, 1622548800000L, "BigInteger representing a timestamp should be converted"), arguments(new AtomicLong(1622548800000L), defaultConverter, 1622548800000L, "AtomicLong containing epoch millis should be converted"), arguments(LocalDate.of(2021, 6, 1), utcConverter, 1622505600000L, "LocalDate should be interpreted as midnight UTC"), arguments(LocalTime.of(12, 34, 56), utcConverter, 45_296_000L, "LocalTime should be interpreted on the epoch day (1970‑01‑01)"), arguments(OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(-5)), utcConverter, 61_200_000L, "OffsetTime -05:00 should be adjusted to UTC (17:00) on epoch day"), arguments(ZonedDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneId.of("America/New_York")), defaultConverter, 1622520000000L, "ZonedDateTime in America/New_York (EDT) should be converted to UTC epoch millis"), arguments("2021-06-01T00:00:00Z", defaultConverter, 1622505600000L, "ISO‑8601 string with Zulu designator should be parsed correctly"), arguments(Long.MAX_VALUE, defaultConverter, Long.MAX_VALUE, "Maximum long value should be accepted as a timestamp"), arguments(-1L, defaultConverter, -1L, "Negative epoch millis (pre‑1970) should be handled"));
    }

    /**
     * Helper that creates a {@link Calendar} instance set to UTC and the supplied epoch millis.
     */
    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    private static Stream<Arguments> exceptionalConversionProvider() {
        return Stream.of(// ---- Group: OffsetDateTimeConversions.toSqlDate (null source) ----
        arguments(null, defaultConverter, NullPointerException.class, "Null source should cause NullPointerException inside toLong"), // ---- Group: OffsetDateTimeConversions.toSqlDate_exceptions ----
        arguments(new Object(), defaultConverter, IllegalArgumentException.class, "Arbitrary object type is unsupported"), arguments(new int[] { 1, 2, 3 }, defaultConverter, IllegalArgumentException.class, "Array input is not supported"), arguments(Arrays.asList(1, 2), defaultConverter, IllegalArgumentException.class, "Collection input is unsupported"), arguments(Collections.singletonMap("key", "value"), defaultConverter, IllegalArgumentException.class, "Map input is unsupported"), arguments('c', defaultConverter, IllegalArgumentException.class, "Character input cannot be interpreted as a timestamp"), arguments("   ", defaultConverter, IllegalArgumentException.class, "Whitespace‑only string cannot be parsed"), arguments("9223372036854775808", defaultConverter, IllegalArgumentException.class, "Numeric string exceeding Long.MAX_VALUE should overflow"), arguments(Float.valueOf(123.45f), defaultConverter, IllegalArgumentException.class, "Floating‑point numbers are not valid epoch millis"), arguments(Double.valueOf(123.45), defaultConverter, IllegalArgumentException.class, "Double values with fractional part are invalid for epoch millis"), arguments(1622548800000L, null, NullPointerException.class, "Null Converter argument must result in NullPointerException"));
    }

    @Test
    void toSqlDate_fromLong_returnsDateWithSameEpochMillis() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        Long epochMillis = 1_642_548_800_000L;
        // WHEN
        Date result = OffsetDateTimeConversions.toSqlDate(epochMillis, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(epochMillis.longValue(), result.getTime(), "Date.getTime() must match the supplied long value");
    }

    @Test
    void toSqlDate_fromString_parsesToLongAndCreatesDate() {
        // GIVEN
        // ISO‑8601 format understood by the Converter
        String isoString = "2021-06-01T12:34:56Z";
        // WHEN
        Date result = OffsetDateTimeConversions.toSqlDate(isoString, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        // The Converter converts the string to a long representing epoch milliseconds.
        // Verify by converting the same string directly via the Converter and comparing.
        Long expectedMillis = converter.convert(isoString, Long.class);
        assertEquals(expectedMillis.longValue(), result.getTime(), "Date.getTime() must equal the long value produced by the Converter");
    }

    @Test
    void toSqlDate_fromOffsetDateTime_returnsCorrectEpochMillis() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2021, 12, 25, 15, 30, 0, 0, ZoneOffset.UTC);
        long expectedMillis = odt.toInstant().toEpochMilli();
        // WHEN
        Date result = OffsetDateTimeConversions.toSqlDate(odt, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(expectedMillis, result.getTime(), "Date.getTime() must match the epoch milliseconds of the OffsetDateTime");
    }

    @Test
    void toSqlDate_fromNull_returnsEpochZeroDate() {
        // GIVEN
        Object nullValue = null;
        // WHEN
        Date result = OffsetDateTimeConversions.toSqlDate(nullValue, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null even when source is null");
        assertEquals(0L, result.getTime(), "When source is null, Converter returns 0 for numeric primitives, so Date should be epoch start");
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("successfulConversionProvider")
    void toSqlDate_successfulConversions(Object from, Converter converter, long expectedEpochMillis, String description) {
        // WHEN
        Date result = OffsetDateTimeConversions.toSqlDate(from, converter);
        // THEN
        assertNotNull(result, "Resulting Date should never be null");
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Expected epoch millis " + expectedEpochMillis + " for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("exceptionalConversionProvider")
    void toSqlDate_exceptionalConversions(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toSqlDate(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
