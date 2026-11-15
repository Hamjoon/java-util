package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toURI_Test {

    @Mock
    private Converter // mocked Converter dependency
    converter;

    // map that will be passed to toURI
    private Map<String, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test
        map = new HashMap<>();
    }

    private static Stream<RuntimeException> converterExceptionProvider() {
        return Stream.of(new RuntimeException("boom"), new IllegalArgumentException("Invalid URI"), new NullPointerException("converter NPE"));
    }

    @Test
    void toURI_WithNonEmptyUriString_UsesConverter() throws Exception {
        // GIVEN
        String uriString = "http://example.com/path?query=1";
        map.put(MapConversions.URI_KEY, uriString);
        URI expected = new URI(uriString);
        when(converter.convert(uriString, URI.class)).thenReturn(expected);
        // WHEN
        URI actual = MapConversions.toURI(map, converter);
        // THEN
        assertEquals(expected, actual, "The URI returned should be the one provided by the Converter");
        verify(converter, times(1)).convert(uriString, URI.class);
    }

    @Test
    void toURI_WithEmptyUriString_FallsBackToFromMap_ReturnsNull() {
        // empty string
        map.put(MapConversions.URI_KEY, "");
        // invoke private fromMap to know what it would return for this input
        Object fallback = invokeFromMap(map, URI.class, new String[] { MapConversions.URI_KEY });
        // WHEN
        URI result = MapConversions.toURI(map, converter);
        // THEN
        assertEquals(fallback, result, "Result should be whatever fromMap returns (null in current implementation)");
        verifyNoInteractions(converter);
    }

    @Test
    void toURI_WithMissingUriKey_FallsBackToFromMap_ReturnsNull() {
        // map does NOT contain the "URI" key at all
        Object fallback = invokeFromMap(map, URI.class, new String[] { MapConversions.URI_KEY });
        URI result = MapConversions.toURI(map, converter);
        assertEquals(fallback, result);
        verifyNoInteractions(converter);
    }

    @Test
    void toURI_WithWhitespaceUriString_FallsBackToFromMap_ReturnsNull() throws Exception {
        // whitespace only
        map.put(MapConversions.URI_KEY, "   ");
        Object fallback = invokeFromMap(map, URI.class, new String[] { MapConversions.URI_KEY });
        URI result = MapConversions.toURI(map, converter);
        assertEquals(fallback, result);
        verifyNoInteractions(converter);
    }

    /* --------------------------------------------------------------
     *  Exceptional / error cases.
     * -------------------------------------------------------------- */
    @Test
    void toURI_WithUriValueWrongType_ThrowsClassCastException() {
        // not a String
        map.put(MapConversions.URI_KEY, 12345);
        assertThrows(ClassCastException.class, () -> MapConversions.toURI(map, converter), "A non‑String value for the URI key should cause a ClassCastException");
    }

    @Test
    void toURI_WithNullMap_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toURI(null, converter), "Passing null as the map should result in NPE");
    }

    @Test
    void toURI_WithNullConverter_ThrowsNullPointerException() {
        map.put(MapConversions.URI_KEY, "http://example.com");
        assertThrows(NullPointerException.class, () -> MapConversions.toURI(map, null), "When the converter is null a NPE should be thrown");
    }

    @Test
    void toURI_WithNonMapFrom_ThrowsClassCastException() {
        String notAMap = "just a plain string";
        assertThrows(ClassCastException.class, () -> MapConversions.toURI(notAMap, converter), "Providing a non‑Map as the first argument should cause a ClassCastException");
    }

    @ParameterizedTest
    @MethodSource("converterExceptionProvider")
    void toURI_WhenConverterThrows_ExceptionPropagates(RuntimeException toThrow) {
        String uriString = "http://example.com/boom";
        map.put(MapConversions.URI_KEY, uriString);
        when(converter.convert(uriString, URI.class)).thenThrow(toThrow);
        RuntimeException thrown = assertThrows(toThrow.getClass(), () -> MapConversions.toURI(map, converter));
        assertEquals(toThrow.getMessage(), thrown.getMessage(), "Exception message should be preserved");
        verify(converter, times(1)).convert(uriString, URI.class);
    }

    @Test
    void toURI_ConverterReturnsNull_ReturnsNull() {
        String uriString = "http://example.com/none";
        map.put(MapConversions.URI_KEY, uriString);
        when(converter.convert(uriString, URI.class)).thenReturn(null);
        URI result = MapConversions.toURI(map, converter);
        assertNull(result, "If the converter returns null, toURI should return null");
        verify(converter, times(1)).convert(uriString, URI.class);
    }

    @Test
    void toURI_ConverterReturnsWrongType_ThrowsClassCastException() {
        String uriString = "http://example.com/wrong";
        map.put(MapConversions.URI_KEY, uriString);
        // Mock returns a String instead of URI – this will cause a ClassCastException
        when(converter.convert(uriString, URI.class)).thenReturn("not a URI");
        assertThrows(ClassCastException.class, () -> MapConversions.toURI(map, converter), "Returning a non‑URI from the converter should cause a ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Helper to invoke the private static fromMap method via reflection.
     * -------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static <T> T invokeFromMap(Object from, Class<T> type, String[]... keySets) throws Exception {
        Method fromMap = MapConversions.class.getDeclaredMethod("fromMap", Object.class, Converter.class, Class.class, String[][].class);
        fromMap.setAccessible(true);
        // Pass a dummy converter because fromMap may call converter internally; null is fine for our current use‑cases
        return (T) fromMap.invoke(null, from, null, type, keySets);
    }
}
