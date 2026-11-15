package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UrlConversions_toURI_Test {

    /**
     * Converter instance required by the method signature (not used internally).
     */
    private static Converter converter;

    /**
     * Helper class to describe the expected components of a URI.
     */
    private static final class URIExpectation {

        final String scheme;

        final String host;

        // -1 means undefined (as per URI#getPort)
        final int port;

        final String path;

        final String query;

        final String fragment;

        URIExpectation(String scheme, String host, int port, String path, String query, String fragment) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.path = path;
            this.query = query;
            this.fragment = fragment;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter constructor needs a ConverterOptions instance; use the default one.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Cleanup static resources if any.
        converter = null;
    }

    /**
     * Supplies a stream of successful conversion cases.
     */
    private static Stream<Arguments> provideSuccessfulUrls() throws Exception {
        return Stream.of(// simple path
        Arguments.of("simple URL with path", new URL("http://example.com/path"), new URIExpectation("http", "example.com", -1, "/path", null, null)), // file scheme
        Arguments.of("file‑scheme URL", new URL("file:/C:/temp/file.txt"), new URIExpectation("file", null, -1, "/C:/temp/file.txt", null, null)), // IPv6 literal host
        Arguments.of("IPv6 host URL", new URL("http://[2001:db8::1]/resource"), new URIExpectation("http", "2001:db8::1", -1, "/resource", null, null)), // URL with port and query
        Arguments.of("URL with port and query", new URL("http://example.com:8080/search?q=test&lang=en"), new URIExpectation("http", "example.com", 8080, "/search", "q=test&lang=en", null)), // URL with fragment
        Arguments.of("URL with fragment", new URL("http://example.com/page#section2"), new URIExpectation("http", "example.com", -1, "/page", null, "section2")), // URL with empty path
        Arguments.of("URL with empty path", new URL("http://example.com"), new URIExpectation("http", "example.com", -1, "", null, null)));
    }

    @Test
    void toURI_convertsValidURLToURI() throws Exception {
        // GIVEN
        URL url = new URL("http://example.com/path?query=123#fragment");
        // WHEN
        URI result = UrlConversions.toURI(url, converter);
        // THEN
        assertEquals(url.toURI(), result, "The returned URI should match URL.toURI()");
    }

    @Test
    void toURI_whenURLCannotBeConverted_throwsIllegalArgumentException() throws Exception {
        // GIVEN: a URL that is syntactically valid for URL but not for URI (contains a pipe character)
        URL problematicUrl = new URL("http://example.com/abc|def");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(problematicUrl, converter));
        // Verify that the cause is a URISyntaxException and that the message contains the URL.
        assertTrue(ex.getCause() instanceof URISyntaxException, "Cause of the IllegalArgumentException should be URISyntaxException");
        assertTrue(ex.getMessage().contains(problematicUrl.toString()), "Exception message should contain the original URL");
    }

    /* --------------------------------------------------------------
     *  Success scenarios – parameterized to keep the test concise.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulUrls")
    void toURI_success(String description, URL inputUrl, URIExpectation expectation) {
        // WHEN
        // converter is not used
        URI result = UrlConversions.toURI(inputUrl, null);
        // THEN – verify each component according to the expectation
        assertEquals(expectation.scheme, result.getScheme(), "Scheme should match");
        assertEquals(expectation.host, result.getHost(), "Host should match");
        assertEquals(expectation.port, result.getPort(), "Port should match");
        assertEquals(expectation.path, result.getPath(), "Path should match");
        assertEquals(expectation.query, result.getQuery(), "Query should match");
        assertEquals(expectation.fragment, result.getFragment(), "Fragment should match");
    }

    /* --------------------------------------------------------------
     *  Null input – expect NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void toURI_nullInput() {
        // GIVEN a null 'from' argument
        Object nullFrom = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UrlConversions.toURI(nullFrom, null), "Passing null as the 'from' argument should throw NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Invalid type – expect ClassCastException.
     * -------------------------------------------------------------- */
    @Test
    void toURI_invalidType() {
        // GIVEN an object that is not a URL
        Object notAUrl = "not a URL";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UrlConversions.toURI(notAUrl, null), "Passing a non‑URL object should cause a ClassCastException");
    }

    /* --------------------------------------------------------------
     *  URISyntaxException wrapped in IllegalArgumentException.
     * -------------------------------------------------------------- */
    @Test
    void toURI_uriSyntaxException_backslash() throws Exception {
        // GIVEN a URL containing a backslash which is illegal in a URI
        URL badUrl = new URL("http://example.com/invalid\\path");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(badUrl, null));
        // Verify that the cause is a URISyntaxException and that the message contains the URL.
        assertTrue(ex.getCause() instanceof URISyntaxException, "Cause should be URISyntaxException");
        assertTrue(ex.getMessage().contains(badUrl.toString()), "Exception message should contain the original URL");
    }

    @Test
    void toURI_uriSyntaxException_pipeCharacter() throws Exception {
        // GIVEN: a URL that is syntactically valid for URL but not for URI (contains a pipe character)
        URL problematicUrl = new URL("http://example.com/abc|def");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(problematicUrl, null));
        // Verify that the cause is a URISyntaxException and that the message contains the URL.
        assertTrue(ex.getCause() instanceof URISyntaxException, "Cause of the IllegalArgumentException should be URISyntaxException");
        assertTrue(ex.getMessage().contains(problematicUrl.toString()), "Exception message should contain the original URL");
    }
}
