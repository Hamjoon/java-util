package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

public class FastByteArrayInputStreamTest {

    private byte[] data;
    private FastByteArrayInputStream stream;

    @Before
    public void setUp() {
        data = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        stream = new FastByteArrayInputStream(data);
    }

    /* ----------------------------------------------------------------------
     *  Tests for read()
     * ---------------------------------------------------------------------- */
    @Test
    public void testReadSingleByteSequential() throws IOException {
        for (int i = 0; i < data.length; i++) {
            int expected = data[i] & 0xff;
            int actual = stream.read();
            assertEquals("Byte at position " + i + " should match", expected, actual);
        }
        // After all bytes are read, should return -1
        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadSingleByteAtEndReturnsMinusOne() throws IOException {
        // Exhaust the stream
        while (stream.read() != -1) { /* consume */ }
        assertEquals(-1, stream.read());
    }

    /* ----------------------------------------------------------------------
     *  Tests for read(byte[], int, int)
     * ---------------------------------------------------------------------- */
    @Test
    public void testReadIntoArrayFull() throws IOException {
        byte[] target = new byte[data.length];
        int read = stream.read(target, 0, target.length);
        assertEquals(data.length, read);
        assertArrayEquals(data, target);
    }

    @Test
    public void testReadIntoArrayPartial() throws IOException {
        byte[] target = new byte[5];
        int read = stream.read(target, 0, target.length);
        assertEquals(5, read);
        byte[] expected = new byte[] {0,1,2,3,4};
        assertArrayEquals(expected, target);
    }

    @Test
    public void testReadWithOffset() throws IOException {
        byte[] target = new byte[10];
        int read = stream.read(target, 2, 4); // should read bytes 0..3 into positions 2..5
        assertEquals(4, read);
        assertEquals(0, target[2]);
        assertEquals(1, target[3]);
        assertEquals(2, target[4]);
        assertEquals(3, target[5]);
        // other positions remain default 0
    }

    @Test
    public void testReadZeroLengthReturnsZero() throws IOException {
        byte[] target = new byte[10];
        int read = stream.read(target, 0, 0);
        assertEquals(0, read);
    }

    @Test(expected = NullPointerException.class)
    public void testReadNullArrayThrowsNPE() {
        stream.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadNegativeOffsetThrowsIOOBE() {
        stream.read(new byte[5], -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadNegativeLengthThrowsIOOBE() {
        stream.read(new byte[5], 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadLengthBeyondArrayThrowsIOOBE() {
        stream.read(new byte[5], 3, 3); // 3+3 > 5
    }

    @Test
    public void testReadAfterEndReturnsMinusOne() throws IOException {
        // Exhaust the stream
        while (stream.read() != -1) { /* consume */ }
        byte[] target = new byte[5];
        int read = stream.read(target, 0, target.length);
        assertEquals(-1, read);
    }

    /* ----------------------------------------------------------------------
     *  Tests for skip(long)
     * ---------------------------------------------------------------------- */
    @Test
    public void testSkipWithinBounds() throws IOException {
        long skipped = stream.skip(4);
        assertEquals(4, skipped);
        assertEquals(4, stream.read()); // next byte should be 4
    }

    @Test
    public void testSkipBeyondEnd() throws IOException {
        long skipped = stream.skip(100);
        assertEquals(data.length, skipped);
        assertEquals(-1, stream.read());
    }

    @Test
    public void testSkipNegativeReturnsZero() throws IOException {
        long skipped = stream.skip(-5);
        assertEquals(0, skipped);
        assertEquals(0, stream.read()); // position unchanged
    }

    /* ----------------------------------------------------------------------
     *  Tests for available()
     * ---------------------------------------------------------------------- */
    @Test
    public void testAvailableInitially() {
        assertEquals(data.length, stream.available());
    }

    @Test
    public void testAvailableAfterPartialRead() throws IOException {
        stream.read();
        stream.read();
        assertEquals(data.length - 2, stream.available());
    }

    @Test
    public void testAvailableAfterSkip() throws IOException {
        stream.skip(3);
        assertEquals(data.length - 3, stream.available());
    }

    @Test
    public void testAvailableAfterExhaustion() throws IOException {
        while (stream.read() != -1) { /* consume */ }
        assertEquals(0, stream.available());
    }

    /* ----------------------------------------------------------------------
     *  Tests for mark/reset/markSupported
     * ---------------------------------------------------------------------- */
    @Test
    public void testMarkSupportedAlwaysTrue() {
        assertTrue(stream.markSupported());
    }

    @Test
    public void testMarkAndResetFunctionality() throws IOException {
        // read first two bytes
        assertEquals(0, stream.read());
        assertEquals(1, stream.read());

        // mark at position 2
        stream.mark(0);
        assertEquals(2, stream.read()); // read third byte

        // reset back to mark (position 2)
        stream.reset();
        assertEquals(2, stream.read()); // should read the same third byte again
    }

    @Test
    public void testResetWithoutMarkReturnsToStart() throws IOException {
        // default mark is 0 (set in constructor)
        stream.read(); // advance one byte
        stream.reset(); // should go back to start
        assertEquals(0, stream.read());
    }

    /* ----------------------------------------------------------------------
     *  Tests for close()
     * ---------------------------------------------------------------------- */
    @Test
    public void testCloseDoesNotAffectSubsequentReads() throws IOException {
        stream.close(); // should be a no‑op
        assertEquals(0, stream.read());
        stream.close();
        assertEquals(1, stream.read());
    }

    /* ----------------------------------------------------------------------
     *  Additional edge‑case tests combining operations
     * ---------------------------------------------------------------------- */
    @Test
    public void testReadAfterSkipAndMarkResetSequence() throws IOException {
        // skip 2 bytes, mark, read 2 bytes, reset, read again
        stream.skip(2);
        stream.mark(0);
        assertEquals(2, stream.read());
        assertEquals(3, stream.read());
        stream.reset();
        assertEquals(2, stream.read());
        assertEquals(3, stream.read());
    }

    @Test
    public void testMultipleSequentialSkipsDoNotExceedBounds() throws IOException {
        long first = stream.skip(3);
        long second = stream.skip(4);
        assertEquals(3, first);
        assertEquals(4, second);
        assertEquals(7, stream.read()); // should be byte value 7
    }

    @Test
    public void testReadArrayAfterPartialSkip() throws IOException {
        stream.skip(5);
        byte[] buf = new byte[3];
        int read = stream.read(buf, 0, buf.length);
        assertEquals(3, read);
        assertArrayEquals(new byte[]{5,6,7}, buf);
    }
}
