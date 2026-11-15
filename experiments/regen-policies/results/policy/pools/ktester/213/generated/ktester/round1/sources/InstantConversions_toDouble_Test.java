package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.mockito.Mockito;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class InstantConversions_toDouble_Test {

    /**
     * Shared {@link Converter} instance used for the tests.
     */
    private static Converter converter;

    /**
     * Simple holder for a single test case.
     */
    private static final class TestCase {

        final Instant from;

        // may be null
        final Converter converter;

        // Double or Class<? extends Throwable>
        final Object expected;

        final String description;

        TestCase(Instant from, Converter converter, Object expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The method under test does not actually use the Converter,
        // but a real instance is created for completeness.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /* ----------  Valid Instant conversions  ---------- */
    private static Stream<TestCase> validInstantConversions() {
        return Stream.of(new TestCase(Instant.ofEpochSecond(0, 0), null, 0.0, "Epoch instant (0 seconds, 0 nanos) should convert to double 0.0."), new TestCase(Instant.ofEpochSecond(1, 0), null, 1.0, "Instant with exactly one second and no nanos should convert to double 1.0."), new TestCase(Instant.ofEpochSecond(123456789L, 0), null, 123456789.0, "Large positive epoch seconds without nanos should be represented accurately as a double."));
    }

    /* ----------  Instants with nanosecond component  ---------- */
    private static Stream<TestCase> instantsWithNanos() {
        return Stream.of(new TestCase(Instant.ofEpochSecond(0, 1), null, 1.0e-9, "Zero seconds plus 1 nanosecond should convert to 1e-9."), new TestCase(Instant.ofEpochSecond(10, 500_000_000L), null, 10.5, "10 seconds and 500,000,000 nanos (half a second) should convert to 10.5."), new TestCase(Instant.ofEpochSecond(-5, 250_000_000L), null, -4.75, "Negative epoch seconds with positive nanos should be normalized (‑5 s + 0.25 s = ‑4.75)."));
    }

    /* ----------  Negative epoch seconds  ---------- */
    private static Stream<TestCase> negativeEpochSeconds() {
        return Stream.of(new TestCase(Instant.ofEpochSecond(-1, 0), null, -1.0, "Instant exactly one second before epoch should convert to -1.0."), new TestCase(Instant.ofEpochSecond(-123456789L, 0), null, -123456789.0, "Large negative epoch seconds without nanos should be represented accurately as a double."));
    }

    /* ----------  Error handling / exceptional cases  ---------- */
    private static Stream<TestCase> exceptionalCases() {
        return Stream.of(new TestCase(null, null, NullPointerException.class, "Passing null as the source object should result in a NullPointerException."), new TestCase((Instant) (Object) "2023-01-01T00:00:00Z", null, ClassCastException.class, "Providing a String instead of an Instant should cause a ClassCastException."), new TestCase((Instant) (Object) new java.util.Date(), null, ClassCastException.class, "Providing a Date instead of an Instant should also trigger a ClassCastException."));
    }

    /* ----------  Normalization of nanosecond adjustment  ---------- */
    private static Stream<TestCase> nanosecondNormalization() {
        return Stream.of(new TestCase(Instant.ofEpochSecond(0, -1), null, -1.0e-9, "Negative nanosecond adjustment should be normalized to a negative fraction of a second."), new TestCase(Instant.ofEpochSecond(0, 2_000_000_000L), null, 2.0, "Nanosecond adjustment larger than one second should roll over into whole seconds."), new TestCase(Instant.ofEpochSecond(-2, -500_000_000L), null, -2.5, "Both epoch seconds and nanosecond adjustment negative; should normalize to –2.5 seconds."));
    }

    /* ----------  Maximum nanosecond fraction  ---------- */
    private static Stream<TestCase> maximumNanosecondFraction() {
        return Stream.of(new TestCase(Instant.ofEpochSecond(0, 999_999_999), null, 0.999999999, "Instant with the largest possible positive nanosecond fraction should convert to 0.999999999."), new TestCase(Instant.ofEpochSecond(-1, 999_999_999), null, -1.0e-9, "Negative second with almost full nanosecond fraction normalizes to a tiny negative value."));
    }

    /* ----------  Boundary large epoch seconds  ---------- */
    private static Stream<TestCase> boundaryLargeEpochSeconds() {
        return Stream.of(new TestCase(Instant.MAX, null, Double.POSITIVE_INFINITY, "Instant.MAX exceeds double range; conversion should yield Double.POSITIVE_INFINITY."), new TestCase(Instant.MIN, null, Double.NEGATIVE_INFINITY, "Instant.MIN is less than Double.MIN_VALUE; conversion should yield Double.NEGATIVE_INFINITY."), new TestCase(Instant.ofEpochSecond(Long.MAX_VALUE, 0), null, Double.POSITIVE_INFINITY, "Maximum long epoch seconds without nanos also overflow to positive infinity."), new TestCase(Instant.ofEpochSecond(Long.MIN_VALUE, 0), null, Double.NEGATIVE_INFINITY, "Minimum long epoch seconds without nanos overflow to negative infinity."));
    }

    /* ----------  Converter parameter effect  ---------- */
    private static Stream<TestCase> converterParameterEffect() {
        // Create a dummy Converter instance (real implementation not needed)
        Converter dummy = new Converter(new ConverterOptions());
        return Stream.of(new TestCase(Instant.ofEpochSecond(42, 123_456_789L), dummy, 42.123456789, "A non‑null Converter is ignored by toDouble; result should be based solely on the Instant value."));
    }

    /* ----------  Rounding edge cases  ---------- */
    private static Stream<TestCase> roundingEdgeCases() {
        return Stream.of(new TestCase(Instant.ofEpochSecond(9_007_199_254_740_992L, 0), null, 9.007199254740992E15, "Epoch seconds at the double integer precision limit should convert without loss."), new TestCase(Instant.ofEpochSecond(9_007_199_254_740_993L, 0), null, 9.007199254740992E15, "Value just above the exact‑integer limit should round to the nearest representable double (still 2^53)."));
    }

    @Test
    void toDouble_convertsInstantWithNanos_correctly() {
        // Instant representing 1 second + 500,000,000 nanoseconds = 1.5 seconds
        Instant instant = Instant.ofEpochSecond(1, 500_000_000L);
        double result = InstantConversions.toDouble(instant, converter);
        assertEquals(1.5, result, 1e-12, "Instant should be converted to 1.5 seconds");
    }

    @Test
    void toDouble_convertsInstantWithoutNanos_correctly() {
        // Instant representing exactly 123,456,789 seconds since epoch
        Instant instant = Instant.ofEpochSecond(123_456_789L, 0);
        double result = InstantConversions.toDouble(instant, converter);
        assertEquals(123_456_789.0, result, 1e-12, "Instant with zero nanos should be converted to its epoch seconds as double");
    }

    @Test
    void toDouble_usesMockConverter_withoutInteraction() {
        // The method does not call any method on the Converter, so we verify that.
        // 10.25 seconds
        Instant instant = Instant.ofEpochSecond(10, 250_000_000L);
        Converter mockConverter = Mockito.mock(Converter.class);
        double result = InstantConversions.toDouble(instant, mockConverter);
        assertEquals(10.25, result, 1e-12, "Instant should be converted to 10.25 seconds");
        // Verify that the mock was never interacted with.
        Mockito.verifyNoInteractions(mockConverter);
    }

    @Test
    void toDouble_nullInstant_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> InstantConversions.toDouble(null, converter), "Passing null as the source should throw NPE because of the cast to Instant");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validInstantConversions")
    void testValidInstantConversions(TestCase tc) {
        double result = InstantConversions.toDouble(tc.from, tc.converter);
        assertEquals((Double) tc.expected, result, 1e-12, tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("instantsWithNanos")
    void testInstantsWithNanosecondComponent(TestCase tc) {
        double result = InstantConversions.toDouble(tc.from, tc.converter);
        assertEquals((Double) tc.expected, result, 1e-12, tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("negativeEpochSeconds")
    void testNegativeEpochSeconds(TestCase tc) {
        double result = InstantConversions.toDouble(tc.from, tc.converter);
        assertEquals((Double) tc.expected, result, 1e-12, tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionalCases")
    void testExceptionalCases(TestCase tc) {
        assertThrows((Class<? extends Throwable>) tc.expected, () -> InstantConversions.toDouble(tc.from, tc.converter), tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nanosecondNormalization")
    void testNanosecondNormalization(TestCase tc) {
        double result = InstantConversions.toDouble(tc.from, tc.converter);
        assertEquals((Double) tc.expected, result, 1e-12, tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("maximumNanosecondFraction")
    void testMaximumNanosecondFraction(TestCase tc) {
        double result = InstantConversions.toDouble(tc.from, tc.converter);
        assertEquals((Double) tc.expected, result, 1e-12, tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryLargeEpochSeconds")
    void testBoundaryLargeEpochSeconds(TestCase tc) {
        double result = InstantConversions.toDouble(tc.from, tc.converter);
        if (Double.isInfinite((Double) tc.expected)) {
            assertTrue(Double.isInfinite(result) && Math.signum(result) == Math.signum((Double) tc.expected), tc.description);
        } else {
            assertEquals((Double) tc.expected, result, 1e-12, tc.description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("converterParameterEffect")
    void testConverterParameterEffect(TestCase tc) {
        double result = InstantConversions.toDouble(tc.from, tc.converter);
        assertEquals((Double) tc.expected, result, 1e-12, tc.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("roundingEdgeCases")
    void testRoundingEdgeCases(TestCase tc) {
        double result = InstantConversions.toDouble(tc.from, tc.converter);
        assertEquals((Double) tc.expected, result, 1e-12, tc.description);
    }
}
