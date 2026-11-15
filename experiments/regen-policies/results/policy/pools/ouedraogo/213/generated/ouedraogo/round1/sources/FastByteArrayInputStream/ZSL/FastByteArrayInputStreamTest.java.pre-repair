package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class FastByteArrayInputStreamTest {

    private byte[] sampleData;
    private FastByteArrayInputStream stream;

    @Before
    public void setUp() {
        // Sample data contains a range of values, including negative byte values to test unsigned conversion.
        sampleData = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                (byte) 0x80, (byte) 0xFF}; // 0x80 = -128, 0xFF = -1
        stream = new FastByteArrayInputStream(sampleData);
    }

    @Test
    public void testReadSingleByte() {
        for (int i = 0; i < sampleData.length; i++) {
            int expected = sampleData[i] & 0xFF; // convert to unsigned int
            int actual = stream.read();
            assertEquals("Byte at index " + i + " differs", expected, actual);
        }
        // After all bytes have been read, read() should return -1
        assertEquals(-1, stream.read());
        // Subsequent reads should continue to return -1
        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadByteArrayExactLength() {
        byte[] target = new byte[sampleData.length];
        int bytesRead = stream.read(target, 0, target.length);
        assertEquals(sampleData.length, bytesRead);
        assertArrayEquals(sampleData, target);
        // End of stream now; next read should be -1
        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadByteArrayPartialLength() {
        byte[] target = new byte[5];
        int bytesRead = stream.read(target, 0, target.length);
        assertEquals(5, bytesRead);
        byte[] expected = new byte[]{0, 1, 2, 3, 4};
        assertArrayEquals(expected, target);
        // Verify that the internal position advanced correctly
        assertEquals(sampleData.length - 5, stream.available());
    }

    @Test
    public void testReadByteArrayLenGreaterThanAvailable() {
        // Skip most of the data, leaving only 2 bytes available
        stream.skip(sampleData.length - 2);
        assertEquals(2, stream.available());

        byte[] target = new byte[10];
        int bytesRead = stream.read(target, 0, target.length);
        assertEquals(2, bytesRead);
        assertArrayEquals(new byte[]{(byte) 0x80, (byte) 0xFF}, 
                new byte[]{target[0], target[1]});
        // After reading the last two bytes, stream is at EOF
        assertEquals(-1, stream.read());
    }

    @Test
    public void testReadByteArrayZeroLength() {
        byte[] target = new byte[10];
        int bytesRead = stream.read(target, 0, 0);
        assertEquals(0, bytesRead);
        // Position should not have moved
        assertEquals(sampleData.length, stream.available());
    }

    @Test(expected = NullPointerException.class)
    public void testReadByteArrayNullBuffer() {
        stream.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArrayNegativeOffset() {
        byte[] target = new byte[5];
        stream.read(target, -1, 3);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArrayNegativeLength() {
        byte[] target = new byte[5];
        stream.read(target, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadByteArrayLengthBeyondArray() {
        byte[] target = new byte[5];
        // off = 2, len = 4 would exceed target.length (5)
        stream.read(target, 2, 4);
    }

    @Test
    public void testSkipWithinBounds() {
        long skipped = stream.skip(4);
        assertEquals(4L, skipped);
        assertEquals(sampleData.length - 4, stream.available());
        // Verify that the next byte read is the 5th element (value 4)
        assertEquals(4, stream.read());
    }

    @Test
    public void testSkipBeyondEnd() {
        long skipped = stream.skip(1000L); // far beyond remaining bytes
        assertEquals(sampleData.length, skipped);
        assertEquals(0, stream.available());
        // After skipping past the end, read should return -1
        assertEquals(-1, stream.read());
    }

    @Test
    public void testSkipNegative() {
        // Position should stay unchanged and 0 be returned
        long skipped = stream.skip(-5L);
        assertEquals(0L, skipped);
        assertEquals(sampleData.length, stream.available());
    }

    @Test
    public void testAvailable() {
        assertEquals(sampleData.length, stream.available());
        stream.read(); // consume one byte
        assertEquals(sampleData.length - 1, stream.available());
        stream.skip(3);
        assertEquals(sampleData.length - 4, stream.available());
    }

    @Test
    public void testMarkAndReset() {
        // Read first two bytes
        assertEquals(0, stream.read());
        assertEquals(1, stream.read());

        // Mark at position 2
        stream.mark(0);
        int markPos = stream.available(); // should be remaining bytes

        // Read next three bytes
        assertEquals(2, stream.read());
        assertEquals(3, stream.read());
        assertEquals(4, stream.read());

        // Reset back to the mark
        stream.reset();
        assertEquals(markPos, stream.available());

        // After reset, the next byte should be the same as before reset (value 2)
        assertEquals(2, stream.read());
    }

    @Test
    public void testMarkSupported() {
        assertTrue(stream.markSupported());
    }

    @Test
    public void testCloseDoesNotAffectReading() throws IOException {
        stream.close(); // No-op in current implementation
        // Stream should still be functional after close
        assertEquals(sampleData.length, stream.available());
        int first = stream.read();
        assertEquals(sampleData[0] & 0xFF, first);
    }

    @Test
    public void testReadAfterExhaustionReturnsMinusOne() {
        // Exhaust the stream
        byte[] buffer = new byte[sampleData.length];
        int read = stream.read(buffer, 0, buffer.length);
        assertEquals(sampleData.length, read);
        // Now at EOF
        assertEquals(-1, stream.read());
        // read(byte[], ...) should also return -1
        assertEquals(-1, stream.read(new byte[1], 0, 1));
    }
}
