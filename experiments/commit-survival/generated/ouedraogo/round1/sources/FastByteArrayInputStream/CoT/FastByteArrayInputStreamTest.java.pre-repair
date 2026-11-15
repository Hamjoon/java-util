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
        // simple deterministic byte pattern: 0,1,2,...,9
        data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        stream = new FastByteArrayInputStream(data);
    }

    /* ---------- read() ---------- */
    @Test
    public void testReadSingleBytes() throws IOException {
        for (int i = 0; i < data.length; i++) {
            int val = stream.read();
            assertEquals("Byte at position " + i + " should match", data[i] & 0xff, val);
        }
        // after all bytes have been read, read() must return -1
        assertEquals(-1, stream.read());
        // subsequent reads still return -1
        assertEquals(-1, stream.read());
    }

    /* ---------- read(byte[],int,int) ---------- */
    @Test
    public void testReadIntoArrayFull() {
        byte[] dest = new byte[10];
        int read = stream.read(dest, 0, dest.length);
        assertEquals(10, read);
        assertArrayEquals(data, dest);
        // stream exhausted
        assertEquals(-1, stream.read(dest, 0, 1));
    }

    @Test
    public void testReadIntoArrayPartial() {
        byte[] dest = new byte[5];
        int read = stream.read(dest, 1, 4); // leave dest[0] untouched
        assertEquals(4, read);
        // verify copied portion
        for (int i = 0; i < 4; i++) {
            assertEquals(data[i] & 0xff, dest[i + 1] & 0xff);
        }
        // dest[0] should still be default 0
        assertEquals(0, dest[0]);
    }

    @Test
    public void testReadZeroLength() {
        byte[] dest = new byte[5];
        int read = stream.read(dest, 2, 0);
        assertEquals(0, read);
        // buffer must stay unchanged
        for (byte b : dest) {
            assertEquals(0, b);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testReadNullArray() {
        stream.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadNegativeOffset() {
        byte[] dest = new byte[5];
        stream.read(dest, -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadNegativeLength() {
        byte[] dest = new byte[5];
        stream.read(dest, 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadLengthBeyondArray() {
        byte[] dest = new byte[5];
        stream.read(dest, 2, 4); // 2+4 > 5
    }

    /* ---------- skip(long) ---------- */
    @Test
    public void testSkipWithinBounds() {
        long skipped = stream.skip(4);
        assertEquals(4L, skipped);
        assertEquals(4, stream.read()); // should read the 5th byte (value 4)
    }

    @Test
    public void testSkipBeyondEnd() {
        long skipped = stream.skip(100);
        assertEquals(10L, skipped); // only 10 bytes exist
        assertEquals(-1, stream.read()); // EOF
    }

    @Test
    public void testSkipNegative() {
        long skipped = stream.skip(-5);
        assertEquals(0L, skipped);
        assertEquals(0, stream.read()); // still at start
    }

    /* ---------- available() ---------- */
    @Test
    public void testAvailableBeforeAndAfterRead() {
        assertEquals(10, stream.available());
        stream.read(); // consume one byte
        assertEquals(9, stream.available());
        stream.skip(5);
        assertEquals(4, stream.available());
        stream.read(new byte[4], 0, 4);
        assertEquals(0, stream.available());
    }

    /* ---------- mark / reset ---------- */
    @Test
    public void testMarkAndReset() {
        // read first three bytes
        assertEquals(0, stream.read());
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());

        // mark at position 3
        stream.mark(0);
        // read next two bytes
        assertEquals(3, stream.read());
        assertEquals(4, stream.read());

        // reset to mark (position 3)
        stream.reset();

        // reading again should yield same values (3,4)
        assertEquals(3, stream.read());
        assertEquals(4, stream.read());
    }

    @Test
    public void testResetWithoutExplicitMark() {
        // default mark is 0 (set in constructor)
        stream.read(); // move forward
        stream.reset(); // should go back to start
        assertEquals(0, stream.read());
    }

    @Test
    public void testMarkAtEndAndReset() {
        stream.skip(10); // move to end
        stream.mark(0);
        // read after end returns -1
        assertEquals(-1, stream.read());
        // reset should bring us back to the end position (still -1 on read)
        stream.reset();
        assertEquals(-1, stream.read());
    }

    /* ---------- markSupported ---------- */
    @Test
    public void testMarkSupportedAlwaysTrue() {
        assertTrue(stream.markSupported());
    }

    /* ---------- close ---------- */
    @Test
    public void testCloseDoesNotThrow() throws IOException {
        stream.close(); // should not throw
        // after close, stream should still behave normally (as per class contract)
        assertEquals(0, stream.read());
    }
}
