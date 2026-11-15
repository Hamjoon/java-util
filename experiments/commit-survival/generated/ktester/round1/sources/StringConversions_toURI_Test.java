package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.of;

@ExtendWith(MockitoExtension.class)
class StringConversions_toURI_Test {

    @Mock
    private Converter // not used by toURI but required by signature
    mockConverter;

    /**
     * Provides a stream of arguments representing the various scenarios
     * described in the JSON test cases.
     *
     * @return a stream of {@link Arguments}
     */
    static Stream<Arguments> uriTestCases() {
        // Very long URI – built lazily to avoid huge constant strings in source
        String longPath = "http://example.com/" + "a".repeat(5_000);
        return Stream.of(// Empty / whitespace‑only strings → null
        of("Empty string should be considered empty", "", null, null), of("String containing only whitespace is treated as empty", "   ", null, null), // Valid URIs
        of("Valid non‑empty URI string", "http://example.com/path?query=1", null, "http://example.com/path?query=1"), of("Different scheme (ftp) is also accepted", "ftp://user:pass@host:21/dir/file.txt", null, "ftp://user:pass@host:21/dir/file.txt"), of("Valid relative URI", "/relative/path?param=value", null, "/relative/path?param=value"), of("Opaque URI scheme (mailto) is accepted", "mailto:user@example.com", null, "mailto:user@example.com"), of("File‑scheme URI should be parsed without error", "file:///C:/temp/file.txt", null, "file:///C:/temp/file.txt"), of("Scheme‑less string is treated as a relative URI", "www.example.com", null, "www.example.com"), of("Percent‑encoded space is valid", "http://example.com/space%20test", null, "http://example.com/space%20test"), of("Very long but syntactically correct URI should be handled", longPath, null, longPath), // Malformed URIs → IllegalArgumentException
        of("Malformed URI string causes URI.create to throw IllegalArgumentException", "invalid uri %zz", IllegalArgumentException.class, null), of("String contains leading/trailing whitespace – URI.create throws", "  http://example.com  ", IllegalArgumentException.class, null), of("Non‑ASCII characters without proper encoding trigger exception", "http://例子.测试", IllegalArgumentException.class, null), of("Raw space inside the URI string is not allowed", "http://example.com/space test", IllegalArgumentException.class, null), of("Backslashes are not valid URI characters", "C:\\folder\\file.txt", IllegalArgumentException.class, null), of("Incomplete percent‑encoding results in a malformed URI", "http://example.com/%", IllegalArgumentException.class, null), of("Illegal character '|' in query component causes exception", "http://example.com/?q=hello|world", IllegalArgumentException.class, null), of("IPv6 host must be enclosed in brackets – missing brackets cause exception", "http://2001:db8::1", IllegalArgumentException.class, null), // Non‑String source objects → ClassCastException
        of("Non‑String object (Integer) causes ClassCastException", 123, ClassCastException.class, null), of("StringBuilder is not a String – casting fails", new StringBuilder("http://example.com"), ClassCastException.class, null), of("Boolean source cannot be cast to String", true, ClassCastException.class, null), // Converter argument is ignored – still returns correct URI
        of("Converter argument is ignored; method still returns the correct URI", "http://example.com/path?query=1", null, "http://example.com/path?query=1"));
    }

    @Test
    void toURI_NullInput_ReturnsNull() {
        // given
        Object input = null;
        // when
        URI result = StringConversions.toURI(input, mockConverter);
        // then
        assertNull(result, "Expected null when input is null");
    }

    @Test
    void toURI_EmptyString_ReturnsNull() {
        // given
        Object input = "";
        // when
        URI result = StringConversions.toURI(input, mockConverter);
        // then
        assertNull(result, "Expected null when input is an empty string");
    }

    @Test
    void toURI_ValidString_ReturnsCorrectURI() {
        // given
        String input = "http://example.com/path?query=123#fragment";
        // when
        URI result = StringConversions.toURI(input, mockConverter);
        // then
        assertNotNull(result, "Resulting URI should not be null for a valid string");
        assertEquals(URI.create(input), result, "The created URI should match the input string");
    }

    @Test
    void toURI_InvalidString_ThrowsIllegalArgumentException() {
        // given
        // A string that URI.create cannot parse (contains spaces)
        String input = "http:// example .com";
        // when / then
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toURI(input, mockConverter), "Expected IllegalArgumentException for an invalid URI string");
    }

    /**
     * Executes the parameterised test cases defined in {@link #uriTestCases()}.
     *
     * @param description        a human‑readable description of the scenario
     * @param input               the {@code from} argument supplied to {@code toURI}
     * @param expectedException  the expected exception type (or {@code null} if none)
     * @param expectedUriString  the expected URI string (or {@code null} if a {@code null} result is expected)
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("uriTestCases")
    void toURI_VariousScenarios(String description, Object input, Class<? extends Throwable> expectedException, String expectedUriString) {
        if (expectedException != null) {
            // The method should throw the specified exception
            assertThrows(expectedException, () -> StringConversions.toURI(input, mockConverter), description);
        } else {
            // No exception expected – obtain the result
            URI result = StringConversions.toURI(input, mockConverter);
            if (expectedUriString == null) {
                // Expected a null result (empty/whitespace string or null input)
                assertNull(result, description);
            } else {
                // Expected a concrete URI instance
                assertNotNull(result, description);
                assertEquals(URI.create(expectedUriString), result, description);
            }
        }
    }
}
