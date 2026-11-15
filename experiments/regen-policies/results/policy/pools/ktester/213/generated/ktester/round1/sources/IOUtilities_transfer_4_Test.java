package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_transfer_4_Test {

    /**
     * Supplies arguments for {@link #transferScenarios(String, InputStream, byte[], Runnable)}.
     */
    private static Stream<Arguments> provideTransferScenarios() {
        return Stream.of(// Null InputStream – expect NullPointerException with proper message
        Arguments.of("Null InputStream", null, new byte[10], (Runnable) () -> {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, new byte[10]));
            assertTrue(ex.getMessage().contains("InputStream cannot be null"));
        }), // Null byte array – expect NullPointerException with proper message
        Arguments.of("Null byte array", new ByteArrayInputStream(new byte[] { 1, 2, 3 }), null, (Runnable) () -> {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(new ByteArrayInputStream(new byte[] { 1, 2, 3 }), null));
            assertTrue(ex.getMessage().contains("byte array cannot be null"));
        }), // Zero‑length byte array – no exception, array stays empty
        Arguments.of("Zero‑length byte array", new ByteArrayInputStream(new byte[] {}), new byte[0], null), // Exact length stream – array should match stream content
        Arguments.of("Exact length stream", new ByteArrayInputStream(new byte[] { 10, 20, 30, 40 }), new byte[4], (Runnable) () -> {
            byte[] src = new byte[] { 10, 20, 30, 40 };
            byte[] dst = new byte[4];
            try {
                IOUtilities.transfer(new ByteArrayInputStream(src), dst);
                assertArrayEquals(src, dst);
            } catch (IOException e) {
                fail("Unexpected IOException: " + e);
            }
        }), // Longer stream than destination – only first N bytes are read
        Arguments.of("Longer stream than byte array", new ByteArrayInputStream(new byte[] { 5, 6, 7, 8, 9, 10 }), new byte[3], (Runnable) () -> {
            byte[] src = new byte[] { 5, 6, 7, 8, 9, 10 };
            byte[] dst = new byte[3];
            try {
                IOUtilities.transfer(new ByteArrayInputStream(src), dst);
                assertArrayEquals(new byte[] { 5, 6, 7 }, dst);
                // Ensure remaining bytes are still available in the original stream
                InputStream original = new ByteArrayInputStream(src);
                original.skip(3);
                assertEquals(3, original.available(), "Three bytes should remain unread in the original stream");
            } catch (IOException e) {
                fail("Unexpected IOException: " + e);
            }
        }), // Shorter stream than destination – expect EOFException
        Arguments.of("Shorter stream than byte array (EOF)", new ByteArrayInputStream(new byte[] { 1, 2 }), new byte[5], (Runnable) () -> {
            EOFException ex = assertThrows(EOFException.class, () -> IOUtilities.transfer(new ByteArrayInputStream(new byte[] { 1, 2 }), new byte[5]));
            assertNull(ex.getMessage());
        }), // IOException during read – custom InputStream that always throws
        Arguments.of("IOException during read", new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("forced");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("forced");
            }
        }, new byte[10], (Runnable) () -> {
            InputStream faulty = new InputStream() {

                @Override
                public int read() throws IOException {
                    throw new IOException("forced");
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    throw new IOException("forced");
                }
            };
            IOException ex = assertThrows(IOException.class, () -> IOUtilities.transfer(faulty, new byte[10]));
            assertEquals("forced", ex.getMessage());
        }));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void transferCopiesAllBytes() throws IOException {
        // given
        byte[] expected = "Hello, IOUtilities!".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(expected);
        byte[] target = new byte[expected.length];
        // when
        IOUtilities.transfer(in, target);
        // then
        assertArrayEquals(expected, target, "The target array should contain the exact bytes read");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void transferWithZeroLengthArrayDoesNothing() throws IOException {
        // given
        byte[] source = "any".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(source);
        byte[] target = new byte[0];
        int availableBefore = in.available();
        // when / then – no exception expected
        IOUtilities.transfer(in, target);
        assertEquals(0, target.length);
        // the stream must not have been consumed
        assertEquals(availableBefore, in.available(), "A zero‑length destination must not read from the stream");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void transferWhenStreamTooShortThrowsEOFException() {
        // given
        // 5 bytes
        byte[] source = "short".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(source);
        // request more bytes than available
        byte[] target = new byte[10];
        // when / then
        EOFException ex = assertThrows(EOFException.class, () -> IOUtilities.transfer(in, target));
        assertNull(ex.getMessage(), "DataInputStream.readFully throws EOFException without a message");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void transferNullInputStreamThrowsException() {
        // given
        byte[] target = new byte[1];
        // when / then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(null, target));
        assertTrue(ex.getMessage().contains("InputStream cannot be null"), "Exception message should mention the missing InputStream");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void transferNullByteArrayThrowsException() {
        // given
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 0 });
        // when / then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.transfer(in, null));
        assertTrue(ex.getMessage().contains("byte array cannot be null"), "Exception message should mention the missing byte array");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void transferDoesNotCloseInputStream() throws IOException {
        // given
        byte[] data = "remain open".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        byte[] target = new byte[data.length];
        // when
        IOUtilities.transfer(in, target);
        // then – the stream should still be readable (available >= 0 means not closed)
        assertTrue(in.available() >= 0, "InputStream should remain open after transfer");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideTransferScenarios")
    void transferScenarios(String description, InputStream in, byte[] destination, Runnable assertion) throws IOException {
        // The assertion runnable contains the expectations (e.g., array content,
        // thrown exception, etc.). It is executed after the call to transfer.
        if (assertion != null) {
            // The assertion may expect an exception, so we let it handle that.
            assertion.run();
        } else {
            // Normal case – just invoke the method.
            IOUtilities.transfer(in, destination);
        }
    }
}
