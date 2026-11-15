package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link UriConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Typical successful conversions for both {@code toMap} and {@code toURL}.</li>
 *   <li>Edge‑case handling such as {@code null} inputs, wrong object types, and malformed URIs.</li>
 *   <li>Exception‑wrapping behaviour of {@code toURL} when {@link URI#toURL()} fails.</li>
 * </ul>
 *
 * <p>Only the public API shown in the prompt and the supplied dependency classes are used.
 */
public class UriConversionsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ---------------------------------------------------------------------- */
    /*  Tests for UriConversions.toMap(Object, Converter)                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toMap_WithValidUri_ReturnsMapContainingUriString() {
        URI uri = URI.create("http://example.com/path?query=123");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) UriConversions.toMap(uri, null);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the key defined by MapConversions.URI_KEY",
                result.containsKey(URI_KEY));
        assertEquals("Stored value must be the URI's string representation",
                uri.toString(), result.get(URI_KEY));
    }

    @Test
    public void toMap_WithNullInput_ThrowsNullPointerException() {
        thrown.expect(NullPointerException.class);
        UriConversions.toMap(null, null);
    }

    @Test
    public void toMap_WithNonUriObject_ThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        UriConversions.toMap("not a uri", null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for UriConversions.toURL(Object, Converter)                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toURL_WithValidHttpUri_ReturnsCorrectUrl() throws Exception {
        URI uri = URI.create("http://example.com/resource");
        URL url = UriConversions.toURL(uri, null);

        assertNotNull("Resulting URL must not be null", url);
        assertEquals("URL string must match the original URI string",
                uri.toString(), url.toString());
    }

    @Test
    public void toURL_WithValidFileUri_ReturnsCorrectUrl() throws Exception {
        // A file‑scheme URI is also convertible to URL.
        URI uri = new URI("file", "/tmp/test.txt", null);
        URL url = UriConversions.toURL(uri, null);

        assertNotNull(url);
        assertEquals(uri.toURL(), url);
    }

    @Test
    public void toURL_WithMalformedUri_ThrowsIllegalArgumentException() throws Exception {
        // mailto scheme cannot be turned into a URL; URI.toURL() throws MalformedURLException.
        URI uri = new URI("mailto:someone@example.com");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unable to convert URI to URL");
        UriConversions.toURL(uri, null);
    }

    @Test
    public void toURL_WithNullInput_ThrowsNullPointerException() {
        thrown.expect(NullPointerException.class);
        UriConversions.toURL(null, null);
    }

    @Test
    public void toURL_WithNonUriObject_ThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        UriConversions.toURL("not a uri", null);
    }
}
