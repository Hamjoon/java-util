package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toCharBuffer_Test {

    /**
     * Mocks used in the tests
     */
    @Mock
    Converter converter;

    @Mock
    ConverterOptions options;

    @Mock
    Charset charset;

    @Mock
    CharBuffer decodedCharBuffer;

    /**
     * Mocks used in the tests
     */
    @Mock
    Converter mockConverter;

    @Mock
    ConverterOptions mockOptions;

    @Mock
    Charset mockCharset;

    @BeforeEach
    void setupBeforeEach() {
        // Common mock hierarchy for tests that use a mocked Converter
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getCharset()).thenReturn(mockCharset);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the “Valid conversions” group.
     * -------------------------------------------------------------- */
    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// byte[] → CharBuffer (UTF‑8)
        Arguments.of(// "hello"
        new byte[] { 72, 101, 108, 108, 111 }, Charset.forName("UTF-8"), "hello"), // String → CharBuffer (UTF‑8)
        Arguments.of("world", Charset.forName("UTF-8"), "world"), // ByteBuffer → CharBuffer (UTF‑8)
        Arguments.of(// "abc"
        ByteBuffer.wrap(new byte[] { 97, 98, 99 }), Charset.forName("UTF-8"), "abc"), // Empty byte[] → empty CharBuffer
        Arguments.of(new byte[] {}, Charset.forName("UTF-8"), ""), // Large homogeneous array (1000 'x')
        Arguments.of(createFilledArray(1000, (byte) 'x'), Charset.forName("UTF-8"), repeat('x', 1000)), // ByteBuffer with UTF‑16BE offset/limit → single char "\u0100"
        Arguments.of(ByteBuffer.wrap(new byte[] { 0, 0, 0x01, 0x00 }, 2, 2), Charset.forName("UTF-16BE"), "\u0100"), // ByteBuffer with UTF‑16LE BOM + 'A' → "A"
        Arguments.of(ByteBuffer.wrap(new byte[] { (byte) 0xFF, (byte) 0xFE, 0x41, 0x00 }), Charset.forName("UTF-16LE"), "A"));
    }

    /**
     * Helper that mimics the expected {@code toByteBuffer} result for the given source.
     * It does **not** call the real implementation – it merely creates a ByteBuffer
     * that would be the correct representation for the test scenario.
     */
    private static ByteBuffer toByteBufferForSource(Object source, Charset charset) {
        if (source instanceof byte[]) {
            return ByteBuffer.wrap((byte[]) source);
        }
        if (source instanceof String) {
            // Encode the string with the supplied charset.
            return charset.encode((String) source);
        }
        if (source instanceof ByteBuffer) {
            // Return a duplicate to avoid altering the original buffer's position/limit.
            return ((ByteBuffer) source).duplicate();
        }
        throw new IllegalArgumentException("Unsupported test source type: " + source.getClass());
    }

    private static byte[] createFilledArray(int length, byte value) {
        byte[] arr = new byte[length];
        for (int i = 0; i < length; i++) arr[i] = value;
        return arr;
    }

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }

    /**
     * Verifies that {@code ByteBufferConversions.toCharBuffer(Object, Converter)}
     * obtains a {@link ByteBuffer} via {@code toByteBuffer}, then decodes it
     * with the {@link java.nio.charset.Charset} supplied by the {@link ConverterOptions}.
     */
    @Test
    void testToCharBuffer_decodesByteBuffer() {
        // -----------------------------------------------------------------
        // GIVEN – a source object and the expected ByteBuffer that the static
        //         method {@code toByteBuffer} will return.
        // -----------------------------------------------------------------
        Object source = "any source object";
        // “ABC”
        ByteBuffer expectedByteBuffer = ByteBuffer.wrap(new byte[] { 65, 66, 67 });
        // Stub the static method ByteBufferConversions.toByteBuffer(...)
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            // When the static helper is called, return our prepared ByteBuffer
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(source, converter)).thenReturn(expectedByteBuffer);
            // -----------------------------------------------------------------
            // AND – the charset.decode(...) call should return a known CharBuffer.
            // -----------------------------------------------------------------
            when(charset.decode(expectedByteBuffer)).thenReturn(decodedCharBuffer);
            // -----------------------------------------------------------------
            // WHEN – the method under test is invoked.
            // -----------------------------------------------------------------
            CharBuffer actual = ByteBufferConversions.toCharBuffer(source, converter);
            // -----------------------------------------------------------------
            // THEN – the returned CharBuffer is exactly the one supplied by the
            //        charset decoder and the interactions occurred as expected.
            // -----------------------------------------------------------------
            assertSame(decodedCharBuffer, actual, "The CharBuffer returned by toCharBuffer should be the one produced by Charset.decode");
            // Verify that the static method was called exactly once with the right args
            mockedStatic.verify(() -> ByteBufferConversions.toByteBuffer(source, converter), times(1));
            // Verify that charset.decode was called with the ByteBuffer we supplied
            verify(charset, times(1)).decode(expectedByteBuffer);
        }
    }

    /**
     * Additional sanity test: when the source object is already a {@link ByteBuffer},
     * the static conversion should still delegate to {@code toByteBuffer} and decode it.
     */
    @Test
    void testToCharBuffer_withByteBufferSource() {
        // -----------------------------------------------------------------
        // GIVEN – a ByteBuffer source object.
        // -----------------------------------------------------------------
        // “xy”
        ByteBuffer sourceBuffer = ByteBuffer.wrap(new byte[] { 120, 121 });
        when(charset.decode(sourceBuffer)).thenReturn(decodedCharBuffer);
        // Stub static conversion to simply return the same buffer (identity)
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(sourceBuffer, converter)).thenReturn(sourceBuffer);
            // -----------------------------------------------------------------
            // WHEN
            // -----------------------------------------------------------------
            CharBuffer result = ByteBufferConversions.toCharBuffer(sourceBuffer, converter);
            // -----------------------------------------------------------------
            // THEN
            // -----------------------------------------------------------------
            assertSame(decodedCharBuffer, result);
            verify(charset, times(1)).decode(sourceBuffer);
        }
    }

    @ParameterizedTest(name = "[{index}] from={0}, charset={1}")
    @MethodSource("validConversionsProvider")
    void testToCharBuffer_validConversions(Object source, Charset charset, String expectedString) {
        // Build a real Converter that simply returns the supplied charset.
        Converter realConverter = new Converter(charset);
        // Determine the ByteBuffer that the static helper should return.
        ByteBuffer buffer = toByteBufferForSource(source, charset);
        // Mock the static helper to return our prepared buffer.
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(source, realConverter)).thenReturn(buffer);
            // Invoke the method under test.
            CharBuffer result = ByteBufferConversions.toCharBuffer(source, realConverter);
            // Verify the decoded content.
            assertEquals(expectedString, result.toString(), () -> "Decoded CharBuffer does not match expected for source: " + source);
        }
    }

    /* --------------------------------------------------------------
     *  Null handling tests.
     * -------------------------------------------------------------- */
    @Test
    void testToCharBuffer_nullSource_throwsNPE() {
        Converter realConverter = new Converter(Charset.forName("UTF-8"));
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(null, realConverter)).thenThrow(NullPointerException.class);
            assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(null, realConverter));
        }
    }

    @Test
    void testToCharBuffer_nullConverter_throwsNPE() {
        Object source = "test";
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toCharBuffer(source, null));
    }

    /* --------------------------------------------------------------
     *  Invalid source type – we force the static helper to throw.
     * -------------------------------------------------------------- */
    @Test
    void testToCharBuffer_invalidSource_throwsIllegalArgumentException() {
        Object invalidSource = new Object();
        Converter realConverter = new Converter(Charset.forName("UTF-8"));
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(invalidSource, realConverter)).thenThrow(IllegalArgumentException.class);
            assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toCharBuffer(invalidSource, realConverter));
        }
    }

    /* --------------------------------------------------------------
     *  ByteBuffer with illegal position/limit – Charset.decode should
     *  raise IllegalArgumentException.
     * -------------------------------------------------------------- */
    @Test
    void testToCharBuffer_illegalByteBufferPositionLimit_throwsIllegalArgumentException() {
        // Create a buffer where position > limit.
        ByteBuffer illegal = ByteBuffer.allocate(4);
        illegal.position(3);
        // illegal state
        illegal.limit(2);
        Converter realConverter = new Converter(Charset.forName("UTF-8"));
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(illegal, realConverter)).thenReturn(illegal);
            assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toCharBuffer(illegal, realConverter));
        }
    }

    /* --------------------------------------------------------------
     *  Charset decoding edge cases – using real Charset to verify that
     *  malformed input does not crash but yields replacement characters.
     * -------------------------------------------------------------- */
    @Test
    void testToCharBuffer_malformedUtf8Sequence_replacedByDecoder() {
        // Malformed UTF‑8: 0xC3 0x28 (invalid continuation byte)
        byte[] malformed = new byte[] { (byte) 0xC3, (byte) 0x28 };
        Converter realConverter = new Converter(Charset.forName("UTF-8"));
        ByteBuffer buffer = ByteBuffer.wrap(malformed);
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(malformed, realConverter)).thenReturn(buffer);
            CharBuffer result = ByteBufferConversions.toCharBuffer(malformed, realConverter);
            // The default decoder replaces the malformed byte with the Unicode replacement character  (U+FFFD)
            String expected = "\uFFFD(";
            assertEquals(expected, result.toString(), "Malformed UTF‑8 should be replaced by the decoder's replacement character.");
        }
    }

    @Test
    void testToCharBuffer_iso8859_1DecodingOfUtf8EuroBytes_resultsInReplacementChars() {
        // UTF‑8 bytes for € : 0xE2 0x82 0xAC
        byte[] euroUtf8 = new byte[] { (byte) 0xE2, (byte) 0x82, (byte) 0xAC };
        Converter realConverter = new Converter(Charset.forName("ISO-8859-1"));
        ByteBuffer buffer = ByteBuffer.wrap(euroUtf8);
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(euroUtf8, realConverter)).thenReturn(buffer);
            CharBuffer result = ByteBufferConversions.toCharBuffer(euroUtf8, realConverter);
            // ISO‑8859‑1 treats each byte as a separate character; none are invalid, so we expect three characters.
            // For the purpose of this test we assert that the result length is 3 and does not equal "€".
            assertEquals(3, result.length(), "ISO‑8859‑1 decoding of UTF‑8 euro bytes should yield three characters.");
            assertNotEquals("€", result.toString(), "Decoded string should not be the euro sign when using ISO‑8859‑1.");
        }
    }
}
