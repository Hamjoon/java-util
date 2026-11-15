package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toStringBuilder_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

    /**
     * Shared real {@link Converter} used for the “no‑custom‑converter” cases.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not require any special options for the basic
        // conversions we are exercising (String, char[], CharBuffer).
        converter = new Converter(new ConverterOptions());
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the
        // test class tidy.
        converter = null;
    }

    /**
     * Test data for the “various input types and converter behaviours” group.
     */
    static Stream<Arguments> provideStandardCases() {
        return Stream.of(// null source + null converter → NPE (as defined by the spec)
        Arguments.of(null, null, NullPointerException.class, null), // CharBuffer source, null converter → same content
        Arguments.of(CharBuffer.wrap("charbuffer"), null, null, "charbuffer"), // char[] source, null converter → same content
        Arguments.of(new char[] { 'J', 'a', 'v', 'a' }, null, null, "Java"), // plain String source, null converter → same content
        Arguments.of("plain string", null, null, "plain string"), // byte[] source with a simple ASCII converter
        Arguments.of(new byte[] { 'A', 'B', 'C' }, simpleByteToCharConverter(), null, "ABC"), // ByteBuffer source with a simple ASCII converter
        Arguments.of(ByteBuffer.wrap(new byte[] { '1', '2', '3' }), simpleByteToCharConverter(), null, "123"), // Integer source with a converter that turns the integer into its string representation
        Arguments.of(Integer.valueOf(456), objectToCharBufferConverter(), null, "456"), // empty String source
        Arguments.of("", null, null, ""));
    }

    /**
     * Test data for the “unsupported source type without converter” group.
     */
    static Stream<Arguments> provideUnsupportedWithoutConverter() {
        return Stream.of(Arguments.of(new Object()));
    }

    /**
     * Test data for the “custom converter returning empty CharBuffer” group.
     */
    static Stream<Arguments> provideEmptyCharBufferConverter() {
        return Stream.of(Arguments.of("any string", emptyCharBufferConverter()));
    }

    /**
     * Test data for the “converter that throws runtime exception” group.
     */
    static Stream<Arguments> provideThrowingConverter() {
        return Stream.of(Arguments.of("trigger", throwingConverter()));
    }

    /**
     * Test data for the “CharBuffer with non‑zero position/limit” group.
     */
    static Stream<Arguments> provideSlicedCharBuffer() {
        CharBuffer sliced = CharBuffer.wrap("abcdef");
        sliced.position(2);
        // characters at indices 2,3,4 -> "cde"
        sliced.limit(5);
        return Stream.of(Arguments.of(sliced, null, "cde"));
    }

    /**
     * Test data for the “source StringBuilder (CharSequence) with null converter” group.
     */
    static Stream<Arguments> provideStringBuilderSource() {
        return Stream.of(Arguments.of(new StringBuilder("sbuilder"), null, "sbuilder"));
    }

    /**
     * Test data for the “byte[] source without converter” group.
     */
    static Stream<Arguments> provideByteArrayWithoutConverter() {
        return Stream.of(Arguments.of(new byte[] { 'x', 'y', 'z' }));
    }

    /**
     * Test data for the “char[] source with non‑null converter (converter ignored)” group.
     */
    static Stream<Arguments> provideCharArrayWithConverter() {
        return Stream.of(Arguments.of(new char[] { 'I', 'g', 'n', 'o', 'r', 'e' }, simpleByteToCharConverter(), "Ignore"));
    }

    /**
     * Simple converter that treats the source as an ASCII byte array and returns a
     * {@link CharBuffer} with the corresponding characters.
     */
    private static Converter simpleByteToCharConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(CharBuffer.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            if (src instanceof byte[]) {
                byte[] bytes = (byte[]) src;
                char[] chars = new char[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    chars[i] = (char) (bytes[i] & 0xFF);
                }
                return CharBuffer.wrap(chars);
            }
            if (src instanceof ByteBuffer) {
                ByteBuffer bb = (ByteBuffer) src;
                byte[] bytes = new byte[bb.remaining()];
                bb.get(bytes);
                char[] chars = new char[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    chars[i] = (char) (bytes[i] & 0xFF);
                }
                return CharBuffer.wrap(chars);
            }
            return null;
        });
        return mock;
    }

    /**
     * Converter that turns any object into its {@code toString()} representation
     * wrapped in a {@link CharBuffer}.
     */
    private static Converter objectToCharBufferConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(CharBuffer.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            return CharBuffer.wrap(String.valueOf(src));
        });
        return mock;
    }

    /**
     * Converter that always returns an empty {@link CharBuffer}.
     */
    private static Converter emptyCharBufferConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(CharBuffer.class))).thenReturn(CharBuffer.allocate(0));
        return mock;
    }

    /**
     * Converter that throws a {@link RuntimeException} whenever it is asked to
     * convert anything.
     */
    private static Converter throwingConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(CharBuffer.class))).thenThrow(new RuntimeException("forced"));
        return mock;
    }

    /**
     * Converter that returns {@code null} for any conversion request.
     */
    private static Converter nullReturningConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(CharBuffer.class))).thenReturn(null);
        return mock;
    }

    @Test
    void toStringBuilder_fromString_returnsMatchingBuilder() {
        String source = "Hello World";
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertNotNull(sb, "Resulting StringBuilder should not be null");
        assertEquals(source, sb.toString(), "StringBuilder content should match the source String");
    }

    @Test
    void toStringBuilder_fromCharArray_returnsMatchingBuilder() {
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertNotNull(sb);
        assertEquals(new String(source), sb.toString(), "StringBuilder content should match the source char array");
    }

    @Test
    void toStringBuilder_fromCharBuffer_returnsMatchingBuilder() {
        CharBuffer source = CharBuffer.wrap("CharBufferContent");
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertNotNull(sb);
        assertEquals(source.toString(), sb.toString(), "StringBuilder content should match the source CharBuffer");
    }

    @Test
    void toStringBuilder_fromEmptyCharArray_returnsEmptyBuilder() {
        char[] source = new char[0];
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertNotNull(sb);
        assertEquals("", sb.toString(), "StringBuilder should be empty when source char array is empty");
    }

    @Test
    void toStringBuilder_fromNull_returnsNullStringBuilder() {
        // The implementation of toCharBuffer(null, …) returns a CharBuffer that
        // represents the string "null". This is the behaviour of the underlying
        // Converter, and we assert that the method does not throw and returns
        // the literal "null".
        StringBuilder sb = CharArrayConversions.toStringBuilder(null, converter);
        assertNotNull(sb, "StringBuilder should not be null even when source is null");
        assertEquals("null", sb.toString(), "StringBuilder should contain the literal \"null\" when source is null");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideStandardCases")
    void toStringBuilder_variousInputs(Object from, Converter converter, Class<? extends Throwable> expectedException, String expectedContent) {
        if (expectedException != null) {
            // Expect an exception (e.g., NullPointerException)
            assertThrows(expectedException, () -> CharArrayConversions.toStringBuilder(from, converter));
        } else {
            // Normal successful conversion
            StringBuilder sb = CharArrayConversions.toStringBuilder(from, converter);
            assertNotNull(sb, "Resulting StringBuilder must not be null");
            assertEquals(expectedContent, sb.toString(), "StringBuilder content does not match expected value");
        }
    }

    @ParameterizedTest
    @MethodSource("provideUnsupportedWithoutConverter")
    void toStringBuilder_unsupportedSourceWithoutConverter(Object from) {
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toStringBuilder(from, null), "Unsupported source type without a converter should throw IllegalArgumentException");
    }

    @ParameterizedTest
    @MethodSource("provideEmptyCharBufferConverter")
    void toStringBuilder_customConverterReturnsEmpty(Object from, Converter converter) {
        StringBuilder sb = CharArrayConversions.toStringBuilder(from, converter);
        assertNotNull(sb);
        assertEquals("", sb.toString(), "A converter that returns an empty CharBuffer must result in an empty StringBuilder");
    }

    @ParameterizedTest
    @MethodSource("provideThrowingConverter")
    void toStringBuilder_converterThrowsRuntimeException(Object from, Converter converter) {
        assertThrows(RuntimeException.class, () -> CharArrayConversions.toStringBuilder(from, converter), "RuntimeException from the converter must be propagated");
    }

    @ParameterizedTest
    @MethodSource("provideSlicedCharBuffer")
    void toStringBuilder_charBufferSlice(CharBuffer from, Converter converter, String expected) {
        StringBuilder sb = CharArrayConversions.toStringBuilder(from, converter);
        assertEquals(expected, sb.toString(), "StringBuilder must reflect the visible slice of the CharBuffer");
    }

    @ParameterizedTest
    @MethodSource("provideStringBuilderSource")
    void toStringBuilder_stringBuilderSource(Object from, Converter converter, String expected) {
        StringBuilder sb = CharArrayConversions.toStringBuilder(from, converter);
        assertEquals(expected, sb.toString(), "StringBuilder source should be treated like a CharSequence");
    }

    @ParameterizedTest
    @MethodSource("provideByteArrayWithoutConverter")
    void toStringBuilder_byteArrayWithoutConverter(Object from) {
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toStringBuilder(from, null), "byte[] without a converter must be rejected");
    }

    @ParameterizedTest
    @MethodSource("provideCharArrayWithConverter")
    void toStringBuilder_charArrayIgnoresConverter(Object from, Converter converter, String expected) {
        StringBuilder sb = CharArrayConversions.toStringBuilder(from, converter);
        assertEquals(expected, sb.toString(), "When source is char[], the supplied converter must be ignored");
    }

    /**
     * Test data for the “large input to test capacity handling” group.
     */
    @Test
    void toStringBuilder_largeInput() {
        // Build a 10 000‑character string consisting solely of 'a'
        char[] chars = new char[10_000];
        java.util.Arrays.fill(chars, 'a');
        String large = new String(chars);
        StringBuilder sb = CharArrayConversions.toStringBuilder(large, defaultConverter);
        assertNotNull(sb);
        assertEquals(10_000, sb.length(), "StringBuilder length must match the large input size");
        assertTrue(sb.chars().allMatch(ch -> ch == 'a'), "All characters in the StringBuilder must be 'a'");
    }

    /**
     * Test data for the “read‑only CharBuffer source” group.
     */
    @Test
    void toStringBuilder_readOnlyCharBuffer() {
        CharBuffer ro = CharBuffer.wrap("readonly").asReadOnlyBuffer();
        StringBuilder sb = CharArrayConversions.toStringBuilder(ro, null);
        assertEquals("readonly", sb.toString(), "Read‑only CharBuffer must be processed correctly");
    }

    /**
     * Test data for the “ByteBuffer source without converter” group.
     */
    @Test
    void toStringBuilder_byteBufferWithoutConverter() {
        ByteBuffer bb = ByteBuffer.wrap(new byte[] { 'a', 'b', 'c' });
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toStringBuilder(bb, null), "ByteBuffer without a converter must be rejected");
    }

    /**
     * Test data for the “converter returns null CharBuffer” group.
     */
    @Test
    void toStringBuilder_converterReturnsNullCharBuffer() {
        Converter nullReturning = nullReturningConverter();
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toStringBuilder("any string", nullReturning), "A null CharBuffer from the converter must cause a NullPointerException");
    }
}
