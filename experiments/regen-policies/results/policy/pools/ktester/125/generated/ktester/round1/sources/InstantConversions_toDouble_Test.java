package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toDouble_Test {

    /**
     * Shared {@link Converter} instance used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options – no custom overrides are needed.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference; helps GC in larger test suites.
        converter = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validInstantProvider() {
        return Stream.of(// Group: Valid Instant conversions
        arguments(Instant.ofEpochSecond(0, 0), null, 0.0d, "Instant at the epoch (0 seconds, 0 nanos) should convert to double 0.0"), arguments(Instant.ofEpochSecond(1, 0), null, 1.0d, "Positive whole seconds only – 1 second should become double 1.0"), arguments(Instant.ofEpochSecond(2, 500_000_000), null, 2.5d, "Positive seconds with half‑second nanos – 2 seconds + 500,000,000 nanos = 2.5"), arguments(Instant.ofEpochSecond(-1, 0), null, -1.0d, "Negative whole seconds – -1 second should become double -1.0"), arguments(Instant.ofEpochSecond(-1, 500_000_000), null, -0.5d, "Negative seconds with positive nanos – Instant normalises to -0.5 seconds"), arguments(Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999), null, Double.parseDouble("9.223372036854776E18"), "Maximum representable epoch second plus maximal nanos"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> boundaryProvider() {
        return Stream.of(// Group: Boundary and Edge Cases
        arguments(Instant.ofEpochSecond(Long.MIN_VALUE, 0), null, Double.parseDouble("-9.223372036854776E18"), "Minimum representable epoch second with zero nanos"), arguments(Instant.ofEpochSecond(0, 1), null, 1.0E-9, "Zero seconds with a single nanosecond"), arguments(Instant.ofEpochSecond(0, -1), null, -1.0E-9, "Zero seconds with a negative nanosecond (normalised to -1 ns)"), arguments(Instant.ofEpochSecond(0, 999_999_999), null, 0.999999999, "Maximum positive nanos just below one second"), arguments(Instant.ofEpochSecond(-0, 500_000_000), null, 0.5, "Negative zero seconds with positive nanos (normalises to +0.5)"), arguments(Instant.ofEpochSecond(-2, 250_000_000), null, -1.75, "Negative seconds with positive nanos – normalises to -1.75"), arguments(Instant.ofEpochSecond(2, -250_000_000), null, 1.75, "Positive seconds with negative nanos – normalises to 1.75"), arguments(Instant.ofEpochSecond(Long.MAX_VALUE, 0), null, Double.parseDouble("9.223372036854776E18"), "Maximum epoch second with zero nanos"), arguments(Instant.ofEpochSecond(Long.MIN_VALUE, 999_999_999), null, Double.parseDouble("-9.223372036854775E18"), "Minimum epoch second with maximal positive nanos"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidProvider() {
        return Stream.of(// Group: Invalid Input Handling
        arguments(null, null, NullPointerException.class, "Passing a null Instant should result in a NullPointerException."), arguments("2021-01-01T00:00:00Z", null, ClassCastException.class, "Providing a non‑Instant object (String) should cause a ClassCastException."), arguments(new Object(), null, ClassCastException.class, "Providing an arbitrary Object that is not an Instant should trigger a ClassCastException."));
    }

    /**
     * Verify that an {@link Instant} is correctly converted to a {@code double}
     * representing seconds (including fractional part for nanoseconds).
     */
    @Test
    void toDouble_convertsInstantToDouble_correctly() {
        // GIVEN
        // An instant representing 10 seconds and 500 000 000 nanoseconds (i.e. 10.5 seconds)
        Instant instant = Instant.ofEpochSecond(10L, 500_000_000L);
        double expected = 10.5d;
        // WHEN
        double actual = InstantConversions.toDouble(instant, converter);
        // THEN
        assertEquals(expected, actual, "Instant should be converted to its epoch seconds as a double, including the fractional nanos part");
    }

    /**
     * Verify conversion for an instant with zero nanoseconds – the result should be
     * an exact whole‑second double.
     */
    @Test
    void toDouble_zeroNanos_returnsWholeSeconds() {
        // GIVEN
        Instant instant = Instant.ofEpochSecond(12345L, 0L);
        double expected = 12345.0d;
        // WHEN
        double actual = InstantConversions.toDouble(instant, converter);
        // THEN
        assertEquals(expected, actual, "Instant with zero nanos should convert to a whole‑second double value");
    }

    /**
     * Verify that the method tolerates a {@code null} {@link Converter}.
     * The implementation does not use the converter, so a {@code null} value is acceptable.
     */
    @Test
    void toDouble_withNullConverter_returnsCorrectValue() {
        // GIVEN
        // 0.25 seconds
        Instant instant = Instant.ofEpochSecond(0L, 250_000_000L);
        double expected = 0.25d;
        // WHEN
        double actual = InstantConversions.toDouble(instant, null);
        // THEN
        assertEquals(expected, actual, "Converter argument is not used; passing null should still produce the correct double value");
    }

    /**
     * Valid Instant conversions – whole seconds, fractions, negative values,
     * and extreme epoch values.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validInstantProvider")
    void toDouble_validInstants(Instant from, Converter conv, double expected, String description) {
        // WHEN
        double actual = InstantConversions.toDouble(from, conv);
        // THEN
        assertEquals(expected, actual, 1e-12, () -> "Failed: " + description);
    }

    /**
     * Boundary and edge cases – tiny fractions, sign normalisation,
     * and the absolute limits of {@link Instant}.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("boundaryProvider")
    void toDouble_boundaryInstants(Instant from, Converter conv, double expected, String description) {
        // WHEN
        double actual = InstantConversions.toDouble(from, conv);
        // THEN
        assertEquals(expected, actual, 1e-12, () -> "Failed: " + description);
    }

    /**
     * Invalid inputs – null Instant, wrong type, etc. – must throw the expected exception.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidProvider")
    void toDouble_invalidInputs(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> InstantConversions.toDouble(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }
}
