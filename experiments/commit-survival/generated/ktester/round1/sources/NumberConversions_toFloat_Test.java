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
class NumberConversions_toFloat_Test {

    @Mock
    private Converter // not used by the method, but required for the signature
    converter;

    /* --------------------------------------------------------------------- */
    private static class MockConverter implements Converter {
        // No behaviour required – the method under test ignores the converter
    }

    /**
     * Supplies all successful conversion cases defined in the JSON specification.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Valid Number conversions
        Arguments.of("Convert a plain Integer to float", Integer.valueOf(123), "123.0f"), Arguments.of("Convert a Long that exceeds int range", Long.valueOf(9876543210L), "9.876543E9f"), Arguments.of("Convert a Float instance", Float.valueOf(3.14f), "3.14f"), Arguments.of("Convert a Double; narrowed to float", Double.valueOf(2.71828), "2.71828f"), Arguments.of("Convert a BigDecimal; rounded as per float semantics", new BigDecimal("12345.6789"), "12345.679f"), Arguments.of("Convert a very large BigInteger", new BigInteger("999999999999999999"), "1.0E18f"), Arguments.of("Convert a Short (negative value) to float", Short.valueOf((short) -7), "-7.0f"), Arguments.of("Convert a Byte (max positive) to float", Byte.valueOf((byte) 127), "127.0f"), Arguments.of("Convert an AtomicInteger (subclass of Number) to float", new AtomicInteger(55), "55.0f"), Arguments.of("Convert an AtomicLong; verifies handling of another Number subclass", new AtomicLong(1234567890123L), "1.2345679E12f"), Arguments.of("Convert Float.NaN; result should be NaN", Float.NaN, "NaN"), Arguments.of("Convert Float.POSITIVE_INFINITY; result should be positive infinity", Float.POSITIVE_INFINITY, "Infinity"), Arguments.of("Convert Float.NEGATIVE_INFINITY; result should be negative infinity", Float.NEGATIVE_INFINITY, "-Infinity"), // Boundary Float Values
        Arguments.of("Maximum finite float value should be returned unchanged", Float.MAX_VALUE, "3.4028235E38f"), Arguments.of("Minimum positive subnormal float value should be returned unchanged", Float.MIN_VALUE, "1.4E-45f"), Arguments.of("Double value larger than Float.MAX_VALUE should convert to positive infinity", Double.MAX_VALUE, "Infinity"), Arguments.of("Smallest positive double subnormal converts to 0.0f due to underflow", Double.MIN_VALUE, "0.0f"), Arguments.of("Double NaN should become Float NaN", Double.NaN, "NaN"), Arguments.of("Double positive infinity maps to Float infinity", Double.POSITIVE_INFINITY, "Infinity"), Arguments.of("Double negative infinity maps to Float negative infinity", Double.NEGATIVE_INFINITY, "-Infinity"), // Numeric Edge Integers
        Arguments.of("Maximum int value conversion to float (may lose precision)", Integer.MAX_VALUE, "2.14748365E9f"), Arguments.of("Minimum int value conversion to float", Integer.MIN_VALUE, "-2.14748365E9f"), Arguments.of("Shortest negative short value conversion", Short.MIN_VALUE, "-32768.0f"), Arguments.of("Byte minimum value conversion", Byte.MIN_VALUE, "-128.0f"), // Non‑null Converter Parameter (converter is ignored)
        Arguments.of("Converter is provided but ignored; result should be same as with null converter", Integer.valueOf(42), "42.0f"));
    }

    /**
     * Supplies all exception‑expecting cases defined in the JSON specification.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of("Passing null as the 'from' argument should throw NullPointerException", null, NullPointerException.class), Arguments.of("Passing a non‑Number object (String) should result in ClassCastException", "test", ClassCastException.class), Arguments.of("Passing an Object that does not implement Number should throw ClassCastException", new Object(), ClassCastException.class));
    }

    @Test
    void toFloat_WithInteger_ReturnsFloatValue() {
        // given
        Integer source = 42;
        // when
        float result = NumberConversions.toFloat(source, converter);
        // then
        assertEquals(42.0f, result);
    }

    @Test
    void toFloat_WithLong_ReturnsFloatValue() {
        // given
        Long source = 123L;
        // when
        float result = NumberConversions.toFloat(source, converter);
        // then
        assertEquals(123.0f, result);
    }

    @Test
    void toFloat_WithDouble_ReturnsFloatValue() {
        // given
        Double source = 3.14159;
        // when
        float result = NumberConversions.toFloat(source, converter);
        // then
        assertEquals(3.14159f, result, 0.00001f);
    }

    @Test
    void toFloat_WithBigDecimal_ReturnsFloatValue() {
        // given
        BigDecimal source = new BigDecimal("123.456");
        // when
        float result = NumberConversions.toFloat(source, converter);
        // then
        assertEquals(123.456f, result, 0.0001f);
    }

    @Test
    void toFloat_WithNull_ThrowsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> NumberConversions.toFloat(source, converter));
    }

    @Test
    void toFloat_WithNonNumber_ThrowsClassCastException() {
        // given
        String source = "not a number";
        // when / then
        assertThrows(ClassCastException.class, () -> NumberConversions.toFloat(source, converter));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toFloat_ValidConversions(String description, Object from, String expectedStr) {
        // when
        float result = NumberConversions.toFloat(from, converter);
        // then – handle special float values explicitly
        switch(expectedStr) {
            case "NaN":
                assertTrue(Float.isNaN(result), description);
                break;
            case "Infinity":
                assertEquals(Float.POSITIVE_INFINITY, result, description);
                break;
            case "-Infinity":
                assertEquals(Float.NEGATIVE_INFINITY, result, description);
                break;
            default:
                float expected = Float.parseFloat(expectedStr);
                // Use a small delta for floating‑point comparisons; most values are exact
                // but values that lose precision (e.g., large longs) need a tolerance.
                float delta = Math.abs(expected) > 1e7f ? Math.abs(expected) * 1e-6f : 1e-6f;
                assertEquals(expected, result, delta, description);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toFloat_Exceptions(String description, Object from, Class<? extends Throwable> exceptionClass) {
        // when / then
        assertThrows(exceptionClass, () -> NumberConversions.toFloat(from, converter), description);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toFloat_WithNonNullConverter_IgnoredByImplementation() {
        // given
        Integer source = 42;
        // not used by the method
        Converter realConverter = new MockConverter();
        // when
        float result = NumberConversions.toFloat(source, realConverter);
        // then
        assertEquals(42.0f, result);
    }
}
