package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_integerTypeToBigDecimal_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * A Converter mock that throws a RuntimeException when any conversion method is called.
     */
    @Mock
    private Converter throwingConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the ConverterOptions class provides a no‑arg constructor.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Provides a variety of integer‑type inputs together with the expected {@link BigDecimal}
     * result of {@code NumberConversions.integerTypeToBigDecimal}.
     */
    private static Stream<Arguments> integerInputs() {
        return Stream.of(// Primitive wrapper types
        Arguments.of(Integer.valueOf(0), BigDecimal.valueOf(0L)), Arguments.of(Integer.valueOf(42), BigDecimal.valueOf(42L)), Arguments.of(Long.valueOf(1234567890123L), BigDecimal.valueOf(1234567890123L)), Arguments.of(Short.valueOf((short) -7), BigDecimal.valueOf(-7L)), Arguments.of(Byte.valueOf((byte) 5), BigDecimal.valueOf(5L)), // Atomic types
        Arguments.of(new AtomicInteger(99), BigDecimal.valueOf(99L)), Arguments.of(new AtomicLong(-1000L), BigDecimal.valueOf(-1000L)), // BigInteger – should be converted via toLong (may truncate if out of range,
        // but for the values we test it fits)
        Arguments.of(BigInteger.valueOf(12345L), BigDecimal.valueOf(12345L)));
    }

    private static Stream<Arguments> normalIntegerInputs() {
        return Stream.of(// Simple integer wrappers
        Arguments.of(42, BigDecimal.valueOf(42L)), Arguments.of(Long.valueOf(1234567890123L), BigDecimal.valueOf(1234567890123L)), Arguments.of(Short.valueOf((short) 7), BigDecimal.valueOf(7L)), Arguments.of(Byte.valueOf((byte) -3), BigDecimal.valueOf(-3L)), // BigInteger that fits in long
        Arguments.of(new BigInteger("9876543210"), BigDecimal.valueOf(9876543210L)), // Atomic types
        Arguments.of(new AtomicInteger(55), BigDecimal.valueOf(55L)), Arguments.of(new AtomicLong(9999999999L), BigDecimal.valueOf(9999999999L)), // Numeric strings (parsed by toLong)
        Arguments.of("12345", BigDecimal.valueOf(12345L)), Arguments.of("-9876", BigDecimal.valueOf(-9876L)), // CharSequence implementation
        Arguments.of(new StringBuilder("6789"), BigDecimal.valueOf(6789L)));
    }

    private static Stream<Arguments> edgeCaseInputs() {
        return Stream.of(// Long boundaries
        Arguments.of(Long.MAX_VALUE, BigDecimal.valueOf(Long.MAX_VALUE)), Arguments.of(Long.MIN_VALUE, BigDecimal.valueOf(Long.MIN_VALUE)), // BigDecimal with fractional part – should be truncated by toLong
        Arguments.of(new BigDecimal("123.45"), BigDecimal.valueOf(123L)), // String with surrounding whitespace – toLong does NOT trim, expect NFE
        Arguments.of("   42   ", NumberFormatException.class));
    }

    private static Stream<Arguments> exceptionInputs() {
        return Stream.of(// Null source object
        Arguments.of(null, NullPointerException.class), // Unsupported type Object
        Arguments.of(new Object(), ClassCastException.class), // BigInteger that overflows long
        Arguments.of(new BigInteger(Long.toString(Long.MAX_VALUE).concat("0")), ArithmeticException.class), // Boolean value – not an integer type
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class), // Non‑numeric string
        Arguments.of("abc", NumberFormatException.class), // Numeric string exceeding Long.MAX_VALUE
        Arguments.of("9223372036854775808", NumberFormatException.class), // BigDecimal exceeding long range
        Arguments.of(new BigDecimal("9223372036854775808"), ArithmeticException.class), // NaN and infinities
        Arguments.of(Double.NaN, IllegalArgumentException.class), Arguments.of(Float.POSITIVE_INFINITY, IllegalArgumentException.class), // Date and LocalDate – not integer types
        Arguments.of(new java.util.Date(), ClassCastException.class), Arguments.of(java.time.LocalDate.now(), ClassCastException.class));
    }

    @ParameterizedTest
    @MethodSource("integerInputs")
    void testIntegerTypeToBigDecimal(Object input, BigDecimal expected) {
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(input, converter);
        assertEquals(expected, result, () -> "Failed conversion for input: " + input + " (class: " + input.getClass().getSimpleName() + ")");
    }

    @Test
    void testIntegerTypeToBigDecimal_NullInput() {
        assertThrows(NullPointerException.class, () -> NumberConversions.integerTypeToBigDecimal(null, converter), "Passing null as the source object should result in NPE (toLong cannot handle null).");
    }

    @ParameterizedTest(name = "[{index}] normal conversion: {0}")
    @MethodSource("normalIntegerInputs")
    void testIntegerTypeToBigDecimal_Normal(Object input, BigDecimal expected) {
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(input, converter);
        assertEquals(expected, result, () -> "Conversion failed for input: " + input + " (class: " + input.getClass().getSimpleName() + ")");
    }

    @ParameterizedTest(name = "[{index}] edge case conversion: {0}")
    @MethodSource("edgeCaseInputs")
    void testIntegerTypeToBigDecimal_EdgeCases(Object input, Object expected) {
        if (expected instanceof Class) {
            // Expect an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> NumberConversions.integerTypeToBigDecimal(input, converter), () -> "Expected exception " + exClass.getSimpleName() + " for input: " + input);
        } else {
            // Normal result
            BigDecimal result = NumberConversions.integerTypeToBigDecimal(input, converter);
            assertEquals(expected, result, () -> "Edge case conversion failed for input: " + input);
        }
    }

    @ParameterizedTest(name = "[{index}] exception case: {0}")
    @MethodSource("exceptionInputs")
    void testIntegerTypeToBigDecimal_Exceptions(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.integerTypeToBigDecimal(input, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + input);
    }

    @Test
    void testIntegerTypeToBigDecimal_ConverterThrowsRuntimeException() {
        // Configure the mock to throw a RuntimeException when any method is invoked.
        // Since NumberConversions delegates to NumberConversions.toLong, we mock that static
        // method via spy on the Converter if it were called – however, the real implementation
        // does not call any method on the Converter for integer types, so we simulate by
        // forcing the mock to be passed and expecting the method to propagate any unchecked
        // exception that might be thrown by a custom Converter implementation.
        // We'll make the mock throw when its toString (any call) is invoked just to ensure
        // the exception bubbles up.
        when(throwingConverter.toString()).thenThrow(new RuntimeException("Converter failure"));
        // The method itself does not use the converter for these types, but we still verify
        // that a RuntimeException from the converter (if used) propagates unchanged.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> NumberConversions.integerTypeToBigDecimal(new AtomicInteger(77), throwingConverter));
        assertEquals("Converter failure", ex.getMessage(), "Exception from converter should propagate unchanged.");
    }

    @Test
    void testIntegerTypeToBigDecimal_NullConverterAllowed() {
        // Passing null as the converter should not affect conversion for integer inputs.
        BigDecimal result = NumberConversions.integerTypeToBigDecimal(42, null);
        assertEquals(BigDecimal.valueOf(42L), result, "Conversion should succeed even when converter is null.");
    }
}
