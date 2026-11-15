package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

/**
 * JUnit 4 tests for {@link UriConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct map creation and contents for {@code toMap}.</li>
 *   <li>Insertion‑order behavior of the returned {@link CompactMap}.</li>
 *   <li>Successful conversion of a well‑formed {@link URI} to {@link URL}.</li>
 *   <li>Proper exception handling when the {@link URI} cannot be turned into a {@link URL}.</li>
 *   <li>Null handling (expected {@link NullPointerException}).</li>
 * </ul>
 */
public class UriConversionsTest {

    /** A dummy {@link Converter} instance – the conversion methods do not use it. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // Passing {@code null} is acceptable for these tests because the
        // methods under test never invoke any Converter API.
        this.dummyConverter = new Converter(null);
    }

    /* -------------------------------------------------
     *  Tests for toMap(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToMapCreatesCorrectMap() throws Exception {
        URI uri = new URI("http://example.com/path?query=1");
        Map<String, Object> map = UriConversions.toMap(uri, dummyConverter);

        // The map should be a CompactMap (builder creates that implementation)
        assertTrue("Returned map should be an instance of CompactMap", map instanceof CompactMap);

        // Exactly one entry should be present
        assertEquals("Map should contain exactly one entry", 1, map.size());

        // The key must be the constant defined in MapConversions
        assertTrue("Map should contain the URI_KEY", map.containsKey(URI_KEY));

        // The value must be the string representation of the original URI
        assertEquals("Map value for URI_KEY must equal uri.toString()", uri.toString(), map.get(URI_KEY));
    }

    @Test
    public void testToMapPreservesInsertionOrder() throws Exception {
        // Build a map with more than one entry by calling toMap and then adding another entry.
        URI uri = new URI("https://cedarsoftware.com");
        Map<String, Object> map = UriConversions.toMap(uri, dummyConverter);

        // Add a second entry manually
        map.put("secondKey", "secondValue");

        // Verify that the iteration order respects insertion order:
        // first entry should be the URI_KEY, second entry should be "secondKey".
        String firstKey = map.entrySet().iterator().next().getKey();
        assertEquals("First entry should be the URI_KEY due to insertion order", URI_KEY, firstKey);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullUriThrowsNPE() {
        // The method casts the input to URI without a null check, so a NPE is expected.
        UriConversions.toMap(null, dummyConverter);
    }

    /* -------------------------------------------------
     *  Tests for toURL(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToURLSuccess() throws Exception {
        URI uri = new URI("http://www.example.org/index.html?foo=bar");
        URL url = UriConversions.toURL(uri, dummyConverter);

        // The URL produced by toURL should be equal to a URL constructed directly from the same string.
        URL expected = new URL(uri.toString());
        assertEquals("Converted URL should match expected URL", expected, url);
    }

    @Test
    public void testToURLFailureForRelativeUri() {
        // Relative URIs cannot be turned into a URL; they cause an IllegalArgumentException
        // which our method wraps in an IllegalArgumentException with a custom message.
        URI relativeUri = URI.create("/relative/path");

        try {
            UriConversions.toURL(relativeUri, dummyConverter);
            fail("Expected IllegalArgumentException to be thrown for relative URI");
        } catch (IllegalArgumentException ex) {
            // Verify the message contains our custom text.
            assertTrue("Exception message should mention inability to convert",
                    ex.getMessage().contains("Unable to convert URI to URL"));

            // The cause should be the original IllegalArgumentException from URI.toURL()
            assertNotNull("Cause should be present", ex.getCause());
            assertTrue("Cause should be IllegalArgumentException",
                    ex.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testToURLWithNullUriThrowsNPE() {
        // As with toMap, a null input results in a NPE during the cast.
        UriConversions.toURL(null, dummyConverter);
    }
}
