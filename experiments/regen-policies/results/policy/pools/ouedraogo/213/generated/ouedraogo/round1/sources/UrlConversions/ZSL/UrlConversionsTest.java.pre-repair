package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static org.junit.Assert.*;

public class UrlConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is not needed for the tests; passing null is acceptable for the constructor signature.
        this.converter = new Converter(null);
    }

    /**
     * Verify that {@code UrlConversions.toMap} returns a map that contains a single entry whose
     * key is {@link MapConversions#URL_KEY} and whose value is the string representation of the URL.
     */
    @Test
    public void testToMapProducesCorrectEntry() throws Exception {
        URL url = new URL("http://example.com/path?query=123#fragment");
        Map<String, Object> result = UrlConversions.toMap(url, converter);

        // The map should not be null and must contain exactly one entry.
        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // Verify the key and value.
        assertTrue("Map must contain the URL_KEY", result.containsKey(URL_KEY));
        assertEquals("Value must be URL's string representation",
                url.toString(),
                result.get(URL_KEY));
    }

    /**
     * Verify that {@code UrlConversions.toURI} correctly converts a well‑formed {@link URL}
     * into an equivalent {@link URI}.
     */
    @Test
    public void testToURISuccessfulConversion() throws Exception {
        URL url = new URL("https://www.cedarsoftware.com/test?param=value#section");
        URI expected = new URI("https://www.cedarsoftware.com/test?param=value#section");

        URI actual = UrlConversions.toURI(url, converter);
        assertNotNull("Converted URI must not be null", actual);
        assertEquals("Converted URI must match expected", expected, actual);
    }

    /**
     * Force {@code UrlConversions.toURI} to encounter an exception from {@link URL#toURI()}
     * and verify that it wraps the exception in an {@link IllegalArgumentException}
     * with the expected message.
     */
    @Test
    public void testToURIExceptionWrappedInIllegalArgumentException() throws Exception {
        // Create a URL subclass that deliberately throws a URISyntaxException when toURI() is called.
        URL problematicUrl = new URL("http://example.com") {
            @Override
            public URI toURI() throws URISyntaxException {
                throw new URISyntaxException("bad", "forced failure for testing");
            }
        };

        try {
            UrlConversions.toURI(problematicUrl, converter);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            // Verify that the exception message contains the original URL string.
            assertTrue("Exception message should contain the URL string",
                    e.getMessage().contains(problematicUrl.toString()));
            // The cause should be a URISyntaxException.
            assertTrue("Cause should be URISyntaxException",
                    e.getCause() instanceof URISyntaxException);
        }
    }

    /**
     * Ensure that {@code UrlConversions.toMap} does not retain a reference to the original URL
     * object (i.e., modifications to the original URL after conversion do not affect the map).
     * Since URL is immutable, we simulate the scenario by converting a different URL instance
     * with the same string value and confirming the map still contains the original string.
     */
    @Test
    public void testToMapIsIndependentOfOriginalUrlInstance() throws Exception {
        URL original = new URL("http://independent.com");
        Map<String, Object> map = UrlConversions.toMap(original, converter);

        // Create a new URL with the same string; the map must still hold the original string value.
        URL another = new URL("http://different.com");
        assertNotEquals("Different URL must have a different string representation",
                original.toString(),
                another.toString());

        // The map should still contain the string from the original URL, not be affected by the new instance.
        assertEquals("Map must still contain the original URL string",
                original.toString(),
                map.get(URL_KEY));
    }
}
