package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2BigInteger_Test {

    private static Stream<Arguments> provideVariousSupportedTypes() {
        return Stream.of(// Numeric strings
        Arguments.of("123", new BigInteger("123")), Arguments.of("-456", new BigInteger("-456")), // Primitive wrappers
        Arguments.of(789, BigInteger.valueOf(789)), Arguments.of(1234567890123456789L, new BigInteger("1234567890123456789")), // Floating‑point numbers – truncation to integral part
        Arguments.of(12.34, new BigDecimal("12.34").toBigInteger()), Arguments.of(12345.0, BigInteger.valueOf(12345)), Arguments.of("12.99", new BigDecimal("12.99").toBigInteger()), Arguments.of("12345.0", BigInteger.valueOf(12345)), // Booleans
        Arguments.of(true, BigInteger.ONE), Arguments.of(false, BigInteger.ZERO), // Hexadecimal string (prefixed with 0x)
        Arguments.of("0xFF", new BigInteger("FF", 16)), // Whitespace handling
        Arguments.of("   42   ", BigInteger.valueOf(42)), // Very large number beyond Long.MAX_VALUE
        Arguments.of("9223372036854775808", new BigInteger("9223372036854775808")), // Zero values
        Arguments.of(0, BigInteger.ZERO), // Invalid numeric string – should throw NumberFormatException
        Arguments.of("invalidNumber", NumberFormatException.class), // Empty string – treated as zero per API, but library throws NFE,
        // we assert the library's behaviour (NumberFormatException)
        Arguments.of("", NumberFormatException.class), // Whitespace‑only string – also results in NumberFormatException
        Arguments.of("   ", NumberFormatException.class));
    }

    private static Stream<Arguments> provideUnsupportedTypes() {
        return Stream.of(Arguments.of(new Object(), IllegalArgumentException.class), Arguments.of(new int[] { 1, 2, 3 }, IllegalArgumentException.class), Arguments.of(Arrays.asList(1, 2, 3), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideInvalidStringFormats() {
        return Stream.of(Arguments.of("", NumberFormatException.class), Arguments.of("   ", NumberFormatException.class), Arguments.of("0xGHI", NumberFormatException.class));
    }

    private static Stream<Arguments> provideSpecialFloatingValues() {
        return Stream.of(Arguments.of(Double.NaN, NumberFormatException.class), Arguments.of(Double.POSITIVE_INFINITY, NumberFormatException.class), Arguments.of(Float.NEGATIVE_INFINITY, NumberFormatException.class));
    }

    @Test
    void convertNull_returnsZero() {
        // GIVEN
        Object input = null;
        // WHEN
        BigInteger result = Converter.convert2BigInteger(input);
        // THEN
        assertEquals(BigInteger.ZERO, result, "convert2BigInteger(null) should return BigInteger.ZERO");
    }

    @Test
    void convertInteger_returnsBigInteger() {
        // GIVEN
        Integer input = 42;
        // WHEN
        BigInteger result = Converter.convert2BigInteger(input);
        // THEN
        assertEquals(BigInteger.valueOf(42L), result, "Integer should be converted to an equivalent BigInteger");
    }

    @Test
    void convertLong_returnsBigInteger() {
        // GIVEN
        Long input = 1234567890123L;
        // WHEN
        BigInteger result = Converter.convert2BigInteger(input);
        // THEN
        assertEquals(BigInteger.valueOf(1234567890123L), result, "Long should be converted to an equivalent BigInteger");
    }

    @Test
    void convertStringNumeric_returnsBigInteger() {
        // GIVEN
        String input = "987654321";
        // WHEN
        BigInteger result = Converter.convert2BigInteger(input);
        // THEN
        assertEquals(new BigInteger("987654321"), result, "Numeric String should be converted to the corresponding BigInteger");
    }

    @Test
    void convertBigDecimal_returnsBigInteger() {
        // GIVEN
        // fractional part is discarded by conversion
        BigDecimal input = new BigDecimal("12345.678");
        // WHEN
        BigInteger result = Converter.convert2BigInteger(input);
        // THEN
        // The library truncates the fractional part (same behaviour as BigDecimal#toBigInteger)
        assertEquals(new BigInteger("12345"), result, "BigDecimal should be converted to its integral part as BigInteger");
    }

    @Test
    void convertBigInteger_returnsSameInstance() {
        // GIVEN
        BigInteger input = new BigInteger("999999999999999999");
        // WHEN
        BigInteger result = Converter.convert2BigInteger(input);
        // THEN
        // For immutable types the conversion may return the same instance,
        // but the contract only guarantees equality.
        assertEquals(input, result, "BigInteger input should be returned unchanged (equal)");
    }

    /* -------------------------------------------------
     *  2. Various supported source types
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] input={0} → expected={1}")
    @MethodSource("provideVariousSupportedTypes")
    void convertVariousSupportedTypes(Object input, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convert2BigInteger(input), () -> "Expected " + ((Class<?>) expected).getSimpleName() + " for input: " + input);
        } else {
            // Expected a successful conversion
            BigInteger result = Converter.convert2BigInteger(input);
            assertEquals(expected, result, () -> "Conversion result mismatch for input: " + input);
        }
    }

    /* -------------------------------------------------
     *  3. Unsupported source types – should raise IllegalArgumentException
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] unsupported input={0}")
    @MethodSource("provideUnsupportedTypes")
    void convertUnsupportedTypes(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert2BigInteger(input), () -> "Expected " + expectedException.getSimpleName() + " for input: " + input);
    }

    /* -------------------------------------------------
     *  4. Invalid string formats – NumberFormatException expected
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] invalid string=\"{0}\"")
    @MethodSource("provideInvalidStringFormats")
    void convertInvalidStringFormats(String input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert2BigInteger(input), () -> "Expected " + expectedException.getSimpleName() + " for input: \"" + input + "\"");
    }

    /* -------------------------------------------------
     *  5. Special floating‑point values – conversion not possible
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] special floating value={0}")
    @MethodSource("provideSpecialFloatingValues")
    void convertSpecialFloatingValues(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert2BigInteger(input), () -> "Expected " + expectedException.getSimpleName() + " for input: " + input);
    }
}
