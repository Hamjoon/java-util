package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UrlConversions_toMap_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    private static Stream<Arguments> validUrlProvider() throws MalformedURLException {
        return Stream.of(Arguments.of(new URL("http://example.com")), Arguments.of(new URL("https://example.com/path?query=1#frag")));
    }

    @Test
    void toMap_givenUrl_returnsMapContainingUrlString() throws MalformedURLException {
        // given
        URL url = new URL("http://example.com/path?query=123");
        // when
        Map<String, Object> result = UrlConversions.toMap(url, converter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result instanceof CompactLinkedMap, "Implementation should be CompactLinkedMap");
        assertEquals(url.toString(), result.get(URL_KEY), "Map value must be the URL's string representation");
    }

    @Test
    void toMap_givenNonUrl_throwsClassCastException() {
        // given
        Object notAUrl = "just a plain string";
        // when / then
        assertThrows(ClassCastException.class, () -> UrlConversions.toMap(notAUrl, converter), "Passing a non‑URL object should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] URL = {0}")
    @MethodSource("validUrlProvider")
    void toMap_givenValidUrl_returnsMapContainingUrlString(URL url) {
        // when
        Map<String, Object> result = UrlConversions.toMap(url, null);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result instanceof CompactLinkedMap, "Implementation should be CompactLinkedMap");
        assertEquals(url.toString(), result.get(URL_KEY), "Map value must be the URL's string representation");
    }

    @Test
    void toMap_givenUrlWithExplicitPort_returnsMapContainingFullUrl() throws MalformedURLException {
        // given
        URL url = new URL("http://example.com:8080");
        // when
        Map<String, Object> result = UrlConversions.toMap(url, null);
        // then
        assertNotNull(result);
        assertEquals(url.toString(), result.get(URL_KEY));
    }

    @Test
    void toMap_givenNullSource_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> UrlConversions.toMap(null, null), "Passing null as the source should result in NullPointerException");
    }

    @Test
    void toMap_givenNonNullConverter_ignoresConverterAndReturnsMap() throws MalformedURLException {
        // given
        URL url = new URL("http://example.org");
        // 'converter' mock is already injected by Mockito; it is non‑null here
        // when
        Map<String, Object> result = UrlConversions.toMap(url, converter);
        // then
        assertNotNull(result);
        assertEquals(url.toString(), result.get(URL_KEY), "Even with a non‑null Converter, the map should contain the URL string");
    }

    @Test
    void toMap_returnsCompactLinkedMapInstance() throws MalformedURLException {
        URL url = new URL("http://example.com");
        Map<String, Object> result = UrlConversions.toMap(url, null);
        assertTrue(result instanceof CompactLinkedMap, "The returned map should be an instance of CompactLinkedMap");
    }
}
