package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_maximum_3_Test {

    /**
     * Helper to convert a String (or null) into a {@link BigInteger}.
     */
    private static BigInteger toBigInt(String s) {
        return s == null ? null : new BigInteger(s);
    }

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(// {"description", {"input values as strings"}, "expected"}
        Arguments.of("When the array contains a single non‑null BigInteger, the method should return that value unchanged.", new String[] { "5" }, "5"), Arguments.of("When the array contains a single null element, the method must throw IllegalArgumentException with the appropriate message.", new String[] { null }, "IllegalArgumentException: Cannot passed null BigInteger entry to maximum()"));
    }

    private static Stream<Arguments> multipleElementProvider() {
        return Stream.of(Arguments.of("Array with several positive numbers; the maximum (3) is in the middle.", new String[] { "1", "3", "2" }, "3"), Arguments.of("All negative numbers; the least‑negative value (-5) should be returned.", new String[] { "-10", "-5", "-20" }, "-5"), Arguments.of("Array of zeros; result must be zero.", new String[] { "0", "0", "0" }, "0"), Arguments.of("Large BigIntegers beyond long range; the biggest value should be returned.", new String[] { "12345678901234567890", "98765432109876543210", "55555555555555555555" }, "98765432109876543210"), Arguments.of("Mixed negative, zero and positive values; positive 100 is the maximum.", new String[] { "-100", "0", "100" }, "100"), Arguments.of("Uses the predefined LONG_MAX and LONG_MIN constants; LONG_MAX is the maximum.", new String[] { "9223372036854775807", "-9223372036854775808", "0" }, "9223372036854775807"), Arguments.of("Duplicate maximum values; method should still return the max (100).", new String[] { "100", "100", "50" }, "100"), Arguments.of("Maximum appears at both first and last positions.", new String[] { "5", "-5", "5" }, "5"), Arguments.of("Long max plus one (requires BigInteger); verifies handling of values just beyond long range.", new String[] { "9223372036854775807", "9223372036854775808" }, "9223372036854775808"), Arguments.of("Very large numbers to ensure no overflow or truncation.", new String[] { "123456789012345678901234567890", "999999999999999999999999999999" }, "999999999999999999999999999999"));
    }

    private static Stream<Arguments> nullSubsequentProvider() {
        return Stream.of(Arguments.of("A null entry after the first element triggers the explicit null‑check inside the loop, throwing IllegalArgumentException with the defined message.", new String[] { "5", null, "10" }));
    }

    @Test
    void maximumSingleElementReturnsSameValue() {
        BigInteger value = new BigInteger("12345678901234567890");
        assertEquals(value, MathUtilities.maximum(value));
    }

    @Test
    void maximumMultipleElementsFindsGreatest() {
        BigInteger a = new BigInteger("-999999999999999999999");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("999999999999999999999");
        BigInteger d = new BigInteger("12345678901234567890");
        assertEquals(c, MathUtilities.maximum(a, b, c, d));
    }

    @Test
    void maximumHandlesNegativeAndPositiveValues() {
        BigInteger neg = new BigInteger("-42");
        BigInteger pos = new BigInteger("42");
        assertEquals(pos, MathUtilities.maximum(neg, pos));
    }

    @Test
    void maximumWithLongMinAndMaxConstants() {
        // Using the public constants defined in MathUtilities
        BigInteger min = MathUtilities.BIG_INT_LONG_MIN;
        BigInteger max = MathUtilities.BIG_INT_LONG_MAX;
        assertEquals(max, MathUtilities.maximum(min, max));
    }

    @Test
    void maximumThrowsWhenAnyArgumentIsNull() {
        BigInteger valid = new BigInteger("10");
        BigInteger[] arrayWithNull = { valid, null, new BigInteger("20") };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(arrayWithNull));
        assertTrue(ex.getMessage().contains("null BigInteger"));
    }

    @Test
    void maximumThrowsWhenFirstArgumentIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum((BigInteger) null));
        assertTrue(ex.getMessage().contains("null BigInteger"));
    }

    @Test
    void maximumWithAllEqualValuesReturnsThatValue() {
        BigInteger v = new BigInteger("777777777777777777777");
        assertEquals(v, MathUtilities.maximum(v, v, v, v));
    }

    @Test
    void maximumWithVeryLargeNumbers() {
        BigInteger veryLarge = new BigInteger("1234567890123456789012345678901234567890");
        BigInteger slightlySmaller = veryLarge.subtract(BigInteger.ONE);
        assertEquals(veryLarge, MathUtilities.maximum(slightlySmaller, veryLarge));
    }

    /* ---------- Single element cases ---------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("singleElementProvider")
    void maximumSingleElementCases(String description, String[] inputs, String expected) {
        // Build the var‑args array
        BigInteger[] values = Stream.of(inputs).map(MathUtilities_maximum_3_Test::toBigInt).toArray(BigInteger[]::new);
        if (expected.startsWith("IllegalArgumentException")) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(values));
            assertTrue(ex.getMessage().contains("null BigInteger"));
        } else {
            BigInteger expectedBigInt = new BigInteger(expected);
            assertEquals(expectedBigInt, MathUtilities.maximum(values));
        }
    }

    /* ---------- Multiple element cases ---------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("multipleElementProvider")
    void maximumMultipleElementCases(String description, String[] inputs, String expected) {
        BigInteger[] values = Stream.of(inputs).map(MathUtilities_maximum_3_Test::toBigInt).toArray(BigInteger[]::new);
        if (expected.startsWith("IllegalArgumentException")) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(values));
            assertTrue(ex.getMessage().contains("null BigInteger"));
        } else {
            BigInteger expectedBigInt = new BigInteger(expected);
            assertEquals(expectedBigInt, MathUtilities.maximum(values));
        }
    }

    /* ---------- Null array reference ---------- */
    @Test
    void maximumNullArrayReferenceThrowsNPE() {
        // Passing a null var‑args array should result in a NullPointerException
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum((BigInteger[]) null));
    }

    /* ---------- Empty array ---------- */
    @Test
    void maximumEmptyArrayThrowsArrayIndexOutOfBoundsException() {
        // The implementation accesses values[0] without a guard, so an empty array triggers AIOOBE
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(new BigInteger[0]));
    }

    /* ---------- First element null with multiple values ---------- */
    @Test
    void maximumFirstElementNullWithMultipleValuesThrowsNPE() {
        BigInteger[] values = { null, new BigInteger("5"), new BigInteger("10") };
        // The method will set current = null and later call .max(null) -> NPE
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum(values));
    }

    /* ---------- Null element in subsequent position ---------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullSubsequentProvider")
    void maximumNullElementInSubsequentPositionThrowsIAE(String description, String[] inputs) {
        BigInteger[] values = Stream.of(inputs).map(MathUtilities_maximum_3_Test::toBigInt).toArray(BigInteger[]::new);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(values));
        assertTrue(ex.getMessage().contains("null BigInteger"));
    }
}
