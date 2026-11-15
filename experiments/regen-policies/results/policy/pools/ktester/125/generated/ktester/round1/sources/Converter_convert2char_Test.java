package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2char_Test {

    private static Stream<Arguments> characterProvider() {
        return Stream.of(Arguments.of(Character.valueOf('A'), 'A'), Arguments.of(Character.valueOf('\u0000'), '\u0000'));
    }

    private static Stream<Arguments> stringProvider() {
        return Stream.of(// single‑character
        Arguments.of("Z", 'Z'), // empty string -> default char
        Arguments.of("", '\u0000'), // first character of longer string
        Arguments.of("Hello", 'H'));
    }

    private static Stream<Arguments> numericProvider() {
        return Stream.of(// 65 -> 'A'
        Arguments.of(Integer.valueOf(65), "Integer", 'A'), // 0L -> null char
        Arguments.of(Long.valueOf(0L), "Long", '\u0000'), // 66 -> 'B'
        Arguments.of(Byte.valueOf((byte) 66), "Byte", 'B'), // 67 -> 'C'
        Arguments.of(Short.valueOf((short) 67), "Short", 'C'), // max char
        Arguments.of(Integer.valueOf(65535), "Integer (max char)", '\uffff'));
    }

    private static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, '1'), Arguments.of(Boolean.FALSE, '0'));
    }

    @Test
    @DisplayName("convert2char returns the null‑character when the source is null")
    void convertNullReturnsZeroChar() {
        // given
        Object source = null;
        // when
        char result = Converter.convert2char(source);
        // then
        assertEquals('\u0000', result, "When the source is null the conversion should return the null‑character (char value 0).");
    }

    @Test
    @DisplayName("convert2char converts a single‑character String to the corresponding char")
    void convertStringToChar() {
        // given
        String source = "Z";
        // when
        char result = Converter.convert2char(source);
        // then
        assertEquals('Z', result, "A one‑character String should be converted to the same char value.");
    }

    @Test
    @DisplayName("convert2char converts an Integer representing a Unicode code point to the corresponding char")
    void convertIntegerToChar() {
        // given
        // Unicode code point for 'A'
        Integer source = 65;
        // when
        char result = Converter.convert2char(source);
        // then
        assertEquals('A', result, "An Integer containing a valid Unicode code point should be converted to the matching char.");
    }

    @Test
    @DisplayName("convert2char converts a Character instance to the same char value")
    void convertCharacterObjectToChar() {
        // given
        Character source = 'm';
        // when
        char result = Converter.convert2char(source);
        // then
        assertEquals('m', result, "A Character object should be converted to its primitive char representation unchanged.");
    }

    @Test
    @DisplayName("convert2char converts a ByteBuffer containing a single UTF‑8 byte to the corresponding char")
    void convertByteBufferToChar() {
        // given
        java.nio.ByteBuffer source = java.nio.ByteBuffer.wrap(new byte[] { 'q' });
        // when
        char result = Converter.convert2char(source);
        // then
        assertEquals('q', result, "A ByteBuffer with a single ASCII byte should be converted to the matching char.");
    }

    @Test
    @DisplayName("convert2char converts a CharBuffer containing a single char to that char")
    void convertCharBufferToChar() {
        // given
        java.nio.CharBuffer source = java.nio.CharBuffer.wrap(new char[] { 'Ω' });
        // when
        char result = Converter.convert2char(source);
        // then
        assertEquals('Ω', result, "A CharBuffer with a single character should be converted to that character.");
    }

    // ---- Character group ----------------------------------------------------
    @ParameterizedTest(name = "[{index}] Character value {0} -> ''{1}''")
    @MethodSource("characterProvider")
    void convertFromCharacter(Character input, char expected) {
        assertEquals(expected, Converter.convert2char(input), "Character instance should be converted unchanged.");
    }

    // ---- String group --------------------------------------------------------
    @ParameterizedTest(name = "[{index}] String \"{0}\" -> ''{1}''")
    @MethodSource("stringProvider")
    void convertFromString(String input, char expected) {
        assertEquals(expected, Converter.convert2char(input), "String conversion should follow the documented rules.");
    }

    // ---- Numeric types group -------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} ({1}) -> ''{2}''")
    @MethodSource("numericProvider")
    void convertFromNumeric(Object input, String typeName, char expected) {
        assertEquals(expected, Converter.convert2char(input), "Numeric conversion should cast the numeric value to char.");
    }

    // ---- Boolean group -------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Boolean {0} -> ''{1}''")
    @MethodSource("booleanProvider")
    void convertFromBoolean(Boolean input, char expected) {
        assertEquals(expected, Converter.convert2char(input), "Boolean true should become '1' and false should become '0'.");
    }

    // ---- CharBuffer group ----------------------------------------------------
    @Test
    @DisplayName("convert2char from CharBuffer returns the character at position 0")
    void convertFromCharBuffer() {
        CharBuffer buffer = CharBuffer.wrap(new char[] { 'X', 'Y' });
        assertEquals('X', Converter.convert2char(buffer), "CharBuffer conversion should return the character at the current position (0).");
    }

    // ---- ByteBuffer group ----------------------------------------------------
    @Test
    @DisplayName("convert2char from ByteBuffer returns the first byte interpreted as unsigned char")
    void convertFromByteBuffer() {
        // 71 -> 'G'
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { (byte) 71 });
        assertEquals('G', Converter.convert2char(buffer), "ByteBuffer conversion should interpret the first byte as an unsigned value and cast to char.");
    }

    // ---- Unsupported type group -----------------------------------------------
    @Test
    @DisplayName("convert2char throws IllegalArgumentException for unsupported source type (Date)")
    void convertUnsupportedTypeThrows() {
        Date unsupported = new Date();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(unsupported), "Conversion from Date to char is not supported and must throw IllegalArgumentException.");
    }
}
