package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class UriConversions_toMap_Test {

    /**
     * Converter instance – not used by the method under test but required by the signature.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; an empty one is sufficient for this test.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release resources if any (none here).
        converter = null;
    }

    /**
     * Supplies the successful test cases defined in the JSON spec.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> validUriProvider() {
        return Stream.of(arguments("Standard URI conversion", URI.create("http://example.com/path?query=1"), "http://example.com/path?query=1"), arguments("Empty URI string", URI.create(""), ""), arguments("File scheme URI", URI.create("file:///C:/temp/file.txt"), "file:///C:/temp/file.txt"), arguments("Relative URI (no scheme)", URI.create("/relative/path"), "/relative/path"), arguments("Opaque URI (mailto scheme)", URI.create("mailto:someone@example.com"), "mailto:someone@example.com"), arguments("URI with user‑info and explicit port", URI.create("http://user:pass@example.com:8080/path"), "http://user:pass@example.com:8080/path"), arguments("URI with IPv6 host literal", URI.create("http://[2001:db8::1]:8080"), "http://[2001:db8::1]:8080"), arguments("URI containing a fragment identifier", URI.create("http://example.com/path#section"), "http://example.com/path#section"));
    }

    @Test
    void toMap_givenValidUri_returnsMapContainingStringRepresentation() {
        // GIVEN
        URI uri = URI.create("http://example.com/some/path?query=1");
        // WHEN
        Map<String, Object> result = UriConversions.toMap(uri, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(uri.toString(), result.get(MapConversions.URI_KEY), "Map entry should contain the URI string under the correct key");
    }

    @Test
    void toMap_givenNonUriObject_throwsClassCastException() {
        // GIVEN
        Object notAUri = "just a plain string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UriConversions.toMap(notAUri, converter), "Passing a non‑URI object should result in a ClassCastException");
    }

    /**
     * Parameterized test that verifies a variety of valid {@link URI} inputs are
     * transformed into a map containing the URI string under {@link MapConversions#URI_KEY}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validUriProvider")
    void toMap_givenValidUri_returnsExpectedMap(String description, URI uri, String expectedString) {
        // WHEN
        Map<String, Object> result = UriConversions.toMap(uri, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(expectedString, result.get(URI_KEY), "Map entry should contain the correct URI string");
    }

    @Test
    void toMap_givenNullFrom_throwsNullPointerException() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UriConversions.toMap(null, converter), "Passing null as 'from' should result in NullPointerException");
    }

    @Test
    void toMap_givenStringInsteadOfUri_throwsClassCastException() {
        // GIVEN
        Object notAUri = "http://example.com";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UriConversions.toMap(notAUri, converter), "Passing a non‑URI object should result in ClassCastException");
    }

    @Test
    void toMap_givenNonNullConverter_ignoresConverter() {
        // GIVEN
        URI uri = URI.create("http://example.com");
        // Create a mock that would throw if any method is invoked.
        Converter mockConverter = mock(Converter.class);
        doThrow(new RuntimeException("Converter should not be invoked")).when(mockConverter).convert(any(), any());
        // WHEN
        Map<String, Object> result = UriConversions.toMap(uri, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(uri.toString(), result.get(URI_KEY));
        // Verify that the mock was never interacted with.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toMap_givenNullConverter_worksNormally() {
        // GIVEN
        URI uri = URI.create("http://example.com");
        // WHEN
        Map<String, Object> result = UriConversions.toMap(uri, null);
        // THEN
        assertNotNull(result);
        assertEquals(uri.toString(), result.get(URI_KEY));
    }
}
