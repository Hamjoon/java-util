package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toDouble_Test {

    /**
     * The Converter instance required by the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Auto‑closeable for Mockito annotations.
     */
    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Verify that the Converter mock was not used (the method does not depend on it).
        verifyNoInteractions(mockConverter);
        // Close the Mockito resources.
        mocks.close();
    }

    /**
     * Provides valid {@link Duration} instances together with their expected double results.
     */
    private static Stream<Arguments> validDurationProvider() {
        return Stream.of(// Zero duration
        arguments(Duration.ZERO, 0.0), // Whole‑second values
        arguments(Duration.ofSeconds(5), 5.0), arguments(Duration.ofSeconds(-3), -3.0), // Seconds with positive nanoseconds
        arguments(Duration.ofSeconds(2, 500_000_000), 2.5), // Seconds with negative nanoseconds
        arguments(Duration.ofSeconds(-2, -250_000_000), -2.25), // Values that overflow double range → infinities
        arguments(Duration.ofSeconds(Long.MAX_VALUE), Double.POSITIVE_INFINITY), arguments(Duration.ofSeconds(Long.MIN_VALUE), Double.NEGATIVE_INFINITY), // Boundary nanosecond rounding
        arguments(Duration.ofSeconds(1, 999_999_999), 1.999999999), arguments(Duration.ofSeconds(-1, -999_999_999), -1.999999999), // Sub‑nanosecond precision loss (still representable as double)
        arguments(Duration.ofSeconds(0, 1), 1.0e-9), arguments(Duration.ofSeconds(0, -1), -1.0e-9), // Large magnitude but still finite
        arguments(Duration.ofSeconds(9_223_372_036_854_775_000L), 9.223372036854775E18), arguments(Duration.ofSeconds(-9_223_372_036_854_775_000L), -9.223372036854775E18), // Zero nanos with non‑zero seconds
        arguments(Duration.ofSeconds(7, 0), 7.0), arguments(Duration.ofSeconds(-7, 0), -7.0));
    }

    /**
     * Provides invalid inputs together with the expected exception type.
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(arguments("not a duration", ClassCastException.class), arguments(null, NullPointerException.class));
    }

    /**
     * Verify conversion of a zero‑length Duration.
     */
    @Test
    void toDouble_zeroDuration_returnsZero() {
        Duration duration = Duration.ZERO;
        double result = DurationConversions.toDouble(duration, mockConverter);
        assertEquals(0.0, result, 0.0, "Zero duration should convert to 0.0");
    }

    /**
     * Verify conversion of a positive duration that contains both seconds and nanoseconds.
     */
    @Test
    void toDouble_positiveDurationWithNanos_returnsExpectedDouble() {
        // 5 seconds + 500,000,000 nanos = 5.5 seconds
        Duration duration = Duration.ofSeconds(5, 500_000_000);
        double result = DurationConversions.toDouble(duration, mockConverter);
        assertEquals(5.5, result, 1e-12, "Duration of 5.5 seconds should convert to 5.5");
    }

    /**
     * Verify conversion of a negative duration (both seconds and nanos).
     */
    @Test
    void toDouble_negativeDurationWithNanos_returnsExpectedDouble() {
        // -3 seconds - 250,000,000 nanos = -3.25 seconds
        Duration duration = Duration.ofSeconds(-3, -250_000_000);
        double result = DurationConversions.toDouble(duration, mockConverter);
        assertEquals(-3.25, result, 1e-12, "Duration of -3.25 seconds should convert to -3.25");
    }

    /**
     * Verify conversion of a large duration that is still representable as a double.
     */
    @Test
    void toDouble_largeDuration_returnsFiniteDouble() {
        // 1,000,000 seconds (no nanos)
        Duration duration = Duration.ofSeconds(1_000_000);
        double result = DurationConversions.toDouble(duration, mockConverter);
        assertEquals(1_000_000.0, result, 1e-6, "Large duration should convert to its double representation");
    }

    /**
     * Verify that an object that is not a {@link Duration} results in a {@link ClassCastException}.
     * The method casts the input to Duration without checking, so the exception is expected.
     */
    @Test
    void toDouble_nonDurationObject_throwsClassCastException() {
        Object notADuration = "not a duration";
        assertThrows(ClassCastException.class, () -> DurationConversions.toDouble(notADuration, mockConverter), "Passing a non‑Duration object should throw ClassCastException");
    }

    /**
     * Parameterised test for all valid {@link Duration} inputs where the
     * {@code converter} argument is {@code null}.  The expected double value
     * is asserted with a tolerance appropriate for the magnitude of the value.
     */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("validDurationProvider")
    void toDouble_validDurations_returnsExpectedDouble(Duration duration, double expected) {
        double result = DurationConversions.toDouble(duration, null);
        if (Double.isInfinite(expected)) {
            assertTrue(Double.isInfinite(result) && Math.signum(result) == Math.signum(expected), () -> "Expected " + expected + " but got " + result);
        } else {
            // Use a relative tolerance for very large numbers, otherwise an absolute tolerance.
            double tolerance = Math.abs(expected) > 1e15 ? 1e9 : 1e-12;
            assertEquals(expected, result, tolerance, () -> "Duration " + duration + " should convert to " + expected);
        }
    }

    /**
     * Parameterised test for invalid inputs that should result in an exception.
     */
    @ParameterizedTest(name = "[{index}] {0} throws {1}")
    @MethodSource("invalidInputProvider")
    void toDouble_invalidInputs_throwsExpectedException(Object input, Class<? extends Throwable> exceptionClass) {
        assertThrows(exceptionClass, () -> DurationConversions.toDouble(input, null), () -> "Input " + input + " should throw " + exceptionClass.getSimpleName());
    }

    /**
     * Verify that the method truly ignores the {@link Converter} argument.
     * The same conversion is performed with a non‑null mock Converter.
     */
    @Test
    void toDouble_converterArgumentIsIgnored() {
        // 3.25 seconds
        Duration duration = Duration.ofSeconds(3, 250_000_000);
        double result = DurationConversions.toDouble(duration, mockConverter);
        assertEquals(3.25, result, 1e-12, "Method must ignore the supplied Converter");
    }
}
