package com.cedarsoftware.util.convert;

import org.junit.Test;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static org.junit.Assert.*;

public class UrlConversionsTest {

    /* -------------------------------------------------
       BASIC FUNCTIONALITY
       ------------------------------------------------- */

    @Test
    public void testToMapCreatesMapWithUrlString() throws Exception {
        URL url = new URL("http://example.com/path?query=123");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = UrlConversions.toMap(url, null);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the URL_KEY", result.containsKey(URL_KEY));
        assertEquals("Stored value must be the URL string",
                     url.toString(),
                     result.get(URL_KEY));
    }

    @Test
    public void testToURIConvertsUrlToUri() throws Exception {
        URL url = new URL("https://www.example.org/some/path?param=value#frag");
        URI uri = UrlConversions.toURI(url, null);

        assertNotNull("Returned URI must not be null", uri);
        assertEquals("URI string must match URL string", url.toString(), uri.toString());
    }

    /* -------------------------------------------------
       EDGE CASES & EXCEPTION HANDLING
       ------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullUrlThrowsNPE() {
        UrlConversions.toMap(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToURIWithNullUrlThrowsNPE() {
        UrlConversions.toURI(null, null);
    }

    @Test
    public void testToURIWithInvalidPercentEncodingThrowsIllegalArgumentException() throws Exception {
        // URL accepts a lone '%' in the path, but toURI() will fail with URISyntaxException
        URL badUrl = new URL("http://example.com/%");
        try {
            UrlConversions.toURI(badUrl, null);
            fail("Expected IllegalArgumentException because URL cannot be converted to URI");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            assertTrue("Exception message should mention the input URL",
                       msg != null && msg.contains(badUrl.toString()));
            // Verify the underlying cause exists (should be URISyntaxException)
            assertNotNull("Cause should be present", e.getCause());
        }
    }
}
