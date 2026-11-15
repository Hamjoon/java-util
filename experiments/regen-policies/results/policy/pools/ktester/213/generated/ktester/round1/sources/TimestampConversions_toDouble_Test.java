package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toDouble_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they provide all built‑in conversions
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Timestamp} from the given epoch seconds and nanos.
     */
    private static Timestamp ts(long epochSecond, int nano) {
        return Timestamp.from(Instant.ofEpochSecond(epochSecond, nano));
    }

    private static Stream<Arguments> validConversionProvider() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 3 seconds
        cal.setTimeInMillis(3_000L);
        return Stream.of(// Duration instance
        arguments(Duration.ofSeconds(5, 500_000_000), 5.5), // Instant instance
        arguments(Instant.ofEpochSecond(10, 250_000_000), 10.25), // java.util.Date (2000 ms)
        arguments(new Date(2_000L), 2.0), // Calendar set to 3000 ms
        arguments(cal, 3.0), // LocalDateTime interpreted as UTC
        arguments(LocalDateTime.of(1970, 1, 1, 0, 0, 7, 500_000_000), 7.5), // OffsetDateTime at epoch+12s UTC
        arguments(OffsetDateTime.ofInstant(Instant.ofEpochSecond(12), ZoneOffset.UTC), 12.0), // ISO‑8601 string in UTC
        arguments("1970-01-01T00:00:04Z", 4.0), // BigDecimal numeric value
        arguments(BigDecimal.valueOf(9.75), 9.75));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// java.sql.Timestamp (4500 ms)
        arguments(new Timestamp(4_500L), 4.5), // ZonedDateTime in UTC with nanos
        arguments(ZonedDateTime.ofInstant(Instant.ofEpochSecond(8, 250_000_000), ZoneId.of("UTC")), 8.25), // YearMonth – no time component → epoch start
        arguments(YearMonth.of(1970, 1), 0.0), // Year – start of year (epoch)
        arguments(Year.of(1970), 0.0), // MonthDay – no year/time → epoch start
        arguments(MonthDay.of(1, 1), 0.0), // BigInteger → whole seconds
        arguments(BigInteger.valueOf(15), 15.0), // Long → seconds
        arguments(Long.valueOf(7L), 7.0), // Integer → seconds
        arguments(Integer.valueOf(3), 3.0), // Double → seconds directly
        arguments(Double.valueOf(2.75), 2.75), // ISO‑8601 string with milliseconds
        arguments("1970-01-01T00:00:00.123Z", 0.123), // ISO‑8601 string with +02:00 offset (5s local → 3s UTC)
        arguments("1970-01-01T00:00:05+02:00", 3.0), // Negative instant with positive nanos
        arguments(Instant.ofEpochSecond(-5, 500_000_000), -4.5), // Duration of one nanosecond
        arguments(Duration.ofNanos(1), 1.0e-9), // Very large duration (Long.MAX_VALUE seconds)
        arguments(Duration.ofSeconds(Long.MAX_VALUE), (double) Long.MAX_VALUE));
    }

    private static Stream<Arguments> exceptionScenarioProvider() {
        return Stream.of(// Null source – according to JSON this should throw NPE
        arguments(null, NullPointerException.class), // Invalid string cannot be parsed
        arguments("not-a-date-time", IllegalArgumentException.class), // Unsupported source type Boolean
        arguments(Boolean.TRUE, IllegalArgumentException.class), // Arbitrary object
        arguments(new Object(), IllegalArgumentException.class), // Array type
        arguments(new int[] { 1, 2, 3 }, IllegalArgumentException.class));
    }

    @Test
    void testTimestampToDouble() {
        // 0 seconds, 0 nanos → 0.0
        assertEquals(0.0, TimestampConversions.toDouble(ts(0, 0), converter), 0.0);
        // 1 second, 0 nanos → 1.0
        assertEquals(1.0, TimestampConversions.toDouble(ts(1, 0), converter), 0.0);
        // 1 second, 500_000_000 nanos → 1.5
        assertEquals(1.5, TimestampConversions.toDouble(ts(1, 500_000_000), converter), 1e-12);
        // 12345 seconds, 250_000_000 nanos → 12345.25
        assertEquals(12345.25, TimestampConversions.toDouble(ts(12_345, 250_000_000), converter), 1e-12);
    }

    @Test
    void testDateToDouble() {
        // java.util.Date uses milliseconds, so we build it from an Instant
        // 2.75 seconds
        Date date = Date.from(Instant.ofEpochSecond(2, 750_000_000));
        assertEquals(2.75, TimestampConversions.toDouble(date, converter), 1e-12);
    }

    @Test
    void testLocalDateTimeToDouble() {
        // LocalDateTime interpreted as UTC for conversion
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(10, 333_333_333, ZoneOffset.UTC);
        assertEquals(10.333333333, TimestampConversions.toDouble(ldt, converter), 1e-12);
    }

    @Test
    void testDurationToDouble() {
        // Directly pass a Duration – the conversion should return the same value
        Duration d = Duration.ofSeconds(7).plusNanos(777_777_777);
        assertEquals(7.777777777, TimestampConversions.toDouble(d, converter), 1e-12);
    }

    @Test
    void testInstantToDouble() {
        // Instant is also supported by the Converter
        Instant instant = Instant.ofEpochSecond(42, 1);
        assertEquals(42.000000001, TimestampConversions.toDouble(instant, converter), 1e-12);
    }

    @Test
    void testNullInputReturnsZero() {
        // The Converter returns 0 for numeric primitives when the source is null.
        // toDuration(null, …) therefore yields Duration.ZERO, which maps to 0.0.
        assertEquals(0.0, TimestampConversions.toDouble(null, converter), 0.0);
    }

    @ParameterizedTest(name = "[{index}] toDouble({0}) = {1}")
    @MethodSource("validConversionProvider")
    void testValidConversions(Object from, double expected) {
        double actual = TimestampConversions.toDouble(from, converter);
        assertEquals(expected, actual, 1e-12);
    }

    @ParameterizedTest(name = "[{index}] toDouble({0}) = {1}")
    @MethodSource("edgeCaseProvider")
    void testEdgeCases(Object from, double expected) {
        double actual = TimestampConversions.toDouble(from, converter);
        assertEquals(expected, actual, 1e-12);
    }

    @ParameterizedTest(name = "[{index}] toDouble({0}) throws {1}")
    @MethodSource("exceptionScenarioProvider")
    void testExceptionScenarios(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> TimestampConversions.toDouble(from, converter));
    }

    @Test
    void testNullConverterThrowsNPE() {
        Instant instant = Instant.ofEpochSecond(1);
        assertThrows(NullPointerException.class, () -> TimestampConversions.toDouble(instant, null));
    }
}
