package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toDuration_Test {

    @Mock
    private Converter // Converter is not used by the method, but we provide a mock.
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Ensure the mock was not interacted with – the method should ignore the converter.
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Zero nanoseconds
        Arguments.of(BigInteger.ZERO, null, "PT0S"), // 0.5 seconds (500,000,000 nanoseconds)
        Arguments.of(BigInteger.valueOf(500_000_000L), null, "PT0.5S"), // Exact 3 seconds
        Arguments.of(BigInteger.valueOf(3_000_000_000L), null, "PT3S"), // 3 seconds + 250,000,123 nanoseconds
        Arguments.of(BigInteger.valueOf(3_250_000_123L), null, "PT3.250000123S"));
    }

    private static Stream<Arguments> negativeConversionProvider() {
        return Stream.of(// -0.5 seconds
        Arguments.of(BigInteger.valueOf(-500_000_000L), null, "-PT0.5S"), // -3.250000123 seconds
        Arguments.of(BigInteger.valueOf(-3_250_000_123L), null, "-PT3.250000123S"));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null 'from' argument
        Arguments.of(null, null, NullPointerException.class), // Wrong type: String
        Arguments.of("some string", null, ClassCastException.class), // Wrong type: Integer
        Arguments.of(Integer.valueOf(123), null, ClassCastException.class));
    }

    @Test
    void toDuration_zeroNanoseconds_returnsZeroDuration() {
        // given
        BigInteger nanoseconds = BigInteger.ZERO;
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, mockConverter);
        // then
        assertEquals(Duration.ZERO, result, "Duration for zero nanoseconds should be zero");
    }

    @Test
    void toDuration_subSecondNanoseconds_returnsCorrectDuration() {
        // given
        // 500,000,000 nanoseconds = 0.5 seconds
        BigInteger nanoseconds = BigInteger.valueOf(500_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, mockConverter);
        // then
        Duration expected = Duration.ofSeconds(0, 500_000_000);
        assertEquals(expected, result, "Duration should represent 0 seconds and 500,000,000 nanos");
    }

    @Test
    void toDuration_multipleSecondsNanoseconds_returnsCorrectDuration() {
        // given
        // 3,500,000,000 nanoseconds = 3 seconds + 500,000,000 nanos
        BigInteger nanoseconds = BigInteger.valueOf(3_500_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, mockConverter);
        // then
        Duration expected = Duration.ofSeconds(3, 500_000_000);
        assertEquals(expected, result, "Duration should represent 3 seconds and 500,000,000 nanos");
    }

    @Test
    void toDuration_largeNanoseconds_returnsCorrectDuration() {
        // given
        // 123,456,789,012,345,678 nanoseconds
        BigInteger nanoseconds = new BigInteger("123456789012345678");
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, mockConverter);
        // then
        // Compute expected seconds and nanos manually
        BigInteger[] divRem = nanoseconds.divideAndRemainder(BigIntegerConversions.BILLION);
        long expectedSeconds = divRem[0].longValue();
        int expectedNanos = divRem[1].intValue();
        Duration expected = Duration.ofSeconds(expectedSeconds, expectedNanos);
        assertEquals(expected, result, "Duration should match the large nanosecond value");
    }

    @Test
    void toDuration_negativeNanoseconds_returnsCorrectDuration() {
        // given
        // -2,500,000,000 nanoseconds = -3 seconds + 500,000,000 nanos (Duration normalises)
        BigInteger nanoseconds = BigInteger.valueOf(-2_500_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, mockConverter);
        // then
        // Using the same algorithm as the implementation to compute the expected value
        BigInteger[] divRem = nanoseconds.divideAndRemainder(BigIntegerConversions.BILLION);
        long seconds = divRem[0].longValue();
        int nanos = divRem[1].intValue();
        Duration expected = Duration.ofSeconds(seconds, nanos);
        assertEquals(expected, result, "Duration should correctly handle negative nanoseconds");
    }

    /* -----------------------------------------------------------
     *  Normal conversion test cases (positive and zero values)
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, expected={2}")
    @MethodSource("normalConversionProvider")
    void toDuration_normalConversions(BigInteger from, Converter converter, String expectedIso) {
        // when
        Duration result = BigIntegerConversions.toDuration(from, converter);
        // then
        Duration expected = Duration.parse(expectedIso);
        assertEquals(expected, result, () -> "Duration for input " + from + " should be " + expectedIso);
    }

    /* -----------------------------------------------------------
     *  Negative conversion test cases (negative nanoseconds)
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, expected={2}")
    @MethodSource("negativeConversionProvider")
    void toDuration_negativeConversions(BigInteger from, Converter converter, String expectedIso) {
        // when
        Duration result = BigIntegerConversions.toDuration(from, converter);
        // then
        Duration expected = Duration.parse(expectedIso);
        assertEquals(expected, result, () -> "Duration for negative input " + from + " should be " + expectedIso);
    }

    /* -----------------------------------------------------------
     *  Exception scenarios (invalid or null inputs)
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} expects {2}")
    @MethodSource("exceptionProvider")
    void toDuration_exceptionScenarios(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> BigIntegerConversions.toDuration(from, converter), () -> "Calling toDuration with from=" + from + " should throw " + expectedException.getSimpleName());
    }
}
