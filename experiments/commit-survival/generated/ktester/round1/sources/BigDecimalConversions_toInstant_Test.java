package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toInstant_Test {

    @Mock
    private Converter // Converter is not used by toInstant, but required by the signature
    converter;

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// Whole seconds – the BigDecimal has no fractional part, so the resulting Instant has 0 nanoseconds.
        Arguments.of(new BigDecimal("1625097600"), "2021-07-01T00:00:00Z", "Whole seconds"), // Fractional seconds with exactly 9 decimal places – the nanoseconds are calculated as 123 456 789.
        Arguments.of(new BigDecimal("1625097600.123456789"), "2021-07-01T00:00:00.123456789Z", "Fractional seconds (9 digits)"), // Fractional part with fewer than 9 digits – the method pads the nanoseconds to 500 000 000.
        Arguments.of(new BigDecimal("10.5"), "1970-01-01T00:00:10.500000000Z", "Fractional part (fewer than 9 digits)"), // Negative whole seconds – the Instant represents one second before the epoch.
        Arguments.of(new BigDecimal("-1"), "1969-12-31T23:59:59Z", "Negative whole seconds"), // Negative fractional seconds – the remainder is negative, resulting in a normalized Instant of -2 seconds + 250 000 000 nanos.
        Arguments.of(new BigDecimal("-1.75"), "1969-12-31T23:59:58.250000000Z", "Negative fractional seconds"), // More than 9 decimal places – extra digits are discarded after moving the decimal point right by 9.
        Arguments.of(new BigDecimal("1.123456789123"), "1970-01-01T00:00:01.123456789Z", "More than 9 decimal places"), // Very large whole‑second value that still fits into a long; verifies that no overflow occurs.
        Arguments.of(new BigDecimal("9223372036"), "2262-04-11T23:47:16Z", "Very large whole‑second value"));
    }

    private static Stream<Arguments> zeroAndSimpleProvider() {
        return Stream.of(Arguments.of(new BigDecimal("0"), "1970-01-01T00:00:00Z", "Zero seconds"), Arguments.of(new BigDecimal("0.0"), "1970-01-01T00:00:00Z", "Zero with explicit decimal point"), Arguments.of(new BigDecimal("0.000000001"), "1970-01-01T00:00:00.000000001Z", "Smallest positive fraction (1 ns)"), Arguments.of(new BigDecimal("-0.000000001"), "1969-12-31T23:59:59.999999999Z", "Smallest negative fraction"));
    }

    private static Stream<Arguments> scientificNotationProvider() {
        return Stream.of(Arguments.of(new BigDecimal("1E3"), "1970-01-01T00:16:40Z", "Scientific notation (1000 seconds)"), Arguments.of(new BigDecimal("10.5000"), "1970-01-01T00:00:10.500000000Z", "Trailing zeros after fractional part"));
    }

    private static Stream<Arguments> largeValuesProvider() {
        return Stream.of(// Maximum positive long seconds – still within Instant range.
        Arguments.of(new BigDecimal(Long.toString(Long.MAX_VALUE)), "292278994-08-17T07:12:55.807Z", "Maximum positive long seconds"));
    }

    private static Stream<Arguments> largeValuesExceptionProvider() {
        return Stream.of(// Maximum long seconds plus a fraction pushes the instant past the supported maximum.
        Arguments.of(new BigDecimal(Long.toString(Long.MAX_VALUE) + ".5"), DateTimeException.class, "Maximum long seconds plus fraction → overflow"), // Minimum long seconds – the earliest Instant that fits in a signed long.
        Arguments.of(new BigDecimal(Long.toString(Long.MIN_VALUE)), Instant.parse("-292278994-08-17T07:12:55.808Z"), "Minimum long seconds"));
    }

    private static Stream<Arguments> nanoOverflowProvider() {
        return Stream.of(// Fractional part with 10 digits – after moving the decimal point right 9 places the nanosecond adjustment exceeds 1 000 000 000, causing the seconds to roll over.
        Arguments.of(new BigDecimal("0.9999999995"), "1970-01-01T00:00:09.999999995Z", "Positive fractional overflow"), // Negative fractional part with overflow – the negative nanosecond adjustment rolls the instant back by 9 seconds plus 5 ns.
        Arguments.of(new BigDecimal("-0.9999999995"), "1969-12-31T23:59:50.000000005Z", "Negative fractional overflow"));
    }

    private static Stream<Arguments> exceptionHandlingProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Null source object"), Arguments.of("123", ClassCastException.class, "Source is a String, not a BigDecimal"));
    }

    private static Stream<Arguments> instantRangeOverflowProvider() {
        return Stream.of(// Whole seconds just beyond Instant.MAX (31556889864403199) – the epoch second is out of the supported range.
        Arguments.of(new BigDecimal("31556889864403200"), DateTimeException.class, "Just beyond Instant.MAX (positive)"), // Whole seconds just before Instant.MIN (‑31557014167219200) – the epoch second is below the supported range.
        Arguments.of(new BigDecimal("-31557014167219201"), DateTimeException.class, "Just before Instant.MIN (negative)"), // Maximum whole‑second value plus a fractional part – the nanosecond adjustment pushes the instant past Instant.MAX.
        Arguments.of(new BigDecimal("31556889864403199.5"), DateTimeException.class, "Maximum whole‑second + fraction → overflow"), // Minimum whole‑second value minus a fractional part – the negative nanosecond adjustment moves the instant before Instant.MIN.
        Arguments.of(new BigDecimal("-31557014167219200.5"), DateTimeException.class, "Minimum whole‑second – fraction → underflow"));
    }

    @Test
    void toInstant_withWholeSeconds() {
        // given
        // 2021‑06‑01T12:00:00Z
        BigDecimal input = new BigDecimal("1622548800");
        Instant expected = Instant.ofEpochSecond(1622548800L);
        // when
        Instant result = BigDecimalConversions.toInstant(input, converter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void toInstant_withFractionalSeconds() {
        // given
        // 2021‑06‑01T12:00:00.123456789Z
        BigDecimal input = new BigDecimal("1622548800.123456789");
        Instant expected = Instant.ofEpochSecond(1622548800L, 123_456_789L);
        // when
        Instant result = BigDecimalConversions.toInstant(input, converter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void toInstant_withNegativeWholeAndFractionalSeconds() {
        // given
        // -1.5 seconds relative to epoch
        BigDecimal input = new BigDecimal("-1.5");
        // Java normalises negative nanos: -1 sec - 0.5 sec = -2 sec + 0.5 sec
        Instant expected = Instant.ofEpochSecond(-2L, 500_000_000L);
        // when
        Instant result = BigDecimalConversions.toInstant(input, converter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void toInstant_withZero() {
        // given
        BigDecimal input = BigDecimal.ZERO;
        Instant expected = Instant.ofEpochSecond(0L, 0L);
        // when
        Instant result = BigDecimalConversions.toInstant(input, converter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void toInstant_withLargeValues() {
        // given
        // ~100 years + fractional nanoseconds
        BigDecimal input = new BigDecimal("3155695200.999999999");
        Instant expected = Instant.ofEpochSecond(3_155_695_200L, 999_999_999L);
        // when
        Instant result = BigDecimalConversions.toInstant(input, converter);
        // then
        assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     *  Valid conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionsProvider")
    void toInstant_validConversions(BigDecimal input, String expectedIso, String description) {
        // given – input is already prepared by the provider
        Instant expected = Instant.parse(expectedIso);
        // when
        Instant result = BigDecimalConversions.toInstant(input, converter);
        // then
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Zero and simple values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("zeroAndSimpleProvider")
    void toInstant_zeroAndSimple(BigDecimal input, String expectedIso, String description) {
        Instant expected = Instant.parse(expectedIso);
        Instant result = BigDecimalConversions.toInstant(input, converter);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Scientific notation and trailing zeros
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("scientificNotationProvider")
    void toInstant_scientificNotation(BigDecimal input, String expectedIso, String description) {
        Instant expected = Instant.parse(expectedIso);
        Instant result = BigDecimalConversions.toInstant(input, converter);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Large values and overflow handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("largeValuesProvider")
    void toInstant_largeValues(BigDecimal input, String expectedIso, String description) {
        Instant expected = Instant.parse(expectedIso);
        Instant result = BigDecimalConversions.toInstant(input, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("largeValuesExceptionProvider")
    void toInstant_largeValues_exceptions(BigDecimal input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigDecimalConversions.toInstant(input, converter), description);
    }

    /* -------------------------------------------------------------
     *  Nanosecond overflow via fractional part
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nanoOverflowProvider")
    void toInstant_nanosecondOverflow(BigDecimal input, String expectedIso, String description) {
        Instant expected = Instant.parse(expectedIso);
        Instant result = BigDecimalConversions.toInstant(input, converter);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Exception handling (null & wrong type)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionHandlingProvider")
    void toInstant_exceptionHandling(Object input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigDecimalConversions.toInstant(input, converter), description);
    }

    /* -------------------------------------------------------------
     *  Instant range overflow
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("instantRangeOverflowProvider")
    void toInstant_instantRangeOverflow(BigDecimal input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigDecimalConversions.toInstant(input, converter), description);
    }
}
