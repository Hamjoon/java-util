package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_4_Test {

    static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of(new double[] { 42.0 }, 42.0), Arguments.of(new double[] { -13.5 }, -13.5), Arguments.of(new double[] { 0.0 }, 0.0), Arguments.of(new double[] { -0.0 }, -0.0), Arguments.of(new double[] { Double.MAX_VALUE }, Double.MAX_VALUE), // Double.MIN_VALUE is the smallest positive non‑zero value (≈4.9E-324)
        Arguments.of(new double[] { Double.MIN_VALUE }, Double.MIN_VALUE));
    }

    static Stream<Arguments> multipleElementsProvider() {
        return Stream.of(Arguments.of(new double[] { 5.0, 3.2, 9.8, -1.1, 0.0 }, -1.1), Arguments.of(new double[] { 10.0, 10.0, 10.0 }, 10.0), Arguments.of(new double[] { 2.5, -0.0, 0.0, -2.5 }, -2.5));
    }

    static Stream<Arguments> nanProvider() {
        return Stream.of(Arguments.of(new double[] { 3.0, Double.NaN, 1.0 }), Arguments.of(new double[] { Double.NaN }));
    }

    static Stream<Arguments> infinityProvider() {
        return Stream.of(// Positive infinity should be ignored when smaller finite numbers exist
        Arguments.of(new double[] { Double.POSITIVE_INFINITY, 100.0, -50.0 }, -50.0), // Negative infinity is the smallest possible value
        Arguments.of(new double[] { Double.NEGATIVE_INFINITY, 0.0, 5.0 }, Double.NEGATIVE_INFINITY), // Both infinities present – -Infinity wins
        Arguments.of(new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }, Double.NEGATIVE_INFINITY));
    }

    static Stream<Arguments> negativeZeroProvider() {
        return Stream.of(Arguments.of(new double[] { 0.0, -0.0 }), Arguments.of(new double[] { -0.0, -0.0, 0.0 }));
    }

    @Test
    void testMinimumWithPositiveAndNegativeValues() {
        double result = MathUtilities.minimum(5.5, 2.2, 9.9);
        assertEquals(2.2, result, 0.0);
        result = MathUtilities.minimum(-3.3, -1.1, -7.7);
        assertEquals(-7.7, result, 0.0);
    }

    @Test
    void testMinimumSingleElementArray() {
        double result = MathUtilities.minimum(42.0);
        assertEquals(42.0, result, 0.0);
    }

    @Test
    void testMinimumWithZeroAndNegatives() {
        double result = MathUtilities.minimum(0.0, -0.0, -5.0, 5.0);
        // -0.0 is considered equal to 0.0, the smallest value is -5.0
        assertEquals(-5.0, result, 0.0);
    }

    @Test
    void testMinimumLargeArray() {
        double[] values = { 45.1, -13.1, 123213123.1, 0.0, -999999.9, 999999.9 };
        double result = MathUtilities.minimum(values);
        assertEquals(-999999.9, result, 0.0);
    }

    @Test
    void testMinimumWithExtremeDoubleValues() {
        double result = MathUtilities.minimum(Double.MAX_VALUE, Double.MIN_VALUE, -Double.MAX_VALUE);
        assertEquals(-Double.MAX_VALUE, result, 0.0);
        result = MathUtilities.minimum(-Double.MAX_VALUE, Double.MAX_VALUE);
        assertEquals(-Double.MAX_VALUE, result, 0.0);
    }

    @Test
    void testMinimumAllEqualValues() {
        double result = MathUtilities.minimum(7.7, 7.7, 7.7);
        assertEquals(7.7, result, 0.0);
    }

    @Test
    void testMinimumWithNaNValues() {
        // According to Math.min, any comparison with NaN returns the other operand.
        // The implementation iterates using Math.min, so NaN values are effectively ignored
        // unless they appear as the first element.
        double result = MathUtilities.minimum(Double.NaN, 1.0, 2.0);
        assertEquals(1.0, result, 0.0);
        result = MathUtilities.minimum(1.0, Double.NaN, 2.0);
        assertEquals(1.0, result, 0.0);
        result = MathUtilities.minimum(1.0, 2.0, Double.NaN);
        assertEquals(1.0, result, 0.0);
    }

    /**
     * 1. Empty var‑args array – should throw ArrayIndexOutOfBoundsException **
     */
    @Test
    void testMinimumEmptyArray() {
        // No arguments -> empty array
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum());
    }

    /**
     * 2. Single‑element arrays – verify the element is returned unchanged **
     */
    @ParameterizedTest(name = "{index}: minimum({0}) = {1}")
    @MethodSource("singleElementProvider")
    void testMinimumSingleElement(double[] input, double expected) {
        double result = MathUtilities.minimum(input);
        assertEquals(expected, result, 0.0);
    }

    /**
     * 3. Multiple‑element arrays – typical mixed values **
     */
    @ParameterizedTest(name = "{index}: minimum{0} = {1}")
    @MethodSource("multipleElementsProvider")
    void testMinimumMultipleElements(double[] input, double expected) {
        double result = MathUtilities.minimum(input);
        assertEquals(expected, result, 0.0);
    }

    /**
     * 4. NaN handling – if any argument is NaN, the result should be NaN **
     */
    @ParameterizedTest(name = "{index}: minimum{0} = NaN")
    @MethodSource("nanProvider")
    void testMinimumWithNaN(double[] input) {
        double result = MathUtilities.minimum(input);
        assertTrue(Double.isNaN(result), "Result should be NaN");
    }

    /**
     * 5. Infinity values – verify correct handling of positive/negative infinity **
     */
    @ParameterizedTest(name = "{index}: minimum{0} = {1}")
    @MethodSource("infinityProvider")
    void testMinimumWithInfinity(double[] input, double expected) {
        double result = MathUtilities.minimum(input);
        assertEquals(expected, result, 0.0);
    }

    /**
     * 6. Negative zero – Math.min preserves the sign when comparing 0.0 and -0.0 **
     */
    @ParameterizedTest(name = "{index}: minimum{0} = -0.0")
    @MethodSource("negativeZeroProvider")
    void testMinimumWithNegativeZero(double[] input) {
        double result = MathUtilities.minimum(input);
        // Verify that the raw bits correspond to -0.0 (sign bit set)
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(result), "Result should be negative zero");
    }

    /**
     * 7. Null array – should throw NullPointerException **
     */
    @Test
    void testMinimumNullArray() {
        assertThrows(NullPointerException.class, () -> MathUtilities.minimum((double[]) null));
    }
}
