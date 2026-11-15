package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDouble_Test {

    /**
     * The Converter is not used by {@code NumberConversions.toDouble}, but we provide a mock
     *  to satisfy the method signature and to demonstrate typical usage.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple {@link Number} subclass that returns a fixed double value.
     */
    private static final class CustomNumber extends Number {

        private final double value;

        CustomNumber(double value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return (long) value;
        }

        @Override
        public float floatValue() {
            return (float) value;
        }

        @Override
        public double doubleValue() {
            return value;
        }
    }

    /**
     * {@link Number} implementation whose {@code doubleValue()} deliberately throws.
     */
    private static final class ThrowingNumber extends Number {

        @Override
        public int intValue() {
            throw new IllegalStateException("intValue");
        }

        @Override
        public long longValue() {
            throw new IllegalStateException("longValue");
        }

        @Override
        public float floatValue() {
            throw new IllegalStateException("floatValue");
        }

        @Override
        public double doubleValue() {
            throw new IllegalStateException("doubleValue");
        }
    }

    private static Stream<Arguments> successCases() {
        return Stream.of(// Primitive wrapper types
        arguments(Double.valueOf(123.456), 123.456), arguments(Float.valueOf(78.9f), 78.9), arguments(Integer.valueOf(42), 42.0), arguments(Long.valueOf(9_999_999_999L), 9_999_999_999.0), arguments(Short.valueOf((short) 7), 7.0), arguments(Byte.valueOf((byte) -3), -3.0), // BigDecimal / BigInteger
        arguments(new BigDecimal("12345.6789"), 12345.6789), arguments(new BigInteger("9876543210"), 9_876_543_210.0), // Atomic numeric types
        arguments(new AtomicInteger(55), 55.0), arguments(new AtomicLong(123_456_789_012_345L), 123_456_789_012_345.0), arguments(new AtomicLong(Long.MAX_VALUE), 9.223372036854776E18), // Float special values (autoboxed to Float)
        arguments(Float.NaN, Double.NaN), arguments(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), arguments(Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), // Double special values
        arguments(Double.NaN, Double.NaN), arguments(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), arguments(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), // Negative big numbers
        arguments(new BigInteger("-1234567890"), -1_234_567_890.0), arguments(new BigDecimal("-0.000001"), -0.000001), // Negative zero
        arguments(-0.0d, -0.0d), // Extreme double values
        arguments(Double.MIN_VALUE, Double.MIN_VALUE), arguments(Double.MAX_VALUE, Double.MAX_VALUE), // Custom Number subclass
        arguments(new CustomNumber(42.5), 42.5));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(arguments(null, NullPointerException.class), arguments("not a number", ClassCastException.class), arguments(new Object(), ClassCastException.class), arguments(new AtomicBoolean(true), ClassCastException.class), // Using a non‑null converter (mock) – still should throw
        arguments("not a number", ClassCastException.class), arguments(new ThrowingNumber(), IllegalStateException.class));
    }

    @Test
    void toDouble_WithInteger_ReturnsDoubleValue() {
        Integer source = 42;
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(42.0, result, 0.000001);
    }

    @Test
    void toDouble_WithDouble_ReturnsSameValue() {
        Double source = 3.14159;
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(3.14159, result, 0.000001);
    }

    @Test
    void toDouble_WithBigDecimal_ReturnsDoubleValue() {
        BigDecimal source = new BigDecimal("123.456");
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(123.456, result, 0.000001);
    }

    @Test
    void toDouble_WithBigInteger_ReturnsDoubleValue() {
        BigInteger source = new BigInteger("987654321");
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(987654321.0, result, 0.000001);
    }

    @Test
    void toDouble_WithAtomicInteger_ReturnsDoubleValue() {
        AtomicInteger source = new AtomicInteger(7);
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(7.0, result, 0.000001);
    }

    @Test
    void toDouble_WithNull_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toDouble(null, mockConverter));
    }

    @Test
    void toDouble_WithNonNumber_ThrowsClassCastException() {
        String source = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toDouble(source, mockConverter));
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases (parameterised)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: toDouble({0}) = {1}")
    @MethodSource("successCases")
    void toDouble_success(Object from, double expected) {
        double result = NumberConversions.toDouble(from, mockConverter);
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(result), "Result should be NaN");
        } else if (Double.isInfinite(expected)) {
            assertEquals(expected, result, "Result should be " + expected);
        } else if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(-0.0d)) {
            // Preserve sign of zero
            assertEquals(Double.doubleToLongBits(-0.0d), Double.doubleToLongBits(result), "Result should be -0.0");
        } else {
            assertEquals(expected, result, 1e-12, () -> "Expected " + expected + " but got " + result);
        }
    }

    /* -------------------------------------------------------------
     *  Exception test cases (parameterised)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: toDouble({0}) throws {1}")
    @MethodSource("exceptionCases")
    void toDouble_exceptions(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toDouble(from, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
