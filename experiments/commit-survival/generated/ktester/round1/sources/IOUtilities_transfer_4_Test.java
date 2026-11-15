package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_4_Test {

    /**
     * Null argument checks – both the InputStream and the destination array must be non‑null.
     */
    @Nested
    class NullArgumentTests {

        @Test
        void shouldThrowNPEWhenInputStreamIsNull() {
            byte[] dest = new byte[3];
            assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, dest));
        }

        @Test
        void shouldThrowNPEWhenDestinationArrayIsNull() {
            InputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
            assertThrows(NullPointerException.class, () -> IOUtilities.transfer(in, null));
        }
    }

    private static Stream<Arguments> provideSuccessfulTransferScenarios() {
        return Stream.of(Arguments.of("Exact size transfer", new byte[] { 1, 2, 3, 4, 5 }, 5, new byte[] { 1, 2, 3, 4, 5 }), Arguments.of("Larger stream, truncate", new byte[] { 10, 20, 30, 40, 50, 60 }, 4, new byte[] { 10, 20, 30, 40 }));
    }

    private static Stream<Arguments> provideInsufficientDataScenarios() {
        return Stream.of(Arguments.of("Stream shorter than destination", new byte[] { 1, 2 }, 5), Arguments.of("Empty stream with non‑zero destination", new byte[0], 3));
    }

    /**
     * Verify that {@link IOUtilities#transfer(InputStream, byte[])} reads the exact
     * number of bytes from the supplied {@link InputStream} into the destination
     * array.
     */
    @Test
    void shouldTransferAllBytesWhenStreamContainsEnoughData() throws IOException {
        // given
        // 13 bytes
        byte[] source = "Hello, world!".getBytes();
        InputStream in = new ByteArrayInputStream(source);
        byte[] destination = new byte[source.length];
        // when
        IOUtilities.transfer(in, destination);
        // then
        assertArrayEquals(source, destination, "The destination array should contain exactly the bytes read from the InputStream");
        // also ensure stream is at EOF after the transfer
        assertEquals(-1, in.read(), "After reading all bytes the InputStream should be exhausted");
    }

    /**
     * Verify that an {@link IOException} is thrown when the {@link InputStream}
     * ends before the required number of bytes have been read.
     */
    @Test
    void shouldThrowIOExceptionWhenStreamEndsPrematurely() {
        // given
        // 5 bytes
        byte[] source = "short".getBytes();
        InputStream in = new ByteArrayInputStream(source);
        // request more bytes than available
        byte[] destination = new byte[10];
        // when / then
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, destination), "Expected an IOException because the InputStream does not contain enough bytes");
        assertTrue(ex.getMessage().contains("Retry"), "Exception message should contain the word 'Retry' as defined in the implementation");
    }

    /**
     * Edge case: zero‑length destination array – the method should return
     * immediately without touching the stream and without throwing.
     */
    @Test
    void shouldHandleZeroLengthDestinationArray() throws IOException {
        // given
        byte[] source = "any".getBytes();
        InputStream in = new ByteArrayInputStream(source);
        // zero length
        byte[] destination = new byte[0];
        // when
        IOUtilities.transfer(in, destination);
        // then
        // No exception, and the stream should remain untouched (still at position 0)
        assertEquals(source[0], in.read(), "When destination length is zero the InputStream should not have been read");
    }

    /**
     * Successful transfer where the InputStream provides exactly the number of bytes
     * required to fill the destination array.
     */
    @Test
    void shouldTransferExactSize() throws IOException {
        byte[] source = { 1, 2, 3, 4, 5 };
        InputStream in = new ByteArrayInputStream(source);
        byte[] dest = new byte[5];
        IOUtilities.transfer(in, dest);
        assertArrayEquals(source, dest, "Destination should contain the exact bytes from the stream");
    }

    /**
     * Successful transfer where the InputStream contains more data than the destination
     * array length. Only the first N bytes should be read.
     */
    @Test
    void shouldTransferLargerStreamTruncatingExtraBytes() throws IOException {
        byte[] source = { 10, 20, 30, 40, 50, 60, 70 };
        InputStream in = new ByteArrayInputStream(source);
        byte[] dest = new byte[4];
        IOUtilities.transfer(in, dest);
        assertArrayEquals(new byte[] { 10, 20, 30, 40 }, dest, "Only the first 4 bytes should be transferred");
        // Ensure the stream still has remaining bytes
        assertEquals(50, in.read(), "The next unread byte should be the fifth element (50)");
    }

    /**
     * Transfer fails because the InputStream ends before the destination array is full.
     */
    @Test
    void shouldThrowOnInsufficientBytes() {
        InputStream in = new ByteArrayInputStream(new byte[] { 1, 2 });
        byte[] dest = new byte[5];
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, dest));
        assertTrue(ex.getMessage().contains("Retry"), "Exception message must contain the word 'Retry'");
    }

    /**
     * InputStream that throws an IOException on the first read call.
     */
    @Test
    void shouldPropagateIOExceptionFromStream() {
        InputStream bad = new InputStream() {

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("simulated I/O failure");
            }

            @Override
            public int read() throws IOException {
                throw new IOException("simulated I/O failure");
            }
        };
        byte[] dest = new byte[4];
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(bad, dest));
        assertEquals("simulated I/O failure", ex.getMessage(), "The original IOException should be propagated unchanged");
    }

    /**
     * InputStream that returns data in multiple reads (2 bytes then 3 bytes) to fill a 5‑byte array.
     */
    @Test
    void shouldHandlePartialReadsAcrossMultipleLoopIterations() throws IOException {
        InputStream multi = new InputStream() {

            private final byte[] data = { 5, 6, 7, 8, 9 };

            private int pos = 0;

            @Override
            public int read(byte[] b, int off, int len) {
                if (pos >= data.length) {
                    return -1;
                }
                // first call returns 2 bytes, second call returns the remaining 3
                int toCopy = (pos == 0) ? Math.min(2, len) : Math.min(data.length - pos, len);
                System.arraycopy(data, pos, b, off, toCopy);
                pos += toCopy;
                return toCopy;
            }

            @Override
            public int read() {
                // not used in this test
                return -1;
            }
        };
        byte[] dest = new byte[5];
        IOUtilities.transfer(multi, dest);
        assertArrayEquals(new byte[] { 5, 6, 7, 8, 9 }, dest, "All bytes must be read correctly across multiple iterations");
    }

    /**
     * InputStream that ends immediately (empty) while a non‑zero destination array is requested.
     */
    @Test
    void shouldThrowWhenStreamEmptyButArrayNonZero() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        byte[] dest = new byte[3];
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(empty, dest));
        assertTrue(ex.getMessage().contains("Retry"), "Exception should contain the word 'Retry' for incomplete transfer");
    }

    /**
     * InputStream that returns some bytes then throws an IOException.
     */
    @Test
    void shouldPropagateIOExceptionAfterPartialRead() {
        InputStream flaky = new InputStream() {

            private int call = 0;

            private final byte[] firstChunk = { 1, 2 };

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (call == 0) {
                    System.arraycopy(firstChunk, 0, b, off, firstChunk.length);
                    call++;
                    return firstChunk.length;
                } else {
                    throw new IOException("failure after partial read");
                }
            }

            @Override
            public int read() throws IOException {
                throw new IOException("failure after partial read");
            }
        };
        byte[] dest = new byte[5];
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(flaky, dest));
        assertEquals("failure after partial read", ex.getMessage(), "The original IOException should be propagated, not replaced by the Retry message");
    }

    /**
     * InputStream that throws a RuntimeException (IllegalStateException) on first read.
     */
    @Test
    void shouldPropagateRuntimeExceptionFromStream() {
        InputStream bad = new InputStream() {

            @Override
            public int read(byte[] b, int off, int len) {
                throw new IllegalStateException("illegal state");
            }

            @Override
            public int read() {
                throw new IllegalStateException("illegal state");
            }
        };
        byte[] dest = new byte[4];
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.transfer(bad, dest));
        assertEquals("illegal state", ex.getMessage());
    }

    /**
     * InputStream that returns 1 byte then throws IllegalArgumentException.
     */
    @Test
    void shouldPropagateRuntimeExceptionAfterPartialRead() {
        InputStream tricky = new InputStream() {

            private int calls = 0;

            @Override
            public int read(byte[] b, int off, int len) {
                if (calls == 0) {
                    b[off] = 42;
                    calls++;
                    return 1;
                } else {
                    throw new IllegalArgumentException("bad argument after first byte");
                }
            }

            @Override
            public int read() {
                throw new IllegalArgumentException("bad argument after first byte");
            }
        };
        byte[] dest = new byte[3];
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.transfer(tricky, dest));
        assertEquals("bad argument after first byte", ex.getMessage());
    }

    /**
     * InputStream that returns an unexpected negative value (-2) on the first read.
     */
    @Test
    void shouldTreatUnexpectedNegativeReturnAsFailure() {
        InputStream weird = new InputStream() {

            @Override
            public int read(byte[] b, int off, int len) {
                // illegal negative value
                return -2;
            }

            @Override
            public int read() {
                return -2;
            }
        };
        byte[] dest = new byte[4];
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(weird, dest));
        assertTrue(ex.getMessage().contains("Retry"), "Unexpected negative return should lead to the same Retry IOException");
    }

    /**
     * InputStream that returns 2 bytes then an unexpected negative value (-5).
     */
    @Test
    void shouldFailWhenNegativeReturnAfterPartialRead() {
        InputStream mixed = new InputStream() {

            private int state = 0;

            private final byte[] data = { 9, 9 };

            @Override
            public int read(byte[] b, int off, int len) {
                if (state == 0) {
                    System.arraycopy(data, 0, b, off, data.length);
                    state = 1;
                    return data.length;
                } else {
                    // illegal negative value
                    return -5;
                }
            }

            @Override
            public int read() {
                return -5;
            }
        };
        byte[] dest = new byte[5];
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(mixed, dest));
        assertTrue(ex.getMessage().contains("Retry"), "Method should throw its own IOException when encountering unexpected negative return");
    }

    /**
     * Parameterised test for successful transfers where the stream length is
     * greater than or equal to the destination size.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSuccessfulTransferScenarios")
    void parameterisedSuccessfulTransfer(String description, byte[] streamData, int destSize, byte[] expected) throws IOException {
        InputStream in = new ByteArrayInputStream(streamData);
        byte[] dest = new byte[destSize];
        IOUtilities.transfer(in, dest);
        assertArrayEquals(expected, dest);
    }

    /**
     * Parameterised test for failure when the stream ends before the buffer is filled.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideInsufficientDataScenarios")
    void parameterisedInsufficientData(String description, byte[] streamData, int destSize) {
        InputStream in = new ByteArrayInputStream(streamData);
        byte[] dest = new byte[destSize];
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(in, dest));
        assertTrue(ex.getMessage().contains("Retry"));
    }
}
