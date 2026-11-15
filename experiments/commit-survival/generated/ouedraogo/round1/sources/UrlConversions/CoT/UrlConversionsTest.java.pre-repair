package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.*;

public class UrlConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter constructor accepts a ConverterOptions instance; passing null is allowed for these tests
        converter = new Converter(null);
    }

    /* -------------------- Basic functionality tests -------------------- */

    @Test
    public void testToMapBasic() throws Exception {
        URL url = new URL("http://example.com/path?query=1");
        Map<String, Object> map = UrlConversions.toMap(url, converter);

        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map must contain exactly one entry", 1, map.size());

        // The key used by UrlConversions is defined in MapConversions.URL_KEY.
        // We do not rely on the exact key name; we just verify the stored value.
        Object storedValue = map.values().iterator().next();
        assertEquals("Stored value must be the URL string", url.toString(), storedValue);
    }

    @Test
    public void testToURIBasic() throws Exception {
        URL url = new URL("http://example.com/simple/path");
        URI uri = UrlConversions.toURI(url, converter);

        assertNotNull("Converted URI must not be null", uri);
        assertEquals("URI must be equal to URL.toURI()", url.toURI(), uri);
    }

    /* -------------------- Edge‑case / exception tests -------------------- */

    // ----- toMap edge cases -----

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        UrlConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_InvalidType() {
        UrlConversions.toMap("not a URL object", converter);
    }

    // ----- toURI edge cases -----

    @Test(expected = NullPointerException.class)
    public void testToURI_NullInput() {
        UrlConversions.toURI(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToURI_InvalidType() {
        UrlConversions.toURI(12345, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURI_IllegalCharacters() throws Exception {
        // URL with an illegal space character – URL constructor accepts it, but toURI() will fail.
        URL url = new URL("http://example.com/space in path");
        UrlConversions.toURI(url, converter);
    }
}
