package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastReader_close_Test {

    private FastReader fastReader;

    @Mock
    private Reader // Mockito will create a mock Reader for us
    mockReader;

    @BeforeEach
    void setupBeforeEach() {
        // Create a FastReader that wraps the mocked Reader.
        // Use the default buffer sizes (the constructor delegates to the 3‑arg ctor).
        fastReader = new FastReader(mockReader);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the FastReader is closed after each test to avoid resource leaks.
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /**
     * Helper to read the private {@code in} field via reflection.
     */
    private static Reader getInternalReader(FastReader fr) throws Exception {
        Field f = FastReader.class.getDeclaredField("in");
        f.setAccessible(true);
        return (Reader) f.get(fr);
    }

    private static Stream<Reader> nonNullReaderProvider() {
        return Stream.of(new StringReader("first"), new StringReader("second"));
    }

    @Test
    @DisplayName("close() should delegate to the underlying Reader's close()")
    void testCloseDelegatesToUnderlyingReader() throws IOException {
        // When
        fastReader.close();
        // Then
        verify(mockReader, times(1)).close();
        // Subsequent close() calls must NOT invoke the underlying close again
        fastReader.close();
        verifyNoMoreInteractions(mockReader);
    }

    @Test
    @DisplayName("close() propagates IOException thrown by the underlying Reader")
    void testClosePropagatesIOExceptionFromUnderlying() throws IOException {
        // Arrange: make the mock throw when close() is invoked
        doThrow(new IOException("underlying close failure")).when(mockReader).close();
        // Act & Assert
        IOException ex = assertThrows(IOException.class, () -> fastReader.close());
        assertEquals("underlying close failure", ex.getMessage());
        // Verify that close() was indeed called on the mock
        verify(mockReader, times(1)).close();
    }

    @Test
    @DisplayName("read() after close() should throw IOException")
    void testReadAfterCloseThrowsIOException() throws IOException {
        // Use a real StringReader to avoid mocking the read behaviour
        FastReader realReader = new FastReader(new StringReader("abc"));
        realReader.close();
        // After closing, any read attempt must fail
        assertThrows(IOException.class, realReader::read);
    }

    @Test
    @DisplayName("Calling close() on an already‑closed FastReader does not throw")
    void testCloseIdempotent() throws IOException {
        // First close – should succeed
        fastReader.close();
        // Second close – should be a no‑op and must not throw
        assertDoesNotThrow(() -> fastReader.close());
        // Underlying Reader's close() must have been called exactly once
        verify(mockReader, times(1)).close();
    }

    @Test
    @DisplayName("close() on a FastReader with null internal Reader does nothing")
    void testCloseWhenInternalReaderIsNull() throws Exception {
        // Create a FastReader (wrapping a mock) and then force its internal 'in' to null.
        FastReader fr = new FastReader(mockReader);
        Field f = FastReader.class.getDeclaredField("in");
        f.setAccessible(true);
        // simulate default state where 'in' is null
        f.set(fr, null);
        // No exception should be thrown
        assertDoesNotThrow(fr::close);
        // Verify that the underlying mock was never touched
        verifyNoInteractions(mockReader);
        // Internal field must still be null
        assertNull(getInternalReader(fr), "Internal 'in' should remain null");
    }

    @Test
    @DisplayName("close() clears the internal reference after successful close")
    void testCloseClearsInternalReference() throws Exception {
        // fastReader is already constructed with mockReader (non‑null)
        fastReader.close();
        // After successful close the internal field must be null
        assertNull(getInternalReader(fastReader), "Internal 'in' should be null after close");
        // Underlying mock should have been closed exactly once
        verify(mockReader, times(1)).close();
    }

    @Test
    @DisplayName("IOException from underlying close is propagated and internal reference stays")
    void testIOExceptionKeepsInternalReference() throws Exception {
        doThrow(new IOException("forced IO error")).when(mockReader).close();
        IOException ex = assertThrows(IOException.class, () -> fastReader.close());
        assertEquals("forced IO error", ex.getMessage());
        // The internal reader must still be non‑null so that a subsequent close can be attempted
        assertNotNull(getInternalReader(fastReader), "Internal 'in' should remain non‑null after IOException");
        // Verify that close was attempted once
        verify(mockReader, times(1)).close();
    }

    @Test
    @DisplayName("Subsequent close attempts after an IOException keep propagating")
    void testRepeatedCloseAfterIOException() throws Exception {
        doThrow(new IOException("forced IO error")).when(mockReader).close();
        // First attempt – exception propagated
        assertThrows(IOException.class, () -> fastReader.close());
        // Internal reference must still be present
        assertNotNull(getInternalReader(fastReader));
        // Second attempt – same behaviour
        assertThrows(IOException.class, () -> fastReader.close());
        // Verify that close() was called twice on the mock
        verify(mockReader, times(2)).close();
    }

    @Test
    @DisplayName("RuntimeException from underlying close is propagated unchanged")
    void testRuntimeExceptionPropagation() throws Exception {
        RuntimeException runtime = new RuntimeException("runtime failure");
        doThrow(runtime).when(mockReader).close();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> fastReader.close());
        assertSame(runtime, ex, "The same RuntimeException instance should be re‑thrown");
        // Internal reference should stay intact (as close failed)
        assertNotNull(getInternalReader(fastReader));
    }

    @Test
    @DisplayName("Error from underlying close is propagated unchanged")
    void testErrorPropagation() throws Exception {
        Error seriousError = new AssertionError("serious error");
        doThrow(seriousError).when(mockReader).close();
        Error ex = assertThrows(Error.class, () -> fastReader.close());
        assertSame(seriousError, ex, "The same Error instance should be re‑thrown");
        // Internal reference should stay intact
        assertNotNull(getInternalReader(fastReader));
    }

    @ParameterizedTest(name = "Idempotent close with reader instance {index}")
    @MethodSource("nonNullReaderProvider")
    @DisplayName("close() is idempotent for any non‑null wrapped Reader")
    void testCloseIdempotentWithVariousReaders(Reader underlying) throws IOException, Exception {
        FastReader fr = new FastReader(underlying);
        // First close – should succeed
        fr.close();
        // Verify internal field cleared
        assertNull(getInternalReader(fr));
        // Second close – must be a no‑op, no exception
        assertDoesNotThrow(fr::close);
    }
}
