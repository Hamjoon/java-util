/***************************************************************************************************
 * JUnit 4 test suite for com.cedarsoftware.util.convert.UriConversions
 *
 * The tests cover:
 *   1. Extraction of public (package‑private static) methods and their signatures.
 *   2. Basic functionality of each method.
 *   3. Edge‑case handling such as null inputs, unsupported URI schemes and malformed URIs.
 *
 * The test class is placed in the same package as UriConversions so it can access the
 * package‑private static methods directly.
 **************************************************************************************************/

package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * Test class for {@link UriConversions}.
 *
 * Note: The {@code Converter} argument is not used by the current implementation,
 * therefore we can safely pass {@code null} in all test cases.
 */
public class UriConversionsTest {

    /* ----------------------------------------------------------------------
     *  Public (package‑private) method signatures extracted from UriConversions
     * ---------------------------------------------------------------------- */
    // static Map toMap(Object from, Converter converter)
    // static URL toURL(Object from, Converter converter)

    /* ----------------------------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMapBasic() throws Exception {
        // Arrange
        URI uri = new URI("http://example.com/path?query=123");
        // Act
        Map result = UriConversions.toMap(uri, null);
        // Assert
        Assert.assertNotNull("Resulting map must not be null", result);
        Assert.assertEquals("Map should contain exactly one entry", 1, result.size());
        Assert.assertTrue("Map must contain the URI_KEY", result.containsKey(URI_KEY));
        Assert.assertEquals("Stored URI string must match the original", uri.toString(),
                result.get(URI_KEY));
    }

    @Test
    public void testToURLBasic() throws Exception {
        // Arrange
        URI uri = new URI("https://www.example.org/resource");
        // Act
        URL url = UriConversions.toURL(uri, null);
        // Assert
        Assert.assertNotNull("Resulting URL must not be null", url);
        Assert.assertEquals("URL string must match the original URI string",
                uri.toString(), url.toString());
    }

    /* ----------------------------------------------------------------------
     *  Edge‑case / exception handling tests
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToMapNullInput() {
        // Passing null should cause a NullPointerException when casting to URI
        UriConversions.toMap(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToURLNullInput() {
        // Passing null should cause a NullPointerException when casting to URI
        UriConversions.toURL(null, null);
    }

    @Test
    public void testToURLUnsupportedScheme() throws Exception {
        // URI with a scheme that cannot be converted to a URL (e.g., "mailto")
        URI mailto = new URI("mailto:user@example.com");
        try {
            UriConversions.toURL(mailto, null);
            Assert.fail("Expected IllegalArgumentException for unsupported scheme");
        } catch (IllegalArgumentException e) {
            // Verify that the cause is the original exception from URI.toURL()
            Assert.assertTrue("Cause should be an exception from URI.toURL()", e.getCause() != null);
            Assert.assertTrue("Message should contain the original URI string",
                    e.getMessage().contains(mailto.toString()));
        }
    }

    @Test
    public void testToURLMalformedURI() throws Exception {
        // A syntactically malformed URI that still constructs but fails when converting to URL
        URI malformed = new URI("http://[invalid-host]");
        try {
            UriConversions.toURL(malformed, null);
            Assert.fail("Expected IllegalArgumentException for malformed URI");
        } catch (IllegalArgumentException e) {
            // Ensure the exception message references the original URI
            Assert.assertTrue(e.getMessage().contains(malformed.toString()));
        }
    }

    @Test
    public void testToMapWithUnicodeAndSpaces() throws Exception {
        // URI containing spaces (which are encoded) and Unicode characters
        URI complex = new URI("http://example.com/pa th/åß∂");
        Map result = UriConversions.toMap(complex, null);
        Assert.assertEquals("Map must contain exactly one entry", 1, result.size());
        Assert.assertEquals("Stored string must be the encoded URI string",
                complex.toString(), result.get(URI_KEY));
    }

    @Test
    public void testToURLWithFragment() throws Exception {
        // URI that includes a fragment component – fragments are ignored by URL conversion
        URI uriWithFragment = new URI("http://example.com/page#section");
        URL url = UriConversions.toURL(uriWithFragment, null);
        Assert.assertEquals("URL should contain the same string representation as the URI",
                uriWithFragment.toString(), url.toString());
    }
}
