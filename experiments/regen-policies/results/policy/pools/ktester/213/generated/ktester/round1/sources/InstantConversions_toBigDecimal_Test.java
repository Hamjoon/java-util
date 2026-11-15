package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toBigDecimal_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is not used by the method under test, but a valid instance is required.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Positive epoch second, zero nanos
        Arguments.of(Instant.ofEpochSecond(12345, 0), null, new BigDecimal("12345"), "Convert a positive epoch second with zero nanos; result should be exact integer value."), // Positive epoch second, half‑second nanos
        Arguments.of(Instant.ofEpochSecond(12345, 500_000_000), null, new BigDecimal("12345.5"), "Convert a positive epoch second with half‑second nanos; result should contain a fractional part .5."), // Negative epoch second, zero nanos
        Arguments.of(Instant.ofEpochSecond(-12345, 0), null, new BigDecimal("-12345"), "Convert a negative epoch second with zero nanos; result should be the negative integer."), // Negative epoch second, positive nanos (Instant normalises)
        Arguments.of(Instant.ofEpochSecond(-12345, 250_000_000), null, new BigDecimal("-12344.75"), "Convert a negative epoch second with positive nanos; Instant semantics add the nanos to the seconds, yielding -12344.75."), // Zero epoch
        Arguments.of(Instant.ofEpochSecond(0, 0), null, new BigDecimal("0"), "Convert the epoch (zero seconds, zero nanos); result should be zero."));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// Single nanosecond
        Arguments.of(Instant.ofEpochSecond(0, 1), null, new BigDecimal("0.000000001"), "Zero epoch seconds with a single nanosecond; verifies sub‑nanosecond precision handling."), // Maximum positive epoch second without nanos
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE, 0), null, new BigDecimal("9223372036854775807"), "Maximum positive epoch second without nanos; ensures the conversion can represent the largest long value exactly."), // Maximum positive epoch second with highest nanos
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999), null, new BigDecimal("9223372036854775807.999999999"), "Maximum positive epoch second with the highest possible nanosecond fraction; tests handling of the fractional part at the extreme range."), // Instant.MAX constant (same as previous case)
        Arguments.of(Instant.MAX, null, new BigDecimal("9223372036854775807.999999999"), "Conversion of the library constant Instant.MAX; should match the value of Long.MAX_VALUE seconds plus 0.999999999 fraction."), // Instant.MIN constant (no fractional part after normalization)
        Arguments.of(Instant.MIN, null, new BigDecimal("-9223372036854775808"), "Conversion of the library constant Instant.MIN; verifies that the most negative instant is handled (no fractional part after normalization)."), // Negative second with max positive nanos → tiny negative decimal
        Arguments.of(Instant.ofEpochSecond(-1, 999_999_999), null, new BigDecimal("-0.000000001"), "Negative epoch second with maximum positive nanos; demonstrates Instant's normalization that yields a tiny negative decimal."), // Non‑null converter should be ignored
        Arguments.of(Instant.ofEpochSecond(0, 500_000_000), new Converter(new ConverterOptions()), new BigDecimal("0.5"), "Converter argument is non‑null; the method should ignore it and still produce the correct decimal value."));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null source
        Arguments.of(null, null, NullPointerException.class, "Passing a null 'from' argument should result in a NullPointerException because the method casts it to Instant."), // String instead of Instant
        Arguments.of("2020-01-01T00:00:00Z", null, ClassCastException.class, "Providing an object that is not an Instant (e.g., a String) should cause a ClassCastException during the cast."), // Generic Object
        Arguments.of(new Object(), null, ClassCastException.class, "Supplying a generic Object instead of an Instant should also trigger a ClassCastException."));
    }

    @Test
    void toBigDecimal_zeroInstant_returnsZero() {
        Instant instant = Instant.ofEpochSecond(0, 0);
        BigDecimal result = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void toBigDecimal_positiveInstant_returnsExpectedBigDecimal() {
        // 1 second + 500,000,000 nanos = 1.5 seconds
        Instant instant = Instant.ofEpochSecond(1, 500_000_000);
        BigDecimal expected = new BigDecimal("1.5");
        BigDecimal result = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void toBigDecimal_negativeInstant_returnsExpectedBigDecimal() {
        // -2 seconds + 250,000,000 nanos = -1.75 seconds
        Instant instant = Instant.ofEpochSecond(-2, 250_000_000);
        BigDecimal expected = new BigDecimal("-1.75");
        BigDecimal result = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void toBigDecimal_invalidSource_throwsClassCastException() {
        Object notAnInstant = "2020-01-01T00:00:00Z";
        assertThrows(ClassCastException.class, () -> InstantConversions.toBigDecimal(notAnInstant, converter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_validConversions(Instant from, Converter conv, BigDecimal expected, String description) {
        // Act
        BigDecimal result = InstantConversions.toBigDecimal(from, conv);
        // Assert – compareTo is used to ignore scale differences (e.g., 1.0 vs 1)
        assertEquals(0, expected.compareTo(result), () -> "Failed: " + description + " – expected " + expected + " but got " + result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("edgeCaseProvider")
    void toBigDecimal_edgeCases(Instant from, Converter conv, BigDecimal expected, String description) {
        // Act
        BigDecimal result = InstantConversions.toBigDecimal(from, conv);
        // Assert
        assertEquals(0, expected.compareTo(result), () -> "Failed: " + description + " – expected " + expected + " but got " + result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputProvider")
    void toBigDecimal_invalidInputs(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // Act & Assert
        assertThrows(expectedException, () -> InstantConversions.toBigDecimal(from, conv), () -> "Failed: " + description + " – expected exception " + expectedException.getSimpleName());
    }
}
