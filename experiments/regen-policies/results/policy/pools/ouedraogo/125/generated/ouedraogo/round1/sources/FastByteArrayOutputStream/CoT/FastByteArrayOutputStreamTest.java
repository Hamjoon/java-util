package com.cedarsoftware.util;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link FastByteArrayOutputStream}.
 *
 * The test class follows the requested steps:
 * 1. List of public methods (see the comment block at the top).
 * 2. Basic tests for each public method.
 * 3. Identification of edge‑cases / exception scenarios.
 * 4. Specific tests that target those edge‑cases.
 * 5. All tests are merged into a single file.
 */
public class FastByteArrayOutputStreamTest {

    /* -------------------------------------------------------------
     * 1. PUBLIC METHODS (extracted from FastByteArrayOutputStream)
     * -------------------------------------------------------------
     *
     * FastByteArrayOutputStream()
     * FastByteArrayOutputStream(int size)
     *
     * void write(int b)
     * void write(byte[] b, int off, int len)
     * void writeBytes(byte[] b)
     *
     * void reset()
     * byte[] toByteArray()
     * byte[] getBuffer()
     * int size()
     * String toString()
     * void writeTo(OutputStream out) throws IOException
     * void close() throws IOException
     *
     * ------------------------------------------------------------- */

    /* -------------------------------------------------------------
     * 2. BASIC FUNCTIONALITY TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testDefaultConstructor() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        assertEquals(0, out.size());
        assertArrayEquals(new byte[0], out.toByteArray());
    }

    @Test
    public void testConstructorWithPositiveSize() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(64);
        assertEquals(0, out.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeSize() {
        new FastByteArrayOutputStream(-1);
    }

    @Test
    public void testWriteIntSingleByte() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write('A');               // 0x41
        assertEquals(1, out.size());
        assertArrayEquals(new byte[]{'A'}, out.toByteArray());
        assertEquals("A", out.toString());
    }

    @Test
    public void testWriteIntMultipleAndGrowth() {
        // start with a tiny buffer to force growth
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(2);
        byte[] data = {0, 1, 2, 3, 4, 5, 6, 7};

        for (byte b : data) {
            out.write(b);
        }

        assertEquals(data.length, out.size());
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    public void testWriteByteArrayPartial() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] src = {10, 20, 30, 40, 50};
        out.write(src, 1, 3); // writes 20,30,40
        assertEquals(3, out.size());
        assertArrayEquals(new byte[]{20, 30, 40}, out.toByteArray());
    }

    @Test
    public void testWriteBytesConvenienceMethod() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] src = {5, 6, 7, 8};
        out.writeBytes(src);
        assertEquals(src.length, out.size());
        assertArrayEquals(src, out.toByteArray());
    }

    @Test
    public void testWriteByteArrayZeroLength() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] src = {1, 2, 3};
        out.write(src, 0, 0); // should be a no‑op
        assertEquals(0, out.size());
        assertArrayEquals(new byte[0], out.toByteArray());
    }

    @Test
    public void testReset() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[]{9, 8, 7});
        assertEquals(3, out.size());

        out.reset();
        assertEquals(0, out.size());
        assertArrayEquals(new byte[0], out.toByteArray());

        // after reset we should be able to write again
        out.write('Z');
        assertEquals(1, out.size());
        assertEquals("Z", out.toString());
    }

    @Test
    public void testToByteArrayReturnsCopy() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[]{1, 2, 3});

        byte[] copy = out.toByteArray();
        assertArrayEquals(new byte[]{1, 2, 3}, copy);

        // mutate the returned array – the internal buffer must stay unchanged
        copy[0] = 99;
        assertArrayEquals(new byte[]{1, 2, 3}, out.toByteArray());
    }

    @Test
    public void testGetBufferReturnsCopy() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[]{4, 5, 6});

        byte[] bufferCopy = out.getBuffer();
        assertArrayEquals(new byte[]{4, 5, 6}, bufferCopy);

        // mutate the copy – internal state must not change
        bufferCopy[2] = 100;
        assertArrayEquals(new byte[]{4, 5, 6}, out.getBuffer());
    }

    @Test
    public void testSize() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        assertEquals(0, out.size());

        out.write('a');
        out.write('b');
        assertEquals(2, out.size());

        out.reset();
        assertEquals(0, out.size());
    }

    @Test
    public void testToStringEncoding() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        String text = "Hello, 世界";
        out.writeBytes(text.getBytes());
        assertEquals(text, out.toString());
    }

    @Test
    public void testWriteTo() throws IOException {
        FastByteArrayOutputStream source = new FastByteArrayOutputStream();
        byte[] payload = {11, 22, 33, 44};
        source.writeBytes(payload);

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        source.writeTo(dest);

        assertArrayEquals(payload, dest.toByteArray());
    }

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write('x');
        out.close(); // should be a no‑op and not throw
    }

    /* -------------------------------------------------------------
     * 3. EDGE‑CASE & EXCEPTION TESTS
     * ------------------------------------------------------------- */

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArray_NullArray() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArray_NegativeOffset() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(new byte[]{1, 2, 3}, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArray_NegativeLength() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(new byte[]{1, 2, 3}, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArray_OffsetBeyondArray() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(new byte[]{1, 2, 3}, 4, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArray_OffsetPlusLengthBeyondArray() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(new byte[]{1, 2, 3}, 2, 2); // 2+2 > 3
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArray_OverflowCalculation() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] data = new byte[Integer.MAX_VALUE - 5];
        // The following will overflow when computing off + len (negative result)
        out.write(data, Integer.MAX_VALUE - 4, 10);
    }

    @Test
    public void testLargeWriteTriggersMultipleGrowths() {
        // start with a very small buffer to ensure many growth steps
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(1);
        int total = 10_000; // large enough to cause several doublings
        byte[] data = new byte[total];
        Arrays.fill(data, (byte) 0x7F);
        out.writeBytes(data);

        assertEquals(total, out.size());
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    public void testWriteZeroLengthDoesNotChangeState() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[]{1, 2, 3});
        out.write(new byte[]{9, 9, 9}, 0, 0); // zero‑length write
        assertEquals(3, out.size());
        assertArrayEquals(new byte[]{1, 2, 3}, out.toByteArray());
    }
}
