package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toCharBuffer_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they provide all built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per test.
        converter = null;
    }

    static Stream<Arguments> customObjectWithConverterProvider() {
        // case 1 – Date converted to "epoch"
        Converter epochConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return "epoch";
            }
        };
        // case 2 – Converter returns null, fallback to object's toString ("custom")
        Converter nullReturningConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        };
        Object customObj = new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        };
        return Stream.of(Arguments.of("Custom Date with converter returning \"epoch\"", new Date(0L), epochConverter, "epoch"), Arguments.of("Custom object with converter returning null (fallback to toString)", customObj, nullReturningConverter, // as per spec: treat null as literal "null"
        "null"));
    }

    static Stream<Arguments> nullInputsProvider() {
        Converter zeroConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return "zero";
            }
        };
        return Stream.of(Arguments.of("Both arguments null → literal \"null\"", null, null, "null"), Arguments.of("Source null, non‑null converter → converter result \"zero\"", null, zeroConverter, "zero"));
    }

    static Stream<Arguments> converterIsNullProvider() {
        return Stream.of(Arguments.of("No converter, simple String source", "hello", "hello"), Arguments.of("No converter, byte[] source interpreted as UTF‑8", new byte[] { 65, 66, 67 }, "ABC"), Arguments.of("No converter, char[] source", new char[] { 'x', 'y', 'z' }, "xyz"));
    }

    static Stream<Arguments> alreadyCharSequenceProvider() {
        return Stream.of(Arguments.of("Source is already a CharBuffer", CharBuffer.wrap("buffer"), "buffer"), Arguments.of("Source is a StringBuilder", new StringBuilder("sb"), "sb"), Arguments.of("Source is a StringBuffer", new StringBuffer("sbuffer"), "sbuffer"));
    }

    @Test
    void toCharBuffer_fromString_returnsMatchingCharBuffer() {
        // GIVEN
        String source = "Hello, world!";
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(source, result.toString(), "CharBuffer content must match the original String");
    }

    @Test
    void toCharBuffer_fromByteArray_returnsMatchingCharBuffer() {
        // GIVEN
        // "Java"
        byte[] source = new byte[] { 'J', 'a', 'v', 'a' };
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals("Java", result.toString());
    }

    @Test
    void toCharBuffer_fromByteBuffer_returnsMatchingCharBuffer() {
        // GIVEN
        // "Buffer"
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 'B', 'u', 'f', 'f', 'e', 'r' });
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals("Buffer", result.toString());
    }

    @Test
    void toCharBuffer_fromCharArray_returnsMatchingCharBuffer() {
        // GIVEN
        // "chars"
        char[] source = new char[] { 'c', 'h', 'a', 'r', 's' };
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals("chars", result.toString());
    }

    /**
     * 1. Custom object with Converter **
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("customObjectWithConverterProvider")
    void toCharBuffer_customObjectWithConverter(String description, Object from, Converter customConverter, String expectedContent) {
        CharBuffer result = ByteArrayConversions.toCharBuffer(from, customConverter);
        assertNotNull(result);
        assertEquals(expectedContent, result.toString(), description);
    }

    /**
     * 2. Null inputs **
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullInputsProvider")
    void toCharBuffer_nullInputs(String description, Object from, Converter customConverter, String expectedContent) {
        CharBuffer result = ByteArrayConversions.toCharBuffer(from, customConverter);
        assertNotNull(result);
        assertEquals(expectedContent, result.toString(), description);
    }

    /**
     * 3. Converter is null **
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("converterIsNullProvider")
    void toCharBuffer_converterIsNull(String description, Object from, String expectedContent) {
        CharBuffer result = ByteArrayConversions.toCharBuffer(from, null);
        assertNotNull(result);
        assertEquals(expectedContent, result.toString(), description);
    }

    /**
     * 4. Byte array with custom Converter **
     */
    @Test
    void toCharBuffer_byteArrayWithHexConverter() {
        // GIVEN
        byte[] source = new byte[] { 0x01, 0x0A, (byte) 0xFF };
        Converter hexConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                byte[] b = (byte[]) src;
                StringBuilder sb = new StringBuilder();
                for (byte v : b) {
                    sb.append(String.format("%02X", v));
                }
                return sb.toString();
            }
        };
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(source, hexConverter);
        // THEN
        assertNotNull(result);
        assertEquals("010AFF", result.toString());
    }

    /**
     * 5. Existing CharBuffer, StringBuilder and StringBuffer inputs **
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("alreadyCharSequenceProvider")
    void toCharBuffer_existingCharSequence(String description, Object from, String expectedContent) {
        CharBuffer result = ByteArrayConversions.toCharBuffer(from, null);
        assertNotNull(result);
        assertEquals(expectedContent, result.toString(), description);
    }

    /**
     * 6. Unsupported type fallback **
     */
    @Test
    void toCharBuffer_unsupportedTypeFallsBackToToString() {
        Integer source = Integer.valueOf(12345);
        CharBuffer result = ByteArrayConversions.toCharBuffer(source, null);
        assertNotNull(result);
        assertEquals("12345", result.toString());
    }

    /**
     * 7. Converter returns non‑String object **
     */
    @Test
    void toCharBuffer_converterReturnsNonStringObject() {
        Converter intReturningConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // Integer, not a String
                return 999;
            }
        };
        CharBuffer result = ByteArrayConversions.toCharBuffer("test", intReturningConverter);
        assertNotNull(result);
        assertEquals("999", result.toString());
    }

    /**
     * 8. Converter throws exception – should propagate **
     */
    @Test
    void toCharBuffer_converterThrowsException() {
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalArgumentException("converter failure");
            }
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toCharBuffer("trigger", explodingConverter));
        assertEquals("converter failure", ex.getMessage());
    }

    /**
     * 9. Source object's toString throws exception – should propagate **
     */
    @Test
    void toCharBuffer_sourceToStringThrowsException() {
        Object badToString = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("bad toString");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ByteArrayConversions.toCharBuffer(badToString, null));
        assertEquals("bad toString", ex.getMessage());
    }

    /**
     * 10. Converter returns object with faulty toString – exception on CharBuffer creation **
     */
    @Test
    void toCharBuffer_converterResultToStringThrowsException() {
        Converter badResultConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return new Object() {

                    @Override
                    public String toString() {
                        throw new IllegalStateException("bad result toString");
                    }
                };
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ByteArrayConversions.toCharBuffer("any", badResultConverter));
        assertEquals("bad result toString", ex.getMessage());
    }
}
