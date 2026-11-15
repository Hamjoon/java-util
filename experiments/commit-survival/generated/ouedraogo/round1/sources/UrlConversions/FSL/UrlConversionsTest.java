package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link UrlConversions}.
 *
 * The tests focus on the two static conversion methods:
 * <ul>
 *   <li>{@link UrlConversions#toMap(Object, Converter)}</li>
 *   <li>{@link UrlConversions#toURI(Object, Converter)}</li>
 * </ul>
 *
 * Only the public API of {@code Converter} and {@code CompactLinkedMap}
 * (as described in the problem statement) are used; no mocking frameworks
 * or external libraries are required.
 */
public class UrlConversionsTest {

    /*--------------------------------------------------------------------
     *  Tests for toMap(Object, Converter)
     *--------------------------------------------------------------------*/

    @Test
    public void testToMapSimpleUrl() throws Exception {
        URL url = new URL("http://example.com");
        Map<String, Object> map = UrlConversions.toMap(url, null);

        // The map must contain exactly one entry with the proper key/value.
        assertEquals(1, map.size());
        assertTrue(map.containsKey(URL_KEY));
        assertEquals(url.toString(), map.get(URL_KEY));
    }

    @Test
    public void testToMapUrlWithPathAndQuery() throws Exception {
        URL url = new URL("https://host.org/path/to/resource?param1=val1&param2=val2#section");
        Map<String, Object> map = UrlConversions.toMap(url, null);

        assertEquals(1, map.size());
        assertEquals(url.toString(), map.get(URL_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullInputThrowsNPE() {
        // The implementation casts the argument to URL without a null‑check,
        // therefore a NullPointerException is expected.
        UrlConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapInvalidTypeThrowsClassCastException() {
        // Passing an object that is not a URL should result in a ClassCastException.
        UrlConversions.toMap("not a URL", null);
    }

    /*--------------------------------------------------------------------
     *  Tests for toURI(Object, Converter)
     *--------------------------------------------------------------------*/

    @Test
    public void testToURINormalConversion() throws Exception {
        URL url = new URL("http://example.org/resource");
        URI uri = UrlConversions.toURI(url, null);

        // The conversion should be equivalent to url.toURI()
        assertEquals(url.toURI(), uri);
    }

    @Test
    public void testToURIWithComplexComponents() throws Exception {
        URL url = new URL("https://example.com:8080/pa th/with%20spaces?query=val+ue#frag ment");
        URI uri = UrlConversions.toURI(url, null);

        // Verify that the resulting URI matches the one produced by URL#toURI()
        assertEquals(url.toURI(), uri);
    }

    @Test
    public void testToURIInvalidUrlThrowsIllegalArgumentException() throws Exception {
        // URL containing a pipe character is syntactically allowed by URL,
        // but URL#toURI() will throw a URISyntaxException, which is wrapped.
        URL url = new URL("http://example.com/invalid|char");
        try {
            UrlConversions.toURI(url, null);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            // The cause should be a URISyntaxException (or any subclass of Exception)
            assertNotNull(e.getCause());
            assertTrue(e.getMessage().contains("Unable to convert URL to URI"));
            assertTrue(e.getMessage().contains(url.toString()));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testToURINullInputThrowsNPE() {
        // As with toMap, a null argument leads to a NullPointerException
        // when the method attempts to call url.toURI().
        UrlConversions.toURI(null, null);
    }
}
