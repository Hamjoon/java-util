package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.IOException;

/**
 * JUnit 4 test suite for {@link FastByteArrayInputStream}.
 *
 * The test suite is generated in several steps:
 * 1. List all public methods with signatures.
 * 2. Provide a basic test for each method.
 * 3. Identify edge‑case / exception scenarios.
 * 4. Add dedicated tests for those scenarios.
 * 5. Combine everything into one test class.
 */
public class FastByteArrayInputStreamTest {

    /* -------------------------------------------------
     * 1. Public method list (extracted from the source)
     * -------------------------------------------------
     *
     * public FastByteArrayInputStream(byte[] buf)
     * public int read()
     * public int read(byte[] b, int off, int len)
     * public long skip(long n)
     * public int available()
     * public void mark(int readLimit)
     * public void reset()
     * public boolean markSupported()
     * public void close() throws IOException
     *
     * -------------------------------------------------
     */

    private byte[] data;
    private FastByteArrayInputStream stream;

    @Before
    public void setUp() {
        data = new byte[] { 10, 20, 30, 40, 50 };
        stream = new FastByteArrayInputStream(data);
    }

    @After
    public void tearDown() throws IOException {
        stream.close();
    }

    /* -------------------------------------------------
     * 2. Basic functional tests for each public method
     * -------------------------------------------------
     */

    @Test
    public void testConstructorCreatesStreamWithCorrectLength() {
        assertEquals("available() should equal buffer length after construction",
                data.length, stream.available());
    }

    @Test
    public void testReadSingleByte() {
        int first = stream.read();
        assertEquals(10, first);
        assertEquals(data.length - 1, stream.available());
    }

    @Test
    public void testReadIntoArrayFull() {
        byte[] target = new byte[5];
        int read = stream.read(target, 0, target.length);
        assertEquals(5, read);
        assertArrayEquals(data, target);
        assertEquals(0, stream.available());
    }

    @Test
    public void testReadIntoArrayPartial() {
        byte[] target = new byte[10];
        int read = stream.read(target, 2, 3); // read first 3 bytes into target[2..4]
        assertEquals(3, read);
        assertEquals(10, target[2]);
        assertEquals(20, target[3]);
        assertEquals(30, target[4]);
        assertEquals(data.length - 3, stream.available());
    }

    @Test
    public void testSkipWithinBounds() {
        long skipped = stream.skip(2);
        assertEquals(2L, skipped);
        assertEquals(10, stream.read()); // should now read the third element (30)
    }

    @Test
    public void testAvailableAfterOperations() {
        assertEquals(5, stream.available());
        stream.read();
        assertEquals(4, stream.available());
        stream.skip(2);
        assertEquals(1, stream.available());
    }

    @Test
    public void testMarkAndReset() {
        stream.read(); // consume one byte (10)
        stream.mark(0);
        stream.read(); // consume second byte (20)
        assertEquals(30, stream.read()); // third byte
        stream.reset(); // should go back to after first byte
        assertEquals(20, stream.read()); // should read 20 again
    }

    @Test
    public void testMarkSupported() {
        assertTrue(stream.markSupported());
    }

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        stream.close(); // should be a no‑op and not throw
    }

    /* -------------------------------------------------
     * 3. Edge‑case / exception handling scenarios
     * -------------------------------------------------
     *
     * - Constructor with empty array.
     * - read() after end of stream returns -1.
     * - read(byte[], …) with null buffer -> NullPointerException.
     * - read(byte[], …) with negative off/len or len > buffer.length-off -> IndexOutOfBoundsException.
     * - read(byte[], …) when stream is exhausted -> returns -1.
     * - read(byte[], …) with len == 0 -> returns 0.
     * - skip(negative) -> returns 0 and does not move position.
     * - skip(large) -> skips only remaining bytes.
     * - reset() before any mark -> should reset to initial position (mark defaults to 0).
     * - available() on empty stream returns 0.
     */

    @Test
    public void testConstructorWithEmptyArray() {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        assertEquals(0, empty.available());
        assertEquals(-1, empty.read());
    }

    @Test
    public void testReadBeyondEndReturnsMinusOne() {
        // consume all bytes
        for (int i = 0; i < data.length; i++) {
            assertNotEquals(-1, stream.read());
        }
        // now at EOF
        assertEquals(-1, stream.read());
        assertEquals(-1, stream.read(new byte[1], 0, 1));
    }

    @Test(expected = NullPointerException.class)
    public void testReadWithNullBufferThrowsNPE() {
        stream.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadWithNegativeOffsetThrowsIOOBE() {
        stream.read(new byte[5], -1, 3);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadWithNegativeLengthThrowsIOOBE() {
        stream.read(new byte[5], 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadWithLengthExceedingBufferThrowsIOOBE() {
        stream.read(new byte[5], 2, 4); // 2 + 4 > 5
    }

    @Test
    public void testReadZeroLengthReturnsZero() {
        byte[] target = new byte[5];
        int read = stream.read(target, 0, 0);
        assertEquals(0, read);
        // Position must not have moved
        assertEquals(data.length, stream.available());
    }

    @Test
    public void testSkipNegativeReturnsZeroAndNoMovement() {
        long skipped = stream.skip(-5L);
        assertEquals(0L, skipped);
        assertEquals(data.length, stream.available());
    }

    @Test
    public void testSkipBeyondEndSkipsOnlyRemaining() {
        long skipped = stream.skip(100L);
        assertEquals(data.length, skipped);
        assertEquals(0, stream.available());
        assertEquals(-1, stream.read());
    }

    @Test
    public void testResetWithoutPriorMarkResetsToStart() {
        stream.read(); // consume one byte
        stream.reset(); // mark defaults to 0
        assertEquals(10, stream.read()); // should read first byte again
    }

    @Test
    public void testAvailableOnEmptyStream() {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        assertEquals(0, empty.available());
    }

    /* -------------------------------------------------
     * 4. Combined comprehensive test cases
     * -------------------------------------------------
     *
     * The tests above together cover:
     *   * normal operation of each public method,
     *   * boundary conditions (empty buffer, zero‑length reads, EOF),
     *   * exceptional paths (null arguments, illegal indices),
     *   * state‑changing methods (skip, mark, reset).
     *
     * No further amalgamated tests are required because each
     * scenario is isolated and deterministic.
     */
}
