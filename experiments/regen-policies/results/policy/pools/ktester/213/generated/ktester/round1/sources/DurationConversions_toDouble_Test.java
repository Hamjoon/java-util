package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toDouble_Test {

    /**
     * A shared {@link Converter} instance – the method under test does not use it,
     *  but the signature requires it.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The library expects a non‑null options object.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that mirrors the logic used inside {@code DurationConversions.toDouble}.
     */
    private static double expectedFromDuration(Duration d) {
        long seconds = d.getSeconds();
        int nanos = d.getNano();
        // The library’s implementation uses a BigDecimal for precision,
        // but for test purposes a double calculation with the same scale is sufficient.
        return seconds + nanos / 1_000_000_000d;
    }

    /**
     * Provides all valid conversion test cases extracted from the JSON payload.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Zero duration
        Arguments.of(Duration.ZERO, 0.0, "Zero duration should be converted to double 0.0"), // Positive whole‑second
        Arguments.of(Duration.ofSeconds(5), 5.0, "Positive whole‑second duration converts to the same double value"), // Negative whole‑second
        Arguments.of(Duration.ofSeconds(-3), -3.0, "Negative whole‑second duration converts to a negative double"), // Half‑second nanos
        Arguments.of(Duration.ofSeconds(2, 500_000_000), 2.5, "Duration with half‑second nanos (500,000,000) yields 2.5"), // Negative half‑second nanos (normalised)
        Arguments.of(Duration.ofSeconds(-2, -500_000_000), -2.5, "Negative duration with negative nanos normalises to -2.5 seconds"), // Max long seconds + max nanos
        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), 9.223372036854776E18, "Maximum long seconds plus max nanos produces a very large double (approx Long.MAX_VALUE + 0.999999999)"), // Min long seconds
        Arguments.of(Duration.ofSeconds(Long.MIN_VALUE), -9.223372036854776E18, "Minimum long seconds converts to a very large negative double"), // Smallest positive nanosecond
        Arguments.of(Duration.ofSeconds(0, 1), 1.0E-9, "Smallest positive nanosecond duration should convert to 1e-9 seconds"), // Smallest negative nanosecond
        Arguments.of(Duration.ofSeconds(0, -1), -1.0E-9, "Smallest negative nanosecond duration should convert to -1e-9 seconds"), // Max long seconds without nanos
        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 0), 9.223372036854776E18, "Maximum long seconds without nanos yields the same large double as Long.MAX_VALUE"), // Min long seconds with -1 nanosecond (tests rounding beyond boundary)
        Arguments.of(Duration.ofSeconds(Long.MIN_VALUE, -1), -9.223372036854777E18, "Minimum long seconds with -1 nanosecond tests rounding beyond Long.MIN_VALUE boundary"));
    }

    /**
     * Provides exception‑related test cases extracted from the JSON payload.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source object
        Arguments.of(null, NullPointerException.class, "Passing null as the source object should throw NullPointerException because the method casts to Duration and then dereferences it."), // Non‑Duration object
        Arguments.of("not a duration", ClassCastException.class, "Providing an object that is not a java.time.Duration causes a ClassCastException when the method attempts to cast the argument to Duration."));
    }

    @Test
    void toDouble_zeroDuration_returnsZero() {
        Duration duration = Duration.ZERO;
        double expected = expectedFromDuration(duration);
        double actual = DurationConversions.toDouble(duration, converter);
        assertEquals(expected, actual, 0d, "Zero duration should convert to 0.0");
    }

    @Test
    void toDouble_wholeSeconds_returnsExactValue() {
        Duration duration = Duration.ofSeconds(12345);
        double expected = expectedFromDuration(duration);
        double actual = DurationConversions.toDouble(duration, converter);
        assertEquals(expected, actual, 0d, "Whole‑second duration conversion mismatch");
    }

    @Test
    void toDouble_secondsAndNanos_returnsPreciseValue() {
        // 5 seconds and 250 milliseconds (250,000,000 nanos)
        Duration duration = Duration.ofSeconds(5).plusNanos(250_000_000);
        double expected = expectedFromDuration(duration);
        double actual = DurationConversions.toDouble(duration, converter);
        // Allow a tiny epsilon due to double rounding.
        assertEquals(expected, actual, 1e-12, "Duration with nanos should convert accurately");
    }

    @Test
    void toDouble_negativeDuration_returnsNegativeValue() {
        // -3 seconds and -400 milliseconds (i.e. -3.4 seconds)
        Duration duration = Duration.ofSeconds(-3).minusNanos(400_000_000);
        double expected = expectedFromDuration(duration);
        double actual = DurationConversions.toDouble(duration, converter);
        assertEquals(expected, actual, 1e-12, "Negative duration conversion mismatch");
    }

    @Test
    void toDouble_largeDuration_handlesBigValues() {
        // 2^31 seconds (~68 years) and a few nanos – beyond typical int range.
        // 2,147,483,648
        long largeSeconds = Integer.MAX_VALUE + 1L;
        Duration duration = Duration.ofSeconds(largeSeconds).plusNanos(123);
        double expected = expectedFromDuration(duration);
        double actual = DurationConversions.toDouble(duration, converter);
        // The magnitude is huge; a relative tolerance is more appropriate.
        double relativeEpsilon = 1e-12;
        assertEquals(expected, actual, Math.abs(expected) * relativeEpsilon, "Large duration conversion should stay within double precision limits");
    }

    @Test
    void toDouble_invalidObject_throwsClassCastException() {
        Object notADuration = "I am not a Duration";
        assertThrows(ClassCastException.class, () -> DurationConversions.toDouble(notADuration, converter), "Passing a non‑Duration object should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toDouble_validDurations_convertCorrectly(Duration from, double expected, String description) {
        // The method under test does not use the converter, so we can safely pass null.
        double actual = DurationConversions.toDouble(from, null);
        // Use a tolerance that accommodates double rounding for extreme values.
        double epsilon = (Math.abs(expected) > 1e15) ? 1e3 : 1e-12;
        assertEquals(expected, actual, epsilon, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    void toDouble_invalidInputs_throwExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> DurationConversions.toDouble(from, null), description);
    }
}
