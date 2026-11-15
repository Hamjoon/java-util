package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toLocalTime_Test {

    /**
     * The Converter is not used by {@code DoubleConversions.toLocalTime}, but the method
     * signature requires it.  A mock is sufficient.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Provides valid inputs together with the expected {@link LocalTime}
     * expressed as an ISO‑8601 string (e.g. {@code "12:00"} or
     * {@code "00:00:01.500"}).  The expected string is parsed with
     * {@link LocalTime#parse(CharSequence)} which supports up to nanosecond
     * precision.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> validConversionProvider() {
        return Stream.of(// Zero seconds → midnight
        org.junit.jupiter.params.provider.Arguments.of(0.0, "00:00"), // 1.5 seconds → 1 second + 500 ms
        org.junit.jupiter.params.provider.Arguments.of(1.5, "00:00:01.500"), // Maximum allowed value (just before next day)
        org.junit.jupiter.params.provider.Arguments.of(86_399.999_999_999, "23:59:59.999999999"), // Exactly half a day → 12:00
        org.junit.jupiter.params.provider.Arguments.of(43_200.0, "12:00"), // One nanosecond expressed as seconds
        org.junit.jupiter.params.provider.Arguments.of(1e-9, "00:00:00.000000001"), // Just before one‑hour mark
        org.junit.jupiter.params.provider.Arguments.of(3_599.999_999_999, "00:59:59.999999999"), // NaN is treated as 0.0 by the implementation (casting to long yields 0)
        org.junit.jupiter.params.provider.Arguments.of(Double.NaN, "00:00"));
    }

    /**
     * Provides inputs that are expected to cause an exception.
     * The third element is the {@link Class} of the expected exception.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidConversionProvider() {
        return Stream.of(// Negative seconds
        org.junit.jupiter.params.provider.Arguments.of(-0.0001, IllegalArgumentException.class), // Exactly 24 hours – out of range
        org.junit.jupiter.params.provider.Arguments.of(86_400.0, IllegalArgumentException.class), // Positive infinity – exceeds max nanoseconds
        org.junit.jupiter.params.provider.Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class), // Null input – unboxing to double triggers NPE
        org.junit.jupiter.params.provider.Arguments.of(null, NullPointerException.class), // Wrong type (Integer) – cast to double fails
        org.junit.jupiter.params.provider.Arguments.of(Integer.valueOf(123), ClassCastException.class));
    }

    @Test
    @DisplayName("toLocalTime converts 0 seconds to midnight")
    void toLocalTime_midnight() {
        double seconds = 0.0;
        LocalTime result = DoubleConversions.toLocalTime(seconds, mockConverter);
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    @DisplayName("toLocalTime converts 43200 seconds (12:00) correctly")
    void toLocalTime_noon() {
        // 12 hours * 3600
        double seconds = 43_200.0;
        LocalTime result = DoubleConversions.toLocalTime(seconds, mockConverter);
        assertEquals(LocalTime.NOON, result);
    }

    @Test
    @DisplayName("toLocalTime converts a fractional second value correctly")
    void toLocalTime_fractionalSeconds() {
        // 1 second + 234 567 890 nanoseconds
        double seconds = 1.23456789;
        LocalTime expected = LocalTime.ofNanoOfDay((long) (seconds * 1_000_000_000L));
        LocalTime result = DoubleConversions.toLocalTime(seconds, mockConverter);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("toLocalTime converts the maximum valid value (23:59:59.999999999)")
    void toLocalTime_maximumValid() {
        // one nanosecond shy of 24h
        double seconds = 86_399.999_999_999;
        LocalTime expected = LocalTime.ofNanoOfDay((long) (seconds * 1_000_000_000L));
        LocalTime result = DoubleConversions.toLocalTime(seconds, mockConverter);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("toLocalTime throws IllegalArgumentException for negative seconds")
    void toLocalTime_negativeThrows() {
        double seconds = -0.1;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalTime(seconds, mockConverter));
        assertTrue(ex.getMessage().contains("Input value [" + seconds + "]"));
    }

    @Test
    @DisplayName("toLocalTime throws IllegalArgumentException for seconds > 86399.999999999")
    void toLocalTime_tooLargeThrows() {
        // exactly 24 hours, out of range
        double seconds = 86_400.0;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalTime(seconds, mockConverter));
        assertTrue(ex.getMessage().contains("Input value [" + seconds + "]"));
    }

    @ParameterizedTest(name = "[{index}] seconds={0} → expected={1}")
    @MethodSource("validConversionProvider")
    @DisplayName("Valid conversions from seconds to LocalTime")
    void toLocalTime_validConversions(double seconds, String expectedIso) {
        // Act
        LocalTime result = DoubleConversions.toLocalTime(seconds, mockConverter);
        // Assert – parse the expected ISO‑8601 string and compare.
        LocalTime expected = LocalTime.parse(expectedIso);
        assertEquals(expected, result, () -> "Failed for input seconds=" + seconds);
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("invalidConversionProvider")
    @DisplayName("Invalid conversions should throw appropriate exceptions")
    void toLocalTime_invalidConversions(Object from, Class<? extends Throwable> expectedException) {
        // Act & Assert
        Throwable thrown = assertThrows(expectedException, () -> DoubleConversions.toLocalTime(from, mockConverter));
        // For IllegalArgumentException we also verify that the message contains the offending value.
        if (thrown instanceof IllegalArgumentException) {
            assertTrue(thrown.getMessage().contains(String.valueOf(from)), "Exception message should contain the invalid input value");
        }
    }

    @Test
    @DisplayName("Fractional seconds with many decimal places are handled correctly")
    void toLocalTime_fractionalSecondsHighPrecision() {
        // 1 second + 234 567 891 ns
        double seconds = 1.234567891;
        LocalTime result = DoubleConversions.toLocalTime(seconds, mockConverter);
        // Expected nanoseconds = seconds * 1_000_000_000, truncated to long
        long expectedNanos = (long) (seconds * 1_000_000_000L);
        LocalTime expected = LocalTime.ofNanoOfDay(expectedNanos);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Maximum valid nanosecond value (23:59:59.999999999) works")
    void toLocalTime_maxNanosecond() {
        double seconds = 86_399.999_999_999;
        LocalTime result = DoubleConversions.toLocalTime(seconds, mockConverter);
        assertEquals(LocalTime.MAX, result, "The maximum representable LocalTime should be returned for the highest valid input");
    }
}
