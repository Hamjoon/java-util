package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toByteBuffer_Test {

    /**
     * Mocked Converter used in all test cases.
     */
    @Mock
    private Converter converterMock;

    /**
     * Mocked ConverterOptions returned by the mocked Converter.
     */
    @Mock
    private ConverterOptions optionsMock;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy:
        // converterMock.getOptions() -> optionsMock
        // optionsMock.getCharset() -> StandardCharsets.UTF_8
        when(converterMock.getOptions()).thenReturn(optionsMock);
        when(optionsMock.getCharset()).thenReturn(StandardCharsets.UTF_8);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(converterMock, optionsMock);
    }

    /**
     * Helper that extracts the remaining bytes from a {@link ByteBuffer}
     * without altering the original buffer's position.
     */
    private static byte[] toByteArray(ByteBuffer buffer) {
        // keep original position intact
        ByteBuffer dup = buffer.duplicate();
        byte[] bytes = new byte[dup.remaining()];
        dup.get(bytes);
        return bytes;
    }

    /**
     * Creates a fresh mock {@link Converter} that returns the supplied {@link Charset}.
     */
    private static Converter mockConverterWithCharset(Charset charset) {
        Converter conv = mock(Converter.class);
        ConverterOptions opts = mock(ConverterOptions.class);
        when(conv.getOptions()).thenReturn(opts);
        when(opts.getCharset()).thenReturn(charset);
        return conv;
    }

    /**
     * Creates a mock {@link Converter} whose {@code getOptions()} returns {@code null}.
     */
    private static Converter mockConverterWithNullOptions() {
        Converter conv = mock(Converter.class);
        when(conv.getOptions()).thenReturn(null);
        return conv;
    }

    /**
     * Creates a mock {@link Converter} whose options return a {@code null} charset.
     */
    private static Converter mockConverterWithNullCharset() {
        Converter conv = mock(Converter.class);
        ConverterOptions opts = mock(ConverterOptions.class);
        when(conv.getOptions()).thenReturn(opts);
        when(opts.getCharset()).thenReturn(null);
        return conv;
    }

    /**
     * Creates a mock {@link Charset} that throws {@link CharacterCodingException} on encode.
     */
    private static Charset mockFailingCharset() throws CharacterCodingException {
        Charset charset = mock(Charset.class);
        when(charset.encode(any(CharBuffer.class))).thenThrow(new CharacterCodingException());
        return charset;
    }

    /**
     * Supplies a stream of simple successful conversion scenarios.
     */
    private static Stream<Arguments> simpleSuccessProvider() {
        return Stream.of(// String → UTF‑8
        Arguments.of("ABC", StandardCharsets.UTF_8, "ABC".getBytes(StandardCharsets.UTF_8)), // char[] → UTF‑8
        Arguments.of(new char[] { '1', '2', '3' }, StandardCharsets.UTF_8, "123".getBytes(StandardCharsets.UTF_8)), // CharBuffer → UTF‑8
        Arguments.of(CharBuffer.wrap("XYZ"), StandardCharsets.UTF_8, "XYZ".getBytes(StandardCharsets.UTF_8)), // StringBuilder → UTF‑8
        Arguments.of(new StringBuilder("Builder"), StandardCharsets.UTF_8, "Builder".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void toByteBuffer_fromCharArray_encodesWithUtf8() {
        // GIVEN
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer must not be null");
        byte[] expected = "Java".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, toByteArray(result), "ByteBuffer content must match UTF‑8 encoding of the source chars");
    }

    @Test
    void toByteBuffer_fromString_encodesWithUtf8() {
        // GIVEN
        String source = "Cedar";
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        // THEN
        assertNotNull(result);
        byte[] expected = "Cedar".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, toByteArray(result));
    }

    @Test
    void toByteBuffer_nullInput_returnsEmptyBuffer() {
        // GIVEN
        Object source = null;
        // WHEN
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        // THEN
        assertNotNull(result);
        assertEquals(0, result.remaining(), "A null source should result in an empty ByteBuffer");
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_fromCharBuffer_encodesWithUtf8() {
        CharBuffer source = CharBuffer.wrap("BufferTest");
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        assertNotNull(result);
        byte[] expected = "BufferTest".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, toByteArray(result));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_nullSource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toByteBuffer(null, converterMock));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_unsupportedType_throwsConversionException() {
        Integer source = 12345;
        assertThrows(ConversionException.class, () -> CharArrayConversions.toByteBuffer(source, converterMock));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_iso8859_1_encodesCorrectly() {
        Converter isoConverter = mockConverterWithCharset(StandardCharsets.ISO_8859_1);
        String source = "café";
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, isoConverter);
        assertNotNull(result);
        byte[] expected = source.getBytes(StandardCharsets.ISO_8859_1);
        assertArrayEquals(expected, toByteArray(result));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_emptyString_returnsEmptyBuffer() {
        String source = "";
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        assertNotNull(result);
        assertEquals(0, result.remaining(), "Empty String should produce an empty ByteBuffer");
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_emptyCharArray_returnsEmptyBuffer() {
        char[] source = new char[0];
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        assertNotNull(result);
        assertEquals(0, result.remaining(), "Empty char[] should produce an empty ByteBuffer");
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_fromStringBuilder_encodesWithUtf8() {
        StringBuilder source = new StringBuilder("BuilderTest");
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        assertNotNull(result);
        byte[] expected = "BuilderTest".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, toByteArray(result));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_fromStringBuffer_encodesWithUtf8() {
        StringBuffer source = new StringBuffer("BufferTest");
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        assertNotNull(result);
        byte[] expected = "BufferTest".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, toByteArray(result));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_emoji_surrogatePairEncodedCorrectly() {
        // 😀 (U+1F600)
        String source = "\uD83D\uDE00";
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        assertNotNull(result);
        byte[] expected = new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 };
        assertArrayEquals(expected, toByteArray(result));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_utf16le_encodesCorrectly() {
        Charset utf16le = StandardCharsets.UTF_16LE;
        Converter conv = mockConverterWithCharset(utf16le);
        String source = "AB";
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, conv);
        assertNotNull(result);
        byte[] expected = source.getBytes(utf16le);
        assertArrayEquals(expected, toByteArray(result));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_readOnlyCharBuffer_encodesCorrectly() {
        CharBuffer source = CharBuffer.wrap("ReadOnly").asReadOnlyBuffer();
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        assertNotNull(result);
        byte[] expected = "ReadOnly".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, toByteArray(result));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_largeInput_producesCorrectLength() {
        char[] chars = new char[10_000];
        java.util.Arrays.fill(chars, 'x');
        String source = new String(chars);
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, converterMock);
        assertNotNull(result);
        assertEquals(10_000, result.remaining(), "ByteBuffer length must match number of 'x' characters");
        // Spot‑check first and last bytes
        byte[] bytes = toByteArray(result);
        assertEquals((byte) 'x', bytes[0]);
        assertEquals((byte) 'x', bytes[bytes.length - 1]);
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_nullConverter_throwsNullPointerException() {
        String source = "Sample";
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer(source, null));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_converterWithNullOptions_throwsNullPointerException() {
        String source = "Sample";
        Converter conv = mockConverterWithNullOptions();
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer(source, conv));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_converterWithNullCharset_throwsNullPointerException() {
        String source = "Sample";
        Converter conv = mockConverterWithNullCharset();
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteBuffer(source, conv));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_charBufferWithBadState_throwsIllegalArgumentException() {
        CharBuffer source = CharBuffer.wrap("BadState");
        // deliberately set an illegal position
        source.position(10);
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toByteBuffer(source, converterMock));
    }

    // -----------------------------------------------------------------
    @Test
    void toByteBuffer_charsetEncodeFailure_propagatesCharacterCodingException() throws Exception {
        Charset failing = mockFailingCharset();
        Converter conv = mockConverterWithCharset(failing);
        String source = "Test";
        assertThrows(CharacterCodingException.class, () -> CharArrayConversions.toByteBuffer(source, conv));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("simpleSuccessProvider")
    void toByteBuffer_simpleSuccess(Object source, Charset charset, byte[] expected) {
        Converter conv = mockConverterWithCharset(charset);
        ByteBuffer result = CharArrayConversions.toByteBuffer(source, conv);
        assertNotNull(result);
        assertArrayEquals(expected, toByteArray(result));
    }
}
