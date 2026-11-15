package com.cedarsoftware.util.convert;

import java.net.URI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toURI_Test {

    /**
     * Converter instance – not used by the method but required by the signature.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a no‑arg constructor; it provides the default configuration.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in larger test suites.
        converter = null;
    }

    static Stream<Arguments> emptyAndNullProvider() {
        return Stream.of(arguments(null, "When the source object is null, the method should return null."), arguments("", "When the source string is empty, the method should return null."));
    }

    static Stream<Arguments> validUriProvider() {
        return Stream.of(arguments("http://example.com", URI.create("http://example.com")), arguments("mailto:user@example.com", URI.create("mailto:user@example.com")), arguments("file:///C:/path/to/file.txt", URI.create("file:///C:/path/to/file.txt")), arguments("urn:isbn:0451450523", URI.create("urn:isbn:0451450523")), arguments("http://example.com/path?query=1#fragment", URI.create("http://example.com/path?query=1#fragment")));
    }

    static Stream<Arguments> malformedUriProvider() {
        return Stream.of(arguments("http:// example .com", IllegalArgumentException.class), arguments("://baduri", IllegalArgumentException.class));
    }

    static Stream<Arguments> nonStringProvider() {
        return Stream.of(arguments(12345, ClassCastException.class), arguments(new StringBuilder("http://example.com"), ClassCastException.class));
    }

    @Test
    void toURI_NullInput_ReturnsNull() {
        URI result = StringConversions.toURI(null, converter);
        assertNull(result, "Converting a null reference should return null");
    }

    @Test
    void toURI_EmptyString_ReturnsNull() {
        URI result = StringConversions.toURI("", converter);
        assertNull(result, "Converting an empty string should return null");
    }

    @Test
    void toURI_ValidString_ReturnsUri() {
        String input = "https://example.com/path?query=1#fragment";
        URI result = StringConversions.toURI(input, converter);
        assertNotNull(result, "Result should not be null for a well‑formed URI string");
        assertEquals(URI.create(input), result, "Returned URI should match the input string");
    }

    @Test
    void toURI_MalformedString_ThrowsIllegalArgumentException() {
        String badUri = "ht!tp://[invalid]";
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toURI(badUri, converter), "Malformed URI strings should cause IllegalArgumentException");
    }

    @Test
    void toURI_NonStringInput_ThrowsClassCastException() {
        // Integer, not a String
        Object notAString = 12345;
        assertThrows(ClassCastException.class, () -> StringConversions.toURI(notAString, converter), "Passing a non‑String object should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] Null or empty input should return null – {2}")
    @MethodSource("emptyAndNullProvider")
    void toURI_EmptyOrNull_ReturnsNull(Object from, String description) {
        URI result = StringConversions.toURI(from, converter);
        assertNull(result, description);
    }
}
