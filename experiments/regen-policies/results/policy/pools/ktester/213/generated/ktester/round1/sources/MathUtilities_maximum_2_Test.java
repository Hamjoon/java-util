package com.cedarsoftware.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

class MathUtilities_maximum_2_Test {

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of("Single non‑null BigDecimal should be returned unchanged.", new String[] { "123.456" }, "123.456"), Arguments.of("Single null entry must trigger IllegalArgumentException.", new String[] { null }, "IllegalArgumentException"));
    }

    private static Stream<Arguments> multipleElementProvider() {
        return Stream.of(Arguments.of("Maximum value is in the middle of the array.", new String[] { "5.5", "2.2", "9.9", "3.3" }, "9.9"), Arguments.of("Zero is the greatest among negative numbers and zero.", new String[] { "0", "-10.5", "-3.3", "-0.1" }, "0"), Arguments.of("All values are negative; the least negative (closest to zero) should be returned.", new String[] { "-100.0", "-200.0", "-50.0" }, "-50.0"), Arguments.of("All entries are equal; the method should return the last equal entry.", new String[] { "123.456", "123.456", "123.456" }, "123.456"), Arguments.of("Equal numeric values with different scales – the last equal argument should be returned.", new String[] { "1.0", "1.00", "0.9999" }, "1.00"));
    }

    private static Stream<Arguments> nullInMultiProvider() {
        return Stream.of(Arguments.of("A null entry among multiple values must trigger IllegalArgumentException.", new String[] { "10", null, "5" }, "IllegalArgumentException"));
    }

    private static Stream<Arguments> boundaryValueProvider() {
        return Stream.of(Arguments.of("Maximum of the double extreme values represented as BigDecimal.", new String[] { "1.7976931348623157E308", "-1.7976931348623157E308" }, "1.7976931348623157E308"), Arguments.of("Positive double max should be selected even when negative max appears first.", new String[] { "-1.7976931348623157E308", "1.7976931348623157E308" }, "1.7976931348623157E308"), Arguments.of("Both entries are the same extreme negative value; the last one is returned.", new String[] { "-1.7976931348623157E308", "-1.7976931348623157E308" }, "-1.7976931348623157E308"));
    }

    /**
     * Converts an array of {@link String} representations (or {@code null}) to a
     * {@link BigDecimal} array suitable for the var‑args method.
     */
    private static BigDecimal[] toBigDecimalArray(String[] strings) {
        return Arrays.stream(strings).map(s -> s == null ? null : new BigDecimal(s)).toArray(BigDecimal[]::new);
    }

    @Test
    void maximumSingleElementReturnsSameInstance() {
        BigDecimal value = new BigDecimal("12345.6789");
        BigDecimal result = MathUtilities.maximum(value);
        // The method should return the exact object supplied when the array length is 1.
        assertSame(value, result, "Maximum of a single element should return that element");
    }

    @Test
    void maximumMultipleElementsFindsGreatest() {
        BigDecimal a = new BigDecimal("-10.5");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("42.42");
        BigDecimal d = new BigDecimal("3.14159");
        BigDecimal result = MathUtilities.maximum(a, b, c, d);
        assertEquals(c, result, "Maximum should be the greatest BigDecimal value");
    }

    @Test
    void maximumWithAllNegativeValues() {
        BigDecimal a = new BigDecimal("-1000");
        BigDecimal b = new BigDecimal("-2000");
        BigDecimal c = new BigDecimal("-3.14");
        BigDecimal d = new BigDecimal("-999.999");
        BigDecimal result = MathUtilities.maximum(a, b, c, d);
        assertEquals(c, result, "Maximum should correctly handle all‑negative numbers");
    }

    @Test
    void maximumWithIdenticalValues() {
        BigDecimal val = new BigDecimal("7.777");
        BigDecimal result = MathUtilities.maximum(val, val, val);
        assertEquals(val, result, "Maximum of identical values must return that value");
    }

    @Test
    void maximumWithBigDecimalConstants() {
        // Use the constants defined in MathUtilities to ensure they are handled correctly.
        BigDecimal max = MathUtilities.maximum(MathUtilities.BIG_DEC_DOUBLE_MIN, MathUtilities.BIG_DEC_DOUBLE_MAX, new BigDecimal("0"));
        assertEquals(MathUtilities.BIG_DEC_DOUBLE_MAX, max, "Maximum should return the larger of the predefined constants");
    }

    @Test
    void maximumThrowsWhenNullElementPresent() {
        BigDecimal a = new BigDecimal("1");
        BigDecimal b = null;
        BigDecimal c = new BigDecimal("2");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(a, b, c), "Passing a null entry must raise IllegalArgumentException");
        assertTrue(ex.getMessage().contains("null BigDecimal entry"), "Exception message should mention null BigDecimal entry");
    }

    @Test
    void maximumThrowsWhenFirstElementIsNull() {
        BigDecimal[] array = { null, new BigDecimal("5") };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(array), "Null as the first element must raise IllegalArgumentException");
        assertTrue(ex.getMessage().contains("null BigDecimal entry"));
    }

    @Test
    void maximumWithVeryLargeAndVerySmallValues() {
        // -Double.MAX_VALUE as BigDecimal
        BigDecimal verySmall = MathUtilities.BIG_DEC_DOUBLE_MIN;
        //  Double.MAX_VALUE as BigDecimal
        BigDecimal veryLarge = MathUtilities.BIG_DEC_DOUBLE_MAX;
        BigDecimal middle = new BigDecimal("0");
        BigDecimal result = MathUtilities.maximum(verySmall, middle, veryLarge);
        assertEquals(veryLarge, result, "Maximum should correctly identify the largest extreme value");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("singleElementProvider")
    void testSingleElementScenarios(String description, String[] inputs, String expected) {
        if ("IllegalArgumentException".equals(expected)) {
            // single null entry should trigger IllegalArgumentException
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(toBigDecimalArray(inputs)), description);
            assertTrue(ex.getMessage().contains("null BigDecimal entry"));
        } else {
            BigDecimal[] values = toBigDecimalArray(inputs);
            BigDecimal result = MathUtilities.maximum(values);
            assertEquals(new BigDecimal(expected), result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("multipleElementProvider")
    void testMultipleElementScenarios(String description, String[] inputs, String expected) {
        BigDecimal[] values = toBigDecimalArray(inputs);
        BigDecimal result = MathUtilities.maximum(values);
        assertEquals(new BigDecimal(expected), result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullInMultiProvider")
    void testNullHandlingInMultiElement(String description, String[] inputs, String expectedException) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(toBigDecimalArray(inputs)), description);
        assertTrue(ex.getMessage().contains("null BigDecimal entry"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryValueProvider")
    void testBoundaryValueScenarios(String description, String[] inputs, String expected) {
        BigDecimal[] values = toBigDecimalArray(inputs);
        BigDecimal result = MathUtilities.maximum(values);
        assertEquals(new BigDecimal(expected), result, description);
    }

    // -----------------------------------------------------------------
    @Test
    void testEmptyInputThrowsArrayIndexOutOfBoundsException() {
        // The method accesses values[0] without checking length, so an empty array triggers AIOOBE.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(), "Calling maximum() with no arguments should result in ArrayIndexOutOfBoundsException");
    }

    // -----------------------------------------------------------------
    @Test
    void testNullArrayThrowsNullPointerException() {
        // Passing a null reference for the var‑args array leads to a NPE when accessing length.
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum((BigDecimal[]) null), "Passing a null array reference should trigger NullPointerException");
    }

    // -----------------------------------------------------------------
    @Test
    void testFirstElementNullInMultiElementThrowsNullPointerException() {
        // Here the first element is null but the array length > 1.
        // The code will set `current = values[0]` (null) and later call max on a non‑null value,
        // which throws NPE inside BigDecimal.max().
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum(null, new BigDecimal("5.0")), "First element null with more than one argument should cause NullPointerException");
    }
}
