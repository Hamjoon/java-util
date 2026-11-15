package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toByteArray_Test {

    /**
     * Shared {@code Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the library’s default charset (UTF‑8) is used for
        // CharSequence → ByteBuffer conversion.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// raw byte[]
        Arguments.of(new byte[] { 1, 2, 3, 4 }, new byte[] { 1, 2, 3, 4 }, "Convert a raw byte[]; should return an identical byte array."), // String
        Arguments.of("Hello", "Hello".getBytes(StandardCharsets.UTF_8), "Convert a String; UTF‑8 encoding expected."), // char[]
        Arguments.of(new char[] { 'J', 'a', 'v', 'a' }, "Java".getBytes(StandardCharsets.UTF_8), "Convert a char[]; UTF‑8 encoding expected."), // ByteBuffer
        Arguments.of(ByteBuffer.wrap(new byte[] { 10, 20, 30 }), new byte[] { 10, 20, 30 }, "Convert an existing ByteBuffer; copy remaining bytes unchanged."), // StringBuilder
        Arguments.of(new StringBuilder("AB"), "AB".getBytes(StandardCharsets.UTF_8), "Convert a StringBuilder; treated as CharSequence."), // CharBuffer with non‑ASCII character Ω
        Arguments.of(CharBuffer.wrap("Ω"), "Ω".getBytes(StandardCharsets.UTF_8), "Convert a CharBuffer containing a non‑ASCII character."), // StringBuffer
        Arguments.of(new StringBuffer("Test"), "Test".getBytes(StandardCharsets.UTF_8), "Convert a StringBuffer; UTF‑8 encoding expected."), // Empty String
        Arguments.of("", new byte[0], "Convert an empty String; result must be empty."), // Empty char[]
        Arguments.of(new char[0], new byte[0], "Convert an empty char[]; result must be empty."), // ByteBuffer with position set to 2 (remaining bytes 7,8)
        Arguments.of(ByteBuffer.wrap(new byte[] { 5, 6, 7, 8 }).position(2), new byte[] { 7, 8 }, "Convert a ByteBuffer whose position is set to 2; only remaining bytes are copied."), // Large byte[1000] (all zeros)
        Arguments.of(new byte[1000], new byte[1000], "Convert a large byte array (1000 zeros); output must match input."), // Emoji 😀 (surrogate pair)
        Arguments.of("😀", "😀".getBytes(StandardCharsets.UTF_8), "Convert a String containing an emoji; UTF‑8 four‑byte sequence expected."), // String containing a null character
        Arguments.of("\u0000", "\u0000".getBytes(StandardCharsets.UTF_8), "Convert a String that contains a null character; resulting byte array must contain a single zero byte."));
    }

    /**
     * Verify that a simple {@code String} is converted to its UTF‑8 byte representation.
     */
    @Test
    void toByteArray_fromString_returnsUtf8Bytes() {
        // given
        String source = "Hello, world!";
        byte[] expected = source.getBytes(StandardCharsets.UTF_8);
        // when
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(expected, actual, "Byte array produced from String should match UTF‑8 encoding");
    }

    /**
     * Verify that a {@code char[]} is converted to the same UTF‑8 bytes as the
     * corresponding {@code String}.
     */
    @Test
    void toByteArray_fromCharArray_returnsUtf8Bytes() {
        // given
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        String asString = new String(source);
        byte[] expected = asString.getBytes(StandardCharsets.UTF_8);
        // when
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(expected, actual, "Byte array produced from char[] should match UTF‑8 encoding of the same characters");
    }

    /**
     * Verify that a {@code CharBuffer} is handled correctly.
     */
    @Test
    void toByteArray_fromCharBuffer_returnsUtf8Bytes() {
        // given
        CharBuffer source = CharBuffer.wrap("buffered");
        byte[] expected = "buffered".getBytes(StandardCharsets.UTF_8);
        // when
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(expected, actual, "Byte array produced from CharBuffer should match UTF‑8 encoding");
    }

    /**
     * Verify that a {@code null} source yields an empty byte array (the
     * underlying {@code toByteBuffer} returns an empty buffer for {@code null}).
     */
    @Test
    void toByteArray_fromNull_returnsEmptyArray() {
        // given
        Object source = null;
        // when
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        // then
        assertNotNull(actual, "Result should never be null");
        assertEquals(0, actual.length, "Byte array for null input should be empty");
    }

    /**
     * Verify that a {@code ByteBuffer} is returned unchanged (i.e., the same
     * bytes are extracted).  This also exercises the path where the source is
     * already a byte‑oriented buffer.
     */
    @Test
    void toByteArray_fromByteBuffer_returnsSameBytes() {
        // given
        byte[] raw = { 0x01, 0x02, 0x7F, (byte) 0xFF };
        ByteBuffer source = ByteBuffer.wrap(raw);
        // The expected array is a copy of the original content.
        byte[] expected = raw.clone();
        // when
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(expected, actual, "Byte array produced from ByteBuffer should be identical to the buffer's contents");
    }

    @Test
    void toByteArray_fromNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteArray(null, converter), "A null source should cause NullPointerException according to the contract");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversionProvider")
    void toByteArray_successfulConversions(Object from, byte[] expected, String description) {
        byte[] actual = CharArrayConversions.toByteArray(from, converter);
        assertArrayEquals(expected, actual, description);
    }

    @Test
    void toByteArray_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteArray("test", null), "A null Converter argument must cause NullPointerException");
    }

    @Test
    void toByteArray_unsupportedType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toByteArray(12345, converter), "Supplying an unsupported type (Integer) should cause IllegalArgumentException");
    }

    @Test
    void toByteArray_converterThrowsRuntimeException_propagated() {
        // Mock a Converter that throws when any method is invoked.
        Converter explodingConverter = mock(Converter.class);
        // The exact method signature used inside toByteBuffer is unknown;
        // we therefore make *any* method call throw.
        when(explodingConverter.convert(any(), any())).thenThrow(new RuntimeException("forced"));
        // The toByteArray method should propagate the RuntimeException.
        assertThrows(RuntimeException.class, () -> CharArrayConversions.toByteArray("any", explodingConverter), "RuntimeException from Converter should be propagated");
    }

    @Test
    void toByteArray_toByteBufferReturnsNull_throwsNPE() {
        // Mock static call to toByteBuffer to return null.
        try (MockedStatic<CharArrayConversions> mocked = mockStatic(CharArrayConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> CharArrayConversions.toByteBuffer(eq("test"), any())).thenReturn(null);
            assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteArray("test", converter), "If toByteBuffer returns null, toByteArray must throw NullPointerException");
        }
    }

    @Test
    void toByteArray_toByteBufferReturnsInvalidBuffer_throwsNegativeArraySizeException() {
        // Create a malformed ByteBuffer (position > limit).
        ByteBuffer badBuffer = ByteBuffer.allocate(10);
        badBuffer.limit(5);
        // now remaining() == -3
        badBuffer.position(8);
        try (MockedStatic<CharArrayConversions> mocked = mockStatic(CharArrayConversions.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> CharArrayConversions.toByteBuffer(eq("test"), any())).thenReturn(badBuffer);
            assertThrows(NegativeArraySizeException.class, () -> CharArrayConversions.toByteArray("test", converter), "Negative remaining size should cause NegativeArraySizeException");
        }
    }
}
