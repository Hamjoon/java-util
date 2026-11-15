package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toByte_Test {

    /**
     * A {@link Converter} instance – the method under test does not use it.
     */
    private Converter converter;

    /**
     * Mocked options required by the {@link Converter} constructor.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * Simple stub extending {@link Converter} so we can pass a non‑null instance.
     */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            // the superclass only stores the options; null is acceptable for the test
            super(null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter constructor only stores the options; a mock is sufficient.
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
        converter = null;
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// 'A' -> 65
        Arguments.of('A', (byte) 65), // null character -> 0
        Arguments.of('\u0000', (byte) 0));
    }

    private static Stream<Arguments> overflowConversionProvider() {
        return Stream.of(// 0xFF (255) -> -1
        Arguments.of('\u00FF', (byte) -1), // 0x80 (128) -> -128
        Arguments.of('\u0080', (byte) -128));
    }

    private static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(// max positive byte
        Arguments.of('\u007F', (byte) 127), Arguments.of('\u007E', (byte) 126), // 254 -> -2
        Arguments.of('\u00FE', (byte) -2));
    }

    private static Stream<Arguments> highCodePointProvider() {
        return Stream.of(// 256 -> 0
        Arguments.of('\u0100', (byte) 0), // 257 -> 1
        Arguments.of('\u0101', (byte) 1), // 32767 -> 255 -> -1
        Arguments.of('\u7FFF', (byte) -1), // 65535 -> 255 -> -1
        Arguments.of('\uFFFF', (byte) -1));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Integer
        Arguments.of(123, ClassCastException.class), // String
        Arguments.of("string", ClassCastException.class), // null
        Arguments.of(null, NullPointerException.class));
    }

    private static Stream<Arguments> nonNullConverterProvider() {
        return Stream.of(Arguments.of('A', (byte) 65), Arguments.of('\u0080', (byte) -128));
    }

    @Test
    void toByte_givenCharacterWithinByteRange_returnsSameValue() {
        // given
        // 65 decimal, fits in a signed byte
        Character from = 'A';
        // when
        byte result = CharacterConversions.toByte(from, converter);
        // then
        assertEquals((byte) 65, result, "Byte value should match the character code");
    }

    @Test
    void toByte_givenCharacterOutsideByteRange_wrapsAccordingToJavaCastingRules() {
        // given
        // 200 decimal = 0x00C8; casting to byte yields -56 (0xC8 as signed byte)
        Character from = (char) 200;
        // when
        byte result = CharacterConversions.toByte(from, converter);
        // then
        assertEquals((byte) -56, result, "Casting a char >127 to byte should follow Java's two‑s complement overflow rules");
    }

    @Test
    void toByte_givenNonCharacterObject_throwsClassCastException() {
        // given
        // not a Character
        Integer from = 123;
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toByte(from, converter), "A non‑Character object should cause a ClassCastException");
    }

    @Test
    void toByte_givenNullObject_throwsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toByte(from, converter), "Passing null should result in a NullPointerException when casting to char");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("normalConversionProvider")
    void toByte_normalConversion(Object from, byte expected) {
        // when
        byte result = CharacterConversions.toByte(from, null);
        // then
        assertEquals(expected, result, () -> "Converting " + from + " should yield byte value " + expected);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("overflowConversionProvider")
    void toByte_overflowConversion(Object from, byte expected) {
        byte result = CharacterConversions.toByte(from, null);
        assertEquals(expected, result, () -> "Overflow conversion of " + (int) ((char) from) + " should be " + expected);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("boundaryValuesProvider")
    void toByte_boundaryValues(Object from, byte expected) {
        byte result = CharacterConversions.toByte(from, null);
        assertEquals(expected, result, () -> "Boundary value " + (int) ((char) from) + " should become " + expected);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("highCodePointProvider")
    void toByte_highCodePoints(Object from, byte expected) {
        byte result = CharacterConversions.toByte(from, null);
        assertEquals(expected, result, () -> "High code point " + (int) ((char) from) + " should wrap to " + expected);
    }

    @ParameterizedTest(name = "[{index}] from={0} expects {1}")
    @MethodSource("invalidInputProvider")
    void toByte_invalidInput_throwsException(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CharacterConversions.toByte(from, null), () -> "Converting " + from + " should throw " + expectedException.getSimpleName());
    }

    @ParameterizedTest(name = "[{index}] from={0} with non‑null converter → expected={1}")
    @MethodSource("nonNullConverterProvider")
    void toByte_nonNullConverterDoesNotAffectResult(Object from, byte expected) {
        // Use a dummy Converter instance (real Converter works as well)
        Converter dummy = new DummyConverter();
        byte result = CharacterConversions.toByte(from, dummy);
        assertEquals(expected, result, () -> "With a non‑null converter, conversion of " + from + " should still be " + expected);
    }
}
