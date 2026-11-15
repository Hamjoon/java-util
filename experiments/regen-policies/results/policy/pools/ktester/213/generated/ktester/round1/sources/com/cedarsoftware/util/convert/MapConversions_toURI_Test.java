package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class MapConversions_toURI_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter class now requires a ConverterOptions instance.
//         converter = new Converter(new ConverterOptions());
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> directConversionProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.arguments("Source is already a URI – should be returned unchanged", URI.create("http://example.com"), URI.create("http://example.com")), org.junit.jupiter.params.provider.Arguments.arguments("Source is a URL – should be converted via URL.toURI()", createUrl("http://example.com"), URI.create("http://example.com")), org.junit.jupiter.params.provider.Arguments.arguments("Source is a valid URI string – should be parsed", "http://example.com/path?query=1", URI.create("http://example.com/path?query=1")));
    }

    private static URL createUrl(String spec) {
        try {
            return new URL(spec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> mapConversionProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.arguments("Map with key \"URI\" and a String value", Collections.singletonMap("URI", "http://example.com/map1"), URI.create("http://example.com/map1")), org.junit.jupiter.params.provider.Arguments.arguments("Map with key \"value\" and a pre‑built URI value", Collections.singletonMap("value", URI.create("http://example.com/map2")), URI.create("http://example.com/map2")), org.junit.jupiter.params.provider.Arguments.arguments("Map with key \"_v\" and a URL value", Collections.singletonMap("_v", createUrl("http://example.com/map3")), URI.create("http://example.com/map3")), org.junit.jupiter.params.provider.Arguments.arguments("Map with multiple recognised keys – priority order (URI > value > _v)", new HashMap<String, Object>() {

            {
                put("value", "http://example.com/second");
                put("URI", "http://example.com/first");
            }
        }, URI.create("http://example.com/first")), org.junit.jupiter.params.provider.Arguments.arguments("Map with key \"URI\" holding a URL instance – should be converted", Collections.singletonMap("URI", createUrl("http://example.com/urlvalue")), URI.create("http://example.com/urlvalue")));
    }

    @Test
    void testToURIFromMapWithURIKey() {
        String uriString = "https://example.com/path?query=1";
        Map<String, Object> map = new HashMap<>();
        map.put("URI", uriString);
        URI result = MapConversions.toURI(map, converter);
        assertNotNull(result);
        assertEquals(uriString, result.toString());
    }

    @Test
    void testToURIFromMapWithValueKey() {
        String uriString = "http://localhost:8080/test";
        Map<String, Object> map = new HashMap<>();
        map.put("value", uriString);
        URI result = MapConversions.toURI(map, converter);
        assertNotNull(result);
        assertEquals(uriString, result.toString());
    }

    @Test
    void testToURIFromMapWithVKey() {
        String uriString = "file:///tmp/file.txt";
        Map<String, Object> map = new HashMap<>();
        map.put("_v", uriString);
        URI result = MapConversions.toURI(map, converter);
        assertNotNull(result);
        assertEquals(uriString, result.toString());
    }

    @Test
    void testToURIWhenSourceIsAlreadyURI() {
        URI source = URI.create("mailto:user@example.com");
        URI result = MapConversions.toURI(source, converter);
        assertSame(source, result, "When the source object is already a URI it should be returned unchanged.");
    }

    @Test
    void testToURINullInput() {
        URI result = MapConversions.toURI(null, converter);
        assertNull(result, "Conversion of null should yield null.");
    }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("directConversionProvider")
//     void testDirectConversions(String description, Object source, URI expected) {
//         URI result = MapConversions.toURI(source, converter);
//         assertNotNull(result, "Result should never be null for valid inputs");
//         assertEquals(expected, result, description);
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("mapConversionProvider")
//     void testMapConversions(String description, Map<String, Object> map, URI expected) {
//         URI result = MapConversions.toURI(map, converter);
//         assertNotNull(result, "Result should not be null for a map containing a recognised key");
//         assertEquals(expected, result, description);
//     }

    @Test
    void testEmptyMapThrowsIllegalArgumentException() {
        Map<String, Object> empty = Collections.emptyMap();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toURI(empty, converter), "An empty map should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("URI"), "Exception message should mention missing URI keys");
    }

    @Test
    void testNullConverterThrowsNullPointerException() {
        URI source = URI.create("http://example.com");
        assertThrows(NullPointerException.class, () -> MapConversions.toURI(source, null), "Passing a null Converter should cause NullPointerException");
    }
}