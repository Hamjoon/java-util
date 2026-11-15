package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.time.LocalTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toLocalTime_Test {

    /**
     * Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter has a public constructor that needs a ConverterOptions instance.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("Convert the minimum allowed nanosecond value (0) to LocalTime", BigInteger.ZERO, LocalTime.MIDNIGHT), Arguments.of("Convert 1,000,000,000 nanoseconds (exactly one second) to LocalTime", BigInteger.valueOf(1_000_000_000L), LocalTime.of(0, 0, 1)), Arguments.of("Convert the maximum valid nanosecond value of a day to LocalTime", BigInteger.valueOf(86_399_999_999_999L), LocalTime.of(23, 59, 59, 999_999_999)));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Negative value
        Arguments.of("Negative nanosecond value", BigInteger.valueOf(-1L)), // One tick beyond the maximum of a day
        Arguments.of("Nanosecond count one tick beyond the maximum of a day", BigInteger.valueOf(86_400_000_000_000L)), // Far larger than a day (Long.MAX_VALUE)
        Arguments.of("Value far larger than the maximum nanoseconds in a day", BigInteger.valueOf(Long.MAX_VALUE)), // Extremely large BigInteger that overflows a long (shift left 70 bits)
        Arguments.of("Extremely large BigInteger that overflows the long range", BigInteger.ONE.shiftLeft(70)));
    }

    /**
     * Happy‑path test: convert zero nanoseconds to {@link LocalTime#MIDNIGHT}.
     */
    @Test
    void toLocalTime_zeroNanos_returnsMidnight() {
        // given
        BigInteger from = BigInteger.ZERO;
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(from, converter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    /**
     * Happy‑path test: convert a typical nanosecond value (1 second).
     */
    @Test
    void toLocalTime_oneSecond_returnsCorrectTime() {
        // given
        // 1 second = 1_000_000_000 ns
        BigInteger from = BigInteger.valueOf(1_000_000_000L);
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(from, converter);
        // then
        assertEquals(LocalTime.ofNanoOfDay(1_000_000_000L), result);
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(1, result.getSecond());
        // ensure nanosecond part is correct
        assertEquals(0, result.getNano() % 1_000_000_000);
    }

    /**
     * Edge‑case test: maximum nanoseconds of a day should map to {@link LocalTime#MAX}.
     */
    @Test
    void toLocalTime_maxNanos_returnsMaxLocalTime() {
        // given
        // 23:59:59.999999999
        BigInteger from = BigInteger.valueOf(86_399_999_999_999L);
        // when
        LocalTime result = BigIntegerConversions.toLocalTime(from, converter);
        // then
        assertEquals(LocalTime.MAX, result);
    }

    /**
     * Negative input must cause an IllegalArgumentException.
     */
    @Test
    void toLocalTime_negativeValue_throwsException() {
        // given
        BigInteger from = BigInteger.valueOf(-1L);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toLocalTime(from, converter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399999999999"));
    }

    /**
     * Value greater than the maximum nanoseconds of a day must cause an IllegalArgumentException.
     */
    @Test
    void toLocalTime_valueExceedingMax_throwsException() {
        // given
        // exactly one day
        BigInteger from = BigInteger.valueOf(86_400_000_000_000L);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toLocalTime(from, converter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399999999999"));
    }

    /**
     * Valid conversions supplied via a parameterized test.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validConversions(String description, BigInteger from, LocalTime expected) {
        // The Converter is not used by the implementation, but we pass the instance created in @BeforeEach.
        LocalTime actual = BigIntegerConversions.toLocalTime(from, converter);
        assertEquals(expected, actual, description);
    }

    /**
     * Invalid conversions that must throw {@link IllegalArgumentException}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void toLocalTime_invalidConversions(String description, BigInteger from) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toLocalTime(from, converter), description);
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399999999999"));
    }

    /**
     * Null handling – passing {@code null} as the {@code from} argument should raise {@link NullPointerException}.
     */
    @Test
    void toLocalTime_nullFrom_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> BigIntegerConversions.toLocalTime(null, converter));
        // The message is not defined by the implementation; we only assert the type.
    }

    /**
     * Passing a non‑{@code BigInteger} object should cause a {@link ClassCastException}.
     */
    @Test
    void toLocalTime_nonBigIntegerFrom_throwsClassCastException() {
        Integer notABigInt = Integer.valueOf(123);
        assertThrows(ClassCastException.class, () -> BigIntegerConversions.toLocalTime(notABigInt, converter));
    }

    /**
     * Verify that providing a non‑null {@link Converter} does not affect the result.
     */
    @Test
    void toLocalTime_withNonNullConverter_ignoresConverter() {
        // 1 second
        BigInteger from = BigInteger.valueOf(1_000_000_000L);
        // Use a freshly created Converter (different from the one in @BeforeEach) to prove it is ignored.
        Converter anotherConverter = new Converter(new ConverterOptions());
        LocalTime result = BigIntegerConversions.toLocalTime(from, anotherConverter);
        assertEquals(LocalTime.of(0, 0, 1), result);
    }
}
