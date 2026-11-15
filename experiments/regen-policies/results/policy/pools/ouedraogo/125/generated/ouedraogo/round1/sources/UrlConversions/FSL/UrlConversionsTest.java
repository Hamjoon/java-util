package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static org.junit.Assert.*;

import java.net.URI;
import java.net.URL;

import org.junit.Test;

/**
 * Unit tests for {@link UrlConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Successful conversion of a {@link URL} to a {@link java.util.Map} via {@code toMap}.</li>
 *   <li>Behaviour when {@code null} is supplied to {@code toMap} (expected NPE).</li>
 *   <li>Successful conversion of a {@link URL} to a {@link URI} via {@code toURI}.</li>
 *   <li>Exception handling when {@code URL.toURI()} throws an exception (e.g., illegal characters).</li>
 * </ul>
 */
public class UrlConversionsTest {

    // ----------------------------------------------------------------------
    // toMap() tests
    // ----------------------------------------------------------------------

    @Test
    public void testToMapProducesCorrectEntry() throws Exception {
        URL url = new URL("http://example.com/some/path?query=123");
        // The Converter argument is not used by the implementation, so we can pass null.
        java.util.Map<String, Object> result = UrlConversions.toMap(url, null);

        // The map should contain exactly one entry.
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // The key must be the constant URL_KEY and the value must be the URL's string representation.
        assertTrue("Map should contain the URL_KEY", result.containsKey(URL_KEY));
        assertEquals("Map value should be URL.toString()", url.toString(), result.get(URL_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullUrlThrowsNPE() {
        // Passing null will cause a NullPointerException when the method attempts to call url.toString().
        UrlConversions.toMap(null, null);
    }

    // ----------------------------------------------------------------------
    // toURI() tests
    // ----------------------------------------------------------------------

    @Test
    public void testToURISuccessfulConversion() throws Exception {
        URL url = new URL("https://www.example.org/resource?id=42");
        URI uri = UrlConversions.toURI(url, null);

        // The returned URI should be equal to the one produced by URL.toURI().
        assertEquals("URI should match URL.toURI()", url.toURI(), uri);
    }

    @Test
    public void testToURIWhenUrlCannotBeConvertedThrowsIllegalArgumentException() throws Exception {
        // The pipe character '|' is allowed in a URL but illegal in a URI, causing URL.toURI() to throw URISyntaxException.
        URL malformedForUri = new URL("http://example.com/abc|def");

        try {
            UrlConversions.toURI(malformedForUri, null);
            fail("Expected IllegalArgumentException to be thrown because URL.toURI() fails");
        } catch (IllegalArgumentException e) {
            // Verify that the cause of the IllegalArgumentException is the original exception from URL.toURI().
            assertNotNull("Cause should not be null", e.getCause());
            // The underlying cause is typically a URISyntaxException.
            assertTrue("Cause should be an instance of Exception (URISyntaxException)", e.getCause() instanceof Exception);
            // The message should contain the expected prefix.
            assertTrue("Exception message should contain context information",
                    e.getMessage().contains("Unable to convert URL to URI"));
            // Also ensure the original URL appears in the message for easier debugging.
            assertTrue("Exception message should include the original URL",
                    e.getMessage().contains(malformedForUri.toString()));
        }
    }
}
