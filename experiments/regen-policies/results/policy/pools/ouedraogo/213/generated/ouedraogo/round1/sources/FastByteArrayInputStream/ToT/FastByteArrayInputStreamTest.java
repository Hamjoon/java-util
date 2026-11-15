package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link FastByteArrayInputStream}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal reads (single byte and bulk)</li>
 *   <li>boundary conditions (empty buffer, end‑of‑stream)</li>
 *   <li>error handling for illegal arguments</li>
 *   <li>skip, available, mark/reset behavior</li>
 *   <li>the {@code close()} contract (no exception)</li>
 * </ul>
 */
public class FastByteArrayInputStreamTest {

    private static final byte[] SAMPLE = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
    private FastByteArrayInputStream stream;

    @Before
    public void setUp() {
        // fresh stream for each test
        stream = new FastByteArrayInputStream(SAMPLE.clone());
    }

    /* -------------------------------------------------------------
     * read() – single‑byte reads
     * ----------------------------------------------------------- */
    @Test
    public void testReadSingleByte_Sequential() throws IOException {
        for (int i = 0; i < SAMPLE.length; i++) {
            int expected = SAMPLE[i] & 0xFF;
            int actual = stream.read();
            assertEquals("Byte at position " + i + " differs", expected, actual);
        }
        // after the last byte the stream must return -1
        assertEquals(-1, stream.read());
        // subsequent reads continue to return -1
        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadSingleByte_EmptyBuffer() throws IOException {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        assertEquals(-1, empty.read());
    }

    /* -------------------------------------------------------------
     * read(byte[], int, int) – bulk reads
     * ----------------------------------------------------------- */
    @Test
    public void testReadByteArray_FullRead() throws IOException {
        byte[] dst = new byte[10];
        int len = stream.read(dst, 2, 5); // read all 5 bytes into dst[2..6]
        assertEquals(5, len);
        // verify that the bytes are copied correctly
        for (int i = 0; i < SAMPLE.length; i++) {
            assertEquals("Byte mismatch at dst[" + (i + 2) + "]",
                    SAMPLE[i], dst[i + 2]);
        }
        // positions before and after the copied region must stay untouched (default 0)
        assertEquals(0, dst[0]);
        assertEquals(0, dst[1]);
        assertEquals(0, dst[7]);
        assertEquals(0, dst[8]);
        assertEquals(0, dst[9]);
        // stream now at EOF
        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadByteArray_PartialRead_WhenLenGreaterThanAvailable() throws IOException {
        // read only first 3 bytes, then request more than remaining
        byte[] dst = new byte[10];
        int first = stream.read(dst, 0, 3);
        assertEquals(3, first);
        int second = stream.read(dst, 3, 10); // only 2 bytes left
        assertEquals(2, second);
        // verify content
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0, 0, 0, 0, 0}, dst);
        // EOF now
        assertEquals(-1, stream.read(dst, 0, 1));
    }

    @Test
    public void testReadByteArray_ZeroLength() throws IOException {
        byte[] dst = new byte[5];
        int result = stream.read(dst, 0, 0);
        assertEquals(0, result);
        // stream position must not move
        assertEquals(SAMPLE.length, stream.available());
    }

    @Test(expected = NullPointerException.class)
    public void testReadByteArray_NullArray() throws IOException {
        stream.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArray_NegativeOffset() throws IOException {
        stream.read(new byte[5], -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArray_NegativeLength() throws IOException {
        stream.read(new byte[5], 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArray_OffsetLengthBeyondArray() throws IOException {
        stream.read(new byte[5], 3, 3); // 3+3 > 5
    }

    /* -------------------------------------------------------------
     * skip(long) – skipping bytes
     * ----------------------------------------------------------- */
    @Test
    public void testSkip_WithinBounds() throws IOException {
        long skipped = stream.skip(2);
        assertEquals(2L, skipped);
        assertEquals(3, stream.available());
        // next read should return the third original byte (value 0x03)
        assertEquals(0x03, stream.read());
    }

    @Test
    public void testSkip_BeyondEnd() throws IOException {
        long skipped = stream.skip(100);
        assertEquals(SAMPLE.length, skipped);
        assertEquals(-1, stream.read());
        assertEquals(0, stream.available());
    }

    @Test
    public void testSkip_Negative() throws IOException {
        long skipped = stream.skip(-5);
        assertEquals(0L, skipped);
        // stream position unchanged
        assertEquals(SAMPLE.length, stream.available());
    }

    /* -------------------------------------------------------------
     * available()
     * ----------------------------------------------------------- */
    @Test
    public void testAvailable_ChangesWithReadAndSkip() throws IOException {
        assertEquals(SAMPLE.length, stream.available());
        stream.read(); // consume one byte
        assertEquals(SAMPLE.length - 1, stream.available());
        stream.skip(2);
        assertEquals(SAMPLE.length - 3, stream.available());
    }

    @Test
    public void testAvailable_EmptyBuffer() throws IOException {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        assertEquals(0, empty.available());
    }

    /* -------------------------------------------------------------
     * mark / reset
     * ----------------------------------------------------------- */
    @Test
    public void testMarkAndReset_RestorePosition() throws IOException {
        // read two bytes, mark, read two more, reset, read again
        assertEquals(0x01, stream.read());
        assertEquals(0x02, stream.read());
        stream.mark(0); // readLimit ignored
        assertEquals(0x03, stream.read());
        assertEquals(0x04, stream.read());
        stream.reset(); // back to after second byte
        assertEquals(0x03, stream.read()); // should read 0x03 again
        assertEquals(0x04, stream.read());
        assertEquals(0x05, stream.read());
        assertEquals(-1, stream.read());
    }

    @Test
    public void testMarkSupported() {
        assertTrue(stream.markSupported());
    }

    /* -------------------------------------------------------------
     * close()
     * ----------------------------------------------------------- */
    @Test
    public void testClose_NoException() throws IOException {
        // close should be a no‑op but must not throw
        stream.close();
        // after close the stream should still behave as before (no state change)
        assertEquals(0x01, stream.read());
    }

    /* -------------------------------------------------------------
     * Constructor edge cases
     * ----------------------------------------------------------- */
    @Test
    public void testConstructor_NullArray_Allowed() {
        // The class does not guard against null; a NPE will be thrown on first use.
        FastByteArrayInputStream s = new FastByteArrayInputStream(null);
        try {
            s.read();
            fail("Expected NullPointerException due to null buffer");
        } catch (NullPointerException expected) {
            // expected path
        }
    }

    @Test
    public void testConstructor_ZeroLengthArray() throws IOException {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        assertEquals(-1, empty.read());
        assertEquals(0, empty.available());
        assertTrue(empty.markSupported());
    }
}
