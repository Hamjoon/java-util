package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.CompactMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UriConversions_toMap_Test {

    @Mock
    Converter // Converter is not used by the method but required for the signature
    mockConverter;

    private static Stream<Arguments> provideValidUriCases() {
        return Stream.of(// basic functionality
        Arguments.of(URI.create("http://example.com/path?query=1"), "http://example.com/path?query=1", "Standard HTTP URI"), Arguments.of(URI.create(""), "", "Empty URI string"), Arguments.of(URI.create("file:///C:/temp/file.txt"), "file:///C:/temp/file.txt", "File scheme URI"), // additional scenarios
        Arguments.of(URI.create("http://example.com/a%20b?c=1#frag"), "http://example.com/a%20b?c=1#frag", "Percent‑encoded space, query and fragment"), Arguments.of(URI.create("mailto:john.doe@example.com"), "mailto:john.doe@example.com", "Opaque mailto URI"), Arguments.of(URI.create("/relative/path"), "/relative/path", "Relative URI without scheme"), Arguments.of(URI.create("http://[2001:db8::1]/resource"), "http://[2001:db8::1]/resource", "IPv6 host literal"), Arguments.of(URI.create("http://example.com:8080/path"), "http://example.com:8080/path", "Explicit port number"), Arguments.of(URI.create("http://user:pass@example.com"), "http://user:pass@example.com", "User‑info in URI"));
    }

    @Test
    void toMap_returnsMapContainingUriString() {
        // GIVEN – a valid URI (converter can be null or the mock; both behave the same)
        // explicit null to emphasise that it is ignored
        Converter converter = null;
        // WHEN
        Map<String, Object> result = UriConversions.toMap(inputUri, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(URI_KEY), "Map should contain the URI_KEY");
        assertEquals(expectedString, result.get(URI_KEY), "Map value must match the URI string");
    }

    @Test
    void toMap_withNonUriObject_throwsClassCastException() {
        // GIVEN
        Object notAUri = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UriConversions.toMap(notAUri, mockConverter), "Passing a non‑URI object should result in ClassCastException");
    }

    @Test
    void toMap_withNullObject_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UriConversions.toMap(nullObj, mockConverter), "Passing null should result in NullPointerException due to casting");
    }

    @Test
    void toMap_returnsCompactMapInstance() {
        // GIVEN
        URI uri = URI.create("http://example.com");
        // WHEN
        Map<String, Object> result = UriConversions.toMap(uri, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof CompactMap, "Returned map should be an instance of CompactMap");
    }

    @Test
    void toMap_isPackagePrivateAndInvocableViaReflection() throws Exception {
        // This test demonstrates that the method can be accessed reflectively,
        // which is useful when the visibility changes in the future.
        Method method = UriConversions.class.getDeclaredMethod("toMap", Object.class, Converter.class);
        method.setAccessible(true);
        URI uri = new URI("http://reflection.test/");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) method.invoke(null, uri, mockConverter);
        assertEquals(uri.toString(), map.get(URI_KEY));
    }
}
