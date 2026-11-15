package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link UriConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Successful conversion of a {@link URI} to a {@code Map}.</li>
 *   <li>Successful conversion of a hierarchical {@link URI} to a {@link URL}.</li>
 *   <li>Failure scenarios such as passing {@code null}, a non‑URI object,
 *       or a non‑hierarchical URI that cannot be turned into a {@link URL}.</li>
 * </ul>
 */
public class UriConversionsTest {

    /** A real {@link Converter} instance – the {@code UriConversions} methods do not use it,
     *  but the signature requires it. */
    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     * toMap() tests
     * -------------------------------------------------------------- */

    @Test
    public void testToMapWithValidUri() {
        URI uri = URI.create("https://example.com/path?query=123");
        Map<String, Object> result = UriConversions.toMap(uri, converter);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());

        // The key is defined in MapConversions.URI_KEY
        assertTrue("Map must contain the URI_KEY", result.containsKey(MapConversions.URI_KEY));
        assertEquals("Value must be the string representation of the URI",
                uri.toString(),
                result.get(MapConversions.URI_KEY));
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithNonUriObject() {
        // The method casts the input to URI; a String will cause ClassCastException.
        UriConversions.toMap("not a uri", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNull() {
        // Casting null to URI succeeds (null), but calling toString() on null throws NPE.
        UriConversions.toMap(null, converter);
    }

    /* --------------------------------------------------------------
     * toURL() tests
     * -------------------------------------------------------------- */

    @Test
    public void testToURLWithHierarchicalUri() throws Exception {
        URI uri = URI.create("http://www.example.org/index.html?param=value#section");
        URL url = UriConversions.toURL(uri, converter);

        assertNotNull("URL must not be null", url);
        assertEquals("URL string must match the original URI", uri.toURL(), url);
    }

    @Test
    public void testToURLWithNonHierarchicalUriThrowsIllegalArgumentException() {
        // "mailto:" URIs are non‑hierarchical and cannot be converted to URL.
        URI uri = URI.create("mailto:someone@example.com");

        try {
            UriConversions.toURL(uri, converter);
            fail("Expected IllegalArgumentException to be thrown for non‑hierarchical URI");
        } catch (IllegalArgumentException e) {
            // Verify the message contains the original URI.
            assertTrue("Exception message must contain the original URI",
                    e.getMessage().contains(uri.toString()));

            // The cause should be a MalformedURLException.
            assertNotNull("Cause must be present", e.getCause());
            assertTrue("Cause must be a MalformedURLException",
                    e.getCause() instanceof java.net.MalformedURLException);
        }
    }

    @Test(expected = ClassCastException.class)
    public void testToURLWithNonUriObject() {
        UriConversions.toURL(12345, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToURLWithNull() {
        // Casting null to URI yields null; calling toURL() on null throws NPE.
        UriConversions.toURL(null, converter);
    }
}
