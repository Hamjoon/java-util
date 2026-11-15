package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toDouble_Test {

    /**
     * Shared Converter instance for all tests.
     */
    private static Converter converter;

    /**
     * Shared Converter instance for successful conversion tests.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the library provides a no‑arg constructor for options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but tidy.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        // Helper objects used in multiple arguments
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 7.75 seconds
        utcCal.setTimeInMillis(7_750L);
        Calendar laCal = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("America/Los_Angeles")).setInstant(0L).build();
        return Stream.of(// null source → Duration.ZERO → 0.0
        Arguments.of(null, new NoOpConverter(), 0.0, "null source should yield 0.0"), // Whole‑second numeric wrappers
        Arguments.of(Long.valueOf(12_345L), new NoOpConverter(), 12_345.0, "Long representing whole seconds"), Arguments.of(Integer.valueOf(42), new NoOpConverter(), 42.0, "Integer representing whole seconds"), Arguments.of(Double.valueOf(12.345), new NoOpConverter(), 12.345, "Double with fractional seconds"), Arguments.of(Float.valueOf(3.14f), new NoOpConverter(), 3.14, "Float with fractional seconds"), // BigDecimal / BigInteger
        Arguments.of(new BigDecimal("123.456"), new NoOpConverter(), 123.456, "BigDecimal preserving fractional part"), Arguments.of(new BigInteger("9876543210"), new NoOpConverter(), 9_876_543_210.0, "BigInteger as whole seconds"), // java.util.Date (epoch milliseconds)
        Arguments.of(new Date(1_500L), new NoOpConverter(), 1.5, "Date 1500 ms after epoch → 1.5 seconds"), // java.sql.Timestamp (millis + nanos)
        Arguments.of(Timestamp.from(Instant.ofEpochSecond(10).plusNanos(123_456_789)), new NoOpConverter(), 10.123456789, "Timestamp with seconds and nanos"), // java.time.Duration (positive & negative)
        Arguments.of(Duration.ofSeconds(5, 250_000_000L), new NoOpConverter(), 5.25, "Duration of 5.25 seconds"), Arguments.of(Duration.ofSeconds(-5).plusNanos(250_000_000L), new NoOpConverter(), -4.75, "Negative Duration normalized with positive nanos"), // java.time.Instant (seconds + nanos)
        Arguments.of(Instant.ofEpochSecond(20, 500_000_000L), new NoOpConverter(), 20.5, "Instant with nanos"), // java.time.LocalDateTime (interpreted as UTC)
        Arguments.of(LocalDateTime.ofEpochSecond(3, 500_000_000, ZoneOffset.UTC), new NoOpConverter(), 3.5, "LocalDateTime of 3.5 seconds since epoch (UTC)"), // java.time.OffsetDateTime (any offset)
        Arguments.of(OffsetDateTime.ofInstant(Instant.ofEpochSecond(3), ZoneOffset.UTC), new NoOpConverter(), 3.0, "OffsetDateTime at epoch second 3"), Arguments.of(OffsetDateTime.ofInstant(Instant.ofEpochSecond(15, 750_000_000L), ZoneOffset.ofHours(-5)), new NoOpConverter(), 15.75, "OffsetDateTime with -05:00 offset, same instant"), // java.time.ZonedDateTime (UTC zone)
        Arguments.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(7, 250_000_000L), ZoneId.of("UTC")), new NoOpConverter(), 7.25, "ZonedDateTime in UTC"), // java.sql.Date (date‑only, no time‑of‑day)
        Arguments.of(Date.valueOf("1970-01-01"), new NoOpConverter(), 0.0, "SQL Date epoch day → 0 seconds"), Arguments.of(Date.valueOf("1970-01-02"), new NoOpConverter(), 86_400.0, "SQL Date one day after epoch → 86 400 seconds"), // java.util.Calendar (UTC)
        Arguments.of(utcCal, new NoOpConverter(), 7.75, "Calendar in UTC with 7 750 ms → 7.75 seconds"), // Calendar with non‑UTC zone but epoch‑millis 0
        Arguments.of(laCal, new NoOpConverter(), 0.0, "Calendar in LA zone with instant 0 → 0 seconds"), // ISO‑8601 strings
        Arguments.of("1970-01-01T00:00:04Z", new NoOpConverter(), 4.0, "ISO‑8601 UTC string"), Arguments.of("1970-01-01T00:00:00.123Z", new NoOpConverter(), 0.123, "ISO‑8601 with fractional seconds"), Arguments.of("1970-01-01T00:00:04+02:00", new NoOpConverter(), 2.0, "ISO‑8601 with +02:00 offset → 2 seconds UTC"), // Negative java.util.Date
        Arguments.of(new Date(-1_500L), new NoOpConverter(), -1.5, "Date with negative epoch milliseconds"));
    }

    private static Stream<Arguments> provideExceptionConversions() {
        return Stream.of(// Unsupported type
        Arguments.of(Boolean.TRUE, new NoOpConverter(), IllegalArgumentException.class, "Boolean should trigger IllegalArgumentException"), // Malformed ISO‑8601 string
        Arguments.of("not-a-date", new NoOpConverter(), DateTimeParseException.class, "Invalid date string should cause DateTimeParseException"), // Null converter
        Arguments.of(null, null, NullPointerException.class, "Null converter must result in NullPointerException"), // NaN value
        Arguments.of(Double.NaN, new NoOpConverter(), IllegalArgumentException.class, "NaN is not a valid number of seconds"), // Positive infinity
        Arguments.of(Double.POSITIVE_INFINITY, new NoOpConverter(), IllegalArgumentException.class, "Infinity cannot be mapped to a finite Duration"), // Arbitrary object not matching any conversion path
        Arguments.of(new Object(), new NoOpConverter(), IllegalArgumentException.class, "Unsupported object type should raise IllegalArgumentException"));
    }

    @Test
    @DisplayName("Convert java.time.Duration to double")
    void testToDoubleFromDuration() {
        // 5.5 seconds
        Duration duration = Duration.ofSeconds(5, 500_000_000);
        double result = TimestampConversions.toDouble(duration, converter);
        assertEquals(5.5, result, 1e-12, "Duration of 5.5 seconds should convert to 5.5");
    }

    @Test
    @DisplayName("Convert java.util.Date (epoch milliseconds) to double")
    void testToDoubleFromDate() {
        // 2.345 seconds after epoch
        Date date = new Date(2_345L);
        double expected = date.getTime() / 1000.0;
        double result = TimestampConversions.toDouble(date, converter);
        assertEquals(expected, result, 1e-12, "Date milliseconds should be converted to seconds as double");
    }

    @Test
    @DisplayName("Convert java.time.Instant to double")
    void testToDoubleFromInstant() {
        // 10.25 seconds
        Instant instant = Instant.ofEpochSecond(10, 250_000_000);
        double result = TimestampConversions.toDouble(instant, converter);
        assertEquals(10.25, result, 1e-12, "Instant of 10.25 seconds should convert to 10.25");
    }

    @Test
    @DisplayName("Convert java.sql.Timestamp to double")
    void testToDoubleFromTimestamp() {
        // 2 seconds after epoch (milliseconds)
        Timestamp ts = new Timestamp(2_000L);
        double expected = ts.getTime() / 1000.0;
        double result = TimestampConversions.toDouble(ts, converter);
        assertEquals(expected, result, 1e-12, "Timestamp milliseconds should be converted to seconds as double");
    }

    @Test
    @DisplayName("Convert java.time.LocalDateTime (UTC) to double")
    void testToDoubleFromLocalDateTime() {
        // 3 seconds and 500,000,000 nanoseconds after epoch, interpreted as UTC.
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(3, 500_000_000, ZoneOffset.UTC);
        double result = TimestampConversions.toDouble(ldt, converter);
        assertEquals(3.5, result, 1e-12, "LocalDateTime of 3.5 seconds (UTC) should convert to 3.5");
    }

    @Test
    @DisplayName("Convert Map with 'time' and 'nanos' keys to double")
    void testToDoubleFromMap() {
        // The library’s Map conversion expects keys "time" (seconds) and "nanos".
        Map<String, Object> map = new CompactMap<>();
        // 2 seconds
        map.put("time", 2L);
        // +0.3 seconds
        map.put("nanos", 300_000_000);
        double result = TimestampConversions.toDouble(map, converter);
        assertEquals(2.3, result, 1e-12, "Map with time=2 and nanos=300_000_000 should convert to 2.3");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideSuccessfulConversions")
    void testToDoubleSuccess(Object from, Converter converter, double expected, String description) {
        double result = TimestampConversions.toDouble(from, converter);
        assertEquals(expected, result, 1e-12, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideExceptionConversions")
    void testToDoubleExceptions(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> TimestampConversions.toDouble(from, converter), description);
    }
}
