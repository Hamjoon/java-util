package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toByteArray_Test {

    /**
     * The Converter used for all tests – created once for the whole test class.
     */
    private static Converter converter;

    /**
     * The real Converter used for the default conversion tests.
     */
    private static Converter realConverter;

    /**
     * Placeholder for the library's {@code ConversionException}.
     * In a real test environment this class would be imported from the library.
     */
    class ConversionException extends RuntimeException {

        public ConversionException(String message) {
            super(message);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor gives a clean configuration.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern symmetrical.
        converter = null;
    }

    /**
     * Creates a Mockito mock of {@link Converter} that returns the supplied {@link ByteBuffer}
     * when {@code convert(..., ByteBuffer.class)} is invoked.
     */
    private static Converter mockConverterReturning(ByteBuffer buffer) {
        Converter mock = mock(Converter.class);
        // The actual Converter API uses a method named "convert".  The exact signature is not
        // public, but for the purpose of these tests we stub any call that returns a ByteBuffer.
        when(mock.convert(any(), eq(ByteBuffer.class))).thenReturn(buffer);
        return mock;
    }

    /**
     * Creates a Mockito mock of {@link Converter} that throws the supplied exception
     * when {@code convert(..., ByteBuffer.class)} is invoked.
     */
    private static Converter mockConverterThrowing(RuntimeException ex) {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(ByteBuffer.class))).thenThrow(ex);
        return mock;
    }

    private static Stream<Arguments> successfulConversionsProvider() {
        // 1. byte[] – no converter required (null is acceptable)
        byte[] bytes = new byte[] { 0x01, 0x02, 0x03 };
        // 2. String – use real converter for UTF‑8 conversion
        String hello = "Hello";
        byte[] helloBytes = hello.getBytes(StandardCharsets.UTF_8);
        // 3. CharBuffer – mock converter returns explicit bytes
        CharBuffer charBuf = CharBuffer.wrap("ABC");
        ByteBuffer bbFromChar = ByteBuffer.wrap(new byte[] { 65, 66, 67 });
        // 4. ArrayList<Byte> – mock converter returns explicit bytes
        List<Byte> list = new ArrayList<>(Arrays.asList((byte) 9, (byte) 10));
        ByteBuffer bbFromList = ByteBuffer.wrap(new byte[] { 9, 10 });
        return Stream.of(Arguments.of(bytes, null, bytes, "byte[] should be returned unchanged"), Arguments.of(hello, realConverter, helloBytes, "String should be converted to UTF‑8 bytes via real Converter"), Arguments.of(charBuf, mockConverterReturning(bbFromChar), new byte[] { 65, 66, 67 }, "CharBuffer should be converted using the mocked Converter"), Arguments.of(list, mockConverterReturning(bbFromList), new byte[] { 9, 10 }, "ArrayList<Byte> should be converted using the mocked Converter"));
    }

    @Test
    void toByteArray_givenByteArray_returnsSameBytes() {
        // GIVEN
        byte[] source = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        // WHEN
        byte[] result = CharArrayConversions.toByteArray(source, converter);
        // THEN
        assertArrayEquals(source, result, "The byte array should be unchanged after conversion.");
    }

    @Test
    void toByteArray_givenByteBuffer_returnsRemainingBytes() {
        // GIVEN
        byte[] data = new byte[] { 10, 20, 30, 40, 50 };
        // Create a buffer with a leading offset to ensure “remaining()” logic is exercised.
        ByteBuffer buffer = ByteBuffer.allocate(10);
        // leave two unused bytes at the start
        buffer.position(2);
        buffer.put(data);
        // prepare for reading – position = 0, limit = data length
        buffer.flip();
        // WHEN
        byte[] result = CharArrayConversions.toByteArray(buffer, converter);
        // THEN
        assertArrayEquals(data, result, "The method should return only the bytes that were put into the buffer.");
    }

    @Test
    void toByteArray_givenString_convertsViaConverterToByteBuffer() {
        // GIVEN
        String source = "hello";
        // Expected bytes are the UTF‑8 representation of the string – the Converter uses the platform default
        // charset when converting a String to a ByteBuffer, which for the purposes of this test we assume UTF‑8.
        byte[] expected = source.getBytes();
        // WHEN
        byte[] result = CharArrayConversions.toByteArray(source, converter);
        // THEN
        assertArrayEquals(expected, result, "A String should be converted to a byte array representing its UTF‑8 encoding.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulConversionsProvider")
    void toByteArray_successfulConversions(Object source, Converter converter, byte[] expected, String description) {
        byte[] result = CharArrayConversions.toByteArray(source, converter);
        assertArrayEquals(expected, result, description);
    }

    @Test
    void toByteArray_emptyByteArray_returnsEmptyArray() {
        byte[] source = new byte[0];
        byte[] result = CharArrayConversions.toByteArray(source, null);
        assertNotNull(result, "Result must not be null");
        assertEquals(0, result.length, "Empty byte[] should produce empty result");
    }

    @Test
    void toByteArray_emptyString_returnsEmptyArray() {
        String source = "";
        Converter mock = mockConverterReturning(ByteBuffer.allocate(0));
        byte[] result = CharArrayConversions.toByteArray(source, mock);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void toByteArray_bufferWithPosition_returnsRemainingBytes() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4 });
        // remaining = {2,3,4}
        buffer.position(2);
        Converter mock = mockConverterReturning(buffer);
        byte[] result = CharArrayConversions.toByteArray("offset", mock);
        assertArrayEquals(new byte[] { 2, 3, 4 }, result);
    }

    @Test
    void toByteArray_bufferWithLimit_returnsLimitedBytes() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 10, 11, 12, 13, 14 });
        // remaining = {10,11,12}
        buffer.limit(3);
        Converter mock = mockConverterReturning(buffer);
        byte[] result = CharArrayConversions.toByteArray("limit", mock);
        assertArrayEquals(new byte[] { 10, 11, 12 }, result);
    }

    @Test
    void toByteArray_readOnlyBuffer_isHandled() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 9, 10 }).asReadOnlyBuffer();
        Converter mock = mockConverterReturning(buffer);
        byte[] result = CharArrayConversions.toByteArray("readonly", mock);
        assertArrayEquals(new byte[] { 9, 10 }, result);
    }

    @Test
    void toByteArray_directBuffer_isHandled() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(2);
        buffer.put(new byte[] { 65, 66 });
        // prepare for reading
        buffer.flip();
        Converter mock = mockConverterReturning(buffer);
        byte[] result = CharArrayConversions.toByteArray("direct", mock);
        assertArrayEquals(new byte[] { 65, 66 }, result);
    }

    @Test
    void toByteArray_largeBuffer_returnsFullArray() {
        byte[] large = new byte[1024];
        Arrays.fill(large, (byte) 0x7F);
        ByteBuffer buffer = ByteBuffer.wrap(large);
        Converter mock = mockConverterReturning(buffer);
        byte[] result = CharArrayConversions.toByteArray("large", mock);
        assertEquals(1024, result.length);
        for (byte b : result) {
            assertEquals((byte) 0x7F, b);
        }
    }

    @Test
    void toByteArray_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteArray(null, null));
    }

    @Test
    void toByteArray_converterThrowsConversionException_propagates() {
        ConversionException ce = new ConversionException("cannot convert");
        Converter mock = mockConverterThrowing(ce);
        assertThrows(ConversionException.class, () -> CharArrayConversions.toByteArray("Unsupported", mock));
    }

    @Test
    void toByteArray_converterReturnsNull_throwsNullPointerException() {
        Converter mock = mockConverterReturning(null);
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteArray("any", mock));
    }

    @Test
    void toByteArray_nullConverterWithNonByteSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteArray("sample", null));
    }

    @Test
    void toByteArray_malformedBuffer_negativeRemaining_throwsNegativeArraySizeException() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 0, 1, 2, 3 });
        buffer.position(4);
        // position > limit → remaining() == -1
        buffer.limit(3);
        Converter mock = mockConverterReturning(buffer);
        assertThrows(NegativeArraySizeException.class, () -> CharArrayConversions.toByteArray("any", mock));
    }

    @Test
    void toByteArray_converterThrowsUncheckedException_propagates() {
        IllegalArgumentException iae = new IllegalArgumentException("bad conversion");
        Converter mock = mockConverterThrowing(iae);
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toByteArray("trigger", mock));
    }

    @Test
    void toByteArray_charArray_isHandledByConverter() {
        char[] source = new char[] { 'x', 'y', 'z' };
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 120, 121, 122 });
        Converter mock = mockConverterReturning(buffer);
        byte[] result = CharArrayConversions.toByteArray(source, mock);
        assertArrayEquals(new byte[] { 120, 121, 122 }, result);
    }

    @Test
    void toByteArray_stringBuilder_isHandledByConverter() {
        StringBuilder sb = new StringBuilder("Test");
        ByteBuffer buffer = ByteBuffer.wrap("Test".getBytes(StandardCharsets.UTF_8));
        Converter mock = mockConverterReturning(buffer);
        byte[] result = CharArrayConversions.toByteArray(sb, mock);
        assertArrayEquals(new byte[] { 84, 101, 115, 116 }, result);
    }

    @Test
    void toByteArray_stringBuffer_isHandledByConverter() {
        StringBuffer sbuf = new StringBuffer("Buf");
        ByteBuffer buffer = ByteBuffer.wrap("Buf".getBytes(StandardCharsets.UTF_8));
        Converter mock = mockConverterReturning(buffer);
        byte[] result = CharArrayConversions.toByteArray(sbuf, mock);
        assertArrayEquals(new byte[] { 66, 117, 102 }, result);
    }
}
