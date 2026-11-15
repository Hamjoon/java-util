package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FastByteArrayOutputStreamTest {

    private FastByteArrayOutputStream stream;

    @Before
    public void setUp() {
        stream = new FastByteArrayOutputStream();
    }

    /* -------------------------------------------------------------
     *  1.  Public constructors
     * ------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesNonNullStream() {
        FastByteArrayOutputStream s = new FastByteArrayOutputStream();
        assertNotNull(s);
        assertEquals(0, s.size());
    }

    @Test
    public void testConstructorWithPositiveSize() {
        FastByteArrayOutputStream s = new FastByteArrayOutputStream(64);
        assertNotNull(s);
        assertEquals(0, s.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeSizeThrows() {
        new FastByteArrayOutputStream(-1);
    }

    /* -------------------------------------------------------------
     *  2.  write(int b)
     * ------------------------------------------------------------- */

    @Test
    public void testWriteIntAppendsByte() {
        stream.write(0x41); // 'A'
        assertEquals(1, stream.size());
        assertArrayEquals(new byte[]{0x41}, stream.toByteArray());
    }

    @Test
    public void testWriteIntHandlesOverflowByTruncation() {
        stream.write(0x1234); // only low 8 bits (0x34) should be stored
        assertEquals(1, stream.size());
        assertArrayEquals(new byte[]{0x34}, stream.toByteArray());
    }

    @Test
    public void testWriteIntTriggersGrowth() {
        // default capacity is 32; write 40 bytes to force a grow
        for (int i = 0; i < 40; i++) {
            stream.write(i);
        }
        assertEquals(40, stream.size());

        byte[] expected = new byte[40];
        for (int i = 0; i < 40; i++) {
            expected[i] = (byte) i;
        }
        assertArrayEquals(expected, stream.toByteArray());
    }

    /* -------------------------------------------------------------
     *  3.  write(byte[] b, int off, int len)
     * ------------------------------------------------------------- */

    @Test
    public void testWriteArrayWithOffsetAndLength() {
        byte[] src = "HelloWorld".getBytes();
        stream.write(src, 2, 5); // "lloWo"
        assertEquals(5, stream.size());
        assertArrayEquals("lloWo".getBytes(), stream.toByteArray());
    }

    @Test
    public void testWriteArrayFullLength() {
        byte[] src = {1, 2, 3, 4};
        stream.write(src, 0, src.length);
        assertEquals(4, stream.size());
        assertArrayEquals(src, stream.toByteArray());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteArrayNullThrows() {
        stream.write(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteArrayNegativeOffsetThrows() {
        stream.write(new byte[]{1, 2, 3}, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteArrayNegativeLengthThrows() {
        stream.write(new byte[]{1, 2, 3}, 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteArrayOffsetBeyondArrayThrows() {
        stream.write(new byte[]{1, 2, 3}, 4, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteArrayOffsetPlusLengthBeyondArrayThrows() {
        stream.write(new byte[]{1, 2, 3}, 2, 2);
    }

    @Test
    public void testWriteArrayCausesGrowth() {
        byte[] src = new byte[100];
        Arrays.fill(src, (byte) 9);
        stream.write(src, 0, src.length); // default capacity 32 -> growth needed
        assertEquals(100, stream.size());
        byte[] result = stream.toByteArray();
        for (byte b : result) {
            assertEquals(9, b);
        }
    }

    /* -------------------------------------------------------------
     *  4.  writeBytes(byte[] b)
     * ------------------------------------------------------------- */

    @Test
    public void testWriteBytesConvenienceMethod() {
        byte[] data = "abc".getBytes();
        stream.writeBytes(data);
        assertEquals(3, stream.size());
        assertArrayEquals(data, stream.toByteArray());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteBytesWithNullArrayThrows() {
        stream.writeBytes(null);
    }

    /* -------------------------------------------------------------
     *  5.  reset()
     * ------------------------------------------------------------- */

    @Test
    public void testResetClearsContent() {
        stream.writeBytes("test".getBytes());
        assertEquals(4, stream.size());
        stream.reset();
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
    }

    /* -------------------------------------------------------------
     *  6.  toByteArray()
     * ------------------------------------------------------------- */

    @Test
    public void testToByteArrayReturnsCopyNotReference() {
        stream.writeBytes("copy".getBytes());
        byte[] arr1 = stream.toByteArray();
        byte[] arr2 = stream.toByteArray();

        // Ensure they are equal but not the same instance
        assertArrayEquals(arr1, arr2);
        assertNotSame(arr1, arr2);

        // Modifying returned array must not affect the stream
        arr1[0] = 'X';
        assertEquals('c', stream.toByteArray()[0]);
    }

    /* -------------------------------------------------------------
     *  7.  getBuffer() (backwards compatibility)
     * ------------------------------------------------------------- */

    @Test
    public void testGetBufferBehavesLikeToByteArray() {
        stream.writeBytes("buffer".getBytes());
        byte[] buf = stream.getBuffer();
        assertArrayEquals("buffer".getBytes(), buf);
        // Ensure copy semantics
        buf[0] = 'Z';
        assertEquals('b', stream.getBuffer()[0]);
    }

    /* -------------------------------------------------------------
     *  8.  size()
     * ------------------------------------------------------------- */

    @Test
    public void testSizeReflectsNumberOfWrittenBytes() {
        assertEquals(0, stream.size());
        stream.write(1);
        assertEquals(1, stream.size());
        stream.writeBytes(new byte[]{2, 3, 4});
        assertEquals(4, stream.size());
    }

    /* -------------------------------------------------------------
     *  9.  toString()
     * ------------------------------------------------------------- */

    @Test
    public void testToStringUsesDefaultCharsetAndRespectsCount() {
        stream.writeBytes("Hello".getBytes());
        assertEquals("Hello", stream.toString());

        // After reset, toString should be empty
        stream.reset();
        assertEquals("", stream.toString());
    }

    /* -------------------------------------------------------------
     * 10. writeTo(OutputStream out)
     * ------------------------------------------------------------- */

    @Test
    public void testWriteToCopiesContentToAnotherStream() throws IOException {
        byte[] data = "copyTo".getBytes();
        stream.writeBytes(data);

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        stream.writeTo(target);

        assertArrayEquals(data, target.toByteArray());
    }

    @Test
    public void testWriteToWithEmptyStreamWritesNothing() throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        stream.writeTo(target);
        assertEquals(0, target.size());
    }

    /* -------------------------------------------------------------
     * 11. close()
     * ------------------------------------------------------------- */

    @Test
    public void testCloseDoesNotThrowAndDoesNotAlterState() throws IOException {
        stream.writeBytes("close".getBytes());
        stream.close(); // should be a no‑op
        assertEquals(5, stream.size());
        assertArrayEquals("close".getBytes(), stream.toByteArray());
    }
}
