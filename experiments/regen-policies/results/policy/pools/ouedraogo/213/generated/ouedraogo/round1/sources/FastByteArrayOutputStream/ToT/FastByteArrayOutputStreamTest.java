package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FastByteArrayOutputStreamTest {

    private FastByteArrayOutputStream stream;

    @Before
    public void setUp() {
        stream = new FastByteArrayOutputStream();
    }

    @After
    public void tearDown() {
        stream = null;
    }

    /**
     * Test default constructor creates a buffer with initial capacity 32
     * and that size() is zero.
     */
    @Test
    public void testDefaultConstructor() {
        assertEquals(0, stream.size());
        // internal buffer size is not exposed; we verify that writing 32 bytes works without growth
        byte[] data = new byte[32];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        stream.writeBytes(data);
        assertEquals(32, stream.size());
        assertArrayEquals(data, stream.toByteArray());
    }

    /**
     * Test constructor with explicit size and negative size handling.
     */
    @Test
    public void testConstructorWithSize() {
        FastByteArrayOutputStream s = new FastByteArrayOutputStream(10);
        assertEquals(0, s.size());

        // negative size should throw IllegalArgumentException
        try {
            new FastByteArrayOutputStream(-1);
            fail("Expected IllegalArgumentException for negative size");
        } catch (IllegalArgumentException e) {
            assertEquals("Negative initial size: -1", e.getMessage());
        }
    }

    /**
     * Test single‑byte write, buffer growth and size tracking.
     */
    @Test
    public void testWriteInt() {
        // write more than initial capacity to trigger growth
        for (int i = 0; i < 100; i++) {
            stream.write(i);
        }
        assertEquals(100, stream.size());

        byte[] expected = new byte[100];
        for (int i = 0; i < 100; i++) expected[i] = (byte) i;
        assertArrayEquals(expected, stream.toByteArray());
    }

    /**
     * Test write(byte[], off, len) with normal parameters,
     * edge cases (zero length) and out‑of‑bounds checks.
     */
    @Test
    public void testWriteByteArrayRange() {
        byte[] source = new byte[20];
        for (int i = 0; i < source.length; i++) source[i] = (byte) (i + 1);

        // normal write
        stream.write(source, 5, 10);
        assertEquals(10, stream.size());
        assertArrayEquals(Arrays.copyOfRange(source, 5, 15), stream.toByteArray());

        // zero‑length write should change nothing
        stream.write(source, 0, 0);
        assertEquals(10, stream.size());

        // out‑of‑bounds scenarios
        int[][] badParams = {
                {null, 0, 1},
                {source, -1, 5},
                {source, 0, -1},
                {source, source.length + 1, 0},
                {source, 10, source.length},      // off+len exceeds length
                {source, Integer.MAX_VALUE, 1}    // overflow check
        };
        for (int[] p : badParams) {
            try {
                stream.write(p[0] == null ? null : source, p[1], p[2]);
                fail("Expected IndexOutOfBoundsException for parameters: " + Arrays.toString(p));
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                // expected
            }
        }
    }

    /**
     * Test writeBytes convenience method.
     */
    @Test
    public void testWriteBytes() {
        byte[] data = {10, 20, 30, 40};
        stream.writeBytes(data);
        assertEquals(4, stream.size());
        assertArrayEquals(data, stream.toByteArray());
    }

    /**
     * Test reset clears the count but does not shrink the buffer.
     */
    @Test
    public void testReset() {
        stream.writeBytes(new byte[]{1, 2, 3});
        assertEquals(3, stream.size());
        stream.reset();
        assertEquals(0, stream.size());
        // after reset we can write again
        stream.writeBytes(new byte[]{9, 9});
        assertEquals(2, stream.size());
        assertArrayEquals(new byte[]{9, 9}, stream.toByteArray());
    }

    /**
     * Test toByteArray returns a copy, not the internal buffer.
     */
    @Test
    public void testToByteArrayCopy() {
        byte[] data = {5, 6, 7};
        stream.writeBytes(data);
        byte[] copy = stream.toByteArray();
        assertArrayEquals(data, copy);

        // modify the returned array and ensure internal state is unchanged
        copy[0] = 99;
        assertEquals(5, stream.toByteArray()[0]);
    }

    /**
     * Test getBuffer (backwards compatibility) behaves like toByteArray.
     */
    @Test
    public void testGetBuffer() {
        byte[] data = {8, 9};
        stream.writeBytes(data);
        assertArrayEquals(data, stream.getBuffer());
    }

    /**
     * Test toString creates a String from the written bytes using platform default charset.
     */
    @Test
    public void testToString() {
        String text = "Hello, 世界";
        byte[] bytes = text.getBytes(); // default charset
        stream.writeBytes(bytes);
        assertEquals(text, stream.toString());
    }

    /**
     * Test writeTo correctly writes the internal buffer to another OutputStream.
     */
    @Test
    public void testWriteTo() throws IOException {
        byte[] data = {0, 1, 2, 3, 4};
        stream.writeBytes(data);

        // simple OutputStream that records written bytes
        class RecordingOS extends OutputStream {
            private byte[] buf = new byte[0];
            @Override
            public void write(int b) {
                buf = Arrays.copyOf(buf, buf.length + 1);
                buf[buf.length - 1] = (byte) b;
            }

            @Override
            public void write(byte[] b, int off, int len) {
                int old = buf.length;
                buf = Arrays.copyOf(buf, old + len);
                System.arraycopy(b, off, buf, old, len);
            }

            byte[] getBytes() {
                return buf;
            }
        }

        RecordingOS out = new RecordingOS();
        stream.writeTo(out);
        assertArrayEquals(data, out.getBytes());
    }

    /**
     * Test close does nothing and does not affect subsequent operations.
     */
    @Test
    public void testClose() throws IOException {
        stream.writeBytes(new byte[]{1, 2});
        stream.close(); // should be a no‑op
        // after close we can still write
        stream.writeBytes(new byte[]{3});
        assertArrayEquals(new byte[]{1, 2, 3}, stream.toByteArray());
    }
}
