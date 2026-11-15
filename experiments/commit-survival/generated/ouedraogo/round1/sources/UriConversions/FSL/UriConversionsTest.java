package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link UriConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of a {@link URI} to a {@link Map} with the proper key/value.</li>
 *   <li>Conversion of a {@link URI} to a {@link URL} for supported schemes.</li>
 *   <li>Handling of {@code null} input (expected {@link NullPointerException}).</li>
 *   <li>Conversion failures (e.g., unsupported scheme) resulting in {@link IllegalArgumentException}.</li>
 *   <li>Edge‑case URIs such as empty strings and relative paths.</li>
 * </ul>
 */
public class UriConversionsTest {

    /** Helper that supplies a dummy {@link Converter}. The implementation under test does not use it. */
    private static Converter dummyConverter() {
        // The Converter constructor requires a ConverterOptions instance.
        // Since the class under test never dereferences the Converter, we can safely pass null.
        return new Converter(null);
    }

    @Test
    public void testToMapWithAbsoluteHttpUri() {
        URI uri = URI.create("http://example.com/path?query=1");
        Map<String, Object> result = UriConversions.toMap(uri, dummyConverter());

        assertNotNull("Resulting map must not be null", result);
        assertTrue("Resulting map must contain the URI key", result.containsKey(URI_KEY));
        assertEquals("Map value must be the URI string", uri.toString(), result.get(URI_KEY));
    }

    @Test
    public void testToMapWithEmptyUriString() {
        URI uri = URI.create("");
        Map<String, Object> result = UriConversions.toMap(uri, dummyConverter());

        assertNotNull(result);
        assertEquals("", result.get(URI_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullInput() {
        // The method casts the input to URI and then calls toString(),
        // which will throw NPE when the argument itself is null.
        UriConversions.toMap(null, dummyConverter());
    }

    @Test
    public void testToURLWithHttpScheme() throws Exception {
        URI uri = new URI("http://example.com:8080/test?param=value");
        URL url = UriConversions.toURL(uri, dummyConverter());

        assertNotNull(url);
        assertEquals(uri.toURL(), url);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
        assertEquals(8080, url.getPort());
        assertEquals("/test", url.getPath());
        assertEquals("param=value", url.getQuery());
    }

    @Test
    public void testToURLWithFileScheme() throws Exception {
        // File URIs are also convertible to URL.
        URI uri = new URI("file:///tmp/test.txt");
        URL url = UriConversions.toURL(uri, dummyConverter());

        assertNotNull(url);
        assertEquals("file", url.getProtocol());
        assertEquals("/tmp/test.txt", url.getPath());
    }

    @Test
    public void testToURLWithUnsupportedScheme() {
        // "mailto" cannot be turned into a URL; expect IllegalArgumentException.
        URI uri = URI.create("mailto:user@example.com");
        try {
            UriConversions.toURL(uri, dummyConverter());
            fail("Expected IllegalArgumentException for unsupported scheme");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unable to convert URI to URL"));
            assertTrue(e.getMessage().contains(uri.toString()));
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testToURLWithRelativeUri() {
        // Relative URIs lack a scheme and host – conversion should fail.
        URI uri = URI.create("/relative/path");
        try {
            UriConversions.toURL(uri, dummyConverter());
            fail("Expected IllegalArgumentException for relative URI");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(uri.toString()));
        }
    }

    @Test
    public void testToURLWithEmptyStringUri() {
        // An empty URI cannot be turned into a URL.
        URI uri = URI.create("");
        try {
            UriConversions.toURL(uri, dummyConverter());
            fail("Expected IllegalArgumentException for empty URI");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unable to convert URI to URL"));
        }
    }

    @Test
    public void testToURLIgnoresConverterParameter() throws Exception {
        // Pass a non‑null converter to prove it is not used.
        Converter converter = dummyConverter();
        URI uri = new URI("http://example.org");
        URL url = UriConversions.toURL(uri, converter);
        assertEquals(uri.toURL(), url);
    }

    @Test
    public void testToMapProducesCompactLinkedMapImplementation() {
        URI uri = URI.create("http://example.com");
        Map<String, Object> map = UriConversions.toMap(uri, dummyConverter());

        // The concrete type should be CompactLinkedMap.
        assertTrue("Map implementation should be CompactLinkedMap",
                map instanceof CompactLinkedMap);
    }
}
