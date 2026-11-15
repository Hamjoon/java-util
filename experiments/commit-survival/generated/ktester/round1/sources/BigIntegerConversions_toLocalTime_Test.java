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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toLocalTime_Test {

    /**
     * The Converter is not used by the method under test, but we provide a mock to satisfy the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple stub implementation of {@code Converter} used for the “converter‑parameter effect” tests.
     */
    private static final class DummyConverter implements Converter {
        // No behaviour required – the conversion method does not consult the instance.
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// midnight (0 nanoseconds)
        Arguments.of(BigInteger.ZERO, null, "00:00"), // typical time 12:34:56.789 (12 h 34 m 56 s 789 ms)
        Arguments.of(BigInteger.valueOf(45_296_789_000_000L), null, "12:34:56.789"), // last nanosecond of the day
        Arguments.of(BigInteger.valueOf(86_399_999_999_999L), null, "23:59:59.999999999"));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// negative value
        Arguments.of(BigInteger.valueOf(-1L), IllegalArgumentException.class, "Negative nanosecond value should be rejected"), // value just above the maximum (86 400 000 000 000)
        Arguments.of(BigInteger.valueOf(86_400_000_000_000L), IllegalArgumentException.class, "Nanosecond value just above the maximum must cause IllegalArgumentException"), // null source object → NullPointerException when calling longValue()
        Arguments.of(null, NullPointerException.class, "Passing null as the source object should throw NullPointerException"), // non‑BigInteger type (String) → ClassCastException during cast
        Arguments.of("12345", ClassCastException.class, "Providing a non‑BigInteger type should cause ClassCastException"), // Long.MAX_VALUE nanoseconds exceeds a day
        Arguments.of(BigInteger.valueOf(Long.MAX_VALUE), IllegalArgumentException.class, "Using Long.MAX_VALUE as nanoseconds exceeds the day's range"));
    }

    private static Stream<Arguments> converterEffectProvider() {
        DummyConverter dummy = new DummyConverter();
        return Stream.of(// zero nanoseconds with a real converter instance
        Arguments.of(BigInteger.ZERO, dummy, "00:00"), // typical time with a real converter instance
        Arguments.of(BigInteger.valueOf(45_296_789_000_000L), dummy, "12:34:56.789"));
    }

    @Test
    void toLocalTime_zeroNanoseconds_returnsMidnight() {
        // given
        BigInteger input = BigInteger.ZERO;
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(input, mockConverter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    void toLocalTime_oneSecond_returnsCorrectTime() {
        // given
        // 1 second in nanoseconds
        BigInteger input = BigInteger.valueOf(1_000_000_000L);
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(input, mockConverter);
        // then
        assertEquals(LocalTime.of(0, 0, 1), result);
    }

    @Test
    void toLocalTime_maximumNanoseconds_returnsLastMomentOfDay() {
        // given
        // 23:59:59.999999999 = 86_399_999_999_999 nanoseconds
        BigInteger input = BigInteger.valueOf(86_399_999_999_999L);
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(input, mockConverter);
        // then
        assertEquals(LocalTime.MAX, result);
    }

    @Test
    void toLocalTime_negativeValue_throwsIllegalArgumentException() {
        // given
        BigInteger input = BigInteger.valueOf(-1L);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toLocalTime(input, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0"));
    }

    @Test
    void toLocalTime_valueExceedsMaximum_throwsIllegalArgumentException() {
        // given
        // One nanosecond beyond the allowed maximum
        BigInteger input = BigInteger.valueOf(86_399_999_999_999L).add(BigInteger.ONE);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toLocalTime(input, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399999999999"));
    }

    /* -------------------------------------------------------------
     *  Valid LocalTime conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} nanoseconds → {2}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validConversions(BigInteger input, Converter converter, String expectedString) {
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(input, converter);
        // then
        LocalTime expected = LocalTime.parse(expectedString);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     *  Invalid LocalTime conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidConversionProvider")
    void toLocalTime_invalidConversions(Object input, Class<? extends Throwable> expectedException, String description) {
        // when / then
        Throwable ex = assertThrows(expectedException, () -> BigIntegerConversions.toLocalTime(input, mockConverter));
        // Simple sanity check that the exception message mentions the range when appropriate
        if (expectedException == IllegalArgumentException.class) {
            assertTrue(ex.getMessage().contains("must be >= 0"));
        }
    }

    /* -------------------------------------------------------------
     *  Converter parameter effect (method ignores the converter)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] converter={1}, input={0}")
    @MethodSource("converterEffectProvider")
    void toLocalTime_converterParameterIgnored(Object input, Converter converter, String expectedString) {
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(input, converter);
        // then
        LocalTime expected = LocalTime.parse(expectedString);
        assertEquals(expected, result);
    }
}
