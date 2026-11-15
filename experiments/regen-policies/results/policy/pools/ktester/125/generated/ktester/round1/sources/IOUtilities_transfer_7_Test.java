package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_7_Test {

    /* --------------------------------------------------------------------- */
    private static int getTransferBufferSize() {
        try {
            Field f = IOUtilities.class.getDeclaredField("TRANSFER_BUFFER");
            f.setAccessible(true);
            return (int) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * --------------------------------------------------------------------- **
     */
    @Test
    void testTransferCopiesDataWhenCallbackIsNull() throws IOException {
        // GIVEN
        byte[] source = "The quick brown fox jumps over the lazy dog".getBytes();
        InputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // WHEN
        IOUtilities.transfer(in, out, null);
        // THEN
        assertArrayEquals(source, out.toByteArray(), "Output stream should contain exactly the bytes read from the input stream");
    }

    /**
     * --------------------------------------------------------------------- **
     */
    @Test
    void testTransferInvokesCallbackWhenProvided() throws IOException {
        // GIVEN
        // small enough for a single buffer
        byte[] source = new byte[100];
        for (int i = 0; i < source.length; i++) source[i] = (byte) i;
        InputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Create a mock TransferCallback
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // Callback never cancels the transfer
        when(callback.isCancelled()).thenReturn(false);
        // WHEN
        IOUtilities.transfer(in, out, callback);
        // THEN
        // Verify that the callback was called exactly once (the whole data fits in one buffer)
        verify(callback, times(1)).bytesTransferred(any(byte[].class), eq(source.length));
        // Verify that the callback was asked whether to cancel after the write
        verify(callback, times(1)).isCancelled();
        // The data must still be transferred correctly
        assertArrayEquals(source, out.toByteArray(), "Data should be transferred correctly even when a callback is present");
    }

    /**
     * --------------------------------------------------------------------- **
     */
    @Test
    void testTransferStopsWhenCallbackCancels() throws IOException {
        // GIVEN
        // Create data larger than the internal buffer (32 768) to force at least two reads
        // two full buffers + a partial
        int totalSize = IOUtilities.TRANSFER_BUFFER * 2 + 123;
        byte[] source = new byte[totalSize];
        for (int i = 0; i < totalSize; i++) source[i] = (byte) (i % 256);
        InputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Mock callback: first call returns false (continue), second call returns true (cancel)
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // The first invocation of isCancelled() returns false, the second returns true
        when(callback.isCancelled()).thenReturn(false, true);
        // No special behaviour for bytesTransferred – we just need the method to be called
        doNothing().when(callback).bytesTransferred(any(byte[].class), anyInt());
        // WHEN
        IOUtilities.transfer(in, out, callback);
        // THEN
        // The output should contain exactly one full buffer (the internal buffer size)
        assertEquals(IOUtilities.TRANSFER_BUFFER, out.size(), "When cancelled, only the first buffer should be written");
        // Verify that bytesTransferred was called at least once with the correct count
        verify(callback, atLeastOnce()).bytesTransferred(any(byte[].class), eq(IOUtilities.TRANSFER_BUFFER));
        // Verify that isCancelled() was consulted after the first write and caused the break
        verify(callback, atLeast(1)).isCancelled();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferWithEmptyInputStream() throws IOException {
        // GIVEN
        // empty
        InputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        // WHEN
        IOUtilities.transfer(in, out, callback);
        // THEN
        assertEquals(0, out.size(), "Output should remain empty for an empty input");
        verify(callback, never()).bytesTransferred(any(), anyInt());
        verify(callback, never()).isCancelled();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferMultipleBuffersWithCallbackRecordingCounts() throws IOException {
        // GIVEN
        // > 3 * TRANSFER_BUFFER
        final int totalSize = 100_000;
        byte[] source = new byte[totalSize];
        for (int i = 0; i < totalSize; i++) source[i] = (byte) (i % 100);
        InputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        when(callback.isCancelled()).thenReturn(false);
        doNothing().when(callback).bytesTransferred(any(byte[].class), anyInt());
        // WHEN
        IOUtilities.transfer(in, out, callback);
        // THEN
        assertEquals(totalSize, out.size(), "All bytes must be transferred");
        // Capture the count argument of each callback invocation
        ArgumentCaptor<Integer> countCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(callback, atLeastOnce()).bytesTransferred(any(byte[].class), countCaptor.capture());
        List<Integer> counts = countCaptor.getAllValues();
        // The sum of all counts must equal the total size
        int sum = counts.stream().mapToInt(Integer::intValue).sum();
        assertEquals(totalSize, sum, "Callback must receive the exact total number of bytes");
        // Every count must be <= buffer size and the last count must be the remainder
        int buffer = getTransferBufferSize();
        for (int i = 0; i < counts.size(); i++) {
            int cnt = counts.get(i);
            assertTrue(cnt > 0 && cnt <= buffer, "Each callback count must be >0 and <= buffer size");
            if (i == counts.size() - 1) {
                int expectedLast = totalSize % buffer;
                if (expectedLast == 0)
                    expectedLast = buffer;
                assertEquals(expectedLast, cnt, "The final callback count should match the remaining bytes");
            }
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferPropagatesIOExceptionFromOutput() {
        // GIVEN
        InputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        OutputStream faultyOut = new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                throw new IOException("forced failure");
            }
        };
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, faultyOut, null));
        assertEquals("forced failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferWithNullInputStream() {
        // GIVEN
        InputStream in = null;
        OutputStream out = new ByteArrayOutputStream();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(in, out, null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferWithNullOutputStream() {
        // GIVEN
        InputStream in = new ByteArrayInputStream(new byte[] { 9, 9, 9 });
        OutputStream out = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IOUtilities.transfer(in, out, null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testCallbackMutationDoesNotAffectWrittenData() throws IOException {
        // GIVEN
        byte[] source = new byte[] { 7, 8, 9 };
        InputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.TransferCallback mutatingCallback = new IOUtilities.TransferCallback() {

            @Override
            public void bytesTransferred(byte[] bytes, int count) {
                // Mutate the buffer after the data has already been written
                for (int i = 0; i < count; i++) {
                    bytes[i] = 0;
                }
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        // WHEN
        IOUtilities.transfer(in, out, mutatingCallback);
        // THEN
        assertArrayEquals(source, out.toByteArray(), "Mutating the callback's buffer must not affect data already written to the output");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferPropagatesIOExceptionFromInput() {
        // GIVEN
        InputStream faultyIn = new InputStream() {

            private int reads = 0;

            @Override
            public int read() throws IOException {
                if (reads++ == 0) {
                    // first byte
                    return 0x55;
                }
                throw new IOException("forced read failure");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (reads++ == 0) {
                    b[off] = (byte) 0x55;
                    return 1;
                }
                throw new IOException("forced read failure");
            }
        };
        OutputStream out = new ByteArrayOutputStream();
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(faultyIn, out, null));
        assertEquals("forced read failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferPropagatesRuntimeExceptionFromCallbackBytesTransferred() {
        // GIVEN
        InputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });
        OutputStream out = new ByteArrayOutputStream();
        IOUtilities.TransferCallback badCallback = mock(IOUtilities.TransferCallback.class);
        doThrow(new RuntimeException("callback failure")).when(badCallback).bytesTransferred(any(byte[].class), anyInt());
        when(badCallback.isCancelled()).thenReturn(false);
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(in, out, badCallback));
        assertEquals("callback failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferPropagatesExceptionFromCallbackIsCancelled() {
        // GIVEN
        InputStream in = new ByteArrayInputStream(new byte[] { 5, 6, 7, 8 });
        OutputStream out = new ByteArrayOutputStream();
        IOUtilities.TransferCallback badCallback = mock(IOUtilities.TransferCallback.class);
        // bytesTransferred works fine
        doNothing().when(badCallback).bytesTransferred(any(byte[].class), anyInt());
        // isCancelled throws
        when(badCallback.isCancelled()).thenThrow(new IllegalStateException("cancel check failed"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.transfer(in, out, badCallback));
        assertEquals("cancel check failed", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferPartialWriteThenIOException() {
        // GIVEN
        final int bufferSize = getTransferBufferSize();
        // two full buffers
        byte[] source = new byte[bufferSize * 2];
        for (int i = 0; i < source.length; i++) source[i] = (byte) 0xAA;
        InputStream in = new ByteArrayInputStream(source);
        OutputStream partialFaultyOut = new OutputStream() {

            private int written = 0;

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (written >= bufferSize) {
                    throw new IOException("forced failure after first buffer");
                }
                // write the allowed part
                int toWrite = Math.min(len, bufferSize - written);
                written += toWrite;
                // (no actual storage needed for the test)
            }

            @Override
            public void write(int b) throws IOException {
                // Not used by IOUtilities.transfer (it uses write(byte[],int,int))
                throw new IOException("single-byte write not supported");
            }
        };
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, partialFaultyOut, null));
        assertEquals("forced failure after first buffer", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferWithNullCallbackDoesNotThrow() throws IOException {
        // GIVEN
        byte[] data = "hello".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // WHEN – callback is null
        IOUtilities.transfer(in, out, null);
        // THEN
        assertArrayEquals(data, out.toByteArray(), "Transfer should succeed even when the callback argument is null");
    }
}
