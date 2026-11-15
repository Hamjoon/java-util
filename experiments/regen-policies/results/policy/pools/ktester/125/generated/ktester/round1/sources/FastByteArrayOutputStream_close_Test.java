package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_close_Test {

    /**
     * Instance under test – created fresh for each test method.
     */
    private FastByteArrayOutputStream outputStream;

    /**
     * Executed before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Initialise a new instance for every test to guarantee isolation.
        outputStream = new FastByteArrayOutputStream();
    }

    /**
     * Executed after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference; not strictly required but mirrors typical cleanup.
        outputStream = null;
    }

    /**
     * Verifies that calling {@code close()} does not throw any exception.
     */
    @Test
    void testCloseDoesNotThrow() {
        assertDoesNotThrow(() -> outputStream.close(), "FastByteArrayOutputStream.close() should not throw any exception");
        assertEquals(0, outputStream.size(), "A newly created stream should still report size 0 after close()");
    }

    /**
     * Close Method Basic Behavior – the stream must stay usable after close().
     */
    @Test
    void testCloseBasicBehavior_streamRemainsUsable() throws IOException {
        assertDoesNotThrow(() -> outputStream.close(), "close() on a new stream should not throw");
        assertEquals(0, outputStream.size(), "Stream size should remain 0 after close()");
        // Verify that we can still write after close
        outputStream.write(new byte[] { 1, 2, 3 });
        assertArrayEquals(new byte[] { 1, 2, 3 }, outputStream.toByteArray(), "Data written after close() must be retained");
    }

    /**
     * Close Method Idempotence – calling close() multiple times must be safe.
     */
    @Test
    void testCloseIdempotence_multipleCalls() throws IOException {
        // first call
        assertDoesNotThrow(() -> outputStream.close(), "First close() should not throw");
        // second call
        assertDoesNotThrow(() -> outputStream.close(), "Second close() should not throw");
        // state unchanged
        assertEquals(0, outputStream.size(), "Stream size must stay 0 after repeated close() calls");
    }

    /**
     * Close Method After Write – data written before close() must stay accessible.
     */
    @Test
    void testCloseAfterWrite_dataPreserved() throws IOException {
        byte[] data = new byte[] { 1, 2, 3, 4, 5 };
        outputStream.write(data);
        assertDoesNotThrow(() -> outputStream.close(), "close() after write should not throw");
        assertArrayEquals(data, outputStream.toByteArray(), "Data written before close() must be unchanged");
    }

    /**
     * Close Method After Reset – stream should stay empty after reset and close().
     */
    @Test
    void testCloseAfterReset_streamEmpty() throws IOException {
        outputStream.write(new byte[] { 9, 9, 9 });
        outputStream.reset();
        assertEquals(0, outputStream.size(), "Size must be 0 after reset");
        assertDoesNotThrow(() -> outputStream.close(), "close() after reset should not throw");
        assertEquals(0, outputStream.size(), "Size must remain 0 after close()");
    }

    /**
     * Close Method Followed by Further Operations – close() must not prevent later writes.
     */
    @Test
    void testCloseFollowedByFurtherWrites() throws IOException {
        byte[] first = new byte[] { 10, 20 };
        byte[] second = new byte[] { 30 };
        outputStream.write(first);
        // no‑op
        outputStream.close();
        outputStream.write(second);
        assertArrayEquals(new byte[] { 10, 20, 30 }, outputStream.toByteArray(), "Writes after close() must succeed and be concatenated");
    }

    /**
     * Close Method After Single Byte Write – single‑byte write must survive close().
     */
    @Test
    void testCloseAfterSingleByteWrite() throws IOException {
        int byteValue = 123;
        outputStream.write(byteValue);
        outputStream.close();
        assertArrayEquals(new byte[] { (byte) byteValue }, outputStream.toByteArray(), "Single byte written before close() must be retained");
    }

    /**
     * Close Method After writeTo Invocation – close() must not affect source or target data.
     */
    @Test
    void testCloseAfterWriteTo() throws IOException {
        byte[] sourceData = new byte[] { 10, 20, 30, 40 };
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        outputStream.write(sourceData);
        outputStream.writeTo(target);
        outputStream.close();
        assertArrayEquals(sourceData, target.toByteArray(), "Data transferred to target must match source");
        assertArrayEquals(sourceData, outputStream.toByteArray(), "Source stream must remain unchanged after close()");
    }

    /**
     * Close Method After Buffer Expansion – large write forces internal buffer growth.
     */
    @Test
    void testCloseAfterBufferExpansion() throws IOException {
        byte[] largeData = new byte[5000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) i;
        }
        outputStream.write(largeData);
        outputStream.close();
        assertArrayEquals(largeData, outputStream.toByteArray(), "All 5000 bytes must be present after close()");
    }

    /**
     * Close Method After Reset Followed by Write – reset clears previous data.
     */
    @Test
    void testCloseAfterResetThenWrite() throws IOException {
        byte[] initialData = new byte[] { 5, 6, 7 };
        byte[] postResetData = new byte[] { 8, 9 };
        outputStream.write(initialData);
        outputStream.reset();
        outputStream.write(postResetData);
        outputStream.close();
        assertArrayEquals(postResetData, outputStream.toByteArray(), "Only data written after reset should remain after close()");
    }

    /**
     * Close Method After toString Invocation – textual representation must stay the same.
     */
    @Test
    void testCloseAfterToString() throws IOException {
        byte[] textData = "HelloWorld".getBytes(StandardCharsets.UTF_8);
        outputStream.write(textData);
        String before = outputStream.toString();
        outputStream.close();
        String after = outputStream.toString();
        assertEquals("HelloWorld", before, "toString() before close() must return the original string");
        assertEquals(before, after, "toString() after close() must remain unchanged");
    }

    /**
     * Close Method After getBuffer Invocation – the internal buffer reference must stay valid.
     */
    @Test
    void testCloseAfterGetBuffer() throws IOException {
        byte[] bytes = new byte[] { 1, 2, 3 };
        outputStream.write(bytes);
        byte[] bufferBefore = outputStream.getBuffer();
        // Ensure the buffer contains the data (may be larger than size, but first three bytes must match)
        assertEquals(bytes[0], bufferBefore[0]);
        assertEquals(bytes[1], bufferBefore[1]);
        assertEquals(bytes[2], bufferBefore[2]);
        outputStream.close();
        byte[] bufferAfter = outputStream.getBuffer();
        assertSame(bufferBefore, bufferAfter, "getBuffer() must return the same array reference before and after close()");
        assertArrayEquals(bytes, Arrays.copyOf(bufferAfter, 3), "Content of internal buffer must stay unchanged after close()");
    }

    /**
     * Close Method After Size Check – size() must be stable across close().
     */
    @Test
    void testCloseAfterSizeCheck() throws IOException {
        byte[] bytes = new byte[] { 9, 9, 9, 9 };
        outputStream.write(bytes);
        assertEquals(4, outputStream.size(), "Size before close() must be 4");
        outputStream.close();
        assertEquals(4, outputStream.size(), "Size after close() must still be 4");
    }

    /**
     * Close Method Interleaved with Writes and Resets – only data after the last reset should survive.
     */
    @Test
    void testInterleavedCloseWriteReset() throws IOException {
        byte[] firstWrite = new byte[] { 1, 1, 1 };
        byte[] secondWrite = new byte[] { 2, 2 };
        byte[] thirdWrite = new byte[] { 3 };
        outputStream.write(firstWrite);
        // no‑op
        outputStream.close();
        outputStream.write(secondWrite);
        // clears first and second writes
        outputStream.reset();
        // no‑op again
        outputStream.close();
        outputStream.write(thirdWrite);
        assertArrayEquals(thirdWrite, outputStream.toByteArray(), "Only bytes written after the final reset should remain");
    }

    /**
     * Close Method Null WriteTo Argument – writeTo(null) must throw NPE, close() must not mask it.
     */
    @Test
    void testWriteToNullArgumentThrowsNPE_thenClose() {
        outputStream.write(new byte[] { 1, 2, 3 });
        NullPointerException npe = assertThrows(NullPointerException.class, () -> outputStream.writeTo(null), "writeTo(null) must throw NullPointerException");
        assertNotNull(npe, "Exception instance should not be null");
        // After the exception, close() must still be a no‑op and not re‑throw.
        assertDoesNotThrow(() -> outputStream.close(), "close() after a failed writeTo should not throw");
    }
}
