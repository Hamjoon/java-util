package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toAtomicBoolean_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private Converter converter;

    /**
     * Converter that always returns {@code 1L} for any request for a {@code Long}
     * (or {@code long}) value.  All other conversion requests are delegated to the
     * real {@link Converter} implementation.
     */
    private static class MockConverter extends Converter {

        MockConverter() {
            super(new ConverterOptions());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object from, Class<T> targetClass) {
            if (targetClass == Long.class || targetClass == long.class) {
                return (T) Long.valueOf(1L);
            }
            return super.convert(from, targetClass);
        }
    }

    /**
     * Converter that always returns {@code 0L} for any request for a {@code Long}
     * (or {@code long}) value.
     */
    private static class MockZeroConverter extends Converter {

        MockZeroConverter() {
            super(new ConverterOptions());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object from, Class<T> targetClass) {
            if (targetClass == Long.class || targetClass == long.class) {
                return (T) Long.valueOf(0L);
            }
            return super.convert(from, targetClass);
        }
    }

    /**
     * Converter that throws {@link IllegalArgumentException} for any conversion
     * request.  Used to verify that exceptions from a custom converter are
     * propagated by {@code NumberConversions.toAtomicBoolean}.
     */
    private static class MockExceptionConverter extends Converter {

        MockExceptionConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object from, Class<T> targetClass) {
            throw new IllegalArgumentException("Mock conversion failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – this gives us the full set of built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// numeric zero / non‑zero
        Arguments.of(0L, null, false), Arguments.of(1, null, true), Arguments.of(-5, null, true), // strings
        Arguments.of("123", null, true), Arguments.of("0", null, false), // BigDecimal / BigInteger
        Arguments.of(new BigDecimal("0.0"), null, false), Arguments.of(new BigDecimal("2.5"), null, true), Arguments.of(new BigInteger("0"), null, false), Arguments.of(new BigInteger("-10"), null, true), // null input
        Arguments.of(null, null, false), // custom converter that always returns a non‑zero long
        Arguments.of(new Object(), new MockConverter(), true), // Boolean values
        Arguments.of(Boolean.TRUE, null, true), Arguments.of(Boolean.FALSE, null, false), // floating point zero / non‑zero
        Arguments.of(0.0d, null, false), Arguments.of(-0.0d, null, false), Arguments.of(3.14f, null, true), // Atomic wrappers
        Arguments.of(new AtomicInteger(0), null, false), Arguments.of(new AtomicInteger(7), null, true), Arguments.of(new AtomicLong(0L), null, false), Arguments.of(new AtomicLong(-123L), null, true), // primitive wrappers
        Arguments.of(new Byte((byte) 0), null, false), Arguments.of(new Short((short) 0), null, false), Arguments.of(new Integer(0), null, false), // extreme values
        Arguments.of(Long.MAX_VALUE, null, true), // custom converter that returns zero
        Arguments.of(new Object(), new MockZeroConverter(), false));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// unparsable strings
        Arguments.of("abc", null, NumberFormatException.class), Arguments.of("", null, NumberFormatException.class), // unsupported object without custom converter
        Arguments.of(new Object(), null, IllegalArgumentException.class), // NaN values
        Arguments.of(Double.NaN, null, IllegalArgumentException.class), Arguments.of(Float.NaN, null, IllegalArgumentException.class), // overflow cases
        Arguments.of(new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE), null, ArithmeticException.class), Arguments.of(new BigInteger("9223372036854775808"), null, ArithmeticException.class), // custom converter that throws IllegalArgumentException
        Arguments.of("123", new MockExceptionConverter(), IllegalArgumentException.class));
    }

    @Test
    void zeroIntegerShouldReturnFalse() {
        AtomicBoolean result = NumberConversions.toAtomicBoolean(0, converter);
        assertFalse(result.get(), "AtomicBoolean should be false for integer 0");
    }

    @Test
    void nonZeroIntegerShouldReturnTrue() {
        AtomicBoolean result = NumberConversions.toAtomicBoolean(42, converter);
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero integer");
    }

    @Test
    void zeroLongShouldReturnFalse() {
        AtomicBoolean result = NumberConversions.toAtomicBoolean(0L, converter);
        assertFalse(result.get(), "AtomicBoolean should be false for long 0");
    }

    @Test
    void nonZeroLongShouldReturnTrue() {
        AtomicBoolean result = NumberConversions.toAtomicBoolean(123L, converter);
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero long");
    }

    @Test
    void zeroStringShouldReturnFalse() {
        AtomicBoolean result = NumberConversions.toAtomicBoolean("0", converter);
        assertFalse(result.get(), "AtomicBoolean should be false for string \"0\"");
    }

    @Test
    void nonZeroStringShouldReturnTrue() {
        AtomicBoolean result = NumberConversions.toAtomicBoolean("7", converter);
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero numeric string");
    }

    @Test
    void booleanTrueShouldReturnTrue() {
        // Converter treats Boolean true as 1L internally.
        AtomicBoolean result = NumberConversions.toAtomicBoolean(Boolean.TRUE, converter);
        assertTrue(result.get(), "AtomicBoolean should be true for Boolean.TRUE");
    }

    @Test
    void booleanFalseShouldReturnFalse() {
        // Converter treats Boolean false as 0L internally.
        AtomicBoolean result = NumberConversions.toAtomicBoolean(Boolean.FALSE, converter);
        assertFalse(result.get(), "AtomicBoolean should be false for Boolean.FALSE");
    }

    @Test
    void nullShouldReturnFalse() {
        AtomicBoolean result = NumberConversions.toAtomicBoolean(null, converter);
        assertFalse(result.get(), "AtomicBoolean should be false when input is null");
    }

    @ParameterizedTest(name = "{index}: toAtomicBoolean({0}) → {2}")
    @MethodSource("validConversionProvider")
    void validConversions(Object from, Converter customConverter, boolean expected) {
        Converter used = customConverter != null ? customConverter : converter;
        AtomicBoolean result = NumberConversions.toAtomicBoolean(from, used);
        assertEquals(expected, result.get(), () -> "AtomicBoolean should be " + expected + " for input: " + from);
    }

    @ParameterizedTest(name = "{index}: toAtomicBoolean({0}) throws {2}")
    @MethodSource("exceptionConversionProvider")
    void exceptionConversions(Object from, Converter customConverter, Class<? extends Throwable> expectedException) {
        Converter used = customConverter != null ? customConverter : converter;
        assertThrows(expectedException, () -> NumberConversions.toAtomicBoolean(from, used), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
