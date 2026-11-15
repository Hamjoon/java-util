package com.cedarsoftware.util;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FastByteArrayOutputStreamTest {

    @Test
    public void testDefaultConstructorAndWriteInt() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        assertEquals("Initial size must be zero", 0, out.size());

        out.write(0x41); // 'A'
        out.write(0x42); // 'B'
        out.write(0xFF); // -1 as byte

        assertEquals("Size after three writes", 3, out.size());

        byte[] expected = new byte[] {0x41, 0x42, (byte) 0xFF};
        assertArrayEquals("Content must match written bytes", expected, out.toByteArray());

        // toString uses platform default charset; for ASCII these bytes map directly
        assertEquals("AB\u00FF", out.toString());
    }

    @Test
    public void testWriteByteArrayWithOffsetAndLength() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(4);
        byte[] src = new byte[] {10, 20, 30, 40, 50, 60};

        // write a slice (20,30,40)
        out.write(src, 1, 3);
        assertEquals(3, out.size());
        assertArrayEquals(new byte[] {20, 30, 40}, out.toByteArray());

        // write another slice that overlaps the end of the source array
        out.write(src, 4, 2); // 50,60
        assertEquals(5, out.size());
        assertArrayEquals(new byte[] {20, 30, 40, 50, 60}, out.toByteArray());
    }

    @Test
    public void testWriteBytesConvenienceMethod() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] data = new byte[] {1, 2, 3, 4, 5};

        out.writeBytes(data);
        assertEquals(5, out.size());
        assertArrayEquals(data, out.toByteArray());

        // modify original array after write – internal buffer must be a copy
        data[0] = 99;
        assertEquals("Internal buffer must not reflect external changes", 1, out.toByteArray()[0]);
    }

    @Test
    public void testResetClearsSizeButNotInternalBuffer() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[] {9, 8, 7});
        assertEquals(3, out.size());

        out.reset();
        assertEquals("Size after reset should be zero", 0, out.size());
        assertArrayEquals("toByteArray after reset must be empty", new byte[0], out.toByteArray());

        // after reset we can write again and it should start from the beginning
        out.write(55);
        assertEquals(1, out.size());
        assertArrayEquals(new byte[] {55}, out.toByteArray());
    }

    @Test
    public void testToByteArrayReturnsCopy() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[] {5, 6, 7});

        byte[] copy = out.toByteArray();
        assertArrayEquals(new byte[] {5, 6, 7}, copy);

        // mutate the returned array – original internal buffer must stay unchanged
        copy[0] = 99;
        assertEquals("Internal buffer must remain unchanged after external mutation",
                5, out.toByteArray()[0]);
    }

    @Test
    public void testGetBufferReturnsCopy() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[] {11, 12, 13});

        byte[] bufCopy = out.getBuffer();
        assertArrayEquals(new byte[] {11, 12, 13}, bufCopy);

        // mutate the returned array – internal state must not be affected
        bufCopy[1] = 88;
        assertEquals("Internal buffer must remain unchanged after getBuffer mutation",
                12, out.getBuffer()[1]);
    }

    @Test
    public void testWriteToWritesAllBytes() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] data = new byte[50];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        out.writeBytes(data);

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        out.writeTo(sink);
        assertArrayEquals("writeTo must copy exactly the same bytes", data, sink.toByteArray());
    }

    @Test
    public void testCloseDoesNothing() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes(new byte[] {1, 2, 3});
        out.close(); // should not throw

        // after close we should still be able to read content
        assertEquals(3, out.size());
        assertArrayEquals(new byte[] {1, 2, 3}, out.toByteArray());
    }

    @Test
    public void testGrowthBeyondInitialCapacity() {
        // Default capacity is 32. Write 100 bytes to force at least two grows.
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(8);
        byte[] big = new byte[100];
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) (i % 128);
        }
        out.writeBytes(big);
        assertEquals(100, out.size());
        assertArrayEquals(big, out.toByteArray());

        // Internal buffer length is not directly accessible, but we can verify that no
        // IndexOutOfBoundsException was thrown during the write, which proves growth works.
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeSizeThrows() {
        new FastByteArrayOutputStream(-5);
    }

    @Test
    public void testWriteByteArrayInvalidArguments() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();

        // null array
        try {
            out.write(null, 0, 1);
            fail("Expected IndexOutOfBoundsException for null array");
        } catch (IndexOutOfBoundsException ignored) {}

        // negative offset
        try {
            out.write(new byte[10], -1, 5);
            fail("Expected IndexOutOfBoundsException for negative offset");
        } catch (IndexOutOfBoundsException ignored) {}

        // negative length
        try {
            out.write(new byte[10], 0, -2);
            fail("Expected IndexOutOfBoundsException for negative length");
        } catch (IndexOutOfBoundsException ignored) {}

        // offset greater than array length
        try {
            out.write(new byte[5], 6, 1);
            fail("Expected IndexOutOfBoundsException for offset > array length");
        } catch (IndexOutOfBoundsException ignored) {}

        // offset+len exceeds array length
        try {
            out.write(new byte[5], 3, 3);
            fail("Expected IndexOutOfBoundsException for offset+len > array length");
        } catch (IndexOutOfBoundsException ignored) {}

        // overflow (off+len < 0) – use large values to cause overflow
        try {
            out.write(new byte[10], Integer.MAX_VALUE, Integer.MAX_VALUE);
            fail("Expected IndexOutOfBoundsException for overflow condition");
        } catch (IndexOutOfBoundsException ignored) {}
    }

    @Test
    public void testToStringWithEmptyStream() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        assertEquals("Empty stream should return empty string", "", out.toString());
    }

    @Test
    public void testMultipleWritesAndSizeConsistency() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write(65); // 'A'
        out.writeBytes(new byte[] {66, 67}); // 'B','C'
        out.write(new byte[] {68, 69, 70}, 1, 2); // 'E','F'
        assertEquals(6, out.size());

        byte[] expected = new byte[] {65, 66, 67, 69, 70};
        // note: the third write writes bytes at positions 1 and 2 of the source array,
        // which are 69 and 70.
        assertArrayEquals(expected, out.toByteArray());
    }
}
