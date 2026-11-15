package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link FastByteArrayInputStream}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal usage of each public method</li>
 *   <li>boundary conditions (empty buffer, end‑of‑stream, zero‑length reads, etc.)</li>
 *   <li>invalid arguments that should trigger the documented exceptions</li>
 *   <li>behaviour of mark / reset and skip</li>
 * </ul>
 *
 * No mocking is used – all objects are created directly from the class under test.
 */
public class FastByteArrayInputStreamTest {

    private byte[] sampleData;

    @Before
    public void setUp() {
        // a deterministic data set that makes it easy to assert values
        sampleData = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    }

    /* -------------------------------------------------
     *  Constructor & read()
     * ------------------------------------------------- */

    @Test
    public void testConstructorAndSingleByteRead() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        // read each byte and compare with the source array
        for (int i = 0; i < sampleData.length; i++) {
            int read = in.read();
            assertEquals("Byte at position " + i + " differs", sampleData[i] & 0xFF, read);
        }
        // after the last byte we must get -1 (EOF)
        assertEquals(-1, in.read());
        // subsequent reads must continue to return -1
        assertEquals(-1, in.read());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullBufferThrowsNPE() {
        // The constructor dereferences the argument to obtain its length,
        // therefore a null argument triggers NPE immediately.
        new FastByteArrayInputStream(null);
    }

    /* -------------------------------------------------
     *  read(byte[], int, int)
     * ------------------------------------------------- */

    @Test
    public void testReadIntoArrayNormal() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] target = new byte[20];
        Arrays.fill(target, (byte) -1); // sentinel value

        int bytesRead = in.read(target, 5, 5); // read 5 bytes into target[5..9]
        assertEquals(5, bytesRead);
        // verify the copied segment
        for (int i = 0; i < 5; i++) {
            assertEquals(sampleData[i], target[5 + i]);
        }
        // the rest of the target array must stay untouched (-1)
        for (int i = 0; i < 5; i++) {
            assertEquals(-1, target[i]);
        }
        for (int i = 10; i < target.length; i++) {
            assertEquals(-1, target[i]);
        }
    }

    @Test
    public void testReadIntoArrayWithLenGreaterThanAvailable() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        // advance the stream by 8 bytes
        in.skip(8);
        byte[] target = new byte[4];
        int bytesRead = in.read(target, 0, 4); // only 2 bytes remain
        assertEquals(2, bytesRead);
        assertArrayEquals(new byte[]{8, 9}, Arrays.copyOf(target, 2));
    }

    @Test
    public void testReadIntoArrayZeroLengthReturnsZero() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] target = new byte[5];
        int bytesRead = in.read(target, 2, 0);
        assertEquals(0, bytesRead);
        // buffer must stay unchanged
        for (byte b : target) {
            assertEquals(0, b); // default zero value
        }
    }

    @Test(expected = NullPointerException.class)
    public void testReadIntoArrayNullDestinationThrowsNPE() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        in.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayNegativeOffsetThrowsIOOBE() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] target = new byte[5];
        in.read(target, -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayNegativeLengthThrowsIOOBE() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] target = new byte[5];
        in.read(target, 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadIntoArrayLengthBeyondArrayThrowsIOOBE() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        byte[] target = new byte[5];
        // off=3, len=3 would require index 6 which is out of bounds
        in.read(target, 3, 3);
    }

    @Test
    public void testReadIntoArrayAfterEofReturnsMinusOne() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        // consume everything
        while (in.read() != -1) { /* empty */ }
        byte[] target = new byte[5];
        int result = in.read(target, 0, 5);
        assertEquals(-1, result);
    }

    /* -------------------------------------------------
     *  skip(long)
     * ------------------------------------------------- */

    @Test
    public void testSkipWithinBounds() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        long skipped = in.skip(4);
        assertEquals(4L, skipped);
        assertEquals(4, in.available());
        // next byte should be the original 4th index value (i.e., 4)
        assertEquals(4, in.read());
    }

    @Test
    public void testSkipBeyondEndReturnsRemaining() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        long skipped = in.skip(100);
        assertEquals(sampleData.length, skipped);
        assertEquals(0, in.available());
        assertEquals(-1, in.read());
    }

    @Test
    public void testSkipNegativeReturnsZeroAndDoesNotMovePointer() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        long skipped = in.skip(-5);
        assertEquals(0L, skipped);
        assertEquals(sampleData.length, in.available());
        assertEquals(0, in.read()); // first byte unchanged
    }

    /* -------------------------------------------------
     *  available()
     * ------------------------------------------------- */

    @Test
    public void testAvailableReflectsRemainingBytes() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        assertEquals(sampleData.length, in.available());
        in.read(); // consume one byte
        assertEquals(sampleData.length - 1, in.available());
        in.skip(3);
        assertEquals(sampleData.length - 4, in.available());
    }

    /* -------------------------------------------------
     *  mark / reset / markSupported
     * ------------------------------------------------- */

    @Test
    public void testMarkSupportedAlwaysTrue() {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        assertTrue(in.markSupported());
    }

    @Test
    public void testMarkAndResetRestorePosition() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        // read first three bytes
        assertEquals(0, in.read());
        assertEquals(1, in.read());
        assertEquals(2, in.read());

        // set a mark here (position = 3)
        in.mark(0);
        // read a couple more bytes
        assertEquals(3, in.read());
        assertEquals(4, in.read());

        // reset should bring us back to the mark (position = 3)
        in.reset();
        assertEquals(3, in.read()); // same byte as before reset
        assertEquals(4, in.read()); // and the following one
    }

    @Test
    public void testResetWithoutPriorMarkReturnsToStart() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        // default mark is 0 (set in field initialization)
        in.read(); // advance one byte
        in.reset(); // should go back to position 0
        assertEquals(0, in.read());
    }

    /* -------------------------------------------------
     *  close()
     * ------------------------------------------------- */

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        FastByteArrayInputStream in = new FastByteArrayInputStream(sampleData);
        // close is a no‑op but must not throw an exception
        in.close();
        // after close the stream should still behave as normal (spec not explicit,
        // but the implementation does nothing)
        assertEquals(0, in.read());
    }
}
