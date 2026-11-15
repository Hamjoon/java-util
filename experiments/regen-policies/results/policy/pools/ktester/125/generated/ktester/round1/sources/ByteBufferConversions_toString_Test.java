package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toString_Test {

    /**
     * Converter used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter is fully functional out‑of‑the‑box.
        converter = new Converter(new ConverterOptions());
    }

    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// CharBuffer → same content
        Arguments.of(CharBuffer.wrap("hello"), new NoOpConverter(), "hello"), // String → same content
        Arguments.of("world", new NoOpConverter(), "world"), // byte[] (ASCII) → UTF‑8 decoder
        Arguments.of(new byte[] { 0x41, 0x42, 0x43 }, new Utf8ByteArrayConverter(), "ABC"), // ByteBuffer (ASCII) → UTF‑8 decoder
        Arguments.of(ByteBuffer.wrap(new byte[] { 0x31, 0x32, 0x33 }), new Utf8ByteBufferConverter(), "123"), // char[] → wrapped
        Arguments.of(new char[] { 'x', 'y', 'z' }, new NoOpConverter(), "xyz"));
    }

    static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// Empty string
        Arguments.of("", new NoOpConverter(), ""), // Empty CharBuffer (capacity 0)
        Arguments.of(CharBuffer.allocate(0), new NoOpConverter(), ""), // Empty byte array decoded as UTF‑8
        Arguments.of(new byte[] {}, new Utf8ByteArrayConverter(), ""), // Empty ByteBuffer decoded as UTF‑8
        Arguments.of(ByteBuffer.allocate(0), new Utf8ByteBufferConverter(), ""), // Empty char array
        Arguments.of(new char[] {}, new NoOpConverter(), ""), // Control characters should be preserved
        Arguments.of("\u0000\u0001\u007F", new NoOpConverter(), "\u0000\u0001\u007F"), // Surrogate pair (emoji) in CharBuffer
        Arguments.of(CharBuffer.wrap("\uD83D\uDE00"), new NoOpConverter(), "\uD83D\uDE00"), // UTF‑8 byte sequence for 😀 emoji
        Arguments.of(new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }, new Utf8ByteArrayConverter(), "\uD83D\uDE00"), // ByteBuffer containing UTF‑8 encoded 😀 emoji
        Arguments.of(ByteBuffer.wrap(new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }), new Utf8ByteBufferConverter(), "\uD83D\uDE00"));
    }

    static Stream<Arguments> exceptionScenarioProvider() {
        return Stream.of(// Unsupported source type (Integer)
        Arguments.of(Integer.valueOf(42), new NoOpConverter(), IllegalArgumentException.class), // Malformed UTF‑8 byte sequence
        Arguments.of(new byte[] { (byte) 0xC3, (byte) 0x28 }, new Utf8ByteArrayConverter(), IllegalArgumentException.class), // String source with a converter that expects a byte[]
        Arguments.of("test", new Utf8ByteArrayConverter(), IllegalArgumentException.class), // ByteBuffer source with a converter that cannot handle ByteBuffer
        Arguments.of(ByteBuffer.wrap(new byte[] { 0x41, 0x42 }), new NoOpConverter(), IllegalArgumentException.class), // Generic Object with no conversion rule
        Arguments.of(new Object(), new NoOpConverter(), IllegalArgumentException.class));
    }

    /**
     * Verify that a plain {@link String} is returned unchanged.
     */
    @Test
    void toString_givenString_returnsSameString() {
        // given
        String source = "Hello, world!";
        // when
        String result = ByteBufferConversions.toString(source, converter);
        // then
        assertEquals(source, result);
    }

    /**
     * Verify that a {@link CharBuffer} is correctly turned into a {@link String}.
     */
    @Test
    void toString_givenCharBuffer_returnsString() {
        // given
        CharBuffer source = CharBuffer.wrap("CharBuffer content");
        // when
        String result = ByteBufferConversions.toString(source, converter);
        // then
        assertEquals(source.toString(), result);
    }

    /**
     * Verify that a {@code byte[]} containing UTF‑8 bytes is converted to the expected string.
     */
    @Test
    void toString_givenByteArray_returnsString() {
        // given
        // default charset is fine for ASCII test
        byte[] source = "Byte array text".getBytes();
        // when
        String result = ByteBufferConversions.toString(source, converter);
        // then
        assertEquals("Byte array text", result);
    }

    /**
     * Verify that a {@link ByteBuffer} is converted to the expected string.
     */
    @Test
    void toString_givenByteBuffer_returnsString() {
        // given
        ByteBuffer source = ByteBuffer.wrap("ByteBuffer content".getBytes());
        // when
        String result = ByteBufferConversions.toString(source, converter);
        // then
        assertEquals("ByteBuffer content", result);
    }

    /**
     * Verify that passing {@code null} as the source throws a {@link NullPointerException}.
     * The implementation of {@code toCharBuffer} delegates to the {@link Converter},
     * which returns {@code null} for a null source; the subsequent {@code toString()}
     * call therefore throws NPE.
     */
    @Test
    void toString_givenNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toString(source, converter));
    }

    @ParameterizedTest(name = "[{index}] toString({0}) with {1} → \"{2}\"")
    @MethodSource("validConversionProvider")
    void toString_validConversions(Object source, Converter conv, String expected) {
        String result = ByteBufferConversions.toString(source, conv);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] edge case source={0}")
    @MethodSource("edgeCaseProvider")
    void toString_edgeCases(Object source, Converter conv, String expected) {
        String result = ByteBufferConversions.toString(source, conv);
        assertEquals(expected, result);
    }

    /**
     * Passing a {@code null} converter should result in {@link NullPointerException}.
     */
    @Test
    void toString_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toString("test", null));
    }

    @ParameterizedTest(name = "[{index}] toString({0}) with {1} throws {2}")
    @MethodSource("exceptionScenarioProvider")
    void toString_exceptionScenarios(Object source, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ByteBufferConversions.toString(source, conv));
    }
}
