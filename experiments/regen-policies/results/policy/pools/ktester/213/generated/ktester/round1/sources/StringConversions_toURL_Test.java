package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URL;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toURL_Test {

    /**
     * Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not need any special configuration for the toURL conversion.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Parameter source that yields one {@link Arguments} entry per test case described in the JSON.
     *
     * The tuple is: {@code fromObject}, {@code expectedResult}.
     * <ul>
     *   <li>If {@code expectedResult} is {@code null}, the method should return {@code null}.</li>
     *   <li>If {@code expectedResult} is a {@link String}, the method should return a {@link URL}
     *       whose {@code toString()} equals that string.</li>
     *   <li>If {@code expectedResult} is {@code IllegalArgumentException.class},
     *       the method must throw that exception.</li>
     *   <li>If {@code expectedResult} is {@code ClassCastException.class},
     *       the method must throw that exception.</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> urlConversionCases() {
        return Stream.of(// null input – treated as empty by StringUtilities.isEmpty → null result
        org.junit.jupiter.params.provider.Arguments.of(null, null), // empty string → null
        org.junit.jupiter.params.provider.Arguments.of("", null), // simple valid URL
        org.junit.jupiter.params.provider.Arguments.of("http://example.com", "http://example.com"), // complex valid URL with query and fragment
        org.junit.jupiter.params.provider.Arguments.of("https://example.com/path?query=1#fragment", "https://example.com/path?query=1#fragment"), // space in URL – illegal
        org.junit.jupiter.params.provider.Arguments.of("http://example.com/space here", IllegalArgumentException.class), // malformed scheme characters
        org.junit.jupiter.params.provider.Arguments.of("ht!tp://example.com", IllegalArgumentException.class), // mailto scheme – URI is valid but not a hierarchical URL
        org.junit.jupiter.params.provider.Arguments.of("mailto:user@example.com", IllegalArgumentException.class), // user‑info in URL
        org.junit.jupiter.params.provider.Arguments.of("http://user:pass@example.com", "http://user:pass@example.com"), // IPv6 literal with port
        org.junit.jupiter.params.provider.Arguments.of("http://[2001:db8::1]:8080/", "http://[2001:db8::1]:8080/"), // file‑scheme URL
        org.junit.jupiter.params.provider.Arguments.of("file:///C:/path/to/file.txt", "file:///C:/path/to/file.txt"), // percent‑encoded space – valid
        org.junit.jupiter.params.provider.Arguments.of("http://example.com/space%20here", "http://example.com/space%20here"), // illegal pipe character
        org.junit.jupiter.params.provider.Arguments.of("http://example.com/|invalid|", IllegalArgumentException.class), // whitespace only – not considered empty, URI.create will fail
        org.junit.jupiter.params.provider.Arguments.of("   ", IllegalArgumentException.class), // relative path – no scheme
        org.junit.jupiter.params.provider.Arguments.of("relative/path", IllegalArgumentException.class), // non‑String object (StringBuilder) – causes ClassCastException
        org.junit.jupiter.params.provider.Arguments.of(new StringBuilder("http://example.com"), ClassCastException.class), // non‑String object (Integer) – causes ClassCastException
        org.junit.jupiter.params.provider.Arguments.of(123, ClassCastException.class), // incomplete percent‑encoding
        org.junit.jupiter.params.provider.Arguments.of("http://example.com/%", IllegalArgumentException.class), // malformed IPv6 literal (missing closing bracket)
        org.junit.jupiter.params.provider.Arguments.of("http://[2001:db8::1", IllegalArgumentException.class), // backslash in path – illegal
        org.junit.jupiter.params.provider.Arguments.of("http://example.com\\path", IllegalArgumentException.class), // unescaped curly braces
        org.junit.jupiter.params.provider.Arguments.of("http://example.com/{test}", IllegalArgumentException.class), // non‑numeric port
        org.junit.jupiter.params.provider.Arguments.of("http://example.com:abc", IllegalArgumentException.class), // URN scheme – cannot be turned into a URL
        org.junit.jupiter.params.provider.Arguments.of("urn:isbn:0451450523", IllegalArgumentException.class));
    }

    @Test
    void toURL_WithEmptyString_ReturnsNull() {
        // given
        String empty = "";
        // when
        URL result = StringConversions.toURL(empty, converter);
        // then
        assertNull(result, "Empty string should be converted to null");
    }

    @Test
    void toURL_WithValidUrlString_ReturnsUrl() {
        // given
        String urlString = "http://example.com/path?query=1#fragment";
        // when
        URL result = StringConversions.toURL(urlString, converter);
        // then
        assertNotNull(result, "Result should not be null for a valid URL string");
        assertEquals(urlString, result.toString(), "Converted URL should match the original string");
    }

    @Test
    void toURL_WithInvalidUrlString_ThrowsIllegalArgumentException() {
        // given
        String badUrl = "not a valid url";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toURL(badUrl, converter), "Invalid URL strings should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Cannot convert String"), "Exception message should indicate conversion failure");
    }

    @Test
    void toURL_WithNullObject_ThrowsClassCastException() {
        // The method casts the input to String without a null‑check, so a null argument
        // results in a ClassCastException (or NullPointerException) wrapped in IllegalArgumentException.
        // This test documents the current behaviour.
        // given
        Object nullObj = null;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toURL(nullObj, converter), "Passing null should result in a ClassCastException due to the cast to String");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("urlConversionCases")
    void toURL_VariousInputs(Object from, Object expected) {
        if (expected == null) {
            // Expected result: null URL
            URL result = StringConversions.toURL(from, converter);
            assertNull(result, "Expected null for input: " + from);
        } else if (expected instanceof String) {
            // Expected result: a URL whose string representation matches the expected string
            URL result = StringConversions.toURL(from, converter);
            assertNotNull(result, "Resulting URL should not be null for input: " + from);
            assertEquals(expected, result.toString(), "URL string mismatch for input: " + from);
        } else if (expected == IllegalArgumentException.class) {
            // Expected exception: IllegalArgumentException
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toURL(from, converter), "Expected IllegalArgumentException for input: " + from);
            assertTrue(ex.getMessage().contains("Cannot convert String"), "Exception message should contain conversion hint");
        } else if (expected == ClassCastException.class) {
            // Expected exception: ClassCastException (cast to String fails)
            assertThrows(ClassCastException.class, () -> StringConversions.toURL(from, converter), "Expected ClassCastException for non‑String input: " + from);
        } else {
            fail("Test data contains an unexpected expected value: " + expected);
        }
    }
}
