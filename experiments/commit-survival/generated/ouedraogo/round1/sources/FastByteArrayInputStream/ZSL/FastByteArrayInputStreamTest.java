package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class FastByteArrayInputStreamTest {

    private byte[] data;
    private FastByteArrayInputStream stream;

    @Before
    public void setUp() {
        // Sample data: includes positive, zero, and negative (signed) byte values
        data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -128};
        stream = new FastByteArrayInputStream(data);
    }

    /*** Constructor and basic state ***/
    @Test
    public void testConstructorInitializesCorrectly() {
        assertEquals("available should equal data length after construction",
                data.length, stream.available());
        assertTrue("markSupported should return true", stream.markSupported());
    }

    /*** read() single‑byte method ***/
    @Test
    public void testReadSingleByteReturnsUnsignedValues() throws IOException {
        // First byte is 0
        assertEquals(0, stream.read());

        // Read through the positive range
        for (int i = 1; i <= 9; i++) {
            assertEquals(i, stream.read());
        }

        // -1 should be read as 255 (0xFF)
        assertEquals(255, stream.read());

        // -128 should be read as 128 (0x80)
        assertEquals(128, stream.read());

        // End of stream
        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read()); // subsequent reads stay at -1
    }

    /*** read(byte[],int,int) normal operation ***/
    @Test
    public void testReadIntoArrayNormal() {
        byte[] target = new byte[5];
        int bytesRead = stream.read(target, 0, target.length);
        assertEquals(5, bytesRead);
        assertArrayEquals(new byte[]{0, 1, 2, 3, 4}, target);

        // read remaining bytes into a larger buffer with offset
        byte[] larger = new byte[10];
        bytesRead = stream.read(larger, 2, 4); // should read 4 more bytes (5,6,7,8)
        assertEquals(4, bytesRead);
        assertEquals(0, larger[0]); // untouched
        assertEquals(0, larger[1]); // untouched
        assertArrayEquals(new byte[]{5, 6, 7, 8}, new byte[]{larger[2], larger[3], larger[4], larger[5]});
    }

    /*** read(byte[],int,int) edge cases ***/
    @Test(expected = NullPointerException.class)
    public void testReadIntoArrayNullBufferThrows() {
        stream.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayNegativeOffsetThrows() {
        stream.read(new byte[4], -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayNegativeLengthThrows() {
        stream.read(new byte[4], 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayLengthBeyondArrayThrows() {
        stream.read(new byte[4], 2, 3); // 2+3 > 4
    }

    @Test
    public void testReadIntoArrayZeroLengthReturnsZero() {
        byte[] buf = new byte[4];
        int result = stream.read(buf, 0, 0);
        assertEquals(0, result);
        // Position should not have advanced
        assertEquals(data.length, stream.available() + stream.available() - (data.length - stream.available()));
    }

    @Test
    public void testReadIntoArrayWhenAtEndReturnsMinusOne() {
        // Exhaust the stream first
        while (stream.read() != -1) {
            // consume
        }
        byte[] buf = new byte[4];
        int result = stream.read(buf, 0, 4);
        assertEquals(-1, result);
    }

    /*** skip(long) ***/
    @Test
    public void testSkipWithinBounds() {
        long skipped = stream.skip(3);
        assertEquals(3L, skipped);
        assertEquals(3, data[0] & 0xFF); // next byte to read should be 3
        assertEquals(3, stream.read());
    }

    @Test
    public void testSkipBeyondEndStopsAtEnd() {
        long skipped = stream.skip(100);
        assertEquals(data.length, skipped);
        assertEquals(-1, stream.read());
    }

    @Test
    public void testSkipNegativeDoesNothing() {
        long skipped = stream.skip(-5);
        assertEquals(0L, skipped);
        assertEquals(0, stream.read()); // still at beginning
    }

    /*** available() ***/
    @Test
    public void testAvailableReflectsRemainingBytes() {
        assertEquals(data.length, stream.available());
        stream.read(); // consume one byte
        assertEquals(data.length - 1, stream.available());
        stream.skip(5);
        assertEquals(data.length - 1 - 5, stream.available());
    }

    /*** mark / reset ***/
    @Test
    public void testMarkAndResetRestorePosition() {
        // Read first three bytes
        assertEquals(0, stream.read());
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());

        // Mark at this point (position = 3)
        stream.mark(0);

        // Read next two bytes
        assertEquals(3, stream.read());
        assertEquals(4, stream.read());

        // Reset should bring us back to the mark (position = 3)
        stream.reset();
        assertEquals(3, stream.read());
        assertEquals(4, stream.read());

        // Reset again should still be at the same spot
        stream.reset();
        assertEquals(3, stream.read());
    }

    @Test
    public void testMarkWithoutReadingThenResetKeepsAtStart() {
        stream.mark(0);
        stream.reset();
        assertEquals(0, stream.read());
    }

    /*** close() ***/
    @Test
    public void testCloseDoesNotThrow() throws IOException {
        stream.close(); // should be a no‑op and not throw
        // After close, the stream should still behave normally
        assertEquals(0, stream.read());
    }
}
