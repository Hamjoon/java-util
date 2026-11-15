package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToCharacter_Test {

    static Stream<Arguments> nullAndDirectProvider() {
        return Stream.of(// null input → null result
        Arguments.of("null input should return null", null, null), // already a Character → same value
        Arguments.of("Character input should be returned unchanged", Character.valueOf('x'), Character.valueOf('x')));
    }

    static Stream<Arguments> stringInputProvider() {
        return Stream.of(Arguments.of("Empty string should throw IllegalArgumentException", "", IllegalArgumentException.class), Arguments.of("Single‑character string should convert to that character", "a", Character.valueOf('a')), Arguments.of("String longer than one character should throw IllegalArgumentException", "ab", IllegalArgumentException.class), Arguments.of("String containing a space should convert to a space character", " ", Character.valueOf(' ')));
    }

    static Stream<Arguments> numericInputProvider() {
        return Stream.of(Arguments.of("Integer 65 → 'A'", Integer.valueOf(65), Character.valueOf('A')), Arguments.of("Integer 0 → '\\u0000'", Integer.valueOf(0), Character.valueOf('\u0000')), Arguments.of("Negative integer should throw IllegalArgumentException", Integer.valueOf(-1), IllegalArgumentException.class), Arguments.of("Maximum char code 65535 → '\\uffff'", Integer.valueOf(65535), Character.valueOf('\uffff')), Arguments.of("Value 65536 (out of range) should throw IllegalArgumentException", Integer.valueOf(65536), IllegalArgumentException.class), Arguments.of("Long 65L → 'A'", Long.valueOf(65L), Character.valueOf('A')), // short
        Arguments.of("Short 70 → 'F'", Short.valueOf((short) 70), Character.valueOf('F')), // byte
        Arguments.of("Byte 71 → 'G'", Byte.valueOf((byte) 71), Character.valueOf('G')));
    }

    static Stream<Arguments> booleanInputProvider() {
        return Stream.of(Arguments.of("Boolean true should throw IllegalArgumentException", Boolean.TRUE, IllegalArgumentException.class), Arguments.of("Boolean false should throw IllegalArgumentException", Boolean.FALSE, IllegalArgumentException.class));
    }

    static Stream<Arguments> charBufferProvider() {
        return Stream.of(Arguments.of("CharBuffer with single char 'z' → 'z'", CharBuffer.wrap("z"), Character.valueOf('z')), Arguments.of("Empty CharBuffer should throw IllegalArgumentException", CharBuffer.allocate(0), IllegalArgumentException.class));
    }

    static Stream<Arguments> byteBufferProvider() {
        return Stream.of(Arguments.of("ByteBuffer with single byte 'x' → 'x'", ByteBuffer.wrap(new byte[] { (byte) 'x' }), Character.valueOf('x')), Arguments.of("ByteBuffer with multiple bytes, first is 'a' → 'a'", ByteBuffer.wrap(new byte[] { (byte) 'a', (byte) 'b' }), Character.valueOf('a')), Arguments.of("Empty ByteBuffer should throw IllegalArgumentException", ByteBuffer.allocate(0), IllegalArgumentException.class));
    }

    static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of("java.util.Date should throw IllegalArgumentException", new java.util.Date(), IllegalArgumentException.class), Arguments.of("java.util.UUID should throw IllegalArgumentException", java.util.UUID.randomUUID(), IllegalArgumentException.class));
    }

    static Stream<Arguments> byteInputProvider() {
        return Stream.of(Arguments.of("Negative byte value should throw IllegalArgumentException", Byte.valueOf((byte) -1), IllegalArgumentException.class));
    }

    static Stream<Arguments> shortInputProvider() {
        return Stream.of(Arguments.of("Negative short value should throw IllegalArgumentException", Short.valueOf((short) -1), IllegalArgumentException.class));
    }

    static Stream<Arguments> floatingPointProvider() {
        return Stream.of(Arguments.of("Float 65.5f should throw IllegalArgumentException", Float.valueOf(65.5f), IllegalArgumentException.class), Arguments.of("Double 65.5 should throw IllegalArgumentException", Double.valueOf(65.5), IllegalArgumentException.class));
    }

    static Stream<Arguments> bigIntegerProvider() {
        return Stream.of(Arguments.of("Negative BigInteger should throw IllegalArgumentException", new BigInteger("-1"), IllegalArgumentException.class), Arguments.of("BigInteger > 65535 should throw IllegalArgumentException", new BigInteger("70000"), IllegalArgumentException.class));
    }

    static Stream<Arguments> bigDecimalProvider() {
        return Stream.of(Arguments.of("Negative BigDecimal should throw IllegalArgumentException", new BigDecimal("-1"), IllegalArgumentException.class), Arguments.of("Fractional BigDecimal should throw IllegalArgumentException", new BigDecimal("65.5"), IllegalArgumentException.class), Arguments.of("BigDecimal > 65535 should throw IllegalArgumentException", new BigDecimal("70000"), IllegalArgumentException.class));
    }

    static Stream<Arguments> atomicNumericProvider() {
        return Stream.of(Arguments.of("AtomicInteger with -1 should throw IllegalArgumentException", new AtomicInteger(-1), IllegalArgumentException.class), Arguments.of("AtomicLong with -1L should throw IllegalArgumentException", new AtomicLong(-1L), IllegalArgumentException.class));
    }

    static Stream<Arguments> charArrayProvider() {
        return Stream.of(Arguments.of("Empty char[] should throw IllegalArgumentException", new char[] {}, IllegalArgumentException.class), Arguments.of("Char array with more than one element should throw IllegalArgumentException", new char[] { 'a', 'b' }, IllegalArgumentException.class));
    }

    static Stream<Arguments> stringBuilderProvider() {
        return Stream.of(Arguments.of("Empty StringBuilder should throw IllegalArgumentException", new StringBuilder(""), IllegalArgumentException.class), Arguments.of("StringBuilder with >1 char should throw IllegalArgumentException", new StringBuilder("ab"), IllegalArgumentException.class));
    }

    static Stream<Arguments> stringBufferProvider() {
        return Stream.of(Arguments.of("Empty StringBuffer should throw IllegalArgumentException", new StringBuffer(""), IllegalArgumentException.class), Arguments.of("StringBuffer with >1 char should throw IllegalArgumentException", new StringBuffer("xy"), IllegalArgumentException.class));
    }

    @Test
    @DisplayName("convertToCharacter returns null when source is null")
    void testNullInput() {
        assertNull(Converter.convertToCharacter(null), "Converting a null source should yield null for object return types");
    }

    @Test
    @DisplayName("convertToCharacter returns the same Character instance")
    void testCharacterInput() {
        Character source = 'Z';
        Character result = Converter.convertToCharacter(source);
        assertNotNull(result);
        assertEquals(source, result, "A Character source should be returned unchanged");
    }

    @Test
    @DisplayName("convertToCharacter converts a single‑character String")
    void testSingleCharString() {
        String source = "g";
        Character result = Converter.convertToCharacter(source);
        assertNotNull(result);
        assertEquals('g', result.charValue(), "A one‑character String should be converted to the corresponding Character");
    }

    @Test
    @DisplayName("convertToCharacter converts a multi‑character String by using the first character")
    void testMultiCharString() {
        // The underlying Converter treats a String as a CharSequence and extracts the first char.
        String source = "hello";
        Character result = Converter.convertToCharacter(source);
        assertNotNull(result);
        assertEquals('h', result.charValue(), "When a String longer than one character is supplied, the first character should be used");
    }

    @Test
    @DisplayName("convertToCharacter converts numeric types to the corresponding char value")
    void testNumericInput() {
        // ASCII 'A'
        Integer intSource = 65;
        Character intResult = Converter.convertToCharacter(intSource);
        assertEquals('A', intResult.charValue());
        // ASCII 'B'
        Long longSource = 66L;
        Character longResult = Converter.convertToCharacter(longSource);
        assertEquals('B', longResult.charValue());
        // ASCII 'C'
        Short shortSource = 67;
        Character shortResult = Converter.convertToCharacter(shortSource);
        assertEquals('C', shortResult.charValue());
        // ASCII 'D'
        Byte byteSource = 68;
        Character byteResult = Converter.convertToCharacter(byteSource);
        assertEquals('D', byteResult.charValue());
    }

    @Test
    @DisplayName("convertToCharacter converts Boolean values to '1' or '0'")
    void testBooleanInput() {
        Boolean trueSource = Boolean.TRUE;
        Boolean falseSource = Boolean.FALSE;
        Character trueResult = Converter.convertToCharacter(trueSource);
        Character falseResult = Converter.convertToCharacter(falseSource);
        assertEquals('1', trueResult.charValue(), "Boolean true should be converted to character '1'");
        assertEquals('0', falseResult.charValue(), "Boolean false should be converted to character '0'");
    }

    @Test
    @DisplayName("convertToCharacter converts CharBuffer to the first character")
    void testCharBufferInput() {
        CharBuffer buffer = CharBuffer.wrap("XYZ");
        Character result = Converter.convertToCharacter(buffer);
        assertEquals('X', result.charValue(), "CharBuffer should be converted using its first character");
    }

    @Test
    @DisplayName("convertToCharacter converts ByteBuffer to the first byte interpreted as char")
    void testByteBufferInput() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 'M', 'N', 'O' });
        Character result = Converter.convertToCharacter(buffer);
        assertEquals('M', result.charValue(), "ByteBuffer should be converted using its first byte as a character");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullAndDirectProvider")
    void testNullAndDirectTypes(String description, Object input, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(IllegalArgumentException.class)) {
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToCharacter(input), description);
        } else {
            Character result = Converter.convertToCharacter(input);
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringInputProvider")
    void testStringInputs(String description, Object input, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(IllegalArgumentException.class)) {
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToCharacter(input), description);
        } else {
            Character result = Converter.convertToCharacter(input);
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericInputProvider")
    void testNumericInputs(String description, Number input, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(IllegalArgumentException.class)) {
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToCharacter(input), description);
        } else {
            Character result = Converter.convertToCharacter(input);
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("booleanInputProvider")
    void testBooleanInputs(String description, Boolean input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("charBufferProvider")
    void testCharBufferInputs(String description, CharBuffer input, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(IllegalArgumentException.class)) {
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToCharacter(input), description);
        } else {
            Character result = Converter.convertToCharacter(input);
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("byteBufferProvider")
    void testByteBufferInputs(String description, ByteBuffer input, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(IllegalArgumentException.class)) {
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToCharacter(input), description);
        } else {
            Character result = Converter.convertToCharacter(input);
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedProvider")
    void testUnsupportedTypes(String description, Object input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("byteInputProvider")
    void testByteInputs(String description, Byte input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("shortInputProvider")
    void testShortInputs(String description, Short input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("floatingPointProvider")
    void testFloatingPointInputs(String description, Number input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bigIntegerProvider")
    void testBigIntegerInputs(String description, BigInteger input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bigDecimalProvider")
    void testBigDecimalInputs(String description, BigDecimal input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("atomicNumericProvider")
    void testAtomicNumericInputs(String description, Object input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("charArrayProvider")
    void testCharArrayInputs(String description, char[] input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringBuilderProvider")
    void testStringBuilderInputs(String description, StringBuilder input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringBufferProvider")
    void testStringBufferInputs(String description, StringBuffer input, Class<?> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }
}
