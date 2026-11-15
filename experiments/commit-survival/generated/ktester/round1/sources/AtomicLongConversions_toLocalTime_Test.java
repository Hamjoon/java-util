package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DateTimeException;

@ExtendWith(MockitoExtension.class)
class AtomicLongConversions_toLocalTime_Test {

    /**
     * A mocked {@link Converter} – the concrete implementation is not needed for this test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Verifies that an {@link AtomicLong} containing {@code 0L} is converted to {@link LocalTime#MIDNIGHT}
     * via the {@code toLocalTime} conversion path.
     */
    @Test
    void toLocalTime_convertsZeroAtomicLongToMidnight() {
        // given
        // 0 milliseconds since start of day
        AtomicLong atomicLong = new AtomicLong(0L);
        // when
        LocalTime result = AtomicLongConversions.toLocalTime(atomicLong, mockConverter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result, "AtomicLong value 0 should be converted to LocalTime.MIDNIGHT");
    }

    /**
     * Verifies that a non‑zero {@link AtomicLong} is correctly converted.
     * The value 12_345_678 milliseconds corresponds to 03:25:45.678.
     */
    @Test
    void toLocalTime_convertsNonZeroAtomicLong() {
        // given
        long millis = 12_345_678L;
        AtomicLong atomicLong = new AtomicLong(millis);
        LocalTime expected = LocalTime.ofNanoOfDay(millis * 1_000_000L);
        // when
        LocalTime result = AtomicLongConversions.toLocalTime(atomicLong, mockConverter);
        // then
        assertEquals(expected, result, "AtomicLong value " + millis + " should be converted to the correct LocalTime");
    }

    /* -------------------------------------------------------------
     *  Valid conversion – zero value
     * ------------------------------------------------------------- */
    @Test
    void validConversion_zeroValue() {
        // given
        // 0 nanoseconds
        AtomicLong atomicLong = new AtomicLong(0L);
        // when
        LocalTime result = AtomicLongConversions.toLocalTime(atomicLong, mockConverter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result, "AtomicLong value 0 should be converted to LocalTime.MIDNIGHT");
    }

    /* -------------------------------------------------------------
     *  Valid conversion – typical value (12 hours)
     * ------------------------------------------------------------- */
    @Test
    void validConversion_typicalValue() {
        // given: 12 hours expressed in nanoseconds
        long nanos = 12L * 60L * 60L * 1_000_000_000L;
        AtomicLong atomicLong = new AtomicLong(nanos);
        // 12:00:00
        LocalTime expected = LocalTime.of(12, 0);
        // when
        LocalTime result = AtomicLongConversions.toLocalTime(atomicLong, mockConverter);
        // then
        assertEquals(expected, result, "AtomicLong representing 12 hours should be converted to LocalTime.of(12,0)");
    }

    /* -------------------------------------------------------------
     *  Valid conversion – maximum nanoseconds of a day
     * ------------------------------------------------------------- */
    @Test
    void validConversion_maxNanosecondsOfDay() {
        // given: 23:59:59.999999999 expressed in nanoseconds
        long maxNanos = // hours
        23L * 60L * 60L * 1_000_000_000L + // minutes
        59L * 60L * 1_000_000_000L + // seconds
        59L * 1_000_000_000L + // nanoseconds
        999_999_999L;
        AtomicLong atomicLong = new AtomicLong(maxNanos);
        LocalTime expected = LocalTime.of(23, 59, 59, 999_999_999);
        // when
        LocalTime result = AtomicLongConversions.toLocalTime(atomicLong, mockConverter);
        // then
        assertEquals(expected, result, "Maximum nanosecond value should be converted to 23:59:59.999999999");
    }

    /* -------------------------------------------------------------
     *  Invalid input – null 'from' argument
     * ------------------------------------------------------------- */
    @Test
    void invalidInput_nullFrom() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicLongConversions.toLocalTime(from, mockConverter), "Passing null as 'from' should result in NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Invalid input – wrong type for 'from'
     * ------------------------------------------------------------- */
    @Test
    void invalidInput_wrongTypeFrom() {
        // given: a String instead of an AtomicLong
        Object from = "not an atomic long";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicLongConversions.toLocalTime(from, mockConverter), "Passing a non‑AtomicLong object should cause ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Invalid input – null 'converter' argument
     * ------------------------------------------------------------- */
    @Test
    void invalidInput_nullConverter() {
        // given
        AtomicLong atomicLong = new AtomicLong(123L);
        Converter nullConverter = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicLongConversions.toLocalTime(atomicLong, nullConverter), "A null converter should propagate a NullPointerException from LongConversions");
    }

    /* -------------------------------------------------------------
     *  Invalid input – nanoseconds exceeding a day
     * ------------------------------------------------------------- */
    @Test
    void invalidInput_nanosecondsExceedingDay() {
        // given: exactly 24 hours in nanoseconds (out of LocalTime range)
        long nanos = 24L * 60L * 60L * 1_000_000_000L;
        AtomicLong atomicLong = new AtomicLong(nanos);
        // when / then
        assertThrows(DateTimeException.class, () -> AtomicLongConversions.toLocalTime(atomicLong, mockConverter), "Value equal to 24 hours should cause DateTimeException");
    }

    /* -------------------------------------------------------------
     *  Invalid input – negative nanoseconds
     * ------------------------------------------------------------- */
    @Test
    void invalidInput_negativeNanoseconds() {
        // given
        AtomicLong atomicLong = new AtomicLong(-1L);
        // when / then
        assertThrows(DateTimeException.class, () -> AtomicLongConversions.toLocalTime(atomicLong, mockConverter), "Negative nanosecond values should cause DateTimeException");
    }

    /* -------------------------------------------------------------
     *  Invalid input – converter throws exception
     * ------------------------------------------------------------- */
    @Test
    void invalidInput_converterThrowsException() {
        // given
        // 1 second in nanoseconds
        AtomicLong atomicLong = new AtomicLong(1_000_000_000L);
        Converter throwingConverter = new Converter() {

            @Override
            @SuppressWarnings("unchecked")
            public <T> T convert(Object src, Class<T> dest) {
                throw new IllegalArgumentException("converter fail");
            }
        };
        // when / then
        assertThrows(IllegalArgumentException.class, () -> AtomicLongConversions.toLocalTime(atomicLong, throwingConverter), "Exception thrown by Converter should propagate unchanged");
    }
}
