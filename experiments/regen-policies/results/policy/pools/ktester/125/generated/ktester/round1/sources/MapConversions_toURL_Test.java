package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.net.URL;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toURL_Test {

    @Mock
    Converter // mocked Converter used in all tests
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test to avoid cross‑test interference.
        Mockito.reset(mockConverter);
        Mockito.reset(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Helper methods
     * ------------------------------------------------------------- */
    private static Map<String, Object> map(Object... keyVals) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            m.put((String) keyVals[i], keyVals[i + 1]);
        }
        return m;
    }

    private static Stream<Arguments> fallbackProvider() {
        return Stream.of(// empty string -> fallback to "url"
        Arguments.of("Empty string, fallback to \"url\" key", map(MapConversions.URL_KEY, "", "url", "https://fallback.example.org"), "url", "https://fallback.example.org"), // whitespace only -> fallback to "url"
        Arguments.of("Whitespace‑only string, fallback to \"url\" key", map(MapConversions.URL_KEY, "   ", "url", "https://fallback.example.org/whitespace"), "url", "https://fallback.example.org/whitespace"), // null value -> fallback to "uri"
        Arguments.of("Null value, fallback to \"uri\" key", map(MapConversions.URL_KEY, null, "uri", "https://fallback.example.org/null"), "uri", "https://fallback.example.org/null"), // URL key absent -> fallback to "uri"
        Arguments.of("Missing \"URL\" key, fallback to \"uri\" key", map("uri", "https://only-fallback.com/resource"), "uri", "https://only-fallback.com/resource"));
    }

    /**
     * Verify that when the map contains a non‑empty {@code "URL"} entry,
     * {@code MapConversions.toURL} delegates to {@link Converter#convert(Object, Class)}
     * and returns the resulting {@link URL}.
     */
    @Test
    void toURL_usesConverterWhenUrlPresent() throws Exception {
        // GIVEN
        String urlString = "http://example.com";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.URL_KEY, urlString);
        URL expected = new URL(urlString);
        when(mockConverter.convert(urlString, URL.class)).thenReturn(expected);
        // WHEN
        URL actual = MapConversions.toURL(map, mockConverter);
        // THEN
        assertEquals(expected, actual, "The returned URL should be the one produced by the Converter");
        verify(mockConverter, times(1)).convert(urlString, URL.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Verify that when the {@code "URL"} entry is empty (or absent),
     * {@code MapConversions.toURL} does **not** call {@link Converter#convert}
     * and falls back to the internal {@code fromMap} handling, which for an empty
     * value results in {@code null}.
     */
    @Test
    void toURL_fallsBackWhenUrlEmpty() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // empty string -> hasContent == false
        map.put(MapConversions.URL_KEY, "");
        // WHEN
        URL result = MapConversions.toURL(map, mockConverter);
        // THEN
        assertNull(result, "When the URL string is empty, the conversion should return null");
        // converter.convert must not be invoked
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  2. Fallback handling – empty, whitespace or null "URL"
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("fallbackProvider")
    void toURL_fallbackScenarios(String description, Map<String, Object> inputMap, String fallbackKey, String fallbackValue) throws Exception {
        // GIVEN
        URL expected = new URL(fallbackValue);
        when(mockConverter.convert(fallbackValue, URL.class)).thenReturn(expected);
        // WHEN
        URL actual = MapConversions.toURL(inputMap, mockConverter);
        // THEN
        assertEquals(expected, actual, "Fallback conversion should produce the expected URL");
        verify(mockConverter, times(1)).convert(fallbackValue, URL.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  3. Primary key takes precedence over alternatives
     * ------------------------------------------------------------- */
    @Test
    void toURL_primaryKeyPrecedenceOverAlternative() throws Exception {
        // GIVEN
        String primary = "https://primary.example.com";
        String secondary = "https://secondary.example.com";
        Map<String, Object> map = map(MapConversions.URL_KEY, primary, "url", secondary);
        URL expected = new URL(primary);
        when(mockConverter.convert(primary, URL.class)).thenReturn(expected);
        // WHEN
        URL actual = MapConversions.toURL(map, mockConverter);
        // THEN
        assertEquals(expected, actual, "Primary \"URL\" entry must be used, alternatives ignored");
        verify(mockConverter, times(1)).convert(primary, URL.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  4. Converter throws – exception propagates
     * ------------------------------------------------------------- */
    @Test
    void toURL_converterThrowsRuntimeException() {
        // GIVEN
        String badUrl = "invalid://url";
        Map<String, Object> map = map(MapConversions.URL_KEY, badUrl);
        when(mockConverter.convert(badUrl, URL.class)).thenThrow(new IllegalArgumentException("bad URL"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toURL(map, mockConverter));
        assertEquals("bad URL", ex.getMessage());
        verify(mockConverter, times(1)).convert(badUrl, URL.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  5. Converter returns null – result is null
     * ------------------------------------------------------------- */
    @Test
    void toURL_converterReturnsNull() {
        // GIVEN
        String urlStr = "http://example.com/null";
        Map<String, Object> map = map(MapConversions.URL_KEY, urlStr);
        when(mockConverter.convert(urlStr, URL.class)).thenReturn(null);
        // WHEN
        URL result = MapConversions.toURL(map, mockConverter);
        // THEN
        assertNull(result, "When converter returns null, toURL should return null");
        verify(mockConverter, times(1)).convert(urlStr, URL.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  6. Null map argument – NPE
     * ------------------------------------------------------------- */
    @Test
    void toURL_nullMapArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toURL(null, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  7. Non‑Map from argument – ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void toURL_nonMapFromArgumentThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> MapConversions.toURL("just a string, not a map", mockConverter));
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  8. URL value of wrong type – ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void toURL_urlValueNotStringThrowsClassCastException() {
        Map<String, Object> map = map(MapConversions.URL_KEY, 12345);
        assertThrows(ClassCastException.class, () -> MapConversions.toURL(map, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  9. Null converter argument – NPE after URL detection
     * ------------------------------------------------------------- */
    @Test
    void toURL_nullConverterThrowsNPE() {
        Map<String, Object> map = map(MapConversions.URL_KEY, "http://example.com");
        assertThrows(NullPointerException.class, () -> MapConversions.toURL(map, null));
    }

    /* -------------------------------------------------------------
     * 10. Converter returns wrong type – ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void toURL_converterReturnsWrongTypeThrowsClassCastException() {
        String urlStr = "http://example.com/wrongtype";
        Map<String, Object> map = map(MapConversions.URL_KEY, urlStr);
        // Integer instead of URL
        when(mockConverter.convert(urlStr, URL.class)).thenReturn(123);
        assertThrows(ClassCastException.class, () -> MapConversions.toURL(map, mockConverter));
        verify(mockConverter, times(1)).convert(urlStr, URL.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     * 11. No URL or fallback keys – IllegalArgumentException from fromMap
     * ------------------------------------------------------------- */
    @Test
    void toURL_noKeysThrowsIllegalArgumentException() {
        Map<String, Object> emptyMap = Collections.emptyMap();
        // The exact exception type thrown by fromMap for a missing key is not public;
        // we assert that an IllegalArgumentException (or subclass) is thrown.
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toURL(emptyMap, mockConverter));
        verifyNoInteractions(mockConverter);
    }
}
