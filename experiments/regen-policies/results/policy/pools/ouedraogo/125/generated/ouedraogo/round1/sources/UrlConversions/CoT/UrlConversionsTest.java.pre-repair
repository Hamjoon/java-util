package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@link UrlConversions}.
 *
 * The test class resides in the same package as {@code UrlConversions}
 * so that the package‑private static methods are directly accessible.
 *
 * The tests cover:
 *   • normal (happy‑path) behaviour of {@code toMap} and {@code toURI}
 *   • null arguments
 *   • arguments of the wrong type (ClassCastException)
 *   • a URL that cannot be turned into a {@code URI} (IllegalArgumentException wrapper)
 */
public class UrlConversionsTest {

    /**
     * Basic test for {@code toMap}.  The method should return a map that
     * contains a single entry whose key is {@code URL_KEY} and whose value
     * is the string representation of the supplied URL.
     */
    @Test
    public void testToMapBasic() throws Exception {
        URL url = new URL("https://example.com/path?query=1");
        // the Converter argument is not used by the method – passing null is safe
        Map<String, Object> result = UrlConversions.toMap(url, null);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the URL_KEY", result.containsKey(URL_KEY));
        assertEquals("Map value must be the URL string", url.toString(), result.get(URL_KEY));
    }

    /**
     * {@code toMap} should throw a {@link NullPointerException} when the
     * supplied object is {@code null}.  The method casts the argument to
     * {@code URL} and then dereferences it, which results in NPE.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapNullInput() {
        UrlConversions.toMap(null, null);
    }

    /**
     * Passing an object that is not a {@code URL} must result in a
     * {@link ClassCastException} because the implementation performs an
     * unchecked cast.
     */
    @Test(expected = ClassCastException.class)
    public void testToMapInvalidType() {
        UrlConversions.toMap("not a URL", null);
    }

    /**
     * Basic test for {@code toURI}.  The returned {@code URI} must be equal
     * to the result of {@code URL#toURI()} for a well‑formed URL.
     */
    @Test
    public void testToURIBasic() throws Exception {
        URL url = new URL("https://example.org/abc/def?x=1&y=2");
        URI expected = url.toURI();

        URI actual = UrlConversions.toURI(url, null);
        assertEquals("Converted URI must match URL#toURI()", expected, actual);
    }

    /**
     * Supplying {@code null} to {@code toURI} should cause a
     * {@link NullPointerException} (the method dereferences the casted URL).
     */
    @Test(expected = NullPointerException.class)
    public void testToURINullInput() {
        UrlConversions.toURI(null, null);
    }

    /**
     * Supplying an object that is not a {@code URL} must cause a
     * {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testToURIInvalidType() {
        UrlConversions.toURI(12345, null);
    }

    /**
     * A URL that cannot be converted to a {@code URI} (because it contains
     * characters illegal in a URI) must cause {@code UrlConversions.toURI}
     * to wrap the original exception in an {@link IllegalArgumentException}.
     *
     * The pipe character ('|') is permitted in a {@code URL} but not in a
     * {@code URI}, so {@code URL#toURI()} throws {@code URISyntaxException}.
     */
    @Test
    public void testToURIExceptionWrapping() throws Exception {
        URL problematicUrl = new URL("http://example.com/invalid|char");

        try {
            UrlConversions.toURI(problematicUrl, null);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            // Verify that the cause is the original exception from URL#toURI()
            assertNotNull("Cause should not be null", e.getCause());
            assertTrue("Cause should be URISyntaxException or subclass",
                    e.getCause() instanceof java.net.URISyntaxException);
            assertTrue("Exception message should contain the original URL",
                    e.getMessage().contains(problematicUrl.toString()));
        }
    }
}
