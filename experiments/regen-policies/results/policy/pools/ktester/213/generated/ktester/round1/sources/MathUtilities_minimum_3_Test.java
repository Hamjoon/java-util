package com.cedarsoftware.util;

import java.math.BigInteger;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_3_Test {

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(// {"5"} -> 5
        Arguments.of(new BigInteger[] { new BigInteger("5") }, new BigInteger("5")), // {null} -> IllegalArgumentException
        Arguments.of(new BigInteger[] { null }, IllegalArgumentException.class));
    }

    private static Stream<Arguments> multipleElementProvider() {
        return Stream.of(// 10,20,5,15 -> 5
        Arguments.of(new BigInteger[] { new BigInteger("10"), new BigInteger("20"), new BigInteger("5"), new BigInteger("15") }, new BigInteger("5")), // -3,-7,2,0 -> -7
        Arguments.of(new BigInteger[] { new BigInteger("-3"), new BigInteger("-7"), new BigInteger("2"), new BigInteger("0") }, new BigInteger("-7")), // 0,0,0 -> 0
        Arguments.of(new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO }, BigInteger.ZERO), // very large numbers
        Arguments.of(new BigInteger[] { new BigInteger("123456789012345678901234567890"), new BigInteger("98765432109876543210987654321") }, new BigInteger("98765432109876543210987654321")));
    }

    private static Stream<Arguments> boundaryValueProvider() {
        return Stream.of(// LONG_MAX and LONG_MIN -> LONG_MIN
        Arguments.of(new BigInteger[] { MathUtilities.BIG_INT_LONG_MAX, MathUtilities.BIG_INT_LONG_MIN }, MathUtilities.BIG_INT_LONG_MIN), // LONG_MAX, LONG_MAX -> LONG_MAX
        Arguments.of(new BigInteger[] { MathUtilities.BIG_INT_LONG_MAX, MathUtilities.BIG_INT_LONG_MAX }, MathUtilities.BIG_INT_LONG_MAX));
    }

    @Test
    @DisplayName("minimum(BigInteger) returns the sole element")
    void testMinimumSingleElement() {
        BigInteger value = new BigInteger("12345678901234567890");
        BigInteger result = MathUtilities.minimum(value);
        assertEquals(value, result, "The method should return the only element supplied");
        // also verify that the same instance is returned (no new object is created)
        assertSame(value, result, "The returned instance should be the same as the input when only one element is provided");
    }

    @Test
    @DisplayName("minimum(BigInteger[]) returns the smallest value among many")
    void testMinimumMultipleElements() {
        BigInteger a = new BigInteger("-999999999999999999999");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("999999999999999999999");
        BigInteger d = new BigInteger("-12345678901234567890");
        BigInteger result = MathUtilities.minimum(a, b, c, d);
        assertEquals(a, result, "The smallest value should be returned");
    }

    @Test
    @DisplayName("minimum works with the predefined BIG_INT constants")
    void testMinimumWithPredefinedConstants() {
        BigInteger result = MathUtilities.minimum(MathUtilities.BIG_INT_LONG_MAX, MathUtilities.BIG_INT_LONG_MIN, BigInteger.ZERO);
        assertEquals(MathUtilities.BIG_INT_LONG_MIN, result, "The method should correctly handle the class constants");
    }

    @Test
    @DisplayName("minimum throws IllegalArgumentException when a null entry is present")
    void testMinimumNullEntryThrows() {
        BigInteger valid = BigInteger.ONE;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(valid, null, valid), "A null entry must cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("null BigInteger entry"), "Exception message should mention the null entry");
    }

    @Test
    @DisplayName("minimum does not modify the original array")
    void testMinimumDoesNotMutateInput() {
        BigInteger[] original = { new BigInteger("5"), new BigInteger("-10"), new BigInteger("3") };
        // Keep a copy for later comparison
        BigInteger[] copy = { new BigInteger("5"), new BigInteger("-10"), new BigInteger("3") };
        BigInteger result = MathUtilities.minimum(original);
        assertEquals(new BigInteger("-10"), result, "Result should be the smallest value");
        // Verify the original array contents are unchanged
        assertArrayEquals(copy, original, "Input array must remain unchanged after the call");
    }

    @Test
    @DisplayName("minimum with an empty var‑args array throws ArrayIndexOutOfBoundsException")
    void testMinimumEmptyArrayThrows() {
        // The method does not guard against zero length; it will throw an AIOOBE.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(), "Calling minimum with no arguments should throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Single element scenarios **
     */
    @ParameterizedTest(name = "[{index}] values={0}")
    @MethodSource("singleElementProvider")
    @DisplayName("Single element scenarios")
    void testSingleElementScenarios(BigInteger[] input, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(IllegalArgumentException.class)) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(input), "Expected IllegalArgumentException for null single element");
            assertTrue(ex.getMessage().contains("null BigInteger entry"));
        } else {
            BigInteger result = MathUtilities.minimum(input);
            assertEquals(expected, result);
        }
    }

    /**
     * Multiple element scenarios **
     */
    @ParameterizedTest(name = "[{index}] values={0}")
    @MethodSource("multipleElementProvider")
    @DisplayName("Multiple element scenarios")
    void testMultipleElementScenarios(BigInteger[] input, BigInteger expected) {
        BigInteger result = MathUtilities.minimum(input);
        assertEquals(expected, result);
    }

    /**
     * Boundary value scenarios **
     */
    @ParameterizedTest(name = "[{index}] values={0}")
    @MethodSource("boundaryValueProvider")
    @DisplayName("Boundary value scenarios")
    void testBoundaryValueScenarios(BigInteger[] input, BigInteger expected) {
        BigInteger result = MathUtilities.minimum(input);
        assertEquals(expected, result);
    }

    /**
     * Null handling in multi‑element array **
     */
    @Test
    @DisplayName("Null handling in multi‑element array throws IllegalArgumentException")
    void testNullInMultiElementArray() {
        BigInteger[] values = { new BigInteger("5"), null, new BigInteger("3") };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(values), "A null element anywhere should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("null BigInteger entry"));
    }

    /**
     * Empty array handling **
     */
    @Test
    @DisplayName("Empty array handling throws ArrayIndexOutOfBoundsException")
    void testEmptyArrayHandling() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(new BigInteger[] {}), "Calling minimum with an empty array should throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Null varargs array handling **
     */
    @Test
    @DisplayName("Null varargs array handling throws NullPointerException")
    void testNullVarargsArrayHandling() {
        // Passing a null reference as the var‑args array.
        assertThrows(NullPointerException.class, () -> MathUtilities.minimum((BigInteger[]) null), "Passing null as the var‑args array should result in NullPointerException");
    }
}
