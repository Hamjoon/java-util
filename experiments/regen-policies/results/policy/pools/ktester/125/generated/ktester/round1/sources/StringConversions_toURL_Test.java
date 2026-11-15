package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.net.URL;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toURL_Test {

    /**
     * Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // If ConverterOptions has no-arg constructor, this works; otherwise adjust accordingly.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (not required here).
        converter = null;
    }

    static Stream<Arguments> nullOrEmptyProvider() {
        return Stream.of(// null input
        Arguments.of(null), // empty string
        Arguments.of(""));
    }

    static Stream<Arguments> validUrlProvider() {
        return Stream.of(// Simple HTTP
        Arguments.of("http://example.com", "http://example.com"), // HTTPS with path and query
        Arguments.of("https://example.com/path?query=1", "https://example.com/path?query=1"), // IPv6 literal host
        Arguments.of("http://[2001:db8::1]/", "http://[2001:db8::1]/"), // Explicit port and path
        Arguments.of("http://example.com:8080/path", "http://example.com:8080/path"), // FTP scheme
        Arguments.of("ftp://ftp.example.org/resource.txt", "ftp://ftp.example.org/resource.txt"), // File scheme (Windows path)
        Arguments.of("file:///C:/temp/file.txt", "file:///C:/temp/file.txt"), // User‑info
        Arguments.of("http://user:pass@example.com", "http://user:pass@example.com"), // Fragment identifier
        Arguments.of("http://example.com#section1", "http://example.com#section1"), // Encoded spaces
        Arguments.of("http://example.com/path%20with%20spaces", "http://example.com/path%20with%20spaces"));
    }

    static Stream<Arguments> invalidUrlProvider() {
        return Stream.of(// Whitespace only
        Arguments.of("   "), // Plain malformed string
        Arguments.of("not a url"), // Unsupported scheme (mailto)
        Arguments.of("mailto:user@example.com"), // Raw space in path
        Arguments.of("http://example.com/space here"), // Malformed IPv6 literal
        Arguments.of("http://[invalid::]/"), // Missing host
        Arguments.of("http://"), // Non‑numeric port
        Arguments.of("http://example.com:badport"), // Internationalized domain name not puny‑coded
        Arguments.of("http://例子.测试"), // Relative URI (no scheme)
        Arguments.of("relative/path/resource"), // Custom unsupported scheme
        Arguments.of("custom://host/resource"), // Port out of range (too high)
        Arguments.of("http://example.com:65536"), // Negative port number
        Arguments.of("http://example.com:-1"), // Trailing colon without port
        Arguments.of("http://example.com:"), // Literal string "null"
        Arguments.of("null"), // Space inside query component
        Arguments.of("http://example.com/path?query=hello world"), // Space in user‑info
        Arguments.of("http://user:pa ss@example.com"));
    }

    static Stream<Arguments> nonStringProvider() {
        return Stream.of(// Integer value
        Arguments.of(12345), // Generic Object instance
        Arguments.of(new Object()));
    }

    @Test
    void toURL_ValidString_ReturnsUrl() {
        // given
        String urlString = "http://example.com";
        // when
        URL result = StringConversions.toURL(urlString, converter);
        // then
        assertNotNull(result, "Resulting URL should not be null for a valid string");
        assertEquals(urlString, result.toString(), "URL string representation must match the input");
    }

    @Test
    void toURL_EmptyString_ReturnsNull() {
        // given
        String empty = "";
        // when
        URL result = StringConversions.toURL(empty, converter);
        // then
        assertNull(result, "Empty string should be converted to null");
    }

    @Test
    void toURL_NullInput_ThrowsIllegalArgumentException() {
        // given
        Object nullObj = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toURL(nullObj, converter), "Passing null should result in IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Cannot convert String"), "Exception message should indicate conversion failure");
    }

    @Test
    void toURL_MalformedString_ThrowsIllegalArgumentException() {
        // given
        String badUrl = "not a valid url";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toURL(badUrl, converter), "Malformed URL string should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Cannot convert String"), "Exception message should indicate conversion failure");
    }

    /**
     * Null or empty inputs should result in a {@code null} return value.
     * The method treats {@code null} and empty strings as “empty”.
     */
    @ParameterizedTest(name = "[{index}] toURL({0}) → null")
    @MethodSource("nullOrEmptyProvider")
    void toURL_NullOrEmpty_ReturnsNull(Object input) {
        // The converter argument is not used by the method; pass {@code null}.
        URL result = StringConversions.toURL(input, null);
        assertNull(result, "Expected null for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] toURL({0}) → {1}")
    @MethodSource("validUrlProvider")
    void toURL_ValidUrls_ReturnsUrl(String input, String expectedUrl) throws Exception {
        URL result = StringConversions.toURL(input, null);
        assertNotNull(result, "Result should not be null for a valid URL");
        assertEquals(expectedUrl, result.toString(), () -> "URL string mismatch for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] toURL({0}) → IllegalArgumentException")
    @MethodSource("invalidUrlProvider")
    void toURL_InvalidUrls_ThrowsIllegalArgumentException(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toURL(input, null), "Expected IllegalArgumentException for input: " + input);
        // Verify that the message contains the original string for easier debugging.
        assertTrue(ex.getMessage().contains("Cannot convert String"), "Exception message should mention conversion failure");
    }

    @ParameterizedTest(name = "[{index}] toURL({0}) → ClassCastException")
    @MethodSource("nonStringProvider")
    void toURL_NonStringInput_ThrowsClassCastException(Object input) {
        assertThrows(ClassCastException.class, () -> StringConversions.toURL(input, null), "Expected ClassCastException for non‑String input: " + input);
    }
}
