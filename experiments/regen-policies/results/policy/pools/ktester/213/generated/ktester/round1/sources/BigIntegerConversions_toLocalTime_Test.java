package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toLocalTime_Test {

    /**
     * The {@link Converter} is not used by {@code BigIntegerConversions.toLocalTime},
     * but the method signature requires it. A mock is sufficient.
     */
    @Mock
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock instance automatically.
        assertNotNull(converter, "Converter mock should be initialised by Mockito");
    }

    static Stream<Arguments> validConversions() {
        return Stream.of(// smallest valid value – midnight
        Arguments.of("Convert the smallest valid nanosecond value (0) to LocalTime", BigInteger.ZERO, null, LocalTime.MIDNIGHT), // maximum valid nanosecond‑of‑day
        Arguments.of("Convert the maximum nanosecond‑of‑day value (86 399 999 999 999) to LocalTime", BigInteger.valueOf(86_399_999_999_999L), null, LocalTime.of(23, 59, 59, 999_999_999)), // typical mid‑range value
        Arguments.of("Convert a typical mid‑range nanosecond value to LocalTime", BigInteger.valueOf(12_345_678L), null, LocalTime.ofNanoOfDay(12_345_678L)), // zero nanoseconds with a real Converter instance (should be ignored)
        Arguments.of("Converter supplied but not used – zero nanoseconds", BigInteger.valueOf(0L), new Converter() {
        }, LocalTime.MIDNIGHT), // maximum valid value expressed as a String‑based BigInteger
        Arguments.of("Maximum valid value created via String constructor", new BigInteger("86399999999999"), null, LocalTime.of(23, 59, 59, 999_999_999)), // anonymous subclass of BigInteger with value 0
        Arguments.of("Anonymous subclass of BigInteger with value 0", new BigInteger("0") {
        }, null, LocalTime.MIDNIGHT));
    }

    static Stream<Arguments> outOfRangeConversions() {
        return Stream.of(// negative value
        Arguments.of("Negative nanosecond value", BigInteger.valueOf(-1L), null, IllegalArgumentException.class), // one nanosecond too many
        Arguments.of("Nanosecond value exceeds the maximum of a day", BigInteger.valueOf(86_400_000_000_000L), null, IllegalArgumentException.class), // value far beyond the day range (also exceeds Long.MAX_VALUE)
        Arguments.of("Very large BigInteger far beyond day range", new BigInteger("12345678901234567890"), null, IllegalArgumentException.class), // Long.MAX_VALUE (9223372036854775807) – exceeds day range but fits in long
        Arguments.of("BigInteger equal to Long.MAX_VALUE but out of day range", new BigInteger("9223372036854775807"), null, IllegalArgumentException.class));
    }

    static Stream<Arguments> wrongTypeConversions() {
        return Stream.of(// not a BigInteger – ClassCastException occurs before try‑catch
        Arguments.of("Input is a Long, not a BigInteger", Long.valueOf(0L), null, ClassCastException.class), // null 'from' argument – NullPointerException during cast
        Arguments.of("Null 'from' argument", null, null, NullPointerException.class));
    }

    /**
     * Verify conversion of the minimum allowed value (0 nanoseconds) to {@code 00:00}.
     */
    @Test
    void toLocalTime_zeroNanoseconds_returnsMidnight() {
        // given
        BigInteger from = BigInteger.ZERO;
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(from, converter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    /**
     * Verify conversion of a typical value (1 000 000 000 nanoseconds = 1 second).
     */
    @Test
    void toLocalTime_oneSecondNanoseconds_returnsCorrectTime() {
        // given
        // 1 second
        BigInteger from = BigInteger.valueOf(1_000_000_000L);
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(from, converter);
        // then
        assertEquals(LocalTime.of(0, 0, 1), result);
    }

    /**
     * Verify conversion of the maximum allowed nanoseconds of a day.
     * 86 399 999 999 999 nanoseconds = 23:59:59.999999999
     */
    @Test
    void toLocalTime_maxNanoseconds_returnsLastMomentOfDay() {
        // given
        BigInteger from = BigInteger.valueOf(86_399_999_999_999L);
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(from, converter);
        // then
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), result);
    }

    /**
     * Negative values are illegal and must cause an {@link IllegalArgumentException}.
     */
    @Test
    void toLocalTime_negativeValue_throwsIllegalArgumentException() {
        // given
        BigInteger from = BigInteger.valueOf(-1L);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toLocalTime(from, converter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399999999999"));
    }

    /**
     * Values larger than the maximum nanoseconds in a day are illegal.
     */
    @Test
    void toLocalTime_tooLargeValue_throwsIllegalArgumentException() {
        // given
        // one nanosecond too many
        BigInteger from = BigInteger.valueOf(86_400_000_000_000L);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toLocalTime(from, converter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399999999999"));
    }

    /* --------------------------------------------------------------
     *  Group: Valid conversions
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversions")
    void toLocalTime_validConversions(String description, Object from, Converter conv, LocalTime expected) {
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(from, conv);
        // then
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Group: Invalid conversions – out of range
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("outOfRangeConversions")
    void toLocalTime_outOfRangeConversions(String description, Object from, Converter conv, Class<? extends Exception> expectedException) {
        // when / then
        Exception ex = assertThrows(expectedException, () -> BigIntegerConversions.toLocalTime(from, conv));
        // verify that the message mentions the allowed range
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399999999999"), "Exception message should contain range information");
    }

    /* --------------------------------------------------------------
     *  Group: Invalid conversions – wrong type
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("wrongTypeConversions")
    void toLocalTime_wrongTypeConversions(String description, Object from, Converter conv, Class<? extends Exception> expectedException) {
        // when / then
        assertThrows(expectedException, () -> BigIntegerConversions.toLocalTime(from, conv));
    }

    /* --------------------------------------------------------------
     *  Group: Invalid conversions – longValue throws
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("BigInteger subclass whose longValue() throws RuntimeException")
    void toLocalTime_longValueThrowsIllegalArgumentException() {
        // given: anonymous subclass that forces longValue() to fail
        BigInteger bad = new BigInteger("123") {

            @Override
            public long longValue() {
                throw new RuntimeException("forced longValue failure");
            }
        };
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toLocalTime(bad, null));
        // The cause should be the RuntimeException we threw
        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals("forced longValue failure", ex.getCause().getMessage());
    }

    /* --------------------------------------------------------------
     *  Group: Invalid conversions – null Converter handling
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Null Converter does not affect successful conversion")
    void toLocalTime_nullConverterSucceeds() {
        // given
        BigInteger from = BigInteger.valueOf(123_456L);
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(from, null);
        // then
        assertEquals(LocalTime.ofNanoOfDay(123_456L), result);
    }
}
