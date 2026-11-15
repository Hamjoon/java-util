package com.cedarsoftware.util;

import java.math.BigInteger;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_3_Test {

    private static Stream<Arguments> singleElementCases() {
        return Stream.of(// Single non‑null element – should be returned unchanged
        Arguments.of(new TestCase(new BigInteger[] { new BigInteger("5") }, new BigInteger("5"), null, "Single non‑null element – method returns that element")), // Single element that is null – IllegalArgumentException per len==1 branch
        Arguments.of(new TestCase(new BigInteger[] { null }, null, IllegalArgumentException.class, "Single null element – should throw IllegalArgumentException")));
    }

    private static Stream<Arguments> multipleElementCases() {
        return Stream.of(// Normal case – smallest value is 3
        Arguments.of(new TestCase(new BigInteger[] { new BigInteger("10"), new BigInteger("3"), new BigInteger("7"), new BigInteger("20") }, new BigInteger("3"), null, "Multiple non‑null values – smallest (3) should be returned")), // Null entry at index 1 – IllegalArgumentException from loop check
        Arguments.of(new TestCase(new BigInteger[] { new BigInteger("10"), null, new BigInteger("5") }, null, IllegalArgumentException.class, "Null entry at index 1 – should trigger IllegalArgumentException")), // First element null with length > 1 – NPE when .min() is invoked
        Arguments.of(new TestCase(new BigInteger[] { null, new BigInteger("5"), new BigInteger("6") }, null, NullPointerException.class, "First element null (len>1) – leads to NullPointerException")), // Empty array – accessing values[0] causes AIOOBE
        Arguments.of(new TestCase(new BigInteger[0], null, ArrayIndexOutOfBoundsException.class, "Empty array – should throw ArrayIndexOutOfBoundsException")));
    }

    private static Stream<Arguments> nullArrayCases() {
        return Stream.of(// The var‑args array itself is null – NPE when reading length
        Arguments.of(new TestCase(null, null, NullPointerException.class, "Null var‑args array – should throw NullPointerException")));
    }

    @Test
    @DisplayName("minimum(BigInteger[]) returns the sole element when only one value is supplied")
    void testMinimumSingleElement() {
        BigInteger value = new BigInteger("12345678901234567890");
        BigInteger result = MathUtilities.minimum(value);
        assertEquals(value, result, "minimum should return the only element provided");
    }

    @Test
    @DisplayName("minimum(BigInteger[]) correctly identifies the smallest value among many")
    void testMinimumMultipleElements() {
        BigInteger[] values = { new BigInteger("98765432109876543210"), new BigInteger("-12345678901234567890"), BigInteger.ZERO, new BigInteger("55555555555555555555") };
        BigInteger expected = new BigInteger("-12345678901234567890");
        assertEquals(expected, MathUtilities.minimum(values));
    }

    @Test
    @DisplayName("minimum(BigInteger[]) works with the predefined LONG_MIN and LONG_MAX constants")
    void testMinimumWithLongBoundaryConstants() {
        BigInteger[] values = { MathUtilities.BIG_INT_LONG_MAX, MathUtilities.BIG_INT_LONG_MIN, BigInteger.ONE };
        assertEquals(MathUtilities.BIG_INT_LONG_MIN, MathUtilities.minimum(values));
    }

    @Test
    @DisplayName("minimum(BigInteger[]) throws IllegalArgumentException when a null element is supplied")
    void testMinimumNullElementThrows() {
        BigInteger[] valuesWithNullFirst = { null, BigInteger.ONE };
        BigInteger[] valuesWithNullLater = { BigInteger.ONE, null };
        assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(valuesWithNullFirst), "Expected IllegalArgumentException for null as first element");
        assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(valuesWithNullLater), "Expected IllegalArgumentException for null as later element");
    }

    @Test
    @DisplayName("minimum(BigInteger[]) throws IllegalArgumentException when the sole element is null")
    void testMinimumSingleNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum((BigInteger) null), "Expected IllegalArgumentException when the only argument is null");
    }

    /* --------------------------------------------------------------------- *
     *  1️⃣  SINGLE‑ELEMENT SCENARIOS
     *  --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("singleElementCases")
    @DisplayName("minimum(BigInteger[]) – single element scenarios")
    void testMinimumSingleElementScenarios(TestCase tc) {
        if (tc.expectedException() != null) {
            // The method should throw the expected exception
            assertThrows(tc.expectedException(), () -> MathUtilities.minimum(tc.values()), tc.description());
        } else {
            // Normal execution – verify the returned value
            BigInteger result = MathUtilities.minimum(tc.values());
            assertEquals(tc.expectedResult(), result, tc.description());
        }
    }

    /* --------------------------------------------------------------------- *
     *  2️⃣  MULTIPLE‑ELEMENT SCENARIOS
     *  --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("multipleElementCases")
    @DisplayName("minimum(BigInteger[]) – multiple element scenarios")
    void testMinimumMultipleElementScenarios(TestCase tc) {
        if (tc.expectedException() != null) {
            assertThrows(tc.expectedException(), () -> MathUtilities.minimum(tc.values()), tc.description());
        } else {
            BigInteger result = MathUtilities.minimum(tc.values());
            assertEquals(tc.expectedResult(), result, tc.description());
        }
    }

    /* --------------------------------------------------------------------- *
     *  3️⃣  NULL ARRAY ARGUMENT SCENARIO
     *  --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullArrayCases")
    @DisplayName("minimum(BigInteger[]) – null array argument")
    void testMinimumNullArrayArgument(TestCase tc) {
        assertThrows(tc.expectedException(), () -> MathUtilities.minimum(tc.values()), tc.description());
    }
}
