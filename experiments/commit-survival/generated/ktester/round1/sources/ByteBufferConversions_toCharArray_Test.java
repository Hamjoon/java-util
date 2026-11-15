package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

class ByteBufferConversions_toCharArray_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a Converter with default options for each test.
        // ConverterOptions has a public no‑arg constructor in the library.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear any state that might have been added to the Converter during a test.
        // The Converter does not expose a clear method, but setting the reference to null
        // helps the garbage collector.
        converter = null;
    }

    @Test
    void toCharArray_convertsByteBufferToCharArray() {
        // GIVEN
        // a simple byte array representing the ASCII characters "test"
        byte[] bytes = new byte[] { 't', 'e', 's', 't' };
        // the ByteBufferConversions methods accept any Object, so we pass the byte[]
        Object from = bytes;
        // WHEN
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN
        assertNotNull(result, "Resulting char array should not be null");
        assertEquals(4, result.length, "Char array length should match input length");
        assertArrayEquals(new char[] { 't', 'e', 's', 't' }, result, "Char array should contain the characters represented by the byte array");
    }

    @Test
    void testToCharArrayFromString() {
        // GIVEN a regular non‑empty Java String
        String from = "Hello World";
        // use the default singleton as per test data
        converter = DefaultConverter.INSTANCE;
        // WHEN converting
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN each character is preserved
        assertArrayEquals(new char[] { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' }, result);
    }

    @Test
    void testToCharArrayFromEmptyString() {
        // GIVEN an empty String
        String from = "";
        converter = DefaultConverter.INSTANCE;
        // WHEN converting
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN the result is an empty array (not null)
        assertNotNull(result, "Result should be an empty array, not null");
        assertEquals(0, result.length, "Empty string should produce empty char array");
    }

    @Test
    void testToCharArrayFromCharArray() {
        // GIVEN a primitive char[]
        char[] from = new char[] { 'a', 'b', 'c' };
        converter = DefaultConverter.INSTANCE;
        // WHEN converting
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN the returned array is identical
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    void testToCharArrayFromEmptyCharArray() {
        // GIVEN an empty char[]
        char[] from = new char[] {};
        converter = DefaultConverter.INSTANCE;
        // WHEN converting
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN the result is an empty array
        assertNotNull(result, "Result should be an empty array, not null");
        assertEquals(0, result.length);
    }

    @Test
    void testToCharArrayFromCharBuffer() {
        // GIVEN a CharBuffer wrapping characters x, y, z
        CharBuffer from = CharBuffer.wrap(new char[] { 'x', 'y', 'z' });
        converter = DefaultConverter.INSTANCE;
        // WHEN converting
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN the characters are correctly extracted
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
    }

    @Test
    void testToCharArrayFromByteBufferUtf8() {
        // GIVEN a UTF‑8 encoded ByteBuffer containing "abc"
        ByteBuffer from = ByteBuffer.wrap("abc".getBytes(StandardCharsets.UTF_8));
        converter = DefaultConverter.INSTANCE;
        // WHEN converting
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN we obtain the expected characters
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    void testToCharArrayFromByteArrayUtf8() {
        // GIVEN a UTF‑8 encoded byte[] containing the Greek letters π and λ
        byte[] from = "πλ".getBytes(StandardCharsets.UTF_8);
        converter = DefaultConverter.INSTANCE;
        // WHEN converting
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN the resulting char array represents the same Unicode characters
        assertEquals(2, result.length);
        assertEquals('π', result[0]);
        assertEquals('λ', result[1]);
    }

    @Test
    void testToCharArrayFromUnicodeSurrogatePair() {
        // GIVEN a String containing the emoji 😀 (represented by a surrogate pair)
        String from = "😀";
        converter = DefaultConverter.INSTANCE;
        // WHEN converting
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN the char[] when turned back into a String equals the original emoji
        assertEquals("😀", new String(result), "The surrogate pair should be preserved and reconstruct the original emoji");
    }

    @Test
    void testToCharArrayFromNullInput() {
        // GIVEN a null source object
        Object from = null;
        converter = DefaultConverter.INSTANCE;
        // WHEN converting
        char[] result = ByteBufferConversions.toCharArray(from, converter);
        // THEN the method returns null (as per expected behaviour in the spec)
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void testToCharArrayWithNullConverter() {
        // GIVEN a non‑null source but a null Converter
        String from = "test";
        Converter nullConverter = null;
        // WHEN converting, a NullPointerException is expected
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharArray(from, nullConverter), "Passing a null Converter should throw NullPointerException");
    }
}
