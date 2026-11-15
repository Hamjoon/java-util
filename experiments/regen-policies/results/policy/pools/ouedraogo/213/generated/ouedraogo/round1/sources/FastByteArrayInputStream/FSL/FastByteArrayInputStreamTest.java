package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

public class FastByteArrayInputStreamTest {

    @Test
    public void testReadSingleBytes() {
        byte[] data = {0, 1, 2, 3, 4, -1, 127};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        // Read each byte and verify unsigned conversion
        for (int i = 0; i < data.length; i++) {
            int expected = data[i] & 0xFF;
            assertEquals("Byte at index " + i + " should match", expected, in.read());
        }

        // End of stream should return -1
        assertEquals(-1, in.read());
        assertEquals(-1, in.read()); // repeatedly -1
    }

    @Test
    public void testReadIntoArrayNormal() {
        byte[] source = "HelloWorld".getBytes();
        FastByteArrayInputStream in = new FastByteArrayInputStream(source);

        byte[] target = new byte[5];
        int bytesRead = in.read(target, 0, target.length);
        assertEquals(5, bytesRead);
        assertArrayEquals("Hello".getBytes(), target);

        // second read
        bytesRead = in.read(target, 0, target.length);
        assertEquals(5, bytesRead);
        assertArrayEquals("World".getBytes(), target);
    }

    @Test
    public void testReadIntoArrayPartialAndZeroLength() {
        byte[] source = {10, 20, 30};
        FastByteArrayInputStream in = new FastByteArrayInputStream(source);

        byte[] target = new byte[10];
        // request more than available -> should read only available bytes
        int bytesRead = in.read(target, 2, 8);
        assertEquals(3, bytesRead);
        assertEquals(10, target[2] & 0xFF);
        assertEquals(20, target[3] & 0xFF);
        assertEquals(30, target[4] & 0xFF);

        // now at EOF
        bytesRead = in.read(target, 0, 5);
        assertEquals(-1, bytesRead);

        // zero length request should return 0 (not -1)
        FastByteArrayInputStream in2 = new FastByteArrayInputStream(new byte[]{1,2,3});
        bytesRead = in2.read(target, 0, 0);
        assertEquals(0, bytesRead);
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoArrayNullBuffer() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1});
        in.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayNegativeOffset() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1,2,3});
        byte[] buf = new byte[5];
        in.read(buf, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayLengthTooLarge() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1,2,3});
        byte[] buf = new byte[5];
        // off=3, len=3 would exceed buf length (5-3=2)
        in.read(buf, 3, 3);
    }

    @Test
    public void testSkipWithinBounds() {
        byte[] data = {0,1,2,3,4,5,6,7,8,9};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        long skipped = in.skip(4);
        assertEquals(4L, skipped);
        assertEquals(4, in.read()); // next byte should be 4

        // skip more than remaining
        skipped = in.skip(100);
        assertEquals(5L, skipped); // only 5 bytes left
        assertEquals(-1, in.read()); // EOF
    }

    @Test
    public void testSkipNegative() {
        byte[] data = {5,6,7};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        long skipped = in.skip(-10);
        assertEquals(0L, skipped);
        assertEquals(5, in.read());
    }

    @Test
    public void testAvailable() {
        byte[] data = {1,2,3,4};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        assertEquals(4, in.available());

        in.read();
        assertEquals(3, in.available());

        in.skip(2);
        assertEquals(0, in.available());

        // after reaching EOF, available stays 0
        assertEquals(-1, in.read());
        assertEquals(0, in.available());
    }

    @Test
    public void testMarkAndReset() throws IOException {
        byte[] data = "MarkResetTest".getBytes();
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        // read some bytes
        byte[] buf = new byte[4];
        int n = in.read(buf, 0, 4);
        assertEquals(4, n);
        assertArrayEquals("Mark".getBytes(), buf);

        // set mark
        in.mark(0);
        // read further
        n = in.read(buf, 0, 4);
        assertEquals(4, n);
        assertArrayEquals("Rese".getBytes(), buf);

        // reset back to mark
        in.reset();
        n = in.read(buf, 0, 4);
        assertEquals(4, n);
        assertArrayEquals("Rese".getBytes(), buf);
    }

    @Test
    public void testMarkSupported() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{});
        assertTrue(in.markSupported());
    }

    @Test
    public void testCloseDoesNotAffectReading() throws IOException {
        byte[] data = {9,8,7};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        in.close(); // should be no‑op
        assertEquals(9, in.read());
        assertEquals(8, in.read());
        assertEquals(7, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testReadAfterExhaustionDoesNotChangeState() {
        byte[] data = {1,2};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(-1, in.read());
        assertEquals(-1, in.read()); // still -1
        assertEquals(0, in.available());
    }

    @Test
    public void testReadByteArrayWithOffset() {
        byte[] source = "ABCDEFGHIJ".getBytes();
        FastByteArrayInputStream in = new FastByteArrayInputStream(source);
        byte[] target = new byte[12];
        // Fill with sentinel
        Arrays.fill(target, (byte) '*');

        int read = in.read(target, 2, 5); // should read A‑E into positions 2‑6
        assertEquals(5, read);
        assertEquals((byte)'A', target[2]);
        assertEquals((byte)'E', target[6]);
        // The rest should remain unchanged
        assertEquals((byte)'*', target[0]);
        assertEquals((byte)'*', target[1]);
        assertEquals((byte)'*', target[7]);
    }
}
