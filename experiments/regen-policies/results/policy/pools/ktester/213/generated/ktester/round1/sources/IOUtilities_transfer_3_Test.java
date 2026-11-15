package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_3_Test {

    /**
     * Sample data used by the normal‑transfer test.
     */
    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    /**
     * A fresh {@link ByteArrayInputStream} for each test that needs it.
     */
    private ByteArrayInputStream freshInput;

    /**
     * A fresh {@link ByteArrayOutputStream} for each test that needs it.
     */
    private ByteArrayOutputStream freshOutput;

    /**
     * InputStream that throws an {@link IOException} after a configurable number of successful reads.
     */
    private static final class FailingInputStream extends InputStream {

        private final byte[] data;

        // number of bytes after which the next read fails
        private final int failAfter;

        private int pos = 0;

        private int bytesReturned = 0;

        FailingInputStream(byte[] data, int failAfter) {
            this.data = data.clone();
            this.failAfter = failAfter;
        }

        @Override
        public int read() throws IOException {
            if (bytesReturned >= failAfter) {
                throw new IOException("Simulated read failure after " + failAfter + " bytes");
            }
            if (pos >= data.length) {
                return -1;
            }
            bytesReturned++;
            return data[pos++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            // delegate to the single‑byte read to keep the failure logic simple
            if (b == null) {
                throw new NullPointerException();
            }
            if (len == 0) {
                return 0;
            }
            int first = read();
            if (first == -1) {
                return -1;
            }
            b[off] = (byte) first;
            return 1;
        }
    }

    /**
     * OutputStream that throws an {@link IOException} after a configurable number of successful writes.
     */
    private static final class FailingOutputStream extends OutputStream {

        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        // number of bytes after which the next write fails
        private final int failAfter;

        private int bytesWritten = 0;

        FailingOutputStream(int failAfter) {
            this.failAfter = failAfter;
        }

        @Override
        public void write(int b) throws IOException {
            if (bytesWritten >= failAfter) {
                throw new IOException("Simulated write failure after " + failAfter + " bytes");
            }
            delegate.write(b);
            bytesWritten++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (bytesWritten + len > failAfter) {
                int allowed = failAfter - bytesWritten;
                if (allowed > 0) {
                    delegate.write(b, off, allowed);
                    bytesWritten += allowed;
                }
                throw new IOException("Simulated write failure after " + failAfter + " bytes");
            }
            delegate.write(b, off, len);
            bytesWritten += len;
        }

        byte[] toByteArray() {
            return delegate.toByteArray();
        }
    }

    /**
     * InputStream that throws a RuntimeException on any read.
     */
    private static final class RuntimeExceptionInputStream extends InputStream {

        private final byte[] data;

        RuntimeExceptionInputStream(byte[] data) {
            this.data = data.clone();
        }

        @Override
        public int read() {
            throw new RuntimeException("Unchecked exception from InputStream");
        }
    }

    /**
     * OutputStream that throws a RuntimeException on any write.
     */
    private static final class RuntimeExceptionOutputStream extends OutputStream {

        @Override
        public void write(int b) {
            throw new RuntimeException("Unchecked exception from OutputStream");
        }
    }

    /* ------------------------------------------------------------
     *  Utility holder class for parameterised tests
     * ------------------------------------------------------------ */
    private static final class ArgumentsHolder {

        final InputStream in;

        final OutputStream out;

        // non‑null for successful copy cases
        final byte[] expectedBytes;

        // non‑null for error cases
        final Class<? extends Exception> expectedException;

        final String description;

        // Constructor for successful copy cases
        ArgumentsHolder(InputStream in, OutputStream out, byte[] expectedBytes, String description) {
            this.in = in;
            this.out = out;
            this.expectedBytes = expectedBytes;
            this.expectedException = null;
            this.description = description;
        }

        // Constructor for exception cases
        ArgumentsHolder(InputStream in, OutputStream out, Class<? extends Exception> expectedException, String description) {
            this.in = in;
            this.out = out;
            this.expectedBytes = null;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise fresh streams for each test case.
        freshInput = new ByteArrayInputStream(SAMPLE_DATA);
        freshOutput = new ByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Ensure streams are closed – the implementation of transfer does not close them,
        // so we are responsible for cleanup.
        freshInput.close();
        freshOutput.close();
    }

    /**
     * Returns the private constant {@code TRANSFER_BUFFER} via reflection.
     */
    private static int getTransferBufferSize() {
        try {
            Field f = IOUtilities.class.getDeclaredField("TRANSFER_BUFFER");
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Exception e) {
            throw new AssertionError("Unable to read TRANSFER_BUFFER", e);
        }
    }

    /**
     * Creates a byte array of the given length, filling it with a repeating 0‑255 pattern.
     */
    private static byte[] createByteArray(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        return data;
    }

    /**
     * Supplies the normal‑transfer test cases (no exception expected).
     */
    private static Stream<ArgumentsHolder> normalTransferProvider() {
        int bufferSize = getTransferBufferSize();
        return Stream.of(// Input smaller than internal buffer (5 bytes)
        new ArgumentsHolder(new ByteArrayInputStream(new byte[] { 10, 20, 30, 40, 50 }), new ByteArrayOutputStream(), new byte[] { 10, 20, 30, 40, 50 }, "InputStream smaller than the internal buffer (5 bytes) – all bytes must be copied exactly."), // Input exactly the size of the buffer
        new ArgumentsHolder(new ByteArrayInputStream(createByteArray(bufferSize)), new ByteArrayOutputStream(), createByteArray(bufferSize), "InputStream exactly the size of the buffer (" + bufferSize + " bytes) – single iteration."), // Input larger than the buffer (100 000 bytes)
        new ArgumentsHolder(new ByteArrayInputStream(createByteArray(100_000)), new ByteArrayOutputStream(), createByteArray(100_000), "InputStream larger than the buffer (100 000 bytes) – multiple iterations."));
    }

    /**
     * Supplies the exception‑propagation test cases.
     */
    private static Stream<ArgumentsHolder> exceptionTransferProvider() {
        return Stream.of(// InputStream throws IOException after three bytes
        new ArgumentsHolder(new FailingInputStream(new byte[] { 1, 2, 3, 4, 5 }, 3), new ByteArrayOutputStream(), IOException.class, "InputStream throws IOException after three bytes – exception propagates."), // OutputStream throws IOException after 30 bytes
        new ArgumentsHolder(new ByteArrayInputStream(createByteArray(50)), new FailingOutputStream(30), IOException.class, "OutputStream throws IOException after 30 bytes – exception propagates."), // InputStream throws IOException on first read (failAfter=0)
        new ArgumentsHolder(new FailingInputStream(new byte[] { 1, 2, 3 }, 0), new ByteArrayOutputStream(), IOException.class, "InputStream throws IOException on the very first read."), // OutputStream throws IOException on first write (failAfter=0)
        new ArgumentsHolder(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 }), new FailingOutputStream(0), IOException.class, "OutputStream throws IOException on the first write."), // InputStream throws unchecked RuntimeException
        new ArgumentsHolder(new RuntimeExceptionInputStream(new byte[] { 9, 9, 9 }), new ByteArrayOutputStream(), RuntimeException.class, "InputStream throws unchecked RuntimeException – should propagate."), // OutputStream throws unchecked RuntimeException
        new ArgumentsHolder(new ByteArrayInputStream(new byte[] { 7, 8, 9 }), new RuntimeExceptionOutputStream(), RuntimeException.class, "OutputStream throws unchecked RuntimeException – should propagate."));
    }

    /**
     * Verify that a normal transfer copies all bytes from the input to the output.
     */
    @Test
    void testTransferCopiesAllBytes() throws IOException {
        // when
        IOUtilities.transfer(freshInput, freshOutput);
        // then
        assertArrayEquals(SAMPLE_DATA, freshOutput.toByteArray(), "The output stream should contain exactly the bytes read from the input stream");
    }

    /**
     * Verify that transferring an empty stream results in an empty output.
     */
    @Test
    void testTransferEmptyInput() throws IOException {
        // given
        ByteArrayInputStream emptyIn = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // when
        IOUtilities.transfer(emptyIn, out);
        // then
        assertEquals(0, out.size(), "Output should be empty when the input stream has no data");
        // cleanup
        emptyIn.close();
        out.close();
    }

    /**
     * Verify that a {@code null} input stream causes the method to throw an exception.
     * The exact exception type depends on {@code Convention.throwIfNull}, but it is a
     * {@link RuntimeException} (typically {@link NullPointerException} or {@link IllegalArgumentException}).
     */
    @Test
    void testTransferNullInputThrows() {
        // given
        OutputStream out = new ByteArrayOutputStream();
        // when / then
        assertThrows(RuntimeException.class, () -> IOUtilities.transfer(null, out));
    }

    /**
     * Verify that a {@code null} output stream causes the method to throw an exception.
     */
    @Test
    void testTransferNullOutputThrows() {
        // given
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        // when / then
        assertThrows(RuntimeException.class, () -> IOUtilities.transfer(in, null));
    }

    /**
     * Simulate an {@link IOException} thrown while reading from the input stream.
     * The exception should propagate unchanged.
     */
    @Test
    void testTransferInputThrowsIOException() throws IOException {
        // given
        InputStream badIn = mock(InputStream.class);
        when(badIn.read(any(byte[].class))).thenThrow(new IOException("simulated read failure"));
        OutputStream out = new ByteArrayOutputStream();
        // when / then
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(badIn, out));
        assertEquals("simulated read failure", ex.getMessage());
        // cleanup
        out.close();
    }

    /**
     * Simulate an {@link IOException} thrown while writing to the output stream.
     * The exception should propagate unchanged.
     */
    @Test
    void testTransferOutputThrowsIOException() throws IOException {
        // given
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        OutputStream badOut = mock(OutputStream.class);
        // The first write attempt will throw.
        doThrow(new IOException("simulated write failure")).when(badOut).write(any(byte[].class), anyInt(), anyInt());
        // when / then
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, badOut));
        assertEquals("simulated write failure", ex.getMessage());
        // cleanup
        in.close();
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalTransferProvider")
    void testTransferVariousSizes(ArgumentsHolder holder) throws IOException {
        IOUtilities.transfer(holder.in, holder.out);
        assertArrayEquals(holder.expectedBytes, holder.out.toByteArray(), "Transferred bytes must match the source data");
        // Ensure streams stay open
        assertDoesNotThrow(() -> holder.in.read(), "Input stream should remain open after transfer");
        assertDoesNotThrow(() -> holder.out.write(0), "Output stream should remain open after transfer");
        holder.in.close();
        holder.out.close();
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionTransferProvider")
    void testTransferExceptionPropagation(ArgumentsHolder holder) {
        // The holder contains either an expected exception class or expected bytes.
        if (holder.expectedException != null) {
            Exception ex = assertThrows(holder.expectedException, () -> IOUtilities.transfer(holder.in, holder.out));
            // No further assertions on the message – the JSON only cares about the type.
        } else {
            fail("Test data error – expectedException must be set for this provider.");
        }
        // Ensure streams are still open (if they are closeable)
        if (holder.in instanceof InputStream) {
            assertDoesNotThrow(() -> ((InputStream) holder.in).read(), "Input stream should remain open after exception");
        }
        if (holder.out instanceof OutputStream) {
            assertDoesNotThrow(() -> ((OutputStream) holder.out).write(0), "Output stream should remain open after exception");
        }
    }
}
