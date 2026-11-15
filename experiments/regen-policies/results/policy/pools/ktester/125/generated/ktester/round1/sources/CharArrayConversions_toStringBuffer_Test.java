package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CharArrayConversions_toStringBuffer_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared default Converter used for identity‑conversion cases.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up; null out the reference for good measure.
        converter = null;
    }

    /**
     * Provides test data for the majority of conversion scenarios.
     *
     * Each argument consists of:
     *   - a description (used only for readability)
     *   - the source object to pass as the first parameter
     *   - a string identifier for the converter to use ("identity", "uppercase", "throwing", or null)
     *   - the expected result (String) or the expected exception class (if an exception is expected)
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> conversionProvider() {
        return Stream.of(// Empty String
        arguments("Empty String", "", "identity", ""), // Non‑empty String
        arguments("Non‑empty String", "hello world", "identity", "hello world"), // Char array
        arguments("Char Array", new char[] { 'J', 'a', 'v', 'a' }, "identity", "Java"), // CharBuffer
        arguments("CharBuffer Input", CharBuffer.wrap("charbuf"), "identity", "charbuf"), // StringBuilder
        arguments("StringBuilder Input", new StringBuilder("builder"), "identity", "builder"), // StringBuffer
        arguments("StringBuffer Input", new java.lang.StringBuffer("buffer"), "identity", "buffer"), // Byte array (UTF‑8)
        arguments("Byte Array UTF‑8", "byte".getBytes(), "identity", "byte"), // ByteBuffer
        arguments("ByteBuffer Input", ByteBuffer.wrap("buff".getBytes()), "identity", "buff"), // Custom Converter Uppercase
        arguments("Custom Converter Uppercase", "lowercase", "uppercase", "LOWERCASE"), // Unsupported source type (Integer)
        arguments("Unsupported Source Type", 123, "identity", IllegalArgumentException.class), // Converter throws RuntimeException
        arguments("Converter Throws Exception", "sample", "throwing", RuntimeException.class), // Invalid UTF‑8 byte sequence (malformed)
        arguments("Invalid Byte Sequence (Malformed UTF‑8)", new byte[] { (byte) 0xC3, (byte) 0x28 }, "identity", IllegalArgumentException.class));
    }

    /**
     * Returns a Converter based on the identifier used in the test data.
     *
     * @param id one of "identity", "uppercase", "throwing", or null
     * @return the corresponding Converter (or null if id is null)
     */
    private Converter resolveConverter(String id) {
        if (id == null) {
            return null;
        }
        switch(id) {
            case "identity":
                return defaultConverter;
            case "uppercase":
                return new Converter(new ConverterOptions()) {

                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> T convert(Object source, Class<T> target) {
                        // Only intercept conversion to CharBuffer for Strings
                        if (source instanceof String && target == CharBuffer.class) {
                            String upper = ((String) source).toUpperCase();
                            return (T) CharBuffer.wrap(upper);
                        }
                        return super.convert(source, target);
                    }
                };
            case "throwing":
                return new Converter(new ConverterOptions()) {

                    @Override
                    public <T> T convert(Object source, Class<T> target) {
                        throw new RuntimeException("Intentional conversion failure");
                    }
                };
            default:
                // Fallback – should never happen in our test data
                return defaultConverter;
        }
    }

    @Test
    void toStringBuffer_fromString_returnsMatchingStringBuffer() {
        // given
        String source = "hello world";
        // when
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        // then
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals(source, result.toString(), "StringBuffer content should match the source string");
    }

    @Test
    void toStringBuffer_fromCharArray_returnsMatchingStringBuffer() {
        // given
        char[] source = new char[] { 'a', 'b', 'c', 'd' };
        String expected = new String(source);
        // when
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals(expected, result.toString());
    }

    @Test
    void toStringBuffer_fromCharBuffer_returnsMatchingStringBuffer() {
        // given
        CharBuffer source = CharBuffer.wrap("CharBufferContent");
        // when
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals(source.toString(), result.toString());
    }

    @Test
    void toStringBuffer_fromByteArray_isConvertedViaCharBuffer() {
        // given
        // default charset (UTF‑8 on most platforms)
        byte[] source = "bytes".getBytes();
        // conversion should yield the same characters
        String expected = new String(source);
        // when
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        // then
        assertNotNull(result);
        assertEquals(expected, result.toString(), "Byte array should be converted to a CharBuffer and then to a matching StringBuffer");
    }

    @Test
    void toStringBuffer_bothArgumentsNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toStringBuffer(null, null), "Both arguments null should result in NPE");
    }

    @Test
    void toStringBuffer_sourceNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toStringBuffer(null, defaultConverter), "Null source should result in NPE");
    }

    @Test
    void toStringBuffer_converterNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toStringBuffer("sample", null), "Null converter should result in NPE");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conversionProvider")
    void toStringBuffer_variousScenarios(String description, Object source, String converterId, Object expected) {
        Converter conv = resolveConverter(converterId);
        if (expected instanceof Class<?>) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> CharArrayConversions.toStringBuffer(source, conv), () -> description + " should throw " + exClass.getSimpleName());
        } else {
            // Expected a successful conversion
            String expectedString = (String) expected;
            StringBuffer result = CharArrayConversions.toStringBuffer(source, conv);
            assertNotNull(result, description + ": result must not be null");
            assertEquals(expectedString, result.toString(), description + ": unexpected StringBuffer content");
        }
    }
}
