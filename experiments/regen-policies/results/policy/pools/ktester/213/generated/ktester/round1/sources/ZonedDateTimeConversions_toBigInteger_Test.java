package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.ZONED_DATE_TIME;

class ZonedDateTimeConversions_toBigInteger_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter constructor requires a ConverterOptions instance.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Instant source
        Arguments.of("Instant source", Instant.parse("2023-03-15T12:34:56Z"), BigInteger.valueOf(1678880096L)), // ZonedDateTime source (offset +02:00)
        Arguments.of("ZonedDateTime source with +02:00 offset", ZonedDateTime.parse("2023-03-15T12:34:56+02:00[Europe/Paris]"), BigInteger.valueOf(1678872896L)), // java.util.Date source
        Arguments.of("java.util.Date source", new Date(1678880096000L), BigInteger.valueOf(1678880096L)), // java.sql.Timestamp source (nanosecond part ignored for epoch second)
        Arguments.of("java.sql.Timestamp source", Timestamp.valueOf("2023-03-15 12:34:56.123"), BigInteger.valueOf(1678880096L)), // Long epoch‑millis source
        Arguments.of("Long epoch‑millis source", 1678880096000L, BigInteger.valueOf(1678880096L)), // String ISO‑8601 instant
        Arguments.of("String ISO‑8601 instant source", "2023-03-15T12:34:56Z", BigInteger.valueOf(1678880096L)), // String with custom pattern (uses pre‑configured CustomPatternConverter)
        Arguments.of("String with custom pattern source", "15-03-2023 12:34:56", BigInteger.valueOf(1678880096L), CustomPatternConverter.INSTANCE), // LocalDateTime source (treated as UTC)
        Arguments.of("LocalDateTime source (treated as UTC)", LocalDateTime.of(2023, 3, 15, 12, 34, 56), BigInteger.valueOf(1678880096L)), // OffsetDateTime source
        Arguments.of("OffsetDateTime source with +02:00 offset", OffsetDateTime.parse("2023-03-15T12:34:56+02:00"), BigInteger.valueOf(1678872896L)), // Calendar source (UTC)
        Arguments.of("Calendar source (UTC)", utcCalendarWithMillis(1678880096000L), BigInteger.valueOf(1678880096L)), // AtomicLong source
        Arguments.of("AtomicLong source", new AtomicLong(1678880096000L), BigInteger.valueOf(1678880096L)), // BigDecimal source (epoch‑millis)
        Arguments.of("BigDecimal source (epoch‑millis)", new BigDecimal("1678880096000"), BigInteger.valueOf(1678880096L)), // Map with ZONED_DATE_TIME key
        Arguments.of("Map source containing ZONED_DATE_TIME entry", mapWithZonedDateTime("2023-03-15T12:34:56+02:00[Europe/Paris]"), BigInteger.valueOf(1678872896L)), // Instant.MAX
        Arguments.of("Instant.MAX source", Instant.MAX, BigInteger.valueOf(Long.MAX_VALUE)), // Instant.MIN
        Arguments.of("Instant.MIN source", Instant.MIN, BigInteger.valueOf(Long.MIN_VALUE)), // java.sql.Date source
        Arguments.of("java.sql.Date source", new java.sql.Date(1678880096000L), BigInteger.valueOf(1678880096L)), // Long.MAX_VALUE epoch‑millis
        Arguments.of("Long.MAX_VALUE epoch‑millis source", Long.MAX_VALUE, BigInteger.valueOf(Long.MAX_VALUE / 1000L)), // Long.MIN_VALUE epoch‑millis
        Arguments.of("Long.MIN_VALUE epoch‑millis source", Long.MIN_VALUE, BigInteger.valueOf(Long.MIN_VALUE / 1000L)), // YearMonth source (first day of month at start of day UTC)
        Arguments.of("YearMonth source", YearMonth.of(2023, 3), BigInteger.valueOf(1677628800L)), // Year source (Jan 1st 00:00 UTC)
        Arguments.of("Year source", Year.of(2023), BigInteger.valueOf(1672531200L)));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// null source
        Arguments.of("null source", null, DefaultConverter.INSTANCE, NullPointerException.class), // unsupported type (plain Object)
        Arguments.of("unsupported plain Object", new Object(), DefaultConverter.INSTANCE, IllegalArgumentException.class), // malformed String
        Arguments.of("malformed String", "not-a-date", DefaultConverter.INSTANCE, DateTimeParseException.class), // LocalTime (no date component)
        Arguments.of("LocalTime source", LocalTime.of(12, 34, 56), DefaultConverter.INSTANCE, IllegalArgumentException.class), // MonthDay (no year)
        Arguments.of("MonthDay source", MonthDay.of(2, 29), DefaultConverter.INSTANCE, IllegalArgumentException.class), // null converter
        Arguments.of("null converter", Instant.parse("2023-03-15T12:34:56Z"), null, NullPointerException.class), // Map without ZONED_DATE_TIME key
        Arguments.of("Map without ZONED_DATE_TIME key", mapWithoutZonedDateTime(), DefaultConverter.INSTANCE, IllegalArgumentException.class), // Map with null ZONED_DATE_TIME value
        Arguments.of("Map with null ZONED_DATE_TIME value", mapWithNullZonedDateTime(), DefaultConverter.INSTANCE, NullPointerException.class), // empty String
        Arguments.of("empty String source", "", DefaultConverter.INSTANCE, DateTimeParseException.class), // unsupported numeric type Float
        Arguments.of("unsupported Float source", 12.34f, DefaultConverter.INSTANCE, IllegalArgumentException.class), // ZoneId alone
        Arguments.of("ZoneId source", ZoneId.of("Europe/Paris"), DefaultConverter.INSTANCE, IllegalArgumentException.class));
    }

    private static Calendar utcCalendarWithMillis(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Map<String, Object> mapWithZonedDateTime(String zdtString) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(ZONED_DATE_TIME, ZonedDateTime.parse(zdtString));
        return map;
    }

    private static Map<String, Object> mapWithoutZonedDateTime() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("someOtherKey", ZonedDateTime.now());
        return map;
    }

    private static Map<String, Object> mapWithNullZonedDateTime() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(ZONED_DATE_TIME, null);
        return map;
    }

    // Adjust the original provider to include a placeholder for the optional custom converter.
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("Instant source", Instant.parse("2023-03-15T12:34:56Z"), BigInteger.valueOf(1678880096L), null), Arguments.of("ZonedDateTime source with +02:00 offset", ZonedDateTime.parse("2023-03-15T12:34:56+02:00[Europe/Paris]"), BigInteger.valueOf(1678872896L), null), Arguments.of("java.util.Date source", new Date(1678880096000L), BigInteger.valueOf(1678880096L), null), Arguments.of("java.sql.Timestamp source", Timestamp.valueOf("2023-03-15 12:34:56.123"), BigInteger.valueOf(1678880096L), null), Arguments.of("Long epoch‑millis source", 1678880096000L, BigInteger.valueOf(1678880096L), null), Arguments.of("String ISO‑8601 instant source", "2023-03-15T12:34:56Z", BigInteger.valueOf(1678880096L), null), Arguments.of("String with custom pattern source", "15-03-2023 12:34:56", BigInteger.valueOf(1678880096L), CustomPatternConverter.INSTANCE), Arguments.of("LocalDateTime source (treated as UTC)", LocalDateTime.of(2023, 3, 15, 12, 34, 56), BigInteger.valueOf(1678880096L), null), Arguments.of("OffsetDateTime source with +02:00 offset", OffsetDateTime.parse("2023-03-15T12:34:56+02:00"), BigInteger.valueOf(1678872896L), null), Arguments.of("Calendar source (UTC)", utcCalendarWithMillis(1678880096000L), BigInteger.valueOf(1678880096L), null), Arguments.of("AtomicLong source", new AtomicLong(1678880096000L), BigInteger.valueOf(1678880096L), null), Arguments.of("BigDecimal source (epoch‑millis)", new BigDecimal("1678880096000"), BigInteger.valueOf(1678880096L), null), Arguments.of("Map source containing ZONED_DATE_TIME entry", mapWithZonedDateTime("2023-03-15T12:34:56+02:00[Europe/Paris]"), BigInteger.valueOf(1678872896L), null), Arguments.of("Instant.MAX source", Instant.MAX, BigInteger.valueOf(Long.MAX_VALUE), null), Arguments.of("Instant.MIN source", Instant.MIN, BigInteger.valueOf(Long.MIN_VALUE), null), Arguments.of("java.sql.Date source", new java.sql.Date(1678880096000L), BigInteger.valueOf(1678880096L), null), Arguments.of("Long.MAX_VALUE epoch‑millis source", Long.MAX_VALUE, BigInteger.valueOf(Long.MAX_VALUE / 1000L), null), Arguments.of("Long.MIN_VALUE epoch‑millis source", Long.MIN_VALUE, BigInteger.valueOf(Long.MIN_VALUE / 1000L), null), Arguments.of("YearMonth source", YearMonth.of(2023, 3), BigInteger.valueOf(1677628800L), null), Arguments.of("Year source", Year.of(2023), BigInteger.valueOf(1672531200L), null));
    }

    /**
     * Verify that a {@link ZonedDateTime} is correctly converted to a {@link BigInteger}
     * representing the number of nanoseconds since the epoch.
     */
    @Test
    void testZonedDateTimeToBigInteger() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2020, 5, 15, 10, 30, 45, 123_456_789, ZoneOffset.UTC);
        // WHEN
        BigInteger result = ZonedDateTimeConversions.toBigInteger(zdt, converter);
        // THEN
        Instant instant = zdt.toInstant();
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(instant.getNano()));
        assertEquals(expected, result, "BigInteger conversion of ZonedDateTime does not match expected nanoseconds value");
    }

    /**
     * Verify that an {@link OffsetDateTime} is also handled correctly.
     */
    @Test
    void testOffsetDateTimeToBigInteger() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 999_000_000, ZoneOffset.ofHours(-5));
        // WHEN
        BigInteger result = ZonedDateTimeConversions.toBigInteger(odt, converter);
        // THEN
        Instant instant = odt.toInstant();
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(instant.getNano()));
        assertEquals(expected, result, "BigInteger conversion of OffsetDateTime does not match expected nanoseconds value");
    }

    /**
     * Verify that a {@code null} source yields {@code null} as the result.
     */
    @Test
    void testNullInputReturnsNull() {
        // GIVEN
        Object from = null;
        // WHEN
        BigInteger result = ZonedDateTimeConversions.toBigInteger(from, converter);
        // THEN
        assertNull(result, "Converting a null source should return null");
    }

    /**
     * Verify that a {@link Map} containing the standard keys used by the
     * conversion infrastructure can be turned into a {@link BigInteger}.
     * The map mimics the structure that {@link Converter} expects for date‑time
     * values (e.g., a key named {@code "time"} holding epoch milliseconds).
     */
    @Test
    void testMapRepresentationToBigInteger() {
        // GIVEN
        Instant instant = Instant.now();
        Map<String, Object> map = new HashMap<>();
        // The conversion layer looks for a "time" key holding a long value (epoch millis)
        map.put("time", instant.toEpochMilli());
        // WHEN
        BigInteger result = ZonedDateTimeConversions.toBigInteger(map, converter);
        // THEN
        // The map is first turned into an Instant, then to BigInteger (nanoseconds)
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(instant.getNano()));
        assertEquals(expected, result, "BigInteger conversion from map representation does not match expected value");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void testValidConversions(String description, Object from, BigInteger expected) {
        BigInteger result = ZonedDateTimeConversions.toBigInteger(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void testInvalidConversions(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toBigInteger(from, conv), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void testValidConversions(String description, Object from, BigInteger expected, Converter customConverter) {
        Converter conv = (customConverter != null) ? customConverter : converter;
        BigInteger result = ZonedDateTimeConversions.toBigInteger(from, conv);
        assertEquals(expected, result, description);
    }
}
