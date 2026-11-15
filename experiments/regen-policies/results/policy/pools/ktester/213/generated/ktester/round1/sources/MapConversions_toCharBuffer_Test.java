package com.cedarsoftware.util.convert;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.stream.Stream;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class MapConversions_toCharBuffer_Test {

    private static Converter converter;

    /**
     * Simple holder class for parameterised test arguments.
     */
    private static class ArgumentsHolder {

        final String description;

        final Object source;

        // null means we expect a null CharBuffer
        final String expected;

        ArgumentsHolder(String description, Object source, String expected) {
            this.description = description;
            this.source = source;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Custom object whose {@code toString()} returns a known value.
     */
    private static class CustomObject {

        @Override
        public String toString() {
            return "custom";
        }
    }

    /**
     * Helper class whose {@code toString()} throws a {@link RuntimeException}.
     */
    private static class ThrowingToString {

        @Override
        public String toString() {
            throw new RuntimeException("boom from ThrowingToString");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter class now requires a ConverterOptions instance.
        // Using a default‑constructed ConverterOptions provides the standard configuration.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Provides a stream of arguments for successful conversions.
     * Each element is an {@link ArgumentsHolder} containing:
     * <ul>
     *   <li>description – used only for readability in the test name.</li>
     *   <li>source object – the value passed to {@code toCharBuffer}.</li>
     *   <li>expected string – the expected CharBuffer content (null means we expect a null result).</li>
     * </ul>
     */
    private static Stream<ArgumentsHolder> validConversionProvider() {
        return Stream.of(new ArgumentsHolder("Plain String", "hello", "hello"), new ArgumentsHolder("CharBuffer instance", CharBuffer.wrap("already"), "already"), new ArgumentsHolder("Map with primary key", mapOf("value", "mapValue"), "mapValue"), new ArgumentsHolder("Map with alternate key", mapOf("_v", "altKey"), "altKey"), new ArgumentsHolder("Byte array (UTF‑8)", new byte[] { 'b', 'y', 't', 'e', 's' }, "bytes"), new ArgumentsHolder("Numeric value (Integer)", 12345, "12345"), new ArgumentsHolder("Empty string", "", ""), new ArgumentsHolder("StringBuilder", new StringBuilder("builder"), "builder"), new ArgumentsHolder("StringBuffer", new StringBuffer("buffer"), "buffer"), new ArgumentsHolder("char[]", new char[] { 'a', 'b', 'c' }, "abc"), new ArgumentsHolder("ByteBuffer (UTF‑8)", ByteBuffer.wrap(new byte[] { 'x', 'y', 'z' }), "xyz"), new ArgumentsHolder("Map with both keys – primary wins", mapOf("value", "first", "_v", "second"), "first"), new ArgumentsHolder("Map with null value", mapOf("value", null), null), new ArgumentsHolder("Map without recognised keys", mapOf("other", "data"), null), new ArgumentsHolder("Custom object toString()", new CustomObject(), "custom"), new ArgumentsHolder("Empty byte array", new byte[0], ""), new ArgumentsHolder("Read‑only CharBuffer", CharBuffer.wrap("readOnly").asReadOnlyBuffer(), "readOnly"));
    }

    /**
     * Utility to create a mutable map with a variable number of entries.
     */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    @Test
    @DisplayName("Convert map with 'value' key to CharBuffer")
    void testMapWithValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "hello world");
        CharBuffer result = MapConversions.toCharBuffer(map, converter);
        assertNotNull("Resulting CharBuffer should not be null", result);
        assertEquals("hello world", result.toString());
    }

    @Test
    @DisplayName("Convert map with '_v' key to CharBuffer")
    void testMapWithUnderscoreVKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "test123");
        CharBuffer result = MapConversions.toCharBuffer(map, converter);
        assertNotNull("Resulting CharBuffer should not be null", result);
        assertEquals("test123", result.toString());
    }

    @Test
    @DisplayName("Pass null source – should return null")
    void testNullSource() {
        CharBuffer result = MapConversions.toCharBuffer(null, converter);
        assertNull("Converting a null source should return null", result);
    }

    @Test
    @DisplayName("Pass an existing CharBuffer – should be returned unchanged")
    void testCharBufferPassthrough() {
        CharBuffer source = CharBuffer.wrap("already a CharBuffer");
        CharBuffer result = MapConversions.toCharBuffer(source, converter);
        assertNotNull("Result should not be null", result);
        assertEquals(source.toString(), result.toString());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    @DisplayName("Valid conversion scenarios")
    void testValidConversions(ArgumentsHolder holder) {
        validConversionProvider().forEach(holder -> {
            CharBuffer result = MapConversions.toCharBuffer(holder.source, converter);
            if (holder.expected == null) {
                assertNull(holder.description + " – expected null result", result);
            } else {
                assertNotNull(holder.description + " – result should not be null", result);
                assertEquals(holder.description + " – CharBuffer content mismatch", holder.expected, result.toString());
            }
        });
    }

    @Test
    @DisplayName("Null converter argument should throw NullPointerException")
    void testNullConverter() {
        try {
            MapConversions.toCharBuffer("anyString", null);
            fail("Passing a null Converter must raise NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    @DisplayName("Unsupported source type (int[]) should raise IllegalArgumentException")
    void testUnsupportedIntArray() {
        int[] unsupported = new int[] { 1, 2, 3 };
        try {
            MapConversions.toCharBuffer(unsupported, converter);
            fail("Unsupported int[] source should cause IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    @DisplayName("Map value whose toString() throws RuntimeException propagates")
    void testMapValueThrowingToString() {
        Map<String, Object> map = mapOf("value", new ThrowingToString());
        try {
            MapConversions.toCharBuffer(map, converter);
            fail("Exception from value.toString() must propagate");
        } catch (RuntimeException ex) {
            assertEquals("boom from ThrowingToString", ex.getMessage());
        }
    }

    @Test
    @DisplayName("Byte array with malformed UTF‑8 should raise CharacterCodingException")
    void testMalformedUtf8ByteArray() {
        // invalid 2‑byte sequence
        byte[] malformed = new byte[] { (byte) 0xC3, (byte) 0x28 };
        try {
            MapConversions.toCharBuffer(malformed, converter);
            fail("Malformed UTF‑8 byte array must cause CharacterCodingException");
        } catch (CharacterCodingException expected) {
            // expected
        }
    }

    @Test
    @DisplayName("ByteBuffer with malformed UTF‑8 should raise CharacterCodingException")
    void testMalformedUtf8ByteBuffer() {
        // invalid 3‑byte sequence
        byte[] malformed = new byte[] { (byte) 0xE2, (byte) 0x28, (byte) 0xA1 };
        ByteBuffer buf = ByteBuffer.wrap(malformed);
        try {
            MapConversions.toCharBuffer(buf, converter);
            fail("Malformed UTF‑8 ByteBuffer must cause CharacterCodingException");
        } catch (CharacterCodingException expected) {
            // expected
        }
    }

    @Test
    @DisplayName("Map value of unsupported type (int[]) should raise IllegalArgumentException")
    void testMapValueUnsupportedType() {
        Map<String, Object> map = mapOf("value", new int[] { 4, 5, 6 });
        try {
            MapConversions.toCharBuffer(map, converter);
            fail("Map value of unsupported type must cause IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    @DisplayName("Object whose toString() throws IllegalStateException propagates")
    void testObjectThrowingIllegalStateException() {
        Object bad = new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        };
        try {
            MapConversions.toCharBuffer(bad, converter);
            fail("Exception from object's toString() must propagate");
        } catch (IllegalStateException ex) {
            assertEquals("boom", ex.getMessage());
        }
    }
}
