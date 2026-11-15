/****************************************************************************************
 *  JUnit‑4 test suite for com.cedarsoftware.util.convert.UrlConversions
 *
 *  The tests cover both public static methods:
 *   • toMap(Object from, Converter converter)
 *   • toURI(Object from, Converter converter)
 *
 *  The test class is placed in the same package as UrlConversions so that the
 *  package‑private class is accessible.  Only APIs that appear in the supplied
 *  source code or the listed dependency signatures are used – no additional
 *  libraries, mocking frameworks, or subclasses are introduced.
 *
 *  Test cases include:
 *   – Typical usage with a well‑formed URL.
 *   – Edge cases such as null arguments and wrong object types.
 *   – Error handling when URL.toURI() throws an exception (illegal characters).
 *
 *  JUnit‑4 annotations are used; assertions are from org.junit.Assert.
 ****************************************************************************************/
package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.net.URL;
import java.net.URI;
import java.util.Map;

/**
 * Test class for UrlConversions.
 */
public class UrlConversionsTest {

    // -------------------------------------------------------------------------
    // Helper – a dummy Converter instance (the methods under test ignore it)
    // -------------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    // -------------------------------------------------------------------------
    // 1. Tests for toMap(...)
    // -------------------------------------------------------------------------

    @Test
    public void testToMapCreatesMapWithUrlString() throws Exception {
        URL url = new URL("http://example.com/path?query=123");
        Map<String, Object> result = UrlConversions.toMap(url, DUMMY_CONVERTER);

        // The map must be non‑null and contain exactly one entry
        assertNotNull("Resulting map should not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // The key must be the constant from MapConversions and the value the URL string
        assertTrue("Map should contain the URL key", result.containsKey(MapConversions.URL_KEY));
        assertEquals("Map value should be the URL's string representation",
                url.toString(),
                result.get(MapConversions.URL_KEY));
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithWrongObjectTypeThrowsClassCastException() {
        // Pass an object that is not a URL – the method casts without checking.
        String notAUrl = "not a url";
        UrlConversions.toMap(notAUrl, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullArgumentThrowsNullPointerException() {
        // Null is cast to URL, which throws NPE before any logic runs.
        UrlConversions.toMap(null, DUMMY_CONVERTER);
    }

    // -------------------------------------------------------------------------
    // 2. Tests for toURI(...)
    // -------------------------------------------------------------------------

    @Test
    public void testToURIConvertsValidUrlToUri() throws Exception {
        URL url = new URL("https://www.example.org/resource#section");
        URI expected = url.toURI();                 // the JDK conversion we expect
        URI actual = UrlConversions.toURI(url, DUMMY_CONVERTER);

        assertEquals("Converted URI should match URL.toURI()", expected, actual);
    }

    @Test
    public void testToURIWithUrlContainingIllegalCharactersThrowsIllegalArgumentException() throws Exception {
        // URL with an embedded space – URL.toURI() throws URISyntaxException,
        // which the wrapper converts to IllegalArgumentException.
        URL url = new URL("http://example.com/space here");

        try {
            UrlConversions.toURI(url, DUMMY_CONVERTER);
            fail("Expected IllegalArgumentException due to illegal characters in URL");
        } catch (IllegalArgumentException e) {
            // Verify that the message contains the original URL for easier debugging
            assertTrue("Exception message should contain the URL string",
                    e.getMessage().contains(url.toString()));
            // The cause should be a URISyntaxException (checked via its class name)
            assertNotNull("Cause should be present", e.getCause());
            assertEquals("Cause should be URISyntaxException",
                    "java.net.URISyntaxException",
                    e.getCause().getClass().getName());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testToURIWithNullArgumentThrowsNullPointerException() {
        // Null cast to URL triggers NPE before the try/catch inside the method.
        UrlConversions.toURI(null, DUMMY_CONVERTER);
    }

    // -------------------------------------------------------------------------
    // 3. Additional sanity check – ensure the returned map is an instance of
    //    CompactMap (the builder used in the implementation).  This is not a
    //    functional requirement but validates the internal contract.
    // -------------------------------------------------------------------------
    @Test
    public void testToMapResultIsCompactMap() throws Exception {
        URL url = new URL("http://example.com");
        Map<String, Object> map = UrlConversions.toMap(url, DUMMY_CONVERTER);
        assertTrue("Resulting map should be an instance of CompactMap",
                map instanceof CompactMap);
    }
}
