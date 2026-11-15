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
// allows non‑static @MethodSource methods
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CharacterConversions_toShort_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    private Stream<Arguments> validConversionProviderGroup1() {
        return Stream.of(// Normal Character 'A' → 65
        Arguments.of(Character.valueOf('A'), false, (short) 65), // Minimum char value (null character) → 0
        Arguments.of(Character.valueOf('\u0000'), false, (short) 0), // Maximum char value 0xFFFF → -1 (sign‑extension)
        Arguments.of(Character.valueOf('\uFFFF'), false, (short) -1), // Non‑null Converter (ignored) with Character 'Z' → 90
        Arguments.of(Character.valueOf('Z'), true, (short) 90));
    }

    private Stream<Arguments> validConversionProviderGroup2() {
        return Stream.of(// Maximum positive char value 0x7FFF → 32767
        Arguments.of(Character.valueOf('\u7FFF'), false, (short) 32767), // Smallest negative short representation (char 0x8000) → -32768
        Arguments.of(Character.valueOf('\u8000'), false, (short) -32768), // High‑surrogate char 0xD800 → -10240
        Arguments.of(Character.valueOf('\uD800'), false, (short) -10240), // Lower‑case 'a' → 97
        Arguments.of(Character.valueOf('a'), false, (short) 97), // Character 'b' with a non‑null Converter (ignored) → 98
        Arguments.of(Character.valueOf('b'), true, (short) 98));
    }

    private Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Integer → ClassCastException
        Arguments.of(Integer.valueOf(123), false, ClassCastException.class), // String → ClassCastException
        Arguments.of("123", false, ClassCastException.class), // null → NullPointerException
        Arguments.of(null, false, NullPointerException.class), // Byte → ClassCastException
        Arguments.of(Byte.valueOf((byte) 10), false, ClassCastException.class), // Boolean → ClassCastException
        Arguments.of(Boolean.TRUE, false, ClassCastException.class), // Double → ClassCastException
        Arguments.of(Double.valueOf(1.0), false, ClassCastException.class), // Short wrapper → ClassCastException
        Arguments.of(Short.valueOf((short) 5), false, ClassCastException.class), // Long wrapper → ClassCastException
        Arguments.of(Long.valueOf(5L), false, ClassCastException.class), // Float wrapper → ClassCastException
        Arguments.of(Float.valueOf(5.0f), false, ClassCastException.class), // Generic Object → ClassCastException
        Arguments.of(new Object(), false, ClassCastException.class), // StringBuilder (implements CharSequence) → ClassCastException
        Arguments.of(new StringBuilder("c"), false, ClassCastException.class), // char array → ClassCastException
        Arguments.of(new char[] { 'a', 'b' }, false, ClassCastException.class));
    }

    @Test
    void toShort_WithCharacter_returnsCorrespondingShort() {
        // given
        // Unicode 65
        Character input = 'A';
        // when
        short result = CharacterConversions.toShort(input, converter);
        // then
        assertEquals((short) 65, result, "The short value should match the char code point");
    }

    @Test
    void toShort_WithDifferentCharacter_returnsCorrespondingShort() {
        // given
        // Unicode 122
        Character input = 'z';
        // when
        short result = CharacterConversions.toShort(input, converter);
        // then
        assertEquals((short) 122, result);
    }

    @Test
    void toShort_WithNonCharacter_throwsClassCastException() {
        // given
        Object input = "not a char";
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toShort(input, converter));
    }

    @Test
    void toShort_WithNull_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toShort(input, converter));
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {2}")
    @MethodSource("validConversionProviderGroup1")
    void toShort_ValidConversions_Group1(Object from, boolean useMockConverter, short expected) {
        Converter conv = useMockConverter ? converter : null;
        short result = CharacterConversions.toShort(from, conv);
        assertEquals(expected, result, () -> "Expected short value " + expected + " for input " + from);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {2}")
    @MethodSource("validConversionProviderGroup2")
    void toShort_ValidConversions_Group2(Object from, boolean useMockConverter, short expected) {
        Converter conv = useMockConverter ? converter : null;
        short result = CharacterConversions.toShort(from, conv);
        assertEquals(expected, result, () -> "Expected short value " + expected + " for input " + from);
    }

    @ParameterizedTest(name = "[{index}] convert {0} throws {2}")
    @MethodSource("invalidConversionProvider")
    void toShort_InvalidConversions(Object from, boolean useMockConverter, Class<? extends Throwable> expectedException) {
        Converter conv = useMockConverter ? converter : null;
        assertThrows(expectedException, () -> CharacterConversions.toShort(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for input " + from);
    }
}
