package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URL;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class UrlConversions_toMap_Test {

    @Mock
    private Converter // Converter is not used by the method, but required by the signature
    mockConverter;

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(arguments("Simple HTTP URL", createUrl("http://example.com"), null), arguments("URL with path, query and fragment", createUrl("https://example.com/path?query=1#section"), null), arguments("FTP URL with credentials", createUrl("ftp://user:pass@example.org/resource.txt"), MockitoExtension.mock(Converter.class)), arguments("File protocol URL", createUrl("file:/path/to/file.txt"), null), arguments("IPv6 literal host", createUrl("http://[2001:db8::1]/"), null), arguments("Explicit non‑default port", createUrl("http://example.com:8080"), null), arguments("URL with query only", createUrl("http://example.com?param=val"), null), arguments("URL with fragment only", createUrl("http://example.com#section"), null), arguments("Non‑null real Converter implementation (ignored)", createUrl("https://example.org"), new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // Dummy implementation – should never be called
                return null;
            }
        }));
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(arguments("String instead of URL", "http://not-a-url-object"), arguments("Date object instead of URL", new Date()));
    }

    /**
     * Utility to create a URL instance, re‑throwing as an unchecked exception for brevity.
     */
    private static URL createUrl(String spec) {
        try {
            return new URL(spec);
        } catch (Exception e) {
            throw new RuntimeException("Invalid URL used in test data: " + spec, e);
        }
    }

    @Test
    void toMap_returnsMapContainingUrlString() throws Exception {
        // GIVEN
        URL url = new URL("http://example.com/path?query=123");
        // WHEN
        Map<String, Object> result = UrlConversions.toMap(url, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(URL_KEY), "Map should contain the URL_KEY");
        assertEquals(url.toString(), result.get(URL_KEY), "Map value should be the URL string");
    }

    @Test
    void toMap_nullUrl_throwsNullPointerException() {
        // GIVEN
        URL nullUrl = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UrlConversions.toMap(nullUrl, mockConverter), "Calling toMap with a null URL should throw NPE because url.toString() is invoked");
    }

    /**
     * Successful conversions – a variety of URL syntaxes.
     * The method must return a LinkedHashMap with a single entry whose key is URL_KEY
     * and whose value equals the original URL's {@code toString()} representation.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toMap_successfulConversions(String description, URL url, Converter converter) {
        // WHEN
        Map<String, Object> result = UrlConversions.toMap(url, converter);
        // THEN – basic map contract
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Map must contain exactly one entry");
        assertTrue(result.containsKey(URL_KEY), "Map must contain the URL_KEY");
        assertEquals(url.toString(), result.get(URL_KEY), "Stored URL string must match the original");
        // THEN – map implementation specifics
        assertTrue(result instanceof LinkedHashMap, "Implementation should be LinkedHashMap to preserve insertion order");
    }

    /**
     * Invalid source types – the method casts the input directly to {@link URL},
     * therefore any non‑URL object should cause a {@link ClassCastException}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidSourceProvider")
    void toMap_invalidSourceTypes(String description, Object invalidSource) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UrlConversions.toMap(invalidSource, null), "Passing a non‑URL object should result in ClassCastException");
    }

    /**
     * Verifies that the returned map is specifically a {@link LinkedHashMap}
     * and contains exactly one entry with the expected key.
     */
    @Test
    void toMap_mapCharacteristics_typeAndSize() throws Exception {
        // GIVEN
        URL url = new URL("http://example.com");
        // WHEN
        Map<String, Object> result = UrlConversions.toMap(url, null);
        // THEN
        assertTrue(result instanceof LinkedHashMap, "Result should be an instance of LinkedHashMap");
        assertEquals(1, result.size(), "Resulting map must contain exactly one entry");
        assertTrue(result.containsKey(URL_KEY), "Resulting map must contain the URL_KEY");
    }
}
