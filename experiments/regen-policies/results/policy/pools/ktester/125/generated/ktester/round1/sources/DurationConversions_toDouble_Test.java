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
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toDouble_Test {

    @Mock
    private Converter // Not used by the method but required by the signature
    converter;

    private static Stream<Arguments> provideValidDurations() {
        return Stream.of(// Positive duration with seconds and nanos
        Arguments.of(Duration.ofSeconds(5, 200_000_000), 5.2, "Positive duration with seconds and nanos; 5 + 0.2 = 5.2"), // Zero duration
        Arguments.of(Duration.ZERO, 0.0, "Zero duration should produce 0.0"), // Negative duration where both parts are negative
        Arguments.of(Duration.ofSeconds(-3, -500_000_000), -3.5, "Negative duration with both seconds and nanos negative"), // Overflow to positive infinity (seconds = Long.MAX_VALUE)
        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 0), Double.POSITIVE_INFINITY, "Duration at Long.MAX_VALUE seconds overflows to +Infinity"), // Underflow to negative infinity (seconds = Long.MIN_VALUE)
        Arguments.of(Duration.ofSeconds(Long.MIN_VALUE, 0), Double.NEGATIVE_INFINITY, "Duration at Long.MIN_VALUE seconds underflows to -Infinity"), // Overflow with maximum seconds plus maximum nanos
        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), Double.POSITIVE_INFINITY, "Maximum seconds plus maximum nanos still overflows to +Infinity"), // Simple whole‑second duration
        Arguments.of(Duration.ofSeconds(2, 0), 2.0, "Whole‑second duration should convert to exact double"), // Positive nanoseconds only
        Arguments.of(Duration.ofNanos(250_000_000), 0.25, "250 000 000 ns = 0.25 s"), // Negative nanoseconds only
        Arguments.of(Duration.ofNanos(-250_000_000), -0.25, "-250 000 000 ns = -0.25 s"), // Maximum positive nanoseconds without whole seconds
        Arguments.of(Duration.ofNanos(999_999_999), 0.999999999, "Maximum sub‑second nanoseconds"), // Negative seconds combined with positive nanos (normalisation)
        Arguments.of(Duration.ofSeconds(-2, 500_000_000), -1.5, "Negative seconds with positive nanos normalises to -1.5 s"), // Positive seconds combined with negative nanos (normalisation)
        Arguments.of(Duration.ofSeconds(2, -300_000_000), 1.7, "Positive seconds with negative nanos normalises to 1.7 s"));
    }

    private static Stream<Arguments> provideExceptionDurations() {
        return Stream.of(// Null source – should raise NPE during cast
        Arguments.of(null, NullPointerException.class, "Null 'from' argument must cause NullPointerException"), // Wrong type – ClassCastException expected
        Arguments.of("not a duration", ClassCastException.class, "Non‑Duration source should cause ClassCastException"));
    }

    @Test
    void toDouble_zeroDuration_returnsZero() {
        Duration duration = Duration.ZERO;
        double result = DurationConversions.toDouble(duration, converter);
        assertEquals(0.0d, result, "Zero duration must be converted to 0.0");
    }

    @Test
    void toDouble_positiveDuration_returnsCorrectDouble() {
        // 5 seconds and 123,456,789 nanoseconds
        Duration duration = Duration.ofSeconds(5, 123_456_789);
        double expected = duration.getSeconds() + duration.getNano() / 1_000_000_000d;
        double result = DurationConversions.toDouble(duration, converter);
        assertEquals(expected, result, 1e-12, "Positive duration conversion is incorrect");
    }

    @Test
    void toDouble_negativeDuration_returnsCorrectDouble() {
        // -3 seconds and -500,000,000 nanoseconds (will be normalized by Duration)
        Duration duration = Duration.ofSeconds(-3, -500_000_000);
        double expected = duration.getSeconds() + duration.getNano() / 1_000_000_000d;
        double result = DurationConversions.toDouble(duration, converter);
        assertEquals(expected, result, 1e-12, "Negative duration conversion is incorrect");
    }

    @Test
    void toDouble_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DurationConversions.toDouble(null, converter), "Calling toDouble with null should throw NPE");
    }

    @Test
    void toDouble_invalidSource_throwsClassCastException() {
        String notADuration = "not a duration";
        assertThrows(ClassCastException.class, () -> DurationConversions.toDouble(notADuration, converter), "Calling toDouble with a non‑Duration source should throw ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideValidDurations")
    void toDouble_validDurations(Object from, double expected, String description) {
        // The converter argument is not used; we pass {@code null} to emphasise that fact.
        double result = DurationConversions.toDouble(from, null);
        if (Double.isInfinite(expected)) {
            // For overflow/underflow we only care about the sign of infinity
            assertTrue(Double.isInfinite(result) && Math.signum(result) == Math.signum(expected), () -> "Expected " + expected + " but got " + result + " – " + description);
        } else {
            // Use a tight tolerance for normal values
            assertEquals(expected, result, 1e-12, () -> "Conversion result differs – " + description);
        }
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideExceptionDurations")
    void toDouble_exceptions(Object from, Class<? extends Throwable> expectedException, String description) {
        // The method should throw the expected exception regardless of the converter argument.
        assertThrows(expectedException, () -> DurationConversions.toDouble(from, null), description);
    }

    @Test
    void toDouble_nonNullConverter_ignored() {
        // Create a dummy Converter mock – the implementation is irrelevant.
        Converter dummyConverter = mock(Converter.class);
        Duration duration = Duration.ofSeconds(3, 0);
        double result = DurationConversions.toDouble(duration, dummyConverter);
        assertEquals(3.0, result, 1e-12, "Method must ignore the Converter argument and return correct value");
    }
}
