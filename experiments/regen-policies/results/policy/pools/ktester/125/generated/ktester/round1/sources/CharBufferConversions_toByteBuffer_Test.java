package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toByteBuffer_Test {

    /**
     * The Converter used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default ConverterOptions – it supplies a Charset (UTF‑8 by default).
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    // ---------- Helper utilities ----------
    private static byte[] toByteArray(ByteBuffer bb) {
        // keep original position unchanged
        ByteBuffer dup = bb.duplicate();
        byte[] arr = new byte[dup.remaining()];
        dup.get(arr);
        return arr;
    }

    private static Converter mockConverterWithCharset(Charset cs) {
        ConverterOptions mockOptions = Mockito.mock(ConverterOptions.class);
        Mockito.when(mockOptions.getCharset()).thenReturn(cs);
        Converter mockConverter = Mockito.mock(Converter.class);
        Mockito.when(mockConverter.getOptions()).thenReturn(mockOptions);
        return mockConverter;
    }

    // ---------- 1. Valid conversions – String input ----------
    static Stream<Arguments> stringInputProvider() {
        return Stream.of(arguments("Hello, 世界", Charset.forName("UTF-8"), "Hello, 世界".getBytes(Charset.forName("UTF-8"))), arguments("", Charset.forName("UTF-8"), new byte[0]));
    }

    // ---------- 2. Valid conversions – CharBuffer input ----------
    static Stream<Arguments> charBufferProvider() {
        return Stream.of(arguments(CharBuffer.wrap("buffer"), Charset.forName("UTF-8"), "buffer".getBytes(Charset.forName("UTF-8"))));
    }

    // ---------- 3. Valid conversions – char[] input ----------
    static Stream<Arguments> charArrayProvider() {
        return Stream.of(arguments(new char[] { 'J', 'a', 'v', 'a' }, Charset.forName("UTF-8"), "Java".getBytes(Charset.forName("UTF-8"))));
    }

    // ---------- 5. Charset variations ----------
    static Stream<Arguments> charsetVariationProvider() {
        return Stream.of(arguments("AB", Charset.forName("UTF-16LE"), new byte[] { 65, 0, 66, 0 }), arguments("©", Charset.forName("ISO-8859-1"), new byte[] { (byte) 0xA9 }));
    }

    // ---------- 10. Valid conversions – StringBuilder ----------
    static Stream<Arguments> stringBuilderProvider() {
        return Stream.of(arguments(new StringBuilder("Builder"), Charset.forName("UTF-8"), "Builder".getBytes(Charset.forName("UTF-8"))), arguments(new StringBuilder(""), Charset.forName("UTF-8"), new byte[0]));
    }

    /**
     * Verify that a simple {@code String} is encoded correctly.
     * The resulting {@link ByteBuffer} is decoded with the same charset and should
     * yield the original string.
     */
    @Test
    void toByteBuffer_encodesString() {
        String source = "Hello, 世界!";
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, converter);
        assertNotNull(bb, "ByteBuffer should not be null");
        // Decode using the same charset the Converter uses.
        Charset cs = converter.getOptions().getCharset();
        CharBuffer decoded = cs.decode(bb);
        assertEquals(source, decoded.toString(), "Decoded ByteBuffer should match the original String");
    }

    /**
     * Verify that a {@code char[]} array is handled correctly.
     */
    @Test
    void toByteBuffer_encodesCharArray() {
        char[] source = new char[] { 'a', 'b', 'c', '1', '2', '3' };
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, converter);
        assertNotNull(bb, "ByteBuffer should not be null");
        Charset cs = converter.getOptions().getCharset();
        CharBuffer decoded = cs.decode(bb);
        assertEquals(new String(source), decoded.toString(), "Decoded ByteBuffer should match the original char array");
    }

    /**
     * Verify that a {@link CharBuffer} is encoded without alteration.
     */
    @Test
    void toByteBuffer_encodesCharBuffer() {
        CharBuffer source = CharBuffer.wrap("CharBufferContent");
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, converter);
        assertNotNull(bb, "ByteBuffer should not be null");
        Charset cs = converter.getOptions().getCharset();
        CharBuffer decoded = cs.decode(bb);
        assertEquals(source.toString(), decoded.toString(), "Decoded ByteBuffer should match the original CharBuffer content");
    }

    /**
     * Verify the behaviour when the source object is {@code null}.
     * The implementation delegates to {@code toCharBuffer(null, …)} which
     * yields an empty CharBuffer; encoding that results in an empty ByteBuffer.
     */
    @Test
    void toByteBuffer_nullSourceProducesEmptyBuffer() {
        ByteBuffer bb = CharBufferConversions.toByteBuffer(null, converter);
        assertNotNull(bb, "ByteBuffer should not be null even for null source");
        assertEquals(0, bb.remaining(), "ByteBuffer should be empty for a null source");
    }

    @ParameterizedTest(name = "[{index}] String ''{0}'' with charset {1}")
    @MethodSource("stringInputProvider")
    void toByteBuffer_stringInput(String source, Charset charset, byte[] expectedBytes) {
        Converter conv = mockConverterWithCharset(charset);
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertArrayEquals(expectedBytes, toByteArray(bb));
    }

    @ParameterizedTest
    @MethodSource("charBufferProvider")
    void toByteBuffer_charBufferInput(CharBuffer source, Charset charset, byte[] expected) {
        Converter conv = mockConverterWithCharset(charset);
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertArrayEquals(expected, toByteArray(bb));
    }

    @ParameterizedTest
    @MethodSource("charArrayProvider")
    void toByteBuffer_charArrayInput(char[] source, Charset charset, byte[] expected) {
        Converter conv = mockConverterWithCharset(charset);
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertArrayEquals(expected, toByteArray(bb));
    }

    // ---------- 4. Valid conversions – null source ----------
    @Test
    void toByteBuffer_nullSourceProducesEmpty() {
        Converter conv = mockConverterWithCharset(Charset.forName("UTF-8"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(null, conv);
        assertNotNull(bb);
        assertEquals(0, bb.remaining());
    }

    @ParameterizedTest
    @MethodSource("charsetVariationProvider")
    void toByteBuffer_charsetVariations(String source, Charset charset, byte[] expected) {
        Converter conv = mockConverterWithCharset(charset);
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertArrayEquals(expected, toByteArray(bb));
    }

    // ---------- 6. Error handling – null converter ----------
    @Test
    void toByteBuffer_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteBuffer("test", null));
    }

    // ---------- 7. Error handling – converter with null options ----------
    @Test
    void toByteBuffer_converterWithNullOptions_throwsNPE() {
        Converter mockConv = Mockito.mock(Converter.class);
        Mockito.when(mockConv.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteBuffer("test", mockConv));
    }

    // ---------- 8. Error handling – options with null charset ----------
    @Test
    void toByteBuffer_optionsWithNullCharset_throwsNPE() {
        ConverterOptions mockOpts = Mockito.mock(ConverterOptions.class);
        Mockito.when(mockOpts.getCharset()).thenReturn(null);
        Converter mockConv = Mockito.mock(Converter.class);
        Mockito.when(mockConv.getOptions()).thenReturn(mockOpts);
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteBuffer("test", mockConv));
    }

    // ---------- 9. Large input handling ----------
    @Test
    void toByteBuffer_largeString() {
        String source = "a".repeat(10_000);
        Converter conv = mockConverterWithCharset(Charset.forName("UTF-8"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertEquals(10_000, bb.remaining());
        // Verify content by decoding back.
        Charset cs = conv.getOptions().getCharset();
        CharBuffer decoded = cs.decode(bb);
        assertEquals(source, decoded.toString());
    }

    @ParameterizedTest
    @MethodSource("stringBuilderProvider")
    void toByteBuffer_stringBuilderInput(StringBuilder source, Charset charset, byte[] expected) {
        Converter conv = mockConverterWithCharset(charset);
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertArrayEquals(expected, toByteArray(bb));
    }

    // ---------- 11. Valid conversions – StringBuffer ----------
    @Test
    void toByteBuffer_stringBufferInput() {
        StringBuffer source = new StringBuffer("Buffer");
        Converter conv = mockConverterWithCharset(Charset.forName("UTF-8"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertArrayEquals("Buffer".getBytes(Charset.forName("UTF-8")), toByteArray(bb));
    }

    // ---------- 12. Valid conversions – Empty char[] ----------
    @Test
    void toByteBuffer_emptyCharArray() {
        char[] source = new char[0];
        Converter conv = mockConverterWithCharset(Charset.forName("UTF-8"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertEquals(0, bb.remaining());
    }

    // ---------- 13. Valid conversions – Empty CharBuffer ----------
    @Test
    void toByteBuffer_emptyCharBuffer() {
        // position == limit -> empty view
        CharBuffer source = CharBuffer.allocate(10);
        source.limit(0);
        Converter conv = mockConverterWithCharset(Charset.forName("UTF-8"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertEquals(0, bb.remaining());
    }

    // ---------- 14. CharBuffer with non‑zero position/limit ----------
    @Test
    void toByteBuffer_charBufferSlice() {
        CharBuffer source = CharBuffer.wrap("prefixXYZsuffix");
        // remaining = "XYZ"
        source.position(6).limit(9);
        Converter conv = mockConverterWithCharset(Charset.forName("UTF-8"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertArrayEquals("XYZ".getBytes(Charset.forName("UTF-8")), toByteArray(bb));
    }

    // ---------- 15. Charset handling – US_ASCII with non‑ASCII ----------
    @Test
    void toByteBuffer_usAsciiReplacesUnmappable() {
        String source = "café";
        Converter conv = mockConverterWithCharset(Charset.forName("US-ASCII"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        // Expected: c a f ? (0x3F)
        assertArrayEquals(new byte[] { 'c', 'a', 'f', '?' }, toByteArray(bb));
    }

    // ---------- 16. Charset handling – ISO_8859_1 with unmappable ----------
    @Test
    void toByteBuffer_iso8859_1ReplacesEuro() {
        // Euro sign not in ISO‑8859‑1
        String source = "€";
        Converter conv = mockConverterWithCharset(Charset.forName("ISO-8859-1"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertArrayEquals(new byte[] { '?' }, toByteArray(bb));
    }

    // ---------- 17. Error handling – unsupported source type ----------
    @Test
    void toByteBuffer_unsupportedSource_throwsIllegalArgumentException() {
        Integer source = Integer.valueOf(12345);
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toByteBuffer(source, converter));
    }

    // ---------- 18. Valid conversions – surrogate pair (emoji) ----------
    @Test
    void toByteBuffer_emojiSurrogatePair() {
        // grinning face emoji
        String source = "😀";
        Converter conv = mockConverterWithCharset(Charset.forName("UTF-8"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertArrayEquals(new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }, toByteArray(bb));
    }

    // ---------- 19. Large mixed‑content string ----------
    @Test
    void toByteBuffer_largeMixedGreekString() {
        String partA = "α".repeat(5_000);
        String partB = "β".repeat(5_000);
        // total 10_000 characters, each 2 bytes in UTF‑8
        String source = partA + partB;
        Converter conv = mockConverterWithCharset(Charset.forName("UTF-8"));
        ByteBuffer bb = CharBufferConversions.toByteBuffer(source, conv);
        assertNotNull(bb);
        assertEquals(20_000, bb.remaining());
        // Spot‑check first and last bytes
        byte[] arr = toByteArray(bb);
        // first byte of α
        assertEquals((byte) 0xCE, arr[0]);
        // second byte of α
        assertEquals((byte) 0xB1, arr[1]);
        // first byte of β
        assertEquals((byte) 0xCE, arr[arr.length - 2]);
        // second byte of β
        assertEquals((byte) 0xB2, arr[arr.length - 1]);
    }
}
