package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toDuration_Test {

    /**
     * Mocked Converter – not used by the method under test but required for the signature.
     */
    @Mock
    Converter mockConverter;

    /**
     * Helper that invokes the private static method via its package‑private visibility.
     */
    private static Duration callToDuration(Object from, Converter converter) {
        return DoubleConversions.toDuration(from, converter);
    }

    static Stream<Arguments> wholeSecondsProvider() {
        return Stream.of(arguments(10.0, "PT10S", "Positive whole number should produce a Duration of exactly 10 seconds."), arguments(-8.0, "-PT8S", "Negative whole number should produce a Duration of -8 seconds."));
    }

    static Stream<Arguments> fractionalSecondsProvider() {
        return Stream.of(arguments(5.75, "PT5.75S", "Positive fractional value should be split into 5 seconds and 750,000,000 nanoseconds."), arguments(-3.2, "-PT3.2S", "Negative fractional value should be split into -3 seconds and -200,000,000 nanoseconds."));
    }

    static Stream<Arguments> zeroAndSpecialProvider() {
        return Stream.of(arguments(0.0, "PT0S", "Zero should produce Duration.ZERO."), arguments(Double.NaN, "PT0S", "NaN is cast to 0 when converting to long, resulting in Duration.ZERO."));
    }

    static Stream<Arguments> overflowInfinityProvider() {
        return Stream.of(arguments(Double.POSITIVE_INFINITY, ArithmeticException.class, "Positive infinity casts to Long.MAX_VALUE for seconds and nanoseconds; Duration.ofSeconds overflows and throws an ArithmeticException."), arguments(Double.NEGATIVE_INFINITY, ArithmeticException.class, "Negative infinity casts to Long.MIN_VALUE; Duration.ofSeconds overflows and throws an ArithmeticException."));
    }

    static Stream<Arguments> largeFiniteWithinRangeProvider() {
        return Stream.of(arguments(9223372036854775807.0, "PT9223372036854775807S", "Maximum long value as double should produce a Duration with exactly Long.MAX_VALUE seconds and zero nanoseconds."), arguments(-9223372036854775808.0, "-PT9223372036854775808S", "Minimum long value as double should produce a Duration with exactly Long.MIN_VALUE seconds and zero nanoseconds."));
    }

    static Stream<Arguments> largeFiniteNanosecondOverflowProvider() {
        return Stream.of(arguments(9223372036854775807.5, ArithmeticException.class, "Double slightly larger than Long.MAX_VALUE seconds results in seconds = Long.MAX_VALUE and a positive nanosecond adjustment, causing Duration.ofSeconds to overflow."), arguments(-9223372036854775808.5, ArithmeticException.class, "Double slightly smaller than Long.MIN_VALUE seconds results in seconds = Long.MIN_VALUE and a negative nanosecond adjustment, causing Duration.ofSeconds to overflow."));
    }

    static Stream<Arguments> subSecondPrecisionProvider() {
        return Stream.of(arguments(1.000000001, "PT1.000000001S", "A value with one nanosecond fraction should be represented as 1 second and 1 nanosecond."), arguments(-0.000000001, "-PT0.000000001S", "Negative one‑nanosecond value should produce a Duration of -1 nanosecond."), arguments(-0.0, "PT0S", "Negative zero should be treated as zero, yielding Duration.ZERO."), arguments(0.0000000004, "PT0S", "A fraction smaller than one nanosecond rounds down to zero nanoseconds, resulting in Duration.ZERO."));
    }

    static Stream<Arguments> nullAndTypeMismatchProvider() {
        return Stream.of(arguments(null, NullPointerException.class, "Passing a null 'from' argument leads to a NullPointerException when casting to Double."), arguments(Integer.valueOf(42), ClassCastException.class, "Providing a non‑Double object (e.g., Integer) causes a ClassCastException during the cast."));
    }

    static Stream<Arguments> finiteHugeValuesOverflowProvider() {
        return Stream.of(arguments(Double.MAX_VALUE, ArithmeticException.class, "Maximum finite double value casts to Long.MAX_VALUE seconds and a huge nanosecond adjustment, causing Duration.ofSeconds to overflow."), arguments(-Double.MAX_VALUE, ArithmeticException.class, "Minimum finite double value (negative) casts to Long.MIN_VALUE seconds with a huge negative nanosecond adjustment, causing overflow."));
    }

    @Test
    void convertsWholeSecondsOnly() {
        // exactly 123 seconds
        Double source = 123.0;
        Duration expected = Duration.ofSeconds(123, 0);
        Duration actual = callToDuration(source, mockConverter);
        assertEquals(expected, actual, "Duration should represent whole seconds with zero nanos");
    }

    @Test
    void convertsPositiveFractionalSeconds() {
        // 45 seconds + 0.6789 sec
        Double source = 45.6789;
        long seconds = 45L;
        // 678_900_000 ns
        long nanos = (long) ((source - seconds) * 1_000_000_000L);
        Duration expected = Duration.ofSeconds(seconds, nanos);
        Duration actual = callToDuration(source, mockConverter);
        assertEquals(expected, actual, "Duration should correctly handle positive fractional part");
    }

    @Test
    void convertsNegativeFractionalSeconds() {
        // -12 seconds - 0.345 sec
        Double source = -12.345;
        long seconds = -12L;
        // The implementation casts the double to long first, truncating toward zero,
        // then computes the fractional part based on the original double.
        long nanos = (long) ((source - seconds) * 1_000_000_000L);
        // For -12.345 the calculation yields: (-12.345 - (-12)) = -0.345 → nanos = -345_000_000
        Duration expected = Duration.ofSeconds(seconds, nanos);
        Duration actual = callToDuration(source, mockConverter);
        assertEquals(expected, actual, "Duration should correctly handle negative fractional part");
    }

    @Test
    void convertsZero() {
        Double source = 0.0;
        Duration expected = Duration.ZERO;
        Duration actual = callToDuration(source, mockConverter);
        assertEquals(expected, actual, "Zero should map to Duration.ZERO");
    }

    @Test
    void throwsClassCastExceptionWhenSourceIsNotDouble() {
        Object notADouble = "not a double";
        assertThrows(ClassCastException.class, () -> callToDuration(notADouble, mockConverter), "A non‑Double source should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("wholeSecondsProvider")
    void wholeSeconds(Double from, String expectedIso, String description) {
        Duration expected = Duration.parse(expectedIso);
        Duration actual = callToDuration(from, null);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("fractionalSecondsProvider")
    void fractionalSeconds(Double from, String expectedIso, String description) {
        Duration expected = Duration.parse(expectedIso);
        Duration actual = callToDuration(from, null);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("zeroAndSpecialProvider")
    void zeroAndSpecial(Object from, String expectedIso, String description) {
        Duration expected = Duration.parse(expectedIso);
        Duration actual = callToDuration(from, null);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("overflowInfinityProvider")
    void overflowInfinity(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> callToDuration(from, null), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("largeFiniteWithinRangeProvider")
    void largeFiniteWithinRange(Double from, String expectedIso, String description) {
        Duration expected = Duration.parse(expectedIso);
        Duration actual = callToDuration(from, null);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("largeFiniteNanosecondOverflowProvider")
    void largeFiniteNanosecondOverflow(Double from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> callToDuration(from, null), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("subSecondPrecisionProvider")
    void subSecondPrecision(Double from, String expectedIso, String description) {
        Duration expected = Duration.parse(expectedIso);
        Duration actual = callToDuration(from, null);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullAndTypeMismatchProvider")
    void nullAndTypeMismatch(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> callToDuration(from, null), description);
    }

    @Test
    void converterParameterEffect() {
        // The method ignores the Converter argument; a non‑null converter should not affect the result.
        Double source = 7.25;
        Duration expected = Duration.parse("PT7.25S");
        Duration actual = callToDuration(source, mockConverter);
        assertEquals(expected, actual, "The method should ignore a non‑null Converter and produce the same Duration.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("finiteHugeValuesOverflowProvider")
    void finiteHugeValuesOverflow(Double from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> callToDuration(from, null), description);
    }
}
