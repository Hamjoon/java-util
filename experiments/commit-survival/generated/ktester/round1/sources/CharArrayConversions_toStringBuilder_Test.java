package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.*;
import java.util.Date;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toStringBuilder_Test {

    /**
     * Shared {@link Converter} instance used for the tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used for the default conversion tests.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter works with the default options; no external resources are required.
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /**
     * Helper that simply forwards to the method under test.
     */
    private static StringBuilder invoke(Object from, Converter converter) {
        return CharArrayConversions.toStringBuilder(from, converter);
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// String
        Arguments.of("plain String", "hello", null, "hello"), // char[]
        Arguments.of("char array", new char[] { 'a', 'b', 'c' }, null, "abc"), // CharBuffer
        Arguments.of("CharBuffer", CharBuffer.wrap("world"), null, "world"));
    }

    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(// null source + null converter → literal "null"
        Arguments.of("null source & null converter", null, null, "null"), // empty String
        Arguments.of("empty String", "", null, ""), // empty char[]
        Arguments.of("empty char[]", new char[] {}, null, ""), // empty CharBuffer
        Arguments.of("empty CharBuffer", CharBuffer.wrap(""), null, ""), // empty ByteBuffer (ASCII zero‑length)
        Arguments.of("empty ByteBuffer", ByteBuffer.wrap(new byte[] {}), null, ""));
    }

    private static Stream<Arguments> builtInTypeProvider() {
        return Stream.of(// ByteBuffer with ASCII bytes
        Arguments.of("ByteBuffer ASCII", ByteBuffer.wrap(new byte[] { 97, 98, 99 }), null, "abc"), // StringBuilder passed directly
        Arguments.of("StringBuilder passthrough", new StringBuilder("builder"), null, "builder"), // StringBuffer passed directly
        Arguments.of("StringBuffer passthrough", new StringBuffer("buffer"), null, "buffer"));
    }

    @Test
    void toStringBuilder_givenCharBuffer_returnsMatchingStringBuilder() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("Hello CharBuffer".toCharArray());
        // WHEN
        StringBuilder result = CharArrayConversions.toStringBuilder(source, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder must not be null");
        assertEquals(source.toString(), result.toString(), "StringBuilder content should match the original CharBuffer");
    }

    @Test
    void toStringBuilder_givenString_returnsMatchingStringBuilder() {
        // GIVEN
        String source = "Hello String";
        // WHEN
        StringBuilder result = CharArrayConversions.toStringBuilder(source, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder must not be null");
        assertEquals(source, result.toString(), "StringBuilder content should match the original String");
    }

    @Test
    void toStringBuilder_givenCharArray_returnsMatchingStringBuilder() {
        // GIVEN
        char[] source = "Hello CharArray".toCharArray();
        // WHEN
        StringBuilder result = CharArrayConversions.toStringBuilder(source, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder must not be null");
        assertEquals(new String(source), result.toString(), "StringBuilder content should match the original char array");
    }

    @Test
    void toStringBuilder_givenByteArray_convertsViaConverter() {
        // GIVEN
        // default platform charset (UTF‑8 on most systems)
        byte[] source = "Hello Bytes".getBytes();
        // WHEN
        StringBuilder result = CharArrayConversions.toStringBuilder(source, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder must not be null");
        // The Converter is expected to treat a byte[] as a UTF‑8 encoded string.
        assertEquals(new String(source), result.toString(), "StringBuilder content should match the byte array interpreted as a UTF‑8 string");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionsProvider")
    void toStringBuilder_validConversions(String description, Object from, Converter converter, String expected) {
        // WHEN
        StringBuilder sb = invoke(from, converter != null ? converter : defaultConverter);
        // THEN
        assertNotNull(sb, description + " – result must not be null");
        assertEquals(expected, sb.toString(), description + " – content mismatch");
    }

    @Test
    void toStringBuilder_byteArray_customConverter() {
        // GIVEN
        // "test"
        byte[] source = new byte[] { 116, 101, 115, 116 };
        Converter custom = mock(Converter.class);
        when(custom.convert(source)).thenReturn(CharBuffer.wrap("test"));
        // WHEN
        StringBuilder sb = invoke(source, custom);
        // THEN
        assertNotNull(sb);
        assertEquals("test", sb.toString());
        verify(custom).convert(source);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullAndEmptyProvider")
    void toStringBuilder_nullAndEmptyInputs(String description, Object from, Converter converter, String expected) {
        // WHEN
        StringBuilder sb = invoke(from, converter != null ? converter : defaultConverter);
        // THEN
        assertNotNull(sb, description + " – result must not be null");
        assertEquals(expected, sb.toString(), description + " – content mismatch");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("builtInTypeProvider")
    void toStringBuilder_builtInTypes(String description, Object from, Converter converter, String expected) {
        // WHEN
        StringBuilder sb = invoke(from, converter != null ? converter : defaultConverter);
        // THEN
        assertNotNull(sb, description + " – result must not be null");
        assertEquals(expected, sb.toString(), description + " – content mismatch");
    }

    @Test
    void toStringBuilder_customConverterTransformsDate() {
        // GIVEN
        Date epoch = new Date(0L);
        Converter custom = mock(Converter.class);
        when(custom.convert(epoch)).thenReturn(CharBuffer.wrap("epoch"));
        // WHEN
        StringBuilder sb = invoke(epoch, custom);
        // THEN
        assertNotNull(sb);
        assertEquals("epoch", sb.toString());
        verify(custom).convert(epoch);
    }

    @Test
    void toStringBuilder_customConverterReturnsNull_throwsNPE() {
        // GIVEN
        Object anyObj = new Object();
        Converter custom = mock(Converter.class);
        when(custom.convert(anyObj)).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> invoke(anyObj, custom), "When a custom Converter returns null, toStringBuilder should throw NPE");
    }

    @Test
    void toStringBuilder_unsupportedType_withoutConverter_throwsIAE() {
        // GIVEN
        BigDecimal bd = new BigDecimal("123.45");
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> invoke(bd, null), "Unsupported source type without a converter must raise IllegalArgumentException");
    }

    @Test
    void toStringBuilder_unsupportedType_withNullConverter_throwsIAE() {
        // GIVEN
        // Date is not handled internally
        Date date = new Date(0L);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> invoke(date, null), "Supported source type that requires a custom converter but none supplied must raise IllegalArgumentException");
    }

    /**
     * If a future change makes {@code toStringBuilder} private, this helper
     * demonstrates how to call it via reflection without breaking the test
     * suite. Currently the method is package‑private, so direct calls are used.
     */
    @SuppressWarnings("unused")
    private static StringBuilder invokeReflectively(Object from, Converter converter) throws Exception {
        Method m = CharArrayConversions.class.getDeclaredMethod("toStringBuilder", Object.class, Converter.class);
        m.setAccessible(true);
        return (StringBuilder) m.invoke(null, from, converter);
    }
}
