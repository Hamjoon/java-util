package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link FastByteArrayOutputStream}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and methods (basic functionality)</li>
 *   <li>Edge‑case and exception scenarios (negative sizes, illegal arguments, buffer growth, etc.)</li>
 *   <li>Behaviour of {@code writeTo} with a throwing {@link OutputStream}</li>
 *   <li>Verification that the internal buffer is not exposed directly (defensive copies)</li>
 * </ul>
 */
public class FastByteArrayOutputStreamTest {

    private FastByteArrayOutputStream stream;

    @Before
    public void setUp() {
        // default constructor uses an initial capacity of 32 bytes
        stream = new FastByteArrayOutputStream();
    }

    /* -------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyStream() {
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
    }

    @Test
    public void testConstructorWithPositiveSize() {
        FastByteArrayOutputStream s = new FastByteArrayOutputStream(10);
        assertEquals(0, s.size());
        // internal capacity is not exposed, we only know that writes work up to the size
        s.write(new byte[10], 0, 10);
        assertEquals(10, s.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeSizeThrows() {
        new FastByteArrayOutputStream(-1);
    }

    /* -------------------------------------------------
     *  write(int) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteIntAddsSingleByte() {
        stream.write(0x41); // 'A'
        assertEquals(1, stream.size());
        assertArrayEquals(new byte[]{0x41}, stream.toByteArray());
    }

    @Test
    public void testWriteIntHandlesNegativeByteValues() {
        stream.write(0xFF); // -1 as signed byte
        assertEquals(1, stream.size());
        assertArrayEquals(new byte[]{(byte) 0xFF}, stream.toByteArray());
    }

    @Test
    public void testWriteIntTriggersBufferGrowth() {
        // Initial capacity is 32; write 40 bytes to force a grow
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

    /* -------------------------------------------------
     *  write(byte[], int, int) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteByteArrayWithOffsetAndLength() {
        byte[] src = "HelloWorld".getBytes();
        stream.write(src, 5, 5); // write "World"
        assertEquals(5, stream.size());
        assertArrayEquals("World".getBytes(), stream.toByteArray());
    }

    @Test
    public void testWriteByteArrayZeroLengthDoesNothing() {
        byte[] src = "abc".getBytes();
        stream.write(src, 1, 0);
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNullArrayThrows() {
        stream.write(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeOffsetThrows() {
        stream.write(new byte[5], -1, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeLengthThrows() {
        stream.write(new byte[5], 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetBeyondArrayThrows() {
        stream.write(new byte[5], 6, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetPlusLengthBeyondArrayThrows() {
        stream.write(new byte[5], 3, 3); // 3+3 > 5
    }

    @Test
    public void testWriteByteArrayTriggersBufferGrowth() {
        byte[] data = new byte[100];
        Arrays.fill(data, (byte) 0x7A);
        stream.write(data, 0, data.length); // forces at least one grow
        assertEquals(100, stream.size());
        assertArrayEquals(data, stream.toByteArray());
    }

    /* -------------------------------------------------
     *  writeBytes(byte[]) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteBytesConvenienceMethod() {
        byte[] src = "quick".getBytes();
        stream.writeBytes(src);
        assertEquals(src.length, stream.size());
        assertArrayEquals(src, stream.toByteArray());
    }

    /* -------------------------------------------------
     *  reset() tests
     * ------------------------------------------------- */

    @Test
    public void testResetClearsStream() {
        stream.writeBytes("data".getBytes());
        assertEquals(4, stream.size());

        stream.reset();
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());

        // After reset we should be able to write again
        stream.writeBytes("new".getBytes());
        assertEquals(3, stream.size());
        assertArrayEquals("new".getBytes(), stream.toByteArray());
    }

    /* -------------------------------------------------
     *  toByteArray() and getBuffer() tests
     * ------------------------------------------------- */

    @Test
    public void testToByteArrayReturnsCopyNotReference() {
        stream.writeBytes("copy".getBytes());
        byte[] arr1 = stream.toByteArray();
        byte[] arr2 = stream.toByteArray();

        assertArrayEquals(arr1, arr2);
        assertNotSame(arr1, arr2); // defensive copy
    }

    @Test
    public void testGetBufferIsBackwardCompatibleCopy() {
        stream.writeBytes("buffer".getBytes());
        byte[] buf = stream.getBuffer();
        assertArrayEquals("buffer".getBytes(), buf);
        assertNotSame(buf, stream.toByteArray()); // also a copy
    }

    /* -------------------------------------------------
     *  size() tests
     * ------------------------------------------------- */

    @Test
    public void testSizeReflectsNumberOfWrittenBytes() {
        assertEquals(0, stream.size());
        stream.writeBytes("abc".getBytes());
        assertEquals(3, stream.size());
        stream.write(0x44); // 'D'
        assertEquals(4, stream.size());
        stream.reset();
        assertEquals(0, stream.size());
    }

    /* -------------------------------------------------
     *  toString() tests
     * ------------------------------------------------- */

    @Test
    public void testToStringReturnsCorrectStringForAscii() {
        stream.writeBytes("Hello".getBytes());
        assertEquals("Hello", stream.toString());
    }

    @Test
    public void testToStringOnEmptyStreamReturnsEmptyString() {
        assertEquals("", stream.toString());
    }

    /* -------------------------------------------------
     *  writeTo(OutputStream) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteToWritesAllBytesToTarget() throws IOException {
        byte[] data = "target".getBytes();
        stream.writeBytes(data);

        CapturingOutputStream out = new CapturingOutputStream();
        stream.writeTo(out);

        assertArrayEquals(data, out.getCaptured());
    }

    @Test(expected = IOException.class)
    public void testWriteToPropagatesIOExceptionFromTarget() throws IOException {
        stream.writeBytes("boom".getBytes());

        OutputStream throwingOut = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("forced");
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("forced");
            }
        };

        stream.writeTo(throwingOut);
    }

    /* -------------------------------------------------
     *  close() tests
     * ------------------------------------------------- */

    @Test
    public void testCloseDoesNotThrowAndLeavesDataIntact() throws IOException {
        stream.writeBytes("still".getBytes());
        stream.close(); // should be a no‑op
        assertEquals("still", stream.toString());
    }

    /* -------------------------------------------------
     *  Helper class used by writeTo tests
     * ------------------------------------------------- */
    private static class CapturingOutputStream extends OutputStream {
        private final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        @Override
        public void write(int b) {
            baos.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            baos.write(b, off, len);
        }

        byte[] getCaptured() {
            return baos.toByteArray();
        }
    }
}
