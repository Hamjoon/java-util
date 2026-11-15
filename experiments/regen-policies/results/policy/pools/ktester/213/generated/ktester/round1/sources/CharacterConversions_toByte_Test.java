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
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toByte_Test {

    @Mock
    private Converter // not used by the method, but required by signature
    mockConverter;

    /* -------------------------------------------------------------
     *  Additional sanity checks – ensure the mock is never used.
     * ------------------------------------------------------------- */
    @AfterEach
    void verifyNoUnexpectedInteractions() {
        // Guarantees that no test accidentally invokes the mock.
        verifyNoMoreInteractions(mockConverter);
    }

    private static Stream<Arguments> provideNormalCharacterConversions() {
        return Stream.of(// 'A' -> 65
        arguments('A', (byte) 65), // 'ÿ' (0x00FF) -> -1 (255 -> -1)
        arguments('\u00FF', (byte) -1), // '\u0080' (128) -> -128
        arguments('\u0080', (byte) -128), // '\u0000' (0) -> 0
        arguments('\u0000', (byte) 0));
    }

    private static Stream<Arguments> provideBoundaryCharacterConversions() {
        return Stream.of(// 0x7F → 127
        arguments('\u007F', (byte) 127), // 0x80 → -128
        arguments('\u0080', (byte) -128), // 0xFF → -1
        arguments('\u00FF', (byte) -1), // 0x100 → 0
        arguments('\u0100', (byte) 0), // 0x1234 → 4660 % 256 = 52
        arguments('\u1234', (byte) 52), // 0x7FFF → 32767 % 256 = 255 → -1
        arguments('\u7FFF', (byte) -1), // 0x8000 → 32768 % 256 = 0
        arguments('\u8000', (byte) 0), // 0xFFFF → 65535 % 256 = 255 → -1
        arguments('\uFFFF', (byte) -1));
    }

    private static Stream<Arguments> provideNonCharacterInputs() {
        return Stream.of(// String
        arguments("A", ClassCastException.class), // Integer
        arguments(123, ClassCastException.class), // generic Object
        arguments(new Object(), ClassCastException.class));
    }

    @Test
    void toByte_WithStandardCharacter_ReturnsCorrespondingByte() {
        // given
        // Unicode 65
        Character input = 'A';
        // when
        byte result = CharacterConversions.toByte(input, mockConverter);
        // then
        assertEquals((byte) 65, result, "Byte value should match the char code point");
    }

    @Test
    void toByte_WithCharacterOverflow_ReturnsWrappedByte() {
        // given
        // Character with code point 200 (0xC8). Byte range is -128..127, so overflow occurs.
        Character input = (char) 200;
        // when
        byte result = CharacterConversions.toByte(input, mockConverter);
        // then
        // 200 as a signed byte is -56 (because 200 - 256 = -56)
        assertEquals((byte) -56, result, "Byte overflow should wrap according to two's‑complement");
    }

    @Test
    void toByte_WithNullInput_ThrowsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toByte(input, mockConverter), "Casting null to char should throw NPE");
    }

    @Test
    void toByte_IgnoresConverterArgument() {
        // given
        Character input = 'Z';
        // The mockConverter could be configured to throw if used, but the method should never call it.
        // No stubbing needed.
        // when
        byte result = CharacterConversions.toByte(input, mockConverter);
        // then
        assertEquals((byte) 'Z', result);
        // Verify that no interactions occurred with the mock (optional safety check)
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Normal character to byte conversions (within and just beyond
     *  the signed‑byte range). Expected result is a primitive byte.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected byte={1}")
    @MethodSource("provideNormalCharacterConversions")
    void toByte_NormalCharacterConversions(Object from, byte expected) {
        // when
        byte result = CharacterConversions.toByte(from, mockConverter);
        // then
        assertEquals(expected, result, () -> "Casting char '" + from + "' should yield byte " + expected);
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Boundary character values beyond the byte range.
     *  These verify the two‑s complement truncation behaviour.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=''{0}'' → expected byte={1}")
    @MethodSource("provideBoundaryCharacterConversions")
    void toByte_BoundaryCharacterConversions(Object from, byte expected) {
        byte result = CharacterConversions.toByte(from, mockConverter);
        assertEquals(expected, result, () -> "Boundary char '" + from + "' should map to byte " + expected);
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Non‑character inputs – should raise ClassCastException.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} (type {1}) → expects {2}")
    @MethodSource("provideNonCharacterInputs")
    void toByte_NonCharacterInputs(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CharacterConversions.toByte(from, mockConverter), () -> "Passing a non‑character source should throw " + expectedException.getSimpleName());
        // Even on exception the converter must remain untouched.
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Null input handling – should raise NullPointerException.
     * ------------------------------------------------------------- */
    @Test
    void toByte_NullInput_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CharacterConversions.toByte(null, mockConverter), "Casting null to char should throw NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Character wrapper (autoboxed) conversion.
     * ------------------------------------------------------------- */
    @Test
    void toByte_WrapperCharacterConversion() {
        // value 66
        Character wrapper = new Character('B');
        byte result = CharacterConversions.toByte(wrapper, mockConverter);
        assertEquals((byte) 66, result, "Wrapper Character 'B' should convert to byte 66");
        verifyNoInteractions(mockConverter);
    }
}
