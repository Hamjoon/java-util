package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_floatToString_Test {

    @Mock
    private Converter // injected by MockitoExtension
    converter;

    static Stream<Arguments> zeroValuesProvider() {
        return Stream.of(Arguments.of(Float.valueOf(0.0f)), Arguments.of(Float.valueOf(-0.0f)));
    }

    static Stream<Arguments> nonZeroFloatValuesProvider() {
        return Stream.of(Arguments.of(Float.valueOf(1.23f), "1.23"), Arguments.of(Float.valueOf(-4.56f), "-4.56"), Arguments.of(Float.NaN, "NaN"), Arguments.of(Float.POSITIVE_INFINITY, "Infinity"), Arguments.of(Float.NEGATIVE_INFINITY, "-Infinity"), // "1.4E-45"
        Arguments.of(Float.valueOf(Float.MIN_VALUE), Float.toString(Float.MIN_VALUE)), // "3.4028235E38"
        Arguments.of(Float.valueOf(Float.MAX_VALUE), Float.toString(Float.MAX_VALUE)), // Converter argument ignored – we pass a throwing converter below
        Arguments.of(Float.valueOf(7.89f), "7.89"));
    }

    static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of(Double.valueOf(0.0)), Arguments.of(Integer.valueOf(42)), Arguments.of(Long.valueOf(123L)), Arguments.of(new java.math.BigDecimal("10.5")));
    }

    @Test
    void zeroFloatReturnsLiteralZeroString() {
        // GIVEN
        Float zero = 0.0f;
        // WHEN
        String result = NumberConversions.floatToString(zero, converter);
        // THEN
        assertEquals("0", result, "A float value of 0.0f should be rendered as the literal \"0\"");
    }

    @Test
    void nonZeroFloatReturnsObjectToString() {
        // GIVEN
        Float value = 3.14159f;
        // Float's toString implementation
        String expected = value.toString();
        // WHEN
        String result = NumberConversions.floatToString(value, converter);
        // THEN
        assertEquals(expected, result, "A non‑zero float should be converted using its toString() representation");
    }

    @Test
    void nonFloatObjectThrowsClassCastException() {
        // GIVEN
        // Not a Float, cannot be cast to primitive float
        Integer notAFloat = 42;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> NumberConversions.floatToString(notAFloat, converter), "Passing a non‑Float object should result in a ClassCastException");
    }

    @Test
    void converterArgumentIsIgnoredEvenIfItThrows() {
        // GIVEN a Converter that throws when its convert method is invoked
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dst) {
                throw new RuntimeException("Should not be called");
            }
        };
        Float value = Float.valueOf(7.89f);
        // WHEN
        String result = NumberConversions.floatToString(value, throwingConverter);
        // THEN
        assertEquals("7.89", result, "floatToString must ignore the Converter argument and simply return the float's toString()");
    }

    @ParameterizedTest(name = "invalid type case: from={0}")
    @MethodSource("invalidTypeProvider")
    void invalidTypeThrowsClassCastException(Object from) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> NumberConversions.floatToString(from, converter), "Passing a non‑Float object should result in a ClassCastException");
    }

    @Test
    void nullFromThrowsNullPointerException() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> NumberConversions.floatToString(null, converter), "Unboxing a null reference to float should result in NullPointerException");
    }
}
