package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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

    private Converter converter;

    private URL sampleUrl;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The Converter is not used by UrlConversions.toMap, but we provide a real instance
        // to satisfy the method signature.
        converter = new Converter(new ConverterOptions());
        sampleUrl = new URL("http://example.com/path?query=1");
    }

    /**
     * Supplies normal‑operation test data.
     *
     * @return a stream of arguments: {@link URL} and {@link Converter}
     */
    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// Real Converter instance
        Arguments.of(createUrl("http://example.com"), new Converter(new ConverterOptions())), // Null Converter (method should ignore it)
        Arguments.of(createUrl("https://host/path?query=1"), null));
    }

    private static Stream<Arguments> nullSourceProvider() {
        return Stream.of(Arguments.of((Object) null));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(// a plain String
        Arguments.of("not a url"));
    }

    /**
     * Utility to create a {@link URL} instance without having to handle the
     * checked {@link java.net.MalformedURLException} in every test case.
     *
     * @param spec the URL string
     * @return a {@link URL} instance
     */
    private static URL createUrl(String spec) {
        try {
            return new URL(spec);
        } catch (Exception e) {
            // In test code we want failures to be loud – rethrow as unchecked.
            throw new RuntimeException("Invalid URL used in test data: " + spec, e);
        }
    }

    @Test
    void toMap_returnsMapContainingUrlString() {
        // given
        Object from = sampleUrl;
        // when
        Map<String, Object> result = UrlConversions.toMap(from, converter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(sampleUrl.toString(), result.get(URL_KEY), "Map entry under URL_KEY must match the URL's string representation");
        // Verify that the map implementation preserves insertion order (CompactMap does)
        assertTrue(result instanceof CompactMap, "Result should be an instance of CompactMap");
    }

    @Test
    void toMap_nullUrl_throwsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> UrlConversions.toMap(from, converter), "Passing a null URL should result in NullPointerException");
    }

    @Test
    void toMap_invalidType_throwsClassCastException() {
        // given
        Object from = "not a URL";
        // when / then
        assertThrows(ClassCastException.class, () -> UrlConversions.toMap(from, converter), "Passing a non‑URL object should result in ClassCastException");
    }
}
