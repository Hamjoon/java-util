package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toByte_Test {

    /**
     * The Converter is not used by {@code BooleanConversions.toByte}, but it must be supplied.
     */
    @Mock
    Converter converter;

    /**
     * The {@code Converter} is not used by {@code BooleanConversions.toByte},
     * but a mock is provided for the non‑null scenarios.
     */
    @Mock
    Converter mockConverter;

    /**
     * Provides a matrix of inputs covering:
     * <ul>
     *   <li>Boolean.TRUE / Boolean.FALSE with a {@code null} converter.</li>
     *   <li>Boolean values with a non‑null mock converter.</li>
     *   <li>Explicit {@code new Boolean(...)} instances.</li>
     *   <li>A converter mock that would throw if used (the method never uses it).</li>
     *   <li>Invalid source types and {@code null} source to verify exception paths.</li>
     * </ul>
     */
    private static Stream<Arguments> provideToByteCases() {
        // A regular (non‑null) mock converter – already supplied by the test class.
        Converter regularMock = mock(Converter.class);
        // A mock that would throw a RuntimeException if any method were called.
        Converter throwingMock = mock(Converter.class);
        // Configure it to throw on any interaction (just to be safe).
        doThrow(new RuntimeException("Converter should not be used")).when(throwingMock).// any method call will trigger the exception
        toString();
        return Stream.of(// true / false with null converter
        Arguments.of(Boolean.TRUE, null, Byte.valueOf((byte) 1)), Arguments.of(Boolean.FALSE, null, Byte.valueOf((byte) 0)), // true / false with a regular non‑null converter
        Arguments.of(Boolean.TRUE, regularMock, Byte.valueOf((byte) 1)), Arguments.of(Boolean.FALSE, regularMock, Byte.valueOf((byte) 0)), // explicit Boolean instances created via `new Boolean(...)`
        Arguments.of(new Boolean(true), null, Byte.valueOf((byte) 1)), Arguments.of(new Boolean(false), null, Byte.valueOf((byte) 0)), // true with a converter that would throw if used
        Arguments.of(Boolean.TRUE, throwingMock, Byte.valueOf((byte) 1)), // invalid source types – expect exceptions
        Arguments.of("true", null, ClassCastException.class), Arguments.of(null, null, NullPointerException.class));
    }

    @Test
    void toByte_givenTrue_returnsByteOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Byte result = BooleanConversions.toByte(source, mockConverter);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "Converting Boolean.TRUE should yield CommonValues.BYTE_ONE");
    }

    @Test
    void toByte_givenFalse_returnsByteZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Byte result = BooleanConversions.toByte(source, mockConverter);
        // then
        assertEquals(CommonValues.BYTE_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.BYTE_ZERO");
    }

    @Test
    void toByte_givenNonBoolean_throwsClassCastException() {
        // given
        // not a Boolean
        Object source = Integer.valueOf(1);
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toByte(source, mockConverter), "Converting a non‑Boolean should throw ClassCastException");
    }

    @Test
    void toByte_givenNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toByte(source, mockConverter), "Converting null should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1} => expected={2}")
    @MethodSource("provideToByteCases")
    void toByte_parameterised(Object from, Converter converter, Object expected) {
        if (expected instanceof Byte) {
            // Normal successful conversion
            Byte result = BooleanConversions.toByte(from, converter);
            assertEquals(expected, result, () -> "Expected conversion result " + expected + " but got " + result);
        } else if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exceptionClass = (Class<? extends Throwable>) expected;
            assertThrows(exceptionClass, () -> BooleanConversions.toByte(from, converter), () -> "Expected exception " + exceptionClass.getSimpleName() + " for input: " + from);
        } else {
            fail("Test data error – unexpected expected value: " + expected);
        }
    }
}
