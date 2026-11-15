package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.ConversionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2byte_Test {

    /**
     * Provides arguments for all successful conversions.
     *
     * @return a stream of {@link Arguments} where each argument consists of:
     *         - the input object
     *         - the expected byte value after conversion
     */
    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// Null Input (already covered, but kept for completeness)
        Arguments.of(null, (byte) 0), // Numeric Conversions
        // within range
        Arguments.of(127, (byte) 127), // overflow
        Arguments.of(128, (byte) -128), // negative
        Arguments.of(-5, (byte) -5), // double truncation
        Arguments.of(12.9, (byte) 12), // numeric string
        Arguments.of("20", (byte) 20), // Byte wrapper
        Arguments.of(Byte.valueOf((byte) 7), (byte) 7), // BigDecimal truncation
        Arguments.of(new BigDecimal("10.5"), (byte) 10), // Boolean Conversions
        Arguments.of(Boolean.TRUE, (byte) 1), Arguments.of(Boolean.FALSE, (byte) 0), // Character Conversions
        Arguments.of('A', (byte) 65), Arguments.of('z', (byte) 122), // Primitive Wrapper Conversions
        // 200 -> -56 (overflow)
        Arguments.of(Short.valueOf((short) 200), (byte) -56), // 300 -> 44
        Arguments.of(Long.valueOf(300L), (byte) 44), // truncation
        Arguments.of(Float.valueOf(45.9f), (byte) 45), // BigInteger Conversions
        Arguments.of(new BigInteger("123"), (byte) 123), Arguments.of(new BigInteger("256"), (byte) 0), // Atomic Types
        Arguments.of(new AtomicInteger(127), (byte) 127), Arguments.of(new AtomicLong(130L), (byte) -126), // String Edge Cases
        // trimmed
        Arguments.of("  42 ", (byte) 42), // plus sign
        Arguments.of("+15", (byte) 15), // negative sign
        Arguments.of("-8", (byte) -8), // Boundary Values
        Arguments.of(-128, (byte) -128), Arguments.of(127, (byte) 127), // Floating‑Point Edge Cases
        Arguments.of(-12.7, (byte) -12), // 255 -> -1 after overflow
        Arguments.of(255.999, (byte) -1));
    }

    /**
     * Provides arguments for all conversions that must fail with a
     * {@link ConversionException} (or any {@link RuntimeException} if the
     * specific exception type is not available at runtime).
     *
     * @return a stream of {@link Arguments} where each argument is the input
     *         object that should trigger a conversion failure.
     */
    private static Stream<Arguments> failingConversions() {
        return Stream.of(// Invalid Conversions
        Arguments.of(new Object()), Arguments.of("abc"), // Hexadecimal notation (unsupported)
        Arguments.of("0x10"), // Empty string
        Arguments.of(""), // Scientific notation strings
        Arguments.of("1e2"), Arguments.of("2.5E-1"), // Unsupported object types
        Arguments.of(new Date()), Arguments.of(LocalDate.now()), Arguments.of(UUID.randomUUID()), Arguments.of(new int[] { 1, 2, 3 }), Arguments.of(List.of(1, 2)), // NaN / Infinity
        Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Double.NEGATIVE_INFINITY), Arguments.of(Float.NaN), Arguments.of(Float.POSITIVE_INFINITY));
    }

    @Test
    void convert2byte_givenNull_returnsZero() {
        // given
        Object input = null;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 0, result, "Null input should convert to 0");
    }

    @Test
    void convert2byte_givenByteReturnsSameValue() {
        // given
        Byte input = (byte) 42;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 42, result);
    }

    @Test
    void convert2byte_givenNumberReturnsByteValue() {
        // given
        // 123 fits in a byte (value 123)
        Integer input = 123;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 123, result);
    }

    @Test
    void convert2byte_givenStringNumberReturnsParsedByte() {
        // given
        String input = "7";
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 7, result);
    }

    @Test
    void convert2byte_givenStringNonNumberThrowsException() {
        // given
        String input = "not-a-number";
        // when / then
        assertThrows(RuntimeException.class, () -> Converter.convert2byte(input), "Non‑numeric string should cause a conversion exception");
    }

    @Test
    void convert2byte_givenBooleanTrueReturnsOne() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 1, result);
    }

    @Test
    void convert2byte_givenBooleanFalseReturnsZero() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 0, result);
    }

    @Test
    void convert2byte_givenCharReturnsByteOfCharCode() {
        // given
        // ASCII 65
        Character input = 'A';
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 65, result);
    }

    @ParameterizedTest(name = "[{index}] convert2byte({0}) => {1}")
    @MethodSource("successfulConversions")
    void convert2byte_successfulScenarios(Object input, byte expected) {
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals(expected, result, () -> "Expected conversion of " + Objects.toString(input) + " to be " + expected);
    }

    @ParameterizedTest(name = "[{index}] convert2byte({0}) throws ConversionException")
    @MethodSource("failingConversions")
    void convert2byte_failingScenarios(Object input) {
        // when / then
        assertThrows(RuntimeException.class, () -> Converter.convert2byte(input), () -> "Conversion of " + Objects.toString(input) + " should have failed");
    }
}
