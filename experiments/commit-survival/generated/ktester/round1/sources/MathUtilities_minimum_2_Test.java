package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_2_Test {

    private static Stream<BigDecimal> singleValidProvider() {
        return Stream.of(new BigDecimal("5"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> multipleNonNullProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(new BigDecimal[] { new BigDecimal("10"), new BigDecimal("3"), new BigDecimal("7"), new BigDecimal("3.5") }, new BigDecimal("3")));
    }

    @Test
    void minimum_singleNonNullValue_returnsSameValue() {
        // given
        BigDecimal value = new BigDecimal("123.456");
        // when
        BigDecimal result = MathUtilities.minimum(value);
        // then
        assertEquals(value, result, "When a single non‑null BigDecimal is supplied, the method should return that value unchanged.");
    }

    @Test
    void minimum_multipleValues_returnsSmallest() {
        // given
        BigDecimal v1 = new BigDecimal("10.5");
        BigDecimal v2 = new BigDecimal("-2.3");
        BigDecimal v3 = new BigDecimal("0.0");
        // smallest
        BigDecimal v4 = new BigDecimal("-2.4");
        // when
        BigDecimal result = MathUtilities.minimum(v1, v2, v3, v4);
        // then
        assertEquals(v4, result, "The method should return the smallest BigDecimal among the supplied arguments.");
    }

    @Test
    void minimum_containsNull_throwsIllegalArgumentException() {
        // given
        BigDecimal v1 = new BigDecimal("5");
        BigDecimal v2 = null;
        BigDecimal v3 = new BigDecimal("1");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(v1, v2, v3));
        assertEquals("Cannot passed null BigDecimal entry to minimum()", ex.getMessage(), "The exception message should match the contract defined in the method.");
    }

    @Test
    void minimum_nullSingleArgument_throwsIllegalArgumentException() {
        // given
        BigDecimal nullValue = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(nullValue));
        assertEquals("Cannot passed null BigDecimal entry to minimum()", ex.getMessage(), "A single null argument must also trigger the IllegalArgumentException.");
    }

    @Test
    void minimum_emptyArray_throwsArrayIndexOutOfBoundsException() {
        // given
        BigDecimal[] empty = new BigDecimal[0];
        // when / then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(empty), "Calling minimum() with an empty array should result in an ArrayIndexOutOfBoundsException " + "because the implementation accesses values[0] without checking length.");
    }

    /**
     * Group: Single element - valid value (already covered by {@link #minimum_singleNonNullValue_returnsSameValue()}).
     * Keeping a dedicated parameterised version for completeness.
     */
    @ParameterizedTest(name = "[{index}] single element valid: {0}")
    @MethodSource("singleValidProvider")
    void minimum_singleValidValue_returnsItself(BigDecimal input) {
        // when
        BigDecimal result = MathUtilities.minimum(input);
        // then
        assertEquals(input, result, "A single non‑null element must be returned unchanged.");
    }

    /**
     * Group: Single element - null value.
     */
    @Test
    void minimum_singleNullValue_throwsIllegalArgumentException() {
        // given
        BigDecimal nullValue = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(nullValue));
        assertEquals("Cannot passed null BigDecimal entry to minimum()", ex.getMessage(), "Method must reject a solitary null argument.");
    }

    /**
     * Group: Multiple elements - all non‑null.
     */
    @ParameterizedTest(name = "[{index}] multiple non‑null values: {0}")
    @MethodSource("multipleNonNullProvider")
    void minimum_multipleNonNullValues_returnsSmallest(BigDecimal[] inputs, BigDecimal expected) {
        // when
        BigDecimal result = MathUtilities.minimum(inputs);
        // then
        assertEquals(expected, result, "The smallest element among the supplied non‑null values should be returned.");
    }

    /**
     * Group: Multiple elements - null in middle.
     */
    @Test
    void minimum_nullInMiddle_throwsIllegalArgumentException() {
        // given
        BigDecimal[] values = new BigDecimal[] { new BigDecimal("10"), null, new BigDecimal("5") };
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(values));
        assertEquals("Cannot passed null BigDecimal entry to minimum()", ex.getMessage(), "Encountering a null during iteration must raise IllegalArgumentException.");
    }

    /**
     * Group: Multiple elements - null as first element.
     * The implementation does not guard the first element, so a
     * {@link NullPointerException} is expected when the second element
     * attempts to call {@code min(current)}.
     */
    @Test
    void minimum_nullAsFirstElement_throwsNullPointerException() {
        // given
        BigDecimal[] values = new BigDecimal[] { null, new BigDecimal("1"), new BigDecimal("2") };
        // when / then
        assertThrows(NullPointerException.class, () -> MathUtilities.minimum(values), "A null first element leads to NPE during the first comparison.");
    }

    /**
     * Group: Multiple elements - min at first position.
     */
    @Test
    void minimum_minimumAtFirstPosition_returnsFirstElement() {
        // given
        BigDecimal[] values = new BigDecimal[] { // smallest, at index 0
        new BigDecimal("-5.5"), new BigDecimal("10"), new BigDecimal("3.2"), new BigDecimal("7.8") };
        // when
        BigDecimal result = MathUtilities.minimum(values);
        // then
        assertEquals(new BigDecimal("-5.5"), result, "When the smallest value is the first element, it should be returned unchanged.");
    }
}
