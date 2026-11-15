package com.cedarsoftware.util.convert;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toURL_Test {

    /**
     * Mocked Converter used for all test cases.
     */
    @Mock
    Converter mockConverter;

    /**
     * Common URL instance used across tests.
     */
    private URL expectedUrl;

    /**
     * Re‑usable {@link URL} instance for positive test cases.
     */
    private URL exampleUrl;

    /**
     * Keys that may hold a URL value inside a map.
     */
    static final String[] URL_KEYS = {"URL", "value", "_v"};

    @BeforeEach
    void setupBeforeEach() throws MalformedURLException {
        // Build a reusable URL instance; any well‑formed URL is fine.
        expectedUrl = new URL("http://example.com/");
        exampleUrl = new URL("http://example.com/");
        // Default behaviour for successful conversion: any CharSequence is turned into a URL.
        when(mockConverter.convert(any(CharSequence.class), eq(URL.class)))
                .thenAnswer(invocation -> new URL(invocation.getArgument(0).toString()));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test to keep expectations isolated.
        reset(mockConverter);
    }

    private static Stream<Arguments> mapKeyAndValueProvider() throws MalformedURLException {
        URL url = new URL("http://example.com/");
        return Stream.of(
                Arguments.of("URL", "http://example.com/", url),
                Arguments.of("value", "http://example.com/", url),
                Arguments.of("_v", "http://example.com/", url),
                Arguments.of("URL", url, url),
                Arguments.of("value", url, url),
                Arguments.of("_v", url, url)
        );
    }

    private static Stream<Arguments> nullValueMapProvider() {
        return Stream.of(
                Arguments.of("URL"),
                Arguments.of("value")
        );
    }

    /**
     * Convert the supplied {@code source} into a {@link URL} using the provided {@link Converter}.
     *
     * @param source    the object to be converted; may be {@code null}
     * @param converter the converter that knows how to turn non‑URL objects into a {@link URL}
     * @return a {@link URL} instance or {@code null}
     * @throws IllegalArgumentException if the conversion cannot be performed
     */
    public static URL toURL(Object source, Converter converter) {
        // 1. Null → null (no conversion)
        if (source == null) {
            return null;
        }
        // 2. Already a URL → return as‑is (no converter interaction)
        if (source instanceof URL) {
            return (URL) source;
        }
        // 3. CharSequence (String, StringBuilder, etc.) → delegate to the converter
        if (source instanceof CharSequence) {
            return converter.convert(source, URL.class);
        }
        // 4. URI → delegate to the converter (the converter may simply call uri.toURL())
        if (source instanceof URI) {
            return converter.convert(source, URL.class);
        }
        // 5. Map handling – look for the first matching key in URL_KEYS order
        if (source instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) source;
            for (String key : URL_KEYS) {
                if (map.containsKey(key)) {
                    Object value = map.get(key);
                    // Null value → null (nothing to convert)
                    if (value == null) {
                        return null;
                    }
                    // If the value is already a URL, return it directly.
                    if (value instanceof URL) {
                        return (URL) value;
                    }
                    // Otherwise ask the converter to turn the value into a URL.
                    return converter.convert(value, URL.class);
                }
            }
            // No recognised key – nothing to convert.
            return null;
        }
        // 6. Fallback – let the converter try to handle any other type.
        return converter.convert(source, URL.class);
    }

    @Test
    void toURL_givenString_callsConverterAndReturnsResult() {
        // GIVEN
        String source = "http://example.com/";
        when(mockConverter.convert(eq(source), eq(URL.class))).thenReturn(expectedUrl);
        // WHEN
        URL actual = MapConversions.toURL(source, mockConverter);
        // THEN
        assertEquals(expectedUrl, actual, "The URL returned should be the one supplied by the Converter");
        verify(mockConverter, times(1)).convert(eq(source), eq(URL.class));
    }

    @Test
    void toURL_givenURL_returnsSameInstanceWithoutConverterCall() {
        // GIVEN
        URL source = expectedUrl;
        // WHEN
        URL actual = MapConversions.toURL(source, mockConverter);
        // THEN
        assertSame(source, actual, "When the source is already a URL, the same instance should be returned");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toURL_givenMapWithValueKeyExtractsAndConverts() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("value", "http://example.com/");
        when(mockConverter.convert(eq("http://example.com/"), eq(URL.class))).thenReturn(expectedUrl);
        // WHEN
        URL actual = MapConversions.toURL(map, mockConverter);
        // THEN
        assertEquals(expectedUrl, actual, "Map with a 'value' key should be converted to the expected URL");
        verify(mockConverter, times(1)).convert(eq("http://example.com/"), eq(URL.class));
    }

    @Test
    void toURL_givenMapWithAlternativeKey_VOr_UriKeyAreHandled() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "http://example.com/");
        when(mockConverter.convert(eq("http://example.com/"), eq(URL.class))).thenReturn(expectedUrl);
        // WHEN
        URL actual = MapConversions.toURL(map, mockConverter);
        // THEN
        assertEquals(expectedUrl, actual, "Map with a '_v' key should be converted to the expected URL");
        verify(mockConverter, times(1)).convert(eq("http://example.com/"), eq(URL.class));
    }

    @Test
    void toURL_givenDirectURLInstance_returnsSameInstanceWithoutConverterCall() throws Exception {
        URL source = new URL("http://example.com/");
        URL result = MapConversions.toURL(source, mockConverter);
        assertSame(source, result, "When the source is already a URL the same instance must be returned");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] key={0}")
    @MethodSource("mapKeyAndValueProvider")
    void toURL_givenMapWithKey_convertsAccordingToKey(String key, Object mapValue, URL expected) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(key, mapValue);
        URL result = MapConversions.toURL(map, mockConverter);
        assertEquals(expected, result);
        if (mapValue instanceof URL) {
            verifyNoInteractions(mockConverter);
        } else {
            verify(mockConverter, times(1)).convert(eq(mapValue), eq(URL.class));
        }
    }

    @Test
    void toURL_givenInvalidString_throwsException() {
        String bad = "not a valid url";
        when(mockConverter.convert(eq(bad), eq(URL.class))).thenThrow(new IllegalArgumentException("Invalid URL"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toURL(bad, mockConverter));
        assertTrue(ex.getMessage().contains("Invalid URL"));
        verify(mockConverter, times(1)).convert(eq(bad), eq(URL.class));
    }

    @Test
    void toURL_givenNull_returnsNull() {
        assertNull(MapConversions.toURL(null, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toURL_givenUnsupportedType_throwsException() {
        Integer source = 12345;
        when(mockConverter.convert(eq(source), eq(URL.class)))
                .thenThrow(new IllegalArgumentException("Cannot convert Integer to URL"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toURL(source, mockConverter));
        assertTrue(ex.getMessage().contains("Integer"));
        verify(mockConverter, times(1)).convert(eq(source), eq(URL.class));
    }

    @Test
    void toURL_mapPrecedence_URLKeyWinsOverValue() throws Exception {
        Map<String, Object> map = Map.of("URL", "http://first.com", "value", "http://second.com");
        URL result = MapConversions.toURL(map, mockConverter);
        assertEquals(new URL("http://first.com"), result);
        verify(mockConverter, times(1)).convert(eq("http://first.com"), eq(URL.class));
    }

    @Test
    void toURL_mapPrecedence_ValueKeyWinsWhenURLMissing() throws Exception {
        Map<String, Object> map = Map.of("value", "http://second.com", "_v", "http://third.com");
        URL result = MapConversions.toURL(map, mockConverter);
        assertEquals(new URL("http://second.com"), result);
        verify(mockConverter, times(1)).convert(eq("http://second.com"), eq(URL.class));
    }

    @Test
    void toURL_mapPrecedence_VKeyUsedWhenOthersMissing() throws Exception {
        Map<String, Object> map = Map.of("_v", "http://third.com");
        URL result = MapConversions.toURL(map, mockConverter);
        assertEquals(new URL("http://third.com"), result);
        verify(mockConverter, times(1)).convert(eq("http://third.com"), eq(URL.class));
    }

    @ParameterizedTest(name = "[{index}] key={0}")
    @MethodSource("nullValueMapProvider")
    void toURL_mapKeyWithNullValue_returnsNull(String key) {
        Map<String, Object> map = Map.of(key, null);
        assertNull(MapConversions.toURL(map, mockConverter));
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toURL_givenValidURI_convertsToURL() throws Exception {
        URI uri = new URI("https://uri-to-url.com/path");
        URL result = MapConversions.toURL(uri, mockConverter);
        assertEquals(uri.toURL(), result);
        verify(mockConverter, times(1)).convert(eq(uri), eq(URL.class));
    }

    @Test
    void toURL_givenInvalidURI_throwsException() {
        URI badUri = URI.create("invalid uri");
        when(mockConverter.convert(eq(badUri), eq(URL.class)))
                .thenThrow(new IllegalArgumentException("Cannot convert URI"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toURL(badUri, mockConverter));
        assertTrue(ex.getMessage().contains("URI"));
        verify(mockConverter, times(1)).convert(eq(badUri), eq(URL.class));
    }

    @Test
    void toURL_customConverterNotInvokedWhenMapValueIsAlreadyURL() throws Exception {
        // A converter that throws if its convert method is ever called.
        Converter explodingConverter = mock(Converter.class);
        doThrow(new AssertionError("Converter should not be invoked"))
                .when(explodingConverter).convert(any(), any());
        Map<String, Object> map = Map.of("URL", new URL("http://already.url"));
        URL result = MapConversions.toURL(map, explodingConverter);
        assertEquals(new URL("http://already.url"), result);
        // No interaction verification needed – the test will fail if convert() is called.
    }

    @Test
    void toURL_givenStringBuilderValid_converts() throws Exception {
        StringBuilder sb = new StringBuilder("http://builder.com");
        URL result = MapConversions.toURL(sb, mockConverter);
        assertEquals(new URL("http://builder.com"), result);
        verify(mockConverter, times(1)).convert(eq(sb), eq(URL.class));
    }

    @Test
    void toURL_givenStringBuilderInvalid_throwsException() {
        StringBuilder sb = new StringBuilder("not a url");
        when(mockConverter.convert(eq(sb), eq(URL.class)))
                .thenThrow(new IllegalArgumentException("Malformed URL"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toURL(sb, mockConverter));
        assertTrue(ex.getMessage().contains("Malformed"));
        verify(mockConverter, times(1)).convert(eq(sb), eq(URL.class));
    }

    @Test
    void toURL_givenEmptyString_throwsException() {
        String empty = "";
        when(mockConverter.convert(eq(empty), eq(URL.class)))
                .thenThrow(new IllegalArgumentException("Empty URL"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toURL(empty, mockConverter));
        assertTrue(ex.getMessage().contains("Empty"));
        verify(mockConverter, times(1)).convert(eq(empty), eq(URL.class));
    }

    @Test
    void toURL_givenWhitespaceString_trimsAndConverts() throws Exception {
        String spaced = "   http://spaced.com   ";
        when(mockConverter.convert(eq(spaced), eq(URL.class)))
                .thenAnswer(inv -> new URL(inv.getArgument(0).toString().trim()));
        URL result = MapConversions.toURL(spaced, mockConverter);
        assertEquals(new URL("http://spaced.com"), result);
        verify(mockConverter, times(1)).convert(eq(spaced), eq(URL.class));
    }

    @Test
    void toURL_mapKeyWithUnsupportedValue_throwsException() {
        Map<String, Object> map = Map.of("URL", 12345);
        when(mockConverter.convert(eq(12345), eq(URL.class)))
                .thenThrow(new IllegalArgumentException("Integer cannot be converted"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toURL(map, mockConverter));
        assertTrue(ex.getMessage().contains("Integer"));
        verify(mockConverter, times(1)).convert(eq(12345), eq(URL.class));
    }

    @Test
    void toURL_mapKeyWithMalformedString_throwsException() {
        Map<String, Object> map = Map.of("URL", "http://%zz");
        when(mockConverter.convert(eq("http://%zz"), eq(URL.class)))
                .thenThrow(new IllegalArgumentException("Malformed URL"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toURL(map, mockConverter));
        assertTrue(ex.getMessage().contains("Malformed"));
        verify(mockConverter, times(1)).convert(eq("http://%zz"), eq(URL.class));
    }
}