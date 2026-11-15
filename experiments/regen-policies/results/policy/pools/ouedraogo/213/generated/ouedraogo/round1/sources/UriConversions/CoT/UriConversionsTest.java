package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

public class UriConversionsTest {

    // -----------------------------------------------------------------
    // Helper – we don't need a real Converter because the class under test
    // ignores the argument.  Passing null keeps the test simple.
    // -----------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null;

    // -----------------------------------------------------------------
    // 1. Basic functionality tests
    // -----------------------------------------------------------------
    @Test
    public void testToMapWithValidUri() {
        URI uri = URI.create("http://example.com/path?query=1");
        Map result = UriConversions.toMap(uri, DUMMY_CONVERTER);

        assertNotNull("Result map must not be null", result);
        assertTrue("Result must contain the URI key", result.containsKey(URI_KEY));
        assertEquals("Map value must be the string representation of the URI",
                uri.toString(), result.get(URI_KEY));
    }

    @Test
    public void testToURLWithValidHierarchicalUri() throws Exception {
        URI uri = URI.create("https://www.example.org/resource.txt");
        URL url = UriConversions.toURL(uri, DUMMY_CONVERTER);

        assertNotNull("Returned URL must not be null", url);
        assertEquals("URL should match the original URI", uri.toURL(), url);
    }

    // -----------------------------------------------------------------
    // 2. Edge‑case / exception tests for toMap
    // -----------------------------------------------------------------
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testToMapWithNull() {
        thrown.expect(NullPointerException.class);
        UriConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test
    public void testToMapWithNonUriObject() {
        thrown.expect(ClassCastException.class);
        UriConversions.toMap("not a uri", DUMMY_CONVERTER);
    }

    @Test
    public void testToMapWithUriContainingSpecialChars() {
        URI uri = URI.create("http://example.com/space%20here?param=äöü");
        Map result = UriConversions.toMap(uri, DUMMY_CONVERTER);

        assertEquals(uri.toString(), result.get(URI_KEY));
    }

    // -----------------------------------------------------------------
    // 3. Edge‑case / exception tests for toURL
    // -----------------------------------------------------------------
    @Test
    public void testToURLWithNull() {
        thrown.expect(NullPointerException.class);
        UriConversions.toURL(null, DUMMY_CONVERTER);
    }

    @Test
    public void testToURLWithNonUriObject() {
        thrown.expect(ClassCastException.class);
        UriConversions.toURL(12345, DUMMY_CONVERTER);
    }

    @Test
    public void testToURLWithUnconvertibleUri() {
        // mailto: scheme cannot be turned into a URL via URI.toURL()
        URI uri = URI.create("mailto:john@example.com");

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unable to convert URI to URL");
        UriConversions.toURL(uri, DUMMY_CONVERTER);
    }

    @Test
    public void testToURLWithOpaqueUriThatIsConvertible() throws Exception {
        // An opaque URI with a scheme that Java knows how to turn into a URL, e.g., "file"
        URI uri = URI.create("file:/tmp/test.txt");
        URL url = UriConversions.toURL(uri, DUMMY_CONVERTER);
        assertEquals(uri.toURL(), url);
    }
}
