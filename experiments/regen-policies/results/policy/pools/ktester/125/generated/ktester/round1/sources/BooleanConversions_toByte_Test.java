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
class BooleanConversions_toByte_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    @Mock
    private Converter // injected by Mockito; used for the "non‑null converter" cases
    mockConverter;

    /* --------------------------------------------------------------
     *  Helper placeholder to represent a non‑null Converter in the
     *  parameterised source. The actual mock instance is injected by
     *  Mockito, but we cannot reference it directly in the static method.
     * -------------------------------------------------------------- */
    private static final class MockedConverterPlaceholder implements Converter {
        // No implementation needed – the test never invokes any method on it.
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Boolean.TRUE with a null converter
        Arguments.of(Boolean.TRUE, null, (byte) 1), // Boolean.FALSE with a null converter
        Arguments.of(Boolean.FALSE, null, (byte) 0), // Boolean.TRUE with a non‑null (mock) converter – method ignores it
        Arguments.of(Boolean.TRUE, new MockedConverterPlaceholder(), (byte) 1), // Boolean.FALSE with a non‑null (mock) converter – method ignores it
        Arguments.of(Boolean.FALSE, new MockedConverterPlaceholder(), (byte) 0));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        return Stream.of(// Null source object – NPE because of unboxing in the ternary operator
        Arguments.of(null, NullPointerException.class), // Incompatible type (Integer) – ClassCastException at the cast to Boolean
        Arguments.of(Integer.valueOf(42), ClassCastException.class));
    }

    @Test
    void toByte_trueReturnsByteOne() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        Byte result = BooleanConversions.toByte(input, converter);
        // then
        assertEquals(Byte.valueOf((byte) 1), result, "true should convert to Byte 1");
    }

    @Test
    void toByte_falseReturnsByteZero() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        Byte result = BooleanConversions.toByte(input, converter);
        // then
        assertEquals(Byte.valueOf((byte) 0), result, "false should convert to Byte 0");
    }

    @Test
    void toByte_nullThrowsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toByte(input, converter), "Passing null should result in NullPointerException");
    }

    @Test
    void toByte_nonBooleanThrowsClassCastException() {
        // given
        Object input = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toByte(input, converter), "Passing a non‑Boolean object should result in ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Successful conversion cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: from={0}, converter={1} => expected={2}")
    @MethodSource("provideSuccessfulConversions")
    void toByte_successfulConversion(Object from, Converter converter, byte expected) {
        // when
        Byte result = BooleanConversions.toByte(from, converter);
        // then
        assertEquals(Byte.valueOf(expected), result, () -> String.format("Converting %s with %s should yield %d", from, (converter == null ? "null" : "mockConverter"), expected));
    }

    /* --------------------------------------------------------------
     *  Exception handling cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: from={0} => throws {1}")
    @MethodSource("provideExceptionScenarios")
    void toByte_exceptionScenarios(Object from, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> BooleanConversions.toByte(from, null), () -> String.format("Converting %s should throw %s", from, expectedException.getSimpleName()));
    }
}
