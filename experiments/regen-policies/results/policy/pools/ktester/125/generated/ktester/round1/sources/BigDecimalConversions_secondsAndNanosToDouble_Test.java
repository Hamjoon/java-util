package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_secondsAndNanosToDouble_Test {

    static Stream<Arguments> standardCases() {
        return Stream.of(Arguments.of("Both seconds and nanos are zero – result should be zero.", 0L, 0L, "0"), Arguments.of("Positive seconds with zero nanos – result equals the seconds value.", 5L, 0L, "5"), Arguments.of("Zero seconds with half‑second nanos – result should be 0.5.", 0L, 500_000_000L, "0.5"), Arguments.of("Positive seconds and quarter‑second nanos – result should be 3.25.", 3L, 250_000_000L, "3.25"), Arguments.of("Negative seconds with zero nanos – result should be negative seconds.", -7L, 0L, "-7"), Arguments.of("Zero seconds with negative quarter‑second nanos – result should be -0.25.", 0L, -250_000_000L, "-0.25"), Arguments.of("Positive seconds with negative half‑second nanos – result should be 9.5.", 10L, -500_000_000L, "9.5"));
    }

    static Stream<Arguments> boundaryCases() {
        return Stream.of(Arguments.of("Maximum long seconds, zero nanos – verifies handling of the largest positive long.", Long.MAX_VALUE, 0L, "9223372036854775807"), Arguments.of("Minimum long seconds, zero nanos – verifies handling of the most negative long.", Long.MIN_VALUE, 0L, "-9223372036854775808"), // Nanos > 1 second (2_000_000_000) should be added as 2 seconds
        Arguments.of("Nanos greater than one second (2 × 10⁹) – should be added as 2 seconds, total 3.", 1L, 2_000_000_000L, "3"), Arguments.of("Negative nanos less than –1 second – should produce –2.", 0L, -2_000_000_000L, "-2"));
    }

    static Stream<Arguments> extremeCases() {
        return Stream.of(Arguments.of("Maximum long nanos – tests BigDecimal scaling with a very large numerator.", 0L, Long.MAX_VALUE, "9223372036.854775807"), Arguments.of("Minimum long nanos – tests negative large numerator handling.", 0L, Long.MIN_VALUE, "-9223372036.854775808"));
    }

    static Stream<Arguments> crossSignCases() {
        return Stream.of(Arguments.of("Negative seconds with positive half‑second nanos – result crosses zero to -0.5.", -1L, 500_000_000L, "-0.5"), Arguments.of("Positive seconds with negative two‑second nanos – total should be -1.", 1L, -2_000_000_000L, "-1"));
    }

    static Stream<Arguments> exactSecondNanos() {
        return Stream.of(Arguments.of("Zero seconds with exactly one‑second nanos – should produce 1.", 0L, 1_000_000_000L, "1"), Arguments.of("Zero seconds with exactly minus one‑second nanos – should produce -1.", 0L, -1_000_000_000L, "-1"));
    }

    static Stream<Arguments> smallFractionCases() {
        return Stream.of(Arguments.of("Nanos just below one second – verifies correct scaling to 0.999999999.", 0L, 999_999_999L, "0.999999999"), Arguments.of("Negative nanos just above minus one second – verifies correct scaling to -0.999999999.", 0L, -999_999_999L, "-0.999999999"), Arguments.of("Smallest positive nanos – should be represented as 0.000000001.", 0L, 1L, "0.000000001"), Arguments.of("Smallest negative nanos – should be represented as -0.000000001.", 0L, -1L, "-0.000000001"));
    }

    static Stream<Arguments> zeroCrossingCases() {
        return Stream.of(Arguments.of("Negative one second plus exactly one second nanos – result should be zero.", -1L, 1_000_000_000L, "0"));
    }

    static Stream<Arguments> largeMixedCases() {
        return Stream.of(Arguments.of("Maximum long seconds with one‑second nanos – verifies overflow handling beyond Long.MAX_VALUE.", Long.MAX_VALUE, 1_000_000_000L, "9223372036854775808"), Arguments.of("Minimum long seconds with minus one‑second nanos – verifies underflow handling beyond Long.MIN_VALUE.", Long.MIN_VALUE, -1_000_000_000L, "-9223372036854775809"), Arguments.of("Both seconds and nanos at Long.MAX_VALUE – tests BigDecimal scaling with very large numerator.", Long.MAX_VALUE, Long.MAX_VALUE, "18446744072854775807.854775807"), Arguments.of("Both seconds and nanos at Long.MIN_VALUE – tests negative large numerator handling.", Long.MIN_VALUE, Long.MIN_VALUE, "-18446744072854775808.854775808"));
    }

    static Stream<Arguments> integerBoundaryNanos() {
        return Stream.of(Arguments.of("Nanos equal to Integer.MAX_VALUE – verifies correct conversion to 2.147483647.", 0L, 2_147_483_647L, "2.147483647"), Arguments.of("Nanos equal to Integer.MIN_VALUE – verifies correct conversion to -2.147483648.", 0L, -2_147_483_648L, "-2.147483648"));
    }

    static Stream<Arguments> typicalMixedValues() {
        return Stream.of(Arguments.of("Typical positive seconds with matching nanos – checks normal fractional addition.", 123_456_789L, 123_456_789L, "123456789.123456789"), Arguments.of("Negative seconds with half‑second nanos – result should be -123456788.5.", -123_456_789L, 500_000_000L, "-123456788.5"));
    }

    @Test
    void zeroSecondsZeroNanos_returnsZero() {
        // given
        long seconds = 0L;
        long nanos = 0L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void positiveSecondsAndNanos_returnsCorrectDecimal() {
        // given
        long seconds = 5L;
        // 0.25 seconds
        long nanos = 250_000_000L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(5).add(BigDecimal.valueOf(250_000_000L, 9));
        assertEquals(0, expected.compareTo(result), "Result should equal 5.25");
    }

    @Test
    void negativeSecondsPositiveNanos_combinesCorrectly() {
        // given
        long seconds = -3L;
        // +0.5 seconds
        long nanos = 500_000_000L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(-3).add(BigDecimal.valueOf(500_000_000L, 9));
        assertEquals(0, expected.compareTo(result), "Result should equal -2.5");
    }

    @Test
    void maxLongSeconds_returnsBigDecimalWithSameValue() {
        // given
        long seconds = Long.MAX_VALUE;
        long nanos = 0L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(Long.MAX_VALUE);
        assertEquals(0, expected.compareTo(result), "Result should equal Long.MAX_VALUE");
    }

    @Test
    void nanosOneScaledToNineDecimalPlaces() {
        // given
        long seconds = 0L;
        // 0.000000001 seconds
        long nanos = 1L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(0).add(BigDecimal.valueOf(1L, 9));
        assertEquals(0, expected.compareTo(result), "Result should equal 0.000000001");
        assertEquals(9, result.scale(), "Scale should be 9 for nanosecond precision");
    }

    @Test
    void mixedPositiveAndNegativeValues() {
        // given
        long seconds = -1L;
        // just shy of a full second
        long nanos = 999_999_999L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        // -1 + 0.999999999 = -0.000000001
        BigDecimal expected = BigDecimal.valueOf(-1).add(BigDecimal.valueOf(999_999_999L, 9));
        assertEquals(0, expected.compareTo(result), "Result should equal -0.000000001");
    }

    // ---------- Standard Cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardCases")
    void standardCases(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }

    // ---------- Boundary Cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryCases")
    void boundaryCases(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }

    // ---------- Extreme Cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("extremeCases")
    void extremeCases(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }

    // ---------- Cross Sign Cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("crossSignCases")
    void crossSignCases(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }

    // ---------- Exact Second Nanos ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exactSecondNanos")
    void exactSecondNanos(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }

    // ---------- Small Fraction Cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("smallFractionCases")
    void smallFractionCases(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }

    // ---------- Zero Crossing Cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zeroCrossingCases")
    void zeroCrossingCases(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }

    // ---------- Large Mixed Cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("largeMixedCases")
    void largeMixedCases(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }

    // ---------- Integer Boundary Nanos ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("integerBoundaryNanos")
    void integerBoundaryNanos(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }

    // ---------- Typical Mixed Values ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("typicalMixedValues")
    void typicalMixedValues(String description, long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), description);
    }
}
