package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * Unit tests for {@link UrlConversions}.
 */
public class UrlConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter instance is not used by the methods under test,
        // but a non‑null instance satisfies the method signatures.
        converter = new Converter(null);
    }

    /***  BASIC FUNCTIONALITY  ***/

    @Test
    public void testToMapBasic() throws Exception {
        URL url = new URL("http://example.com/path?query=1");
        Map<String, Object> map = UrlConversions.toMap(url, converter);
        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map must contain exactly one entry", 1, map.size());
        assertEquals("Map must contain the URL string under URL_KEY",
                     url.toString(), map.get(URL_KEY));
    }

    @Test
    public void testToURIBasic() throws Exception {
        URL url = new URL("https://example.org/resource");
        URI uri = UrlConversions.toURI(url, converter);
        assertNotNull("Resulting URI must not be null", uri);
        assertEquals("URI must be equal to URL.toURI()", url.toURI(), uri);
    }

    /***  EDGE‑CASE & EXCEPTION TESTS  ***/

    @Test(expected = NullPointerException.class)
    public void testToMapNull() {
        UrlConversions.toMap(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToURINull() {
        UrlConversions.toURI(null, converter);
    }

    @Test
    public void testToURIWithIllegalCharacters() throws Exception {
        // ‘|’ is illegal in a URI; URL.toURI() will raise URISyntaxException,
        // which UrlConversions.toURI should wrap in IllegalArgumentException.
        URL badUrl = new URL("http://example.com/invalid|uri");
        try {
            UrlConversions.toURI(badUrl, converter);
            fail("Expected IllegalArgumentException because URL cannot be converted to URI");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message must mention conversion failure",
                       e.getMessage().contains("Unable to convert URL to URI"));
            assertNotNull("Cause of IllegalArgumentException must be non‑null", e.getCause());
        }
    }
}
