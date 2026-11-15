package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLong_Test {

    /**
     * The Converter instance required by the method signature.
     *  It is not used by {@code StringConversions.toLong(Object, Converter)},
     *  but a non‑null reference keeps the API contract clear.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // A minimal, functional Converter – default options are sufficient.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> successfulConversionProvider() {
        Converter nonNullConverter = new Converter(new ConverterOptions());
        return Stream.of(// Empty string → 0L
        Arguments.of("", null, 0L), // Direct parse
        Arguments.of("12345", null, 12345L), // Boundary values
        Arguments.of("-9223372036854775808", null, Long.MIN_VALUE), Arguments.of("9223372036854775807", null, Long.MAX_VALUE), // Whitespace trimmed by fallback
        Arguments.of("  42  ", null, 42L), // Decimal string – fallback truncates
        Arguments.of("123.456", null, 123L), // Null input → 0L
        Arguments.of(null, null, 0L), // Explicit plus sign
        Arguments.of("+123", null, 123L), // Leading zeros
        Arguments.of("00123", null, 123L), // Negative decimal – truncates toward zero
        Arguments.of("-123.456", null, -123L), // String with only spaces – treated as empty
        Arguments.of("   ", null, 0L), // Non‑null converter does not affect outcome
        Arguments.of("42", nonNullConverter, 42L));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        Converter nonNullConverter = new Converter(new ConverterOptions());
        return Stream.of(// Value larger than Long.MAX_VALUE → IllegalArgumentException
        Arguments.of("9223372036854775808", null, IllegalArgumentException.class), // Value smaller than Long.MIN_VALUE → IllegalArgumentException
        Arguments.of("-9223372036854775809", null, IllegalArgumentException.class), // Non‑numeric string → IllegalArgumentException
        Arguments.of("notANumber", null, IllegalArgumentException.class), // Scientific notation not supported → IllegalArgumentException
        Arguments.of("1e3", null, IllegalArgumentException.class), // Alphanumeric string → IllegalArgumentException
        Arguments.of("123abc", null, IllegalArgumentException.class), // Passing a non‑String object (Integer) → ClassCastException
        Arguments.of(12345, null, ClassCastException.class), // "NaN" – Long.valueOf throws NumberFormatException, fallback also throws → NumberFormatException
        Arguments.of("NaN", null, NumberFormatException.class), // Underscore numeric literal – not accepted by Long.valueOf or BigDecimal → NumberFormatException
        Arguments.of("1_000", null, NumberFormatException.class), // Lone plus sign – invalid literal → NumberFormatException
        Arguments.of("+", null, NumberFormatException.class));
    }

    /**
     * Empty or whitespace strings should be interpreted as zero.
     */
    @Test
    void emptyStringReturnsZero() {
        Long result = StringConversions.toLong("", converter);
        assertEquals(0L, result);
    }

    /**
     * A well‑formed numeric string must be parsed directly by {@code Long.valueOf}.
     */
    @Test
    void validLongStringIsParsed() {
        Long result = StringConversions.toLong("1234567890", converter);
        assertEquals(1_234_567_890L, result);
    }

    /**
     * A numeric string that exceeds {@code Long.MAX_VALUE} must trigger the fallback
     *  path and ultimately throw {@code IllegalArgumentException}.
     */
    @Test
    void outOfRangeStringThrowsException() {
        // Long.MAX_VALUE + 1
        String tooBig = "9223372036854775808";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toLong(tooBig, converter));
        assertTrue(ex.getMessage().contains("not parseable as a long value"));
    }

    /**
     * A non‑numeric string that can be interpreted as a number via {@code BigDecimal}
     *  (e.g., a decimal value) should be converted to a {@code Long} by the private
     *  fallback method.  The expected behaviour is truncation toward zero.
     */
    @Test
    void decimalStringIsConvertedViaBigDecimal() {
        Long result = StringConversions.toLong("123.99", converter);
        // The private conversion truncates the fractional part.
        assertEquals(123L, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={2}")
    @MethodSource("successfulConversionProvider")
    void successfulConversions(Object from, Converter conv, Long expected) {
        Long result = StringConversions.toLong(from, conv);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("exceptionConversionProvider")
    void exceptionConversions(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringConversions.toLong(from, conv));
    }
}
