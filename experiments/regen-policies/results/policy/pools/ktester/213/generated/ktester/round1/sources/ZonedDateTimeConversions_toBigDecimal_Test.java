package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigInteger;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toBigDecimal_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Converter used for the majority of the tests (default configuration).
     */
    private static final Converter DEFAULT_CONVERTER = DefaultConverter.INSTANCE;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter has a constructor that requires ConverterOptions.
        // Using the default options is sufficient for the built‑in conversions we need.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Valid conversion cases that should produce a {@link BigDecimal}.
     */
    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Instant
        Arguments.of(Instant.parse("2023-03-25T10:15:30.00Z"), new BigDecimal("1679740530"), "Direct Instant instance should be converted to its epoch seconds as BigDecimal."), // java.util.Date
        Arguments.of(Date.from(Instant.parse("2023-03-25T10:15:30.00Z")), new BigDecimal("1679740530"), "java.util.Date should be converted to Instant (via Date#getTime) and then to the same epoch seconds."), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2023-03-25 10:15:30"), new BigDecimal("1679740530"), "java.sql.Timestamp is a subclass of java.util.Date; conversion should yield the same epoch seconds."), // ZonedDateTime with +02:00 offset (Europe/Paris)
        Arguments.of(ZonedDateTime.parse("2023-03-25T10:15:30+02:00[Europe/Paris]"), new BigDecimal("1679733330"), "ZonedDateTime with +02:00 offset should be normalized to UTC instant (epoch seconds 1679733330)."), // OffsetDateTime
        Arguments.of(OffsetDateTime.parse("2023-03-25T10:15:30+02:00"), new BigDecimal("1679733330"), "OffsetDateTime conversion follows the same path as ZonedDateTime."), // Long epoch‑millis
        Arguments.of(Long.valueOf(1679740530000L), new BigDecimal("1679740530"), "Long representing milliseconds since epoch should be converted to Instant and then to epoch seconds."), // ISO‑8601 String
        Arguments.of("2023-03-25T10:15:30Z", new BigDecimal("1679740530"), "ISO‑8601 string should be parsed to Instant and then to BigDecimal."), // LocalDateTime (interpreted in default zone = UTC)
        Arguments.of(LocalDateTime.of(2023, 3, 25, 10, 15, 30), new BigDecimal("1679740530"), "LocalDateTime without zone is interpreted in the converter's default zone (UTC)."), // LocalDate (start of day UTC)
        Arguments.of(LocalDate.of(2023, 3, 25), new BigDecimal("1679703600"), "LocalDate is interpreted as the start of the day (00:00) in UTC."), // LocalTime (epoch date 1970‑01‑01 UTC)
        Arguments.of(LocalTime.of(10, 15, 30), new BigDecimal("36930"), "LocalTime is interpreted on the epoch date (1970‑01‑01) in UTC."), // Calendar set to UTC
        Arguments.of(utcCalendar(1679740530000L), new BigDecimal("1679740530"), "A Calendar set to UTC with the same millisecond instant should yield the identical epoch‑second BigDecimal."), // AtomicLong holding epoch‑millis
        Arguments.of(new AtomicLong(1679740530000L), new BigDecimal("1679740530"), "AtomicLong holding milliseconds since epoch is converted to an Instant, then to epoch‑seconds."), // BigInteger representing epoch‑millis
        Arguments.of(new BigInteger("1679740530000"), new BigDecimal("1679740530"), "BigInteger representing milliseconds since epoch follows the same path as a Long value."), // String containing epoch‑millis
        Arguments.of("1679740530000", new BigDecimal("1679740530"), "A numeric string is interpreted as milliseconds since epoch."), // Map with ZONED_DATE_TIME key
        Arguments.of(mapWithZonedDateTime("2023-03-25T10:15:30+02:00[Europe/Paris]"), new BigDecimal("1679733330"), "Map containing the ZONED_DATE_TIME key with an ISO‑8601 ZonedDateTime string."));
    }

    /**
     * Cases that should result in an {@link IllegalArgumentException}.
     */
    static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(// Boolean (unsupported type)
        Arguments.of(Boolean.TRUE, "A type that cannot be mapped to an Instant should cause an IllegalArgumentException."), // Map missing ZONED_DATE_TIME key
        Arguments.of(new LinkedHashMap<String, Object>() {

            {
                put("otherKey", "2023-03-25T10:15:30Z");
            }
        }, "A map that does not contain the required ZONED_DATE_TIME entry should cause an IllegalArgumentException."), // Float (unsupported numeric type)
        Arguments.of(Float.valueOf(12345.67f), "A Float (representing milliseconds) is not a supported numeric type for Instant conversion."), // Year (unsupported temporal type)
        Arguments.of(Year.of(2023), "A Year instance cannot be mapped to an Instant, so the conversion should raise an IllegalArgumentException."));
    }

    /**
     * Cases that should result in a {@link DateTimeParseException}.
     */
    static Stream<Arguments> dateTimeParseProvider() {
        return Stream.of(// Malformed ISO‑8601 string
        Arguments.of("2023-13-40T25:61:61Z", "A string that does not conform to ISO‑8601 format should cause a DateTimeParseException."), // Map with invalid ZONED_DATE_TIME value
        Arguments.of(mapWithZonedDateTime("invalid-date-time"), "When the map contains the ZONED_DATE_TIME key but its value cannot be parsed as a ZonedDateTime."));
    }

    /**
     * Cases that should result in a {@link NumberFormatException}.
     */
    static Stream<Arguments> numberFormatProvider() {
        return Stream.of(Arguments.of("notANumber", "A string that cannot be interpreted as epoch‑millisecond numeric value should result in a NumberFormatException."));
    }

    /**
     * Creates a {@link Calendar} instance set to UTC with the supplied epoch‑millis.
     */
    private static Calendar utcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Builds a map containing the {@code ZONED_DATE_TIME} key with the given value.
     */
    private static Map<String, Object> mapWithZonedDateTime(String zonedDateTimeString) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.ZONED_DATE_TIME, zonedDateTimeString);
        return map;
    }

    /**
     * Verify that a {@link ZonedDateTime} is correctly converted to a {@link BigDecimal}
     * representing the epoch seconds with nanosecond precision.
     */
    @Test
    void toBigDecimal_fromZonedDateTime_returnsExpectedBigDecimal() {
        // Given
        // 2021‑01‑01T00:00:00.123456789Z
        Instant instant = Instant.ofEpochSecond(1_609_459_200L, 123_456_789);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
        // When
        BigDecimal result = ZonedDateTimeConversions.toBigDecimal(zdt, converter);
        // Then
        BigDecimal expected = new BigDecimal("1609459200.123456789");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    /**
     * Verify that a {@link java.util.Date} (millisecond precision) is correctly converted.
     */
    @Test
    void toBigDecimal_fromDate_returnsExpectedBigDecimal() {
        // Given
        // exactly 2021‑01‑01T00:00:00Z
        long epochMilli = 1_609_459_200_000L;
        Date date = new Date(epochMilli);
        // When
        BigDecimal result = ZonedDateTimeConversions.toBigDecimal(date, converter);
        // Then
        // Date has only millisecond precision → nanoseconds part is zero.
        BigDecimal expected = new BigDecimal("1609459200");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    /**
     * Verify that an {@link Instant} is correctly converted.
     */
    @Test
    void toBigDecimal_fromInstant_returnsExpectedBigDecimal() {
        // Given
        // 2020‑09‑13T12:26:40.5Z
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 500_000_000);
        // When
        BigDecimal result = ZonedDateTimeConversions.toBigDecimal(instant, converter);
        // Then
        BigDecimal expected = new BigDecimal("1600000000.5");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    /**
     * Verify that {@code null} input yields {@code null} output.
     */
    @Test
    void toBigDecimal_fromNull_returnsNull() {
        // When
        BigDecimal result = ZonedDateTimeConversions.toBigDecimal(null, converter);
        // Then
        assertNull(result, "Converting null should return null");
    }

    /**
     * Verify that a numeric epoch‑millis {@link Long} is handled correctly.
     */
    @Test
    void toBigDecimal_fromLongEpochMillis_returnsExpectedBigDecimal() {
        // Given
        // 2021‑01‑01T00:00:00.123Z
        Long epochMillis = 1_609_459_200_123L;
        // When
        BigDecimal result = ZonedDateTimeConversions.toBigDecimal(epochMillis, converter);
        // Then
        // 123 ms = 0.123 seconds
        BigDecimal expected = new BigDecimal("1609459200.123");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_validConversions(Object from, BigDecimal expected, String description) {
        // When
        BigDecimal result = ZonedDateTimeConversions.toBigDecimal(from, DEFAULT_CONVERTER);
        // Then
        assertNotNull(result, "Result should not be null for valid conversion.");
        assertEquals(0, expected.compareTo(result), () -> description + " Expected " + expected + " but got " + result);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("illegalArgumentProvider")
    void toBigDecimal_illegalArgument(Object from, String description) {
        // Then
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toBigDecimal(from, DEFAULT_CONVERTER), () -> description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("dateTimeParseProvider")
    void toBigDecimal_dateTimeParseException(Object from, String description) {
        // Then
        assertThrows(DateTimeParseException.class, () -> ZonedDateTimeConversions.toBigDecimal(from, DEFAULT_CONVERTER), () -> description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("numberFormatProvider")
    void toBigDecimal_numberFormatException(Object from, String description) {
        // Then
        assertThrows(NumberFormatException.class, () -> ZonedDateTimeConversions.toBigDecimal(from, DEFAULT_CONVERTER), () -> description);
    }

    @Test
    void toBigDecimal_nullInput_returnsNull() {
        // When
        BigDecimal result = ZonedDateTimeConversions.toBigDecimal(null, DEFAULT_CONVERTER);
        // Then
        assertNull(result, "Converting null should return null");
    }

    @Test
    void toBigDecimal_nullConverter_throwsNullPointerException() {
        // Given
        Instant instant = Instant.parse("2023-03-25T10:15:30Z");
        // Then
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toBigDecimal(instant, null), "Passing a null Converter should result in NullPointerException.");
    }
}
