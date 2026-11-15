package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_doubleToString_Test {

    @Mock
    private Converter // Converter is not used by the method, but required for the signature
    mockConverter;

    static Stream<Arguments> zeroValueProvider() {
        return Stream.of(Arguments.of(Double.valueOf(0.0), "0"), Arguments.of(Double.valueOf(-0.0), "0"));
    }

    static Stream<Arguments> nonZeroValueProvider() {
        return Stream.of(Arguments.of(Double.valueOf(1.23), "1.23"), Arguments.of(Double.valueOf(-5.0), "-5.0"), Arguments.of(Double.valueOf(Double.NaN), "NaN"), Arguments.of(Double.valueOf(Double.POSITIVE_INFINITY), "Infinity"), Arguments.of(Double.valueOf(Double.NEGATIVE_INFINITY), "-Infinity"));
    }

    static Stream<Arguments> extremeValueProvider() {
        return Stream.of(// Maximum positive double
        Arguments.of(Double.valueOf(Double.MAX_VALUE), Double.toString(Double.MAX_VALUE)), // Minimum positive sub‑normal (Double.MIN_VALUE == 4.9E-324)
        Arguments.of(Double.valueOf(5e-324), Double.toString(5e-324)), // Smallest normal positive double
        Arguments.of(Double.valueOf(2.2250738585072014e-308), Double.toString(2.2250738585072014e-308)));
    }

    static Stream<Arguments> notationValueProvider() {
        return Stream.of(Arguments.of(Double.valueOf(2.0), "2.0"), Arguments.of(Double.valueOf(1e20), "1.0E20"), Arguments.of(Double.valueOf(1e-20), "1.0E-20"));
    }

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Primitive wrappers that are not Double
        Arguments.of(Integer.valueOf(42), ClassCastException.class), Arguments.of("not a number", ClassCastException.class), Arguments.of(Float.valueOf(3.14f), ClassCastException.class), Arguments.of(Long.valueOf(5L), ClassCastException.class), Arguments.of(AtomicInteger.valueOf(7), ClassCastException.class), // Various other non‑Double numeric types
        Arguments.of(new BigDecimal("123.45"), ClassCastException.class), Arguments.of(new BigInteger("123"), ClassCastException.class), Arguments.of(new AtomicLong(10L), ClassCastException.class), Arguments.of(new AtomicBoolean(true), ClassCastException.class), Arguments.of(Short.valueOf((short) 5), ClassCastException.class), Arguments.of(Byte.valueOf((byte) 2), ClassCastException.class), Arguments.of(Character.valueOf('a'), ClassCastException.class), // Completely unrelated objects
        Arguments.of(new Object(), ClassCastException.class), Arguments.of(new StringBuilder("123"), ClassCastException.class), // Null reference – unboxing a null leads to NPE
        Arguments.of(null, NullPointerException.class));
    }

    @Test
    void doubleZeroIsConvertedToStringZero() {
        // GIVEN
        Double input = 0.0;
        // WHEN
        String result = NumberConversions.doubleToString(input, mockConverter);
        // THEN
        assertEquals("0", result, "Zero double should be converted to the literal string \"0\"");
    }

    @Test
    void nonZeroDoubleIsConvertedUsingToString() {
        // GIVEN
        Double input = 123.456;
        // WHEN
        String result = NumberConversions.doubleToString(input, mockConverter);
        // THEN
        assertEquals(input.toString(), result, "Non‑zero double should be converted by delegating to its toString() implementation");
    }

    @Test
    void negativeNonZeroDoubleIsConvertedUsingToString() {
        // GIVEN
        Double input = -78.9;
        // WHEN
        String result = NumberConversions.doubleToString(input, mockConverter);
        // THEN
        assertEquals(input.toString(), result, "Negative non‑zero double should also be converted by its toString() implementation");
    }

    @Test
    void doubleNaNIsConvertedUsingToString() {
        // GIVEN
        Double input = Double.NaN;
        // WHEN
        String result = NumberConversions.doubleToString(input, mockConverter);
        // THEN
        assertEquals(input.toString(), result, "NaN double should be converted by its toString() implementation (\"NaN\")");
    }

    // ---------- Zero handling (including negative zero) ----------
    @ParameterizedTest(name = "Zero handling: from={0}")
    @MethodSource("zeroValueProvider")
    void zeroValueHandling(Object from, String expected) {
        assertEquals(expected, NumberConversions.doubleToString(from, mockConverter), "Zero (or -0.0) must be converted to literal \"0\"");
    }

    // ---------- Non‑zero ordinary values (positive, negative, NaN, infinities) ----------
    @ParameterizedTest(name = "Non‑zero value: from={0}")
    @MethodSource("nonZeroValueProvider")
    void nonZeroValueHandling(Object from, String expected) {
        assertEquals(expected, NumberConversions.doubleToString(from, mockConverter), "Non‑zero double should be delegated to its toString()");
    }

    // ---------- Extreme double values ----------
    @ParameterizedTest(name = "Extreme value: from={0}")
    @MethodSource("extremeValueProvider")
    void extremeDoubleValues(Object from, String expected) {
        assertEquals(expected, NumberConversions.doubleToString(from, mockConverter), "Extreme double values must be rendered using Double.toString()");
    }

    // ---------- Whole‑number and scientific notation handling ----------
    @ParameterizedTest(name = "Whole/scientific notation: from={0}")
    @MethodSource("notationValueProvider")
    void wholeAndScientificNotation(Object from, String expected) {
        assertEquals(expected, NumberConversions.doubleToString(from, mockConverter), "Whole numbers and scientific notation must be preserved");
    }

    // ---------- Converter argument effect ----------
    @Test
    void converterArgumentIsIgnored() {
        // GIVEN
        Double input = 3.14159;
        // WHEN
        String result = NumberConversions.doubleToString(input, mockConverter);
        // THEN
        assertEquals("3.14159", result, "The Converter argument must not influence the result");
    }

    // ---------- Exception handling for invalid types ----------
    @ParameterizedTest(name = "Invalid type: from={0}")
    @MethodSource("exceptionProvider")
    void invalidTypesThrowExpectedException(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.doubleToString(from, mockConverter), "Invalid input should trigger the expected exception");
    }
}
