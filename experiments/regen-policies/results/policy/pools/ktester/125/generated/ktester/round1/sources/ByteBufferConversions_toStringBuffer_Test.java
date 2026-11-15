package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toStringBuffer_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Simple {@link CharSequence} implementation backed by a String.
     */
    private static final class MyCharSequence implements CharSequence {

        private final String data;

        MyCharSequence(String data) {
            this.data = data;
        }

        @Override
        public int length() {
            return data.length();
        }

        @Override
        public char charAt(int index) {
            return data.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return data.subSequence(start, end);
        }

        @Override
        public String toString() {
            return data;
        }
    }

    /**
     * CharSequence that deliberately throws from {@code charAt}.
     */
    private static final class MyFaultyCharSequence implements CharSequence {

        @Override
        public int length() {
            return 1;
        }

        @Override
        public char charAt(int index) {
            throw new RuntimeException("faulty charAt");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new RuntimeException("faulty subSequence");
        }

        @Override
        public String toString() {
            return "faulty";
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration required for the conversions we test.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up; just clear the reference.
        converter = null;
    }

    /**
     * Helper that invokes the focal method.
     */
    private static StringBuffer invoke(Object from) {
        return ByteBufferConversions.toStringBuffer(from, converter);
    }

    private static Stream<Arguments> provideSuccessfulCases() {
        // Custom converters used in a few test cases
        Converter upperCaseConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return ((String) src).toUpperCase();
            }
        };
        Converter nullReturningConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        };
        return Stream.of(// 1. Plain String, no custom converter
        Arguments.of("simple string", null, "simple string"), // 2. char[]
        Arguments.of(new char[] { 'a', 'b', 'c' }, null, "abc"), // 3. ByteBuffer with ASCII bytes
        Arguments.of(ByteBuffer.wrap(new byte[] { 65, 66, 67 }), null, "ABC"), // 4. null source – toCharBuffer should produce the literal "null"
        Arguments.of(null, null, "null"), // 5. Empty String
        Arguments.of("", null, ""), // 6. Emoji surrogate pair (already a String)
        Arguments.of("\uD83D\uDE00", null, "\uD83D\uDE00"), // 7. Object with overridden toString()
        Arguments.of(new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        }, null, "custom"), // 8. Upper‑case conversion via a custom Converter
        Arguments.of("test", upperCaseConverter, "TEST"), // 9. StringBuilder
        Arguments.of(new StringBuilder("builder"), null, "builder"), // 10. StringBuffer
        Arguments.of(new StringBuffer("buf"), null, "buf"), // 11. CharBuffer
        Arguments.of(CharBuffer.wrap(new char[] { 'x', 'y', 'z' }), null, "xyz"), // 12. raw byte[] (ASCII)
        Arguments.of(new byte[] { 72, 105 }, null, "Hi"), // 13. UTF‑8 encoded emoji in a ByteBuffer
        Arguments.of(ByteBuffer.wrap(new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }), null, "\uD83D\uDE00"), // 14. UTF‑8 encoded 'é'
        Arguments.of(ByteBuffer.wrap(new byte[] { (byte) 0xC3, (byte) 0xA9 }), null, "é"), // 15. ByteBuffer with position set to 1 (skip first byte)
        Arguments.of(ByteBuffer.wrap(new byte[] { 65, 66, 67 }).position(1), null, "BC"), // 16. Custom CharSequence implementation
        Arguments.of(new MyCharSequence("myseq"), null, "myseq"), // 17. Integer – fallback to toString()
        Arguments.of(Integer.valueOf(123), null, "123"), // 18. Converter that returns null – should become literal "null"
        Arguments.of("test", nullReturningConverter, "null"));
    }

    private static Stream<Arguments> provideExceptionCases() {
        // Converter that returns an unsupported type (Integer)
        Converter integerReturningConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return Integer.valueOf(42);
            }
        };
        // Converter that throws a RuntimeException
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("converter failure");
            }
        };
        return Stream.of(// 1. int[] – not a supported source type
        Arguments.of(new int[] { 1, 2, 3 }, null, IllegalArgumentException.class, null), // 2. java.util.Date – unsupported
        Arguments.of(new Date(), null, IllegalArgumentException.class, null), // 3. Converter returns Integer – cannot be interpreted as characters
        Arguments.of("ignored", integerReturningConverter, IllegalArgumentException.class, null), // 4. Faulty CharSequence that throws from charAt()
        Arguments.of(new MyFaultyCharSequence(), null, RuntimeException.class, "faulty charAt"), // 5. Converter throws RuntimeException – should propagate
        Arguments.of("boom", throwingConverter, RuntimeException.class, "converter failure"));
    }

    @Test
    void toStringBuffer_fromString() {
        String source = "Hello, world!";
        StringBuffer result = invoke(source);
        assertNotNull(result);
        assertEquals(source, result.toString());
    }

    @Test
    void toStringBuffer_fromCharArray() {
        char[] source = new char[] { 'a', 'b', 'c' };
        StringBuffer result = invoke(source);
        assertNotNull(result);
        assertEquals(new String(source), result.toString());
    }

    @Test
    void toStringBuffer_fromCharBuffer() {
        CharBuffer source = CharBuffer.wrap("CharBufferTest");
        StringBuffer result = invoke(source);
        assertNotNull(result);
        assertEquals(source.toString(), result.toString());
    }

    @Test
    void toStringBuffer_fromByteArray() {
        // Byte array representing ASCII characters for "bytes"
        byte[] source = new byte[] { 'b', 'y', 't', 'e', 's' };
        StringBuffer result = invoke(source);
        assertNotNull(result);
        assertEquals("bytes", result.toString());
    }

    @Test
    void toStringBuffer_fromByteBuffer() {
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 'J', 'a', 'v', 'a' });
        StringBuffer result = invoke(source);
        assertNotNull(result);
        assertEquals("Java", result.toString());
    }

    @Test
    void toStringBuffer_fromEmptyByteArray() {
        byte[] source = new byte[0];
        StringBuffer result = invoke(source);
        assertNotNull(result);
        assertEquals("", result.toString());
    }

    @Test
    void toStringBuffer_fromNullReturnsNullBuffer() {
        // According to Converter contract, converting null to CharBuffer yields null.
        // ByteBufferConversions should then create a StringBuffer with a null CharBuffer,
        // which results in an empty StringBuffer.
        StringBuffer result = invoke(null);
        assertNotNull(result);
        assertEquals("", result.toString());
    }

    @Test
    void toStringBuffer_fromListOfCharacters() {
        // Converter can turn a List<Character> into a CharBuffer.
        java.util.List<Character> source = java.util.List.of('L', 'i', 's', 't');
        StringBuffer result = invoke(source);
        assertNotNull(result);
        assertEquals("List", result.toString());
    }

    @Test
    void toStringBuffer_fromMapWithValueKey() {
        // Converter treats a map with a "value" entry as the source for conversion.
        java.util.Map<String, Object> source = Collections.singletonMap("value", "MapValue");
        StringBuffer result = invoke(source);
        assertNotNull(result);
        assertEquals("MapValue", result.toString());
    }

    /**
     * Successful conversion scenarios – the method should return a {@link StringBuffer}
     * whose {@code toString()} matches the expected value.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideSuccessfulCases")
    void toStringBuffer_success(Object from, Converter customConverter, String expected) {
        StringBuffer result = ByteBufferConversions.toStringBuffer(from, customConverter);
        assertNotNull(result, "Resulting StringBuffer must not be null");
        assertEquals(expected, result.toString(), () -> "Unexpected conversion result for source: " + from);
    }

    /**
     * Scenarios that are expected to throw an exception.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideExceptionCases")
    void toStringBuffer_exceptions(Object from, Converter customConverter, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        Throwable thrown = assertThrows(expectedException, () -> ByteBufferConversions.toStringBuffer(from, customConverter), () -> "Expected exception not thrown for source: " + from);
        if (expectedMessageFragment != null) {
            assertTrue(thrown.getMessage().contains(expectedMessageFragment), () -> "Exception message does not contain expected fragment. Message: " + thrown.getMessage());
        }
    }
}
