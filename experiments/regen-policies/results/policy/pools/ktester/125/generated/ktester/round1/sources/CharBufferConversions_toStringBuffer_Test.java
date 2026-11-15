package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toStringBuffer_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used by most tests.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter has a no‑arg constructor that creates a
        // ConverterOptions instance with the standard configuration.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference (helps GC when many test classes run together).
        converter = null;
    }

    static Stream<Arguments> stringSourceProvider() {
        return Stream.of(Arguments.of("Hello World", "Hello World"), Arguments.of("", ""));
    }

    static Stream<Arguments> charArraySourceProvider() {
        return Stream.of(Arguments.of(new char[] { 'a', 'b', 'c' }, "abc"), Arguments.of(new char[] {}, ""));
    }

    static Stream<Arguments> byteBufferSourceProvider() {
        ByteBuffer utf8 = ByteBuffer.wrap("µñ".getBytes(StandardCharsets.UTF_8));
        ByteBuffer empty = ByteBuffer.allocate(0);
        return Stream.of(Arguments.of(utf8, "µñ", "UTF‑8 ByteBuffer should be decoded correctly"), Arguments.of(empty, "", "Empty ByteBuffer should produce an empty StringBuffer"));
    }

    @Test
    void toStringBuffer_fromString_returnsMatchingStringBuffer() {
        // GIVEN
        String source = "Hello, world!";
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals(source, result.toString(), "StringBuffer content should match the original String");
    }

    @Test
    void toStringBuffer_fromCharBuffer_returnsMatchingStringBuffer() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("CharBuffer content");
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals(source.toString(), result.toString(), "StringBuffer content should match the original CharBuffer");
    }

    @Test
    void toStringBuffer_fromNull_returnsEmptyStringBuffer() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuffer result = CharBufferConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null even for null input");
        assertEquals(0, result.length(), "StringBuffer created from a null source should be empty");
    }

    @Test
    void toStringBuffer_nullSourceAndNullConverter_returnsEmptyStringBuffer() {
        StringBuffer result = CharBufferConversions.toStringBuffer(null, null);
        assertNotNull(result, "Result must not be null even when both arguments are null");
        assertEquals(0, result.length(), "Resulting StringBuffer should be empty");
    }

    @Test
    void toStringBuffer_nullSourceNonNullConverter_returnsEmptyStringBuffer() {
        StringBuffer result = CharBufferConversions.toStringBuffer(null, defaultConverter);
        assertNotNull(result);
        assertEquals(0, result.length(), "Null source should yield an empty StringBuffer");
    }

    @Test
    void toStringBuffer_nonNullSourceNullConverter_string() {
        String source = "simple";
        StringBuffer result = CharBufferConversions.toStringBuffer(source, null);
        assertNotNull(result);
        assertEquals(source, result.toString(), "When converter is null, a String source should be copied verbatim");
    }

    @Test
    void toStringBuffer_nonNullSourceNullConverter_charBuffer() {
        CharBuffer source = CharBuffer.wrap("buffer");
        StringBuffer result = CharBufferConversions.toStringBuffer(source, null);
        assertNotNull(result);
        assertEquals(source.toString(), result.toString(), "When converter is null, an existing CharBuffer should be wrapped unchanged");
    }

    @ParameterizedTest(name = "[{index}] source=\"{0}\"")
    @MethodSource("stringSourceProvider")
    void toStringBuffer_stringSource(String source, String expected) {
        StringBuffer result = CharBufferConversions.toStringBuffer(source, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.toString(), "String source should be converted to an equivalent StringBuffer");
    }

    @ParameterizedTest(name = "[{index}] charArray={0}")
    @MethodSource("charArraySourceProvider")
    void toStringBuffer_charArraySource(char[] source, String expected) {
        StringBuffer result = CharBufferConversions.toStringBuffer(source, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.toString(), "char[] source should be converted to a StringBuffer with matching content");
    }

    @Test
    void toStringBuffer_charBufferSource() {
        CharBuffer source = CharBuffer.wrap(new char[] { 'x', 'y', 'z' });
        StringBuffer result = CharBufferConversions.toStringBuffer(source, defaultConverter);
        assertNotNull(result);
        assertEquals("xyz", result.toString(), "CharBuffer source should be copied into a new StringBuffer");
    }

    @ParameterizedTest(name = "[{index}] description={1}")
    @MethodSource("byteBufferSourceProvider")
    void toStringBuffer_byteBufferSource(ByteBuffer source, String expected, String description) {
        StringBuffer result = CharBufferConversions.toStringBuffer(source, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.toString(), description);
    }

    @Test
    void toStringBuffer_stringBuilderSource() {
        StringBuilder source = new StringBuilder("builder");
        StringBuffer result = CharBufferConversions.toStringBuffer(source, defaultConverter);
        assertNotNull(result);
        assertEquals("builder", result.toString(), "StringBuilder should be converted to a StringBuffer preserving its characters");
    }

    @Test
    void toStringBuffer_largeStringBoundary() {
        String large = "a".repeat(10_000);
        StringBuffer result = CharBufferConversions.toStringBuffer(large, defaultConverter);
        assertNotNull(result);
        assertEquals(10_000, result.length(), "Resulting StringBuffer should contain exactly 10,000 characters");
        assertTrue(result.chars().allMatch(ch -> ch == 'a'), "All characters in the large StringBuffer should be 'a'");
    }

    @Test
    void toStringBuffer_unsupportedSource_throwsIllegalArgumentException() {
        // not a supported character source
        Integer source = 42;
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuffer(source, defaultConverter), "Unsupported source types should trigger IllegalArgumentException");
    }
}
