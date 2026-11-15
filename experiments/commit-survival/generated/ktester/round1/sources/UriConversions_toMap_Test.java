package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.URL;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class UriConversions_toMap_Test {

    @Mock
    private Converter // not used by the method, but required by signature
    converter;

    /**
     * Provides a wide range of {@link URI} instances together with the
     * expected string that must be stored in the resulting map.
     */
    private static Stream<Arguments> validUriProvider() {
        return Stream.of(// Standard HTTP URI
        arguments(URI.create("http://example.com/path?query=1"), "http://example.com/path?query=1", "Standard HTTP URI"), // URN scheme (non‑hierarchical)
        arguments(URI.create("urn:example:animal:ferret:nose"), "urn:example:animal:ferret:nose", "URN scheme"), // File URI with percent‑encoded characters
        arguments(URI.create("file:///C:/temp/file%20name.txt"), "file:///C:/temp/file%20name.txt", "File URI with percent‑encoding"), // Empty URI string (zero‑length)
        arguments(URI.create(""), "", "Empty URI"), // Percent‑encoded space in path
        arguments(URI.create("http://example.com/space%20here"), "http://example.com/space%20here", "Path contains percent‑encoded space"), // Relative path components (.. and .) – not normalized
        arguments(URI.create("http://example.com/../a/b/./c"), "http://example.com/../a/b/./c", "Relative path components"), // URI built from separate components
        arguments(new URI("http", "user:pass@example.com", "/path", "query", "fragment"), "http://user:pass@example.com/path?query#fragment", "Components‑based construction"), // Mailto scheme (non‑hierarchical)
        arguments(URI.create("mailto:user@example.com"), "mailto:user@example.com", "Mailto scheme"), // Explicit default port (80)
        arguments(URI.create("http://example.com:80"), "http://example.com:80", "Explicit default port"), // IPv6 host literal
        arguments(URI.create("http://[2001:db8::1]/path"), "http://[2001:db8::1]/path", "IPv6 host literal"), // Query string containing a plus sign (not percent‑encoded)
        arguments(URI.create("http://example.com/?q=1+2"), "http://example.com/?q=1+2", "Query with plus sign"), // Fragment containing percent‑encoded space
        arguments(URI.create("http://example.com/#section%20name"), "http://example.com/#section%20name", "Fragment with percent‑encoded space"));
    }

    @Test
    void toMap_returnsMapContainingUriString() throws Exception {
        // GIVEN
        URI uri = new URI("http://example.com/path?query=1");
        // WHEN
        Map<String, Object> result = UriConversions.toMap(uri, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(URI_KEY), "Map should contain the URI_KEY");
        assertEquals(uri.toString(), result.get(URI_KEY), "Stored URI string should match the original URI");
    }

    @Test
    void toMap_throwsClassCastException_whenInputIsNotUri() {
        // GIVEN
        Object notAUri = "not a uri";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UriConversions.toMap(notAUri, converter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validUriProvider")
    void toMap_validUris_returnExpectedMap(URI uri, String expectedString, String description) {
        // WHEN
        // using the @Mock converter
        Map<String, Object> map = UriConversions.toMap(uri, converter);
        // THEN – map contains exactly one entry with the correct key/value
        assertNotNull(map, "Resulting map must not be null");
        assertEquals(1, map.size(), "Map should contain exactly one entry");
        assertTrue(map.containsKey(URI_KEY), "Map must contain the constant key URI_KEY");
        assertEquals(expectedString, map.get(URI_KEY), "Stored value must match URI.toString()");
        // AND – the map is mutable (extra entries can be added)
        map.put("extra", "value");
        assertEquals("value", map.get("extra"), "Map should be mutable after creation");
    }

    @Test
    void toMap_nullFrom_throwsNullPointerException() {
        // Both parameters are null – the cast to URI will trigger NPE
        assertThrows(NullPointerException.class, () -> UriConversions.toMap(null, null), "Passing null as the source object should throw NullPointerException");
    }

    @Test
    void toMap_nonUriObject_throwsClassCastException() throws Exception {
        // Using a URL instance (any non‑URI type) must result in ClassCastException
        URL url = new URL("http://example.com");
        assertThrows(ClassCastException.class, () -> UriConversions.toMap(url, null), "Casting a non‑URI object to URI should throw ClassCastException");
    }

    @Test
    void toMap_doesNotInteractWithConverter() throws Exception {
        // GIVEN a URI and a mock converter that would throw on any interaction
        URI uri = new URI("http://example.com/no-converter");
        // No stubbing needed – we simply verify that no methods are called
        // WHEN
        Map<String, Object> result = UriConversions.toMap(uri, converter);
        // THEN
        assertEquals(uri.toString(), result.get(URI_KEY));
        // Verify that the mock had zero interactions
        Mockito.verifyNoInteractions(converter);
    }
}
