package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.URL;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toURI_Test {

    // used for the “fallback” test
    private Converter realConverter;

    // used for the “URI key present” test
    private Converter mockConverter;

    private static final String URI_KEY = "URI";

    /**
     * Simple subclass of {@link HashMap} to emulate {@code com.cedarsoftware.util.CompactMap}.
     */
    static class CompactMap<K, V> extends HashMap<K, V> {
        // No extra behaviour needed for the test
    }

    @BeforeEach
    void setupBeforeEach() {
        // a real Converter with default options – needed for the fallback path
        realConverter = new Converter(new ConverterOptions());
        // a Mockito mock – needed to verify that the converter is delegated to
        mockConverter = mock(Converter.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear references – helps GC when many tests run
        realConverter = null;
        mockConverter = null;
    }

    @Test
    @DisplayName("toURI returns converter.convert result when map contains a non‑empty 'URI' entry")
    void toURI_UsesConverterWhenUriKeyPresent() throws Exception {
        // GIVEN
        String uriString = "http://example.com/path?query=1";
        URI expected = new URI(uriString);
        Map<String, Object> map = new HashMap<>();
        map.put(URI_KEY, uriString);
        // configure mock to return the expected URI
        when(mockConverter.convert(uriString, URI.class)).thenReturn(expected);
        // WHEN
        URI result = MapConversions.toURI(map, mockConverter);
        // THEN
        assertEquals(expected, result, "The returned URI should be exactly what the mock converter provides");
        // verify that the converter was called exactly once with the correct arguments
        verify(mockConverter, times(1)).convert(uriString, URI.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    @DisplayName("toURI falls back to generic map conversion when 'URI' entry is missing or empty")
    void toURI_FallbackWhenUriKeyMissing() throws Exception {
        // GIVEN
        // No explicit "URI" key – but a generic "value" key that the Converter knows how to turn into a URI
        String uriString = "https://fallback.example.org/resource";
        Map<String, Object> map = new HashMap<>();
        // the generic key used by Converter for map‑to‑URI conversion
        map.put("value", uriString);
        // WHEN
        URI result = MapConversions.toURI(map, realConverter);
        // THEN
        assertNotNull(result, "Result should not be null when a convertible value is present");
        assertEquals(new URI(uriString), result, "Converter should turn the 'value' entry into the correct URI");
    }

    @Test
    @DisplayName("toURI passes original (un‑trimmed) string to converter even if it contains surrounding whitespace")
    void toURI_ConverterReceivesOriginalWhitespaceString() throws Exception {
        // GIVEN
        String raw = "   https://host.org   ";
        String trimmed = "https://host.org";
        URI expected = new URI(trimmed);
        Map<String, Object> map = new HashMap<>();
        map.put(URI_KEY, raw);
        when(mockConverter.convert(raw, URI.class)).thenReturn(expected);
        // WHEN
        URI result = MapConversions.toURI(map, mockConverter);
        // THEN
        assertEquals(expected, result);
        verify(mockConverter).convert(raw, URI.class);
    }

    @Test
    @DisplayName("toURI propagates exception thrown by the converter")
    void toURI_ConverterThrowsException() {
        // GIVEN
        String badUri = "invalid://:uri";
        Map<String, Object> map = new HashMap<>();
        map.put(URI_KEY, badUri);
        RuntimeException ex = new RuntimeException("conversion failed");
        when(mockConverter.convert(badUri, URI.class)).thenThrow(ex);
        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> MapConversions.toURI(map, mockConverter));
        assertSame(ex, thrown, "Exception from converter should propagate unchanged");
    }

    @Test
    @DisplayName("toURI falls back when 'URI' entry is empty string")
    void toURI_FallbackWhenUriEmptyString() throws Exception {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // empty -> treated as missing
        map.put(URI_KEY, "");
        map.put("scheme", "https");
        map.put("host", "secure.com");
        map.put("path", "/login");
        map.put("query", "user=admin");
        // WHEN
        URI result = MapConversions.toURI(map, realConverter);
        // THEN
        assertEquals(new URI("https://secure.com/login?user=admin"), result);
    }

    @Test
    @DisplayName("toURI falls back when 'URI' entry is null")
    void toURI_FallbackWhenUriNull() throws Exception {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(URI_KEY, null);
        map.put("scheme", "ftp");
        map.put("host", "fileserver");
        map.put("path", "/downloads/file.zip");
        // WHEN
        URI result = MapConversions.toURI(map, realConverter);
        // THEN
        assertEquals(new URI("ftp://fileserver/downloads/file.zip"), result);
    }

    @Test
    @DisplayName("toURI builds non‑hierarchical URI (mailto) via fallback")
    void toURI_FallbackMailtoScheme() throws Exception {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("scheme", "mailto");
        map.put("path", "user@example.com");
        // WHEN
        URI result = MapConversions.toURI(map, realConverter);
        // THEN
        assertEquals(new URI("mailto:user@example.com"), result);
    }

    @Test
    @DisplayName("toURI throws IllegalArgumentException when map provides no data for URI construction")
    void toURI_FallbackEmptyMapThrows() {
        // GIVEN
        Map<String, Object> empty = new HashMap<>();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toURI(empty, realConverter));
    }

    @Test
    @DisplayName("Whitespace‑only 'URI' value triggers fallback and converter is not invoked")
    void toURI_WhitespaceOnlyUriTriggersFallback() throws Exception {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // whitespace only
        map.put(URI_KEY, "   \t   ");
        map.put("scheme", "https");
        map.put("host", "whitespace.com");
        map.put("path", "/index.html");
        // WHEN
        URI result = MapConversions.toURI(map, mockConverter);
        // THEN
        assertEquals(new URI("https://whitespace.com/index.html"), result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("Non‑String value for 'URI' key results in ClassCastException")
    void toURI_NonStringUriValueThrowsClassCastException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // Integer, not String
        map.put(URI_KEY, 12345);
        map.put("scheme", "http");
        map.put("host", "example.org");
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toURI(map, realConverter));
    }

    @Test
    @DisplayName("Null map argument leads to NullPointerException")
    void toURI_NullMapThrowsNPE() {
        // GIVEN
        Object nullMap = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toURI(nullMap, realConverter));
    }

    @Test
    @DisplayName("Null converter argument causes NPE when 'URI' entry is present")
    void toURI_NullConverterWithUriPresentThrowsNPE() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(URI_KEY, "http://nullconverter.test");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toURI(map, null));
    }

    @Test
    @DisplayName("URI key present – other map entries are ignored")
    void toURI_IgnoresOtherEntriesWhenUriPresent() throws Exception {
        // GIVEN
        String uriString = "http://priority.example.com/overridden";
        Map<String, Object> map = new HashMap<>();
        map.put(URI_KEY, uriString);
        map.put("scheme", "http");
        map.put("host", "should.not.use");
        map.put("path", "/ignored");
        when(mockConverter.convert(uriString, URI.class)).thenReturn(new URI(uriString));
        // WHEN
        URI result = MapConversions.toURI(map, mockConverter);
        // THEN
        assertEquals(new URI(uriString), result);
        verify(mockConverter).convert(uriString, URI.class);
    }

    @Test
    @DisplayName("Map implementation subclass (CompactMap) works like a regular Map")
    void toURI_CompactMapSubclassWorks() throws Exception {
        // GIVEN
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("scheme", "http");
        map.put("host", "compact.example");
        map.put("path", "/compact");
        // WHEN
        URI result = MapConversions.toURI(map, realConverter);
        // THEN
        assertEquals(new URI("http://compact.example/compact"), result);
    }

    @Test
    @DisplayName("Map contains both URI and URL keys – URI takes precedence")
    void toURI_UriTakesPrecedenceOverUrl() throws Exception {
        // GIVEN
        String uriString = "https://preferred.example/path";
        String urlString = "https://ignored.example/other";
        Map<String, Object> map = new HashMap<>();
        map.put(URI_KEY, uriString);
        // not the constant key, but still present
        map.put("URL", urlString);
        when(mockConverter.convert(uriString, URI.class)).thenReturn(new URI(uriString));
        // WHEN
        URI result = MapConversions.toURI(map, mockConverter);
        // THEN
        assertEquals(new URI(uriString), result);
        verify(mockConverter).convert(uriString, URI.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    @DisplayName("Non‑Map source argument causes ClassCastException (List)")
    void toURI_NonMapSourceListThrowsClassCastException() {
        // GIVEN
        List<String> notAMap = Arrays.asList("not", "a", "map");
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toURI(notAMap, realConverter));
    }

    @Test
    @DisplayName("Non‑Map source argument causes ClassCastException (String)")
    void toURI_NonMapSourceStringThrowsClassCastException() {
        // GIVEN
        String notAMap = "just a plain string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toURI(notAMap, realConverter));
    }

    @Test
    @DisplayName("Null converter with fallback to fromMap triggers NullPointerException")
    void toURI_NullConverterFallbackThrowsNPE() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("scheme", "http");
        map.put("host", "nullconverter.example");
        map.put("path", "/resource");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toURI(map, null));
    }
}
