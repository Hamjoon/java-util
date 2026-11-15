package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toStringBuilder_Test {

    private Converter converter;

    private Converter defaultConverter;

    /**
     * Simple holder for a test input / expected pair.
     * The {@code input} field can be any object accepted by {@code toStringBuilder}.
     */
    private static class ArgumentsHolder {

        final Object input;

        final String expected;

        ArgumentsHolder(Object input, String expected) {
            this.input = input;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return String.valueOf(input);
        }
    }

    @BeforeEach
    void setupBeforeEach(@Mock ConverterOptions mockOptions) {
        // Mock the ConverterOptions to return an empty overrides map
        MockitoAnnotations.openMocks(this);
        Map<Class<?>, Object> emptyOverrides = Collections.emptyMap();
        when(mockOptions.getConverterOverrides()).thenReturn(emptyOverrides);
        // Create the real Converter used for the majority of tests
        defaultConverter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock and the converter reference
        converter = null;
    }

    private static Stream<ArgumentsHolder> provideValidCharBuffer() {
        return Stream.of(new ArgumentsHolder(CharBuffer.wrap("hello"), "hello"));
    }

    private static Stream<ArgumentsHolder> provideValidString() {
        return Stream.of(new ArgumentsHolder("hello", "hello"), new ArgumentsHolder("", ""));
    }

    private static Stream<ArgumentsHolder> provideValidStringBuilder() {
        return Stream.of(new ArgumentsHolder(new StringBuilder("world"), "world"));
    }

    private static Stream<ArgumentsHolder> provideValidStringBuffer() {
        return Stream.of(new ArgumentsHolder(new StringBuffer("buffer"), "buffer"));
    }

    private static Stream<ArgumentsHolder> provideValidCharArray() {
        return Stream.of(new ArgumentsHolder(new char[] { 'a', 'b', 'c' }, "abc"), new ArgumentsHolder(new char[0], ""));
    }

    private static Stream<ArgumentsHolder> provideValidByteArray() {
        return Stream.of(new ArgumentsHolder("utf8".getBytes(StandardCharsets.UTF_8), "utf8"));
    }

    private static Stream<ArgumentsHolder> provideValidByteBuffer() {
        return Stream.of(new ArgumentsHolder(ByteBuffer.wrap("bytebuf".getBytes(StandardCharsets.UTF_8)), "bytebuf"));
    }

    @Test
    void toStringBuilder_WithCharBuffer_ReturnsEquivalentStringBuilder() {
        // given
        CharBuffer input = CharBuffer.wrap("hello");
        // when
        StringBuilder result = CharBufferConversions.toStringBuilder(input, converter);
        // then
        assertNotNull(result);
        assertEquals("hello", result.toString());
    }

    @Test
    void toStringBuilder_WithString_ReturnsEquivalentStringBuilder() {
        // given
        String input = "world";
        // when
        StringBuilder result = CharBufferConversions.toStringBuilder(input, converter);
        // then
        assertNotNull(result);
        assertEquals("world", result.toString());
    }

    @Test
    void toStringBuilder_WithCharArray_ReturnsEquivalentStringBuilder() {
        // given
        char[] input = new char[] { 'a', 'b', 'c' };
        // when
        StringBuilder result = CharBufferConversions.toStringBuilder(input, converter);
        // then
        assertNotNull(result);
        assertEquals("abc", result.toString());
    }

    @Test
    void toStringBuilder_WithNull_ReturnsEmptyStringBuilder() {
        // given
        Object input = null;
        // when
        StringBuilder result = CharBufferConversions.toStringBuilder(input, converter);
        // then
        assertNotNull(result);
        assertEquals("", result.toString());
    }

    @ParameterizedTest(name = "[{index}] StringBuilder ''{0}'' → StringBuilder")
    @MethodSource("provideValidStringBuilder")
    void toStringBuilder_WithStringBuilder_ReturnsEquivalentStringBuilder(ArgumentsHolder holder) {
        StringBuilder result = CharBufferConversions.toStringBuilder(holder.input, defaultConverter);
        assertNotNull(result);
        assertEquals(holder.expected, result.toString());
    }

    @ParameterizedTest(name = "[{index}] StringBuffer ''{0}'' → StringBuilder")
    @MethodSource("provideValidStringBuffer")
    void toStringBuilder_WithStringBuffer_ReturnsEquivalentStringBuilder(ArgumentsHolder holder) {
        StringBuilder result = CharBufferConversions.toStringBuilder(holder.input, defaultConverter);
        assertNotNull(result);
        assertEquals(holder.expected, result.toString());
    }

    @ParameterizedTest(name = "[{index}] byte[] (UTF‑8) ''{0}'' → StringBuilder")
    @MethodSource("provideValidByteArray")
    void toStringBuilder_WithByteArrayUtf8_ReturnsEquivalentStringBuilder(ArgumentsHolder holder) {
        StringBuilder result = CharBufferConversions.toStringBuilder(holder.input, defaultConverter);
        assertNotNull(result);
        assertEquals(holder.expected, result.toString());
    }

    @ParameterizedTest(name = "[{index}] ByteBuffer (UTF‑8) ''{0}'' → StringBuilder")
    @MethodSource("provideValidByteBuffer")
    void toStringBuilder_WithByteBufferUtf8_ReturnsEquivalentStringBuilder(ArgumentsHolder holder) {
        StringBuilder result = CharBufferConversions.toStringBuilder(holder.input, defaultConverter);
        assertNotNull(result);
        assertEquals(holder.expected, result.toString());
    }

    @Test
    void toStringBuilder_WithNull_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toStringBuilder(null, defaultConverter));
    }

    @Test
    void toStringBuilder_WithUnsupportedType_ThrowsIllegalArgumentException() {
        Integer unsupported = Integer.valueOf(42);
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toStringBuilder(unsupported, defaultConverter));
    }

    @Test
    void toStringBuilder_WithCustomConverter_AppendsSuffix() {
        // Arrange – a mock Converter that appends "_suffix" to any CharSequence
        Converter mockConverter = mock(Converter.class);
        // The CharBufferConversions implementation eventually asks the Converter to produce a CharBuffer.
        // We stub that interaction.
        when(mockConverter.convert(eq("test"), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap("test_suffix"));
        // Act
        StringBuilder result = CharBufferConversions.toStringBuilder("test", mockConverter);
        // Assert
        assertNotNull(result);
        assertEquals("test_suffix", result.toString());
        // Verify that the mock was exercised exactly once
        verify(mockConverter, times(1)).convert(eq("test"), eq(CharBuffer.class));
    }

    @Test
    void toStringBuilder_WithLargeString_ProducesFullContent() {
        // Build a 10,000‑character string consisting solely of 'x'
        char[] chars = new char[10_000];
        java.util.Arrays.fill(chars, 'x');
        String largeString = new String(chars);
        StringBuilder result = CharBufferConversions.toStringBuilder(largeString, defaultConverter);
        assertNotNull(result);
        assertEquals(10_000, result.length());
        assertTrue(result.chars().allMatch(c -> c == 'x'));
    }
}
