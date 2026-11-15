package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.net.URL;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toURL_Test {

    /**
     * Mocked {@link Converter} used for all test cases.
     */
    @Mock
    private Converter converter;

    /**
     * Holds a reference to the mock for convenience.
     */
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the mock before each test (MockitoExtension already injects it).
        this.mockConverter = converter;
        this.mockConverter = converter;
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to keep each test isolated.
        clearInvocations(mockConverter);
        // Reset mock interactions between tests.
        clearInvocations(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Fallback path – the map does NOT contain a usable URL string.
     * -------------------------------------------------------------- */
    private static Stream<Arguments> fallbackScenarios() {
        return Stream.of(// missing key
        Arguments.of(Map.of("someOtherKey", "value"), URL.create("http://fallback.com")), // key present but null value
        Arguments.of(Map.of("URL", null, "other", "x"), URL.create("http://fallback-null.com")), // key present but empty string
        Arguments.of(Map.of("URL", ""), URL.create("http://fallback-empty.com")), // key present but whitespace only
        Arguments.of(Map.of("URL", "   "), URL.create("http://blank.com")));
    }

    /**
     * When the map contains a non‑empty {@code "URL"} entry the method should delegate
     * to {@link Converter#convert(Object, Class)} and return the converted {@link URL}.
     */
    @Test
    void toURL_WithValidUrlString_DelegatesToConverter() throws Exception {
        // GIVEN
        String urlString = "http://example.com";
        URL expectedUrl = new URL(urlString);
        Map<String, Object> map = new HashMap<>();
        map.put("URL", urlString);
        // Configure the mock to return the expected URL when asked to convert.
        when(mockConverter.convert(urlString, URL.class)).thenReturn(expectedUrl);
        // WHEN
        URL result = MapConversions.toURL(map, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedUrl, result, "Returned URL should be the one supplied by the converter");
        // Verify that the converter was invoked exactly once with the correct arguments.
        verify(mockConverter, times(1)).convert(urlString, URL.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * When the map does not contain a {@code "URL"} key (or the value is empty),
     * the method should *not* call {@link Converter#convert(Object, Class)}.
     * The private {@code fromMap} implementation will be used internally.
     */
    @Test
    void toURL_WithoutUrlKey_DoesNotCallConverter() {
        // GIVEN
        // empty map – no "URL" entry
        Map<String, Object> map = new HashMap<>();
        // WHEN
        URL result = MapConversions.toURL(map, mockConverter);
        // THEN
        // The contract of the private fallback is not defined here; we only assert that
        // the converter's convert method was never called.
        assertNull(result, "When no URL key is present the fallback returns null (as per current implementation)");
        verifyNoInteractions(mockConverter);
    }

    /**
     * When the map contains a {@code "URL"} entry whose value is an empty string,
     * the method should treat it as missing and therefore not invoke the converter.
     */
    @Test
    void toURL_WithEmptyUrlString_DoesNotCallConverter() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // whitespace only – considered empty by StringUtilities
        map.put("URL", "   ");
        // WHEN
        URL result = MapConversions.toURL(map, mockConverter);
        // THEN
        assertNull(result, "Empty URL string should be treated as absent and result in null");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest
    @MethodSource("fallbackScenarios")
    void toURL_FallbackScenarios_ReturnsConverterResult(Map<String, Object> map, URL expected) {
        // The converter is asked to convert *something* to URL.  We do not know the exact argument
        // because the private {@code fromMap} decides which key(s) to read, therefore we accept any.
        when(mockConverter.convert(any(), eq(URL.class))).thenReturn(expected);
        // WHEN
        URL result = MapConversions.toURL(map, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result);
        // Verify that the converter was invoked exactly once (fallback path).
        verify(mockConverter, times(1)).convert(any(), eq(URL.class));
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toURL_NonStringValue_ThrowsClassCastException() {
        // GIVEN: map contains an Integer under the "URL" key.
        Map<String, Object> map = Map.of("URL", 12345);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toURL(map, mockConverter), "Casting a non‑String value to String should throw ClassCastException");
        // No interaction with the converter is expected.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toURL_MalformedUrlString_PropagatesConverterException() {
        // GIVEN
        String badUrl = "ht!tp://[invalid]";
        Map<String, Object> map = Map.of("URL", badUrl);
        when(mockConverter.convert(badUrl, URL.class)).thenThrow(new IllegalArgumentException("Invalid URL"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toURL(map, mockConverter));
        assertEquals("Invalid URL", ex.getMessage());
        verify(mockConverter, times(1)).convert(badUrl, URL.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toURL_NullFromArgument_ThrowsNullPointerException() {
        // GIVEN a null map reference
        Object nullMap = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toURL(nullMap, mockConverter), "Casting null to Map should throw NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toURL_NonMapFromArgument_ThrowsClassCastException() {
        // GIVEN a List instead of a Map
        List<String> notAMap = List.of("URL", "http://example.com");
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toURL(notAMap, mockConverter), "Casting a non‑Map object to Map should throw ClassCastException");
        verifyNoInteractions(mockConverter);
    }
}
