package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.net.URL;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class UriConversions_toURL_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Supplies the various error cases described in the JSON test specification.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideErrorScenarios() {
        return Stream.of(// Null input – cast succeeds (null) but .toURL() throws NPE
        arguments("Null input (from = null)", null, null, NullPointerException.class), // Invalid type – casting String to URI throws ClassCastException
        arguments("Invalid type (from = String)", "http://example.com", null, ClassCastException.class), // Relative URI – URI.toURL() throws IllegalArgumentException
        arguments("Relative URI (\"/relative/path\")", URI.create("/relative/path"), null, IllegalArgumentException.class), // Unsupported scheme – mailto URI throws IllegalArgumentException
        arguments("Unsupported scheme (mailto)", URI.create("mailto:someone@example.com"), null, IllegalArgumentException.class), // Malformed hierarchical URI (missing authority)
        arguments("Malformed hierarchical URI (\"http:/example.com\")", URI.create("http:/example.com"), null, java.net.MalformedURLException.class), // Opaque URN – throws IllegalArgumentException
        arguments("Opaque URN (urn:isbn:0451450523)", URI.create("urn:isbn:0451450523"), null, IllegalArgumentException.class));
    }

    @Test
    void toURL_successfulConversion_returnsExpectedURL() {
        // GIVEN a well‑formed absolute HTTP URI
        URI uri = URI.create("http://example.com/path");
        // WHEN
        URL result = UriConversions.toURL(uri, mockConverter);
        // THEN
        assertEquals(uri.toURL(), result);
    }

    @Test
    void toURL_malformedURI_throwsIllegalArgumentException() {
        // GIVEN
        // A URI that cannot be converted to a URL (e.g., a mailto URI)
        URI uri = URI.create("mailto:someone@example.com");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UriConversions.toURL(uri, mockConverter));
        // Verify that the cause is a MalformedURLException and the message contains the URI
        assertNotNull(ex.getCause());
        assertTrue(ex.getMessage().contains("Unable to convert URI to URL"));
        assertTrue(ex.getMessage().contains(uri.toString()));
    }

    @Test
    void toURL_successfulConversionWithNonNullConverter_returnsExpectedURL() {
        // GIVEN a well‑formed absolute HTTPS URI and a non‑null Converter (the mock)
        URI uri = URI.create("https://example.org/resource");
        // WHEN
        URL result = UriConversions.toURL(uri, mockConverter);
        // THEN
        assertEquals(uri.toURL(), result);
    }

    @Test
    void toURL_uriWithEncodedCharacters_isConvertedCorrectly() {
        // GIVEN a URI that contains percent‑encoded characters
        URI uri = URI.create("http://example.com/space%20here");
        // WHEN
        URL result = UriConversions.toURL(uri, null);
        // THEN
        assertEquals(uri.toURL(), result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideErrorScenarios")
    void toURL_errorScenarios(String description, Object from, Converter converter, Class<? extends Throwable> expectedCause) {
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UriConversions.toURL(from, converter), description);
        // The wrapper exception must have a non‑null cause of the expected type
        assertNotNull(ex.getCause(), description);
        assertTrue(expectedCause.isInstance(ex.getCause()), description + " – expected cause of type " + expectedCause.getSimpleName());
    }
}
