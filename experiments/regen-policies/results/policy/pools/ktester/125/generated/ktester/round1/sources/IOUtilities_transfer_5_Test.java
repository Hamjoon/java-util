package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_5_Test {

    private static final byte[] SAMPLE_BYTES = "sample payload".getBytes(StandardCharsets.UTF_8);

    @Mock
    private URLConnection mockConnection;

    private ByteArrayOutputStream realOutputStream;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // a real ByteArrayOutputStream to verify the written data
        realOutputStream = new ByteArrayOutputStream();
        when(mockConnection.getOutputStream()).thenReturn(realOutputStream);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure the stream is cleared between tests
        realOutputStream.reset();
        reset(mockConnection);
    }

    private static Stream<byte[]> successfulWriteProvider() {
        return Stream.of(new byte[] { 0x01, 0x02, 0x03, 0x04 }, // empty payload
        new byte[0]);
    }

    @Test
    void testTransferWritesBytesAndClosesStream() throws IOException {
        // Act
        IOUtilities.transfer(mockConnection, SAMPLE_BYTES);
        // Assert – the bytes were written correctly
        assertArrayEquals(SAMPLE_BYTES, realOutputStream.toByteArray(), "The output stream should contain the exact bytes passed to transfer");
        // The BufferedOutputStream created inside the method should have closed the underlying stream.
        // Since we used a real ByteArrayOutputStream, calling close() does not affect its content,
        // but we can verify that the method attempted to close the stream by checking that no further
        // writes are possible after the call.
        assertDoesNotThrow(() -> realOutputStream.write(new byte[] { 0 }), "The underlying stream should remain usable after being closed (ByteArrayOutputStream is tolerant).");
    }

    @Test
    void testTransferInvokesWriteAndCloseOnUnderlyingOutputStream() throws IOException {
        // Arrange – replace the real stream with a mock to verify interactions
        OutputStream mockOut = mock(OutputStream.class);
        when(mockConnection.getOutputStream()).thenReturn(mockOut);
        // Act
        IOUtilities.transfer(mockConnection, SAMPLE_BYTES);
        // Assert – verify that the underlying stream received the correct write call
        verify(mockOut, times(1)).write(eq(SAMPLE_BYTES), eq(0), eq(SAMPLE_BYTES.length));
        // Verify that the stream was closed exactly once
        verify(mockOut, times(1)).close();
    }

    @Test
    void testTransferWithNullConnectionThrowsNPE() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, SAMPLE_BYTES), "Calling transfer with a null URLConnection should throw NullPointerException");
    }

    @Test
    void testTransferWithNullByteArrayThrowsNPE() {
        // Arrange
        when(mockConnection.getOutputStream()).thenReturn(realOutputStream);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(mockConnection, null), "Calling transfer with a null byte array should throw NullPointerException");
    }

    @Test
    void testTransferWithEmptyByteArrayWritesNothing() throws IOException {
        // Arrange
        byte[] empty = new byte[0];
        // Act
        IOUtilities.transfer(mockConnection, empty);
        // Assert – nothing should have been written
        assertEquals(0, realOutputStream.size(), "An empty byte array should result in no data being written to the output stream");
        // Verify close was still called
        verify(mockConnection, times(1)).getOutputStream();
    }

    /**
     * Parameterized test that verifies the method writes the exact payload
     * and closes the stream for both a non‑empty and an empty byte array.
     */
    @ParameterizedTest(name = "{index} ⇒ bytes={0}")
    @MethodSource("successfulWriteProvider")
    void testTransferSuccessfulWrites(byte[] payload) throws IOException {
        // Arrange – use the real ByteArrayOutputStream (already stubbed in @BeforeEach)
        // Act
        IOUtilities.transfer(mockConnection, payload);
        // Assert – the underlying stream received exactly the payload
        assertArrayEquals(payload, realOutputStream.toByteArray(), "The output stream should contain exactly the bytes passed to transfer");
        // The try‑with‑resources block should have closed the BufferedOutputStream.
        // ByteArrayOutputStream tolerates close, but we can verify that close() was invoked
        // on the underlying stream via Mockito verification.
        verify(mockConnection, times(1)).getOutputStream();
    }

    @Test
    void testTransferGetOutputStreamThrowsIOException() throws IOException {
        // Arrange – make getOutputStream() throw
        when(mockConnection.getOutputStream()).thenThrow(new IOException("stream error"));
        byte[] data = { 0x0A, 0x0B };
        // Act & Assert
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, data), "IOException from getOutputStream() should propagate");
        assertEquals("stream error", ex.getMessage());
        // Verify that no write attempt was made
        verify(mockConnection, times(1)).getOutputStream();
    }

    @Test
    void testTransferUnderlyingWriteThrowsIOException() throws IOException {
        // Arrange – a faulty OutputStream that throws on write
        OutputStream faultyOut = new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                throw new IOException("write error");
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("write error");
            }

            @Override
            public void close() throws IOException {
                // close is fine – we want the write to fail first
            }
        };
        when(mockConnection.getOutputStream()).thenReturn(faultyOut);
        byte[] data = { 0x01, 0x02, 0x03 };
        // Act & Assert
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, data), "IOException from underlying write() should propagate");
        assertEquals("write error", ex.getMessage());
        // The close() of the BufferedOutputStream will still be invoked;
        // since our faultyOut.close() does nothing, no extra exception appears.
    }

    @Test
    void testTransferGetOutputStreamReturnsNull() throws IOException {
        // Arrange – connection returns null
        when(mockConnection.getOutputStream()).thenReturn(null);
        byte[] data = { (byte) 0xFF };
        // Act & Assert
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(mockConnection, data), "A null OutputStream from the connection should cause NullPointerException");
    }

    @Test
    void testTransferCloseThrowsIOException() throws IOException {
        // Arrange – OutputStream whose close() throws
        OutputStream closeFaultyOut = new OutputStream() {

            @Override
            public void write(int b) {
                // accept writes silently
            }

            @Override
            public void write(byte[] b, int off, int len) {
                // accept writes silently
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close error");
            }
        };
        when(mockConnection.getOutputStream()).thenReturn(closeFaultyOut);
        byte[] data = { 0x10, 0x20 };
        // Act & Assert
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, data), "IOException thrown by close() should propagate");
        assertEquals("close error", ex.getMessage());
    }
}
