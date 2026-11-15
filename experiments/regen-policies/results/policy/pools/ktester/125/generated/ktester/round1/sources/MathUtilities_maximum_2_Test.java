package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_maximum_2_Test {

    /**
     * Single element scenarios **
     */
    static Stream<Arguments> singleElementProvider() {
        return Stream.of(// non‑null single element
        Arguments.of(new String[] { "123.456" }, new BigDecimal("123.456"), null), // single null element – expect IllegalArgumentException
        Arguments.of(new String[] { null }, null, IllegalArgumentException.class));
    }

    /**
     * Multiple element scenarios **
     */
    static Stream<Arguments> multipleElementProvider() {
        return Stream.of(// mixed negative, zero, positive – max 5.25
        Arguments.of(new String[] { "-10.5", "0", "5.25", "3.14" }, new BigDecimal("5.25"), null), // all negative – max -50
        Arguments.of(new String[] { "-100", "-200", "-50" }, new BigDecimal("-50"), null), // identical zeros – max 0
        Arguments.of(new String[] { "0", "0", "0" }, new BigDecimal("0"), null), // same numeric value, different scales – last element returned (scale 0)
        Arguments.of(new String[] { "1.000", "1.0", "1" }, new BigDecimal("1"), null), // very large exponent values – max 1.23E+1000
        Arguments.of(new String[] { "1.23E+1000", "9.99E+999" }, new BigDecimal("1.23E+1000"), null), // very large negative exponent values – max -9.99E+999 (less negative)
        Arguments.of(new String[] { "-1.23E+1000", "-9.99E+999" }, new BigDecimal("-9.99E+999"), null), // negative zero vs positive zero – last element (positive zero) wins
        Arguments.of(new String[] { "-0.0", "0.0" }, new BigDecimal("0.0"), null));
    }

    /**
     * Null element in multi‑array **
     */
    static Stream<Arguments> nullInMultiArrayProvider() {
        return Stream.of(Arguments.of(new String[] { null, "5" }, IllegalArgumentException.class), Arguments.of(new String[] { "5", null }, IllegalArgumentException.class));
    }

    @Test
    @DisplayName("maximum(BigDecimal) with a single non‑null value returns that value")
    void testMaximumSingleElement() {
        BigDecimal value = new BigDecimal("123.456");
        assertSame(value, MathUtilities.maximum(value));
    }

    @Test
    @DisplayName("maximum(BigDecimal) with multiple values returns the greatest")
    void testMaximumMultipleElements() {
        BigDecimal a = new BigDecimal("-10.5");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("42.42");
        BigDecimal d = new BigDecimal("3.14");
        BigDecimal result = MathUtilities.maximum(a, b, c, d);
        assertEquals(c, result);
    }

    @Test
    @DisplayName("maximum(BigDecimal) works with positive, negative and zero values")
    void testMaximumMixedSigns() {
        BigDecimal result = MathUtilities.maximum(new BigDecimal("-1000"), new BigDecimal("0"), new BigDecimal("1000"), new BigDecimal("-500"));
        assertEquals(new BigDecimal("1000"), result);
    }

    @Test
    @DisplayName("maximum(BigDecimal) returns the predefined BIG_DEC_DOUBLE_MAX when it is the greatest")
    void testMaximumWithPredefinedConstant() {
        BigDecimal result = MathUtilities.maximum(MathUtilities.BIG_DEC_DOUBLE_MIN, MathUtilities.BIG_DEC_DOUBLE_MAX, new BigDecimal("1"));
        assertEquals(MathUtilities.BIG_DEC_DOUBLE_MAX, result);
    }

    @Test
    @DisplayName("maximum(BigDecimal) throws IllegalArgumentException when a null entry is present")
    void testMaximumNullEntryThrows() {
        BigDecimal good = new BigDecimal("10");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(good, null, new BigDecimal("20")));
        assertTrue(ex.getMessage().contains("null BigDecimal entry"));
    }

    @Test
    @DisplayName("maximum(BigDecimal) throws IllegalArgumentException when the sole argument is null")
    void testMaximumSingleNullThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum((BigDecimal) null));
        assertTrue(ex.getMessage().contains("null BigDecimal entry"));
    }

    @Test
    @DisplayName("maximum(BigDecimal) works with very large values beyond Double range")
    void testMaximumVeryLargeValues() {
        // Values that exceed Double.MAX_VALUE must be represented as BigDecimal.
        // larger than Double.MAX_VALUE
        BigDecimal huge1 = new BigDecimal("1E309");
        BigDecimal huge2 = new BigDecimal("9.999E308");
        BigDecimal result = MathUtilities.maximum(huge1, huge2);
        assertEquals(huge1, result);
    }

    @ParameterizedTest(name = "{index}: values={0}")
    @MethodSource("singleElementProvider")
    @DisplayName("Single‑element array scenarios")
    void testSingleElementScenarios(String[] rawValues, BigDecimal expectedResult, Class<? extends Throwable> expectedException) {
        // Convert raw string array to BigDecimal array (preserving nulls)
        BigDecimal[] input = rawValues == null ? null : Stream.of(rawValues).map(s -> s == null ? null : new BigDecimal(s)).toArray(BigDecimal[]::new);
        if (expectedException != null) {
            Throwable ex = assertThrows(expectedException, () -> MathUtilities.maximum(input));
            assertTrue(ex.getMessage().contains("null BigDecimal entry"));
        } else {
            BigDecimal result = MathUtilities.maximum(input);
            assertEquals(expectedResult, result);
        }
    }

    @ParameterizedTest(name = "{index}: values={0}")
    @MethodSource("multipleElementProvider")
    @DisplayName("Multiple‑element array scenarios")
    void testMultipleElementScenarios(String[] rawValues, BigDecimal expectedResult, Class<? extends Throwable> expectedException) {
        BigDecimal[] input = Stream.of(rawValues).map(BigDecimal::new).toArray(BigDecimal[]::new);
        if (expectedException != null) {
            assertThrows(expectedException, () -> MathUtilities.maximum(input));
        } else {
            BigDecimal result = MathUtilities.maximum(input);
            assertEquals(expectedResult, result);
        }
    }

    @ParameterizedTest(name = "{index}: values={0}")
    @MethodSource("nullInMultiArrayProvider")
    @DisplayName("Null element in multi‑element array should throw IllegalArgumentException")
    void testNullElementInMultiArray(String[] rawValues, Class<? extends Throwable> expectedException) {
        BigDecimal[] input = Stream.of(rawValues).map(s -> s == null ? null : new BigDecimal(s)).toArray(BigDecimal[]::new);
        Throwable ex = assertThrows(expectedException, () -> MathUtilities.maximum(input));
        assertTrue(ex.getMessage().contains("null BigDecimal entry"));
    }

    /**
     * Empty array scenario **
     */
    @Test
    @DisplayName("Calling maximum with an empty array throws ArrayIndexOutOfBoundsException")
    void testEmptyArrayThrows() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(new BigDecimal[0]));
    }

    /**
     * Null array argument **
     */
    @Test
    @DisplayName("Passing a null var‑args reference throws NullPointerException")
    void testNullArrayArgumentThrows() {
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum((BigDecimal[]) null));
    }
}
