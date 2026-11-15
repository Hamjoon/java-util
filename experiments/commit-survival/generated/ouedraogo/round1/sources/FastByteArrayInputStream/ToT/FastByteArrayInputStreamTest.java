package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FastByteArrayInputStreamTest {

    private static final byte[] SAMPLE_DATA = "Hello, World!".getBytes();
    private FastByteArrayInputStream emptyStream;
    private FastByteArrayInputStream fullStream;

    @Before
    public void setUp() {
        emptyStream = new FastByteArrayInputStream(new byte[0]);
        fullStream = new FastByteArrayInputStream(Arrays.copyOf(SAMPLE_DATA, SAMPLE_DATA.length));
    }

    /* ------------------- Constructor & basic state ------------------- */

    @Test
    public void testConstructorInitialState() {
        assertEquals("available should equal buffer length", SAMPLE_DATA.length, fullStream.available());
        assertTrue("markSupported should be true", fullStream.markSupported());
    }

    @Test
    public void testConstructorWithEmptyArray() {
        assertEquals("available should be zero for empty buffer", 0, emptyStream.available());
        assertEquals("read should return -1 immediately on empty buffer", -1, emptyStream.read());
    }

    /* ------------------- read() ------------------- */

    @Test
    public void testReadSingleByte() throws IOException {
        int first = fullStream.read();
        assertEquals("First byte should be 'H'", 'H', first);
        assertEquals("available should decrease by 1", SAMPLE_DATA.length - 1, fullStream.available());

        // read the rest of the bytes
        for (int i = 1; i < SAMPLE_DATA.length; i++) {
            assertEquals("Byte at position " + i + " should match", SAMPLE_DATA[i] & 0xff, fullStream.read());
        }

        // after all bytes are consumed
        assertEquals("read should return -1 at EOF", -1, fullStream.read());
    }

    @Test
    public void testReadBeyondEndReturnsMinusOne() {
        // exhaust the stream
        while (fullStream.read() != -1) { /* consume */ }

        assertEquals("Subsequent read after EOF should still return -1", -1, fullStream.read());
    }

    /* ------------------- read(byte[], int, int) ------------------- */

    @Test
    public void testReadIntoArrayNormal() {
        byte[] target = new byte[5];
        int bytesRead = fullStream.read(target, 0, target.length);
        assertEquals("Should read 5 bytes", 5, bytesRead);
        assertArrayEquals("First 5 bytes should match", Arrays.copyOfRange(SAMPLE_DATA, 0, 5), target);
        assertEquals("available should be reduced accordingly", SAMPLE_DATA.length - 5, fullStream.available());
    }

    @Test
    public void testReadIntoArrayWithOffset() {
        byte[] target = new byte[10];
        // Fill with sentinel value
        Arrays.fill(target, (byte) -1);
        int bytesRead = fullStream.read(target, 2, 4);
        assertEquals("Should read 4 bytes", 4, bytesRead);
        // Verify the region that was written
        assertArrayEquals("Bytes written at offset 2 should match",
                Arrays.copyOfRange(SAMPLE_DATA, 0, 4),
                Arrays.copyOfRange(target, 2, 6));
        // Verify untouched areas remain sentinel
        for (int i = 0; i < 2; i++) {
            assertEquals("Byte before offset unchanged", -1, target[i]);
        }
        for (int i = 6; i < target.length; i++) {
            assertEquals("Byte after written region unchanged", -1, target[i]);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoArrayNullArray() {
        fullStream.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayNegativeOffset() {
        byte[] buf = new byte[10];
        fullStream.read(buf, -1, 5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayNegativeLength() {
        byte[] buf = new byte[10];
        fullStream.read(buf, 0, -3);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayLengthExceedsArray() {
        byte[] buf = new byte[5];
        fullStream.read(buf, 2, 4); // 2+4 > 5
    }

    @Test
    public void testReadIntoArrayWhenAtEOFReturnsMinusOne() {
        // Exhaust the stream
        while (fullStream.read() != -1) { /* consume */ }
        byte[] buf = new byte[4];
        int result = fullStream.read(buf, 0, buf.length);
        assertEquals("At EOF read should return -1", -1, result);
    }

    @Test
    public void testReadIntoArrayZeroLengthReturnsZero() {
        byte[] buf = new byte[10];
        int result = fullStream.read(buf, 0, 0);
        assertEquals("Reading zero length should return 0", 0, result);
        assertEquals("available should remain unchanged", SAMPLE_DATA.length, fullStream.available());
    }

    @Test
    public void testReadIntoArrayWithLenGreaterThanAvailable() {
        byte[] buf = new byte[100];
        int bytesRead = fullStream.read(buf, 0, buf.length);
        assertEquals("Should read only available bytes", SAMPLE_DATA.length, bytesRead);
        assertArrayEquals("Data read should equal original buffer",
                SAMPLE_DATA,
                Arrays.copyOf(buf, SAMPLE_DATA.length));
    }

    /* ------------------- skip(long) ------------------- */

    @Test
    public void testSkipWithinBounds() {
        long skipped = fullStream.skip(5);
        assertEquals("Should skip 5 bytes", 5, skipped);
        assertEquals("available should reflect skip", SAMPLE_DATA.length - 5, fullStream.available());
        assertEquals("Next byte after skip should be ',' (ASCII 44)", ',', (char) fullStream.read());
    }

    @Test
    public void testSkipBeyondEnd() {
        long skipped = fullStream.skip(1000);
        assertEquals("Should skip only up to end of stream", SAMPLE_DATA.length, skipped);
        assertEquals("available should be zero after over-skip", 0, fullStream.available());
        assertEquals("read after over-skip should be -1", -1, fullStream.read());
    }

    @Test
    public void testSkipNegativeIsTreatedAsZero() {
        long skipped = fullStream.skip(-10);
        assertEquals("Negative skip should result in 0 skipped", 0, skipped);
        assertEquals("available should remain unchanged", SAMPLE_DATA.length, fullStream.available());
    }

    /* ------------------- available() ------------------- */

    @Test
    public void testAvailableDecreasesAfterReadsAndSkips() {
        assertEquals("Initial available", SAMPLE_DATA.length, fullStream.available());
        fullStream.read();
        assertEquals("After one read", SAMPLE_DATA.length - 1, fullStream.available());
        fullStream.skip(3);
        assertEquals("After skipping 3", SAMPLE_DATA.length - 4, fullStream.available());
    }

    /* ------------------- mark / reset ------------------- */

    @Test
    public void testMarkAndReset() {
        // read some bytes
        fullStream.read(); // pos = 1
        fullStream.read(); // pos = 2

        fullStream.mark(0); // mark = 2
        // read further
        fullStream.read(); // pos = 3
        fullStream.read(); // pos = 4

        // reset back to mark
        fullStream.reset();
        assertEquals("After reset, position should be back to mark (2)", 2, fullStream.available());

        // The next byte should be the third byte of original data
        assertEquals("Byte after reset should be original third byte", SAMPLE_DATA[2] & 0xff, fullStream.read());
    }

    @Test
    public void testResetWithoutPriorMarkUsesInitialMarkZero() {
        // read a few bytes
        fullStream.read(); // pos = 1
        fullStream.read(); // pos = 2
        // reset without explicit mark (default mark = 0)
        fullStream.reset();
        assertEquals("After reset without prior mark, position should be 0", SAMPLE_DATA.length, fullStream.available());
        assertEquals("First byte after reset should be 'H'", 'H', fullStream.read());
    }

    @Test
    public void testMarkSupportedReturnsTrue() {
        assertTrue("markSupported should always return true", fullStream.markSupported());
    }

    /* ------------------- close() ------------------- */

    @Test
    public void testCloseDoesNotAffectSubsequentOperations() throws IOException {
        fullStream.close(); // no-op
        // Ensure stream still works after close
        assertEquals("First byte should still be readable after close", 'H', fullStream.read());
        assertEquals("available should be length-1 after one read", SAMPLE_DATA.length - 1, fullStream.available());
    }

    /* ------------------- Additional Edge Cases ------------------- */

    @Test
    public void testReadAfterMarkAndPartialRead() {
        // Mark at start
        fullStream.mark(0);
        // Read half the data
        byte[] half = new byte[SAMPLE_DATA.length / 2];
        int read = fullStream.read(half, 0, half.length);
        assertEquals("Should read half of the data", half.length, read);
        // Reset to beginning
        fullStream.reset();
        assertEquals("available after reset should be full length", SAMPLE_DATA.length, fullStream.available());
        // Read again and verify same data
        byte[] again = new byte[half.length];
        fullStream.read(again, 0, again.length);
        assertArrayEquals("Data after reset should match first read", half, again);
    }

    @Test
    public void testMultipleMarksAndResets() {
        fullStream.read(); // pos =1
        fullStream.mark(0); // mark =1
        fullStream.read(); // pos =2
        fullStream.mark(0); // mark =2
        fullStream.read(); // pos =3
        fullStream.reset(); // back to mark=2
        assertEquals("After first reset, should be at position 2", SAMPLE_DATA.length - 2, fullStream.available());
        fullStream.reset(); // back to earlier mark=1
        assertEquals("After second reset, should be at position 1", SAMPLE_DATA.length - 1, fullStream.available());
    }
}
