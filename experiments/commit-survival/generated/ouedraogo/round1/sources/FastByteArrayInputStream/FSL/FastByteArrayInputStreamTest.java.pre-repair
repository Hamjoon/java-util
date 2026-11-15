package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;
import java.io.IOException;
import java.util.Arrays;

public class FastByteArrayInputStreamTest {

    @Test
    public void testConstructorAndAvailableInitial() {
        byte[] data = {10, 20, 30};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        assertEquals("Initial available bytes must equal length of buffer", data.length, in.available());
    }

    @Test
    public void testReadSingleByte() {
        byte[] data = {0, 1, 2, (byte) 255};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        // read() returns unsigned byte values (0‑255) or -1 at EOF
        assertEquals(0, in.read());
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(255, in.read()); // 0xFF should be 255 after & 0xff
        assertEquals(-1, in.read()); // end of stream
        assertEquals(-1, in.read()); // subsequent reads stay at -1
    }

    @Test
    public void testReadByteArrayFull() {
        byte[] data = {5, 6, 7, 8, 9};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        byte[] target = new byte[5];
        int bytesRead = in.read(target, 0, target.length);
        assertEquals(5, bytesRead);
        assertArrayEquals(data, target);
        assertEquals(-1, in.read()); // now at EOF
    }

    @Test
    public void testReadByteArrayWithOffsetAndLength() {
        byte[] data = {11, 22, 33, 44, 55};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        byte[] target = new byte[10];
        // leave first two bytes untouched, read three bytes into target[2]
        int bytesRead = in.read(target, 2, 3);
        assertEquals(3, bytesRead);
        assertEquals(0, target[0]); // untouched
        assertEquals(0, target[1]); // untouched
        assertEquals(11, target[2]);
        assertEquals(22, target[3]);
        assertEquals(33, target[4]);
        // remaining bytes still zero
        for (int i = 5; i < target.length; i++) {
            assertEquals(0, target[i]);
        }
    }

    @Test
    public void testReadByteArrayPartialWhenLenGreaterThanAvailable() {
        byte[] data = {100, 101};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        byte[] target = new byte[5];
        int bytesRead = in.read(target, 0, 5); // request more than available
        assertEquals(2, bytesRead);
        assertEquals(100, target[0]);
        assertEquals(101, target[1]);
        // rest unchanged (still zero)
        for (int i = 2; i < target.length; i++) {
            assertEquals(0, target[i]);
        }
        assertEquals(-1, in.read()); // EOF
    }

    @Test
    public void testReadByteArrayZeroLength() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        byte[] target = new byte[3];
        int bytesRead = in.read(target, 0, 0);
        assertEquals(0, bytesRead);
        // buffer unchanged
        assertArrayEquals(new byte[3], target);
        // position should not have moved
        assertEquals(3, in.available()); // still all bytes available
    }

    @Test(expected = NullPointerException.class)
    public void testReadByteArrayNullBuffer() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1});
        in.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArrayNegativeOffset() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2});
        byte[] target = new byte[2];
        in.read(target, -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArrayNegativeLength() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2});
        byte[] target = new byte[2];
        in.read(target, 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArrayLengthExceedsBuffer() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2});
        byte[] target = new byte[2];
        // off=1, len=2 would exceed target.length (2)
        in.read(target, 1, 2);
    }

    @Test
    public void testReadByteArrayAtEofReturnsMinusOne() {
        byte[] data = {42};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        // consume the single byte
        assertEquals(42, in.read());
        // now at EOF
        byte[] target = new byte[1];
        int result = in.read(target, 0, 1);
        assertEquals(-1, result);
    }

    @Test
    public void testSkipWithinBounds() {
        byte[] data = {10, 20, 30, 40, 50};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        long skipped = in.skip(2);
        assertEquals(2L, skipped);
        assertEquals(2, in.available()); // 5 - 2 = 3 left actually, but after skip pos=2, available = 5-2 =3
        assertEquals(30, in.read()); // should read the third element
    }

    @Test
    public void testSkipBeyondEnd() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        long skipped = in.skip(10);
        assertEquals(3L, skipped); // only 3 bytes available
        assertEquals(-1, in.read()); // EOF
        assertEquals(0, in.available());
    }

    @Test
    public void testSkipNegative() {
        byte[] data = {5, 6, 7};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        long skipped = in.skip(-5);
        assertEquals(0L, skipped);
        // position unchanged
        assertEquals(3, in.available());
        assertEquals(5, in.read());
    }

    @Test
    public void testMarkAndReset() {
        byte[] data = {9, 8, 7, 6};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        assertEquals(9, in.read()); // pos =1
        in.mark(0);                  // mark =1
        assertEquals(8, in.read()); // pos =2
        assertEquals(7, in.read()); // pos =3
        in.reset();                  // pos back to 1
        assertEquals(8, in.read()); // reads again from marked position
        assertEquals(7, in.read());
    }

    @Test
    public void testMarkSupported() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{0});
        assertTrue(in.markSupported());
    }

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2, 3});
        in.close(); // should not throw any exception
        // After close, stream should still be usable because close is a no‑op
        assertEquals(1, in.read());
    }

    @Test
    public void testAvailableAfterVariousOperations() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        assertEquals(5, in.available());
        in.read(); // consume 1
        assertEquals(4, in.available());
        in.skip(2); // skip 2 more
        assertEquals(1, in.available());
        byte[] buf = new byte[2];
        int read = in.read(buf, 0, 2); // only 1 byte left, should read 1
        assertEquals(1, read);
        assertEquals(0, in.available());
    }

    @Test
    public void testConstructorWithEmptyArray() {
        byte[] empty = new byte[0];
        FastByteArrayInputStream in = new FastByteArrayInputStream(empty);
        assertEquals(0, in.available());
        assertEquals(-1, in.read());
        assertEquals(0L, in.skip(5));
        assertTrue(in.markSupported());
    }

    @Test
    public void testReadAfterMarkResetMultipleTimes() {
        byte[] data = {10, 20, 30};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        in.mark(0);
        assertEquals(10, in.read());
        in.reset();
        assertEquals(10, in.read());
        in.mark(0);
        assertEquals(20, in.read());
        in.reset();
        assertEquals(20, in.read());
        assertEquals(30, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testSkipZeroDoesNotChangePosition() {
        byte[] data = {1, 2, 3};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        long skipped = in.skip(0);
        assertEquals(0L, skipped);
        assertEquals(3, in.available());
        assertEquals(1, in.read());
    }
}
