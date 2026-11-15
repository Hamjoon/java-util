package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link UriConversions}.
 *
 * The tests cover normal operation as well as edge‑cases such as
 * null inputs and URIs that cannot be turned into a {@link URL}.
 *
 * The {@code Converter} argument is not used by the methods under test,
 * therefore a {@code null} instance is supplied.
 */
public class UriConversionsTest {

    // ----------------------------------------------------------------------
    // Helper / fixture
    // ----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null; // not used by UriConversions

    // ----------------------------------------------------------------------
    // 1.  Tests for toMap(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToMapCreatesCorrectMap() throws Exception {
        // Arrange
        URI uri = new URI("https://example.com/path?query=1");

        // Act
        Map result = UriConversions.toMap(uri, DUMMY_CONVERTER);

        // Assert
        assertNotNull("Resulting map must not be null", result);
        assertTrue("Result must be a CompactLinkedMap", result instanceof CompactLinkedMap);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the URI key", result.containsKey(URI_KEY));
        assertEquals("Stored value must be the string representation of the URI",
                uri.toString(), result.get(URI_KEY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToMapWithNullInputThrowsIllegalArgumentException() {
        // Passing null will cause a NullPointerException when uri.toString() is called,
        // which is caught by the test as an IllegalArgumentException (wrapped by the method).
        UriConversions.toMap(null, DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // 2.  Tests for toURL(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToURLConvertsSuccessfully() throws Exception {
        // Arrange
        URI uri = new URI("http://www.example.org/index.html");

        // Act
        URL url = UriConversions.toURL(uri, DUMMY_CONVERTER);

        // Assert
        assertNotNull("URL must not be null", url);
        assertEquals("URL must match the original URI", uri.toURL(), url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURLWithNonHierarchicalURIThrowsIllegalArgumentException() throws Exception {
        // URI with a scheme that does not map to a hierarchical URL (e.g., mailto)
        URI uri = new URI("mailto:user@example.com");
        UriConversions.toURL(uri, DUMMY_CONVERTER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURLWithNullInputThrowsIllegalArgumentException() {
        // Null input leads to a NullPointerException inside the try‑block,
        // which is wrapped as IllegalArgumentException by the method.
        UriConversions.toURL(null, DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // 3.  Additional edge‑case: malformed URI that still parses but cannot become a URL
    // ----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testToURLWithInvalidAuthorityThrowsIllegalArgumentException() throws Exception {
        // URI with an illegal authority component (e.g., missing host)
        URI uri = new URI("http://");
        UriConversions.toURL(uri, DUMMY_CONVERTER);
    }
}
