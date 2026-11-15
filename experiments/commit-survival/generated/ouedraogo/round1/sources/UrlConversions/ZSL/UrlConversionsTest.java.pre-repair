package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link UrlConversions}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Correct conversion of a {@link URL} to a {@link Map} representation.</li>
 *   <li>The map contains the expected key ({@code URL_KEY}) and the string value of the URL.</li>
 *   <li>The concrete map implementation returned is {@link CompactLinkedMap}.</li>
 *   <li>Successful conversion of a {@link URL} to a {@link URI}.</li>
 *   <li>Proper handling of error conditions – a {@code null} URL causes an {@link IllegalArgumentException}
 *       with the original exception wrapped.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The {@link Converter} argument required by the static methods is not used by the implementation.
 * Consequently a minimal instance created with a {@code null} {@code ConverterOptions} is sufficient.
 * </p>
 */
public class UrlConversionsTest {

    /**
     * Helper that creates a {@link Converter} instance using a {@code null}
     * {@code ConverterOptions}. The constructor of {@code Converter} accepts
     * {@code null} because the options are optional for the methods under test.
     */
    private static Converter dummyConverter() {
        // The library permits a null options argument – the conversion logic does not depend on it.
        return new Converter(null);
    }

    @Test
    public void testToMapProducesCompactLinkedMap() throws Exception {
        URL url = new URL("http://example.com/path?query=123#frag");
        Map<String, Object> result = UrlConversions.toMap(url, dummyConverter());

        // The concrete type must be CompactLinkedMap
        assertTrue("Result map should be an instance of CompactLinkedMap",
                result instanceof CompactLinkedMap);

        // The map must contain exactly one entry with the correct key and value
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the URL_KEY", result.containsKey(URL_KEY));
        assertEquals("Stored value must be the URL's string representation",
                url.toString(), result.get(URL_KEY));
    }

    @Test
    public void testToMapHandlesVariousUrlForms() throws Exception {
        // A collection of URLs covering different schemes and edge‑cases
        String[] urlStrings = {
                "http://localhost/",
                "https://user:pass@example.com:8443/secure?param=value#section",
                "file:/tmp/test.txt",
                "ftp://ftp.example.org/resource",
                "mailto:someone@example.com"
        };

        for (String s : urlStrings) {
            URL url = new URL(s);
            Map<String, Object> map = UrlConversions.toMap(url, dummyConverter());

            assertEquals("Map must contain the URL string for " + s,
                    url.toString(), map.get(URL_KEY));
        }
    }

    @Test
    public void testToMapIsIndependentOfOriginalUrl() throws Exception {
        URL original = new URL("http://example.org/initial");
        Map<String, Object> map = UrlConversions.toMap(original, dummyConverter());

        // Changing the reference does not affect the map (the map stores a String copy)
        original = new URL("http://example.org/changed");
        assertEquals("Map value must remain the original URL string",
                "http://example.org/initial", map.get(URL_KEY));
    }

    @Test
    public void testToURIConvertsSuccessfully() throws Exception {
        URL url = new URL("https://example.com:8080/path/to/resource?query=abc#anchor");
        URI uri = UrlConversions.toURI(url, dummyConverter());

        // The URI should be equal to the one produced directly from the URL
        assertEquals("URI produced by UrlConversions must match URL.toURI()", url.toURI(), uri);
    }

    @Test
    public void testToURIThrowsIllegalArgumentExceptionWhenUrlIsNull() {
        try {
            UrlConversions.toURI(null, dummyConverter());
            fail("Expected IllegalArgumentException when converting a null URL");
        } catch (IllegalArgumentException e) {
            // The cause should be a NullPointerException (the original exception)
            assertTrue("Cause of IllegalArgumentException should be NullPointerException",
                    e.getCause() instanceof NullPointerException);
            assertTrue("Exception message should contain the word 'URL'",
                    e.getMessage().contains("URL"));
        }
    }
}
