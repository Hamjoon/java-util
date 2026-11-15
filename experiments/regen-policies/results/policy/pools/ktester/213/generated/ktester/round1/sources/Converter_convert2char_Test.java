package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2char_Test {

    static Stream<Arguments> validCharacterProvider() {
        return Stream.of(// Character instance
        Arguments.of('z', 'z'), // Single‑character String
        Arguments.of("X", 'X'));
    }

    static Stream<Arguments> numericInputProvider() {
        return Stream.of(// Integer
        Arguments.of(66, 'B'), // Double whole number
        Arguments.of(97.0, 'a'), // Lowest Unicode code point
        Arguments.of(0, '\u0000'), // Highest char value
        Arguments.of(65535, '\uffff'));
    }

    static Stream<Arguments> numericInvalidProvider() {
        return Stream.of(// Negative int
        Arguments.of(-1), // Out of char range
        Arguments.of(70000), // Non‑whole double
        Arguments.of(65.5), // Negative long
        Arguments.of(-1L), // Long out of range
        Arguments.of(70000L), // Negative short
        Arguments.of((short) -1), // Negative byte
        Arguments.of((byte) -1));
    }

    static Stream<Arguments> invalidStringProvider() {
        return Stream.of(// longer than one char
        Arguments.of("AB"), // empty string
        Arguments.of(""));
    }

    static Stream<Arguments> charBufferProvider() {
        return Stream.of(Arguments.of(CharBuffer.wrap("k"), 'k'));
    }

    static Stream<Arguments> charBufferInvalidProvider() {
        return Stream.of(// multiple chars
        Arguments.of(CharBuffer.wrap("ab")), // empty buffer
        Arguments.of(CharBuffer.allocate(0)));
    }

    static Stream<Arguments> byteBufferProvider() {
        return Stream.of(// ASCII 'A'
        Arguments.of(ByteBuffer.wrap(new byte[] { (byte) 0x41 }), 'A'));
    }

    static Stream<Arguments> byteBufferInvalidProvider() {
        return Stream.of(// empty
        Arguments.of(ByteBuffer.allocate(0)), // multiple bytes
        Arguments.of(ByteBuffer.wrap(new byte[] { (byte) 0x41, (byte) 0x42 })));
    }

    static Stream<Arguments> bigNumberProvider() {
        return Stream.of(// 90 → 'Z'
        Arguments.of(new BigInteger("90"), 'Z'), // 100 → 'd'
        Arguments.of(new BigDecimal("100"), 'd'));
    }

    static Stream<Arguments> bigNumberInvalidProvider() {
        return Stream.of(// non‑whole
        Arguments.of(new BigDecimal("123.45")), // negative
        Arguments.of(new BigInteger("-5")), // out of range
        Arguments.of(new BigInteger("70000")));
    }

    static Stream<Arguments> unsupportedPrimitiveProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE), // non‑integral double
        Arguments.of(3.14159), // non‑whole float
        Arguments.of(Float.valueOf(65.5f)), Arguments.of(Float.NaN), Arguments.of(Float.POSITIVE_INFINITY), Arguments.of(Double.NaN), Arguments.of(Double.NEGATIVE_INFINITY));
    }

    static Stream<Arguments> invalidArrayProvider() {
        return Stream.of(// char[]
        // multiple chars
        Arguments.of(new char[] { 'a', 'b' }), // empty
        Arguments.of(new char[] {}), // byte[]
        // multiple bytes
        Arguments.of(new byte[] { (byte) 0x41, (byte) 0x42 }), // empty
        Arguments.of(new byte[] {}));
    }

    static Stream<Arguments> unsupportedObjectProvider() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(UUID.randomUUID()), Arguments.of(DayOfWeek.MONDAY), Arguments.of(LocalDate.of(2020, 1, 1)), Arguments.of(new Date()), // Collection
        Arguments.of(Arrays.asList('a')));
    }

    @Test
    @DisplayName("convert2char(null) should return the default char value '\\u0000'")
    void testNullInputReturnsDefaultChar() {
        char result = Converter.convert2char(null);
        assertEquals('\u0000', result, "Null input must yield the default char value");
    }

    @Test
    @DisplayName("convert2char(Character) should return the same character")
    void testCharacterInput() {
        Character source = 'Z';
        char result = Converter.convert2char(source);
        assertEquals('Z', result, "Character input must be returned unchanged");
    }

    @Test
    @DisplayName("convert2char(String) with a single character should return that character")
    void testStringSingleCharInput() {
        String source = "m";
        char result = Converter.convert2char(source);
        assertEquals('m', result, "Single‑character String should be converted to that char");
    }

    @Test
    @DisplayName("convert2char(String) with multiple characters should convert the first character")
    void testStringMultiCharInput() {
        String source = "xyz";
        char result = Converter.convert2char(source);
        // The documented behavior for multi‑character strings is to use the first char.
        assertEquals('x', result, "Conversion should use the first character of the String");
    }

    @Test
    @DisplayName("convert2char(Integer) should convert the numeric value to the corresponding Unicode character")
    void testIntegerInput() {
        // Unicode code point for 'A'
        Integer source = 65;
        char result = Converter.convert2char(source);
        assertEquals('A', result, "Integer 65 should be converted to the character 'A'");
    }

    @Test
    @DisplayName("convert2char(Long) should convert the numeric value to the corresponding Unicode character")
    void testLongInput() {
        // Unicode code point for 'a'
        Long source = 97L;
        char result = Converter.convert2char(source);
        assertEquals('a', result, "Long 97 should be converted to the character 'a'");
    }

    @Test
    @DisplayName("convert2char(Byte) should convert the numeric value to the corresponding Unicode character")
    void testByteInput() {
        // Unicode code point for 'B'
        Byte source = 66;
        char result = Converter.convert2char(source);
        assertEquals('B', result, "Byte 66 should be converted to the character 'B'");
    }

    @Test
    @DisplayName("convert2char(Boolean) should map true→'1' and false→'0'")
    void testBooleanInput() {
        assertEquals('1', Converter.convert2char(Boolean.TRUE), "true should map to character '1'");
        assertEquals('0', Converter.convert2char(Boolean.FALSE), "false should map to character '0'");
    }

    // ---------- Valid Character Input ----------
    @ParameterizedTest(name = "{index}: convert2char({0}) → ''{1}''")
    @MethodSource("validCharacterProvider")
    void testValidCharacterInputs(Object input, char expected) {
        assertEquals(expected, Converter.convert2char(input));
    }

    // ---------- Numeric Input ----------
    @ParameterizedTest(name = "{index}: convert2char({0}) → ''{1}''")
    @MethodSource("numericInputProvider")
    void testNumericInputs(Object input, char expected) {
        assertEquals(expected, Converter.convert2char(input));
    }

    // ---------- Numeric Edge Cases that should fail ----------
    @ParameterizedTest(name = "{index}: convert2char({0}) throws IllegalArgumentException")
    @MethodSource("numericInvalidProvider")
    void testNumericInvalidInputs(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(input));
    }

    // ---------- String length validation ----------
    @ParameterizedTest(name = "{index}: convert2char(\"{0}\") throws IllegalArgumentException")
    @MethodSource("invalidStringProvider")
    void testInvalidStringLengths(String input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(input));
    }

    // ---------- CharBuffer inputs ----------
    @ParameterizedTest(name = "{index}: convert2char(CharBuffer) → ''{1}''")
    @MethodSource("charBufferProvider")
    void testCharBufferInputs(CharBuffer buffer, char expected) {
        assertEquals(expected, Converter.convert2char(buffer));
    }

    @ParameterizedTest(name = "{index}: convert2char(CharBuffer) throws IllegalArgumentException")
    @MethodSource("charBufferInvalidProvider")
    void testInvalidCharBuffers(CharBuffer buffer) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(buffer));
    }

    // ---------- ByteBuffer inputs ----------
    @ParameterizedTest(name = "{index}: convert2char(ByteBuffer) → ''{1}''")
    @MethodSource("byteBufferProvider")
    void testByteBufferInputs(ByteBuffer buffer, char expected) {
        assertEquals(expected, Converter.convert2char(buffer));
    }

    @ParameterizedTest(name = "{index}: convert2char(ByteBuffer) throws IllegalArgumentException")
    @MethodSource("byteBufferInvalidProvider")
    void testInvalidByteBuffers(ByteBuffer buffer) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(buffer));
    }

    // ---------- BigInteger & BigDecimal ----------
    @ParameterizedTest(name = "{index}: convert2char({0}) → ''{1}''")
    @MethodSource("bigNumberProvider")
    void testBigNumberInputs(Object input, char expected) {
        assertEquals(expected, Converter.convert2char(input));
    }

    @ParameterizedTest(name = "{index}: convert2char({0}) throws IllegalArgumentException")
    @MethodSource("bigNumberInvalidProvider")
    void testInvalidBigNumbers(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(input));
    }

    // ---------- Primitive wrappers that are unsupported ----------
    @ParameterizedTest(name = "{index}: convert2char({0}) throws IllegalArgumentException")
    @MethodSource("unsupportedPrimitiveProvider")
    void testUnsupportedPrimitiveWrappers(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(input));
    }

    // ---------- Array inputs ----------
    @ParameterizedTest(name = "{index}: convert2char({0}) throws IllegalArgumentException")
    @MethodSource("invalidArrayProvider")
    void testInvalidArrayInputs(Object array) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(array));
    }

    // ---------- Unsupported object types ----------
    @ParameterizedTest(name = "{index}: convert2char({0}) throws IllegalArgumentException")
    @MethodSource("unsupportedObjectProvider")
    void testUnsupportedObjectTypes(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(input));
    }
}
