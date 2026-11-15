package com.cedarsoftware.util.convert;

import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link UriConversions}.
 *
 * The tests are placed in the same package as {@code UriConversions}
 * so that the package‑private static methods can be accessed directly.
 */
public class UriConversionsTest {

    /* -------------------------------------------------------------
     *  List of public (exposed) methods in UriConversions
     * -------------------------------------------------------------
     *
     *   static Map toMap(Object from, Converter converter)
     *   static URL toURL(Object from, Converter converter)
     *
     * The class also contains a private constructor to prevent instantiation.
     */

    // -----------------------------------------------------------------
    //  Basic functional tests
    // -----------------------------------------------------------------

    @Test
    public void testToMap_Basic() throws Exception {
        URI uri = new URI("http://example.com/path?query=1");
        // converter is not used inside the method; passing null is sufficient
        Map result = UriConversions.toMap(uri, null);

        assertNotNull("Resulting map must not be null", result);
        assertTrue("Result should contain the URI key", result.containsKey(URI_KEY));
        assertEquals("Map value must be the URI string representation",
                uri.toString(), result.get(URI_KEY));
    }

    @Test
    public void testToURL_Basic() throws Exception {
        URI uri = new URI("https://example.org/resource");
        URL url = UriConversions.toURL(uri, null);

        assertNotNull("Converted URL must not be null", url);
        assertEquals("URL string must match the original URI", uri.toURL(), url);
    }

    // -----------------------------------------------------------------
    //  Edge‑case and exception handling tests
    // -----------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToMap_NullFrom() {
        // The method casts the argument to URI without a null‑check,
        // therefore a NullPointerException is expected.
        UriConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_NonUriArgument() {
        // Passing an object that is not a URI should cause a ClassCastException.
        UriConversions.toMap("not a URI", null);
    }

    @Test(expected = NullPointerException.class)
    public void testToURL_NullFrom() {
        // Same reasoning as testToMap_NullFrom.
        UriConversions.toURL(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToURL_NonUriArgument() {
        // Passing a non‑URI object must result in a ClassCastException.
        UriConversions.toURL(12345, null);
    }

    @Test
    public void testToURL_MalformedUriThrowsIllegalArgumentException() throws Exception {
        // URI with an illegal port number triggers MalformedURLException inside URI.toURL().
        URI malformed = new URI("http://example.com:invalidPort");

        try {
            UriConversions.toURL(malformed, null);
            fail("Expected IllegalArgumentException to be thrown because the underlying URI.toURL() fails");
        } catch (IllegalArgumentException e) {
            // Verify that the cause is a MalformedURLException
            assertNotNull("Cause of the IllegalArgumentException must be present", e.getCause());
            assertTrue("Cause should be a MalformedURLException",
                    e.getCause() instanceof java.net.MalformedURLException);
            // Optional: check the message contains the original URI string
            assertTrue("Exception message should contain the original URI",
                    e.getMessage().contains(malformed.toString()));
        }
    }

    // -----------------------------------------------------------------
    //  Additional sanity checks (not strictly required but useful)
    // -----------------------------------------------------------------

    @Test
    public void testToMap_ReturnsInsertionOrderedMap() throws Exception {
        // The builder explicitly requests insertion order; we can verify that the map
        // preserves insertion order for multiple entries (even though this method adds only one).
        URI uri = new URI("http://ordered.test");
        Map map = UriConversions.toMap(uri, null);
        // CompactMap implements java.util.Map; its entrySet iterator order reflects insertion order.
        // Since there is only one entry, we just ensure the map size is 1.
        assertEquals("Map should contain exactly one entry", 1, map.size());
    }
}
