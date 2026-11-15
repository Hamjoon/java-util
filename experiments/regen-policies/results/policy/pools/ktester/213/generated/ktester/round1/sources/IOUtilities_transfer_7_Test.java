package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_7_Test {

    private static final byte[] SMALL_DATA = "Hello, IOUtilities!".getBytes();

    // > 1 buffer, multiple loops
    private static final int LARGE_SIZE = IOUtilities.TRANSFER_BUFFER * 3 + 123;

    /**
     * Helper to obtain the private {@code TRANSFER_BUFFER} constant via reflection.
     */
    private static int getTransferBufferSize() {
        try {
            Field f = IOUtilities.class.getDeclaredField("TRANSFER_BUFFER");
            f.setAccessible(true);
            return (int) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(Arguments.of(null, new ByteArrayOutputStream(), "InputStream cannot be null"), Arguments.of(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), null, "OutputStream cannot be null"));
    }

    /**
     * Simple copy test without a callback.
     */
    @Test
    void testTransferCopiesDataWithoutCallback() throws IOException {
        InputStream in = new ByteArrayInputStream(SMALL_DATA);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(in, out, null);
        assertArrayEquals(SMALL_DATA, out.toByteArray(), "The output must be identical to the input when no callback is supplied");
    }

    /**
     * Verify that the callback receives each chunk of data transferred.
     */
    @Test
    void testTransferInvokesCallbackForEachChunk() throws IOException {
        // Build a payload larger than the internal buffer to force multiple reads.
        byte[] largePayload = new byte[LARGE_SIZE];
        for (int i = 0; i < largePayload.length; i++) {
            largePayload[i] = (byte) (i % 256);
        }
        InputStream in = new ByteArrayInputStream(largePayload);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Mock callback – default isCancelled() returns false.
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        when(callback.isCancelled()).thenReturn(false);
        IOUtilities.transfer(in, out, callback);
        // Verify that the output matches the input.
        assertArrayEquals(largePayload, out.toByteArray(), "All bytes must be transferred to the output stream");
        // Capture the arguments passed to the callback.
        ArgumentCaptor<byte[]> bufferCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Integer> countCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(callback, atLeastOnce()).bytesTransferred(bufferCaptor.capture(), countCaptor.capture());
        // Ensure that each captured count is > 0 and ≤ TRANSFER_BUFFER.
        for (int count : countCaptor.getAllValues()) {
            assertTrue(count > 0, "Callback count must be positive");
            assertTrue(count <= IOUtilities.TRANSFER_BUFFER, "Callback count must not exceed the internal buffer size");
        }
        // The total of all counts should equal the payload size.
        int totalTransferred = countCaptor.getAllValues().stream().mapToInt(Integer::intValue).sum();
        assertEquals(LARGE_SIZE, totalTransferred, "Sum of callback counts must equal total number of transferred bytes");
    }

    /**
     * Ensure that a callback can cancel the transfer.
     */
    @Test
    void testTransferCancelsWhenCallbackRequestsIt() throws IOException {
        // Payload larger than one buffer.
        byte[] payload = new byte[IOUtilities.TRANSFER_BUFFER + 50];
        Arrays.fill(payload, (byte) 'x');
        InputStream in = new ByteArrayInputStream(payload);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Mock callback: first call returns false, second call returns true (cancel).
        IOUtilities.TransferCallback callback = mock(IOUtilities.TransferCallback.class);
        when(callback.isCancelled()).thenReturn(// after first chunk
        false).thenReturn(// after second chunk (should stop)
        true);
        // The callback will be invoked for the first chunk; after that transfer stops.
        IOUtilities.transfer(in, out, callback);
        // Verify that only the first chunk (up to buffer size) was written.
        byte[] written = out.toByteArray();
        assertEquals(IOUtilities.TRANSFER_BUFFER, written.length, "Transfer should stop after the callback signals cancellation");
        // Verify callback interaction order.
        InOrder inOrder = inOrder(callback);
        inOrder.verify(callback).bytesTransferred(any(byte[].class), eq(IOUtilities.TRANSFER_BUFFER));
        // after first chunk
        inOrder.verify(callback).isCancelled();
        // second chunk
        inOrder.verify(callback).bytesTransferred(any(byte[].class), anyInt());
        // after second chunk (true)
        inOrder.verify(callback).isCancelled();
        verifyNoMoreInteractions(callback);
    }

    /**
     * Passing a null InputStream must result in an exception (as enforced by Convention).
     */
    @Test
    void testTransferNullInputStream() {
        OutputStream out = new ByteArrayOutputStream();
        NullPointerException npe = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, out, null));
        assertTrue(npe.getMessage().contains("InputStream cannot be null"));
    }

    /**
     * Passing a null OutputStream must result in an exception (as enforced by Convention).
     */
    @Test
    void testTransferNullOutputStream() {
        InputStream in = new ByteArrayInputStream(SMALL_DATA);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(in, null, null));
        assertTrue(npe.getMessage().contains("OutputStream cannot be null"));
    }

    @ParameterizedTest
    @MethodSource("nullArgumentProvider")
    void testTransferNullChecks(InputStream in, OutputStream out, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.transfer(in, out, null));
        assertTrue(ex.getMessage().contains(expectedMessage), "Exception message must contain: " + expectedMessage);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferPropagatesIOExceptionFromInput() throws IOException {
        InputStream faultyIn = mock(InputStream.class);
        when(faultyIn.read(any(byte[].class))).thenThrow(new IOException("simulated read failure"));
        OutputStream out = new ByteArrayOutputStream();
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(faultyIn, out, null));
        assertEquals("simulated read failure", ex.getMessage(), "IOException from InputStream.read must be propagated unchanged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferPropagatesIOExceptionFromOutput() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 });
        OutputStream faultyOut = mock(OutputStream.class);
        // Throw on the first write call.
        doThrow(new IOException("simulated write failure")).when(faultyOut).write(any(byte[].class), anyInt(), anyInt());
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, faultyOut, null));
        assertEquals("simulated write failure", ex.getMessage(), "IOException from OutputStream.write must be propagated unchanged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferPropagatesRuntimeExceptionFromCallbackBytesTransferred() {
        InputStream in = new ByteArrayInputStream(new byte[] { 5, 6, 7, 8 });
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.TransferCallback badCallback = new IOUtilities.TransferCallback() {

            @Override
            public void bytesTransferred(byte[] bytes, int count) {
                throw new RuntimeException("callback error");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(in, out, badCallback));
        assertEquals("callback error", ex.getMessage(), "Unchecked exception from bytesTransferred must propagate");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTransferPropagatesRuntimeExceptionFromCallbackIsCancelled() {
        InputStream in = new ByteArrayInputStream(new byte[] { 9, 10 });
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.TransferCallback badCallback = new IOUtilities.TransferCallback() {

            @Override
            public void bytesTransferred(byte[] bytes, int count) {
                // No‑op
            }

            @Override
            public boolean isCancelled() {
                throw new RuntimeException("cancel check failed");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.transfer(in, out, badCallback));
        assertEquals("cancel check failed", ex.getMessage(), "Unchecked exception from isCancelled must propagate");
    }
}
