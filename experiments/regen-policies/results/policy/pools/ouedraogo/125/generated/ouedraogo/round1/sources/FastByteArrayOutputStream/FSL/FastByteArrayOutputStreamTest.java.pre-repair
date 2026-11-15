package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link FastByteArrayOutputStream}.
 * Covers constructors, write methods, capacity growth, reset, conversion helpers,
 * and writeTo behaviour, including error handling.
 */
public class FastByteArrayOutputStreamTest {

    /* -----------------------  Constructors  ----------------------- */

    @Test
    public void defaultConstructorCreatesNonZeroBuffer() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        // default size is 32, we only need to verify that we can write up to that size without growing
        for (int i = 0; i < 32; i++) {
            out.write(i);
        }
        assertEquals(32, out.size());
    }

    @Test
    public void sizedConstructorAcceptsZeroAndPositiveSizes() {
        FastByteArrayOutputStream outZero = new FastByteArrayOutputStream(0);
        assertEquals(0, outZero.size());

        FastByteArrayOutputStream outFive = new FastByteArrayOutputStream(5);
        for (int i = 0; i < 5; i++) {
            outFive.write(i);
        }
        assertEquals(5, outFive.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void sizedConstructorRejectsNegativeSize() {
        new FastByteArrayOutputStream(-1);
    }

    /* -----------------------  write(int)  ----------------------- */

    @Test
    public void writeIntAppendsSingleByte() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(2);
        out.write(0x41); // 'A'
        out.write(0x42); // 'B'
        assertEquals(2, out.size());
        assertArrayEquals(new byte[]{0x41, 0x42}, out.toByteArray());
    }

    @Test
    public void writeIntTriggersGrowthWhenCapacityExceeded() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(2);
        // write 5 bytes, forcing at least two growth steps (2 -> 4 -> 5)
        for (int i = 0; i < 5; i++) {
            out.write(i);
        }
        assertEquals(5, out.size());
        byte[] expected = new byte[]{0, 1, 2, 3, 4};
        assertArrayEquals(expected, out.toByteArray());
    }

    @Test
    public void writeIntHandlesNegativeIntValuesCorrectly() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(0xFF); // -1 as a byte
        assertEquals(1, out.size());
        assertEquals((byte) 0xFF, out.toByteArray()[0]);
    }

    /* -----------------------  write(byte[],off,len)  ----------------------- */

    @Test
    public void writeArrayWithOffsetAndLengthCopiesCorrectSlice() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(4);
        byte[] src = new byte[]{10, 20, 30, 40, 50};
        out.write(src, 1, 3); // writes 20,30,40
        assertEquals(3, out.size());
        assertArrayEquals(new byte[]{20, 30, 40}, out.toByteArray());
    }

    @Test
    public void writeArrayZeroLengthLeavesStreamUnchanged() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] data = {1, 2, 3};
        out.write(data, 0, 0);
        assertEquals(0, out.size());
        assertArrayEquals(new byte[0], out.toByteArray());
    }

    @Test
    public void writeArrayTriggersGrowthWhenNecessary() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(2);
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        out.write(data, 0, data.length); // should grow beyond initial 2 bytes
        assertEquals(10, out.size());
        assertArrayEquals(data, out.toByteArray());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writeArrayRejectsNullArray() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writeArrayRejectsNegativeOffset() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(new byte[]{1, 2, 3}, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writeArrayRejectsNegativeLength() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(new byte[]{1, 2, 3}, 0, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writeArrayRejectsOffsetBeyondArray() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(new byte[]{1, 2, 3}, 4, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writeArrayRejectsOffsetPlusLengthBeyondArray() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(new byte[]{1, 2, 3}, 2, 2); // 2+2 = 4 > 3
    }

    /* -----------------------  writeBytes(byte[])  ----------------------- */

    @Test
    public void writeBytesConvenienceWritesWholeArray() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] data = {9, 8, 7};
        out.writeBytes(data);
        assertEquals(3, out.size());
        assertArrayEquals(data, out.toByteArray());
    }

    /* -----------------------  reset()  ----------------------- */

    @Test
    public void resetClearsBufferButRetainsCapacity() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(4);
        out.writeBytes(new byte[]{1, 2, 3, 4});
        assertEquals(4, out.size());

        out.reset();
        assertEquals(0, out.size());
        // after reset we can write again up to the original capacity without growth
        out.writeBytes(new byte[]{5, 6});
        assertEquals(2, out.size());
        assertArrayEquals(new byte[]{5, 6}, out.toByteArray());
    }

    /* -----------------------  toByteArray()  ----------------------- */

    @Test
    public void toByteArrayReturnsCopyNotReference() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[]{1, 2, 3});
        byte[] copy = out.toByteArray();
        // modify copy and verify internal buffer unchanged
        copy[0] = 9;
        assertArrayEquals(new byte[]{1, 2, 3}, out.toByteArray());
    }

    /* -----------------------  getBuffer() (backwards compatibility)  ----------------------- */

    @Test
    public void getBufferReturnsCopyIdenticalToToByteArray() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[]{11, 22, 33});
        assertArrayEquals(out.toByteArray(), out.getBuffer());
    }

    @Test
    public void getBufferModificationDoesNotAffectStream() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[]{5, 6, 7});
        byte[] buf = out.getBuffer();
        buf[1] = 99;
        assertArrayEquals(new byte[]{5, 6, 7}, out.toByteArray());
    }

    /* -----------------------  size()  ----------------------- */

    @Test
    public void sizeReflectsNumberOfWrittenBytes() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        assertEquals(0, out.size());
        out.write(1);
        assertEquals(1, out.size());
        out.writeBytes(new byte[]{2, 3, 4});
        assertEquals(4, out.size());
    }

    /* -----------------------  toString()  ----------------------- */

    @Test
    public void toStringCreatesStringFromWrittenBytesUsingDefaultCharset() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        String text = "Hello, 世界";
        out.writeBytes(text.getBytes()); // default charset round‑trip
        assertEquals(text, out.toString());
    }

    @Test
    public void toStringOnEmptyStreamReturnsEmptyString() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        assertEquals("", out.toString());
    }

    /* -----------------------  writeTo(OutputStream)  ----------------------- */

    @Test
    public void writeToCopiesExactlyCountBytesToTarget() throws IOException {
        FastByteArrayOutputStream src = new FastByteArrayOutputStream();
        src.writeBytes(new byte[]{10, 20, 30, 40});

        // custom OutputStream that records what is written
        class RecordingOutputStream extends OutputStream {
            private byte[] data = new byte[0];

            @Override
            public void write(int b) {
                data = Arrays.copyOf(data, data.length + 1);
                data[data.length - 1] = (byte) b;
            }

            @Override
            public void write(byte[] b, int off, int len) {
                int old = data.length;
                data = Arrays.copyOf(data, old + len);
                System.arraycopy(b, off, data, old, len);
            }

            byte[] getData() {
                return data;
            }
        }

        RecordingOutputStream rec = new RecordingOutputStream();
        src.writeTo(rec);
        assertArrayEquals(new byte[]{10, 20, 30, 40}, rec.getData());
    }

    @Test(expected = IOException.class)
    public void writeToPropagatesIOExceptionFromTarget() throws IOException {
        FastByteArrayOutputStream src = new FastByteArrayOutputStream();
        src.writeBytes(new byte[]{1, 2, 3});

        OutputStream bad = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("forced");
            }
        };
        src.writeTo(bad); // should throw
    }

    /* -----------------------  close()  ----------------------- */

    @Test
    public void closeDoesNotThrowAndLeavesDataIntact() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[]{42});
        out.close(); // no exception expected
        assertEquals(1, out.size());
        assertArrayEquals(new byte[]{42}, out.toByteArray());
    }
}
