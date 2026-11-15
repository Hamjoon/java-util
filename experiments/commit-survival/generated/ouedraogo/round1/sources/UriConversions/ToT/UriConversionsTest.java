package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link UriConversions}.
 *
 * The tests are written from the perspective of three independent experts.
 * Each expert contributed one test case for each public static method.
 * The suite therefore covers:
 *
 * 1. Normal (happy‑path) conversions.
 * 2. Edge cases such as empty or null inputs.
 * 3. Error handling – ensuring the wrapper exception is thrown with the
 *    correct message when the underlying conversion fails.
 *
 * No mocking frameworks are used; all objects are created via their public
 * constructors or factory methods as required by the problem statement.
 */
public class UriConversionsTest {

    /* --------------------------------------------------------------------- */
    /* Expert‑1: toMap – normal case                                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToMap_WithValidUri_ShouldReturnMapContainingUriString() {
        // Arrange – a typical absolute HTTP URI
        URI uri = URI.create("http://example.com:8080/path?query=123#fragment");

        // Act – invoke the method under test (the Converter argument is unused)
        Map<String, Object> result = UriConversions.toMap(uri, null);

        // Assert – map must contain exactly one entry with the proper key/value
        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Resulting map should be an instance of CompactLinkedMap",
                result instanceof CompactLinkedMap);
        assertEquals("Stored URI string must match the original URI's toString()",
                uri.toString(), result.get(URI_KEY));
    }

    /* --------------------------------------------------------------------- */
    /* Expert‑2: toMap – null input (edge case)                              */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testToMap_WithNullUri_ShouldThrowNullPointerException() {
        // Arrange – a null reference (casting null to URI is allowed)
        URI nullUri = null;

        // Act – this should throw NPE when uri.toString() is invoked inside toMap
        UriConversions.toMap(nullUri, null);
    }

    /* --------------------------------------------------------------------- */
    /* Expert‑3: toMap – empty URI string (edge case)                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToMap_WithEmptyUriString_ShouldStoreEmptyString() {
        // Arrange – an empty (relative) URI
        URI emptyUri = URI.create("");

        // Act
        Map<String, Object> result = UriConversions.toMap(emptyUri, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(URI_KEY));
    }

    /* --------------------------------------------------------------------- */
    /* Expert‑1: toURL – normal case (happy path)                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToURL_WithHttpUri_ShouldReturnEquivalentURL() {
        // Arrange – a well‑formed hierarchical URI
        URI uri = URI.create("https://www.example.org/resource?id=42");

        // Act
        URL url = UriConversions.toURL(uri, null);

        // Assert – the URL should be equal to uri.toURL()
        assertNotNull("Resulting URL must not be null", url);
        assertEquals("URL produced by toURL must match URI.toURL()", uri.toURL(), url);
    }

    /* --------------------------------------------------------------------- */
    /* Expert‑2: toURL – unsupported scheme (error handling)                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToURL_WithNonHierarchicalUri_ShouldWrapException() {
        // Arrange – a URI with a scheme that cannot be turned into a URL (e.g., mailto)
        URI mailtoUri = URI.create("mailto:user@example.com");

        // Act & Assert
        try {
            UriConversions.toURL(mailtoUri, null);
            fail("Expected IllegalArgumentException to be thrown for non‑hierarchical URI");
        } catch (IllegalArgumentException ex) {
            // Verify that the cause is a MalformedURLException (or any Exception) and
            // that the message contains the original URI string.
            assertTrue("Cause should be an instance of Exception",
                    ex.getCause() instanceof Exception);
            assertTrue("Exception message must contain the original URI",
                    ex.getMessage().contains(mailtoUri.toString()));
        }
    }

    /* --------------------------------------------------------------------- */
    /* Expert‑3: toURL – null input (edge case)                              */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testToURL_WithNullUri_ShouldThrowNullPointerException() {
        // Arrange – null URI
        URI nullUri = null;

        // Act – this will attempt to call nullUri.toURL() and throw NPE
        UriConversions.toURL(nullUri, null);
    }

    /* --------------------------------------------------------------------- */
    /* Expert‑1: toURL – malformed URL caused by illegal characters          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToURL_WithUriContainingIllegalCharacters_ShouldWrapException() {
        // Arrange – a URI that is syntactically valid but produces a malformed URL
        // (e.g., a space character that is not percent‑encoded)
        URI badUri = URI.create("http://example.com/invalid path");

        // Act & Assert
        try {
            UriConversions.toURL(badUri, null);
            fail("Expected IllegalArgumentException due to malformed URL");
        } catch (IllegalArgumentException ex) {
            assertTrue("Exception message must contain the original URI",
                    ex.getMessage().contains(badUri.toString()));
        }
    }
}
