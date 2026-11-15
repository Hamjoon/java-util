package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_maximum_3_Test {

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of("When the array contains a single non‑null BigInteger, the method should return that element unchanged.", new String[] { "42" }, "42"), Arguments.of("A single‑element array with a null entry must cause an IllegalArgumentException as per the null‑check branch.", new String[] { null }, "IllegalArgumentException"));
    }

    private static Stream<Arguments> multipleElementsProvider() {
        return Stream.of(Arguments.of("Array with three positive numbers; the maximum is the middle element.", new String[] { "5", "10", "3" }, "10"), Arguments.of("Array where the maximum is the last element.", new String[] { "100", "50", "200", "150" }, "200"), Arguments.of("All negative numbers; the least negative number is the maximum.", new String[] { "-5", "-10", "-3" }, "-3"), Arguments.of("All elements are equal; the method should return that common value.", new String[] { "0", "0", "0" }, "0"));
    }

    private static Stream<Arguments> nullEntryProvider() {
        return Stream.of(Arguments.of("A null element appears in the middle of the array; method must reject it.", new String[] { "7", null, "9" }), Arguments.of("A null element appears as the first entry; method must reject it.", new String[] { null, "1", "2" }));
    }

    private static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(Arguments.of("Uses BIG_INT_LONG_MAX and BIG_INT_LONG_MIN constants; the maximum is LONG_MAX.", new String[] { "9223372036854775807", "-9223372036854775808", "0" }, "9223372036854775807"), Arguments.of("Duplicate LONG_MAX values; method should still return LONG_MAX.", new String[] { "9223372036854775807", "9223372036854775807", "-9223372036854775808" }, "9223372036854775807"), Arguments.of("Array containing only LONG_MIN; the result must be LONG_MIN.", new String[] { "-9223372036854775808", "-9223372036854775808" }, "-9223372036854775808"), Arguments.of("Values exceed the range of long; method must correctly compare large BigIntegers.", new String[] { "123456789012345678901234567890", "987654321098765432109876543210" }, "987654321098765432109876543210"));
    }

    /* --------------------------------------------------------------------- */
    private static BigInteger[] toBigIntegerArray(String[] strings) {
        if (strings == null) {
            return null;
        }
        BigInteger[] result = new BigInteger[strings.length];
        for (int i = 0; i < strings.length; i++) {
            result[i] = (strings[i] == null) ? null : new BigInteger(strings[i]);
        }
        return result;
    }

    @Test
    void maximumReturnsLargestBigInteger() {
        BigInteger a = new BigInteger("-123456789012345678901234567890");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("987654321098765432109876543210");
        BigInteger d = new BigInteger("-1");
        BigInteger result = MathUtilities.maximum(a, b, c, d);
        assertEquals(c, result, "maximum should return the greatest BigInteger");
    }

    @Test
    void maximumWithSingleElementReturnsThatElement() {
        BigInteger value = new BigInteger("42");
        assertEquals(value, MathUtilities.maximum(value), "single‑element array should return the element itself");
        // also test the public constants defined in MathUtilities
        assertEquals(MathUtilities.BIG_INT_LONG_MAX, MathUtilities.maximum(MathUtilities.BIG_INT_LONG_MAX), "single constant should be returned unchanged");
    }

    @Test
    void maximumHandlesLongBoundaryConstantsCorrectly() {
        BigInteger max = MathUtilities.BIG_INT_LONG_MAX;
        BigInteger min = MathUtilities.BIG_INT_LONG_MIN;
        // exceeds long range
        BigInteger larger = max.add(BigInteger.ONE);
        // max should still be the greatest among min and max
        assertEquals(max, MathUtilities.maximum(min, max), "should pick the larger long‑range constant");
        // when a value larger than LONG_MAX is present, it should be returned
        assertEquals(larger, MathUtilities.maximum(min, max, larger), "should correctly compare values beyond long range");
    }

    @Test
    void maximumThrowsWhenAnyArgumentIsNull() {
        BigInteger valid = BigInteger.TEN;
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum((BigInteger) null), "null single argument must cause IllegalArgumentException");
        assertTrue(ex1.getMessage().contains("null BigInteger"), "exception message should mention null entry");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(valid, null, valid), "null within var‑args must cause IllegalArgumentException");
        assertTrue(ex2.getMessage().contains("null BigInteger"), "exception message should mention null entry");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Single element: {0}")
    @MethodSource("singleElementProvider")
    void maximumSingleElement(String description, String[] values, String expected) {
        BigInteger[] bigInts = toBigIntegerArray(values);
        if ("IllegalArgumentException".equals(expected)) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(bigInts), description);
            assertTrue(ex.getMessage().contains("null BigInteger"));
        } else {
            BigInteger expectedBig = new BigInteger(expected);
            assertEquals(expectedBig, MathUtilities.maximum(bigInts), description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Multiple elements normal: {0}")
    @MethodSource("multipleElementsProvider")
    void maximumMultipleElements(String description, String[] values, String expected) {
        BigInteger[] bigInts = toBigIntegerArray(values);
        BigInteger expectedBig = new BigInteger(expected);
        assertEquals(expectedBig, MathUtilities.maximum(bigInts), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Null entry in array: {0}")
    @MethodSource("nullEntryProvider")
    void maximumNullEntry(String description, String[] values) {
        BigInteger[] bigInts = toBigIntegerArray(values);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(bigInts), description);
        assertTrue(ex.getMessage().contains("null BigInteger"));
    }

    // -----------------------------------------------------------------
    @Test
    void maximumEmptyArrayThrowsAIOOBE() {
        // The implementation accesses values[0] without a guard, so an empty array triggers AIOOBE.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(), "Calling maximum with an empty array should trigger an ArrayIndexOutOfBoundsException");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Boundary values: {0}")
    @MethodSource("boundaryValuesProvider")
    void maximumBoundaryValues(String description, String[] values, String expected) {
        BigInteger[] bigInts = toBigIntegerArray(values);
        BigInteger expectedBig = new BigInteger(expected);
        assertEquals(expectedBig, MathUtilities.maximum(bigInts), description);
    }

    // -----------------------------------------------------------------
    @Test
    void maximumNullArrayReferenceThrowsNPE() {
        // Passing a null reference as the var‑args array triggers NPE when the method accesses values.length.
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum((BigInteger[]) null), "Passing a null reference as the var‑args array should cause a NullPointerException");
    }
}
