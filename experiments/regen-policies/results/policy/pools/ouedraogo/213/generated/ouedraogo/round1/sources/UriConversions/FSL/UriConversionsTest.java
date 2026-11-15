package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

public class UriConversionsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Verify that {@code toMap} creates a map containing a single entry whose key is
     * {@link MapConversions#URI_KEY} and whose value is the string representation of the supplied URI.
     */
    @Test
    public void testToMapCreatesCorrectEntry() {
        URI uri = URI.create("http://example.com/path?query=123");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) UriConversions.toMap(uri, null);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the URI_KEY", result.containsKey(URI_KEY));
        assertEquals("Value must be the URI's string form", uri.toString(), result.get(URI_KEY));
    }

    /**
     * Ensure that {@code toMap} throws a {@link NullPointerException} when a {@code null}
     * source object is supplied (the method casts the argument to {@link URI} without a check).
     */
    @Test(expected = NullPointerException.class)
    public void testToMapNullInputThrowsNPE() {
        UriConversions.toMap(null, null);
    }

    /**
     * Verify that a well‑formed {@link URI} is correctly converted to a {@link URL}
     * by {@code toURL}.
     */
    @Test
    public void testToURLValidUri() throws Exception {
        URI uri = new URI("https://www.example.org/test?param=value");
        URL url = UriConversions.toURL(uri, null);

        assertNotNull("URL should not be null", url);
        assertEquals("URL string must match the original URI string", uri.toString(), url.toString());
        assertEquals("Protocol must be https", "https", url.getProtocol());
    }

    /**
     * Verify that {@code toURL} wraps a {@link java.net.MalformedURLException}
     * in an {@link IllegalArgumentException} with the original URI in the message.
     */
    @Test
    public void testToURLInvalidUriThrowsIllegalArgumentException() throws Exception {
        // A URI with a scheme that cannot be converted to a URL (e.g., "mailto") triggers
        // a MalformedURLException from URI.toURL().
        URI uri = new URI("mailto:someone@example.com");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unable to convert URI to URL, input URI: " + uri);

        UriConversions.toURL(uri, null);
    }

    /**
     * Confirm that passing {@code null} to {@code toURL} results in a {@link NullPointerException},
     * because the method attempts to cast the argument to {@link URI} before any checks.
     */
    @Test(expected = NullPointerException.class)
    public void testToURLNullInputThrowsNPE() {
        UriConversions.toURL(null, null);
    }
}
