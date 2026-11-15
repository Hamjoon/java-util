package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toStringBuilder_Test {

    /**
     * The shared Converter used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the default options are sufficient for our tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test suites.
        converter = null;
    }

    /**
     * Supplies empty inputs for CharBuffer, String, byte[], and ByteBuffer.
     */
    private static Stream<Arguments> emptyInputProvider() {
        return Stream.of(// empty CharBuffer
        Arguments.of(CharBuffer.wrap("")), // empty String
        Arguments.of(""), // empty byte[]
        Arguments.of(new byte[] {}), // empty ByteBuffer
        Arguments.of(ByteBuffer.wrap(new byte[] {})));
    }

    @Test
    void shouldCreateStringBuilderFromString() {
        // given
        String source = "Hello, World!";
        // when
        StringBuilder result = ByteBufferConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result, "Resulting StringBuilder must not be null");
        assertEquals(source, result.toString(), "StringBuilder content should match the source string");
    }

    @Test
    void shouldCreateStringBuilderFromCharBuffer() {
        // given
        CharBuffer source = CharBuffer.wrap("CharBufferTest");
        // when
        StringBuilder result = ByteBufferConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result);
        assertEquals(source.toString(), result.toString());
    }

    @Test
    void shouldCreateStringBuilderFromByteBuffer() {
        // given
        byte[] bytes = "ByteBufferTest".getBytes(StandardCharsets.UTF_8);
        ByteBuffer source = ByteBuffer.wrap(bytes);
        // when
        StringBuilder result = ByteBufferConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result);
        assertEquals("ByteBufferTest", result.toString());
    }

    @Test
    void shouldCreateStringBuilderFromByteArray() {
        // given
        byte[] source = "ByteArrayTest".getBytes(StandardCharsets.UTF_8);
        // when
        StringBuilder result = ByteBufferConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result);
        assertEquals("ByteArrayTest", result.toString());
    }

    @Test
    void shouldReturnEmptyStringBuilderWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        StringBuilder result = ByteBufferConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.length(), "Result should be an empty StringBuilder for a null source");
    }

    /**
     * CharBuffer input – converter is {@code null}.
     */
    @Test
    void shouldCreateStringBuilderFromCharBufferWhenConverterIsNull() {
        CharBuffer source = CharBuffer.wrap("abc");
        StringBuilder sb = ByteBufferConversions.toStringBuilder(source, null);
        assertNotNull(sb);
        assertEquals("abc", sb.toString(), "When the source is a CharBuffer, the resulting StringBuilder must contain the same characters.");
    }

    /**
     * String (CharSequence) input – converter is {@code null}.
     */
    @Test
    void shouldCreateStringBuilderFromStringWhenConverterIsNull() {
        String source = "def";
        StringBuilder sb = ByteBufferConversions.toStringBuilder(source, null);
        assertNotNull(sb);
        assertEquals("def", sb.toString(), "When the source is a CharSequence, the method should produce an equivalent StringBuilder.");
    }

    /**
     * byte[] input (UTF‑8) – converter is {@code null}.
     */
    @Test
    void shouldCreateStringBuilderFromByteArrayWhenConverterIsNull() {
        // UTF‑8 bytes for "ghi"
        byte[] source = new byte[] { 'g', 'h', 'i' };
        StringBuilder sb = ByteBufferConversions.toStringBuilder(source, null);
        assertNotNull(sb);
        assertEquals("ghi", sb.toString(), "A UTF‑8 byte array must be decoded and wrapped in a StringBuilder.");
    }

    /**
     * ByteBuffer input (UTF‑8) – converter is {@code null}.
     */
    @Test
    void shouldCreateStringBuilderFromByteBufferWhenConverterIsNull() {
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 'j', 'k', 'l' });
        StringBuilder sb = ByteBufferConversions.toStringBuilder(source, null);
        assertNotNull(sb);
        assertEquals("jkl", sb.toString(), "A UTF‑8 ByteBuffer must be converted to characters and placed in the resulting StringBuilder.");
    }

    /**
     * Unsupported type – conversion delegated to a mocked Converter.
     */
    @Test
    void shouldDelegateUnsupportedTypeToConverter(@Mock Converter mockConverter) {
        // Arrange: mock converter to return the desired CharBuffer
        when(mockConverter.convert(any(), eq(CharBuffer.class))).thenReturn(CharBuffer.wrap("mno"));
        // any type not directly handled
        Object unsupported = new Object();
        // Act
        StringBuilder sb = ByteBufferConversions.toStringBuilder(unsupported, mockConverter);
        // Assert
        assertNotNull(sb);
        assertEquals("mno", sb.toString(), "When the source type is unsupported, the provided Converter should be used.");
        verify(mockConverter, times(1)).convert(eq(unsupported), eq(CharBuffer.class));
    }

    /**
     * Null source and null converter – should yield an empty StringBuilder.
     */
    @Test
    void shouldReturnEmptyStringBuilderWhenBothSourceAndConverterAreNull() {
        StringBuilder sb = ByteBufferConversions.toStringBuilder(null, null);
        assertNotNull(sb);
        assertEquals(0, sb.length(), "Both source and converter are null – the method should return an empty StringBuilder.");
    }

    /**
     * Parameterized test for the various empty‑input scenarios.
     */
    @ParameterizedTest(name = "{index} => source={0}")
    @MethodSource("emptyInputProvider")
    void shouldReturnEmptyStringBuilderForEmptyInputs(Object source) {
        StringBuilder sb = ByteBufferConversions.toStringBuilder(source, null);
        assertNotNull(sb);
        assertEquals(0, sb.length(), "Empty input of type " + (source == null ? "null" : source.getClass().getSimpleName()) + " must produce an empty StringBuilder.");
    }

    /**
     * Unsupported type without a Converter – expect NullPointerException.
     */
    @Test
    void shouldThrowNPEWhenUnsupportedTypeAndConverterIsNull() {
        // a type not handled directly
        Object unsupported = new java.util.Date();
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toStringBuilder(unsupported, null), "When no Converter is supplied for an unsupported type, a NullPointerException is expected.");
    }

    /**
     * Converter throws a runtime exception – the exception must propagate.
     */
    @Test
    void shouldPropagateExceptionFromConverter(@Mock Converter mockConverter) {
        // Arrange: make the mock throw an IllegalStateException
        when(mockConverter.convert(any(), eq(CharBuffer.class))).thenThrow(new IllegalStateException("conversion failed"));
        Object source = new Object();
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ByteBufferConversions.toStringBuilder(source, mockConverter), "Exceptions thrown by the Converter should propagate unchanged.");
        assertEquals("conversion failed", ex.getMessage());
    }

    /**
     * Converter returns {@code null} – new StringBuilder(null) should cause NPE.
     */
    @Test
    void shouldThrowNPEWhenConverterReturnsNull(@Mock Converter mockConverter) {
        // Arrange: mock returns null instead of a CharBuffer
        when(mockConverter.convert(any(), eq(CharBuffer.class))).thenReturn(null);
        Object source = new Object();
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toStringBuilder(source, mockConverter), "If the Converter returns null, constructing a StringBuilder should throw NullPointerException.");
    }
}
