package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toURI_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(// Empty string should be treated as empty → null
        Arguments.of("", null), // Well‑formed URI with fragment
        Arguments.of("http://example.com/path?query=1#frag", URI.create("http://example.com/path?query=1#frag")));
    }

    @Test
    void toURI_WithEmptyString_ReturnsNull() {
        String empty = "";
        URI result = StringConversions.toURI(empty, mockConverter);
        assertNull(result, "Empty string should result in null URI");
    }

    @Test
    void toURI_WithValidString_ReturnsCorrectURI() {
        String url = "http://example.com/path?query=1";
        URI expected = URI.create(url);
        URI result = StringConversions.toURI(url, mockConverter);
        assertNotNull(result, "Resulting URI should not be null for a valid string");
        assertEquals(expected, result, "The created URI should match the expected value");
    }

    @Test
    void toURI_WithNonStringInput_ThrowsClassCastException() {
        // not a String
        Object nonString = 12345;
        assertThrows(ClassCastException.class, () -> StringConversions.toURI(nonString, mockConverter), "Passing a non‑String object should throw ClassCastException");
    }

    /* -------------------------------------------------------------
     * Group: toURI - normal behavior
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=\"{0}\" → expected={1}")
    @MethodSource("normalBehaviorProvider")
    void toURI_NormalBehavior(Object from, URI expected) {
        // The converter argument is not used; we can pass null or the mock.
        URI result = StringConversions.toURI(from, null);
        assertEquals(expected, result, "Resulting URI does not match expected value");
    }

    /* -------------------------------------------------------------
     * Group: toURI - null input
     * ------------------------------------------------------------- */
    @Test
    void toURI_NullInput_ReturnsNull() {
        // Passing a null source should result in null (StringUtilities treats null as empty)
        URI result = StringConversions.toURI(null, null);
        assertNull(result, "Null input should produce a null URI");
    }

    /* -------------------------------------------------------------
     * Group: toURI - non‑String input
     * ------------------------------------------------------------- */
    @Test
    void toURI_NonStringInput_ThrowsClassCastException() {
        // Integer, not a String
        Object nonString = 12345;
        assertThrows(ClassCastException.class, () -> StringConversions.toURI(nonString, null), "Passing a non‑String object should throw ClassCastException");
    }

    /* -------------------------------------------------------------
     * Group: toURI - whitespace only string
     * ------------------------------------------------------------- */
    @Test
    void toURI_WhitespaceOnlyString_ReturnsNull() {
        String whitespace = "   ";
        URI result = StringConversions.toURI(whitespace, null);
        assertNull(result, "A string containing only whitespace should be considered empty and return null");
    }

    /* -------------------------------------------------------------
     * Group: toURI - malformed URI
     * ------------------------------------------------------------- */
    @Test
    void toURI_MalformedUri_ThrowsIllegalArgumentException() {
        String malformed = "http://example .com";
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toURI(malformed, null), "A syntactically invalid URI string should cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     * Group: toURI - valid URI with encoded characters
     * ------------------------------------------------------------- */
    @Test
    void toURI_EncodedCharacters_ReturnsCorrectUri() {
        String encoded = "http://example.com/path%20with%20spaces";
        URI expected = URI.create(encoded);
        URI result = StringConversions.toURI(encoded, null);
        assertNotNull(result, "Result should not be null for a valid encoded URI");
        assertEquals(expected, result, "The created URI should match the expected encoded URI");
    }

    /* -------------------------------------------------------------
     * Group: toURI - non‑null converter argument
     * ------------------------------------------------------------- */
    @Test
    void toURI_NonNullConverter_IgnoredByMethod() {
        String url = "https://test.org";
        URI expected = URI.create(url);
        // Use the @Mock instance to prove that a non‑null converter does not affect the outcome
        URI result = StringConversions.toURI(url, mockConverter);
        assertNotNull(result, "Resulting URI should not be null when a non‑null converter is supplied");
        assertEquals(expected, result, "Converter argument must be ignored; URI should match expected value");
    }
}
