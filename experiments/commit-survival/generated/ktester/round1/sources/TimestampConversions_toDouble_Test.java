package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toDouble_Test {

    /**
     * Converter used for the tests – created once for the whole suite.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides the
        // standard set of built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the garbage collector in long test runs.
        converter = null;
    }

    private static Stream<Arguments> validNumericAndTemporalProvider() {
        return Stream.of(// Numeric Long
        arguments("Numeric Long should be interpreted as seconds", 12345L, 12345.0), // BigDecimal
        arguments("BigDecimal is accepted; its exact decimal value is converted to double", new BigDecimal("12345.678"), 12345.678), // java.util.Date (millis → seconds)
        arguments("java.util.Date is converted to epoch seconds (millis → seconds)", new Date(1_600_000_000_000L), 1_600_000_000.0), // java.sql.Timestamp with nanos
        arguments("java.sql.Timestamp includes nanoseconds; conversion yields seconds with fractional part", new Timestamp(1_600_000_000_123L), 1_600_000_000.123), // Instant with nanos
        arguments("Instant is directly mapped to seconds + nanos", Instant.ofEpochSecond(1_600_000_000, 456_000_000), 1_600_000_000.456), // LocalDateTime interpreted in system default zone (assume UTC for deterministic test)
        arguments("LocalDateTime is interpreted in the system default zone; expected epoch seconds for 2020‑09‑13T12:00:00", LocalDateTime.of(2020, 9, 13, 12, 0, 0), 1_600_003_200.0), // ZonedDateTime UTC
        arguments("ZonedDateTime (UTC) converts to the same epoch seconds as the above LocalDateTime when system zone is UTC", ZonedDateTime.of(2020, 9, 13, 12, 0, 0, 0, ZoneId.of("UTC")), 1_600_003_200.0), // OffsetDateTime with zero offset
        arguments("OffsetDateTime with zero offset yields the same epoch seconds as UTC ZonedDateTime", OffsetDateTime.of(2020, 9, 13, 12, 0, 0, 0, ZoneOffset.UTC), 1_600_003_200.0), // Calendar (UTC) with millis and nanos derived from millis
        arguments("Calendar (UTC) is converted to seconds with fractional part derived from milliseconds", calendarWithMillisAndZone(1_600_000_000_123L, "UTC"), 1_600_000_000.123));
    }

    private static Stream<Arguments> validAdditionalNumericProvider() {
        return Stream.of(arguments("Integer value should be interpreted as seconds and returned as double", Integer.valueOf(42), 42.0), arguments("Short value conversion to double", Short.valueOf((short) 7), 7.0), arguments("Byte value conversion to double", Byte.valueOf((byte) 1), 1.0), arguments("Double value should be returned unchanged", Double.valueOf(3.14), 3.14), arguments("Float value conversion to double", Float.valueOf(2.71f), 2.71), arguments("BigInteger is converted to double; precision loss is acceptable", new BigInteger("12345678901234567890"), 1.2345678901234568E19));
    }

    private static Stream<Arguments> validTimezoneOffsetsProvider() {
        return Stream.of(arguments("ZonedDateTime with New York offset (EDT, -04:00) yields correct epoch seconds", ZonedDateTime.of(2020, 9, 13, 12, 0, 0, 0, ZoneId.of("America/New_York")), 1_600_017_600.0), arguments("OffsetDateTime with -05:00 offset converts to the corresponding UTC epoch seconds", OffsetDateTime.of(2020, 9, 13, 12, 0, 0, 0, ZoneOffset.ofHours(-5)), 1_600_018_800.0));
    }

    private static Stream<Arguments> validDurationProvider() {
        return Stream.of(arguments("Direct Duration input is converted to double representing seconds with nanosecond fraction", Duration.ofSeconds(12_345, 678_000_000L), 12_345.678));
    }

    private static Stream<Arguments> edgeCaseEpochBoundariesProvider() {
        return Stream.of(arguments("Zero epoch instant should return 0.0", Instant.ofEpochSecond(0, 0), 0.0), arguments("Negative epoch second with positive nanos yields a negative fractional double", Instant.ofEpochSecond(-1, 500_000_000), -0.5));
    }

    /**
     * Creates a {@link Calendar} instance set to the supplied epoch milliseconds
     * and the given time‑zone ID.
     */
    private static Calendar calendarWithMillisAndZone(long epochMillis, String zoneId) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Verify that a {@link Timestamp} representing 1.5 seconds since the epoch is correctly
     * converted to a {@code double} value of {@code 1.5}.
     */
    @Test
    void toDouble_TimestampWithNanos_ReturnsExpectedDouble() {
        // 1 second = 1000 ms, plus 500 000 000 nanos (0.5 seconds)
        Timestamp ts = new Timestamp(1_000L);
        // adds the fractional part
        ts.setNanos(500_000_000);
        double result = TimestampConversions.toDouble(ts, converter);
        assertEquals(1.5, result, 1e-12, "Timestamp of 1.5 seconds should be converted to double 1.5");
    }

    /**
     * Verify that a {@link Date} representing the epoch (0 milliseconds) is converted to {@code 0.0}.
     */
    @Test
    void toDouble_EpochDate_ReturnsZero() {
        Date epoch = new Date(0L);
        double result = TimestampConversions.toDouble(epoch, converter);
        assertEquals(0.0, result, 1e-12, "Date at epoch should be converted to double 0.0");
    }

    /**
     * Verify that {@code null} input yields {@code 0.0}. The implementation of
     * {@link TimestampConversions#toDuration(Object, Converter)} treats {@code null}
     * as a zero‑duration, which in turn results in a double value of {@code 0.0}.
     */
    @Test
    void toDouble_NullInput_ReturnsZero() {
        double result = TimestampConversions.toDouble(null, converter);
        assertEquals(0.0, result, 1e-12, "Null input should be converted to double 0.0");
    }

    /**
     * Verify that a {@link java.time.Instant} is correctly handled.
     */
    @Test
    void toDouble_Instant_ReturnsExpectedDouble() {
        // Instant of 2.25 seconds after epoch
        java.time.Instant instant = java.time.Instant.ofEpochSecond(2, 250_000_000L);
        double result = TimestampConversions.toDouble(instant, converter);
        assertEquals(2.25, result, 1e-12, "Instant of 2.25 seconds should be converted to double 2.25");
    }

    /**
     * Verify that a {@link java.time.Duration} is directly converted.
     */
    @Test
    void toDouble_Duration_ReturnsExpectedDouble() {
        // 3.75 seconds
        Duration duration = Duration.ofSeconds(3).plusNanos(750_000_000L);
        double result = TimestampConversions.toDouble(duration, converter);
        assertEquals(3.75, result, 1e-12, "Duration of 3.75 seconds should be converted to double 3.75");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validNumericAndTemporalProvider")
    void toDouble_ValidNumericAndTemporal(String description, Object input, double expected) {
        double result = TimestampConversions.toDouble(input, DummyConverter.INSTANCE);
        assertEquals(expected, result, 1e-12, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validAdditionalNumericProvider")
    void toDouble_ValidAdditionalNumeric(String description, Object input, double expected) {
        double result = TimestampConversions.toDouble(input, DummyConverter.INSTANCE);
        assertEquals(expected, result, 1e-12, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validTimezoneOffsetsProvider")
    void toDouble_ValidTimezoneOffsets(String description, Object input, double expected) {
        double result = TimestampConversions.toDouble(input, DummyConverter.INSTANCE);
        assertEquals(expected, result, 1e-12, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validDurationProvider")
    void toDouble_ValidDuration(String description, Duration input, double expected) {
        double result = TimestampConversions.toDouble(input, DummyConverter.INSTANCE);
        assertEquals(expected, result, 1e-12, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCaseEpochBoundariesProvider")
    void toDouble_EpochBoundaries(String description, Object input, double expected) {
        double result = TimestampConversions.toDouble(input, DummyConverter.INSTANCE);
        assertEquals(expected, result, 1e-12, description);
    }

    @Test
    void toDouble_CustomIsoInstantStringConverter_ReturnsExpectedDouble() {
        String iso = "2020-09-13T12:00:00Z";
        double result = TimestampConversions.toDouble(iso, IsoInstantStringConverter.INSTANCE);
        assertEquals(1_600_003_200.0, result, 1e-12);
    }

    @Test
    void toDouble_UnsupportedObject_ThrowsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toDouble(unsupported, DummyConverter.INSTANCE));
    }

    @Test
    void toDouble_NullSource_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> TimestampConversions.toDouble(null, DummyConverter.INSTANCE));
    }

    @Test
    void toDouble_NullConverter_ThrowsNullPointerException() {
        Instant instant = Instant.now();
        assertThrows(NullPointerException.class, () -> TimestampConversions.toDouble(instant, null));
    }

    @Test
    void toDouble_UnparseableString_ThrowsIllegalArgumentException() {
        String bad = "not-a-date";
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toDouble(bad, DummyConverter.INSTANCE));
    }

    @Test
    void toDouble_UnsupportedTemporal_LocalTime_ThrowsIllegalArgumentException() {
        LocalTime lt = LocalTime.of(12, 34, 56);
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toDouble(lt, DummyConverter.INSTANCE));
    }

    @Test
    void toDouble_UnsupportedTemporal_YearMonth_ThrowsIllegalArgumentException() {
        YearMonth ym = YearMonth.of(2020, 9);
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toDouble(ym, DummyConverter.INSTANCE));
    }

    @Test
    void toDouble_UnsupportedTemporal_MonthDay_ThrowsIllegalArgumentException() {
        MonthDay md = MonthDay.of(Month.JANUARY, 15);
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toDouble(md, DummyConverter.INSTANCE));
    }

    @Test
    void toDouble_UnsupportedCollection_List_ThrowsIllegalArgumentException() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toDouble(list, DummyConverter.INSTANCE));
    }

    @Test
    void toDouble_UnsupportedCollection_Map_ThrowsIllegalArgumentException() {
        Map<String, Object> emptyMap = Collections.emptyMap();
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toDouble(emptyMap, DummyConverter.INSTANCE));
    }

    @Test
    void toDouble_ConverterThrowsRuntimeException_PropagatesException() {
        String iso = "2020-01-01T00:00:00Z";
        assertThrows(RuntimeException.class, () -> TimestampConversions.toDouble(iso, ThrowingConverter.INSTANCE));
    }
}
