package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

class MathUtilities_maximum_3_Test {

    private static Stream<Arguments> provideSuccessfulMaximumCases() {
        return Stream.of(// single non‑null element
        Arguments.of("Single non‑null element should be returned unchanged", new BigInteger[] { BigInteger.valueOf(5) }, BigInteger.valueOf(5)), // distinct positive numbers
        Arguments.of("Array with distinct positive numbers; greatest is 7", new BigInteger[] { BigInteger.valueOf(3), BigInteger.valueOf(7), BigInteger.valueOf(5) }, BigInteger.valueOf(7)), // negative numbers and zero
        Arguments.of("Array containing negative numbers and zero; maximum is 0", new BigInteger[] { BigInteger.valueOf(-10), BigInteger.ZERO, BigInteger.valueOf(-5) }, BigInteger.ZERO), // predefined LONG_MIN and LONG_MAX constants
        Arguments.of("Long.MAX_VALUE constant should be returned", new BigInteger[] { MathUtilities.BIG_INT_LONG_MIN, MathUtilities.BIG_INT_LONG_MAX }, MathUtilities.BIG_INT_LONG_MAX), // all elements equal
        Arguments.of("All equal elements; the common value should be returned", new BigInteger[] { BigInteger.valueOf(42), BigInteger.valueOf(42), BigInteger.valueOf(42) }, BigInteger.valueOf(42)), // very large number beyond long range
        Arguments.of("Array with a very large number (beyond long range)", new BigInteger[] { BigInteger.valueOf(1000), new BigInteger("999999999999999999"), BigInteger.valueOf(500) }, new BigInteger("999999999999999999")));
    }

    private static Stream<Arguments> provideIllegalArgumentExceptionCases() {
        return Stream.of(// single null element
        Arguments.of("Single null element must trigger IllegalArgumentException", new BigInteger[] { null }), // null entry in middle of array
        Arguments.of("Array contains a null entry after the first element", new BigInteger[] { BigInteger.ONE, null, BigInteger.TEN }), // array starts with null, later element also null (first null triggers later check)
        Arguments.of("Array starts with null; exception should be thrown immediately", new BigInteger[] { null, BigInteger.valueOf(5), BigInteger.valueOf(10) }), // first element null, later element also null (second null is detected in loop)
        Arguments.of("First element null with subsequent null should raise IllegalArgumentException", new BigInteger[] { null, null }));
    }

    @Test
    @DisplayName("maximum(BigInteger) returns the sole element")
    void singleElementReturned() {
        // given
        BigInteger value = new BigInteger("12345678901234567890");
        // when
        BigInteger result = MathUtilities.maximum(value);
        // then
        assertEquals(value, result, "Maximum of a single element should be the element itself");
    }

    @Test
    @DisplayName("maximum(BigInteger...) returns the greatest element")
    void returnsGreatestOfMultipleElements() {
        // given
        BigInteger a = new BigInteger("-99999999999999999999");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("12345678901234567890");
        BigInteger d = new BigInteger("99999999999999999999");
        // when
        BigInteger result = MathUtilities.maximum(a, b, c, d);
        // then
        assertEquals(d, result, "Maximum should be the greatest BigInteger value");
    }

    @Test
    @DisplayName("maximum(BigInteger...) throws IllegalArgumentException when a null entry is present")
    void throwsWhenNullEntry() {
        // given
        BigInteger valid = BigInteger.ONE;
        BigInteger[] values = { valid, null, BigInteger.TEN };
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(values), "Method should reject null entries");
        assertTrue(ex.getMessage().contains("Cannot passed null BigInteger entry"), "Exception message should indicate a null entry problem");
    }

    @Test
    @DisplayName("maximum(BigInteger...) throws ArrayIndexOutOfBoundsException when called with no arguments")
    void throwsWhenNoArguments() {
        // given – an empty var‑args array
        BigInteger[] empty = {};
        // when / then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(empty), "Calling maximum with no arguments should result in an index‑out‑of‑bounds error");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulMaximumCases")
    @DisplayName("maximum(BigInteger[]) returns the expected greatest value")
    void maximumReturnsExpected(String description, BigInteger[] input, BigInteger expected) {
        // when
        BigInteger result = MathUtilities.maximum(input);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideIllegalArgumentExceptionCases")
    @DisplayName("maximum(BigInteger[]) throws IllegalArgumentException for null entries")
    void maximumThrowsIllegalArgumentException(String description, BigInteger[] input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(input), description);
        assertTrue(ex.getMessage().contains("Cannot passed null BigInteger entry"), "Exception message must mention null entry");
    }

    @Test
    @DisplayName("maximum(BigInteger[]) throws NullPointerException when var‑args array reference is null")
    void maximumThrowsNullPointerExceptionWhenArrayIsNull() {
        // when / then
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum((BigInteger[]) null), "Passing a null array reference should cause NullPointerException");
    }

    @Test
    @DisplayName("maximum(BigInteger[]) throws NullPointerException when first element is null and second is non‑null")
    void maximumThrowsNullPointerExceptionWhenFirstIsNullSecondNonNull() {
        // given
        BigInteger[] input = { null, BigInteger.valueOf(5) };
        // when / then
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum(input), "First element null followed by non‑null should cause NullPointerException");
    }

    @Test
    @DisplayName("maximum(BigInteger[]) throws ArrayIndexOutOfBoundsException for empty var‑args")
    void maximumThrowsArrayIndexOutOfBoundsForEmptyArray() {
        // when / then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(), "Calling maximum with no arguments should result in ArrayIndexOutOfBoundsException");
    }
}
