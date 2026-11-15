package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_inputStreamToBytes_Test {

    private static Stream<Arguments> boundaryStreamProvider() {
        return Stream.of(// exactly the initial buffer size
        Arguments.of(16_384), // one byte more – forces a resize
        Arguments.of(16_385));
    }

    /**
     * ----------------------------------------------------------------------
     *  1️⃣  Normal operation – a simple InputStream should be fully read and
     *      returned as a byte array.
     *  ---------------------------------------------------------------------- **
     */
    @Test
    void inputStreamToBytes_returnsAllBytes() {
        // given
        byte[] expected = "Hello IOUtilities!".getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(expected);
        // when
        byte[] actual = IOUtilities.inputStreamToBytes(in);
        // then
        assertArrayEquals(expected, actual, "The returned byte array must contain exactly the data read from the InputStream");
    }

    /**
     * ----------------------------------------------------------------------
     *  2️⃣  Empty stream – the method should return an empty (zero‑length) array,
     *      not null.
     *  ---------------------------------------------------------------------- **
     */
    @Test
    void inputStreamToBytes_emptyStream_returnsEmptyArray() {
        // given
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        // when
        byte[] result = IOUtilities.inputStreamToBytes(empty);
        // then
        assertNotNull(result, "Result must not be null for an empty stream");
        assertEquals(0, result.length, "Result length should be zero for an empty stream");
    }

    /**
     * ----------------------------------------------------------------------
     *  3️⃣  Null argument – the implementation catches any Exception, so a
     *      NullPointerException from the internal transfer should be swallowed
     *      and the method should return null.
     *  ---------------------------------------------------------------------- **
     */
    @Test
    void inputStreamToBytes_nullInput_returnsNull() {
        // given
        InputStream nullStream = null;
        // when
        byte[] result = IOUtilities.inputStreamToBytes(nullStream);
        // then
        assertNull(result, "When a null InputStream is supplied the method should return null");
    }

    /**
     * ----------------------------------------------------------------------
     *  4️⃣  Exceptional situation – simulate an IOException while reading.
     *      The method catches the exception and returns null.
     *  ---------------------------------------------------------------------- **
     */
    @Test
    void inputStreamToBytes_whenExceptionIsThrown_returnsNull(@Mock InputStream failingStream) throws IOException {
        // given
        // The first call to read() throws; any further calls are irrelevant because the
        // exception aborts the transfer.
        when(failingStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("simulated read failure"));
        // when
        byte[] result = IOUtilities.inputStreamToBytes(failingStream);
        // then
        assertNull(result, "If an exception occurs during transfer the method must return null");
        // verify that read was indeed attempted
        verify(failingStream, atLeastOnce()).read(any(byte[].class), anyInt(), anyInt());
    }

    @Test
    void inputStreamToBytes_largeStream_exceedsInitialBuffer() {
        // given – a byte array larger than the FastByteArrayOutputStream default (16384)
        byte[] large = new byte[200_000];
        new Random(0).nextBytes(large);
        InputStream in = new ByteArrayInputStream(large);
        // when
        byte[] actual = IOUtilities.inputStreamToBytes(in);
        // then
        assertArrayEquals(large, actual, "The method must correctly grow the internal buffer and return the full content");
    }

    @ParameterizedTest(name = "Boundary size {0} bytes")
    @MethodSource("boundaryStreamProvider")
    void inputStreamToBytes_boundaryConditions(int size) {
        // given
        // all zeros by default
        byte[] data = new byte[size];
        InputStream in = new ByteArrayInputStream(data);
        // when
        byte[] result = IOUtilities.inputStreamToBytes(in);
        // then
        assertNotNull(result);
        assertEquals(size, result.length);
        assertArrayEquals(data, result);
    }

    @Test
    void inputStreamToBytes_bufferedInputStream_isReadCorrectly() {
        // given
        byte[] payload = "buffered".getBytes(StandardCharsets.UTF_8);
        InputStream wrapped = new BufferedInputStream(new ByteArrayInputStream(payload));
        // when
        byte[] result = IOUtilities.inputStreamToBytes(wrapped);
        // then
        assertArrayEquals(payload, result);
    }

    @Test
    void inputStreamToBytes_whenIOExceptionThrown_returnsNull(@Mock InputStream failingStream) throws IOException {
        // given – the first read throws IOException
        when(failingStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("simulated read failure"));
        // when
        byte[] result = IOUtilities.inputStreamToBytes(failingStream);
        // then
        assertNull(result, "If an IOException occurs during transfer the method must return null");
        verify(failingStream, atLeastOnce()).read(any(byte[].class), anyInt(), anyInt());
    }

    @Test
    void inputStreamToBytes_whenIOExceptionAfterPartialRead_returnsNull(@Mock InputStream partialFailing) throws IOException {
        // given – return 5 bytes then throw
        when(partialFailing.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            byte[] buf = invocation.getArgument(0);
            int off = invocation.getArgument(1);
            int len = invocation.getArgument(2);
            // write 5 bytes 'a'
            int toWrite = Math.min(5, len);
            for (int i = 0; i < toWrite; i++) {
                buf[off + i] = 'a';
            }
            return toWrite;
        }).thenThrow(new IOException("forced after some bytes"));
        // when
        byte[] result = IOUtilities.inputStreamToBytes(partialFailing);
        // then
        assertNull(result, "Even after partial success the method must return null on IOException");
    }

    @Test
    void inputStreamToBytes_uncheckedIllegalArgumentException_isPropagated() {
        // given
        InputStream bad = new InputStream() {

            @Override
            public int read() {
                throw new IllegalArgumentException("forced unchecked");
            }
        };
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.inputStreamToBytes(bad), "Unchecked IllegalArgumentException should propagate");
        assertEquals("forced unchecked", ex.getMessage());
    }

    @Test
    void inputStreamToBytes_uncheckedIllegalStateException_isPropagated() {
        // given – returns three bytes then throws IllegalStateException
        InputStream flaky = new InputStream() {

            private int count = 0;

            @Override
            public int read() {
                if (count < 3) {
                    count++;
                    return 'x';
                }
                throw new IllegalStateException("forced after some bytes");
            }
        };
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.inputStreamToBytes(flaky), "Unchecked IllegalStateException should propagate");
        assertEquals("forced after some bytes", ex.getMessage());
    }
}
