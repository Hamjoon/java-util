package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toString_Test {

    /**
     * Converter instance used for all tests.
     */
    private Converter converter;

    /**
     * Real {@link Converter} used for the happy‑path tests.
     */
    private Converter realConverter;

    /**
     * Mock {@link Converter} used for the error‑path tests.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Minimal stub of the library's {@code ConversionException} so the test compiles.
     * In the real project this class already exists; the stub is only needed for the
     * isolated test compilation environment.
     */
    class ConversionException extends RuntimeException {

        public ConversionException(String message) {
            super(message);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Converter has a public constructor that takes a ConverterOptions object.
        // Using the default options is sufficient for the built‑in conversions we need.
        converter = new Converter(new ConverterOptions());
    }

    static Stream<Arguments> stringProvider() {
        return Stream.of(Arguments.of("Hello World", "Hello World"), Arguments.of("", ""));
    }

    static Stream<Arguments> charArrayProvider() {
        return Stream.of(Arguments.of(new char[] { 'J', 'a', 'v', 'a' }, "Java"), Arguments.of(new char[] {}, ""));
    }

    static Stream<Arguments> charBufferProvider() {
        return Stream.of(Arguments.of(CharBuffer.wrap("BufferTest"), "BufferTest"), Arguments.of(CharBuffer.allocate(0), ""));
    }

    static Stream<Arguments> byteArrayProvider() {
        return Stream.of(Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, "Hello"), Arguments.of(new byte[] {}, ""));
    }

    static Stream<Arguments> byteBufferProvider() {
        return Stream.of(Arguments.of(ByteBuffer.wrap(new byte[] { 87, 111, 114, 108, 100 }), "World"), Arguments.of(ByteBuffer.allocate(0), ""));
    }

    static Stream<Arguments> numberProvider() {
        return Stream.of(Arguments.of(12345, "12345"), Arguments.of(3.14159, "3.14159"));
    }

    /**
     * Verify that a {@link CharBuffer} source is returned unchanged as a {@code String}.
     */
    @Test
    void toString_givenCharBuffer_returnsSameContent() {
        CharBuffer source = CharBuffer.wrap("hello world");
        String result = CharBufferConversions.toString(source, converter);
        assertEquals("hello world", result);
    }

    /**
     * Verify that a {@link String} source is correctly converted to a {@code CharBuffer}
     * and then to its {@code String} representation.
     */
    @Test
    void toString_givenString_returnsSameContent() {
        String source = "sample text";
        String result = CharBufferConversions.toString(source, converter);
        assertEquals("sample text", result);
    }

    /**
     * Verify that a {@code char[]} source is correctly converted.
     */
    @Test
    void toString_givenCharArray_returnsCorrectString() {
        char[] source = new char[] { 'a', 'b', 'c', 'd' };
        String result = CharBufferConversions.toString(source, converter);
        assertEquals("abcd", result);
    }

    /**
     * Verify that a {@link java.nio.ByteBuffer} containing UTF‑8 bytes can be turned into a string.
     * The {@code Converter} knows how to turn a {@code ByteBuffer} into a {@code CharBuffer}.
     */
    @Test
    void toString_givenByteBuffer_returnsCorrectString() {
        // ByteBuffer with UTF‑8 bytes for "xyz"
        byte[] bytes = "xyz".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.ByteBuffer source = java.nio.ByteBuffer.wrap(bytes);
        String result = CharBufferConversions.toString(source, converter);
        assertEquals("xyz", result);
    }

    /**
     * Verify that a {@link java.util.Map} with a single entry {@code "_v"} (the key used by the
     * conversion utilities) is correctly transformed to a string.
     */
    @Test
    void toString_givenMapWithValueKey_returnsValueString() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put(MapConversions.VALUE, "mapped value");
        String result = CharBufferConversions.toString(map, converter);
        assertEquals("mapped value", result);
    }

    /**
     * String → CharBuffer → String
     */
    @ParameterizedTest(name = "{index}: from=\"{0}\"")
    @MethodSource("stringProvider")
    void toString_fromString(String source, String expected) {
        String result = CharBufferConversions.toString(source, realConverter);
        assertEquals(expected, result);
    }

    /**
     * char[] → CharBuffer → String
     */
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("charArrayProvider")
    void toString_fromCharArray(char[] source, String expected) {
        String result = CharBufferConversions.toString(source, realConverter);
        assertEquals(expected, result);
    }

    /**
     * CharBuffer → CharBuffer → String
     */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("charBufferProvider")
    void toString_fromCharBuffer(CharBuffer source, String expected) {
        String result = CharBufferConversions.toString(source, realConverter);
        assertEquals(expected, result);
    }

    /**
     * byte[] (UTF‑8) → CharBuffer → String
     */
    @ParameterizedTest(name = "{index}: bytes={0}")
    @MethodSource("byteArrayProvider")
    void toString_fromByteArray(byte[] source, String expected) {
        String result = CharBufferConversions.toString(source, realConverter);
        assertEquals(expected, result);
    }

    /**
     * ByteBuffer (UTF‑8) → CharBuffer → String
     */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("byteBufferProvider")
    void toString_fromByteBuffer(ByteBuffer source, String expected) {
        String result = CharBufferConversions.toString(source, realConverter);
        assertEquals(expected, result);
    }

    /**
     * Number → CharBuffer → String
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numberProvider")
    void toString_fromNumber(Object source, String expected) {
        String result = CharBufferConversions.toString(source, realConverter);
        assertEquals(expected, result);
    }

    /**
     * Very large String (10 000 characters)
     */
    @Test
    void toString_fromLargeString() {
        StringBuilder sb = new StringBuilder(10_000);
        for (int i = 0; i < 10_000; i++) {
            sb.append('A');
        }
        String large = sb.toString();
        String result = CharBufferConversions.toString(large, realConverter);
        assertEquals(large, result);
    }

    /**
     * Passing a null source should raise NPE (the conversion routine cannot handle null).
     */
    @Test
    void toString_fromNull() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toString(null, realConverter));
    }

    /**
     * Passing a null Converter should raise NPE.
     */
    @Test
    void toString_nullConverter() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toString("sample", null));
    }

    /**
     * Unsupported source type – expect ClassCastException (or a subclass).
     */
    @Test
    void toString_fromUnsupportedType() {
        Object unsupported = new Object();
        assertThrows(ClassCastException.class, () -> CharBufferConversions.toString(unsupported, realConverter));
    }

    /**
     * Unsupported collection type – also expected to fail with ClassCastException.
     */
    @Test
    void toString_fromUnsupportedCollection() {
        List<Integer> list = List.of(1, 2, 3);
        assertThrows(ClassCastException.class, () -> CharBufferConversions.toString(list, realConverter));
    }

    /**
     * Malformed UTF‑8 byte array – should result in IllegalArgumentException.
     */
    @Test
    void toString_malformedByteArray() {
        byte[] malformed = new byte[] { (byte) -1, (byte) -2 };
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toString(malformed, realConverter));
    }

    /**
     * Malformed UTF‑8 ByteBuffer – should result in IllegalArgumentException.
     */
    @Test
    void toString_malformedByteBuffer() {
        // 0xC3 0x28 is an illegal UTF‑8 sequence.
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { (byte) 0xC3, (byte) 0x28 });
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toString(buf, realConverter));
    }

    /**
     * Converter that throws a ConversionException – the exception must propagate.
     */
    @Test
    void toString_converterThrowsException() {
        // Simulate the internal call that Converter would receive.
        // The real Converter API uses `convert(Object, Class<T>)`; we stub that.
        doThrow(new ConversionException("forced")).when(mockConverter).convert(any(), any());
        assertThrows(ConversionException.class, () -> CharBufferConversions.toString("someValue", mockConverter));
    }
}
