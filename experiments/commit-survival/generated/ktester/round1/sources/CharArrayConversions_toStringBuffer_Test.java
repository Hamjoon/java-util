package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toStringBuffer_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * The default Converter used for most tests – a no‑op that simply forwards the input to CharBuffer.
     */
    private static Converter defaultConverter;

    /**
     * A no‑op converter that treats CharSequence objects as already convertible.
     */
    static class NoOpConverter implements Converter {

        @Override
        public CharBuffer toCharBuffer(Object from) {
            if (from == null)
                return CharBuffer.wrap("null");
            if (from instanceof CharSequence) {
                return CharBuffer.wrap((CharSequence) from);
            }
            if (from instanceof char[]) {
                return CharBuffer.wrap((char[]) from);
            }
            if (from instanceof CharBuffer) {
                return (CharBuffer) from;
            }
            // fallback – use toString()
            return CharBuffer.wrap(from.toString());
        }
        // other Converter methods are not used in these tests
    }

    /**
     * Converts a byte[] or ByteBuffer to a CharBuffer using UTF‑8.
     */
    static class ByteToCharConverter implements Converter {

        @Override
        public CharBuffer toCharBuffer(Object from) {
            byte[] bytes;
            if (from instanceof byte[]) {
                bytes = (byte[]) from;
            } else if (from instanceof ByteBuffer) {
                ByteBuffer bb = (ByteBuffer) from;
                bytes = new byte[bb.remaining()];
                bb.get(bytes);
            } else {
                throw new IllegalArgumentException("Unsupported type for ByteToCharConverter");
            }
            return CharBuffer.wrap(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     * Returns {@code null} – used to test null‑return handling.
     */
    static class NullReturningConverter implements Converter {

        @Override
        public CharBuffer toCharBuffer(Object from) {
            return null;
        }
    }

    /**
     * Throws an unchecked exception supplied at construction time.
     */
    static class ThrowingConverter implements Converter {

        private final RuntimeException toThrow;

        ThrowingConverter(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public CharBuffer toCharBuffer(Object from) {
            throw toThrow;
        }
    }

    /**
     * Converts any object to its {@code toString()} representation.
     */
    static class ObjectToStringConverter implements Converter {

        @Override
        public CharBuffer toCharBuffer(Object from) {
            return CharBuffer.wrap(String.valueOf(from));
        }
    }

    /**
     * Formats a {@link Date} as a string.
     */
    static class DateToCharConverter implements Converter {

        @Override
        public CharBuffer toCharBuffer(Object from) {
            if (!(from instanceof Date)) {
                throw new IllegalArgumentException("Expected Date");
            }
            return CharBuffer.wrap(from.toString());
        }
    }

    /**
     * A converter that always throws {@link ClassCastException}.
     */
    static class ClassCastThrowingConverter implements Converter {

        @Override
        public CharBuffer toCharBuffer(Object from) {
            throw new ClassCastException("Intentional class‑cast failure");
        }
    }

    /* -----------------------------------------------------------------------
     *  Helper holder class for parameterized tests
     * --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final StringBuffer expected;

        final String description;

        ArgumentsHolder(Object from, Converter converter, StringBuffer expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        // Overload for cases where expected is just a string (used in some parameterized tests)
        ArgumentsHolder(Object from, Converter converter, String expectedStr, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = new StringBuffer(expectedStr);
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
    }

    // ---------- Group: Valid conversions – primitive and CharSequence sources ----------
    private static Stream<ArgumentsHolder> validPrimitiveAndCharSequence() {
        return Stream.of(new ArgumentsHolder("Hello World", new NoOpConverter(), new StringBuffer("Hello World"), "String source should be wrapped into a CharBuffer and then into a StringBuffer with identical content."), new ArgumentsHolder(new char[] { 'J', 'a', 'v', 'a' }, new NoOpConverter(), new StringBuffer("Java"), "char[] source should be converted to a CharBuffer and then to a StringBuffer preserving the characters."), new ArgumentsHolder(CharBuffer.wrap("Buffer"), new NoOpConverter(), new StringBuffer("Buffer"), "A CharBuffer instance should be used directly; the resulting StringBuffer must contain the same characters."));
    }

    // ---------- Group: Valid conversions – byte based sources ----------
    private static Stream<ArgumentsHolder> validByteBasedSources() {
        return Stream.of(new ArgumentsHolder(new byte[] { 72, 101, 108, 108, 111 }, new ByteToCharConverter(), new StringBuffer("Hello"), "byte[] source should be decoded via the supplied Converter (UTF‑8)."), new ArgumentsHolder(ByteBuffer.wrap(new byte[] { (byte) 0xE2, (byte) 0x9C, (byte) 0x93 }), new ByteToCharConverter(), new StringBuffer("✓"), "ByteBuffer source must be converted via the supplied Converter; result should be the check‑mark character."));
    }

    private static Stream<ArgumentsHolder> emptyAndWhitespaceSources() {
        return Stream.of(new ArgumentsHolder("", new NoOpConverter(), new StringBuffer(""), "Empty String → empty StringBuffer"), new ArgumentsHolder(new char[0], new NoOpConverter(), new StringBuffer(""), "Zero‑length char[] → empty StringBuffer"), new ArgumentsHolder("   \t\n", new NoOpConverter(), new StringBuffer("   \t\n"), "Whitespace String should be preserved"));
    }

    @Test
    void toStringBuffer_fromString_returnsMatchingBuffer() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals(source, result.toString(), "StringBuffer content must match the original String");
    }

    @Test
    void toStringBuffer_fromCharArray_returnsMatchingBuffer() {
        // GIVEN
        char[] source = new char[] { 'a', 'b', 'c', '1', '2', '3' };
        String expected = new String(source);
        // WHEN
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString(), "StringBuffer created from a char[] must contain the same characters");
    }

    @Test
    void toStringBuffer_fromCharBuffer_returnsMatchingBuffer() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("BufferTest");
        // WHEN
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(source.toString(), result.toString(), "StringBuffer created from a CharBuffer must match its string representation");
    }

    @Test
    void toStringBuffer_fromNull_returnsStringBufferWithNullString() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null even when source is null");
        assertEquals("null", result.toString(), "When the source object is null, the StringBuffer should contain the literal \"null\"");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validPrimitiveAndCharSequence")
    void toStringBuffer_validPrimitiveAndCharSequence(ArgumentsHolder holder) {
        StringBuffer result = CharArrayConversions.toStringBuffer(holder.from, holder.converter);
        assertEquals(holder.expected.toString(), result.toString(), "Resulting StringBuffer content does not match expected.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validByteBasedSources")
    void toStringBuffer_validByteBasedSources(ArgumentsHolder holder) {
        StringBuffer result = CharArrayConversions.toStringBuffer(holder.from, holder.converter);
        assertEquals(holder.expected.toString(), result.toString(), "Byte‑based conversion produced unexpected result.");
    }

    // ---------- Group: Null and edge cases ----------
    @Test
    void toStringBuffer_nullSource_returnsLiteralNullString() {
        StringBuffer result = CharArrayConversions.toStringBuffer(null, new NoOpConverter());
        assertNotNull(result);
        assertEquals("null", result.toString(), "Null source should be represented by the literal string \"null\".");
    }

    @Test
    void toStringBuffer_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toStringBuffer("NonNull", null), "Passing a null Converter must throw NullPointerException.");
    }

    @Test
    void toStringBuffer_unsupportedSource_delegatesToConverter() {
        Object source = new Object();
        String expected = source.toString();
        StringBuffer result = CharArrayConversions.toStringBuffer(source, new ObjectToStringConverter());
        assertEquals(expected, result.toString(), "Unsupported source should be handled by the provided Converter.");
    }

    // ---------- Group: Additional CharSequence sources ----------
    @Test
    void toStringBuffer_fromStringBuilder() {
        StringBuilder sb = new StringBuilder("Builder");
        StringBuffer result = CharArrayConversions.toStringBuffer(sb, new NoOpConverter());
        assertEquals("Builder", result.toString(), "StringBuilder should be converted correctly.");
    }

    @Test
    void toStringBuffer_fromStringBuffer() {
        java.lang.StringBuffer sbuf = new java.lang.StringBuffer("SBuf");
        StringBuffer result = CharArrayConversions.toStringBuffer(sbuf, new NoOpConverter());
        assertEquals("SBuf", result.toString(), "StringBuffer source should result in an identical StringBuffer.");
    }

    // ---------- Group: Empty and whitespace sources ----------
    @ParameterizedTest
    @MethodSource("emptyAndWhitespaceSources")
    void toStringBuffer_emptyAndWhitespace(ArgumentsHolder holder) {
        StringBuffer result = CharArrayConversions.toStringBuffer(holder.from, holder.converter);
        assertEquals(holder.expected.toString(), result.toString(), "Empty/whitespace source conversion failed.");
    }

    // ---------- Group: CharBuffer with non‑zero position/limit ----------
    @Test
    void toStringBuffer_charBufferWithOffset() {
        // positions at "World"
        CharBuffer cb = CharBuffer.wrap("HelloWorld".toCharArray(), 5, 5);
        StringBuffer result = CharArrayConversions.toStringBuffer(cb, new NoOpConverter());
        assertEquals("World", result.toString(), "CharBuffer with offset should expose only the remaining characters.");
    }

    @Test
    void toStringBuffer_charBufferWithSurrogateAndLimits() {
        CharBuffer cb = CharBuffer.wrap("😀😃😄".toCharArray()).position(// points to first surrogate of second emoji
        1).limit(// limit after that surrogate
        2);
        StringBuffer result = CharArrayConversions.toStringBuffer(cb, new NoOpConverter());
        // The slice corresponds to a single code point "😃"
        assertEquals("😃", result.toString(), "CharBuffer with custom position/limit should yield the selected emoji.");
    }

    // ---------- Group: Converter returns null CharBuffer ----------
    @Test
    void toStringBuffer_converterReturnsNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toStringBuffer("any", new NullReturningConverter()), "If the Converter returns null, StringBuffer constructor should throw NPE.");
    }

    // ---------- Group: Converter propagates exception ----------
    @Test
    void toStringBuffer_converterThrowsIllegalStateException_propagates() {
        Converter throwing = new ThrowingConverter(new IllegalStateException("boom"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CharArrayConversions.toStringBuffer("trigger", throwing));
        assertEquals("boom", ex.getMessage(), "Exception from Converter must propagate unchanged.");
    }

    @Test
    void toStringBuffer_converterThrowsRuntimeException_propagates() {
        Converter throwing = new ThrowingConverter(new RuntimeException("runtime"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharArrayConversions.toStringBuffer("trigger", throwing));
        assertEquals("runtime", ex.getMessage(), "RuntimeException from Converter must propagate unchanged.");
    }

    // ---------- Group: Unsupported source – custom converter logic ----------
    @Test
    void toStringBuffer_dateWithCustomConverter() {
        Date epoch = new Date(0L);
        // default Date.toString() format
        String expected = epoch.toString();
        StringBuffer result = CharArrayConversions.toStringBuffer(epoch, new DateToCharConverter());
        assertEquals(expected, result.toString(), "Date should be formatted by custom converter.");
    }

    // ---------- Group: Unsupported source type – NoOpConverter should reject ----------
    @Test
    void toStringBuffer_unsupportedPrimitiveArray_throwsIllegalArgumentException() {
        int[] source = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> CharArrayConversions.toStringBuffer(source, new NoOpConverter()), "Unsupported primitive array should cause IllegalArgumentException.");
    }

    // ---------- Group: Converter throws ClassCastException ----------
    @Test
    void toStringBuffer_converterThrowsClassCastException_propagates() {
        Converter bad = new ClassCastThrowingConverter();
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toStringBuffer("some string", bad), "ClassCastException from Converter must propagate.");
    }

    // ---------- Group: CharBuffer with illegal position/limit ----------
    @Test
    void toStringBuffer_illegalCharBufferPositionLimit_throwsIllegalArgumentException() {
        CharBuffer cb = CharBuffer.wrap(new char[] { 'a', 'b', 'c' });
        assertThrows(IllegalArgumentException.class, () -> {
            // illegal configuration
            cb.position(5).limit(2);
            CharArrayConversions.toStringBuffer(cb, new NoOpConverter());
        }, "Creating a CharBuffer with position > limit should result in IllegalArgumentException.");
    }
}
