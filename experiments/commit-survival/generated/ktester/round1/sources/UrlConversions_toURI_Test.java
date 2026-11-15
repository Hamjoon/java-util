package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UrlConversions_toURI_Test {

    /**
     * The Converter is not used by {@code UrlConversions.toURI}, but we provide a mock
     *  to satisfy the method signature and to illustrate typical test‑setup practice.
     */
    @Mock
    private Converter mockConverter;

    private static class Arguments {

        static org.junit.jupiter.params.provider.Arguments of(String description, URL url, URI expected) {
            return org.junit.jupiter.params.provider.Arguments.of(description, url, expected);
        }
    }

    /**
     * Supplies test data for {@link #toURI_successfulConversions}.
     */
    private static Stream<Arguments> successfulConversionProvider() throws MalformedURLException {
        return Stream.of(Arguments.of("Standard HTTP URL with query and fragment", new URL("http://example.com/path?query=1#frag"), URI.create("http://example.com/path?query=1#frag")), Arguments.of("File‑scheme URL", new URL("file:/tmp/test.txt"), URI.create("file:/tmp/test.txt")), Arguments.of("HTTPS URL with explicit port", new URL("https://example.com:8080/"), URI.create("https://example.com:8080/")), Arguments.of("Mailto URL", new URL("mailto:user@example.com"), URI.create("mailto:user@example.com")));
    }

    /**
     * Happy‑path: a well‑formed {@link URL} is converted to the expected {@link URI}.
     */
    @Test
    void toURI_convertsUrlToUriSuccessfully() throws Exception {
        // given
        URL url = new URL("http://example.com/path?query=123#fragment");
        // when
        URI result = UrlConversions.toURI(url, mockConverter);
        // then
        assertEquals(url.toURI(), result, "The returned URI should match URL.toURI()");
    }

    /**
     * Error‑path: when {@link URL#toURI()} throws a checked exception,
     *  {@code UrlConversions.toURI} must wrap it in an {@link IllegalArgumentException}.
     */
    @Test
    void toURI_wrapsExceptionInIllegalArgumentException() throws Exception {
        // given
        URL mockUrl = mock(URL.class);
        URISyntaxException cause = new URISyntaxException("bad uri", "Invalid syntax");
        when(mockUrl.toURI()).thenThrow(cause);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(mockUrl, mockConverter), "Expected an IllegalArgumentException when URL.toURI() fails");
        assertTrue(ex.getMessage().contains("Unable to convert URL to URI"), "Exception message should contain the custom error text");
        assertSame(cause, ex.getCause(), "Original URISyntaxException should be the cause");
    }

    /**
     * Parameterized test that verifies a variety of well‑formed URLs are converted
     * to the exact same URI that {@link URL#toURI()} would produce.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toURI_successfulConversions(String description, URL url, URI expected) {
        // when
        URI result = UrlConversions.toURI(url, mockConverter);
        // then
        assertEquals(expected, result, "Converted URI does not match expected value");
    }

    /**
     * Verifies that when {@link URL#toURI()} throws a {@link URISyntaxException},
     * the method wraps it in an {@link IllegalArgumentException} preserving the cause.
     */
    @Test
    void toURI_wrapsURISyntaxExceptionInIllegalArgumentException() throws Exception {
        // given
        URL mockUrl = mock(URL.class);
        URISyntaxException cause = new URISyntaxException("bad uri", "Invalid syntax");
        when(mockUrl.toURI()).thenThrow(cause);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(mockUrl, mockConverter), "Expected an IllegalArgumentException when URL.toURI() fails");
        assertTrue(ex.getMessage().contains("Unable to convert URL to URI"), "Exception message should contain the custom error text");
        assertSame(cause, ex.getCause(), "Original URISyntaxException should be the cause");
    }

    /**
     * Demonstrates that a URL subclass that throws a {@link SecurityException}
     * from {@code toURI()} is also wrapped in an {@link IllegalArgumentException}.
     */
    @Test
    void toURI_wrapsSecurityExceptionInIllegalArgumentException() throws Exception {
        // given – anonymous subclass of URL
        URL urlWithSecurityException = new URL("http://example.com") {

            @Override
            public URI toURI() {
                throw new SecurityException("test security");
            }
        };
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(urlWithSecurityException, mockConverter));
        assertTrue(ex.getMessage().contains("Unable to convert URL to URI"));
        assertTrue(ex.getCause() instanceof SecurityException, "Cause should be the original SecurityException");
        assertEquals("test security", ex.getCause().getMessage());
    }

    @Test
    void toURI_nullFromThrowsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> UrlConversions.toURI(null, mockConverter), "Passing null as the 'from' argument should result in NPE");
    }

    @Test
    void toURI_nonUrlStringThrowsClassCastException() {
        // given
        Object notAUrl = "some string";
        // when / then
        assertThrows(ClassCastException.class, () -> UrlConversions.toURI(notAUrl, mockConverter), "A non‑URL object should cause a ClassCastException");
    }

    @Test
    void toURI_genericObjectThrowsClassCastException() {
        // given
        Object notAUrl = new Object();
        // when / then
        assertThrows(ClassCastException.class, () -> UrlConversions.toURI(notAUrl, mockConverter), "A generic Object not being a URL should cause a ClassCastException");
    }
}
