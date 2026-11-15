package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_integerTypeToBigDecimal_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Converter with default options – the conversion logic is exercised
        // by the NumberConversions class itself.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Zero integer
        Arguments.of(Integer.valueOf(0), BigDecimal.valueOf(0L), "Zero integer should be converted to a BigDecimal representing 0."), // Positive Integer
        Arguments.of(Integer.valueOf(42), BigDecimal.valueOf(42L), "Positive Integer should be converted to an equal BigDecimal."), // Negative Long
        Arguments.of(Long.valueOf(-123456789L), BigDecimal.valueOf(-123456789L), "Negative Long value should be converted preserving sign."), // Short
        Arguments.of(Short.valueOf((short) 7), BigDecimal.valueOf(7L), "Short value should be widened to long then to BigDecimal."), // Byte
        Arguments.of(Byte.valueOf((byte) 5), BigDecimal.valueOf(5L), "Byte value should be converted correctly."), // BigInteger (within long range)
        Arguments.of(new BigInteger("12345678901234567890"), new BigDecimal("12345678901234567890"), "BigInteger should be converted to an equivalent BigDecimal."), // String containing a valid integer
        Arguments.of("9876543210", BigDecimal.valueOf(9876543210L), "String containing a valid integer representation should be parsed and converted."), // String with surrounding whitespace
        Arguments.of("   123   ", BigDecimal.valueOf(123L), "String with surrounding whitespace should be trimmed and parsed correctly to a BigDecimal."), // AtomicInteger
        Arguments.of(new AtomicInteger(999), BigDecimal.valueOf(999L), "AtomicInteger should be converted by extracting its int value and then to a BigDecimal."), // AtomicLong
        Arguments.of(new AtomicLong(-555L), BigDecimal.valueOf(-555L), "AtomicLong should be converted by extracting its long value and then to a BigDecimal."), // Long.MAX_VALUE
        Arguments.of(Long.MAX_VALUE, BigDecimal.valueOf(Long.MAX_VALUE), "Maximum long value should be represented exactly as a BigDecimal."), // Long.MIN_VALUE
        Arguments.of(Long.MIN_VALUE, BigDecimal.valueOf(Long.MIN_VALUE), "Minimum long value should be represented exactly as a BigDecimal."), // Double (truncated to long part)
        Arguments.of(Double.valueOf(123.99), BigDecimal.valueOf(123L), "A floating‑point Double is truncated to its long part before conversion, yielding a BigDecimal of the integral component."), // Using a non‑null (mock) Converter that simply returns the original argument
        Arguments.of(Integer.valueOf(10), BigDecimal.valueOf(10L), "Providing a non‑null Converter that does not alter the value should still produce the correct BigDecimal."));
    }

    private static Stream<Arguments> exceptionalProvider() {
        // Mock converter that simply returns the input argument for any method call.
        Converter mockConverter = mock(Converter.class, invocation -> invocation.getArgument(0));
        return Stream.of(// Null source object – toLong cannot handle null, expect NPE.
        Arguments.of(null, null, NullPointerException.class, "Passing a null source object should result in a NullPointerException because toLong cannot handle null."), // Empty string – NumberFormatException.
        Arguments.of("", null, NumberFormatException.class, "An empty string cannot be parsed as a number and should cause a NumberFormatException."), // Hexadecimal string – NumberFormatException.
        Arguments.of("0xFF", null, NumberFormatException.class, "Hexadecimal string literals are not recognized by the default parser and should raise a NumberFormatException."), // BigInteger larger than Long.MAX_VALUE – ArithmeticException.
        Arguments.of(new BigInteger("9223372036854775808"), null, ArithmeticException.class, "BigInteger value larger than Long.MAX_VALUE cannot be converted to long; an ArithmeticException is expected."), // Unsupported object type – ClassCastException.
        Arguments.of(new Object(), null, ClassCastException.class, "Unsupported object types should cause a ClassCastException when toLong attempts to interpret them as a number."), // Mock converter case – should still succeed, but we include it here to demonstrate that a non‑null converter does not break conversion.
        Arguments.of(Integer.valueOf(10), mockConverter, null, "Providing a non‑null mock Converter that simply returns the original argument should still produce the correct BigDecimal."));
    }

    @Test
    void integerTypeToBigDecimal_fromInteger_returnsCorrectValue() {
        Integer source = 42;
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(42L), result);
    }

    @Test
    void integerTypeToBigDecimal_fromLong_returnsCorrectValue() {
        Long source = 1234567890123L;
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(1234567890123L), result);
    }

    @Test
    void integerTypeToBigDecimal_fromShort_returnsCorrectValue() {
        Short source = 7;
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(7L), result);
    }

    @Test
    void integerTypeToBigDecimal_fromByte_returnsCorrectValue() {
        Byte source = 5;
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(5L), result);
    }

    @Test
    void integerTypeToBigDecimal_fromAtomicInteger_returnsCorrectValue() {
        AtomicInteger source = new AtomicInteger(99);
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(99L), result);
    }

    @Test
    void integerTypeToBigDecimal_fromAtomicLong_returnsCorrectValue() {
        AtomicLong source = new AtomicLong(1_000_000L);
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(1_000_000L), result);
    }

    @Test
    void integerTypeToBigDecimal_fromNull_returnsZero() {
        // According to Converter contract, null numeric conversion yields 0.
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(null, converter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void integerTypeToBigDecimal_validConversions(Object from, BigDecimal expected, String description) {
        // The JSON test data uses a null converter to indicate “default” behaviour.
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(from, null);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionalProvider")
    void integerTypeToBigDecimal_exceptions(Object from, Converter customConverter, Class<? extends Throwable> expectedException, String description) {
        // For the mock‑converter case we need a specially prepared mock.
        Converter usedConverter = customConverter != null ? customConverter : null;
        assertThrows(expectedException, () -> NumberConversions.integerTypeToBigDecimal(from, usedConverter), description);
    }
}
