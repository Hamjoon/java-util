package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.junit.Test;

/**
 * JUnit 4 test cases for {@link UriConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Typical successful conversions for {@code toMap} and {@code toURL}.</li>
 *   <li>Edge‑case handling such as {@code null} input.</li>
 *   <li>Error scenarios where conversion should fail and an appropriate exception is thrown.</li>
 * </ul>
 *
 * <p>Only the public API shown in the problem statement is used; no mocking frameworks are employed.
 */
public class UriConversionsTest {

    /** Helper to create a simple URI instance used by several tests. */
    private static URI createSimpleUri() {
        return URI.create("http://example.com/path?query=123");
    }

    /**
     * Verify that {@code toMap} returns a {@link CompactMap} containing a single entry
     * with the key {@link MapConversions#URI_KEY} and the string representation of the URI.
     */
    @Test
    public void testToMapTypical() {
        URI uri = createSimpleUri();

        // The Converter argument is not used by the implementation, but we must provide a non‑null instance.
        Converter dummyConverter = new Converter(new ConverterOptions());

        Map<String, Object> result = UriConversions.toMap(uri, dummyConverter);

        // The returned map must be a CompactMap (inherits from Map) and preserve insertion order.
        assertNotNull("Resulting map must not be null", result);
        assertTrue("Resulting map should be an instance of CompactMap",
                result instanceof CompactMap);

        // The map should contain exactly one entry.
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // Verify the key and value.
        assertTrue("Map must contain the URI_KEY", result.containsKey(URI_KEY));
        assertEquals("Value stored must be the URI's string representation",
                uri.toString(), result.get(URI_KEY));
    }

    /**
     * Verify that {@code toMap} throws a {@link NullPointerException} when the supplied
     * {@code from} object is {@code null}. The implementation casts the argument to {@link URI}
     * and then dereferences it, which results in NPE.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapNullInput() {
        Converter dummyConverter = new Converter(new ConverterOptions());

        // Pass null – the cast succeeds (null remains null) but uri.toString() triggers NPE.
        UriConversions.toMap(null, dummyConverter);
    }

    /**
     * Verify that {@code toURL} correctly converts a well‑formed {@link URI} to a {@link URL}.
     */
    @Test
    public void testToURLTypical() throws Exception {
        URI uri = createSimpleUri();

        Converter dummyConverter = new Converter(new ConverterOptions());

        URL url = UriConversions.toURL(uri, dummyConverter);

        assertNotNull("Converted URL must not be null", url);
        assertEquals("The URL string must match the original URI string",
                uri.toString(), url.toString());
    }

    /**
     * Verify that {@code toURL} throws an {@link IllegalArgumentException} when the URI
     * cannot be converted to a URL (e.g., missing a scheme).
     */
    @Test
    public void testToURLInvalidUri() {
        // A URI without a scheme cannot be converted to a URL.
        URI invalidUri = URI.create("/relative/path");

        Converter dummyConverter = new Converter(new ConverterOptions());

        try {
            UriConversions.toURL(invalidUri, dummyConverter);
            fail("Expected IllegalArgumentException due to malformed URI");
        } catch (IllegalArgumentException e) {
            // Verify that the exception message contains the original URI for diagnostics.
            assertTrue("Exception message should contain the original URI",
                    e.getMessage().contains(invalidUri.toString()));
            // The cause should be the original exception thrown by URI.toURL().
            assertNotNull("Cause of IllegalArgumentException should not be null", e.getCause());
        }
    }

    /**
     * Verify that {@code toURL} throws a {@link NullPointerException} when a {@code null}
     * URI is supplied. The method casts the argument to {@link URI} (resulting in {@code null})
     * and then calls {@code uri.toURL()}, which triggers NPE before the catch block.
     */
    @Test(expected = NullPointerException.class)
    public void testToURLNullInput() {
        Converter dummyConverter = new Converter(new ConverterOptions());

        // This will cause a NullPointerException inside the method.
        UriConversions.toURL(null, dummyConverter);
    }
}
