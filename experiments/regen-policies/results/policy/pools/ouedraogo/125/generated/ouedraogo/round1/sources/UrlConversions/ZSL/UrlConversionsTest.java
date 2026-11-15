package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link UrlConversions}. All public static methods are exercised,
 * including normal operation and error handling.
 */
public class UrlConversionsTest {

    private Converter converter;   // not used by UrlConversions but required by signature

    @Before
    public void setUp() {
        // UrlConversions does not use the Converter instance, so a null options object is sufficient.
        this.converter = new Converter(null);
    }

    /**
     * Verify that {@code toMap} returns a {@link CompactMap} containing exactly one entry:
     * the {@code URL_KEY} mapped to the string representation of the supplied {@link URL}.
     */
    @Test
    public void testToMapReturnsExpectedMap() throws MalformedURLException {
        URL url = new URL("https://example.com/path?query=123#frag");

        Map<String, Object> result = UrlConversions.toMap(url, converter);

        // The returned map should be a CompactMap (the concrete class used by the implementation)
        assertTrue("Result should be an instance of CompactMap", result instanceof CompactMap);

        // It must contain exactly one entry
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // The key must be the constant URL_KEY and the value must be the URL's string form
        assertTrue("Map should contain the URL_KEY", result.containsKey(URL_KEY));
        assertEquals("Map value must match URL.toString()", url.toString(), result.get(URL_KEY));
    }

    /**
     * Verify that {@code toURI} correctly converts a well‑formed {@link URL} into a {@link URI}.
     */
    @Test
    public void testToURISuccessfulConversion() throws Exception {
        URL url = new URL("http://www.cedarsoftware.com/test/path?param=value#section");

        URI expected = url.toURI(); // the JDK's conversion – the method under test should produce the same result
        URI actual = UrlConversions.toURI(url, converter);

        assertEquals("Converted URI must be identical to URL.toURI()", expected, actual);
    }

    /**
     * Passing {@code null} as the source object should result in an {@link IllegalArgumentException}
     * with the original {@link NullPointerException} as its cause.
     */
    @Test
    public void testToURIThrowsIllegalArgumentExceptionWhenSourceIsNull() {
        try {
            UrlConversions.toURI(null, converter);
            fail("Expected IllegalArgumentException to be thrown for null source");
        } catch (IllegalArgumentException e) {
            // Verify that the cause is a NullPointerException
            assertNotNull("Cause should not be null", e.getCause());
            assertTrue("Cause should be NullPointerException", e.getCause() instanceof NullPointerException);
            // The message should contain the original URL (which is null) placeholder
            assertTrue("Exception message should mention URL conversion failure",
                    e.getMessage().contains("Unable to convert URL to URI"));
        }
    }

    /**
     * A URL that is syntactically valid for {@link URL} but invalid for {@link URI}
     * triggers the same wrapping behavior. The JDK accepts spaces in the path when they
     * are escaped as %20, but the {@code toURI()} method will reject an unescaped space.
     *
     * This test builds such a URL using the three‑argument constructor that does not
     * perform strict URI validation, then forces {@code toURI()} to throw.
     */
    @Test
    public void testToURIWrapsURISyntaxException() throws Exception {
        // Construct a URL that contains an unescaped space in the path.
        // The URL constructor will accept it because the space is encoded as %20 internally,
        // but calling toURI() will raise URISyntaxException.
        URL url = new URL("http", "example.com", "/path with space");

        try {
            UrlConversions.toURI(url, converter);
            fail("Expected IllegalArgumentException due to URISyntaxException");
        } catch (IllegalArgumentException e) {
            // The root cause should be a URISyntaxException
            assertNotNull("Cause should not be null", e.getCause());
            assertTrue("Cause should be URISyntaxException", e.getCause().getClass().getSimpleName().equals("URISyntaxException"));
            assertTrue("Exception message should contain original URL", e.getMessage().contains(url.toString()));
        }
    }
}
