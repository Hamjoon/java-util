package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.nio.*;
import java.util.stream.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toStringBuffer_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    /**
     * Converter used for the majority of test cases (default behaviour).
     */
    private static Converter defaultConverter;

    /**
     * Simple mock converter that always returns the supplied string wrapped in a CharBuffer.
     */
    private static class MockConverter extends Converter {

        private final String result;

        MockConverter(String result) {
            super(new ConverterOptions());
            this.result = result;
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            // The real implementation returns a CharBuffer when asked for CharBuffer.class.
            if (CharBuffer.class.equals(toClass)) {
                // Returning null is allowed – the caller will handle it (used in one test case).
                return result == null ? null : (T) CharBuffer.wrap(result);
            }
            // Fallback to the default conversion for any other target type.
            return super.convert(from, toClass);
        }
    }

    /**
     * Mock converter that throws a supplied RuntimeException when conversion is attempted.
     */
    private static class MockConverterThrows extends Converter {

        private final RuntimeException toThrow;

        MockConverterThrows(RuntimeException toThrow) {
            super(new ConverterOptions());
            this.toThrow = toThrow;
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw toThrow;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default options – no special configuration required for these simple conversions
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the test tidy
        converter = null;
    }

    /**
     * Normal conversions – String, char[] and CharBuffer.
     */
    private static Stream<Arguments> normalConversionsProvider() {
        return Stream.of(Arguments.of("hello", null, "hello", "Converts a plain String to a StringBuffer; the resulting buffer should contain the original string."), Arguments.of(new char[] { 'a', 'b', 'c' }, null, "abc", "Converts a char[] to a StringBuffer; the buffer should contain the characters in order."), Arguments.of(CharBuffer.wrap("world"), null, "world", "Converts an existing CharBuffer to a StringBuffer; the content must be preserved."));
    }

    /**
     * Null and empty inputs – expected exception or empty result.
     */
    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(// According to the JSON spec a null source should raise NPE.
        Arguments.of(null, null, NullPointerException.class, "Passing a null source object should result in a NullPointerException because the internal toCharBuffer cannot create a CharBuffer from null."), Arguments.of("", null, "", "An empty String should produce an empty StringBuffer."), Arguments.of(new char[0], null, "", "An empty char[] should result in an empty StringBuffer."), Arguments.of(CharBuffer.wrap(""), null, "", "An empty CharBuffer should be converted to an empty StringBuffer."));
    }

    /**
     * Non‑string source types (StringBuilder, ByteBuffer, byte[], Integer).
     */
    private static Stream<Arguments> nonStringTypesProvider() {
        return Stream.of(Arguments.of(new StringBuilder("builder"), null, "builder", "A StringBuilder should be converted to a StringBuffer containing the same characters."), Arguments.of(ByteBuffer.wrap(new byte[] { 65, 66, 67 }), null, "ABC", "A ByteBuffer with ASCII bytes should be interpreted as characters and placed into the resulting StringBuffer."), Arguments.of(new byte[] { 68, 69, 70 }, null, "DEF", "A byte[] containing ASCII values should be converted to the corresponding characters in a StringBuffer."), Arguments.of(Integer.valueOf(123), null, ClassCastException.class, "An unsupported type (Integer) without a custom Converter should cause a ClassCastException inside toCharBuffer."));
    }

    /**
     * Custom converter usage – overriding default conversion or handling nulls.
     */
    private static Stream<Arguments> customConverterProvider() {
        return Stream.of(Arguments.of("any input", new MockConverter("converted"), "converted", "When a non‑null Converter is supplied, its conversion logic overrides the default handling; the mock returns a CharBuffer wrapping the string \"converted\"."), Arguments.of(null, new MockConverter("nullConverted"), "nullConverted", "A custom Converter can define how to treat a null source; the mock returns a CharBuffer with \"nullConverted\"."));
    }

    /**
     * Large data handling – 1000‑character string.
     */
    private static Stream<Arguments> largeDataProvider() {
        String longString = "x".repeat(1000);
        return Stream.of(Arguments.of(longString, null, longString, "A very long string (1000 characters) should be fully preserved in the resulting StringBuffer, testing performance and buffer growth."));
    }

    /**
     * Custom converter edge cases – null return and runtime exception.
     */
    private static Stream<Arguments> customConverterEdgeProvider() {
        return Stream.of(Arguments.of("any input", new MockConverter(null), NullPointerException.class, "A custom Converter that returns null should cause the StringBuffer constructor to throw NullPointerException because it receives a null CharSequence."), Arguments.of("trigger", new MockConverterThrows(new RuntimeException("convert error")), RuntimeException.class, "If the supplied Converter throws a RuntimeException during conversion, the exception should propagate out of toStringBuffer."));
    }

    /**
     * Unsupported source types – plain Object and int[].
     */
    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(new Object(), null, ClassCastException.class, "Passing an object of a type that has no conversion rule (e.g., plain Object) should result in a ClassCastException inside toCharBuffer."), Arguments.of(new int[] { 1, 2, 3 }, null, ClassCastException.class, "An unsupported primitive array type (int[]) should trigger a ClassCastException when toCharBuffer attempts to treat it as a character source."));
    }

    @Test
    @DisplayName("toStringBuffer converts a String to a matching StringBuffer")
    void testStringConversion() {
        String source = "Hello, World!";
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals(source, result.toString(), "StringBuffer content should match the original String");
    }

    @Test
    @DisplayName("toStringBuffer converts a char[] to a matching StringBuffer")
    void testCharArrayConversion() {
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        assertNotNull(result);
        assertEquals(new String(source), result.toString(), "StringBuffer content should match the original char array");
    }

    @Test
    @DisplayName("toStringBuffer converts a CharBuffer to a matching StringBuffer")
    void testCharBufferConversion() {
        CharBuffer source = CharBuffer.wrap("CharBufferContent");
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        assertNotNull(result);
        assertEquals(source.toString(), result.toString(), "StringBuffer content should match the original CharBuffer");
    }

    @Test
    @DisplayName("toStringBuffer handles empty input correctly")
    void testEmptyInput() {
        String source = "";
        StringBuffer result = CharArrayConversions.toStringBuffer(source, converter);
        assertNotNull(result);
        assertEquals("", result.toString(), "Empty String should produce an empty StringBuffer");
    }

    @Test
    @DisplayName("toStringBuffer with null input returns a StringBuffer containing \"null\"")
    void testNullInput() {
        // The underlying CharArrayConversions implementation treats null as the literal string "null"
        StringBuffer result = CharArrayConversions.toStringBuffer(null, converter);
        assertNotNull(result);
        assertEquals("null", result.toString(), "Null input should be represented by the literal string \"null\" in the StringBuffer");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalConversionsProvider")
    @DisplayName("Normal conversions")
    void testNormalConversions(Object from, Converter converter, String expected, String description) {
        Converter used = (converter == null) ? defaultConverter : converter;
        StringBuffer result = CharArrayConversions.toStringBuffer(from, used);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.toString(), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullAndEmptyProvider")
    @DisplayName("Null and empty inputs")
    void testNullAndEmptyInputs(Object from, Converter converter, Object expectedOrException, String description) {
        Converter used = (converter == null) ? defaultConverter : converter;
        if (expectedOrException instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expectedOrException)) {
            // Expect an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expectedOrException;
            assertThrows(exClass, () -> CharArrayConversions.toStringBuffer(from, used), description);
        } else {
            // Expect a normal result (empty string)
            StringBuffer result = CharArrayConversions.toStringBuffer(from, used);
            assertNotNull(result, "Result must not be null");
            assertEquals(expectedOrException, result.toString(), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nonStringTypesProvider")
    @DisplayName("Non‑string source types")
    void testNonStringTypes(Object from, Converter converter, Object expectedOrException, String description) {
        Converter used = (converter == null) ? defaultConverter : converter;
        if (expectedOrException instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expectedOrException)) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expectedOrException;
            assertThrows(exClass, () -> CharArrayConversions.toStringBuffer(from, used), description);
        } else {
            StringBuffer result = CharArrayConversions.toStringBuffer(from, used);
            assertNotNull(result);
            assertEquals(expectedOrException, result.toString(), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("customConverterProvider")
    @DisplayName("Custom converter usage")
    void testCustomConverter(Object from, Converter converter, String expected, String description) {
        // The supplied converter is already a concrete instance (MockConverter)
        StringBuffer result = CharArrayConversions.toStringBuffer(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.toString(), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("largeDataProvider")
    @DisplayName("Large data handling")
    void testLargeData(Object from, Converter converter, String expected, String description) {
        StringBuffer result = CharArrayConversions.toStringBuffer(from, defaultConverter);
        assertNotNull(result);
        assertEquals(expected, result.toString(), description);
        assertEquals(1000, result.length(), "Length of the resulting buffer should be 1000");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("customConverterEdgeProvider")
    @DisplayName("Custom converter edge cases")
    void testCustomConverterEdgeCases(Object from, Converter converter, Object expectedOrException, String description) {
        if (expectedOrException instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expectedOrException)) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expectedOrException;
            assertThrows(exClass, () -> CharArrayConversions.toStringBuffer(from, converter), description);
        } else {
            // Not expected for the current JSON spec
            fail("Unexpected non‑exception expectation in edge case test");
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unsupportedTypesProvider")
    @DisplayName("Unsupported source types")
    void testUnsupportedTypes(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        Converter used = (converter == null) ? defaultConverter : converter;
        assertThrows(expectedException, () -> CharArrayConversions.toStringBuffer(from, used), description);
    }

    @Test
    @DisplayName("toStringBuffer with null input using default converter throws NullPointerException")
    void testNullInputDefaultConverter() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toStringBuffer(null, defaultConverter), "Default converter should not accept null and must raise NPE");
    }
}
