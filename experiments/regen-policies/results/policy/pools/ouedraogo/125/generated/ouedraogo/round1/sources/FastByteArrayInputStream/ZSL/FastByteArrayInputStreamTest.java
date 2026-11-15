package com.cedarsoftware.util;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class FastByteArrayInputStreamTest {

    @Test
    public void testReadSingleByte_EveryByteAndEof() {
        byte[] data = {0, 1, 2, (byte) 255};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        // Read each byte and verify unsigned conversion
        assertEquals(0, in.read());
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(255, in.read()); // 0xFF -> 255 after & 0xff

        // End of stream should return -1
        assertEquals(-1, in.read());
        // Subsequent reads should continue to return -1
        assertEquals(-1, in.read());
    }

    @Test
    public void testReadByteArray_ExactLength() {
        byte[] source = "HelloWorld".getBytes();
        FastByteArrayInputStream in = new FastByteArrayInputStream(source);

        byte[] buffer = new byte[source.length];
        int bytesRead = in.read(buffer, 0, buffer.length);
        assertEquals(source.length, bytesRead);
        assertArrayEquals(source, buffer);

        // Stream exhausted
        assertEquals(-1, in.read());
    }

    @Test
    public void testReadByteArray_PartialAndZeroLength() {
        byte[] source = {10, 20, 30, 40, 50};
        FastByteArrayInputStream in = new FastByteArrayInputStream(source);

        byte[] buffer = new byte[10];
        // Request more bytes than available; should read only available (5)
        int firstRead = in.read(buffer, 2, 8);
        assertEquals(5, firstRead);
        assertEquals(10, buffer[2] & 0xFF);
        assertEquals(20, buffer[3] & 0xFF);
        assertEquals(30, buffer[4] & 0xFF);
        assertEquals(40, buffer[5] & 0xFF);
        assertEquals(50, buffer[6] & 0xFF);

        // Now stream is at EOF
        assertEquals(-1, in.read());

        // Zero‑length read should return 0 and not modify buffer
        int zeroRead = in.read(buffer, 0, 0);
        assertEquals(0, zeroRead);
    }

    @Test(expected = NullPointerException.class)
    public void testReadByteArray_NullArray() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2, 3});
        in.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArray_InvalidOffset() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2, 3});
        byte[] buffer = new byte[2];
        // offset out of bounds
        in.read(buffer, -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArray_InvalidLength() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2, 3});
        byte[] buffer = new byte[2];
        // length exceeds remaining space in buffer
        in.read(buffer, 0, 3);
    }

    @Test
    public void testSkip_NormalAndBeyondEnd() {
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        // Skip 4 bytes
        long skipped = in.skip(4);
        assertEquals(4L, skipped);
        assertEquals(4, in.read()); // next byte should be value 4

        // Skip more than remaining (should stop at end)
        skipped = in.skip(100);
        assertEquals(5L, skipped); // 5 bytes left (5..9)
        assertEquals(-1, in.read());
    }

    @Test
    public void testSkip_Negative() {
        byte[] data = {10, 20, 30};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        long skipped = in.skip(-5);
        assertEquals(0L, skipped);
        // Position should be unchanged
        assertEquals(10, in.read());
    }

    @Test
    public void testAvailable_BeforeAndAfterReadAndSkip() {
        byte[] data = {1, 2, 3, 4, 5};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);
        assertEquals(5, in.available());

        in.read();
        assertEquals(4, in.available());

        in.skip(2);
        assertEquals(1, in.available());

        in.read();
        assertEquals(0, in.available());
    }

    @Test
    public void testMarkAndReset() {
        byte[] data = {100, 101, 102, 103, 104};
        FastByteArrayInputStream in = new FastByteArrayInputStream(data);

        // Read first two bytes
        assertEquals(100, in.read());
        assertEquals(101, in.read());

        // Mark at position 2
        in.mark(0);

        // Read next two bytes
        assertEquals(102, in.read());
        assertEquals(103, in.read());

        // Reset should bring us back to the mark (position 2)
        in.reset();

        // Read again should return the same bytes as before reset
        assertEquals(102, in.read());
        assertEquals(103, in.read());

        // Continue reading to end
        assertEquals(104, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testMarkSupported() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{0});
        assertTrue(in.markSupported());
    }

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[]{1, 2, 3});
        // close should be a no‑op and must not throw
        in.close();
        // After close, stream should still behave normally (not required but verified)
        assertEquals(1, in.read());
    }

    @Test
    public void testEmptyBufferBehaviour() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(new byte[0]);

        // read should immediately return -1
        assertEquals(-1, in.read());

        // read into array should return -1
        byte[] buf = new byte[10];
        assertEquals(-1, in.read(buf, 0, 5));

        // skip should return 0
        assertEquals(0L, in.skip(10));

        // available should be 0
        assertEquals(0, in.available());

        // mark/reset should have no effect
        in.mark(0);
        in.reset();
        assertEquals(-1, in.read());
    }
}
