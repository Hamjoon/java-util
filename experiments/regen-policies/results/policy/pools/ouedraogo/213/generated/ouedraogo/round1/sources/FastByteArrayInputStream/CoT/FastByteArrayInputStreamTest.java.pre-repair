package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * JUnit 4 test cases for {@link FastByteArrayInputStream}.
 */
public class FastByteArrayInputStreamTest {

    // ------------------------------------------------------------------------
    // 1. List of public methods (including constructor) extracted from the class
    // ------------------------------------------------------------------------
    // public FastByteArrayInputStream(byte[] buf)
    // public int read()
    // public int read(byte[] b, int off, int len)
    // public long skip(long n)
    // public int available()
    // public void mark(int readLimit)
    // public void reset()
    // public boolean markSupported()
    // public void close() throws IOException

    private byte[] sampleData;

    @Before
    public void setUp() {
        // Sample data used by many tests
        sampleData = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    }

    // ------------------------------------------------------------------------
    // 2. Basic functionality tests for each public method
    // ------------------------------------------------------------------------

    @Test
    public void testConstructorAndReadSingleByte() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        // read first byte (0)
        int first = in.read();
        assertEquals(0, first);
        // read second byte (1)
        assertEquals(1, in.read());
    }

    @Test
    public void testReadReturnsMinusOneAtEndOfStream() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{10});
        assertEquals(10, in.read());      // read the only byte
        assertEquals(-1, in.read());      // EOF
        assertEquals(-1, in.read());      // subsequent reads still -1
        in.close();                       // should not throw
    }

    @Test
    public void testReadByteArrayFull() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] dest = new byte[10];
        int bytesRead = in.read(dest, 0, dest.length);
        assertEquals(10, bytesRead);
        assertArrayEquals(sampleData, dest);
    }

    @Test
    public void testReadByteArrayPartial() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] dest = new byte[5];
        int bytesRead = in.read(dest, 0, dest.length);
        assertEquals(5, bytesRead);
        byte[] expected = new byte[]{0, 1, 2, 3, 4};
        assertArrayEquals(expected, dest);
        // second read should continue where first left off
        bytesRead = in.read(dest, 0, dest.length);
        assertEquals(5, bytesRead);
        byte[] expected2 = new byte[]{5, 6, 7, 8, 9};
        assertArrayEquals(expected2, dest);
    }

    @Test
    public void testReadWithOffset() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] dest = new byte[6];
        // leave first two positions untouched
        int bytesRead = in.read(dest, 2, 4);
        assertEquals(4, bytesRead);
        // dest[2..5] should contain first four bytes of sampleData
        assertEquals(0, dest[2]);
        assertEquals(1, dest[3]);
        assertEquals(2, dest[4]);
        assertEquals(3, dest[5]);
    }

    @Test
    public void testAvailable() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        assertEquals(10, in.available());
        in.read(); // consume one byte
        assertEquals(9, in.available());
    }

    @Test
    public void testMarkAndReset() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        in.read(); // read first byte (0)
        in.mark(0); // mark after first byte
        in.read(); // read second byte (1)
        in.read(); // read third byte (2)
        assertEquals(3, in.read()); // next byte is 3
        in.reset(); // should go back to position after first byte (i.e., before byte 1)
        assertEquals(1, in.read()); // should read 1 again
    }

    @Test
    public void testMarkSupported() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        assertTrue(in.markSupported());
    }

    @Test
    public void testSkipWithinBounds() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        long skipped = in.skip(4);
        assertEquals(4, skipped);
        assertEquals(4, in.read()); // after skipping 4 bytes, next byte is 4
    }

    @Test
    public void testSkipBeyondEnd() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        long skipped = in.skip(100);
        assertEquals(10, skipped); // only 10 bytes available
        assertEquals(-1, in.read()); // EOF
    }

    @Test
    public void testSkipNegative() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        long skipped = in.skip(-5);
        assertEquals(0, skipped);
        assertEquals(0, in.read()); // position unchanged
    }

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        in.close(); // should be a no‑op and not throw
    }

    // ------------------------------------------------------------------------
    // 3. Edge‑case and exception handling tests
    // ------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testReadByteArray_NullBufferThrowsNPE() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        in.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArray_NegativeOffsetThrowsIOOBE() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] dest = new byte[5];
        in.read(dest, -1, 3);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArray_NegativeLengthThrowsIOOBE() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] dest = new byte[5];
        in.read(dest, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArray_LengthExceedsBufferThrowsIOOBE() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] dest = new byte[5];
        // off=2, len=4 would require 6 bytes in dest, which exceeds length
        in.read(dest, 2, 4);
    }

    @Test
    public void testReadByteArray_ZeroLengthReturnsZero() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] dest = new byte[5];
        int result = in.read(dest, 0, 0);
        assertEquals(0, result);
        // Ensure position has not moved
        assertEquals(0, in.read());
    }

    @Test
    public void testReadByteArray_EofReturnsMinusOne() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[0]); // empty buffer
        byte[] dest = new byte[10];
        int result = in.read(dest, 0, 5);
        assertEquals(-1, result);
    }

    @Test
    public void testSkipZeroReturnsZeroAndDoesNotChangePosition() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        long skipped = in.skip(0);
        assertEquals(0, skipped);
        assertEquals(0, in.read()); // still at start
    }

    @Test
    public void testMarkResetWithoutPriorMarkResetsToStart() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        in.read(); // consume one byte
        in.reset(); // mark defaults to 0 (constructor sets mark = 0)
        assertEquals(0, in.read()); // should read first byte again
    }

    @Test
    public void testAvailableAfterSkipAndRead() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        assertEquals(10, in.available());
        in.skip(3);
        assertEquals(7, in.available());
        in.read();
        assertEquals(6, in.available());
    }

    @Test
    public void testReadAfterResetAfterSkip() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        in.skip(5);            // position = 5
        in.mark(0);            // mark = 5
        in.read();            // reads byte 5 (value 5)
        in.reset();           // back to 5
        assertEquals(5, in.read()); // should read 5 again
    }
}
