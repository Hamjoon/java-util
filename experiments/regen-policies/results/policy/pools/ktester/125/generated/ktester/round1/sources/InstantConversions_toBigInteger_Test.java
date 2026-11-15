package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toBigInteger_Test {

    /**
     * The mock Converter – the method under test does not use it, but the signature requires it.
     */
    @Mock
    private static Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to avoid any accidental interactions.
        // (MockitoExtension already resets mocks, but we keep it explicit.)
        org.mockito.Mockito.reset(mockConverter);
    }

    /**
     * Helper that replicates the conversion logic used inside {@code InstantConversions.toBigInteger}
     * to build the expected result.
     */
    private static BigInteger expectedBigInteger(Instant instant) {
        long seconds = instant.getEpochSecond();
        int nanos = instant.getNano();
        return BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nanos));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> provideStandardConversions() {
        return Stream.of(Arguments.of(Instant.ofEpochSecond(0, 0), null, "Zero epoch seconds and zero nanoseconds should produce a BigInteger value of 0."), Arguments.of(Instant.ofEpochSecond(1, 0), null, "One second (1,000,000,000 nanoseconds) should be converted correctly."), Arguments.of(Instant.ofEpochSecond(0, 123_456_789), null, "Zero seconds with 123,456,789 nanoseconds should return that exact nanosecond count."), Arguments.of(Instant.ofEpochSecond(42, 987_654_321), null, "Mixed seconds and nanoseconds: 42 × 10⁹ + 987,654,321 nanoseconds."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> provideEdgeCases() {
        return Stream.of(Arguments.of(Instant.MAX, null, "Instant.MAX (the farthest future instant supported) should be converted to its nanosecond representation."), Arguments.of(Instant.MIN, null, "Instant.MIN (the farthest past instant supported) should be converted to its nanosecond representation (negative value)."), Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999), null, "Maximum long epoch seconds with maximum nanosecond fraction; verifies handling of large positive values without overflow."), Arguments.of(Instant.ofEpochSecond(Long.MIN_VALUE, 0), null, "Minimum long epoch seconds (negative) with zero nanoseconds; checks correct handling of large negative values."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> provideNegativeMixedCases() {
        return Stream.of(Arguments.of(Instant.ofEpochSecond(-1, 500_000_000), null, "Negative epoch seconds with positive nanoseconds are normalized; -1 s + 500 ms = -500 000 000 ns."), Arguments.of(Instant.ofEpochSecond(1, -500_000_000), null, "Positive epoch seconds with negative nanoseconds are normalized; 1 s – 500 ms = 500 000 000 ns."), Arguments.of(Instant.ofEpochSecond(0, -1), null, "Zero seconds with a negative nanosecond fraction normalizes to -1 ns."), Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE, -1), null, "Maximum long seconds with a negative nanosecond fraction; verifies subtraction of one nanosecond."), Arguments.of(Instant.ofEpochSecond(Long.MIN_VALUE, 1), null, "Minimum long seconds with a positive nanosecond fraction; checks handling of large negative values plus one nanosecond."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> provideConverterEffectCases() {
        return Stream.of(Arguments.of(Instant.ofEpochSecond(5, 250_000_000), mockConverter, "A non‑null Converter instance is supplied but the method does not use it; conversion should be identical to the standard case."), Arguments.of(Instant.ofEpochSecond(-3, 750_000_000), mockConverter, "Non‑null Converter with a negative‑seconds/positive‑nanoseconds instant; confirms the converter argument is ignored."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> provideNormalizationEdgeCases() {
        return Stream.of(Arguments.of(Instant.ofEpochSecond(0, 1_000_000_000), null, "Nanosecond fraction equal to one billion rolls over to an extra second; result should be exactly one second in nanoseconds."), Arguments.of(Instant.ofEpochSecond(-2, 1_500_000_000), null, "Negative seconds with nanoseconds > 1 000 000 000 normalize to a less‑negative total; -2 s + 1.5 s = -0.5 s = -500 000 000 ns."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> provideErrorCases() {
        return Stream.of(Arguments.of(null, null, NullPointerException.class, "Passing a null `from` argument should result in a NullPointerException because of the cast to Instant."), Arguments.of("2023-01-01T00:00:00Z", null, ClassCastException.class, "Providing an object that is not an Instant (e.g., a String) should cause a ClassCastException."));
    }

    @Test
    @DisplayName("toBigInteger – epoch instant (0 seconds, 0 nanos)")
    void testEpochInstant() {
        // GIVEN
        // 1970‑01‑01T00:00:00Z
        Instant instant = Instant.EPOCH;
        // WHEN
        BigInteger result = InstantConversions.toBigInteger(instant, mockConverter);
        // THEN
        assertEquals(expectedBigInteger(instant), result, "Conversion of the epoch instant should yield zero as a BigInteger");
    }

    @Test
    @DisplayName("toBigInteger – positive seconds with nanoseconds")
    void testPositiveInstant() {
        // GIVEN
        // 2023‑03‑15T12:34:56.123456789Z
        Instant instant = Instant.ofEpochSecond(1_680_000_000L, 123_456_789L);
        // WHEN
        BigInteger result = InstantConversions.toBigInteger(instant, mockConverter);
        // THEN
        assertEquals(expectedBigInteger(instant), result, "Conversion should correctly combine seconds and nanos for a positive instant");
    }

    @Test
    @DisplayName("toBigInteger – negative seconds with nanoseconds")
    void testNegativeInstant() {
        // GIVEN
        // 1960‑01‑01T00:00:00.987654321Z (negative epoch seconds)
        Instant instant = Instant.ofEpochSecond(-315_619_200L, 987_654_321L);
        // WHEN
        BigInteger result = InstantConversions.toBigInteger(instant, mockConverter);
        // THEN
        assertEquals(expectedBigInteger(instant), result, "Conversion should correctly handle negative epoch seconds and nanos");
    }

    @Test
    @DisplayName("toBigInteger – zero seconds with non‑zero nanos")
    void testZeroSecondsNonZeroNanos() {
        // GIVEN
        Instant instant = Instant.ofEpochSecond(0L, 555_555_555L);
        // WHEN
        BigInteger result = InstantConversions.toBigInteger(instant, mockConverter);
        // THEN
        assertEquals(expectedBigInteger(instant), result, "Conversion should correctly handle the case where seconds are zero but nanos are present");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideStandardConversions")
    void testStandardConversions(Instant instant, Converter converter, String description) {
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expectedBigInteger(instant), result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideEdgeCases")
    void testEdgeCases(Instant instant, Converter converter, String description) {
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expectedBigInteger(instant), result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideNegativeMixedCases")
    void testNegativeAndMixedSignValues(Instant instant, Converter converter, String description) {
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expectedBigInteger(instant), result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideConverterEffectCases")
    void testConverterParameterEffect(Instant instant, Converter converter, String description) {
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expectedBigInteger(instant), result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideNormalizationEdgeCases")
    void testNormalizationEdgeCases(Instant instant, Converter converter, String description) {
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expectedBigInteger(instant), result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideErrorCases")
    void testErrorHandling(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> InstantConversions.toBigInteger(from, converter), description);
    }
}
