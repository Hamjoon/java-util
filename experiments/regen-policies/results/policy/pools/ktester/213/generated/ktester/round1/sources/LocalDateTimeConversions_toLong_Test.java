package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toLong_Test {

    /**
     * Converter used for all tests – it is stateless after construction.
     */
    private static Converter converter;

    /* --------------------------------------------------------------
     *  Helper for Calendar initialisation (Java 8 style lambda)
     * -------------------------------------------------------------- */
    @FunctionalInterface
    private interface CalendarConsumer {

        void accept(Calendar cal);
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance; the default options are sufficient for the tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release; the static converter will be garbage‑collected after the JVM shuts down.
        converter = null;
    }

    /**
     * Provides all cases that should return a concrete epoch‑millisecond value.
     */
    private static Stream<Arguments> valueConversionProvider() {
        return Stream.of(// java.util.Date
        Arguments.of(new Date(1_640_995_200_000L), new NoOpConverter(), 1_640_995_200_000L, "Convert java.util.Date to epoch millis"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2022-01-01 12:34:56.789"), new NoOpConverter(), 1_641_046_496_789L, "Convert java.sql.Timestamp to epoch millis (UTC test environment)"), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1_640_995_200_000L), new NoOpConverter(), 1_640_995_200_000L, "Instant should be returned unchanged"), // LocalDateTime with explicit ZoneOffsetConverter (UTC)
        Arguments.of(LocalDateTime.of(2022, 1, 1, 0, 0, 0), new ZoneOffsetConverter(ZoneOffset.UTC), 1_640_995_200_000L, "LocalDateTime midnight UTC → epoch millis"), // Long wrapper
        Arguments.of(Long.valueOf(1_640_995_200_000L), new NoOpConverter(), 1_640_995_200_000L, "Long value passed through unchanged"), // Numeric String
        Arguments.of("1640995200000", new NoOpConverter(), 1_640_995_200_000L, "String containing a number should be parsed to long"), // LocalDate (2022‑01‑02) with UTC offset
        Arguments.of(LocalDate.of(2022, 1, 2), new ZoneOffsetConverter(ZoneOffset.UTC), 1_641_081_600_000L, "LocalDate interpreted as start‑of‑day UTC"), // LocalDate (1970‑01‑01) with UTC offset → epoch start
        Arguments.of(LocalDate.of(1970, 1, 1), new ZoneOffsetConverter(ZoneOffset.UTC), 0L, "Epoch day should map to 0 ms"), // LocalTime → epoch date (1970‑01‑01) with UTC offset
        Arguments.of(LocalTime.of(12, 34, 56, 789_000_000), new ZoneOffsetConverter(ZoneOffset.UTC), 45_296_789L, "LocalTime mapped onto epoch date"), // ZonedDateTime (Europe/Paris) → instant 2022‑01‑01T11:00:00Z
        Arguments.of(ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Paris")), new NoOpConverter(), 1_640_991_600_000L, "ZonedDateTime with its own zone should be converted to epoch millis"), // OffsetDateTime (‑02:00) → instant 2022‑01‑01T14:00:00Z
        Arguments.of(OffsetDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-2)), new NoOpConverter(), 1_640_998_800_000L, "OffsetDateTime with offset –02:00 → epoch millis"), // YearMonth (2022‑02) → 2022‑02‑01T00:00:00Z
        Arguments.of(YearMonth.of(2022, 2), new ZoneOffsetConverter(ZoneOffset.UTC), 1_643_673_600_000L, "YearMonth interpreted as first day of month UTC"), // Year (2023) → 2023‑01‑01T00:00:00Z
        Arguments.of(Year.of(2023), new ZoneOffsetConverter(ZoneOffset.UTC), 1_672_531_200_000L, "Year interpreted as start of year UTC"), // Calendar (UTC) set to specific instant
        Arguments.of(Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply(c -> c.setTimeInMillis(1_640_995_200_000L)), new NoOpConverter(), 1_640_995_200_000L, "Calendar in UTC should return its epoch millis"), // java.sql.Date (midnight UTC)
        Arguments.of(java.sql.Date.valueOf("2022-01-01"), new NoOpConverter(), 1_640_995_200_000L, "java.sql.Date interpreted as midnight UTC"), // BigDecimal containing epoch millis
        Arguments.of(new BigDecimal("1640995200000"), new NoOpConverter(), 1_640_995_200_000L, "BigDecimal should be converted to long"), // BigInteger containing epoch millis
        Arguments.of(new BigInteger("1640995200000"), new NoOpConverter(), 1_640_995_200_000L, "BigInteger should be converted to long"), // AtomicLong holding epoch millis
        Arguments.of(new AtomicLong(1_640_995_200_000L), new NoOpConverter(), 1_640_995_200_000L, "AtomicLong should be unwrapped to long"), // LocalDateTime with custom ZoneOffset (‑05:00)
        Arguments.of(LocalDateTime.of(2022, 1, 1, 0, 0, 0), new ZoneOffsetConverter(ZoneOffset.ofHours(-5)), 1_641_013_200_000L, "LocalDateTime with ZoneOffset –05:00 → epoch millis"));
    }

    /**
     * Provides all cases that should result in an exception.
     */
    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Null source – should NPE (delegates to toInstant which validates)
        Arguments.of(null, new NoOpConverter(), NullPointerException.class, "Null source must cause NullPointerException"), // Unsupported type Boolean
        Arguments.of(Boolean.TRUE, new NoOpConverter(), ConversionException.class, "Boolean source should trigger ConversionException"), // LocalDateTime without zone offset (NoOpConverter supplies none)
        Arguments.of(LocalDateTime.of(2022, 1, 1, 12, 0, 0), new NoOpConverter(), ConversionException.class, "LocalDateTime without ZoneOffset → ConversionException"), // LocalDate without zone offset
        Arguments.of(LocalDate.of(2022, 1, 1), new NoOpConverter(), ConversionException.class, "LocalDate without ZoneOffset → ConversionException"), // LocalTime without zone offset
        Arguments.of(LocalTime.of(12, 34, 56), new NoOpConverter(), ConversionException.class, "LocalTime without ZoneOffset → ConversionException"), // YearMonth without zone offset
        Arguments.of(YearMonth.of(2022, 5), new NoOpConverter(), ConversionException.class, "YearMonth without ZoneOffset → ConversionException"), // Year without zone offset
        Arguments.of(Year.of(2022), new NoOpConverter(), ConversionException.class, "Year without ZoneOffset → ConversionException"), // Empty string
        Arguments.of("", new NoOpConverter(), ConversionException.class, "Empty string should cause ConversionException"), // Whitespace‑only string
        Arguments.of("   ", new NoOpConverter(), ConversionException.class, "Whitespace‑only string should cause ConversionException"), // Numeric string overflow (greater than Long.MAX_VALUE)
        Arguments.of("9223372036854775808", new NoOpConverter(), ConversionException.class, "Overflowing numeric string should cause ConversionException"), // Double value (unsupported)
        Arguments.of(Double.valueOf(12345.67), new NoOpConverter(), ConversionException.class, "Double source should trigger ConversionException"), // Float value (unsupported)
        Arguments.of(Float.valueOf(12.34f), new NoOpConverter(), ConversionException.class, "Float source should trigger ConversionException"), // Short value (unsupported)
        Arguments.of(Short.valueOf((short) 123), new NoOpConverter(), ConversionException.class, "Short source should trigger ConversionException"), // Integer value (unsupported)
        Arguments.of(Integer.valueOf(123_456), new NoOpConverter(), ConversionException.class, "Integer source should trigger ConversionException"), // Null converter
        Arguments.of(new Date(1_640_995_200_000L), null, NullPointerException.class, "Null converter should cause NullPointerException"));
    }

    // Utility method to apply a consumer to a newly created Calendar instance.
    private static Calendar apply(CalendarConsumer consumer) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        consumer.accept(cal);
        return cal;
    }

    @Test
    void toLongFromInstant_returnsEpochMilli() {
        Instant now = Instant.now();
        long expected = now.toEpochMilli();
        long actual = LocalDateTimeConversions.toLong(now, converter);
        assertEquals(expected, actual, "toLong should return the same epoch milli as the supplied Instant");
    }

    @Test
    void toLongFromDate_returnsEpochMilli() {
        long epoch = System.currentTimeMillis();
        Date date = new Date(epoch);
        // Date stores milliseconds since epoch directly
        long expected = epoch;
        long actual = LocalDateTimeConversions.toLong(date, converter);
        assertEquals(expected, actual, "toLong should return the same epoch milli as the supplied java.util.Date");
    }

    @Test
    void toLongFromLocalDateTime_usesConverterToInstant() {
        // Choose a deterministic LocalDateTime to avoid flaky tests caused by time‑zones.
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MARCH, 15, 12, 30, 45, 123_000_000);
        // The Converter decides which zone to use; we let it do the conversion and then compare.
        Instant instant = converter.convert(ldt, Instant.class);
        long expected = instant.toEpochMilli();
        long actual = LocalDateTimeConversions.toLong(ldt, converter);
        assertEquals(expected, actual, "toLong should convert LocalDateTime to Instant via the Converter and return its epoch milli");
    }

    @Test
    void toLongFromCalendar_returnsEpochMilli() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // a fixed point in time
        cal.setTimeInMillis(1_600_000_000_000L);
        long expected = cal.getTimeInMillis();
        long actual = LocalDateTimeConversions.toLong(cal, converter);
        assertEquals(expected, actual, "toLong should return the same epoch milli as the supplied Calendar");
    }

    @Test
    void toLongFromNull_returnsZero() {
        long actual = LocalDateTimeConversions.toLong(null, converter);
        assertEquals(0L, actual, "Conversion of a null source should yield 0 (the epoch start) for a long value");
    }

    @Test
    void toLongFromMapWithTimeKey_returnsEpochMilli() {
        // The Converter can turn a Map with a "time" entry into an Instant.
        long epoch = 1_650_000_000_000L;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("time", epoch);
        long expected = epoch;
        long actual = LocalDateTimeConversions.toLong(map, converter);
        assertEquals(expected, actual, "toLong should interpret a Map containing a \"time\" key as epoch milliseconds");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("valueConversionProvider")
    void toLong_successfulConversion(Object from, Converter conv, long expected, String description) {
        long actual = LocalDateTimeConversions.toLong(from, conv);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionConversionProvider")
    void toLong_expectedException(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toLong(from, conv), description);
    }
}
