package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.Reader;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FastReader_close_Test {

    private FastReader fastReader;

    @Mock
    private Reader mockReader;

    @BeforeEach
    void setupBeforeEach() {
        // FastReader requires a non‑null Reader for the normal path.
        // Use a small buffer size – the exact values are not important
        // for the close() behavior.
        fastReader = new FastReader(mockReader, 16, 4);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions so each test starts with a clean slate
        reset(mockReader);
        fastReader = null;
    }

    @Test
    void closeInvokesUnderlyingReaderClose() throws IOException {
        // WHEN
        fastReader.close();
        // THEN
        verify(mockReader, times(1)).close();
    }

    @Test
    void closeIsIdempotentWhenReaderIsAlreadyClosed() throws IOException {
        // GIVEN – first close clears the internal reference
        fastReader.close();
        // WHEN – call close again
        fastReader.close();
        // THEN – underlying reader's close should have been called only once
        verify(mockReader, times(1)).close();
    }

    @Test
    void closeHandlesNullInternalReaderGracefully() throws IOException {
        // GIVEN – construct a FastReader with a null underlying Reader
        FastReader frWithNull = new FastReader(null, 16, 4);
        // WHEN – close should not throw any exception
        frWithNull.close();
        // THEN – nothing to verify (no NPE should be thrown)
        // The test will fail automatically if an exception propagates.
    }

    /**
     * Verify that after a successful close the internal {@code in} field is cleared.
     */
    @Test
    void closeSetsInternalReaderToNullAfterSuccessfulClose() throws Exception {
        // WHEN
        fastReader.close();
        // THEN – delegate close was called
        verify(mockReader, times(1)).close();
        // AND – the private field 'in' is now null
        Field inField = FastReader.class.getDeclaredField("in");
        inField.setAccessible(true);
        assertNull(inField.get(fastReader), "FastReader.in should be null after successful close");
    }

    /**
     * When the wrapped Reader throws an {@link IOException}, FastReader.close()
     * must propagate the exception and must NOT null out the internal reference.
     */
    @Test
    void closePropagatesIOExceptionAndKeepsInternalReader() throws Exception {
        // GIVEN – mockReader throws on close()
        doThrow(new IOException("boom")).when(mockReader).close();
        FastReader fr = new FastReader(mockReader, 16, 4);
        // WHEN / THEN – exception propagates
        IOException ex = assertThrows(IOException.class, fr::close, "IOException should propagate");
        assertEquals("boom", ex.getMessage());
        // AND – underlying close was attempted exactly once
        verify(mockReader, times(1)).close();
        // AND – internal field still holds the original reader (not set to null)
        Field inField = FastReader.class.getDeclaredField("in");
        inField.setAccessible(true);
        Object internal = inField.get(fr);
        assertSame(mockReader, internal, "FastReader.in must remain non‑null when close() throws");
    }

    /**
     * Re‑invoking close after a failed close (exception thrown) should still attempt
     * to close the underlying reader again, because the internal reference was not cleared.
     */
    @Test
    void closeAfterFailedCloseAttemptsDelegateAgain() throws Exception {
        // GIVEN – first close throws
        doThrow(new IOException("first")).when(mockReader).close();
        FastReader fr = new FastReader(mockReader, 16, 4);
        // first attempt – expect exception
        assertThrows(IOException.class, fr::close);
        // reset mock to behave normally for the second attempt
        reset(mockReader);
        when(mockReader.close()).thenReturn(null);
        // WHEN – second close succeeds
        assertDoesNotThrow(fr::close);
        // THEN – underlying close was called twice in total (once before exception, once after)
        // only the second successful call is verified after reset
        verify(mockReader, times(1)).close();
        // Verify internal field is now null after successful close
        Field inField = FastReader.class.getDeclaredField("in");
        inField.setAccessible(true);
        assertNull(inField.get(fr), "FastReader.in should be null after successful close following a failure");
    }
}
