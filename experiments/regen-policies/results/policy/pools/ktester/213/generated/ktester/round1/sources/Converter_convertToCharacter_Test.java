package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.*;
import java.util.stream.Stream;

class Converter_convertToCharacter_Test {

    // ---------- basic types ----------
    private static Stream<Arguments> basicTypesProvider() {
        return Stream.of(Arguments.of("A", 'A', "String of length 1 should be converted to Character 'A'."), Arguments.of("B", 'B', "String of length 1 should be converted to Character 'B'."), Arguments.of(null, null, "Null input should return null."), Arguments.of('C', 'C', "Already a Character instance should be returned unchanged."));
    }

    // ---------- numeric types ----------
    private static Stream<Arguments> numericTypesProvider() {
        return Stream.of(Arguments.of(65, 'A', "Integer value 65 should be converted to the character 'A' (ASCII)."), Arguments.of(66L, 'B', "Long value 66L should be converted to the character 'B'."), Arguments.of((short) 67, 'C', "Short value 67 should be converted to the character 'C'."), Arguments.of((byte) 68, 'D', "Byte value 68 should be converted to the character 'D'."), Arguments.of(new BigInteger("69"), 'E', "BigInteger value 69 should be converted to the character 'E'."), Arguments.of(new BigDecimal("70"), 'F', "BigDecimal value 70 should be converted to the character 'F'."));
    }

    // ---------- boolean values ----------
    private static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(true, null, "Boolean true is not a supported source for Character conversion; should return null."), Arguments.of(false, null, "Boolean false is not a supported source for Character conversion; should return null."));
    }

    // ---------- CharBuffer and ByteBuffer ----------
    private static Stream<Arguments> bufferProvider() {
        return Stream.of(Arguments.of(CharBuffer.wrap("G"), 'G', "CharBuffer containing a single character should be converted to that character."), Arguments.of(CharBuffer.wrap("HI"), 'H', "CharBuffer with more than one character should yield the first character."), Arguments.of(ByteBuffer.wrap(new byte[] { 73 }), 'I', "ByteBuffer containing a single ASCII byte 73 should be converted to character 'I'."), Arguments.of(ByteBuffer.wrap(new byte[] { 74, 75 }), 'J', "ByteBuffer with multiple bytes should be converted using the first byte (value 74 => 'J')."));
    }

    // ---------- arrays and collections ----------
    private static Stream<Arguments> arrayCollectionProvider() {
        return Stream.of(Arguments.of(new char[] { 'K', 'L' }, 'K', "char[] with at least one element should be converted to its first element."), Arguments.of(new String[] { "M" }, 'M', "String[] where the first element is a single‑character string should be converted to that character."), Arguments.of(Collections.singletonList("N"), 'N', "A List containing a one‑character string should be converted to that character."));
    }

    // ---------- unsupported or edge inputs ----------
    private static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of(new Object(), null, "Arbitrary Object is not convertible to Character; should return null."), Arguments.of(new Date(), null, "Date instances are not convertible to Character; should return null."), Arguments.of("", null, "Empty String has no character to return; conversion should yield null."), Arguments.of("OP", null, "String longer than one character is ambiguous; conversion should return null."), Arguments.of(CharBuffer.allocate(0), null, "Empty CharBuffer cannot provide a character; conversion should return null."), Arguments.of(ByteBuffer.allocate(0), null, "Empty ByteBuffer cannot provide a byte; conversion should return null."));
    }

    // ---------- numeric overflow – expect IllegalArgumentException ----------
    private static Stream<Arguments> numericOverflowProvider() {
        return Stream.of(Arguments.of(Integer.MAX_VALUE, IllegalArgumentException.class, "Integer value larger than Character.MAX_VALUE should cause IllegalArgumentException."), Arguments.of(new BigInteger("100000"), IllegalArgumentException.class, "BigInteger value greater than 0xFFFF cannot be converted to a char and should throw IllegalArgumentException."), Arguments.of(new BigDecimal("65535.5"), IllegalArgumentException.class, "BigDecimal with a fractional part cannot be cast to a char; conversion should raise IllegalArgumentException."));
    }

    // ---------- empty arrays – expect ArrayIndexOutOfBoundsException ----------
    private static Stream<Arguments> emptyArrayProvider() {
        return Stream.of(Arguments.of(new char[] {}, ArrayIndexOutOfBoundsException.class, "Empty char[] leads to attempt to read first element and throws ArrayIndexOutOfBoundsException."), Arguments.of(new String[] {}, ArrayIndexOutOfBoundsException.class, "Empty String[] should cause ArrayIndexOutOfBoundsException when the converter accesses element 0."));
    }

    // ---------- null elements in arrays/collections – expect NullPointerException ----------
    private static Stream<Arguments> nullElementProvider() {
        return Stream.of(Arguments.of(new String[] { null }, NullPointerException.class, "String array whose first element is null triggers NullPointerException during conversion."), Arguments.of(Collections.singletonList(null), NullPointerException.class, "Collection containing a null element causes NullPointerException when the converter attempts to read it."));
    }

    // ---------- ByteBuffer underflow – expect BufferUnderflowException ----------
    private static Stream<Arguments> byteBufferUnderflowProvider() {
        ByteBuffer buf = ByteBuffer.allocate(1);
        // set position at limit -> underflow on read
        buf.position(1);
        return Stream.of(Arguments.of(buf, java.nio.BufferUnderflowException.class, "ByteBuffer whose position is at limit results in BufferUnderflowException when the converter tries to read a byte."));
    }

    @Test
    void convertNullReturnsNull() {
        // Given
        Object source = null;
        // When
        Character result = Converter.convertToCharacter(source);
        // Then
        assertNull(result, "Converting null should return null for object target types");
    }

    @Test
    void convertFromCharacterReturnsSameInstance() {
        // Given
        Character source = 'Z';
        // When
        Character result = Converter.convertToCharacter(source);
        // Then
        assertEquals(source, result, "Converting a Character should return the same value");
    }

    @Test
    void convertFromSingleCharString() {
        // Given
        String source = "A";
        // When
        Character result = Converter.convertToCharacter(source);
        // Then
        assertEquals('A', result);
    }

    @Test
    void convertFromCharBuffer() {
        // Given
        java.nio.CharBuffer source = java.nio.CharBuffer.wrap("b");
        // When
        Character result = Converter.convertToCharacter(source);
        // Then
        assertEquals('b', result);
    }

    @Test
    void convertFromInteger() {
        // Given
        // ASCII code for 'C'
        Integer source = 67;
        // When
        Character result = Converter.convertToCharacter(source);
        // Then
        assertEquals('C', result);
    }

    @Test
    void convertFromByteArray() {
        // Given
        // ASCII for 'D'
        byte[] source = new byte[] { 68 };
        // When
        Character result = Converter.convertToCharacter(source);
        // Then
        assertEquals('D', result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicTypesProvider")
    void convertToCharacter_basicTypes(Object input, Character expected, String description) {
        Character result = Converter.convertToCharacter(input);
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericTypesProvider")
    void convertToCharacter_numericTypes(Object input, Character expected, String description) {
        Character result = Converter.convertToCharacter(input);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("booleanProvider")
    void convertToCharacter_booleanValues(Object input, Character expected, String description) {
        Character result = Converter.convertToCharacter(input);
        assertNull(result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("bufferProvider")
    void convertToCharacter_buffers(Object input, Character expected, String description) {
        Character result = Converter.convertToCharacter(input);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("arrayCollectionProvider")
    void convertToCharacter_arraysAndCollections(Object input, Character expected, String description) {
        Character result = Converter.convertToCharacter(input);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedProvider")
    void convertToCharacter_unsupported(Object input, Character expected, String description) {
        Character result = Converter.convertToCharacter(input);
        assertNull(result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericOverflowProvider")
    void convertToCharacter_numericOverflow(Object input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("emptyArrayProvider")
    void convertToCharacter_emptyArrays(Object input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullElementProvider")
    void convertToCharacter_nullElements(Object input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("byteBufferUnderflowProvider")
    void convertToCharacter_byteBufferUnderflow(Object input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> Converter.convertToCharacter(input), description);
    }
}
