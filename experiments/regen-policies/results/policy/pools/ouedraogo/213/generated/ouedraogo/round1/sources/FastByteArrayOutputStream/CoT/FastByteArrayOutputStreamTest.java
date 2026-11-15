package com.cedarsoftware.util;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.*;

public class FastByteArrayOutputStreamTest {

    /* ---------------------------------------------------------------------- */
    /*  List of public methods (for reference)                               */
    /* ---------------------------------------------------------------------- */
    // FastByteArrayOutputStream()
    // FastByteArrayOutputStream(int size)
    // void write(int b)
    // void write(byte[] b, int off, int len)
    // void writeBytes(byte[] b)
    // void reset()
    // byte[] toByteArray()
    // byte[] getBuffer()
    // int size()
    // String toString()
    // void writeTo(OutputStream out) throws IOException
    // void close() throws IOException

    /* ---------------------------------------------------------------------- */
    /*  Basic functionality tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorAndWrite() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(); // default 32 bytes

        // write using write(int)
        out.write(65); // 'A'
        out.write(66); // 'B'

        // write using write(byte[], off, len)
        byte[] extra = new byte[] {67, 68, 69}; // 'C','D','E'
        out.write(extra, 0, extra.length);

        // verify size
        assertEquals(5, out.size());

        // verify content via toByteArray
        byte[] expected = new byte[] {65, 66, 67, 68, 69};
        assertArrayEquals(expected, out.toByteArray());

        // verify toString (default charset, ASCII safe)
        assertEquals("ABCDE", out.toString());
    }

    @Test
    public void testWriteBytesConvenienceMethod() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(4);
        byte[] data = new byte[] {1, 2, 3, 4};
        out.writeBytes(data);
        assertEquals(4, out.size());
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    public void testResetClearsBuffer() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[] {10, 20, 30});
        assertEquals(3, out.size());

        out.reset();
        assertEquals(0, out.size());
        assertArrayEquals(new byte[0], out.toByteArray());
        assertEquals("", out.toString());
    }

    @Test
    public void testToByteArrayReturnsCopy() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[] {5, 6, 7});
        byte[] copy = out.toByteArray();

        // modify the returned array
        copy[0] = 99;
        // original internal buffer must stay unchanged
        assertEquals(5, out.toByteArray()[0]);
    }

    @Test
    public void testGetBufferIsAliasForToByteArray() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[] {8, 9});
        assertArrayEquals(out.toByteArray(), out.getBuffer());
    }

    @Test
    public void testWriteToCopiesDataToOtherStream() throws IOException {
        FastByteArrayOutputStream source = new FastByteArrayOutputStream();
        source.writeBytes("hello".getBytes());

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        source.writeTo(target);

        assertArrayEquals("hello".getBytes(), target.toByteArray());
    }

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.close(); // should be a no‑op and not throw
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case and exception handling tests                               */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeSizeThrows() {
        new FastByteArrayOutputStream(-1);
    }

    @Test
    public void testWriteByteArrayNullThrows() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        try {
            out.write(null, 0, 0);
            fail("Expected IndexOutOfBoundsException for null array");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testWriteByteArrayInvalidParameters() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] data = new byte[5];

        // off < 0
        try {
            out.write(data, -1, 1);
            fail("Expected IndexOutOfBoundsException for off < 0");
        } catch (IndexOutOfBoundsException ignored) {}

        // len < 0
        try {
            out.write(data, 0, -1);
            fail("Expected IndexOutOfBoundsException for len < 0");
        } catch (IndexOutOfBoundsException ignored) {}

        // off > data.length
        try {
            out.write(data, 6, 0);
            fail("Expected IndexOutOfBoundsException for off > length");
        } catch (IndexOutOfBoundsException ignored) {}

        // off + len > data.length
        try {
            out.write(data, 3, 3);
            fail("Expected IndexOutOfBoundsException for off+len > length");
        } catch (IndexOutOfBoundsException ignored) {}
    }

    @Test
    public void testWriteZeroLengthDoesNotChangeSize() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[] {1, 2, 3});
        int sizeBefore = out.size();

        out.write(new byte[] {9, 9, 9}, 1, 0); // zero length
        assertEquals(sizeBefore, out.size());
    }

    @Test
    public void testGrowCapacityWhenWritingLargeData() {
        // start with a very small buffer to force growth
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(2);
        byte[] large = new byte[100];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) i;
        }
        out.write(large, 0, large.length);
        assertEquals(100, out.size());
        assertArrayEquals(large, out.toByteArray());
    }

    @Test
    public void testWriteIntCastsToByte() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(0x123); // low 8 bits = 0x23
        out.write(0xFF);  // -1 as signed byte
        byte[] result = out.toByteArray();
        assertEquals(2, result.length);
        assertEquals(0x23, result[0] & 0xFF);
        assertEquals(0xFF, result[1] & 0xFF);
    }

    @Test
    public void testWriteToPropagatesIOException() {
        FastByteArrayOutputStream src = new FastByteArrayOutputStream();
        src.writeBytes("data".getBytes());

        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("forced failure");
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("forced failure");
            }
        };

        try {
            src.writeTo(failing);
            fail("Expected IOException to be propagated");
        } catch (IOException e) {
            assertEquals("forced failure", e.getMessage());
        }
    }
}
