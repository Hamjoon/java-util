package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Test;
import org.junit.Before;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.Assert.*;

public class UriConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // The actual content of the options is irrelevant for the static methods we are testing.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToMapCreatesCorrectMap() throws Exception {
        URI uri = new URI("http://example.com/test?param=value");
        Map result = UriConversions.toMap(uri, converter);

        // The result should be a CompactLinkedMap (or at least a Map)
        assertNotNull("Resulting map should not be null", result);
        assertTrue("Resulting map should be an instance of CompactLinkedMap",
                result instanceof CompactLinkedMap);

        // It must contain exactly one entry with the proper key and value
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain the URI_KEY", result.containsKey(URI_KEY));
        assertEquals("Stored URI string should match the original URI",
                uri.toString(), result.get(URI_KEY));
    }

    @Test
    public void testToURLSuccessfulConversion() throws Exception {
        URI uri = new URI("https://www.example.org:8080/path/to/resource?query=123#fragment");
        URL url = UriConversions.toURL(uri, converter);

        assertNotNull("Converted URL should not be null", url);
        assertEquals("URL string should match the URI string (minus the fragment)",
                uri.toURL().toString(), url.toString());
    }

    @Test
    public void testToURLThrowsIllegalArgumentExceptionForNonHierarchicalURI() throws Exception {
        // A non‑hierarchical URI such as a mailto: scheme cannot be converted to a URL
        URI uri = new URI("mailto:user@example.com");

        try {
            UriConversions.toURL(uri, converter);
            fail("Expected IllegalArgumentException to be thrown for non‑hierarchical URI");
        } catch (IllegalArgumentException e) {
            // Verify that the cause is a MalformedURLException (or a subclass)
            assertNotNull("Cause of the exception should not be null", e.getCause());
            assertTrue("Cause should be a MalformedURLException",
                    e.getCause() instanceof java.net.MalformedURLException);
            // Verify that the message contains the original URI
            assertTrue("Exception message should contain the original URI",
                    e.getMessage().contains(uri.toString()));
        }
    }

    @Test
    public void testToMapHandlesURIEscapingCorrectly() throws Exception {
        // URI containing spaces and special characters must be correctly stringified
        URI uri = new URI("http", "user:pass", "host.com", 80, "/path with spaces/", "q=1&c=2", "frag");
        Map result = UriConversions.toMap(uri, converter);

        assertEquals("Stored string must be the exact URI string representation",
                uri.toString(), result.get(URI_KEY));
    }
}
