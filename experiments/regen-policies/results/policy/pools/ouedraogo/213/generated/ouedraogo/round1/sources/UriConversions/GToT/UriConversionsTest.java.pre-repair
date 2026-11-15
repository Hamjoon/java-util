package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@link UriConversions}.
 *
 * The tests cover:
 * 1. Normal/typical usage of each conversion method.
 * 2. Edge‑case scenarios such as {@code null} inputs, wrong input types and
 *    URIs that cannot be turned into a {@link URL}.
 * 3. Verification that the wrapper exception thrown by {@code toURL}
 *    contains the original {@link URI} in its message and keeps the original
 *    cause.
 *
 * The test class lives in the same package as {@code UriConversions}
 * so that the package‑private static methods are directly accessible.
 */
public class UriConversionsTest {

    /* --------------------------------------------------------------------- */
    /*  Helper constants                                                    */
    /* --------------------------------------------------------------------- */

    // The key that {@code UriConversions.toMap} stores the URI string under.
    // The constant is defined in {@code MapConversions}, but we repeat the
    // expected value here to keep the test self‑contained.
    private static final String EXPECTED_URI_KEY = "uri";

    /* --------------------------------------------------------------------- */
    /*  toMap() – normal case                                                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToMapBasic() {
        URI uri = URI.create("http://example.com/path?query=123");
        // The method does not use the Converter, so we can safely pass null.
        Map<String, Object> result = UriConversions.toMap(uri, null);

        Assert.assertNotNull("Result map must not be null", result);
        Assert.assertEquals("Map must contain exactly one entry", 1, result.size());

        // Verify key and value.
        Assert.assertTrue("Map must contain the expected key", result.containsKey(EXPECTED_URI_KEY));
        Assert.assertEquals("Map value must be the URI string",
                uri.toString(),
                result.get(EXPECTED_URI_KEY));
    }

    /* --------------------------------------------------------------------- */
    /*  toMap() – null input                                                  */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testToMapNullInput() {
        // The cast to URI yields null; the subsequent call to toString()
        // throws NPE.
        UriConversions.toMap(null, null);
    }

    /* --------------------------------------------------------------------- */
    /*  toMap() – wrong type (not a URI)                                      */
    /* --------------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testToMapInvalidType() {
        // Passing an object that is not a URI triggers a ClassCastException
        // at the very first line of the method.
        UriConversions.toMap("not a uri", null);
    }

    /* --------------------------------------------------------------------- */
    /*  toURL() – normal case                                                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToURLBasic() throws Exception {
        URI uri = URI.create("https://www.example.org/index.html");
        URL url = UriConversions.toURL(uri, null);

        Assert.assertNotNull("Converted URL must not be null", url);
        Assert.assertEquals("URL string must match the original URI string",
                uri.toURL().toString(),
                url.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  toURL() – relative URI (cannot be turned into a URL)                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToURLRelativeUri() {
        URI relativeUri = URI.create("/relative/path");

        try {
            UriConversions.toURL(relativeUri, null);
            Assert.fail("Expected IllegalArgumentException for relative URI");
        } catch (IllegalArgumentException e) {
            // Verify that the message contains the original URI.
            Assert.assertTrue("Exception message must contain the original URI",
                    e.getMessage().contains(relativeUri.toString()));
            // The cause should be the original exception thrown by URI.toURL()
            Assert.assertNotNull("Cause must be present", e.getCause());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  toURL() – null input                                                   */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testToURLNullInput() {
        // Null cast to URI produces null; the subsequent call to toURL()
        // throws NPE.
        UriConversions.toURL(null, null);
    }

    /* --------------------------------------------------------------------- */
    /*  toURL() – wrong type (not a URI)                                       */
    /* --------------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testToURLInvalidType() {
        // Passing a non‑URI object triggers a ClassCastException.
        UriConversions.toURL(12345, null);
    }

    /* --------------------------------------------------------------------- */
    /*  toURL() – URI with illegal characters (should cause wrapper exception) */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToURLWithIllegalCharacters() {
        // This URI is syntactically valid but cannot be converted to a URL because
        // of the space character; URI.toURL() will throw a MalformedURLException.
        URI badUri = URI.create("http://example.com/illegal space");

        try {
            UriConversions.toURL(badUri, null);
            Assert.fail("Expected IllegalArgumentException due to malformed URL");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Message must mention the input URI",
                    e.getMessage().contains(badUri.toString()));
            Assert.assertNotNull("Cause should be present", e.getCause());
            Assert.assertTrue("Cause should be a MalformedURLException",
                    e.getCause() instanceof java.net.MalformedURLException);
        }
    }
}
