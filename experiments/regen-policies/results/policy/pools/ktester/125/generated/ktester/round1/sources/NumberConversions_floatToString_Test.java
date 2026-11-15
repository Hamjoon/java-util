package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_floatToString_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    // Zero and negative‑zero cases
    static Stream<Arguments> provideFloatToStringZeroCases() {
        return Stream.of(arguments(0.0f, "0", "Zero literal 0.0f should be converted to \"0\""), arguments(-0.0f, "0", "Negative zero -0.0f compares equal to 0.0f and should be \"0\""));
    }

    // All non‑zero cases (including special constants and wrapped Float objects)
    static Stream<Arguments> provideFloatToStringNonZeroCases() {
        return Stream.of(arguments(1.23f, "1.23", "Typical positive float should be stringified via toString()"), arguments(-4.56f, "-4.56", "Negative non‑zero float must preserve the sign"), arguments(Float.MAX_VALUE, Float.toString(Float.MAX_VALUE), "Maximum float value should be stringified exactly as Float.MAX_VALUE.toString()"), arguments(Float.MIN_VALUE, Float.toString(Float.MIN_VALUE), "Minimum positive non‑zero (subnormal) float should be stringified via toString()"), arguments(Float.NaN, "NaN", "NaN is not zero and its toString() should be returned"), arguments(Float.POSITIVE_INFINITY, "Infinity", "Positive infinity should be returned as \"Infinity\""), arguments(Float.NEGATIVE_INFINITY, "-Infinity", "Negative infinity should be returned as \"-Infinity\""), arguments(new Float(2.5f), "2.5", "Wrapped Float object should be unboxed and its toString() used"), arguments(new Float(-0.0f), "0", "Wrapped Float containing -0.0f should compare equal to 0.0f and return \"0\""));
    }

    // Exceptional cases (null argument, etc.)
    static Stream<Arguments> provideFloatToStringExceptionCases() {
        return Stream.of(arguments(null, NullPointerException.class, "Null 'from' argument should cause NullPointerException due to unboxing"));
    }

    @Test
    void floatZeroIsConvertedToStringZero() {
        // GIVEN
        Float zero = 0.0f;
        // WHEN
        String result = NumberConversions.floatToString(zero, mockConverter);
        // THEN
        assertEquals("0", result, "Zero float should be converted to the literal string \"0\"");
    }

    @Test
    void nonZeroFloatUsesItsOwnToString() {
        // GIVEN
        Float value = 3.14f;
        // WHEN
        String result = NumberConversions.floatToString(value, mockConverter);
        // THEN
        assertEquals(value.toString(), result, "Non‑zero float should be converted using its own toString() representation");
    }

    @Test
    void negativeFloatUsesItsOwnToString() {
        // GIVEN
        Float value = -2.5f;
        // WHEN
        String result = NumberConversions.floatToString(value, mockConverter);
        // THEN
        assertEquals(value.toString(), result, "Negative float should be converted using its own toString() representation");
    }

    @Test
    void passingNonFloatObjectThrowsClassCastException() {
        // GIVEN
        // Double cannot be cast to primitive float directly
        Double notAFloat = 1.23;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> NumberConversions.floatToString(notAFloat, mockConverter), "A non‑float object should cause a ClassCastException");
    }

    /**
     * Tests that zero and negative‑zero values are normalised to the literal "0".
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideFloatToStringZeroCases")
    void floatToStringZeroValues(Object from, String expected, String description) {
        // WHEN
        String result = NumberConversions.floatToString(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Tests a variety of non‑zero float inputs, including special constants.
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideFloatToStringNonZeroCases")
    void floatToStringNonZeroValues(Object from, String expected, String description) {
        // WHEN
        String result = NumberConversions.floatToString(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Tests that a {@code null} value for {@code from} triggers a NullPointerException
     * because of unboxing to a primitive {@code float}.
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideFloatToStringExceptionCases")
    void floatToStringExceptions(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> NumberConversions.floatToString(from, mockConverter), description);
    }
}
