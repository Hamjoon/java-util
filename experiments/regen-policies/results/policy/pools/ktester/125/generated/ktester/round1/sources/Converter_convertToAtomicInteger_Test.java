package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class Converter_convertToAtomicInteger_Test {

    class Dummy {
    }

    private static Stream<Arguments> successCases() {
        return Stream.of(// basic type conversions
        Arguments.of(Integer.valueOf(123), 123), Arguments.of("456", 456), Arguments.of(new AtomicInteger(789), 789), // numeric type conversions
        Arguments.of(123L, 123), Arguments.of((short) 45, 45), Arguments.of((byte) 7, 7), // double truncates
        Arguments.of(12.99, 12), // float truncates
        Arguments.of(9.5f, 9), Arguments.of(new BigInteger("200"), 200), Arguments.of(new BigDecimal("300.75"), 300), // edge numeric values within int range
        Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE), Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE), // special and supported CharSequence types
        Arguments.of(CharBuffer.wrap("123"), 123), // whitespace handling
        Arguments.of("  42  ", 42));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// invalid numeric string
        Arguments.of("not a number", NumberFormatException.class), // overflow / out‑of‑range values
        Arguments.of(Long.MAX_VALUE, NumberFormatException.class), // > Integer.MAX_VALUE
        Arguments.of("2147483648", NumberFormatException.class), // < Integer.MIN_VALUE
        Arguments.of("-2147483649", NumberFormatException.class), // unsupported and special types
        // per JSON spec
        Arguments.of(true, NumberFormatException.class), // character literal
        Arguments.of('5', NumberFormatException.class), Arguments.of(new Object(), IllegalArgumentException.class), // empty string
        Arguments.of("", NumberFormatException.class), // special numeric edge cases
        Arguments.of(Double.NaN, NumberFormatException.class), Arguments.of(Double.POSITIVE_INFINITY, NumberFormatException.class), Arguments.of(Double.NEGATIVE_INFINITY, NumberFormatException.class), Arguments.of(Float.NaN, NumberFormatException.class), Arguments.of(Float.POSITIVE_INFINITY, NumberFormatException.class), Arguments.of(new BigInteger("2147483648"), NumberFormatException.class), Arguments.of(new BigInteger("-2147483649"), NumberFormatException.class), Arguments.of(new BigDecimal("9223372036854775808"), NumberFormatException.class));
    }

    @Test
    void convertNullReturnsNull() {
        AtomicInteger result = Converter.convertToAtomicInteger(null);
        assertNull(result, "Converting null should return null for AtomicInteger");
    }

    @Test
    void convertAtomicIntegerReturnsSameValue() {
        AtomicInteger source = new AtomicInteger(42);
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        assertNotNull(result);
        assertEquals(42, result.get());
        // The converter creates a new instance; ensure it is not the same reference
        assertNotSame(source, result);
    }

    @Test
    void convertStringNumber() {
        AtomicInteger result = Converter.convertToAtomicInteger("123");
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    void convertNumberTypes() {
        assertEquals(7, Converter.convertToAtomicInteger(7L).get());
        assertEquals(7, Converter.convertToAtomicInteger(7).get());
        assertEquals(7, Converter.convertToAtomicInteger(7.0f).get());
        assertEquals(7, Converter.convertToAtomicInteger(7.0d).get());
        assertEquals(7, Converter.convertToAtomicInteger((short) 7).get());
        assertEquals(7, Converter.convertToAtomicInteger((byte) 7).get());
        assertEquals(7, Converter.convertToAtomicInteger(new java.math.BigInteger("7")).get());
        assertEquals(7, Converter.convertToAtomicInteger(new java.math.BigDecimal("7")).get());
    }

    @Test
    void convertBooleanTrueFalse() {
        assertEquals(1, Converter.convertToAtomicInteger(true).get());
        assertEquals(0, Converter.convertToAtomicInteger(false).get());
    }

    @Test
    void convertUnsupportedTypeThrowsException() {
        // An example of a type that has no direct conversion to AtomicInteger
        class Dummy {
        }
        assertThrows(RuntimeException.class, () -> Converter.convertToAtomicInteger(new Dummy()));
    }

    @ParameterizedTest(name = "[{index}] convertToAtomicInteger({0}) → {1}")
    @MethodSource("successCases")
    void convertToAtomicInteger_success(Object input, int expected) {
        AtomicInteger result = Converter.convertToAtomicInteger(input);
        assertNotNull(result, "Result should not be null for input: " + input);
        assertEquals(expected, result.get(), () -> "Unexpected AtomicInteger value for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] convertToAtomicInteger({0}) throws {1}")
    @MethodSource("exceptionCases")
    void convertToAtomicInteger_failure(Object input, Class<? extends Throwable> exceptionClass) {
        assertThrows(exceptionClass, () -> Converter.convertToAtomicInteger(input), () -> "Expected " + exceptionClass.getSimpleName() + " for input: " + input);
    }
}
