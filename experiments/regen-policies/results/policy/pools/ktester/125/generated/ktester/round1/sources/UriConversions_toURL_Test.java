package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.MalformedURLException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UriConversions_toURL_Test {

    /**
     * The Converter instance used in the tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter does not need any special configuration for the
        // toURL conversion – a plain instance is sufficient.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference; helps GC in larger test suites.
        converter = null;
    }

    /**
     * Supplies the data for {@link #toURL_additionalScenarios}.
     *
     * Each entry consists of:
     * - description (for readable test names)
     * - the URI to convert
     * - the Converter to pass (null or a dummy instance)
     * - the expected outcome (URL instance for success, exception class for failure)
     */
    static Stream<Arguments> additionalTestProvider() throws Exception {
        // A dummy Converter that does nothing; the method ignores it.
        Converter dummy = new Converter(new ConverterOptions());
        return Stream.of(// HTTPS with query and fragment – should preserve all components.
        Arguments.of("HTTPS URI with query and fragment", new URI("https://example.com/search?q=test#section"), dummy, new URL("https://example.com/search?q=test#section")), // File‑scheme URI – conversion to URL should succeed.
        Arguments.of("File‑scheme URI", new URI("file:///tmp/test.txt"), dummy, new URL("file:/tmp/test.txt")), // IPv6 host literal – must be handled correctly.
        Arguments.of("IPv6 host URI", new URI("http://[2001:db8::1]/"), dummy, new URL("http://[2001:db8::1]/")), // Relative URI lacking scheme – should raise IllegalArgumentException.
        Arguments.of("Relative URI without scheme", new URI("/relative/path"), dummy, IllegalArgumentException.class), // Unsupported mailto scheme – also results in IllegalArgumentException.
        Arguments.of("Mailto scheme URI", new URI("mailto:user@example.com"), dummy, IllegalArgumentException.class), // Non‑null Converter argument – conversion result must be unchanged.
        Arguments.of("Non‑null Converter argument (custom subclass)", new URI("http://example.com"), new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object o, Class<?> c) {
                // Intentionally return null to prove it is ignored.
                return null;
            }
        }, new URL("http://example.com")), // URI containing user‑info – credentials should survive conversion.
        Arguments.of("URI with user‑info", new URI("http://user:pass@example.com"), dummy, new URL("http://user:pass@example.com")));
    }

    @Test
    void toURL_convertsValidUriToUrl() {
        // GIVEN
        URI uri = URI.create("https://www.example.com/path?query=1");
        // WHEN
        URL result = UriConversions.toURL(uri, converter);
        // THEN
        assertNotNull(result, "Resulting URL should not be null");
        assertEquals("https", result.getProtocol());
        assertEquals("www.example.com", result.getHost());
        assertEquals("/path", result.getPath());
        assertEquals("query=1", result.getQuery());
    }

    @Test
    void toURL_throwsIllegalArgumentExceptionWhenUriCannotBeConverted() {
        // GIVEN
        // A URI with a scheme that cannot be represented as a URL (mailto:)
        URI uri = URI.create("mailto:user@example.com");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UriConversions.toURL(uri, converter), "Expected IllegalArgumentException for non‑URL‑compatible URI");
        // Verify that the exception message contains the original URI for easier debugging.
        assertTrue(ex.getMessage().contains(uri.toString()), "Exception message should contain the original URI");
        // The cause should be a MalformedURLException.
        assertNotNull(ex.getCause(), "Cause should not be null");
        assertEquals(java.net.MalformedURLException.class, ex.getCause().getClass(), "Cause should be a MalformedURLException");
    }

    @Test
    void toURL_validHierarchicalUri_returnsUrl() throws Exception {
        // GIVEN
        URI uri = new URI("http://example.com/path");
        // WHEN
        URL result = UriConversions.toURL(uri, converter);
        // THEN
        assertEquals(new URL("http://example.com/path"), result);
    }

    @Test
    void toURL_nonHierarchicalUrn_throwsIllegalArgumentException() {
        // GIVEN
        URI uri = URI.create("urn:example:test");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UriConversions.toURL(uri, converter), "URN cannot be converted to a URL");
        assertTrue(ex.getMessage().contains("Unable to convert URI to URL"), "Exception message must contain the standard prefix");
        assertNotNull(ex.getCause(), "Cause should be present");
        assertEquals(MalformedURLException.class, ex.getCause().getClass(), "Underlying cause must be a MalformedURLException");
    }

    @Test
    void toURL_nullInput_throwsIllegalArgumentException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UriConversions.toURL(from, converter), "Null URI should result in IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Unable to convert URI to URL"));
        assertNotNull(ex.getCause(), "Cause should be present");
        assertEquals(NullPointerException.class, ex.getCause().getClass(), "Underlying cause must be NullPointerException");
    }

    @Test
    void toURL_inputNotUri_throwsClassCastException() {
        // GIVEN
        Object notAUri = "just a string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UriConversions.toURL(notAUri, converter), "Passing a non‑URI should raise ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalTestProvider")
    void toURL_additionalScenarios(String description, URI uri, Converter conv, Object expected) throws Exception {
        if (expected instanceof URL) {
            // Expected a successful conversion
            URL result = UriConversions.toURL(uri, conv);
            assertEquals(expected, result, description);
        } else if (expected instanceof Class) {
            // Expected an exception type
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            Throwable thrown = assertThrows(exClass, () -> UriConversions.toURL(uri, conv), description);
            if (thrown instanceof IllegalArgumentException) {
                IllegalArgumentException iae = (IllegalArgumentException) thrown;
                assertTrue(iae.getMessage().contains("Unable to convert URI to URL"));
                assertNotNull(iae.getCause(), "Cause must be present");
                assertEquals(MalformedURLException.class, iae.getCause().getClass(), "Cause should be MalformedURLException for this scenario");
            }
        }
    }
}
