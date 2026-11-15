package com.cedarsoftware.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_2_Test {

    private static Stream<Arguments> provideNormalCases() {
        return Stream.of(// Single non‑null element – should be returned unchanged
        arguments(new BigDecimal[] { new BigDecimal("42.0") }, new BigDecimal("42.0")), // Minimum is a middle element
        arguments(new BigDecimal[] { new BigDecimal("10.5"), new BigDecimal("-3.2"), new BigDecimal("7.8") }, new BigDecimal("-3.2")), // Minimum is the first element (algorithm must still iterate)
        arguments(new BigDecimal[] { new BigDecimal("-100.0"), new BigDecimal("0"), new BigDecimal("50") }, new BigDecimal("-100.0")), // Minimum is the last element
        arguments(new BigDecimal[] { new BigDecimal("15.2"), new BigDecimal("8.4"), new BigDecimal("-3.7") }, new BigDecimal("-3.7")));
    }

    private static Stream<Arguments> provideIllegalArgumentCases() {
        return Stream.of(// Single null element
        arguments(new BigDecimal[] { null }), // Null element after the first entry
        arguments(new BigDecimal[] { new BigDecimal("5"), null, new BigDecimal("1") }));
    }

    @Test
    void minimum_WithMultipleValues_ReturnsSmallest() {
        BigDecimal a = new BigDecimal("10.5");
        BigDecimal b = new BigDecimal("-3.14");
        BigDecimal c = new BigDecimal("0");
        BigDecimal d = new BigDecimal("7.89");
        assertEquals(b, MathUtilities.minimum(a, b, c, d));
    }

    @Test
    void minimum_WithSingleValue_ReturnsThatValue() {
        BigDecimal only = new BigDecimal("42.42");
        assertEquals(only, MathUtilities.minimum(only));
    }

    @Test
    void minimum_WithAllPositiveValues() {
        assertEquals(new BigDecimal("1"), MathUtilities.minimum(new BigDecimal("5"), new BigDecimal("1"), new BigDecimal("3"), new BigDecimal("9")));
    }

    @Test
    void minimum_WithAllNegativeValues() {
        assertEquals(new BigDecimal("-100"), MathUtilities.minimum(new BigDecimal("-10"), new BigDecimal("-100"), new BigDecimal("-20")));
    }

    @Test
    void minimum_WithMixedScaleValues() {
        // Different scales to ensure min works irrespective of scale
        BigDecimal v1 = new BigDecimal("1.0000");
        BigDecimal v2 = new BigDecimal("0.9999");
        BigDecimal v3 = new BigDecimal("1.0");
        assertEquals(v2, MathUtilities.minimum(v1, v2, v3));
    }

    @Test
    void minimum_WithConstants_FromMathUtilities() {
        // Using the public constants defined in MathUtilities
        assertEquals(MathUtilities.BIG_DEC_DOUBLE_MIN, MathUtilities.minimum(MathUtilities.BIG_DEC_DOUBLE_MAX, MathUtilities.BIG_DEC_DOUBLE_MIN, new BigDecimal("0")));
    }

    @Test
    void minimum_WithNullElement_ThrowsIllegalArgumentException() {
        BigDecimal good = new BigDecimal("1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(good, null, good));
        assertTrue(ex.getMessage().contains("null BigDecimal"));
    }

    @Test
    void minimum_WithFirstElementNull_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(null, new BigDecimal("5")));
        assertTrue(ex.getMessage().contains("null BigDecimal"));
    }

    @Test
    void minimum_WithEmptyArray_ReturnsNullPointerException() {
        // The method does not guard against zero‑length varargs; it will throw
        // an ArrayIndexOutOfBoundsException when trying to access values[0].
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum());
    }

    /**
     * Verify that the method returns the smallest {@link BigDecimal}
     * for a variety of input arrays.
     */
    @ParameterizedTest(name = "{index}: minimum({0}) = {1}")
    @MethodSource("provideNormalCases")
    void minimum_ReturnsSmallestValue(BigDecimal[] values, BigDecimal expected) {
        assertEquals(expected, MathUtilities.minimum(values));
    }

    /**
     * The method must reject any {@code null} entry inside the var‑args
     * array by throwing {@link IllegalArgumentException}.
     */
    @ParameterizedTest(name = "{index}: minimum throws IAE for {0}")
    @MethodSource("provideIllegalArgumentCases")
    void minimum_NullElement_ThrowsIllegalArgumentException(BigDecimal[] values) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(values));
        assertTrue(ex.getMessage().contains("null BigDecimal"), "Exception message should mention null BigDecimal");
    }

    @Test
    void minimum_WithEmptyArray_ThrowsArrayIndexOutOfBoundsException() {
        // The implementation accesses values[0] without checking length.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(new BigDecimal[] {}));
    }

    @Test
    void minimum_WithNullVarargsArray_ThrowsNullPointerException() {
        // Passing a null reference as the var‑args array triggers NPE
        assertThrows(NullPointerException.class, () -> MathUtilities.minimum((BigDecimal[]) null));
    }

    @Test
    void minimum_FirstElementNullWithMultipleEntries_ThrowsNullPointerException() {
        // values[0] is null, so 'current' becomes null.
        // The next iteration calls values[1].min(null) → NPE.
        assertThrows(NullPointerException.class, () -> MathUtilities.minimum(new BigDecimal[] { null, new BigDecimal("5") }));
    }
}
