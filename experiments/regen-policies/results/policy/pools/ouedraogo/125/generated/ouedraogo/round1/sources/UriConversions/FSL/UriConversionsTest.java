package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

public class UriConversionsTest {

    /** Helper that creates a dummy {@link Converter}. The implementation of {@link Converter}
     *  is not exercised by {@link UriConversions}, therefore a {@code null} options object is sufficient. */
    private static Converter dummyConverter() {
        return new Converter(null);
    }

    @Test
    public void testToMapProducesMapWithUriString() {
        URI uri = URI.create("http://example.com/path?query=123");
        Map result = UriConversions.toMap(uri, dummyConverter());

        // The map must contain exactly one entry, the URI_KEY → uri.toString()
        assertNotNull("Resulting map should not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain the URI_KEY", result.containsKey(URI_KEY));
        assertEquals("Value stored under URI_KEY must be the URI string",
                uri.toString(), result.get(URI_KEY));

        // Verify the concrete type is a CompactMap (builder creates this type)
        assertTrue("Result should be an instance of CompactMap",
                result instanceof CompactMap);
    }

    @Test
    public void testToMapWithEmptyUri() {
        URI emptyUri = URI.create("");
        Map result = UriConversions.toMap(emptyUri, dummyConverter());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(URI_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullInputThrowsNPE() {
        // The method casts the argument to URI without a null‑check, so a NPE is expected.
        UriConversions.toMap(null, dummyConverter());
    }

    @Test
    public void testToURLSuccessfulConversion() throws Exception {
        URI uri = URI.create("http://example.org/");
        URL url = UriConversions.toURL(uri, dummyConverter());

        assertNotNull("URL result must not be null", url);
        assertEquals("URL string must match the original URI string", uri.toString(), url.toString());
    }

    @Test
    public void testToURLFileSchemeConversion() throws Exception {
        // File scheme URIs are also hierarchical and can be turned into URLs.
        URI fileUri = URI.create("file:/tmp/test.txt");
        URL url = UriConversions.toURL(fileUri, dummyConverter());

        assertNotNull(url);
        assertEquals("file", url.getProtocol());
        assertEquals("/tmp/test.txt", url.getPath());
    }

    @Test
    public void testToURLThrowsIllegalArgumentExceptionForNonHierarchicalUri() {
        // URN is not hierarchical; URI.toURL() throws a MalformedURLException which we wrap.
        URI urn = URI.create("urn:isbn:0451450523");

        try {
            UriConversions.toURL(urn, dummyConverter());
            fail("Expected IllegalArgumentException to be thrown for non‑hierarchical URI");
        } catch (IllegalArgumentException e) {
            // Verify the message contains the original URI string
            assertTrue(e.getMessage().contains(urn.toString()));
            // The cause should be a MalformedURLException (or a subclass thereof)
            assertNotNull("Cause must be present", e.getCause());
            assertTrue(e.getCause() instanceof java.net.MalformedURLException);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testToURLWithNullInputThrowsNPE() {
        // Same as toMap, the method casts without null‑check.
        UriConversions.toURL(null, dummyConverter());
    }
}
