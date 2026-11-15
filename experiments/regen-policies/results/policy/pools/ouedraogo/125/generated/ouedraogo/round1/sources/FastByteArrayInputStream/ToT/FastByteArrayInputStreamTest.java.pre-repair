package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FastByteArrayInputStreamTest {

    private static final byte[] SAMPLE_DATA = new byte[]{
            (byte) 0x00, (byte) 0x7F, (byte) 0x80, (byte) 0xFF,
            (byte) 0x10, (byte) 0x20, (byte) 0x30, (byte) 0x40
    };

    private FastByteArrayInputStream stream;

    @Before
    public void setUp() {
        stream = new FastByteArrayInputStream(Arrays.copyOf(SAMPLE_DATA, SAMPLE_DATA.length));
    }

    /* -------------------------------------------------
     *  Tests for read()
     * ------------------------------------------------- */
    @Test
    public void testReadSingleByteTypical() {
        // Read first three bytes and verify unsigned conversion
        assertEquals(0x00, stream.read());
        assertEquals(0x7F, stream.read());
        assertEquals(0x80, stream.read()); // 0x80 & 0xff = 128
        // Verify that available decreased accordingly
        assertEquals(SAMPLE_DATA.length - 3, stream.available());
    }

    @Test
    public void testReadSingleByteUntilEof() {
        // Exhaust the stream
        for (int i = 0; i < SAMPLE_DATA.length; i++) {
            assertNotEquals(-1, stream.read());
        }
        // After all bytes have been read, further reads return -1
        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read()); // multiple calls stay at -1
    }

    /* -------------------------------------------------
     *  Tests for read(byte[], int, int)
     * ------------------------------------------------- */
    @Test
    public void testReadIntoArrayTypical() {
        byte[] target = new byte[4];
        int bytesRead = stream.read(target, 0, target.length);
        assertEquals(4, bytesRead);
        assertArrayEquals(new byte[]{0x00, 0x7F, (byte) 0x80, (byte) 0xFF}, target);
        assertEquals(SAMPLE_DATA.length - 4, stream.available());
    }

    @Test
    public void testReadIntoArrayWithOffset() {
        byte[] target = new byte[6];
        // Fill first two positions with sentinel values
        target[0] = (byte) 0xAA;
        target[1] = (byte) 0xBB;
        // Read 4 bytes starting at offset 2
        int bytesRead = stream.read(target, 2, 4);
        assertEquals(4, bytesRead);
        assertArrayEquals(new byte[]{
                (byte) 0xAA, (byte) 0xBB,
                0x00, 0x7F, (byte) 0x80, (byte) 0xFF
        }, target);
    }

    @Test
    public void testReadIntoArrayPartialWhenNearEof() {
        // Move position near the end
        stream.skip(6);
        byte[] target = new byte[5];
        int bytesRead = stream.read(target, 0, target.length);
        // Only 2 bytes remain
        assertEquals(2, bytesRead);
        assertArrayEquals(new byte[]{0x30, 0x40, 0, 0, 0}, target);
        assertEquals(0, stream.available());
        // Subsequent read returns -1
        assertEquals(-1, stream.read(target, 0, target.length));
    }

    @Test
    public void testReadIntoArrayZeroLength() {
        byte[] target = new byte[10];
        int bytesRead = stream.read(target, 5, 0);
        assertEquals(0, bytesRead);
        // Position should not move
        assertEquals(SAMPLE_DATA.length, stream.available());
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoArrayNullBuffer() {
        stream.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayNegativeOffset() {
        byte[] target = new byte[5];
        stream.read(target, -1, 3);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayNegativeLength() {
        byte[] target = new byte[5];
        stream.read(target, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayLengthExceedsBuffer() {
        byte[] target = new byte[5];
        // off = 2, len = 4 => off+len = 6 > target.length (5)
        stream.read(target, 2, 4);
    }

    /* -------------------------------------------------
     *  Tests for skip(long)
     * ------------------------------------------------- */
    @Test
    public void testSkipTypical() {
        long skipped = stream.skip(3);
        assertEquals(3L, skipped);
        assertEquals(SAMPLE_DATA.length - 3, stream.available());
        // Next read should be the 4th byte (0xFF)
        assertEquals(0xFF, stream.read());
    }

    @Test
    public void testSkipBeyondEnd() {
        long skipped = stream.skip(100);
        assertEquals(SAMPLE_DATA.length, skipped);
        assertEquals(0, stream.available());
        assertEquals(-1, stream.read());
    }

    @Test
    public void testSkipNegative() {
        long skipped = stream.skip(-5);
        assertEquals(0L, skipped);
        assertEquals(SAMPLE_DATA.length, stream.available());
    }

    /* -------------------------------------------------
     *  Tests for available()
     * ------------------------------------------------- */
    @Test
    public void testAvailableInitial() {
        assertEquals(SAMPLE_DATA.length, stream.available());
    }

    @Test
    public void testAvailableAfterReadAndSkip() {
        stream.read();          // 1 byte
        stream.skip(2);         // 2 more bytes
        assertEquals(SAMPLE_DATA.length - 3, stream.available());
    }

    /* -------------------------------------------------
     *  Tests for mark/reset
     * ------------------------------------------------- */
    @Test
    public void testMarkAndReset() {
        // Read two bytes
        stream.read();
        stream.read();
        // Mark at position 2
        stream.mark(0);
        // Read next three bytes
        stream.read();
        stream.read();
        stream.read();
        // Reset back to mark (position 2)
        stream.reset();
        // The next byte should be the third original byte (0x80)
        assertEquals(0x80, stream.read());
    }

    @Test
    public void testMarkSupported() {
        assertTrue(stream.markSupported());
    }

    /* -------------------------------------------------
     *  Tests for close()
     * ------------------------------------------------- */
    @Test
    public void testCloseDoesNotAffectReading() throws IOException {
        stream.close(); // no-op
        // Ensure stream still works after close
        assertEquals(0x00, stream.read());
        assertEquals(0x7F, stream.read());
    }

    /* -------------------------------------------------
     *  Combined scenario exercising multiple methods
     * ------------------------------------------------- */
    @Test
    public void testCombinedScenario() throws IOException {
        // 1. Read first byte
        assertEquals(0x00, stream.read());

        // 2. Mark here
        stream.mark(0);

        // 3. Skip 2 bytes (0x7F, 0x80)
        assertEquals(2L, stream.skip(2));

        // 4. Read next byte (should be 0xFF)
        assertEquals(0xFF, stream.read());

        // 5. Reset to mark (position after first byte)
        stream.reset();

        // 6. Read into array the remaining bytes
        byte[] remaining = new byte[stream.available()];
        int read = stream.read(remaining, 0, remaining.length);
        assertEquals(remaining.length, read);
        // Expected remaining bytes: 0x7F,0x80,0xFF,0x10,0x20,0x30,0x40
        assertArrayEquals(new byte[]{
                0x7F, (byte) 0x80, (byte) 0xFF,
                0x10, 0x20, 0x30, 0x40
        }, remaining);

        // 7. After consuming all, available should be 0 and read returns -1
        assertEquals(0, stream.available());
        assertEquals(-1, stream.read());

        // 8. Close and verify still -1
        stream.close();
        assertEquals(-1, stream.read());
    }
}
