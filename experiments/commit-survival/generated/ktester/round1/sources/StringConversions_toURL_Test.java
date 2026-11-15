package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URL;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.MalformedURLException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toURL_Test {

    @Mock
    private Converter // Converter is not used by toURL, a mock is sufficient
    mockConverter;

    private static Stream<Arguments> validUrlProvider() {
        return Stream.of(Arguments.of("Simple HTTP URL", "http://example.com", "http://example.com"), Arguments.of("HTTPS with path, query and fragment", "https://example.com/path?query=1#frag", "https://example.com/path?query=1#frag"), Arguments.of("Explicit port number", "http://example.com:8080", "http://example.com:8080"), Arguments.of("IPv6 literal host", "http://[2001:db8::1]/", "http://[2001:db8::1]/"));
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(Arguments.of("Whitespace only string", "   "), Arguments.of("Missing host", "http://"), Arguments.of("Mailto scheme (non‑hierarchical)", "mailto:user@example.com"), Arguments.of("Unescaped space in path", "http://example.com/a b"), Arguments.of("Malformed scheme with missing authority", "http:/example.com"), Arguments.of("Port out of range", "http://example.com:99999"), Arguments.of("Unknown protocol", "unknown://host"), Arguments.of("Control character in path", "http://example.com/\u0000"));
    }

    @Test
    void toURL_ValidString_ReturnsUrl() {
        // given
        String urlString = "http://example.com/path?query=123#fragment";
        // when
        URL result = StringConversions.toURL(urlString, mockConverter);
        // then
        assertNotNull(result, "Resulting URL should not be null for a valid string");
        assertEquals(urlString, result.toString(), "The URL string representation should match the input");
    }

    @Test
    void toURL_EmptyString_ReturnsNull() {
        // given
        String empty = "";
        // when
        URL result = StringConversions.toURL(empty, mockConverter);
        // then
        assertNull(result, "Empty string should be converted to null");
    }

    @Test
    void toURL_InvalidString_ThrowsIllegalArgumentException() {
        // given
        String badUrl = "ht!tp://[invalid]";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toURL(badUrl, mockConverter), "Invalid URL strings should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Cannot convert String"), "Exception message should indicate conversion failure");
    }

    @Test
    void toURL_NullInput_ReturnsNull() {
        // when
        URL result = StringConversions.toURL(null, mockConverter);
        // then
        assertNull(result, "Null input should be treated as empty and return null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("illegalArgumentProvider")
    void toURL_InvalidInput_ThrowsIllegalArgumentException(String description, Object input) {
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toURL(input, mockConverter), () -> "Expected IllegalArgumentException for case: " + description);
        // Verify that the exception message contains a hint about conversion failure
        assertTrue(ex.getMessage().contains("Cannot convert String"), "Exception message should indicate conversion failure");
    }

    @Test
    void toURL_NonStringObject_ThrowsClassCastException() {
        // given
        Integer nonString = 12345;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toURL(nonString, mockConverter), "Providing a non‑String object should trigger ClassCastException");
    }
}
