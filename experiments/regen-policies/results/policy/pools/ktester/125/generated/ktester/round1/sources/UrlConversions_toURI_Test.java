package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.util.Objects;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlConversions_toURI_Test {

    @Mock
    private Converter // Converter is not used by the method but required by signature
    mockConverter;

    /**
     * Dummy implementation of {@link Converter} – used to prove the argument is ignored.
     */
    private static class DummyConverter implements Converter {
    }

    /**
     * Subclass of {@link URL} that throws a {@link RuntimeException} from {@link #toURI()}.
     */
    private static class RuntimeExceptionURL extends URL {

        RuntimeExceptionURL(URL delegate) throws MalformedURLException {
            super(delegate.toString());
        }

        @Override
        public URI toURI() {
            throw new RuntimeException("forced RuntimeException from toURI()");
        }
    }

    /**
     * Subclass of {@link URL} that throws a {@link SecurityException} from {@link #toURI()}.
     */
    private static class SecurityExceptionURL extends URL {

        SecurityExceptionURL(URL delegate) throws MalformedURLException {
            super(delegate.toString());
        }

        @Override
        public URI toURI() {
            throw new SecurityException("forced SecurityException from toURI()");
        }
    }

    @Test
    void toURI_successfulConversion_returnsExpectedURI() throws Exception {
        // given
        URL url = new URL("http://example.com/path?query=123#fragment");
        // when
        URI result = UrlConversions.toURI(url, mockConverter);
        // then
        assertEquals(url.toURI(), result, "The returned URI should match URL.toURI()");
    }

    @Test
    void toURI_invalidURL_throwsIllegalArgumentException() throws Exception {
        // given: a URL that cannot be converted to a URI (contains an illegal character '|')
        URL url = new URL("http://example.com/abc|def");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(url, mockConverter));
        // Verify that the cause is a URISyntaxException and the message contains the expected text
        assertTrue(ex.getCause() instanceof URISyntaxException, "Cause of the exception should be URISyntaxException");
        assertTrue(ex.getMessage().contains("Unable to convert URL to URI"), "Exception message should contain the predefined text");
    }

    @Test
    void successfulConversion_returnsExpectedURI() throws Exception {
        // group: Successful conversion
        URL url = new URL("http://example.com/path");
        URI result = UrlConversions.toURI(url, mockConverter);
        assertEquals(url.toURI(), result, "The returned URI should match URL.toURI()");
    }

    @Test
    void exceptionHandling_urlToURIThrowsURISyntaxException_isWrapped() throws Exception {
        // group: Exception handling – URL.toURI throws URISyntaxException
        URL mockUrl = mock(URL.class);
        URISyntaxException cause = new URISyntaxException("http://bad", "invalid");
        when(mockUrl.toURI()).thenThrow(cause);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(mockUrl, mockConverter));
        assertSame(cause, ex.getCause(), "Cause must be the original URISyntaxException");
        assertTrue(ex.getMessage().contains("Unable to convert URL to URI"), "Exception message should contain the predefined text");
    }

    @Test
    void nullUrlArgument_isWrappedInIllegalArgumentException() {
        // group: Exception handling – null URL argument
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(null, mockConverter));
        assertTrue(ex.getCause() instanceof NullPointerException, "Cause should be NullPointerException when a null URL is supplied");
        assertTrue(ex.getMessage().contains("Unable to convert URL to URI"));
    }

    @Test
    void nonUrlInput_throwsClassCastException() {
        // group: ClassCastException when non‑URL input
        String notAUrl = "just a string";
        assertThrows(ClassCastException.class, () -> UrlConversions.toURI(notAUrl, mockConverter), "Passing a non‑URL object should result in a ClassCastException");
    }

    @Test
    void nonNullConverterArgument_isIgnored() throws Exception {
        // group: Non‑null converter argument is ignored
        URL url = new URL("https://example.org/resource");
        DummyConverter dummy = new DummyConverter();
        URI result = UrlConversions.toURI(url, dummy);
        assertEquals(url.toURI(), result, "Converter argument must not influence the conversion");
    }

    @Test
    void realURISyntaxException_fromIllegalCharacter_isWrapped() throws Exception {
        // group: Real URISyntaxException from URL containing illegal characters
        // The pipe character '|' is illegal in a URI and triggers URISyntaxException.
        URL url = new URL("http://example.com/abc|def");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(url, mockConverter));
        assertTrue(ex.getCause() instanceof URISyntaxException, "Cause should be URISyntaxException for illegal characters");
        assertTrue(ex.getMessage().contains("Unable to convert URL to URI"));
    }

    @Test
    void customUrlSubclass_throwingRuntimeException_isWrapped() throws Exception {
        // group: Custom URL subclass throwing RuntimeException from toURI()
        URL base = new URL("http://example.com");
        RuntimeExceptionURL url = new RuntimeExceptionURL(base);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(url, mockConverter));
        assertTrue(ex.getCause() instanceof RuntimeException, "Cause must be the RuntimeException thrown by the subclass");
        assertTrue(ex.getMessage().contains("Unable to convert URL to URI"));
    }

    @Test
    void urlWithIPv6Host_isConvertedSuccessfully() throws Exception {
        // group: URL with IPv6 host
        URL url = new URL("http://[2001:db8::1]/path");
        URI result = UrlConversions.toURI(url, mockConverter);
        assertEquals(URI.create("http://[2001:db8::1]/path"), result, "IPv6 literal host should be preserved in the resulting URI");
    }

    @Test
    void customUrlSubclass_throwingSecurityException_isWrapped() throws Exception {
        // group: Exception handling – SecurityException from URL.toURI()
        URL base = new URL("http://example.com");
        SecurityExceptionURL url = new SecurityExceptionURL(base);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> UrlConversions.toURI(url, mockConverter));
        assertTrue(ex.getCause() instanceof SecurityException, "Cause must be the SecurityException thrown by the subclass");
        assertTrue(ex.getMessage().contains("Unable to convert URL to URI"));
    }
}
