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
     * Provides test arguments for single‑element and multiple‑element scenarios.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>String[] – the textual representation of the BigDecimal values to pass</li>
     *   <li>Class&lt;? extends Throwable&gt; – the expected exception type (or {@code null} if none)</li>
     *   <li>String – the expected result as a string (ignored when an exception is expected)</li>
     * </ul>
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> maximumProvider() {
        return Stream.of(// Single element cases
        org.junit.jupiter.params.provider.Arguments.of(new String[] { "5.0" }, null, "5.0"), org.junit.jupiter.params.provider.Arguments.of(new String[] { null }, IllegalArgumentException.class, "Cannot pass null BigDecimal entry to maximum()"), // Multiple element cases
        org.junit.jupiter.params.provider.Arguments.of(new String[] { "1.5", "2.5", "3.0" }, null, "3.0"), org.junit.jupiter.params.provider.Arguments.of(new String[] { "-10.0", "-5.0", "-7.5" }, null, "-5.0"), org.junit.jupiter.params.provider.Arguments.of(new String[] { "0", "0", "0" }, null, "0"), org.junit.jupiter.params.provider.Arguments.of(new String[] { "123456789123456789.123456789", "123456789123456789.123456788" }, null, "123456789123456789.123456789"), org.junit.jupiter.params.provider.Arguments.of(new String[] { "1.0", null, "2.0" }, IllegalArgumentException.class, "Cannot pass null BigDecimal entry to maximum()"), // Empty array – the implementation will throw ArrayIndexOutOfBoundsException
        org.junit.jupiter.params.provider.Arguments.of(new String[] {}, ArrayIndexOutOfBoundsException.class, null), // Null var‑args array – handled separately in a dedicated test
        org.junit.jupiter.params.provider.Arguments.of(null, NullPointerException.class, null));
    }

    @Test
    @DisplayName("maximum(BigDecimal) returns the same value when only one element is supplied")
    void maximum_singleElement_returnsSame() {
        BigDecimal value = new BigDecimal("123.456");
        BigDecimal result = MathUtilities.maximum(value);
        assertEquals(value, result, "Maximum of a single element should be the element itself");
    }

    @Test
    @DisplayName("maximum(BigDecimal) throws IllegalArgumentException when the single element is null")
    void maximum_singleNull_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum((BigDecimal) null));
        assertEquals("Cannot pass null BigDecimal entry to maximum()", ex.getMessage());
    }

    @Test
    @DisplayName("maximum(BigDecimal...) returns the greatest value among multiple elements")
    void maximum_multipleElements_returnsGreatest() {
        BigDecimal a = new BigDecimal("-10.5");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("42.42");
        BigDecimal d = new BigDecimal("3.14159");
        BigDecimal result = MathUtilities.maximum(a, b, c, d);
        assertEquals(c, result, "Maximum should be the greatest BigDecimal value");
    }

    @Test
    @DisplayName("maximum(BigDecimal...) throws IllegalArgumentException when any element is null")
    void maximum_containsNull_throwsException() {
        BigDecimal a = new BigDecimal("1");
        BigDecimal b = null;
        BigDecimal c = new BigDecimal("2");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.maximum(a, b, c));
        assertEquals("Cannot pass null BigDecimal entry to maximum()", ex.getMessage());
    }

    @Test
    @DisplayName("maximum(BigDecimal...) with all equal values returns that value")
    void maximum_allEqual_returnsSame() {
        BigDecimal value = new BigDecimal("99.99");
        BigDecimal result = MathUtilities.maximum(value, value, value);
        assertEquals(value, result, "Maximum of equal values should be that value");
    }

    @Test
    @DisplayName("maximum(BigDecimal...) with varargs array being null throws NullPointerException")
    void maximum_nullArray_throwsNPE() {
        // Directly passing a null reference as the varargs array
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum((BigDecimal[]) null));
    }

    @ParameterizedTest(name = "{index}: values={0}")
    @MethodSource("maximumProvider")
    @DisplayName("Parameterized tests for MathUtilities.maximum(BigDecimal...)")
    void maximum_parameterized(String[] rawValues, Class<? extends Throwable> expectedException, String expectedResult) {
        // Convert raw string array to BigDecimal array, preserving nulls
        BigDecimal[] values = null;
        if (rawValues != null) {
            values = new BigDecimal[rawValues.length];
            for (int i = 0; i < rawValues.length; i++) {
                values[i] = rawValues[i] == null ? null : new BigDecimal(rawValues[i]);
            }
        }
        if (expectedException != null) {
            // When an exception type is expected, verify it is thrown with the correct message (if any)
            Throwable thrown = assertThrows(expectedException, () -> MathUtilities.maximum(values));
            if (expectedException == IllegalArgumentException.class && expectedResult != null) {
                assertEquals(expectedResult, thrown.getMessage(), "Exception message should match the specification");
            }
        } else {
            // No exception expected – verify the returned value
            BigDecimal result = MathUtilities.maximum(values);
            assertEquals(new BigDecimal(expectedResult), result, "Maximum should match the expected value");
        }
    }

    @Test
    @DisplayName("maximum(BigDecimal...) with empty array throws ArrayIndexOutOfBoundsException")
    void maximum_emptyArray_throwsAIOOBE() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(new BigDecimal[] {}));
    }
}
