package com.cedarsoftware.util;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_read_Test {

    /**
     * Shared buffer used by several tests.
     */
    private byte[] sharedBuffer;

    /**
     * Instance under test.
     */
    private FastByteArrayInputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh buffer and stream before each test.
        sharedBuffer = new byte[] { 1, 2, 3, 4, 5 };
        stream = new FastByteArrayInputStream(sharedBuffer);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly close the stream – the implementation is a no‑op but it mirrors normal usage.
        try {
            stream.close();
        } catch (IOException ignored) {
            // ignore – close does not throw for this class
        }
        stream = null;
        sharedBuffer = null;
    }

    /**
     * Provides a variety of buffers together with the expected sequence of
     * {@code read()} results. The expected array may contain {@code -1}
     * to denote the end‑of‑stream marker.
     */
    private static Stream<Arguments> readNormalCases() {
        return Stream.of(// single byte 'a' (0x61)
        Arguments.of(new byte[] { 0x61 }, new int[] { 97 }), // empty buffer
        Arguments.of(new byte[] {}, new int[] { -1 }), // single byte 0xFF (signed -1)
        Arguments.of(new byte[] { (byte) 0xFF }, new int[] { 255 }), // three‑byte buffer {10,20,30}
        Arguments.of(new byte[] { 10, 20, 30 }, new int[] { 10, 20, 30, -1 }), // buffer {0, 0x7F, 0x80, 0xFF, 100}
        Arguments.of(new byte[] { 0, 0x7F, (byte) 0x80, (byte) 0xFF, 100 }, new int[] { 0, 127, 128, 255, 100, -1 }), // buffer {5,6,7}
        Arguments.of(new byte[] { 5, 6, 7 }, new int[] { 5, 6, 7, -1 }), // single byte 0xFE (-2)
        Arguments.of(new byte[] { (byte) 0xFE }, new int[] { 254, -1 }), // single zero byte
        Arguments.of(new byte[] { 0 }, new int[] { 0, -1 }), // empty buffer, five reads
        Arguments.of(new byte[] {}, new int[] { -1, -1, -1, -1, -1 }), // three‑byte buffer with extra reads
        Arguments.of(new byte[] { 10, 20, 30 }, new int[] { 10, 20, 30, -1, -1, -1 }));
    }

    /**
     * Verify that successive calls to {@code read()} return the correct unsigned values
     *  and that {@code -1} is returned after the end of the stream is reached.
     */
    @Test
    void testReadSequentialBytes() {
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());
        assertEquals(3, stream.read());
        assertEquals(4, stream.read());
        assertEquals(5, stream.read());
        // end of stream
        assertEquals(-1, stream.read());
    }

    /**
     * A stream created from an empty array must return {@code -1} immediately.
     */
    @Test
    void testReadFromEmptyStream() {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        assertEquals(-1, empty.read());
    }

    /**
     * Ensure that the method returns unsigned values (0‑255) even when the underlying
     *  byte values are negative in Java's signed byte representation.
     */
    @Test
    void testReadUnsignedByteValues() {
        byte[] data = { (byte) 0xFF, (byte) 0x80, (byte) 0x00, (byte) 0x7F };
        FastByteArrayInputStream unsignedStream = new FastByteArrayInputStream(data);
        // 0xFF & 0xFF = 255
        assertEquals(255, unsignedStream.read());
        // 0x80 & 0xFF = 128
        assertEquals(128, unsignedStream.read());
        // 0x00
        assertEquals(0, unsignedStream.read());
        // 0x7F
        assertEquals(127, unsignedStream.read());
        // end of stream
        assertEquals(-1, unsignedStream.read());
    }

    /**
     * After invoking {@code close()}, the stream should still be readable because the
     *  implementation does not release the underlying buffer.
     */
    @Test
    void testReadAfterClose() throws IOException {
        // close is a no‑op for this class
        stream.close();
        // first byte still readable
        assertEquals(1, stream.read());
        // subsequent reads continue as normal
        assertEquals(2, stream.read());
    }

    /**
     * Skipping bytes should affect the next value returned by {@code read()}.
     */
    @Test
    void testReadAfterSkip() {
        long skipped = stream.skip(2);
        assertEquals(2L, skipped);
        // should read the third element
        assertEquals(3, stream.read());
    }

    /**
     * Using {@code mark()} and {@code reset()} must restore the position for subsequent reads.
     */
    @Test
    void testReadWithMarkAndReset() {
        assertTrue(stream.markSupported(), "mark should be supported");
        stream.mark(0);
        // advance
        assertEquals(1, stream.read());
        // advance
        assertEquals(2, stream.read());
        // back to start
        stream.reset();
        // should read the first element again
        assertEquals(1, stream.read());
    }

    /**
     * Verify that {@code read()} correctly reports the end of stream after a mixture of
     *  {@code read()}, {@code skip()}, and {@code mark()/reset()} operations.
     */
    @Test
    void testReadEndOfStreamAfterVariousOperations() {
        // pos = 1
        assertEquals(1, stream.read());
        // pos = 3
        stream.skip(2);
        // pos = 4
        assertEquals(4, stream.read());
        stream.mark(0);
        // pos = 5 (end)
        assertEquals(5, stream.read());
        // already at end
        assertEquals(-1, stream.read());
        // back to marked position (pos = 4)
        stream.reset();
        // read last byte again
        assertEquals(5, stream.read());
        // end again
        assertEquals(-1, stream.read());
    }

    @ParameterizedTest(name = "[{index}] buffer={0}")
    @MethodSource("readNormalCases")
    void testReadNormalScenarios(byte[] buffer, int[] expectedSequence) {
        FastByteArrayInputStream s = new FastByteArrayInputStream(buffer);
        for (int expected : expectedSequence) {
            assertEquals(expected, s.read(), () -> "Unexpected value while reading from buffer " + java.util.Arrays.toString(buffer));
        }
    }

    /**
     * Constructing the stream with a {@code null} buffer must cause {@code read()} to throw
     *  a {@link NullPointerException}.
     */
    @Test
    void testReadWithNullBufferThrowsNPE() {
        FastByteArrayInputStream nullStream = new FastByteArrayInputStream(null);
        assertThrows(NullPointerException.class, nullStream::read, "Reading from a stream backed by a null buffer should throw NPE");
    }

    /**
     * Manipulate the private final {@code count} field so that it exceeds the actual
     * buffer length. The subsequent {@code read()} call should attempt to access
     * beyond the array bounds and therefore throw {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    void testReadWithInflatedCountThrowsAIOOBE() throws Exception {
        // real length = 2
        byte[] data = { 1, 2 };
        FastByteArrayInputStream s = new FastByteArrayInputStream(data);
        // Use reflection to change the final field 'count' to 5
        Field countField = FastByteArrayInputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        // Remove the final modifier (required for Java 12+ as well)
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(countField, countField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        // now count > data.length
        countField.setInt(s, 5);
        // The first call to read() will try to read buffer[0] (ok) but the second call
        // will try to read buffer[1] (still ok). The third call will attempt buffer[2] -> OOB.
        // first byte ok
        assertEquals(1, s.read());
        // second byte ok
        assertEquals(2, s.read());
        assertThrows(ArrayIndexOutOfBoundsException.class, s::read, "Reading past the real array length should throw AIOOBE");
    }

    /**
     * Set the private {@code pos} field to a negative value via reflection.
     * The next {@code read()} call evaluates {@code pos < count} as true and
     * attempts to access {@code buffer[-1]}, which must throw {@link ArrayIndexOutOfBoundsException}.
     */
    @Test
    void testReadWithNegativePosThrowsAIOOBE() throws Exception {
        byte[] data = { 10, 20, 30 };
        FastByteArrayInputStream s = new FastByteArrayInputStream(data);
        // Set pos = -1
        Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
        posField.setAccessible(true);
        posField.setInt(s, -1);
        assertThrows(ArrayIndexOutOfBoundsException.class, s::read, "Reading with a negative position should throw AIOOBE");
    }
}
