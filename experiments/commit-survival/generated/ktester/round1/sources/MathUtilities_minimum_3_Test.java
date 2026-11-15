package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_3_Test {

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(// Single non‑null element should be returned directly.
        Arguments.of("Single non‑null element", new BigInteger[] { BigInteger.valueOf(5) }, BigInteger.valueOf(5)), // Single null element must trigger IllegalArgumentException.
        Arguments.of("Single null element", new BigInteger[] { null }, IllegalArgumentException.class));
    }

    private static Stream<Arguments> multipleElementsProvider() {
        return Stream.of(// Typical case with three distinct positive values; the smallest (3) should be returned.
        Arguments.of("Three positive values", new BigInteger[] { BigInteger.valueOf(10), BigInteger.valueOf(3), BigInteger.valueOf(7) }, BigInteger.valueOf(3)), // Mix of negative numbers and zero; the most negative value (-10) should be returned.
        Arguments.of("Negative numbers and zero", new BigInteger[] { BigInteger.valueOf(-5), BigInteger.valueOf(-10), BigInteger.ZERO }, BigInteger.valueOf(-10)), // Array contains the predefined Long max/min constants; the minimum (Long.MIN_VALUE) must be returned.
        Arguments.of("Long max/min constants", new BigInteger[] { MathUtilities.BIG_INT_LONG_MAX, MathUtilities.BIG_INT_LONG_MIN, BigInteger.ZERO }, MathUtilities.BIG_INT_LONG_MIN), // Very large numbers beyond the long range; the negative large number should be identified as the minimum.
        Arguments.of("Very large numbers beyond long range", new BigInteger[] { new BigInteger("123456789012345678901234567890"), new BigInteger("-123456789012345678901234567891") }, new BigInteger("-123456789012345678901234567891")), // All elements are equal; the method should return that common value.
        Arguments.of("All equal elements", new BigInteger[] { BigInteger.valueOf(5), BigInteger.valueOf(5), BigInteger.valueOf(5) }, BigInteger.valueOf(5)));
    }

    private static Stream<Arguments> nullElementProvider() {
        return Stream.of(// A null entry appears after the first element; the method must throw IllegalArgumentException.
        Arguments.of("Null after first element", new BigInteger[] { BigInteger.valueOf(1), null, BigInteger.valueOf(2) }, IllegalArgumentException.class), // The first element is null in a multi‑element array; the method must throw IllegalArgumentException.
        Arguments.of("Null as first element in multi‑element array", new BigInteger[] { null, BigInteger.valueOf(1) }, IllegalArgumentException.class));
    }

    @Test
    void minimum_singleNonNullValue_returnsThatValue() {
        // given
        BigInteger value = new BigInteger("12345678901234567890");
        // when
        BigInteger result = MathUtilities.minimum(value);
        // then
        assertEquals(value, result, "Minimum of a single non‑null element should be the element itself");
    }

    @Test
    void minimum_multipleValues_returnsSmallest() {
        // given
        BigInteger a = new BigInteger("999");
        BigInteger b = new BigInteger("-1000");
        BigInteger c = new BigInteger("0");
        BigInteger d = new BigInteger("500");
        // when
        BigInteger result = MathUtilities.minimum(a, b, c, d);
        // then
        assertEquals(b, result, "Minimum should be the smallest BigInteger in the array");
    }

    @Test
    void minimum_valuesWithSameNumber_returnsThatNumber() {
        // given
        BigInteger same = new BigInteger("42");
        // when
        BigInteger result = MathUtilities.minimum(same, same, same);
        // then
        assertEquals(same, result, "When all values are equal, the result should be that value");
    }

    @Test
    void minimum_nullSingleArgument_throwsIllegalArgumentException() {
        // given
        BigInteger nullValue = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(nullValue));
        assertEquals("Cannot passed null BigInteger entry to minimum()", ex.getMessage());
    }

    @Test
    void minimum_nullInArray_throwsIllegalArgumentException() {
        // given
        BigInteger a = new BigInteger("10");
        BigInteger b = null;
        BigInteger c = new BigInteger("20");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(a, b, c));
        assertEquals("Cannot passed null BigInteger entry to minimum()", ex.getMessage());
    }

    @Test
    void minimum_emptyArray_throwsArrayIndexOutOfBoundsException() {
        // given – an empty var‑args array
        BigInteger[] empty = new BigInteger[0];
        // when / then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(empty), "Calling minimum with an empty array should propagate ArrayIndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("singleElementProvider")
    void minimum_singleElementHandling(String description, BigInteger[] values, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            Throwable thrown = assertThrows(exClass, () -> MathUtilities.minimum(values), description);
            // Verify the exact message for IllegalArgumentException as per implementation
            if (exClass == IllegalArgumentException.class) {
                assertEquals("Cannot passed null BigInteger entry to minimum()", thrown.getMessage(), "Exception message must match contract");
            }
        } else {
            // Expected a concrete BigInteger result
            BigInteger result = MathUtilities.minimum(values);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("multipleElementsProvider")
    void minimum_multipleElementsHandling(String description, BigInteger[] values, Object expected) {
        // All cases in this group are successful (no exception expected)
        BigInteger result = MathUtilities.minimum(values);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullElementProvider")
    void minimum_nullElementHandling(String description, BigInteger[] values, Class<? extends Throwable> expectedException) {
        Throwable thrown = assertThrows(expectedException, () -> MathUtilities.minimum(values), description);
        assertEquals("Cannot passed null BigInteger entry to minimum()", thrown.getMessage(), "Exception message must match contract");
    }

    @Test
    void minimum_nullArray_throwsNullPointerException() {
        // given – a null reference for the var‑args parameter
        BigInteger[] nullArray = null;
        // when / then
        assertThrows(NullPointerException.class, () -> MathUtilities.minimum(nullArray), "Passing a null array reference should trigger NullPointerException");
    }
}
