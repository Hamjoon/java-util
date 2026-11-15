package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.net.URL;
import java.net.URI;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static org.junit.Assert.*;

public class UrlConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The UrlConversions methods do not actually use the Converter instance,
        // so we can safely pass null or a dummy instance.
        dummyConverter = null;
    }

    /*** --------------------------------------------------------------------
     *  Tests for UrlConversions.toMap(...)
     *  ----------------------------------------------------------------- ***/

    @Test
    public void testToMap_BasicConversion() throws Exception {
        URL url = new URL("http://example.com/path?query=123");
        Map<String, Object> result = UrlConversions.toMap(url, dummyConverter);

        assertNotNull("Result map should not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain the URL_KEY", result.containsKey(URL_KEY));
        assertEquals("Stored URL string should match the original URL",
                url.toString(), result.get(URL_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        // Passing null should cause a NullPointerException when url.toString() is invoked
        UrlConversions.toMap(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        // Passing an object that is not a URL should cause a ClassCastException
        UrlConversions.toMap("not a URL", dummyConverter);
    }

    /*** --------------------------------------------------------------------
     *  Tests for UrlConversions.toURI(...)
     *  ----------------------------------------------------------------- ***/

    @Test
    public void testToURI_BasicConversion() throws Exception {
        URL url = new URL("https://www.example.org/some/path?param=value#section");
        URI uri = UrlConversions.toURI(url, dummyConverter);

        assertNotNull("Resulting URI should not be null", uri);
        assertEquals("URI string should match URL string", url.toString(), uri.toString());
    }

    @Test
    public void testToURI_NullInput_throwsIllegalArgumentException() {
        try {
            UrlConversions.toURI(null, dummyConverter);
            fail("Expected IllegalArgumentException when converting null URL");
        } catch (IllegalArgumentException e) {
            assertNotNull("Cause should be present", e.getCause());
            assertTrue("Cause should be NullPointerException",
                    e.getCause() instanceof NullPointerException);
            assertTrue("Exception message should contain the word 'convert'",
                    e.getMessage().toLowerCase().contains("convert"));
        }
    }

    @Test
    public void testToURI_MalformedURL_throwsIllegalArgumentException() throws Exception {
        // Construct a URL that is syntactically valid but will cause URL.toURI()
        // to throw a URISyntaxException. The classic example is a URL containing
        // an illegal character in the path that the URL class accepts but the
        // URI class rejects. The pipe character '|' is such a case.
        URL url = new URL("http://example.com/pipe|character");
        try {
            UrlConversions.toURI(url, dummyConverter);
            fail("Expected IllegalArgumentException due to URISyntaxException");
        } catch (IllegalArgumentException e) {
            assertNotNull("Cause should be present", e.getCause());
            assertTrue("Cause should be URISyntaxException",
                    e.getCause().getClass().getSimpleName().equals("URISyntaxException"));
        }
    }
}
