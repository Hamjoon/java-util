package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * Unit tests for {@link UriConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of a {@link URI} to a {@link Map} via {@link UriConversions#toMap(Object, Converter)}.</li>
 *   <li>Successful conversion of a {@link URI} to a {@link URL} via {@link UriConversions#toURL(Object, Converter)}.</li>
 *   <li>Failure path when a {@link URI} cannot be converted to a {@link URL} (illegal protocol).</li>
 *   <li>Verification that the {@code Converter} argument is not used by the current implementation.</li>
 * </ul>
 *
 * The test class resides in the same package as {@code UriConversions} so that the
 * package‑private static methods are directly accessible.
 */
public class UriConversionsTest {

    /**
     * Helper that creates a {@link Converter} instance.
     * The {@code Converter} does not influence the behavior of the methods under test,
     * therefore a {@code null} options instance is sufficient.
     */
    private static Converter dummyConverter() {
        return new Converter(null);
    }

    /**
     * Verify that {@code toMap} returns a map containing a single entry whose key
     * equals {@code MapConversions.URI_KEY} and whose value is the string form of the
     * supplied {@link URI}.
     */
    @Test
    public void testToMapSimpleUri() {
        // Arrange
        URI uri = URI.create("https://example.com/path?query=123");
        Converter converter = dummyConverter();

        // Act
        Map<String, Object> result = UriConversions.toMap(uri, converter);

        // Assert
        Assert.assertNotNull("Result map must not be null", result);
        Assert.assertEquals("Map should contain exactly one entry", 1, result.size());
        Assert.assertTrue("Map must contain the expected key",
                result.containsKey(URI_KEY));
        Assert.assertEquals("Map value must be the URI string",
                uri.toString(),
                result.get(URI_KEY));
    }

    /**
     * Verify that {@code toMap} correctly handles URIs that contain characters
     * requiring percent‑encoding (e.g., spaces). The string stored in the map must
     * be the exact {@code toString()} representation of the original {@link URI}.
     */
    @Test
    public void testToMapUriWithEncodedCharacters() {
        // Arrange
        URI uri = URI.create("http://host.com/space%20here");
        Converter converter = dummyConverter();

        // Act
        Map<String, Object> map = UriConversions.toMap(uri, converter);

        // Assert
        Assert.assertEquals("Encoded space must be preserved in map value",
                "http://host.com/space%20here",
                map.get(URI_KEY));
    }

    /**
     * Verify that a well‑formed {@link URI} is successfully converted to a {@link URL}
     * and that the resulting {@code URL} equals the expected value.
     */
    @Test
    public void testToURLSuccess() throws Exception {
        // Arrange
        URI uri = URI.create("https://www.example.org/resource?id=42");
        Converter converter = dummyConverter();

        // Act
        URL url = UriConversions.toURL(uri, converter);

        // Assert
        Assert.assertNotNull("Converted URL must not be null", url);
        Assert.assertEquals("URL string must match the original URI string",
                uri.toString(),
                url.toString());
    }

    /**
     * Verify that an {@link IllegalArgumentException} is thrown when the supplied
     * {@link URI} cannot be converted to a {@link URL}.  The classic example is a
     * {@code mailto:} URI, which does not have a protocol supported by {@code URL}.
     *
     * The test also checks that the exception message contains the original URI
     * string, providing useful diagnostic information.
     */
    @Test
    public void testToURLInvalidProtocol() {
        // Arrange
        URI uri = URI.create("mailto:someone@example.com");
        Converter converter = dummyConverter();

        try {
            // Act
            UriConversions.toURL(uri, converter);
            Assert.fail("Expected IllegalArgumentException for unsupported URI protocol");
        } catch (IllegalArgumentException ex) {
            // Assert
            String expectedFragment = "Unable to convert URI to URL, input URI: " + uri;
            Assert.assertTrue("Exception message should contain the original URI",
                    ex.getMessage().contains(expectedFragment));
            // The cause should be a MalformedURLException (or subclass)
            Assert.assertTrue("Cause should be a MalformedURLException",
                    ex.getCause() instanceof java.net.MalformedURLException);
        }
    }

    /**
     * Verify that passing {@code null} as the source object results in a
     * {@link NullPointerException}.  The current implementation casts the input
     * to {@link URI} without a null‑check, so a NPE is the expected behaviour.
     */
    @Test(expected = NullPointerException.class)
    public void testToMapNullInput() {
        Converter converter = dummyConverter();
        UriConversions.toMap(null, converter);
    }

    /**
     * Verify that passing {@code null} as the source object to {@code toURL}
     * also results in a {@link NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testToURLNullInput() {
        Converter converter = dummyConverter();
        UriConversions.toURL(null, converter);
    }
}
