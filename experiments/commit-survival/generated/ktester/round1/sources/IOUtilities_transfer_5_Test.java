package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URLConnection;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_5_Test {

    @Mock
    private URLConnection mockConnection;

    @Mock
    private OutputStream mockOut;

    private byte[] sampleBytes;

    /**
     * OutputStream that records the byte array passed to {@code write(byte[])} and the length.
     */
    private static class RecordingOutputStream extends OutputStream {

        private byte[] writtenBytes;

        private int lastWriteLength;

        @Override
        public void write(byte[] b) {
            // defensive copy
            this.writtenBytes = b.clone();
            this.lastWriteLength = b.length;
        }

        @Override
        public void write(int b) {
            // not used in these tests
            throw new UnsupportedOperationException();
        }

        byte[] getWrittenBytes() {
            return writtenBytes;
        }

        int getLastWriteLength() {
            return lastWriteLength;
        }
    }

    /**
     * OutputStream that succeeds on {@code write} and {@code close} but throws an {@link IOException}
     * when {@code flush} is invoked. BufferedOutputStream calls {@code flush} from its {@code close}
     * implementation, allowing us to test flush‑failure propagation.
     */
    private static class FlushFailingOutputStream extends OutputStream {

        @Override
        public void write(int b) {
            // accept any data
        }

        @Override
        public void flush() throws IOException {
            throw new IOException("flush fail");
        }

        @Override
        public void close() {
            // no‑op; close will be reached after flush throws, but the exception is already propagated
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a deterministic payload for each test
        sampleBytes = new byte[] { 10, 20, 30, 40, 50 };
        // Configure the mock connection to return the mock OutputStream
        when(mockConnection.getOutputStream()).thenReturn(mockOut);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to avoid cross‑test contamination
        reset(mockConnection, mockOut);
    }

    @Test
    void transfer_writesAllBytesAndClosesStream() throws IOException {
        // GIVEN – a URLConnection that provides a mock OutputStream (configured in @BeforeEach)
        // WHEN
        IOUtilities.transfer(mockConnection, sampleBytes);
        // THEN
        // Verify that the underlying OutputStream received the exact byte array
        verify(mockOut, times(1)).write(sampleBytes);
        // Verify that the stream was closed (try‑with‑resources)
        verify(mockOut, times(1)).close();
        // No other interactions should have occurred
        verifyNoMoreInteractions(mockOut);
    }

    @Test
    void transfer_withEmptyByteArray_writesZeroBytesButClosesStream() throws IOException {
        // GIVEN
        byte[] empty = new byte[0];
        // WHEN
        IOUtilities.transfer(mockConnection, empty);
        // THEN
        verify(mockOut, times(1)).write(empty);
        verify(mockOut, times(1)).close();
    }

    @Test
    void transfer_nullConnection_throwsNullPointerException() {
        // GIVEN
        URLConnection nullConn = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(nullConn, sampleBytes));
    }

    @Test
    void transfer_nullByteArray_throwsNullPointerException() {
        // GIVEN
        byte[] nullBytes = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(mockConnection, nullBytes));
    }

    @Test
    void transfer_getOutputStreamThrowsIOException_propagates() throws IOException {
        // GIVEN – getOutputStream() throws
        when(mockConnection.getOutputStream()).thenThrow(new IOException("boom"));
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, new byte[] { 10, 20 }));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void transfer_outputStreamWriteThrowsIOException_propagates() throws IOException {
        // GIVEN – underlying stream throws on write
        doThrow(new IOException("write fail")).when(mockOut).write(any(byte[].class));
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, new byte[] { 5, 6, 7 }));
        assertEquals("write fail", ex.getMessage());
        // ensure close is still attempted (BufferedOutputStream will still close)
        verify(mockOut, times(1)).close();
    }

    @Test
    void transfer_outputStreamCloseThrowsIOException_propagates() throws IOException {
        // GIVEN – close() throws
        doThrow(new IOException("close fail")).when(mockOut).close();
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, new byte[] { 9 }));
        assertEquals("close fail", ex.getMessage());
        // write should have succeeded before close
        verify(mockOut, times(1)).write(new byte[] { 9 });
    }

    @Test
    void transfer_emptyByteArray_stillClosesStream() throws IOException {
        // GIVEN
        byte[] empty = new byte[0];
        // WHEN
        IOUtilities.transfer(mockConnection, empty);
        // THEN – close must be called exactly once
        verify(mockOut, times(1)).close();
    }

    @Test
    void transfer_connectionReturnsNullOutputStream_throwsNullPointerException() {
        // GIVEN – getOutputStream() returns null
        when(mockConnection.getOutputStream()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(mockConnection, new byte[] { 7, 8, 9 }));
    }

    @Test
    void transfer_flushFailsDuringClose_propagatesIOException() throws IOException {
        // GIVEN – a custom OutputStream that throws on flush (BufferedOutputStream.close() calls flush)
        OutputStream flushFailing = new FlushFailingOutputStream();
        when(mockConnection.getOutputStream()).thenReturn(flushFailing);
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mockConnection, new byte[] { 1, 2, 3 }));
        assertEquals("flush fail", ex.getMessage());
    }

    @Test
    void transfer_largePayload_isWrittenCompletely() throws IOException {
        // GIVEN – a real ByteArrayOutputStream to capture the data
        ByteArrayOutputStream realOut = new ByteArrayOutputStream();
        when(mockConnection.getOutputStream()).thenReturn(realOut);
        // 200 000 incremental bytes (0‑255 wrap)
        byte[] large = IntStream.range(0, 200_000).map(i -> i & 0xFF).collect(StringBuilder::new, (sb, i) -> sb.append((char) i), StringBuilder::append).toString().getBytes();
        // WHEN
        IOUtilities.transfer(mockConnection, large);
        // THEN – the captured bytes must be exactly the same
        assertArrayEquals(large, realOut.toByteArray());
        // also verify close was called (BufferedOutputStream closes the underlying stream)
        // Since we used a real stream, we cannot verify via Mockito; we rely on the fact
        // that ByteArrayOutputStream.close() is a no‑op but does not throw.
    }

    @Test
    void transfer_writesWithCorrectLengthRecorded() throws IOException {
        // GIVEN – a RecordingOutputStream that records the length argument of write(byte[])
        RecordingOutputStream recOut = new RecordingOutputStream();
        when(mockConnection.getOutputStream()).thenReturn(recOut);
        byte[] payload = new byte[] { 10, 20, 30, 40, 50 };
        // WHEN
        IOUtilities.transfer(mockConnection, payload);
        // THEN – the recorded length must equal the payload length
        assertEquals(payload.length, recOut.getLastWriteLength(), "OutputStream.write(byte[]) should receive the full array length");
        // also ensure the data itself matches
        assertArrayEquals(payload, recOut.getWrittenBytes());
    }
}
