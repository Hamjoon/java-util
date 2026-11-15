package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toBigDecimal_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Boolean constants with a null converter (the usual case)
        Arguments.of(Boolean.TRUE, null, BigDecimal.ONE), Arguments.of(Boolean.FALSE, null, BigDecimal.ZERO), // Explicit Boolean objects created via constructor
        Arguments.of(new Boolean(true), null, BigDecimal.ONE), Arguments.of(new Boolean(false), null, BigDecimal.ZERO), // Using a non‑null (mock) Converter – the method must ignore it
        Arguments.of(Boolean.TRUE, mock(Converter.class), BigDecimal.ONE));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source – unboxing a null Boolean causes NPE
        Arguments.of(null, null, NullPointerException.class), // Wrong type – cast to Boolean fails
        Arguments.of("some string", null, ClassCastException.class));
    }

    // Helper to create a Mockito mock without needing the @Mock field (used in provider)
    private static Converter mock(Class<Converter> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }

    @Test
    void toBigDecimal_whenTrue_returnsOne() {
        Boolean input = Boolean.TRUE;
        BigDecimal result = BooleanConversions.toBigDecimal(input, converter);
        assertEquals(BigDecimal.ONE, result, "Boolean TRUE should convert to BigDecimal.ONE");
    }

    @Test
    void toBigDecimal_whenFalse_returnsZero() {
        Boolean input = Boolean.FALSE;
        BigDecimal result = BooleanConversions.toBigDecimal(input, converter);
        assertEquals(BigDecimal.ZERO, result, "Boolean FALSE should convert to BigDecimal.ZERO");
    }

    @Test
    void toBigDecimal_whenNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BooleanConversions.toBigDecimal(null, converter), "Passing null should result in NullPointerException due to unboxing");
    }

    @Test
    void toBigDecimal_whenNonBoolean_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> BooleanConversions.toBigDecimal("true", converter), "Passing a non‑Boolean object should cause a ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases (returning a BigDecimal)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, converter={1}, expected={2}")
    @MethodSource("successfulConversionProvider")
    void toBigDecimal_successful(Object from, Converter conv, BigDecimal expected) {
        // Act
        BigDecimal result = BooleanConversions.toBigDecimal(from, conv);
        // Assert
        assertEquals(expected, result, () -> String.format("Converting %s should yield %s", from, expected));
    }

    /* -------------------------------------------------------------
     *  Exception test cases (null source or wrong type)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, expectedException={2}")
    @MethodSource("exceptionProvider")
    void toBigDecimal_exceptions(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        // Act & Assert
        assertThrows(expectedException, () -> BooleanConversions.toBigDecimal(from, conv), () -> String.format("Converting %s should throw %s", from, expectedException.getSimpleName()));
    }
}
