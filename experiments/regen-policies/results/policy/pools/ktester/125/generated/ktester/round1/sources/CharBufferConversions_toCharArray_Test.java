package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.ArrayUtilities;
import java.nio.CharBuffer;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toCharArray_Test {

    /**
     * Shared {@link Converter} used by all tests.
     */
    private static Converter converter;

    /**
     * Shared real {@link Converter} used for the majority of tests.
     */
    private static Converter realConverter;

    /* --------------------------------------------------------------
     *  Helper class used for parameterised test data.
     * -------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final char[] expected;

        ArgumentsHolder(Object from, Converter converter, char[] expected) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "ArgumentsHolder{from=" + from + ", converter=" + converter + '}';
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    /**
     * Helper method used by the parameterised tests.
     */
    private static Stream<ArgumentsHolder> emptyBufferProvider() {
        return Stream.of(// empty String – the internal toCharBuffer will produce an empty CharBuffer
        new ArgumentsHolder("", null, EMPTY_CHAR_ARRAY), // empty char[] – same expectation
        new ArgumentsHolder(new char[0], null, EMPTY_CHAR_ARRAY));
    }

    @Test
    void toCharArray_givenCharBuffer_returnsCorrectCharArray() {
        CharBuffer buffer = CharBuffer.wrap("hello");
        char[] actual = CharBufferConversions.toCharArray(buffer, realConverter);
        assertArrayEquals(new char[] { 'h', 'e', 'l', 'l', 'o' }, actual);
    }

    @Test
    void toCharArray_givenEmptyCharBuffer_returnsEmptyArrayConstant() {
        CharBuffer empty = CharBuffer.wrap("");
        char[] actual = CharBufferConversions.toCharArray(empty, converter);
        // The implementation returns the shared EMPTY_CHAR_ARRAY instance when there are no remaining chars.
        assertSame(EMPTY_CHAR_ARRAY, actual);
        assertArrayEquals(EMPTY_CHAR_ARRAY, actual);
    }

    @Test
    void toCharArray_givenString_returnsCharArray() {
        String source = "xyz";
        char[] actual = CharBufferConversions.toCharArray(source, realConverter);
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, actual);
    }

    @Test
    void toCharArray_givenNull_returnsEmptyArrayConstant() {
        // The Converter returns null for a null source when converting to CharBuffer,
        // which leads to an empty CharBuffer and thus EMPTY_CHAR_ARRAY.
        char[] actual = CharBufferConversions.toCharArray(null, converter);
        assertSame(EMPTY_CHAR_ARRAY, actual);
    }

    @ParameterizedTest(name = "[{index}] empty source ''{0}'' -> EMPTY_CHAR_ARRAY")
    @MethodSource("emptyBufferProvider")
    void toCharArray_emptySources_returnEmptyConstant(ArgumentsHolder holder) {
        char[] result = CharBufferConversions.toCharArray(holder.from, holder.converter);
        assertSame(EMPTY_CHAR_ARRAY, result, "Expected the shared EMPTY_CHAR_ARRAY instance");
        assertArrayEquals(EMPTY_CHAR_ARRAY, result);
    }

    /**
     * Non‑empty String
     */
    @Test
    void toCharArray_nonEmptyString() {
        String src = "abc";
        char[] result = CharBufferConversions.toCharArray(src, realConverter);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    /**
     * Non‑empty primitive char[]
     */
    @Test
    void toCharArray_nonEmptyCharArray() {
        char[] src = new char[] { 'x', 'y', 'z' };
        char[] result = CharBufferConversions.toCharArray(src, realConverter);
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
    }

    /**
     * CharBuffer with its position advanced – only the remaining chars are returned.
     */
    @Test
    void toCharArray_charBufferWithOffset() {
        CharBuffer buffer = CharBuffer.wrap("hello");
        // skip 'h' and 'e'
        buffer.position(2);
        char[] result = CharBufferConversions.toCharArray(buffer, realConverter);
        assertArrayEquals(new char[] { 'l', 'l', 'o' }, result);
    }

    /**
     * byte[] containing ASCII codes – uses the real Converter (UTF‑8)
     */
    @Test
    void toCharArray_byteArrayAscii() {
        // 'A', 'B', 'C'
        byte[] src = new byte[] { 65, 66, 67 };
        char[] result = CharBufferConversions.toCharArray(src, realConverter);
        assertArrayEquals(new char[] { 'A', 'B', 'C' }, result);
    }

    /**
     * ByteBuffer with offset – only remaining bytes are converted.
     */
    @Test
    void toCharArray_byteBufferWithOffset() {
        // 'D','E','F'
        ByteBuffer bb = ByteBuffer.wrap(new byte[] { 68, 69, 70 });
        // start at 'E'
        bb.position(1);
        char[] result = CharBufferConversions.toCharArray(bb, realConverter);
        assertArrayEquals(new char[] { 'E', 'F' }, result);
    }

    /**
     * Null source object – should yield the empty constant.
     */
    @Test
    void toCharArray_nullSourceObject() {
        char[] result = CharBufferConversions.toCharArray(null, null);
        assertSame(EMPTY_CHAR_ARRAY, result);
    }

    /**
     * Integer conversion via the default Converter (which turns it into a String).
     */
    @Test
    void toCharArray_integerViaDefaultConverter() {
        Integer src = 123;
        char[] result = CharBufferConversions.toCharArray(src, realConverter);
        assertArrayEquals(new char[] { '1', '2', '3' }, result);
    }

    /**
     * Large input – 1000 'a' characters.
     */
    @Test
    void toCharArray_largeInput_boundary() {
        StringBuilder sb = new StringBuilder(1000);
        for (int i = 0; i < 1000; i++) sb.append('a');
        String src = sb.toString();
        char[] result = CharBufferConversions.toCharArray(src, realConverter);
        assertEquals(1000, result.length);
        for (char c : result) {
            assertEquals('a', c);
        }
    }

    /**
     * Unsupported source type – expect IllegalArgumentException.
     */
    @Test
    void toCharArray_unsupportedSourceType_throws() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toCharArray(unsupported, realConverter));
    }

    /**
     * Null converter for a byte[] source – should throw IllegalArgumentException.
     */
    @Test
    void toCharArray_nullConverterForByteArray_throws() {
        byte[] src = new byte[] { 65, 66 };
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toCharArray(src, null));
    }

    /**
     * Converter that throws IllegalStateException during conversion – exception must propagate.
     */
    @Test
    void toCharArray_converterThrowsException_propagates() {
        // Create a mock Converter that throws IllegalStateException for any conversion request.
        Converter throwingConverter = Mockito.mock(Converter.class);
        // The internal implementation of CharBufferConversions calls Converter.convert(Object, Class).
        // We stub any call to that method to throw the desired exception.
        doThrow(new IllegalStateException("forced")).when(throwingConverter).convert(any(), any());
        assertThrows(IllegalStateException.class, () -> CharBufferConversions.toCharArray("trigger", throwingConverter));
    }
}
