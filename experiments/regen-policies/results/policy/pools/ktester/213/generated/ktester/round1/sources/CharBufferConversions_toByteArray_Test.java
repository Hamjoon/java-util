package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class CharBufferConversions_toByteArray_Test {

    /**
     * Converter used for all tests – created once because it has no mutable state that
     *  interferes with the tests.
     */
    private static Converter converter;

    /**
     * Shared Converter instance – it has no mutable state that interferes with the tests.
     */
    private static Converter defaultConverter;

    /* -------------------------------------------------------------
     *  Parameterised tests for the bulk of the conversion scenarios
     * ------------------------------------------------------------- */
    private static class TestCase {

        final Object from;

        // null when an exception is expected
        final byte[] expectedBytes;

        final Class<? extends Throwable> expectedException;

        final String description;

        TestCase(Object from, byte[] expectedBytes, Class<? extends Throwable> expectedException, String description) {
            this.from = from;
            this.expectedBytes = expectedBytes;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter has a public constructor that takes a ConverterOptions instance.
        // Using the default options is sufficient for the built‑in conversions we need.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Provides test cases for successful conversions (no exception expected).
     */
    private static Stream<TestCase> successfulConversionProvider() {
        return Stream.of(new TestCase(CharBuffer.wrap("abc"), "abc".getBytes(StandardCharsets.UTF_8), null, "ASCII CharBuffer"), new TestCase(CharBuffer.wrap("こんにちは"), // UTF‑8 bytes for the Japanese greeting
        new byte[] { (byte) 227, (byte) 129, (byte) 147, (byte) 227, (byte) 130, (byte) 147, (byte) 227, (byte) 129, (byte) 171, (byte) 227, (byte) 129, (byte) 161, (byte) 227, (byte) 129, (byte) 175 }, null, "Unicode CharBuffer"), new TestCase("hello", "hello".getBytes(StandardCharsets.UTF_8), null, "ASCII String"), new TestCase(// single emoji (surrogate pair)
        "😀", new byte[] { (byte) 240, (byte) 159, (byte) 152, (byte) 128 }, null, "Unicode String (emoji)"), new TestCase(new char[] { 'J', 'a', 'v', 'a' }, "Java".getBytes(StandardCharsets.UTF_8), null, "ASCII char[]"), new TestCase(CharBuffer.allocate(0), new byte[0], null, "Empty CharBuffer"), new TestCase("", new byte[0], null, "Empty String"), new TestCase(new byte[] { 1, 2, 3, 4 }, new byte[] { 1, 2, 3, 4 }, null, "Byte[] identity"), new TestCase(ByteBuffer.wrap(new byte[] { 5, 6, 7 }), new byte[] { 5, 6, 7 }, null, "ByteBuffer identity"), new TestCase(String.join("", Collections.nCopies(1000, "a")), // 1000 times the byte value for 'a' (97)
        new byte[1000], null, "Large data (1000 'a's)"));
    }

    /**
     * Provides test cases that are expected to throw an exception.
     */
    private static Stream<TestCase> exceptionalConversionProvider() {
        return Stream.of(new TestCase(null, null, NullPointerException.class, "Null source with non‑null converter"), new TestCase(12345, null, IllegalArgumentException.class, "Unsupported source type (Integer)"), new TestCase(new Double(3.14), null, IllegalArgumentException.class, "Unsupported source type (Double)"), new TestCase(new Object(), null, IllegalArgumentException.class, "Unsupported source type (custom Object)"));
    }

    @Test
    void toByteArray_fromCharBuffer_returnsUtf8Bytes() {
        // given
        CharBuffer source = CharBuffer.wrap("hello world");
        // when
        byte[] result = CharBufferConversions.toByteArray(source, converter);
        // then
        assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    void toByteArray_fromString_returnsUtf8Bytes() {
        // given
        String source = "JUnit‑5";
        // when
        byte[] result = CharBufferConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(source.getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    void toByteArray_fromCharArray_returnsUtf8Bytes() {
        // given
        char[] source = "charArray".toCharArray();
        // when
        byte[] result = CharBufferConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(new String(source).getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    void toByteArray_fromNull_returnsNull() {
        // given
        Object source = null;
        // when
        byte[] result = CharBufferConversions.toByteArray(source, converter);
        // then
        assertNull(result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("successfulConversionProvider")
    void toByteArray_successfulConversions(TestCase tc) {
        // Special handling for the large‑data test: we need to fill the expected byte array.
        if ("Large data (1000 'a's)".equals(tc.description)) {
            Arrays.fill(tc.expectedBytes, (byte) 'a');
        }
        // when
        byte[] result = CharBufferConversions.toByteArray(tc.from, defaultConverter);
        // then
        assertArrayEquals(tc.expectedBytes, result, tc.description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionalConversionProvider")
    void toByteArray_expectedExceptions(TestCase tc) {
        // when / then
        assertThrows(tc.expectedException, () -> CharBufferConversions.toByteArray(tc.from, defaultConverter), tc.description);
    }

    @Test
    void toByteArray_nullConverter_throwsNPE() {
        // given
        String source = "test";
        // when / then
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteArray(source, null), "Null converter with non‑null source should throw NPE");
    }

    @Test
    void toByteArray_bothArgumentsNull_throwsNPE() {
        // both parameters null – fast‑fail with NPE
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteArray(null, null), "Both arguments null should throw NPE");
    }

    @Test
    void toByteArray_converterThrowsRuntimeException_propagates() {
        // Arrange a mock Converter that throws a RuntimeException on any conversion call.
        Converter throwingConverter = Mockito.mock(Converter.class);
        Mockito.when(throwingConverter.convert(any(), any())).thenThrow(new RuntimeException("forced failure"));
        // The source can be any supported type; we use a simple String.
        String source = "test";
        // Act & Assert – the RuntimeException must propagate unchanged.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharBufferConversions.toByteArray(source, throwingConverter), "Exception from Converter should propagate");
        assertEquals("forced failure", ex.getMessage());
    }
}
