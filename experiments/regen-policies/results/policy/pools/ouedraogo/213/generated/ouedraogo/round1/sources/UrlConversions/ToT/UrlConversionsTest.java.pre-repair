package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;

/**
 * JUnit‑4 test suite for {@link UrlConversions}.
 *
 * The suite covers:
 * <ul>
 *   <li>Typical successful conversions for {@code toMap} and {@code toURI}.</li>
 *   <li>Edge‑case URLs (query strings, fragments, special characters).</li>
 *   <li>Null input handling (should result in {@link NullPointerException}).</li>
 *   <li>Error handling when {@code URL.toURI()} throws {@link java.net.URISyntaxException}
 *       – the method must wrap it in an {@link IllegalArgumentException}.</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore {@code null}
 * is supplied in all tests (the API permits it).
 */
public class UrlConversionsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ---------------------------------------------------------------------- */
    /*  toMap() tests                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toMap_basicUrl() throws Exception {
        URL url = new URL("http://example.com");
        Map<String, Object> map = UrlConversions.toMap(url, null);

        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map must contain exactly one entry", 1, map.size());
        assertTrue("Map must contain the URL_KEY", map.containsKey(URL_KEY));
        assertEquals("URL string must be stored correctly", url.toString(),
                     map.get(URL_KEY));
    }

    @Test
    public void toMap_urlWithQueryAndFragment() throws Exception {
        URL url = new URL("https://example.org/path?param=value#section");
        Map<String, Object> map = UrlConversions.toMap(url, null);

        assertEquals(url.toString(), map.get(URL_KEY));
    }

    @Test
    public void toMap_nullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        UrlConversions.toMap(null, null);
    }

    @Test
    public void toMap_wrongTypeThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        // Pass an object that is not a URL – the implementation casts without checking.
        UrlConversions.toMap("not a URL", null);
    }

    /* ---------------------------------------------------------------------- */
    /*  toURI() tests                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toURI_basicUrl() throws Exception {
        URL url = new URL("http://example.com");
        URI uri = UrlConversions.toURI(url, null);

        assertEquals(url.toURI(), uri);
    }

    @Test
    public void toURI_urlWithSpecialCharacters() throws Exception {
        // The percent‑encoded pipe character is legal for URL but illegal for URI,
        // causing URL.toURI() to throw URISyntaxException which must be wrapped.
        URL url = new URL("http://example.com/pipe%7Cchar");
        // This URL is syntactically valid; the conversion to URI succeeds.
        URI expected = new URI("http://example.com/pipe%7Cchar");
        assertEquals(expected, UrlConversions.toURI(url, null));
    }

    @Test
    public void toURI_urlThatTriggersURISyntaxException_isWrapped() throws Exception {
        // Construct a URL that is accepted by java.net.URL but will cause
        // URL.toURI() to throw URISyntaxException (e.g., an unescaped pipe character).
        URL url = new URL("http://example.com/pipe|char");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unable to convert URL to URI");
        thrown.expectMessage(url.toString());

        UrlConversions.toURI(url, null);
    }

    @Test
    public void toURI_nullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        UrlConversions.toURI(null, null);
    }

    @Test
    public void toURI_wrongTypeThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        UrlConversions.toURI(12345, null);
    }
}
