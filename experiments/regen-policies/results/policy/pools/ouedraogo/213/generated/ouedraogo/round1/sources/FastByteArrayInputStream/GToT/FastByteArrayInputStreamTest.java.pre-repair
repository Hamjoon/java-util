package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import java.io.IOException;
import static org.junit.Assert.*;

public class FastByteArrayInputStreamTest {

    private byte[] sample;
    private FastByteArrayInputStream stream;

    @Before
    public void setUp() {
        // Sample data: 0,1,2,3,4,5,6,7,8,9
        sample = new byte[10];
        for (int i = 0; i < sample.length; i++) {
            sample[i] = (byte) i;
        }
        stream = new FastByteArrayInputStream(sample);
    }

    // ---------- Constructor & available ----------
    @Test
    public void testConstructorAndAvailable() {
        assertEquals("Initially available should equal buffer length",
                sample.length, stream.available());
    }

    // ---------- read() ----------
    @Test
    public void testReadSingleBytes() {
        for (int i = 0; i < sample.length; i++) {
            int val = stream.read();
            assertEquals("Byte " + i + " must match", sample[i] & 0xff, val);
        }
        // after EOF
        assertEquals(-1, stream.read());
        // subsequent reads still -1
        assertEquals(-1, stream.read());
    }

    // ---------- read(byte[],int,int) ----------
    @Test
    public void testReadIntoArrayFull() {
        byte[] dest = new byte[10];
        int read = stream.read(dest, 0, dest.length);
        assertEquals("Should read all bytes", sample.length, read);
        assertArrayEquals("Destination must equal source", sample, dest);
        // stream now at EOF
        assertEquals(-1, stream.read(dest, 0, 1));
    }

    @Test
    public void testReadIntoArrayPartial() {
        byte[] dest = new byte[5];
        int read = stream.read(dest, 1, 3); // read 3 bytes into dest[1..3]
        assertEquals(3, read);
        // dest[0] unchanged (default 0), dest[1..3] should match sample[0..2]
        assertEquals(0, dest[0]);
        assertEquals(sample[0] & 0xff, dest[1] & 0xff);
        assertEquals(sample[1] & 0xff, dest[2] & 0xff);
        assertEquals(sample[2] & 0xff, dest[3] & 0xff);
        assertEquals(0, dest[4]);
    }

    @Test(expected = NullPointerException.class)
    public void testReadNullArray() {
        stream.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadNegativeOffset() {
        stream.read(new byte[5], -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadNegativeLength() {
        stream.read(new byte[5], 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadOffsetLengthExceedArray() {
        stream.read(new byte[5], 3, 3); // 3+3 > 5
    }

    @Test
    public void testReadZeroLength() {
        byte[] dest = new byte[5];
        int read = stream.read(dest, 0, 0);
        assertEquals(0, read);
        // ensure array unchanged
        for (byte b : dest) {
            assertEquals(0, b);
        }
    }

    @Test
    public void testReadWhenExhaustedReturnsMinusOne() {
        // Exhaust the stream first
        stream.read(new byte[10], 0, 10);
        assertEquals(-1, stream.read(new byte[5], 0, 5));
    }

    // ---------- skip(long) ----------
    @Test
    public void testSkipWithinBounds() {
        long skipped = stream.skip(4);
        assertEquals(4L, skipped);
        assertEquals(4, stream.available());
        assertEquals(sample[4] & 0xff, stream.read());
    }

    @Test
    public void testSkipBeyondEnd() {
        long skipped = stream.skip(100L);
        assertEquals(sample.length, skipped);
        assertEquals(-1, stream.read());
    }

    @Test
    public void testSkipNegative() {
        long skipped = stream.skip(-5L);
        assertEquals(0L, skipped);
        assertEquals(sample.length, stream.available());
    }

    // ---------- markSupported ----------
    @Test
    public void testMarkSupported() {
        assertTrue(stream.markSupported());
    }

    // ---------- mark & reset ----------
    @Test
    public void testMarkAndReset() {
        // read two bytes
        assertEquals(sample[0] & 0xff, stream.read());
        assertEquals(sample[1] & 0xff, stream.read());

        stream.mark(0);          // mark at position 2
        assertEquals(sample[2] & 0xff, stream.read()); // read third byte

        stream.reset();           // back to position 2
        assertEquals(sample[2] & 0xff, stream.read()); // should read same third byte again
    }

    @Test
    public void testResetWithoutExplicitMark() {
        // No prior mark – default mark is 0
        stream.read(); // advance to pos 1
        stream.reset();
        assertEquals(sample[0] & 0xff, stream.read()); // back to start
    }

    @Test
    public void testMultipleMarks() {
        stream.read(); // pos =1
        stream.mark(0); // mark=1
        stream.read(); // pos=2
        stream.mark(0); // mark=2
        stream.read(); // pos=3
        stream.reset(); // back to 2
        assertEquals(sample[2] & 0xff, stream.read()); // read again from 2
        stream.reset(); // back to 2 again
        assertEquals(sample[2] & 0xff, stream.read());
    }

    // ---------- close ----------
    @Test
    public void testCloseDoesNotAffectStream() throws IOException {
        stream.close(); // should not throw
        // after close we can still read
        assertEquals(sample[0] & 0xff, stream.read());
        stream.close(); // calling again is fine
    }
}
