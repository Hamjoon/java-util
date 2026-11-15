package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toCalendar_Test {

    @Mock
    Converter mockConverter;

    @Mock
    ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Default stub: UTC timezone – individual tests can override if needed
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // Default behaviour: UTC timezone
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
    }

    @Test
    void toCalendar_zeroNanoseconds_usesUtcTimezone() {
        // GIVEN
        // 0 nanoseconds → 0 milliseconds
        BigInteger epochNanos = BigInteger.ZERO;
        // WHEN
        Calendar result = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(0L, result.getTimeInMillis(), "Calendar time should be epoch (0 ms) for zero nanoseconds");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar should use the UTC timezone supplied by ConverterOptions");
    }

    @Test
    void toCalendar_nonMultipleOfMillion_truncatesToMillis() {
        // GIVEN
        // 1,500,123,456 nanoseconds → 1,500 milliseconds (integer division truncates)
        BigInteger epochNanos = new BigInteger("1500123456");
        // WHEN
        Calendar result = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertEquals(1500L, result.getTimeInMillis(), "Nanoseconds should be divided by 1_000_000 and truncated to whole milliseconds");
    }

    @Test
    void toCalendar_usesProvidedTimeZone() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // epoch start
        BigInteger epochNanos = BigInteger.valueOf(0);
        // WHEN
        Calendar result = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertEquals(tz, result.getTimeZone(), "Calendar must reflect the timezone returned by ConverterOptions");
        assertEquals(0L, result.getTimeInMillis(), "Time in milliseconds should still correspond to the supplied epoch value");
    }

    /* -------------------------------------------------------------
     *  Normal conversion – positive epoch nanoseconds
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_positiveNanoseconds_defaultUtc() {
        // GIVEN: 1_600_000_000_000_000_000 ns  → 1_600_000_000_000 ms
        BigInteger epochNanos = BigInteger.valueOf(1_600_000_000_000_000_000L);
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertNotNull(cal);
        assertEquals(1_600_000_000_000L, cal.getTimeInMillis(), "Milliseconds should be nanoseconds / 1_000_000");
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Default timezone must be UTC");
    }

    @Test
    void toCalendar_positiveNanoseconds_customPst() {
        // GIVEN
        TimeZone pst = TimeZone.getTimeZone("America/Los_Angeles");
        when(mockOptions.getTimeZone()).thenReturn(pst);
        // arbitrary large value
        BigInteger epochNanos = new BigInteger("1234567890123456789");
        // Expected milliseconds = floor(nanos / 1_000_000)
        long expectedMillis = epochNanos.divide(BigInteger.valueOf(1_000_000)).longValue();
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertEquals(pst, cal.getTimeZone(), "Timezone must be the one supplied by ConverterOptions");
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Milliseconds should be correctly truncated from nanoseconds");
    }

    /* -------------------------------------------------------------
     *  Zero epoch nanoseconds
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_zeroNanoseconds() {
        // GIVEN
        BigInteger epochNanos = BigInteger.ZERO;
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertEquals(0L, cal.getTimeInMillis(), "Zero nanoseconds must result in epoch start (0 ms)");
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Timezone should be UTC by default");
    }

    /* -------------------------------------------------------------
     *  Negative epoch nanoseconds
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_negativeNanoseconds() {
        // GIVEN: -1_500_000_000 ns = -1.5 seconds → -1500 ms
        BigInteger epochNanos = BigInteger.valueOf(-1_500_000_000L);
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertEquals(-1500L, cal.getTimeInMillis(), "Negative nanoseconds must be divided and keep sign");
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Timezone should remain UTC");
    }

    /* -------------------------------------------------------------
     *  Nanoseconds not multiple of one million (truncation)
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_truncatesSubMillisecondPortion() {
        // GIVEN: 1_500_000 ns = 1.5 ms → truncates to 1 ms
        BigInteger epochNanos = BigInteger.valueOf(1_500_000L);
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertEquals(1L, cal.getTimeInMillis(), "Division by 1_000_000 must truncate toward zero");
    }

    /* -------------------------------------------------------------
     *  Very large epoch nanoseconds causing long overflow
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_overflowLongWrapsToMinValue() {
        // GIVEN: ((Long.MAX_VALUE + 1) milliseconds) expressed in nanoseconds
        BigInteger oneMillion = BigInteger.valueOf(1_000_000L);
        BigInteger overflowMillis = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        // now in nanoseconds
        BigInteger epochNanos = overflowMillis.multiply(oneMillion);
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertEquals(Long.MIN_VALUE, cal.getTimeInMillis(), "longValue() overflows to Long.MIN_VALUE when exceeding Long.MAX_VALUE");
    }

    /* -------------------------------------------------------------
     *  Custom time‑zone offset handling
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_customTimeZoneOffset() {
        // GIVEN: 2021‑05‑05T00:00:00Z → 1_620_000_000_000_000_000 ns
        BigInteger epochNanos = BigInteger.valueOf(1_620_000_000_000_000_000L);
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertEquals(tz, cal.getTimeZone(), "Calendar must use the timezone supplied by the converter");
        assertEquals(1_620_000_000_000L, cal.getTimeInMillis(), "Milliseconds should correspond to the supplied nanoseconds");
    }

    /* -------------------------------------------------------------
     *  Null converter throws NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_nullConverter_throwsNPE() {
        BigInteger epochNanos = BigInteger.ONE;
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toCalendar(epochNanos, null), "Passing a null converter must result in NPE");
    }

    /* -------------------------------------------------------------
     *  Null from argument throws NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_nullFrom_throwsNPE() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toCalendar(null, mockConverter), "Passing a null 'from' argument must result in NPE");
    }

    /* -------------------------------------------------------------
     *  Invalid type for from argument throws ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_invalidFromType_throwsClassCastException() {
        Long invalid = Long.valueOf(1_600_000_000_000L);
        assertThrows(ClassCastException.class, () -> BigIntegerConversions.toCalendar(invalid, mockConverter), "Providing a non‑BigInteger should cause a ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Converter with null options throws NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_converterWithNullOptions_throwsNPE() {
        // GIVEN: converter returns null options
        when(mockConverter.getOptions()).thenReturn(null);
        BigInteger epochNanos = BigInteger.ONE;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toCalendar(epochNanos, mockConverter), "If Converter.getOptions() returns null, a NPE must be thrown");
    }
}
