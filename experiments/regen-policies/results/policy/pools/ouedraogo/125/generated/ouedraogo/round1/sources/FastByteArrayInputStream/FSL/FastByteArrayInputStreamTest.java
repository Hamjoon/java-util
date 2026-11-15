package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class FastByteArrayInputStreamTest {

    @Test
    public void testReadSingleByte() {
        byte[] data = new byte[]{0, 1, 127, (byte) -1};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        assertEquals(0, in.read());
        assertEquals(1, in.read());
        assertEquals(127, in.read());
        // -1 byte should be returned as unsigned 255
        assertEquals(255, in.read());

        // End of stream -> -1
        assertEquals(-1, in.read());
    }

    @Test
    public void testReadByteArrayFull() {
        byte[] data = new byte[]{10, 20, 30, 40, 50};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        byte[] dest = new byte[5];
        int read = in.read(dest, 0, dest.length);
        assertEquals(5, read);
        assertArrayEquals(data, dest);

        // subsequent read should return -1
        assertEquals(-1, in.read(dest, 0, dest.length));
    }

    @Test
    public void testReadByteArrayPartialAndOffset() {
        byte[] data = new byte[]{5, 6, 7, 8, 9};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        byte[] dest = new byte[10];
        // read 3 bytes into dest starting at offset 2
        int read = in.read(dest, 2, 3);
        assertEquals(3, read);
        assertEquals(5, dest[2] & 0xFF);
        assertEquals(6, dest[3] & 0xFF);
        assertEquals(7, dest[4] & 0xFF);
        // remaining bytes should be untouched (still 0)
        for (int i = 0; i < 2; i++) {
            assertEquals(0, dest[i]);
        }
        for (int i = 5; i < dest.length; i++) {
            assertEquals(0, dest[i]);
        }

        // position should now be 3, reading remaining 2 bytes
        assertEquals(2, in.available());
        read = in.read(dest, 0, 2);
        assertEquals(2, read);
        assertEquals(8, dest[0] & 0xFF);
        assertEquals(9, dest[1] & 0xFF);
    }

    @Test
    public void testReadByteArrayZeroLength() {
        byte[] data = new byte[]{1, 2, 3};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        byte[] dest = new byte[5];
        int read = in.read(dest, 1, 0);
        assertEquals(0, read);
        // position must not advance
        assertEquals(3, in.available());
        // buffer unchanged
        assertArrayEquals(new byte[5], dest);
    }

    @Test(expected = NullPointerException.class)
    public void testReadByteArrayNullBuffer() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2});
        in.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArrayNegativeOffset() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2});
        byte[] dest = new byte[2];
        in.read(dest, -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArrayNegativeLength() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2});
        byte[] dest = new byte[2];
        in.read(dest, 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArrayLengthBeyondArray() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2, 3});
        byte[] dest = new byte[2];
        // off=1, len=2 would require index 3 which is out of bounds
        in.read(dest, 1, 2);
    }

    @Test
    public void testReadByteArrayLengthGreaterThanAvailable() {
        byte[] data = new byte[]{10, 20};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        byte[] dest = new byte[5];
        int read = in.read(dest, 0, 5); // request more than available
        assertEquals(2, read);
        assertEquals(10, dest[0] & 0xFF);
        assertEquals(20, dest[1] & 0xFF);
        // remaining bytes untouched
        for (int i = 2; i < dest.length; i++) {
            assertEquals(0, dest[i]);
        }
        // stream exhausted
        assertEquals(-1, in.read());
    }

    @Test
    public void testSkipWithinBounds() {
        byte[] data = new byte[]{0, 1, 2, 3, 4, 5};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        long skipped = in.skip(3);
        assertEquals(3, skipped);
        assertEquals(3, in.read()); // should be the 4th byte (value 3)
    }

    @Test
    public void testSkipBeyondEnd() {
        byte[] data = new byte[]{9, 8, 7};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        long skipped = in.skip(10);
        assertEquals(3, skipped);
        assertEquals(-1, in.read());
    }

    @Test
    public void testSkipNegative() {
        byte[] data = new byte[]{1, 2, 3};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        long skipped = in.skip(-5);
        assertEquals(0, skipped);
        assertEquals(1, in.read());
    }

    @Test
    public void testAvailableBeforeAndAfterOperations() {
        byte[] data = new byte[]{5, 6, 7, 8};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        assertEquals(4, in.available());

        in.read();
        assertEquals(3, in.available());

        in.skip(2);
        assertEquals(0, in.available());

        // after reaching end, available stays 0
        assertEquals(0, in.available());
    }

    @Test
    public void testMarkAndReset() {
        byte[] data = new byte[]{11, 22, 33, 44, 55};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        // read two bytes
        assertEquals(11, in.read());
        assertEquals(22, in.read());

        // mark at position after two reads
        in.mark(0);
        // read next two bytes
        assertEquals(33, in.read());
        assertEquals(44, in.read());

        // reset should bring back to mark (position after second byte)
        in.reset();
        assertEquals(33, in.read()); // same byte again
        assertEquals(44, in.read());
        // ensure subsequent read continues correctly
        assertEquals(55, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testMarkSupported() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1});
        assertTrue(in.markSupported());
    }

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2, 3});
        in.close(); // should not throw any exception
        // after close, stream should still behave normally (as per implementation)
        assertEquals(1, in.read());
    }

    @Test
    public void testEmptyBuffer() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[0]);
        assertEquals(0, in.available());
        assertEquals(-1, in.read());
        assertEquals(-1, in.read(new byte[1], 0, 1));
        assertEquals(0, in.skip(5));
        assertTrue(in.markSupported());
        in.mark(0);
        in.reset(); // should not change anything
        assertEquals(-1, in.read());
    }
}
