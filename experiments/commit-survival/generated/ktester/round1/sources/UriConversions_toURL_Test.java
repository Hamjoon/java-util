package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UriConversions_toURL_Test {

    /**
     * The Converter is not used by {@code UriConversions.toURL}, but we provide a mock
     *  to satisfy the method signature and to demonstrate proper test‑setup.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> provideSuccessCases() throws Exception {
        return Stream.of(// group: toURL_success
        Arguments.of("A well‑formed hierarchical HTTP URI should be converted to an equivalent URL", URI.create("http://example.com"), new URL("http://example.com")), // group: toURL_success_additional – HTTPS with query & fragment
        Arguments.of("HTTPS URI with path, query and fragment", URI.create("https://secure.example.org/path?query=1#section"), new URL("https://secure.example.org/path?query=1#section")), // file scheme
        Arguments.of("File‑scheme URI should be converted to a file URL (single slash after scheme)", URI.create("file:///C:/temp/file.txt"), new URL("file:/C:/temp/file.txt")), // FTP scheme
        Arguments.of("Hierarchical FTP URI should be converted to the corresponding FTP URL", URI.create("ftp://ftp.example.com/pub/file.txt"), new URL("ftp://ftp.example.com/pub/file.txt")), // percent‑encoded space
        Arguments.of("URI containing percent‑encoded spaces should be preserved in the resulting URL", URI.create("http://example.com/space%20here"), new URL("http://example.com/space%20here")));
    }

    private static Stream<Arguments> provideInvalidSchemeCases() {
        return Stream.of(Arguments.of("mailto URI cannot be converted to a URL", URI.create("mailto:user@example.com")), Arguments.of("URN (non‑hierarchical) URI should cause IllegalArgumentException", URI.create("urn:isbn:0451450523")), Arguments.of("Relative URI without a scheme should result in IllegalArgumentException", URI.create("relative/path/resource")), Arguments.of("Opaque data URI cannot be converted to a URL", URI.create("data:text/plain,Hello%20World")));
    }

    @Test
    @DisplayName("toURL converts a valid URI to a URL")
    void testToURL_Success() throws MalformedURLException {
        // given
        URI uri = URI.create("http://example.com/path?query=123#fragment");
        // when
        URL result = UriConversions.toURL(uri, mockConverter);
        // then
        assertNotNull(result, "Resulting URL should not be null");
        assertEquals(uri.toURL(), result, "The URL produced should match URI.toURL()");
    }

    @Test
    @DisplayName("toURL throws IllegalArgumentException when URI cannot be converted to URL")
    void testToURL_InvalidURI_ThrowsIllegalArgumentException() {
        // given: a URI with a scheme that cannot be represented as a URL (e.g., mailto)
        URI uri = URI.create("mailto:someone@example.com");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UriConversions.toURL(uri, mockConverter), "Expected IllegalArgumentException for non‑convertible URI");
        // optional: verify that the cause is a MalformedURLException
        assertTrue(ex.getCause() instanceof MalformedURLException, "Cause of the exception should be a MalformedURLException");
        assertTrue(ex.getMessage().contains("Unable to convert URI to URL"), "Exception message should contain the expected prefix");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessCases")
    @DisplayName("toURL – successful conversions")
    void testToURL_SuccessCases(String description, URI input, URL expected) throws Exception {
        // when
        URL actual = UriConversions.toURL(input, mockConverter);
        // then
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideInvalidSchemeCases")
    @DisplayName("toURL – invalid scheme should throw IllegalArgumentException")
    void testToURL_InvalidScheme(String description, URI input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UriConversions.toURL(input, mockConverter), description);
        assertTrue(ex.getCause() instanceof MalformedURLException, "Underlying cause must be MalformedURLException");
    }

    @Test
    @DisplayName("toURL – null 'from' argument should throw NullPointerException")
    void testToURL_NullInput() {
        assertThrows(NullPointerException.class, () -> UriConversions.toURL(null, mockConverter), "Passing null for the 'from' argument should result in NPE");
    }

    @Test
    @DisplayName("toURL – wrong type for 'from' argument should throw ClassCastException")
    void testToURL_WrongType() {
        // The method casts the argument to URI, so passing a String triggers ClassCastException
        assertThrows(ClassCastException.class, () -> UriConversions.toURL("http://example.com", mockConverter), "Passing a non‑URI object should trigger ClassCastException");
    }
}
