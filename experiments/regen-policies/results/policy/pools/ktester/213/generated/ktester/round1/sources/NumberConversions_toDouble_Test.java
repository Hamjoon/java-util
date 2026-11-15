package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDouble_Test {

    @Mock
    private Converter // Converter is not used by toDouble, but required by the signature
    mockConverter;

    /**
     * Simple subclass of {@link Number} that returns a fixed long value.
     */
    private static final class CustomNumber extends Number {

        private final long value;

        CustomNumber(long value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }
    }

    /**
     * Subclass of {@link Number} whose {@code doubleValue()} deliberately throws.
     */
    private static final class ThrowingNumber extends Number {

        @Override
        public int intValue() {
            return 0;
        }

        @Override
        public long longValue() {
            return 0L;
        }

        @Override
        public float floatValue() {
            return 0f;
        }

        @Override
        public double doubleValue() {
            throw new IllegalStateException("doubleValue() failure");
        }
    }

    /**
     * Normal conversions – plain numbers and a user‑defined {@link Number}.
     */
    private static Stream<Arguments> normalConversionsProvider() {
        return Stream.of(Arguments.of(5, 5.0, "Convert a plain Integer to double; should return its doubleValue."), Arguments.of(3.14, 3.14, "Convert a Double instance; should return the same value."), Arguments.of(new BigDecimal("123.456"), 123.456, "Convert a BigDecimal to double; doubleValue() loses precision but matches the decimal part."), Arguments.of(new CustomNumber(42L), 42.0, "Convert a user‑defined subclass of Number; should use its doubleValue implementation."));
    }

    /**
     * Primitive wrapper edge cases – Byte, Short, Long, Float, Atomic types and BigInteger.
     */
    private static Stream<Arguments> wrapperEdgeCasesProvider() {
        return Stream.of(Arguments.of(Byte.valueOf((byte) 0), 0.0, "Convert a Byte wrapper with value 0; should return 0.0."), Arguments.of(Short.valueOf((short) -5), -5.0, "Convert a Short wrapper with a negative value; should return -5.0."), Arguments.of(Long.valueOf(1234567890123L), 1.234567890123E12, "Convert a Long wrapper; doubleValue() yields scientific notation."), Arguments.of(Float.valueOf(2.5f), 2.5, "Convert a Float wrapper; should preserve the decimal part."), Arguments.of(new AtomicInteger(7), 7.0, "Convert an AtomicInteger (subclass of Number); should return its int value as double."), Arguments.of(new AtomicLong(-9L), -9.0, "Convert an AtomicLong with a negative value; should return -9.0."), Arguments.of(new BigInteger("12345678901234567890"), 1.2345678901234568E19, "Convert a BigInteger that exceeds the range of exact double representation; result is the nearest double."));
    }

    /**
     * Special double values such as NaN, infinities, signed zero and extreme magnitudes.
     */
    private static Stream<Arguments> specialValuesProvider() {
        return Stream.of(Arguments.of(Double.NaN, Double.NaN, "Convert Double.NaN; doubleValue() returns NaN."), Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, "Convert positive infinity; should remain Infinity."), Arguments.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, "Convert negative infinity; should remain -Infinity."), Arguments.of(-0.0, -0.0, "Convert negative zero; Double.toString preserves the sign."), Arguments.of(Double.MAX_VALUE, Double.MAX_VALUE, "Convert the largest finite double value."), Arguments.of(Double.MIN_VALUE, Double.MIN_VALUE, "Convert the smallest positive sub‑normal double value."));
    }

    /**
     * Null and invalid inputs – expecting exceptions.
     */
    private static Stream<Arguments> exceptionCasesProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing a null 'from' should throw NullPointerException due to casting to Number."), Arguments.of("not a number", ClassCastException.class, "A String is not a Number; casting should raise ClassCastException."), Arguments.of(new Object(), ClassCastException.class, "An arbitrary Object that does not extend Number should cause ClassCastException."), Arguments.of(ThrowingNumber.class, IllegalStateException.class, "Custom Number whose doubleValue() throws IllegalStateException; the method should propagate this exception."));
    }

    /**
     * Helper to create the ThrowingNumber instance for the exception test.
     */
    private static Object resolveExceptionInput(Object input) {
        if (input instanceof Class && ((Class<?>) input).equals(ThrowingNumber.class)) {
            return new ThrowingNumber();
        }
        return input;
    }

    @Test
    void toDouble_WithInteger_ReturnsDoubleValue() {
        Integer source = 42;
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(42.0, result, 0.000001);
    }

    @Test
    void toDouble_WithLong_ReturnsDoubleValue() {
        Long source = 123L;
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(123.0, result, 0.000001);
    }

    @Test
    void toDouble_WithDouble_ReturnsSameValue() {
        Double source = 3.14159;
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(3.14159, result, 0.000001);
    }

    @Test
    void toDouble_WithFloat_ReturnsDoubleValue() {
        Float source = 1.5f;
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(1.5, result, 0.000001);
    }

    @Test
    void toDouble_WithBigDecimal_ReturnsDoubleValue() {
        BigDecimal source = new BigDecimal("2.71828");
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(2.71828, result, 0.000001);
    }

    @Test
    void toDouble_WithInvalidType_ThrowsClassCastException() {
        String source = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toDouble(source, mockConverter));
    }

    @Test
    void toDouble_WithNull_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toDouble(null, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalConversionsProvider")
    void toDouble_normalConversions(Object from, double expected, String description) {
        double result = NumberConversions.toDouble(from, null);
        assertEquals(expected, result, 1e-12, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("wrapperEdgeCasesProvider")
    void toDouble_wrapperEdgeCases(Object from, double expected, String description) {
        double result = NumberConversions.toDouble(from, null);
        assertEquals(expected, result, 1e-12, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("specialValuesProvider")
    void toDouble_specialValues(Object from, double expected, String description) {
        double result = NumberConversions.toDouble(from, null);
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(result), description);
        } else if (Double.isInfinite(expected)) {
            assertEquals(expected, result, description);
        } else if (expected == 0.0 && Double.doubleToLongBits(expected) != Double.doubleToLongBits(result)) {
            // Handles the signed zero case
            assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(result), description);
        } else {
            assertEquals(expected, result, 0.0, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionCasesProvider")
    void toDouble_exceptionCases(Object rawFrom, Class<? extends Throwable> expectedException, String description) {
        Object from = resolveExceptionInput(rawFrom);
        // Use the mockConverter for the case that explicitly wants a non‑null converter
        Converter converter = (description.contains("non‑null Converter")) ? mockConverter : null;
        assertThrows(expectedException, () -> NumberConversions.toDouble(from, converter), description);
    }

    @Test
    void toDouble_nonNullConverterIsIgnored() {
        // This test corresponds to the JSON case where a DummyConverter is supplied.
        Integer source = Integer.valueOf(10);
        // mockConverter != null
        double result = NumberConversions.toDouble(source, mockConverter);
        assertEquals(10.0, result, 1e-12, "Provide a non‑null Converter implementation; method ignores it and still returns the double value.");
    }
}
