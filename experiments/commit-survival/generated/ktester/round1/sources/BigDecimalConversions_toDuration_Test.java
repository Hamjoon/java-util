package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toDuration_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; we mock it to avoid any external configuration.
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getConverterOverrides()).thenReturn(Collections.emptyMap());
        converter = new Converter(options);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Whole seconds
        Arguments.of(new BigDecimal("0"), Duration.ZERO, "Zero seconds should produce a zero Duration."), Arguments.of(new BigDecimal("42"), Duration.ofSeconds(42), "Positive whole number seconds should produce a Duration with the same number of seconds and no nanos."), Arguments.of(new BigDecimal("-17"), Duration.ofSeconds(-17), "Negative whole number seconds should produce a Duration with the same negative seconds and no nanos."), // Fractional seconds
        Arguments.of(new BigDecimal("1.5"), Duration.ofSeconds(1, 500_000_000), "Positive fractional seconds (1.5) should yield 1 second and 500,000,000 nanos."), Arguments.of(new BigDecimal("0.000000001"), Duration.ofSeconds(0, 1), "A tiny fractional part of one nanosecond should be correctly converted to 1 nano."), Arguments.of(new BigDecimal("-2.75"), Duration.ofSeconds(-2, -750_000_000), "Negative fractional seconds should keep the sign for both seconds and nanos; Duration normalises the negative nanos automatically."), // High precision & large values
        Arguments.of(new BigDecimal("123456789.123456789"), Duration.ofSeconds(123_456_789L, 123_456_789L), "A large number with many fractional digits should be truncated to seconds and the fractional part converted to nanos."), Arguments.of(new BigDecimal("9223372036854775807"), Duration.ofSeconds(Long.MAX_VALUE), "Maximum long value as seconds should be accepted without overflow."), // Fractional precision truncation
        Arguments.of(new BigDecimal("0.9999999999"), Duration.ofSeconds(0, 999_999_999), "A fractional value with more than nine decimal places is truncated after moving the decimal point nine places to the right; the extra digits are discarded."), Arguments.of(new BigDecimal("1.0000000005"), Duration.ofSeconds(1, 0), "Fractional part smaller than one nanosecond is truncated to zero nanoseconds; the result is exactly one whole second."), // Exponent notation & sub‑nanosecond values
        Arguments.of(new BigDecimal("1E-10"), Duration.ofSeconds(0, 0), "Scientific notation representing 0.1 nanoseconds is truncated to zero nanos."), Arguments.of(new BigDecimal("-1E-9"), Duration.ofSeconds(0, -1), "Negative one nanosecond expressed in exponent form should yield a duration of -1 nanosecond."), // Negative zero handling
        Arguments.of(new BigDecimal("-0"), Duration.ZERO, "BigDecimal representing negative zero is treated the same as zero; the resulting Duration is ZERO."), // Boundary values for long seconds
        Arguments.of(new BigDecimal(Long.MIN_VALUE), Duration.ofSeconds(Long.MIN_VALUE), "The minimum long value should be accepted as the seconds component without overflow."), // Long.MAX_VALUE + 1
        Arguments.// Long.MAX_VALUE + 1
        of(// Long.MAX_VALUE + 1
        new BigDecimal("9223372036854775808"), Duration.ofSeconds(Long.MAX_VALUE), "A value exceeding Long.MAX_VALUE is truncated by BigDecimal.longValue() to Long.MAX_VALUE; the method should return a Duration with that many seconds."), // Converter argument is ignored (non‑null instance)
        Arguments.of(new BigDecimal("2.25"), Duration.ofSeconds(2, 250_000_000), "Even when a non‑null Converter instance is supplied, the method does not use it; conversion proceeds solely from the BigDecimal value."), // Truncation of excess fractional digits
        Arguments.of(new BigDecimal("0.123456789123456789"), Duration.ofSeconds(0, 123_456_789), "Only the first nine digits after the decimal point are used to compute nanoseconds; the remaining digits are discarded."));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing null as the source object should result in a NullPointerException when the method attempts to call remainder on it."), Arguments.of(Integer.valueOf(10), ClassCastException.class, "Providing a non‑BigDecimal object (e.g., Integer) should cause a ClassCastException during the cast."), Arguments.of("not a BigDecimal", ClassCastException.class, "Providing a non‑BigDecimal object (e.g., a String) should cause a ClassCastException during the cast to BigDecimal."));
    }

    @Test
    void toDuration_wholeSeconds() {
        // given
        // 123 seconds, no fraction
        BigDecimal input = BigDecimal.valueOf(123L);
        // when
        Duration result = BigDecimalConversions.toDuration(input, converter);
        // then
        Duration expected = Duration.ofSeconds(123L, 0L);
        assertEquals(expected, result, "Duration should represent whole seconds correctly");
    }

    @Test
    void toDuration_fractionalSeconds() {
        // given
        // 123.456789123 seconds -> 123 seconds + 456,789,123 nanoseconds
        BigDecimal input = new BigDecimal("123.456789123");
        // when
        Duration result = BigDecimalConversions.toDuration(input, converter);
        // then
        Duration expected = Duration.ofSeconds(123L, 456_789_123L);
        assertEquals(expected, result, "Duration should correctly handle fractional seconds");
    }

    @Test
    void toDuration_zero() {
        // given
        BigDecimal input = BigDecimal.ZERO;
        // when
        Duration result = BigDecimalConversions.toDuration(input, converter);
        // then
        assertEquals(Duration.ZERO, result, "Zero input must produce Duration.ZERO");
    }

    @Test
    void toDuration_negativeFractionalSeconds() {
        // given
        // -5.5 seconds -> -5 seconds and -500,000,000 nanoseconds
        BigDecimal input = new BigDecimal("-5.5");
        // when
        Duration result = BigDecimalConversions.toDuration(input, converter);
        // then
        Duration expected = Duration.ofSeconds(-5L, -500_000_000L);
        assertEquals(expected, result, "Duration should correctly handle negative fractional seconds");
    }

    /* -------------------------------------------------------------
     *  Valid conversion tests (parameterised)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void toDuration_validConversions(BigDecimal input, Duration expected, String description) {
        // The converter argument is deliberately passed as {@code null} because the method ignores it.
        Duration result = BigDecimalConversions.toDuration(input, null);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Invalid conversion tests (parameterised)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("invalidConversionProvider")
    void toDuration_invalidInputs(Object input, Class<? extends Throwable> expectedException, String description) {
        // The method casts the first argument to BigDecimal, so passing a wrong type or null should fail.
        assertThrows(expectedException, () -> BigDecimalConversions.toDuration(input, null), description);
    }

    /* -------------------------------------------------------------
     *  Additional dedicated test for the “converter ignored” scenario
     * ------------------------------------------------------------- */
    @Test
    void toDuration_converterArgumentIgnored() {
        // Arrange: a dummy Converter that is *not* the shared static instance.
        Converter dummy = mock(Converter.class);
        BigDecimal input = new BigDecimal("2.25");
        // Act
        Duration result = BigDecimalConversions.toDuration(input, dummy);
        // Assert
        Duration expected = Duration.ofSeconds(2, 250_000_000);
        assertEquals(expected, result, "Method must ignore the supplied Converter and use only the BigDecimal value.");
    }
}
