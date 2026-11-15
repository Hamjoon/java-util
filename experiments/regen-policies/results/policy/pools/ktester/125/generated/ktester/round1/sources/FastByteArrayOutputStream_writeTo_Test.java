package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_writeTo_Test {

    private FastByteArrayOutputStream fastOut;

    private byte[] sampleData;

    /**
     * Simple OutputStream that records the parameters of a single write call.
     */
    private static class RecordingOutputStream extends OutputStream {

        int invocationCount = 0;

        int offset = -1;

        int length = -1;

        byte[] writtenBytes = null;

        @Override
        public void write(byte[] b, int off, int len) {
            invocationCount++;
            offset = off;
            length = len;
            writtenBytes = Arrays.copyOfRange(b, off, off + len);
        }

        // The single‑byte write method delegates to the array version (as ByteArrayOutputStream does)
        @Override
        public void write(int b) {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }
    }

    /**
     * OutputStream that always throws a RuntimeException on any write.
     */
    private static class RuntimeExceptionOutputStream extends OutputStream {

        @Override
        public void write(int b) {
            throw new RuntimeException("forced runtime exception");
        }

        @Override
        public void write(byte[] b, int off, int len) {
            throw new RuntimeException("forced runtime exception");
        }
    }

    /**
     * OutputStream that throws IllegalArgumentException when length > 0.
     */
    private static class IllegalArgumentOutputStream extends OutputStream {

        @Override
        public void write(int b) {
            // single byte write is allowed – we only care about the array version
        }

        @Override
        public void write(byte[] b, int off, int len) {
            if (len > 0) {
                throw new IllegalArgumentException("length must be zero");
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        fastOut = new FastByteArrayOutputStream();
        sampleData = "FastByteArrayOutputStream".getBytes();
        fastOut.write(sampleData);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset references to help GC; not strictly required
        fastOut = null;
        sampleData = null;
    }

    /**
     * Verify that {@code writeTo(OutputStream)} copies the exact bytes
     * that have been written to the {@link FastByteArrayOutputStream}
     * into the supplied {@link OutputStream}.
     */
    @Test
    void testWriteToCopiesDataCorrectly() throws IOException {
        // given: a real ByteArrayOutputStream to capture the data
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        // when: the data is written to the target stream
        fastOut.writeTo(target);
        // then: the target contains an exact copy of the original bytes
        assertArrayEquals(sampleData, target.toByteArray(), "The bytes written to the target OutputStream should match the source data");
    }

    /**
     * Verify that an {@link IOException} thrown by the destination stream
     * is propagated by {@code writeTo}.
     */
    @Test
    void testWriteToPropagatesIOException(@Mock OutputStream failingOut) throws IOException {
        // given: the mock is configured to throw IOException on write
        doThrow(new IOException("forced failure")).when(failingOut).write(any(byte[].class), eq(0), anyInt());
        // when / then: the exception is propagated
        IOException ex = assertThrows(IOException.class, () -> fastOut.writeTo(failingOut), "writeTo should propagate IOException from the underlying OutputStream");
        assertEquals("forced failure", ex.getMessage());
    }

    /**
     * Normal operation – empty buffer.
     * No data has been written to the stream (count == 0). The destination
     * must remain empty after {@code writeTo}.
     */
    @Test
    void testWriteToEmptyBuffer() throws IOException {
        // no writes
        FastByteArrayOutputStream empty = new FastByteArrayOutputStream();
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        empty.writeTo(target);
        assertEquals(0, target.size(), "Target stream should be empty when source buffer is empty");
    }

    /**
     * Normal operation – small payload.
     * Write three distinct bytes and verify they are copied verbatim.
     */
    @Test
    void testWriteToSmallPayload() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] payload = new byte[] { 0x01, 0x02, 0x03 };
        out.write(payload);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        out.writeTo(target);
        assertArrayEquals(payload, target.toByteArray(), "Target should contain exactly the three bytes that were written");
    }

    /**
     * Normal operation – buffer expansion.
     * Write 10 000 bytes (value 0x7F) to force internal buffer growth and
     * ensure the whole payload is transferred.
     */
    @Test
    void testWriteToBufferExpansion() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] large = new byte[10_000];
        Arrays.fill(large, (byte) 0x7F);
        out.write(large);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        out.writeTo(target);
        assertArrayEquals(large, target.toByteArray(), "All 10 000 bytes must be written after buffer expansion");
    }

    /**
     * Exception handling – null OutputStream.
     * Passing {@code null} should result in a {@link NullPointerException}
     * when the method attempts to invoke {@code out.write(...)}.
     */
    @Test
    void testWriteToNullOutputStream() {
        assertThrows(NullPointerException.class, () -> fastOut.writeTo(null), "Calling writeTo with a null OutputStream must throw NPE");
    }

    /**
     * Edge case – count larger than internal buffer (set via reflection).
     * The method does not perform its own bounds checking, so the underlying
     * {@link OutputStream} will receive an illegal length and an exception
     * (typically {@link IndexOutOfBoundsException}) is expected.
     */
    @Test
    void testWriteToCountLargerThanBuffer() throws Exception {
        // Force count to be larger than the actual buffer length
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        bufField.setAccessible(true);
        countField.setAccessible(true);
        byte[] originalBuf = (byte[]) bufField.get(fastOut);
        int originalCount = (int) countField.get(fastOut);
        // Set count to buf.length + 10
        countField.setInt(fastOut, originalBuf.length + 10);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        // The underlying write will throw an IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> fastOut.writeTo(target), "writeTo should propagate IndexOutOfBoundsException when count exceeds buffer size");
        // Restore original state for safety (not strictly needed because each test gets a fresh instance)
        countField.setInt(fastOut, originalCount);
    }

    /**
     * State after {@code reset()} – the internal count becomes zero.
     * Subsequent {@code writeTo} must produce an empty destination.
     */
    @Test
    void testWriteToAfterReset() throws IOException {
        // write some data first
        fastOut.write(new byte[] { 0x11, 0x22, 0x33 });
        // clears count
        fastOut.reset();
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        fastOut.writeTo(target);
        assertEquals(0, target.size(), "After reset, writeTo should write zero bytes, leaving the destination empty");
    }

    /**
     * Multiple {@code writeTo} invocations – the same data should be written
     * each time, resulting in concatenated output.
     */
    @Test
    void testMultipleWriteToInvocations() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] payload = new byte[] { 0x0A, 0x0B };
        out.write(payload);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        out.writeTo(target);
        // second invocation
        out.writeTo(target);
        byte[] expected = new byte[] { 0x0A, 0x0B, 0x0A, 0x0B };
        assertArrayEquals(expected, target.toByteArray(), "Two successive writeTo calls must concatenate the same bytes");
    }

    /**
     * Custom {@link OutputStream} that records the arguments of the single
     * {@code write(byte[],int,int)} call.
     */
    @Test
    void testCustomOutputStreamParameterVerification() throws IOException {
        // Prepare known payload
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] payload = new byte[] { 0x55, 0x66, 0x77 };
        out.write(payload);
        RecordingOutputStream recorder = new RecordingOutputStream();
        out.writeTo(recorder);
        assertEquals(1, recorder.invocationCount, "Exactly one write call should be recorded");
        assertEquals(0, recorder.offset, "Offset must be zero");
        assertEquals(out.size(), recorder.length, "Length must equal FastByteArrayOutputStream size");
        assertArrayEquals(payload, recorder.writtenBytes, "Recorded bytes must be identical to the source buffer");
    }

    /**
     * Negative count via reflection – should trigger an {@link IndexOutOfBoundsException}
     * (or {@link ArrayIndexOutOfBoundsException}) because the underlying stream receives
     * a negative length.
     */
    @Test
    void testNegativeCountViaReflection() throws Exception {
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(fastOut, -5);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        assertThrows(IndexOutOfBoundsException.class, () -> fastOut.writeTo(target), "Negative count must cause an IndexOutOfBoundsException");
    }

    /**
     * WriteTo after {@code close()} – the close method is a no‑op, so data must still be
     * writable to the supplied stream.
     */
    @Test
    void testWriteToAfterClose() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] payload = new byte[] { 0x55, 0x66 };
        out.write(payload);
        // no‑op
        out.close();
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        out.writeTo(target);
        assertArrayEquals(payload, target.toByteArray(), "Data must still be written after calling close()");
    }

    /**
     * Internal buffer set to {@code null} via reflection – should result in a
     * {@link NullPointerException} when the destination stream attempts to write.
     */
    @Test
    void testInternalBufferNull() throws Exception {
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        // make internal buffer null
        bufField.set(fastOut, null);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        assertThrows(NullPointerException.class, () -> fastOut.writeTo(target), "Null internal buffer must cause NullPointerException");
    }

    /**
     * OutputStream that throws a unchecked {@link RuntimeException} on write.
     * The exception must propagate unchanged.
     */
    @Test
    void testOutputStreamThrowsRuntimeException() {
        RuntimeExceptionOutputStream badOut = new RuntimeExceptionOutputStream();
        assertThrows(RuntimeException.class, () -> fastOut.writeTo(badOut), "RuntimeException from the destination stream must propagate");
    }

    /**
     * OutputStream that validates the length argument and throws
     * {@link IllegalArgumentException} when length > 0.
     */
    @Test
    void testOutputStreamThrowsIllegalArgumentException() {
        IllegalArgumentOutputStream illegalOut = new IllegalArgumentOutputStream();
        assertThrows(IllegalArgumentException.class, () -> fastOut.writeTo(illegalOut), "IllegalArgumentException from the destination stream must propagate");
    }
}
