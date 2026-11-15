/****************************************************************************************
 * JUnit 4 test suite for com.cedarsoftware.util.FastByteArrayOutputStream
 *
 * The tests cover:
 *   • Constructor validation (default size, custom size, negative size)
 *   • Writing single bytes and byte arrays (including automatic growth)
 *   • Edge cases for write(byte[], int, int) – null array, negative offsets,
 *     lengths, and out‑of‑bounds parameters
 *   • writeBytes convenience method
 *   • reset, size, toByteArray, getBuffer, and toString behaviours
 *   • writeTo(OutputStream) correctness
 *   • close() (no‑op) – ensures it does not throw
 *
 * All tests use only the public API of FastByteArrayOutputStream and standard JDK
 * classes; no mocking or subclassing is performed.
 ****************************************************************************************/

package com.cedarsoftware.util;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FastByteArrayOutputStreamTest {

    /*-------------------------------------------------
     *  Constructor Tests
     *-------------------------------------------------*/

    @Test
    public void testDefaultConstructorCreatesNonZeroBuffer() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        // default capacity is 32 – we cannot access it directly,
        // but we can verify that the stream works without throwing.
        fos.write('a');
        assertEquals(1, fos.size());
        assertEquals("a", fos.toString());
    }

    @Test
    public void testCustomSizeConstructor() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream(10);
        assertEquals(0, fos.size());

        // Fill exactly the initial capacity – should not grow.
        byte[] data = new byte[10];
        Arrays.fill(data, (byte) 'x');
        fos.write(data, 0, data.length);
        assertEquals(10, fos.size());
        assertArrayEquals(data, fos.toByteArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeInitialSizeThrows() {
        new FastByteArrayOutputStream(-5);
    }

    /*-------------------------------------------------
     *  write(int) Tests
     *-------------------------------------------------*/

    @Test
    public void testWriteSingleByteAppendsCorrectly() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream(2);
        fos.write('A');
        fos.write('B');
        assertEquals(2, fos.size());
        assertEquals("AB", fos.toString());

        // Verify that the internal buffer grew (initial capacity 2, wrote 2 bytes – no growth)
        fos.write('C'); // should trigger growth
        assertEquals(3, fos.size());
        assertEquals("ABC", fos.toString());
    }

    /*-------------------------------------------------
     *  write(byte[], int, int) Tests
     *-------------------------------------------------*/

    @Test
    public void testWriteByteArrayWithOffsetAndLength() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream(4);
        byte[] src = "HelloWorld".getBytes();
        fos.write(src, 2, 5); // writes "lloWo"
        assertEquals(5, fos.size());
        assertEquals("lloWo", fos.toString());
    }

    @Test
    public void testWriteByteArrayZeroLengthDoesNothing() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        byte[] src = "data".getBytes();
        fos.write(src, 2, 0);
        assertEquals(0, fos.size());
        assertEquals("", fos.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNullArrayThrows() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.write(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeOffsetThrows() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.write(new byte[5], -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeLengthThrows() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.write(new byte[5], 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetBeyondArrayThrows() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.write(new byte[5], 6, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetPlusLengthBeyondArrayThrows() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.write(new byte[5], 3, 3); // 3+3 > 5
    }

    /*-------------------------------------------------
     *  writeBytes(byte[]) Convenience Method
     *-------------------------------------------------*/

    @Test
    public void testWriteBytesWritesEntireArray() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        byte[] data = "full".getBytes();
        fos.writeBytes(data);
        assertEquals(4, fos.size());
        assertArrayEquals(data, fos.toByteArray());
    }

    /*-------------------------------------------------
     *  reset() Tests
     *-------------------------------------------------*/

    @Test
    public void testResetClearsSizeButRetainsBufferContent() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.writeBytes("keep".getBytes());
        assertEquals(4, fos.size());

        fos.reset();
        assertEquals(0, fos.size());
        assertEquals("", fos.toString());

        // After reset we can write again and the new data should start at index 0
        fos.writeBytes("new".getBytes());
        assertEquals(3, fos.size());
        assertEquals("new", fos.toString());
    }

    /*-------------------------------------------------
     *  toByteArray() and getBuffer() Tests
     *-------------------------------------------------*/

    @Test
    public void testToByteArrayReturnsCopy() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.writeBytes("copy".getBytes());

        byte[] copy = fos.toByteArray();
        assertArrayEquals("copy".getBytes(), copy);

        // Mutate the returned array – should NOT affect the stream
        copy[0] = 'X';
        assertEquals("copy", fos.toString());
    }

    @Test
    public void testGetBufferIsBackwardsCompatibleAndReturnsCopy() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.writeBytes("buffer".getBytes());

        byte[] buf = fos.getBuffer();
        assertArrayEquals("buffer".getBytes(), buf);

        // Mutate the returned buffer – should NOT affect the stream
        buf[0] = 'Y';
        assertEquals("buffer", fos.toString());
    }

    /*-------------------------------------------------
     *  size() and toString() Tests
     *-------------------------------------------------*/

    @Test
    public void testSizeReflectsNumberOfWrittenBytes() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        assertEquals(0, fos.size());

        fos.write('a');
        assertEquals(1, fos.size());

        fos.writeBytes("bc".getBytes());
        assertEquals(3, fos.size());
    }

    @Test
    public void testToStringUsesCurrentCountOnly() {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.writeBytes("hello".getBytes());
        fos.write(' ');
        fos.writeBytes("world".getBytes());

        assertEquals("hello world", fos.toString());

        // After reset the string should be empty even though the underlying buffer
        // still contains old data.
        fos.reset();
        assertEquals("", fos.toString());
    }

    /*-------------------------------------------------
     *  writeTo(OutputStream) Tests
     *-------------------------------------------------*/

    @Test
    public void testWriteToWritesExactlyCurrentContent() throws IOException {
        FastByteArrayOutputStream source = new FastByteArrayOutputStream();
        source.writeBytes("payload".getBytes());

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        source.writeTo(dest);

        assertArrayEquals("payload".getBytes(), dest.toByteArray());

        // Ensure that modifying source after writeTo does not affect dest
        source.reset();
        source.writeBytes("new".getBytes());
        assertArrayEquals("payload".getBytes(), dest.toByteArray());
    }

    @Test(expected = IOException.class)
    public void testWriteToPropagatesIOExceptionFromTarget() throws IOException {
        FastByteArrayOutputStream source = new FastByteArrayOutputStream();
        source.writeBytes("data".getBytes());

        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("forced failure");
            }
        };
        source.writeTo(failing);
    }

    /*-------------------------------------------------
     *  close() Tests
     *-------------------------------------------------*/

    @Test
    public void testCloseDoesNothingButDoesNotThrow() throws IOException {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        fos.writeBytes("something".getBytes());
        fos.close(); // should be a no‑op
        // After close we can still use the stream (as per OutputStream contract)
        fos.write('!');
        assertEquals("something!", fos.toString());
    }

    /*-------------------------------------------------
     *  Internal Growth Verification (indirect)
     *-------------------------------------------------*/

    @Test
    public void testAutomaticGrowthWhenWritingBeyondInitialCapacity() {
        // initial capacity = 2, write 5 bytes → should trigger at least two growth steps
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream(2);
        fos.writeBytes("abcde".getBytes());

        assertEquals(5, fos.size());
        assertEquals("abcde", fos.toString());

        // Verify that the internal buffer length is >= 5 (cannot access directly,
        // but the fact that no exception was thrown proves growth succeeded)
        byte[] result = fos.toByteArray();
        assertEquals(5, result.length);
    }
}
