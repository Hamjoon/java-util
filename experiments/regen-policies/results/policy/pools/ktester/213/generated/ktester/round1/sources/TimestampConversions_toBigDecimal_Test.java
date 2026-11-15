package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toBigDecimal_Test {

    private static Converter converter;

    /**
     * A real converter used for the “happy‑path” tests.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs options; DefaultConverterOptions provides the standard configuration.
        converter = new Converter(new DefaultConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No resources to release, but method kept for symmetry with the template.
        converter = null;
    }

    /**
     * Helper that builds a {@link Timestamp} from a date string and time components.
     */
    private Timestamp createTimestamp(String dateStr, int hour, int minute, int second, int nanos) {
        return Timestamp.from(LocalDate.parse(dateStr).atTime(hour, minute, second, nanos).atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Computes the expected {@link BigDecimal} representation of a {@link Timestamp}.
     */
    private BigDecimal expectedBigDecimal(Timestamp ts) {
        Instant instant = ts.toInstant();
        // epoch seconds + nanos as a fraction of a second (scale = 9)
        return BigDecimal.valueOf(instant.getEpochSecond()).add(BigDecimal.valueOf(instant.getNano(), 9));
    }

    private static Stream<Arguments> validConversionProvider() {
        Timestamp ts1 = Timestamp.valueOf("2023-01-01 12:34:56.789");
        Timestamp ts2 = Timestamp.valueOf("1970-01-01 00:00:00.000");
        return Stream.of(Arguments.of(ts1, // 2023‑01‑01 12:34:56.789 → epoch seconds + 0.789
        new BigDecimal("1672578896.789")), Arguments.of(ts2, BigDecimal.ZERO));
    }

    private static Stream<Arguments> boundaryProvider() {
        Timestamp justBeforeEpoch = Timestamp.valueOf("1969-12-31 23:59:59.999");
        Timestamp max2038 = Timestamp.valueOf("2038-01-19 03:14:07.999");
        // Instant.MAX is out of the range of java.sql.Timestamp, but the test case expects it to work.
        // We create it via Timestamp.from(Instant.MAX) and catch any unexpected exception.
        Timestamp instantMaxTs = Timestamp.from(Instant.MAX);
        return Stream.of(Arguments.of(justBeforeEpoch, new BigDecimal("-0.001")), Arguments.of(max2038, new BigDecimal("2147483647.999")), Arguments.of(instantMaxTs, // Instant.MAX epoch seconds + 0.999999999
        new BigDecimal(Instant.MAX.getEpochSecond()).add(BigDecimal.valueOf(Instant.MAX.getNano(), 9))));
    }

    private static Stream<Arguments> nanoPrecisionProvider() {
        Timestamp zeroNanos = Timestamp.valueOf("2023-01-01 12:34:56.000");
        Timestamp fullNanos = Timestamp.valueOf("2023-01-01 12:34:56.123456789");
        return Stream.of(Arguments.of(zeroNanos, new BigDecimal("1672578896")), Arguments.of(fullNanos, new BigDecimal("1672578896.123456789")));
    }

    @Test
    void testTimestampToBigDecimal() {
        Timestamp ts1 = createTimestamp("1888-01-02", 12, 30, 45, 123_456_789);
        Timestamp ts2 = createTimestamp("1969-12-31", 23, 59, 59, 999_999_999);
        Timestamp ts3 = createTimestamp("1970-01-01", 0, 0, 1, 1);
        Timestamp ts4 = createTimestamp("2023-06-15", 15, 30, 0, 500_000_000);
        assertEquals(expectedBigDecimal(ts1), TimestampConversions.toBigDecimal(ts1, converter), "Conversion of ts1 failed");
        assertEquals(expectedBigDecimal(ts2), TimestampConversions.toBigDecimal(ts2, converter), "Conversion of ts2 failed");
        assertEquals(expectedBigDecimal(ts3), TimestampConversions.toBigDecimal(ts3, converter), "Conversion of ts3 failed");
        assertEquals(expectedBigDecimal(ts4), TimestampConversions.toBigDecimal(ts4, converter), "Conversion of ts4 failed");
    }

    /* -------------------------------------------------------------
     *  1. Valid conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => timestamp={0}")
    @MethodSource("validConversionProvider")
    void testValidConversions(Timestamp ts, BigDecimal expected) {
        assertEquals(expected, TimestampConversions.toBigDecimal(ts, realConverter), "Conversion result does not match expected value");
    }

    /* -------------------------------------------------------------
     *  2. Null and invalid inputs
     * ------------------------------------------------------------- */
    @Test
    void testNullFromThrowsNPE() {
        assertThrows(NullPointerException.class, () -> TimestampConversions.toBigDecimal(null, realConverter), "Null source object should cause NullPointerException");
    }

    @Test
    void testWrongTypeThrowsClassCastException() {
        Date notATimestamp = Date.from(Instant.now());
        assertThrows(ClassCastException.class, () -> TimestampConversions.toBigDecimal(notATimestamp, realConverter), "Passing a non‑Timestamp should cause ClassCastException");
    }

    @Test
    void testNullConverterThrowsNPE() {
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56.789");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toBigDecimal(ts, null), "Null converter should cause NullPointerException when forwarded to InstantConversions");
    }

    /* -------------------------------------------------------------
     *  3. Converter overrides result
     * ------------------------------------------------------------- */
    @Test
    void testConverterOverridesResult() {
        // A converter that always returns the fixed BigDecimal, regardless of input.
        Converter fixedConverter = mock(Converter.class);
        BigDecimal fixed = new BigDecimal("12345.6789");
        when(fixedConverter.convert(any(), eq(BigDecimal.class))).thenReturn(fixed);
        Timestamp ts = Timestamp.valueOf("2023-01-01 00:00:00.000");
        BigDecimal result = TimestampConversions.toBigDecimal(ts, fixedConverter);
        assertEquals(fixed, result, "Result should be the value supplied by the custom converter");
    }

    /* -------------------------------------------------------------
     *  4. Boundary timestamps
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => timestamp={0}, expected={1}")
    @MethodSource("boundaryProvider")
    void testBoundaryTimestamps(Timestamp ts, BigDecimal expected) {
        assertEquals(expected, TimestampConversions.toBigDecimal(ts, realConverter));
    }

    /* -------------------------------------------------------------
     *  5. Nanosecond precision handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => timestamp={0}, expected={1}")
    @MethodSource("nanoPrecisionProvider")
    void testNanosecondPrecision(Timestamp ts, BigDecimal expected) {
        assertEquals(expected, TimestampConversions.toBigDecimal(ts, realConverter));
    }

    /* -------------------------------------------------------------
     *  6. Timestamp out of Instant range
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => timestampMillis={0}")
    @ValueSource(longs = { Long.MAX_VALUE, Long.MIN_VALUE })
    void testTimestampOutOfInstantRange(long millis) {
        Timestamp ts = new Timestamp(millis);
        assertThrows(DateTimeException.class, () -> TimestampConversions.toBigDecimal(ts, realConverter), "Timestamp with millis " + millis + " should cause DateTimeException");
    }

    /* -------------------------------------------------------------
     *  7. Converter exception propagation
     * ------------------------------------------------------------- */
    @Test
    void testConverterExceptionPropagation() {
        // A converter that throws an IllegalArgumentException when asked to convert.
        Converter throwingConverter = mock(Converter.class);
        when(throwingConverter.convert(any(), eq(BigDecimal.class))).thenThrow(new IllegalArgumentException("converter failure"));
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56.789");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toBigDecimal(ts, throwingConverter));
        assertEquals("converter failure", ex.getMessage(), "Exception message should be propagated unchanged");
    }
}
