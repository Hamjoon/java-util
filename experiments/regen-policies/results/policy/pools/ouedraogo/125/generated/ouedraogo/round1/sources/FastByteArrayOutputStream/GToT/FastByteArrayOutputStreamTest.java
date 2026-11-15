package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Comprehensive JUnit‑4 test suite for {@link FastByteArrayOutputStream}.
 *
 * The tests are organised by the public API of the class:
 * <ul>
 *   <li>construction and illegal arguments</li>
 *   <li>write operations (single byte, array, writeBytes)</li>
 *   <li>reset, size, toByteArray, getBuffer and toString</li>
 *   <li>writeTo and close</li>
 * </ul>
 *
 * Edge‑cases such as zero‑length writes, boundary offsets, buffer growth,
 * and the immutability of the returned byte arrays are explicitly verified.
 */
public class FastByteArrayOutputStreamTest
{
    private FastByteArrayOutputStream fos;

    @Before
    public void setUp()
    {
        // default constructor uses an initial capacity of 32 bytes
        fos = new FastByteArrayOutputStream();
    }

    /* -------------------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyStream()
    {
        assertEquals(0, fos.size());
        assertArrayEquals(new byte[0], fos.toByteArray());
    }

    @Test
    public void testConstructorWithPositiveSize()
    {
        FastByteArrayOutputStream custom = new FastByteArrayOutputStream(10);
        assertEquals(0, custom.size());
        // internal buffer size is not exposed, but we can write 10 bytes without growth
        byte[] ten = new byte[10];
        custom.writeBytes(ten);
        assertEquals(10, custom.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeSizeThrows()
    {
        new FastByteArrayOutputStream(-1);
    }

    /* -------------------------------------------------------------
     *  write(int) tests
     * ------------------------------------------------------------- */

    @Test
    public void testWriteSingleByte()
    {
        fos.write(0x41); // 'A'
        assertEquals(1, fos.size());
        assertArrayEquals(new byte[] { 0x41 }, fos.toByteArray());
    }

    @Test
    public void testWriteSingleByteCausesGrowth()
    {
        // write more than default capacity (32) to trigger internal growth
        for (int i = 0; i < 40; i++) {
            fos.write(i);
        }
        assertEquals(40, fos.size());

        byte[] expected = new byte[40];
        for (int i = 0; i < 40; i++) {
            expected[i] = (byte) i;
        }
        assertArrayEquals(expected, fos.toByteArray());
    }

    /* -------------------------------------------------------------
     *  write(byte[], int, int) tests
     * ------------------------------------------------------------- */

    @Test
    public void testWriteByteArrayFull()
    {
        byte[] data = {1, 2, 3, 4, 5};
        fos.write(data, 0, data.length);
        assertEquals(data.length, fos.size());
        assertArrayEquals(data, fos.toByteArray());
    }

    @Test
    public void testWriteByteArrayPartial()
    {
        byte[] data = {10, 20, 30, 40, 50};
        fos.write(data, 1, 3); // writes 20,30,40
        assertEquals(3, fos.size());
        assertArrayEquals(new byte[]{20,30,40}, fos.toByteArray());
    }

    @Test
    public void testWriteByteArrayZeroLength()
    {
        byte[] data = {9,8,7};
        fos.write(data, 0, 0);
        assertEquals(0, fos.size());
        assertArrayEquals(new byte[0], fos.toByteArray());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeOffset()
    {
        fos.write(new byte[]{1,2,3}, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeLength()
    {
        fos.write(new byte[]{1,2,3}, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetBeyondArray()
    {
        fos.write(new byte[]{1,2,3}, 4, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetPlusLengthBeyondArray()
    {
        fos.write(new byte[]{1,2,3}, 2, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNullArray()
    {
        fos.write(null, 0, 1);
    }

    /* -------------------------------------------------------------
     *  writeBytes(byte[]) tests (delegates to write(byte[],int,int))
     * ------------------------------------------------------------- */

    @Test
    public void testWriteBytes()
    {
        byte[] payload = {5, 6, 7};
        fos.writeBytes(payload);
        assertEquals(3, fos.size());
        assertArrayEquals(payload, fos.toByteArray());
    }

    @Test
    public void testWriteBytesEmptyArray()
    {
        fos.writeBytes(new byte[0]);
        assertEquals(0, fos.size());
        assertArrayEquals(new byte[0], fos.toByteArray());
    }

    /* -------------------------------------------------------------
     *  reset(), size(), toByteArray(), getBuffer(), toString()
     * ------------------------------------------------------------- */

    @Test
    public void testResetClearsContent()
    {
        fos.writeBytes(new byte[]{1,2,3});
        assertEquals(3, fos.size());

        fos.reset();
        assertEquals(0, fos.size());
        assertArrayEquals(new byte[0], fos.toByteArray());

        // after reset we should be able to write again
        fos.write(0x55);
        assertEquals(1, fos.size());
        assertArrayEquals(new byte[]{0x55}, fos.toByteArray());
    }

    @Test
    public void testToByteArrayReturnsCopy()
    {
        fos.writeBytes(new byte[]{9,8,7});
        byte[] copy = fos.toByteArray();

        // Mutate the returned array – internal buffer must stay unchanged
        copy[0] = 0;
        assertEquals(9, fos.toByteArray()[0]);
    }

    @Test
    public void testGetBufferReturnsCopy()
    {
        fos.writeBytes(new byte[]{3,2,1});
        byte[] copy = fos.getBuffer();

        copy[1] = 0;
        assertEquals(2, fos.getBuffer()[1]);
    }

    @Test
    public void testSizeReflectsWrittenBytes()
    {
        assertEquals(0, fos.size());
        fos.write(0x01);
        assertEquals(1, fos.size());
        fos.writeBytes(new byte[]{0x02,0x03});
        assertEquals(3, fos.size());
    }

    @Test
    public void testToStringUsesCurrentContent()
    {
        String text = "Hello, World!";
        fos.writeBytes(text.getBytes());
        assertEquals(text, fos.toString());
    }

    @Test
    public void testToStringAfterResetIsEmpty()
    {
        fos.writeBytes("abc".getBytes());
        fos.reset();
        assertEquals("", fos.toString());
    }

    /* -------------------------------------------------------------
     *  writeTo(OutputStream) and close()
     * ------------------------------------------------------------- */

    @Test
    public void testWriteToWritesExactBytes() throws IOException
    {
        byte[] payload = {11,22,33,44};
        fos.writeBytes(payload);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fos.writeTo(baos);

        assertArrayEquals(payload, baos.toByteArray());
    }

    @Test
    public void testWriteToWithEmptyStreamWritesNothing() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fos.writeTo(baos);
        assertEquals(0, baos.size());
    }

    @Test
    public void testCloseDoesNotThrow()
    throws IOException
    {
        // close is a no‑op; just ensure it can be called without exception
        fos.close();
    }
}
