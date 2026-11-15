package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link UrlConversions}. The tests cover the public static methods
 * {@code toMap} and {@code toURI} and exercise normal, edge‑case and error paths.
 *
 * The {@link Converter} instance is required by the method signatures but
 * is not used by the implementation; therefore a {@code null} or a
 * {@code Converter} constructed with {@code null} options is sufficient.
 */
public class UrlConversionsTest {

    /** Helper to create a dummy {@link Converter} – the implementation does not use it. */
    private static Converter dummyConverter() {
        // The Converter constructor requires a ConverterOptions instance.
        // Passing {@code null} is acceptable for the purposes of these tests.
        return new Converter(null);
    }

    /* -------------------------------------------------------------
     * Tests for UrlConversions.toMap(Object, Converter)
     * ------------------------------------------------------------- */

    @Test
    public void toMap_returnsMapWithUrlString() throws MalformedURLException {
        URL url = new URL("http://example.com/path?query=1");
        Map<String, Object> result = UrlConversions.toMap(url, dummyConverter());

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the URL_KEY", result.containsKey(URL_KEY));
        assertEquals("Map value must be the URL string", url.toString(), result.get(URL_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void toMap_nullInput_throwsNullPointerException() {
        // The implementation casts the argument to URL without a null check.
        UrlConversions.toMap(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toMap_wrongType_throwsClassCastException() {
        // Passing an object that is not a URL triggers a ClassCastException.
        UrlConversions.toMap("not a URL", dummyConverter());
    }

    /* -------------------------------------------------------------
     * Tests for UrlConversions.toURI(Object, Converter)
     * ------------------------------------------------------------- */

    @Test
    public void toURI_normalUrl_convertsSuccessfully() throws Exception {
        URL url = new URL("https://www.example.org/resource?param=value#fragment");
        URI uri = UrlConversions.toURI(url, dummyConverter());

        // The returned URI must be equal to the one produced by URL.toURI().
        assertEquals("URI must match URL.toURI()", url.toURI(), uri);
    }

    @Test
    public void toURI_urlWithIllegalUriChar_throwsIllegalArgumentException() throws Exception {
        // The pipe character '|' is accepted by java.net.URL but not by java.net.URI.
        URL url = new URL("http://example.com/illegal|char");

        try {
            UrlConversions.toURI(url, dummyConverter());
            fail("Expected IllegalArgumentException because URL.toURI() should fail");
        } catch (IllegalArgumentException ex) {
            // Verify that the cause is the original exception from URL.toURI()
            assertNotNull("Cause must be present", ex.getCause());
            assertTrue("Cause should be URISyntaxException",
                    ex.getCause().getClass().getSimpleName().equals("URISyntaxException"));
            assertTrue("Exception message must contain the original URL",
                    ex.getMessage().contains(url.toString()));
        }
    }

    @Test(expected = NullPointerException.class)
    public void toURI_nullInput_throwsNullPointerException() {
        // As with toMap, a null argument leads to a NullPointerException when cast.
        UrlConversions.toURI(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toURI_wrongType_throwsClassCastException() {
        // Passing a non‑URL object results in a ClassCastException.
        UrlConversions.toURI(12345, dummyConverter());
    }

    /* -------------------------------------------------------------
     * Additional sanity checks – ensure that the Converter argument
     * is truly irrelevant to the conversion logic.
     * ------------------------------------------------------------- */

    @Test
    public void toMap_converterCanBeNull_andStillWorks() throws MalformedURLException {
        URL url = new URL("http://null.converter.test/");
        Map<String, Object> result = UrlConversions.toMap(url, null);

        assertEquals(url.toString(), result.get(URL_KEY));
    }

    @Test
    public void toURI_converterCanBeNull_andStillWorks() throws Exception {
        URL url = new URL("http://null.converter.test/");
        URI uri = UrlConversions.toURI(url, null);

        assertEquals(url.toURI(), uri);
    }
}
