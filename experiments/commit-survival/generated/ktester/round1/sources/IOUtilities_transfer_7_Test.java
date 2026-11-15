package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_7_Test {

    // larger than buffer to test loops
    private static final byte[] SOURCE_DATA = new byte[100_000];

    private ByteArrayInputStream inputStream;

    private ByteArrayOutputStream outputStream;

    @BeforeAll
    static void setupBeforeAll() {
        // Fill the source data with deterministic values (0‑255 repeated)
        for (int i = 0; i < SOURCE_DATA.length; i++) {
            SOURCE_DATA[i] = (byte) (i % 256);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default streams – individual tests may replace them with custom implementations
        inputStream = new ByteArrayInputStream(SOURCE_DATA);
        outputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure streams are closed after each test
        inputStream.close();
        outputStream.close();
    }

    @Test
    void transfer_WithCallback_AllBytesTransferred() throws IOException {
        // GIVEN
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // Simulate a non‑cancelling callback
        when(callback.isCancelled()).thenReturn(false);
        // WHEN
        IOUtilities.transfer(inputStream, outputStream, callback);
        // THEN
        // Verify that the output matches the input
        assertArrayEquals(SOURCE_DATA, outputStream.toByteArray(), "All bytes should be transferred to the output stream");
        // Verify that the callback was invoked at least once and never cancelled the operation
        verify(callback, atLeastOnce()).bytesTransferred(any(byte[].class), anyInt());
        // isCancelled is only checked after each callback call
        verify(callback, never()).isCancelled();
    }

    @Test
    void transfer_WithCallback_CancelledMidTransfer() throws IOException {
        // GIVEN
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // Cancel after the first buffer write
        final int[] callCount = { 0 };
        doAnswer(invocation -> {
            callCount[0]++;
            // Cancel on the second invocation (i.e., after first buffer has been processed)
            return null;
        }).when(callback).bytesTransferred(any(byte[].class), anyInt());
        when(callback.isCancelled()).thenAnswer(invocation -> callCount[0] > 1);
        // WHEN
        IOUtilities.transfer(inputStream, outputStream, callback);
        // THEN
        // The output should contain only the first buffer (or part of it if the source is smaller)
        byte[] result = outputStream.toByteArray();
        int expectedLength = Math.min(IOUtilities.TRANSFER_BUFFER, SOURCE_DATA.length);
        assertEquals(expectedLength, result.length, "Output should contain only the bytes transferred before cancellation");
        // Verify callback interaction
        verify(callback, atLeastOnce()).bytesTransferred(any(byte[].class), anyInt());
        verify(callback, atLeastOnce()).isCancelled();
    }

    @Test
    void transfer_NullCallback_TransfersAllBytes() throws IOException {
        // GIVEN
        // No callback (null) – just a plain transfer
        // WHEN
        IOUtilities.transfer(inputStream, outputStream, null);
        // THEN
        assertArrayEquals(SOURCE_DATA, outputStream.toByteArray(), "All bytes should be transferred even when callback is null");
    }

    @Test
    @DisplayName("Normal flow without callback – small byte array")
    void transfer_NoCallback_SmallData() throws IOException {
        // GIVEN
        byte[] data = new byte[] { 1, 2, 3, 4, 5 };
        inputStream = new ByteArrayInputStream(data);
        outputStream = new ByteArrayOutputStream();
        // WHEN
        IOUtilities.transfer(inputStream, outputStream, null);
        // THEN
        assertArrayEquals(data, outputStream.toByteArray(), "Output should match the input byte sequence");
    }

    @Test
    @DisplayName("Callback invoked on each chunk – large sequential data")
    void transfer_WithCallback_MultipleChunks() throws IOException {
        // GIVEN
        // Re‑use SOURCE_DATA (100 000 bytes) – already set up in @BeforeEach
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        when(callback.isCancelled()).thenReturn(false);
        // Capture the count values for each invocation
        ArgumentCaptor<Integer> countCaptor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(callback).bytesTransferred(any(byte[].class), countCaptor.capture());
        // WHEN
        IOUtilities.transfer(inputStream, outputStream, callback);
        // THEN
        assertArrayEquals(SOURCE_DATA, outputStream.toByteArray(), "All source bytes must be transferred");
        // Expected number of buffer reads = ceil(100000 / 32768) = 4
        int expectedCalls = (int) Math.ceil((double) SOURCE_DATA.length / IOUtilities.TRANSFER_BUFFER);
        verify(callback, times(expectedCalls)).bytesTransferred(any(byte[].class), anyInt());
        verify(callback, never()).isCancelled();
        // Verify the count arguments (first three full buffers, last partial)
        List<Integer> counts = countCaptor.getAllValues();
        assertEquals(expectedCalls, counts.size(), "Incorrect number of callback invocations");
        for (int i = 0; i < counts.size(); i++) {
            int expected = (i < counts.size() - 1) ? IOUtilities.TRANSFER_BUFFER : SOURCE_DATA.length - IOUtilities.TRANSFER_BUFFER * (counts.size() - 1);
            assertEquals(expected, counts.get(i).intValue(), "Callback count mismatch on chunk " + (i + 1));
        }
    }

    @Test
    @DisplayName("Callback cancels after first bytesTransferred call")
    void transfer_WithCallback_CancelAfterFirstChunk() throws IOException {
        // GIVEN
        byte[] data = new byte[] { 10, 20, 30 };
        inputStream = new ByteArrayInputStream(data);
        outputStream = new ByteArrayOutputStream();
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // First call – do nothing; second call – isCancelled returns true
        when(callback.isCancelled()).thenReturn(false, true);
        // bytesTransferred will be called once; we let it proceed normally
        doNothing().when(callback).bytesTransferred(any(byte[].class), anyInt());
        // WHEN
        IOUtilities.transfer(inputStream, outputStream, callback);
        // THEN
        // All three bytes are read in a single buffer (size < TRANSFER_BUFFER) before cancellation check
        assertArrayEquals(data, outputStream.toByteArray(), "All bytes should be transferred because cancellation is checked after the write");
        verify(callback, times(1)).bytesTransferred(any(byte[].class), anyInt());
        verify(callback, atLeastOnce()).isCancelled();
    }

    @Test
    @DisplayName("Empty input stream – callback never invoked")
    void transfer_EmptyInput_NoCallbackInvocations() throws IOException {
        // GIVEN
        inputStream = new ByteArrayInputStream(new byte[0]);
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        when(callback.isCancelled()).thenReturn(false);
        // WHEN
        IOUtilities.transfer(inputStream, outputStream, callback);
        // THEN
        assertEquals(0, outputStream.size(), "Output should remain empty for empty input");
        verify(callback, never()).bytesTransferred(any(byte[].class), anyInt());
        verify(callback, never()).isCancelled();
    }

    @Test
    @DisplayName("InputStream throws IOException after two bytes")
    void transfer_InputStreamThrowsIOException() {
        // GIVEN
        InputStream flakyIn = new InputStream() {

            private int readCount = 0;

            private final byte[] payload = new byte[] { 1, 2, 3, 4 };

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (readCount >= 2) {
                    throw new IOException("Simulated read failure");
                }
                int toCopy = Math.min(len, 1);
                b[off] = payload[readCount];
                readCount++;
                return 1;
            }

            @Override
            public int read() throws IOException {
                return read(b, 0, 1);
            }
        };
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(flakyIn, outputStream, null), "IOException should propagate from the input stream");
        assertEquals("Simulated read failure", ex.getMessage());
        // Verify that the two successfully read bytes are present in the output
        byte[] expected = new byte[] { 1, 2 };
        assertArrayEquals(expected, outputStream.toByteArray(), "Output should contain bytes read before the exception");
    }

    @Test
    @DisplayName("OutputStream throws IOException on second write")
    void transfer_OutputStreamThrowsIOException() {
        // GIVEN
        // Input: 50 000 bytes (will require two buffer writes)
        byte[] data = new byte[50_000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        inputStream = new ByteArrayInputStream(data);
        OutputStream flakyOut = new OutputStream() {

            private int writeCalls = 0;

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                writeCalls++;
                if (writeCalls > 1) {
                    throw new IOException("Simulated write failure");
                }
                // First write succeeds – delegate to a real ByteArrayOutputStream
                outputStream.write(b, off, len);
            }

            @Override
            public void write(int b) throws IOException {
                // Not used by IOUtilities.transfer
                throw new UnsupportedOperationException();
            }
        };
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(inputStream, flakyOut, null), "IOException should propagate from the output stream");
        assertEquals("Simulated write failure", ex.getMessage());
        // Verify that only the first buffer (up to TRANSFER_BUFFER) was written
        int expectedBytes = Math.min(IOUtilities.TRANSFER_BUFFER, data.length);
        assertEquals(expectedBytes, outputStream.size(), "Only the first successful write should be present");
    }

    @Test
    @DisplayName("Exact buffer size (32768 bytes) – single callback invocation")
    void transfer_ExactBufferSize() throws IOException {
        // GIVEN
        byte[] data = new byte[IOUtilities.TRANSFER_BUFFER];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 128);
        inputStream = new ByteArrayInputStream(data);
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        when(callback.isCancelled()).thenReturn(false);
        // WHEN
        IOUtilities.transfer(inputStream, outputStream, callback);
        // THEN
        assertArrayEquals(data, outputStream.toByteArray(), "All bytes should be transferred for exact buffer size");
        verify(callback, times(1)).bytesTransferred(any(byte[].class), eq(IOUtilities.TRANSFER_BUFFER));
        verify(callback, never()).isCancelled();
    }

    @Test
    @DisplayName("Buffer size + 1 (32769 bytes) – two callback invocations")
    void transfer_BufferSizePlusOne() throws IOException {
        // GIVEN
        int total = IOUtilities.TRANSFER_BUFFER + 1;
        byte[] data = new byte[total];
        for (int i = 0; i < total; i++) data[i] = (byte) (i % 128);
        inputStream = new ByteArrayInputStream(data);
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        when(callback.isCancelled()).thenReturn(false);
        // WHEN
        IOUtilities.transfer(inputStream, outputStream, callback);
        // THEN
        assertArrayEquals(data, outputStream.toByteArray(), "All bytes should be transferred for buffer+1 size");
        // First call with full buffer, second with 1 byte
        InOrder inOrder = inOrder(callback);
        inOrder.verify(callback).bytesTransferred(any(byte[].class), eq(IOUtilities.TRANSFER_BUFFER));
        inOrder.verify(callback).bytesTransferred(any(byte[].class), eq(1));
        verify(callback, never()).isCancelled();
    }

    @Test
    @DisplayName("Null InputStream should cause NullPointerException")
    void transfer_NullInputStream() {
        // GIVEN
        InputStream nullIn = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(nullIn, outputStream, null), "Calling transfer with null InputStream must throw NPE");
    }

    @Test
    @DisplayName("Null OutputStream should cause NullPointerException")
    void transfer_NullOutputStream() {
        // GIVEN
        OutputStream nullOut = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(inputStream, nullOut, null), "Calling transfer with null OutputStream must throw NPE");
    }

    @Test
    @DisplayName("Callback bytesTransferred throws RuntimeException")
    void transfer_CallbackBytesTransferredThrows() {
        // GIVEN
        byte[] data = new byte[] { 9, 9, 9 };
        inputStream = new ByteArrayInputStream(data);
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        doThrow(new RuntimeException("callback failure")).when(callback).bytesTransferred(any(byte[].class), anyInt());
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(inputStream, outputStream, callback), "RuntimeException from callback should propagate");
        assertEquals("callback failure", ex.getMessage());
        // Verify that only the first buffer (the whole data) was attempted before exception
        // The output may contain partial data depending on when the exception was thrown;
        // we only assert that the method stopped after the exception.
    }

    @Test
    @DisplayName("Callback isCancelled throws RuntimeException")
    void transfer_CallbackIsCancelledThrows() {
        // GIVEN
        byte[] data = new byte[] { 5, 5, 5, 5 };
        inputStream = new ByteArrayInputStream(data);
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // bytesTransferred succeeds once
        doNothing().when(callback).bytesTransferred(any(byte[].class), anyInt());
        // isCancelled throws
        when(callback.isCancelled()).thenThrow(new RuntimeException("cancel check failure"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(inputStream, outputStream, callback), "RuntimeException from isCancelled should propagate");
        assertEquals("cancel check failure", ex.getMessage());
        // Verify that the first write happened before the exception
        assertEquals(IOUtilities.TRANSFER_BUFFER, outputStream.size(), "First buffer should have been written before cancellation check failed");
    }
}
