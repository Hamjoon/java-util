package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 tests for {@link FastByteArrayOutputStream}.
 */
public class FastByteArrayOutputStreamTest {

    @Test
    public void testDefaultConstructorInitialCapacity() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        assertEquals(0, out.size());
        // Write a single byte and ensure no exception (capacity should be at least 32)
        out.write('a');
        assertEquals(1, out.size());
        assertEquals("a", out.toString());
    }

    @Test
    public void testConstructorWithNegativeSizeThrows() {
        try {
            new FastByteArrayOutputStream(-1);
            fail("Expected IllegalArgumentException for negative size");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Negative initial size"));
        }
    }

    @Test
    public void testWriteSingleBytesAndAutomaticGrowth() {
        // Start with a tiny buffer to force growth
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(2);
        out.write('x');
        out.write('y');
        // At this point capacity is exactly 2, next write forces grow
        out.write('z');
        assertEquals(3, out.size());
        assertEquals("xyz", out.toString());

        // Verify internal buffer grew (cannot access directly, but we can write more)
        for (int i = 0; i < 100; i++) {
            out.write('a' + (i % 26));
        }
        assertEquals(103, out.size());
        assertTrue(out.toString().startsWith("xyz"));
    }

    @Test
    public void testWriteByteArrayFullAndPartial() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(4);
        byte[] data = "hello".getBytes(); // length 5
        // write with offset/len
        out.write(data, 1, 3); // "ell"
        assertEquals(3, out.size());
        assertEquals("ell", out.toString());

        // write full array via helper
        out.writeBytes(data); // "hello"
        assertEquals(8, out.size());
        assertEquals("ellhello", out.toString());
    }

    @Test
    public void testWriteByteArrayInvalidParameters() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] data = "test".getBytes();

        // null array
        try {
            out.write(null, 0, 1);
            fail("Expected IndexOutOfBoundsException for null array");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // negative offset
        try {
            out.write(data, -1, 1);
            fail("Expected IndexOutOfBoundsException for negative offset");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // negative length
        try {
            out.write(data, 0, -1);
            fail("Expected IndexOutOfBoundsException for negative length");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // offset beyond array length
        try {
            out.write(data, data.length + 1, 0);
            fail("Expected IndexOutOfBoundsException for offset > length");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        // offset+len exceeds array length
        try {
            out.write(data, 2, 3);
            fail("Expected IndexOutOfBoundsException for offset+len > length");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testResetClearsDataButKeepsCapacity() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(10);
        out.writeBytes("abcdef".getBytes());
        assertEquals(6, out.size());
        out.reset();
        assertEquals(0, out.size());
        // After reset we can write again without growing
        out.writeBytes("123".getBytes());
        assertEquals(3, out.size());
        assertEquals("123", out.toString());
    }

    @Test
    public void testToByteArrayAndGetBufferReturnCopies() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        byte[] src = {1, 2, 3, 4};
        out.writeBytes(src);

        byte[] copy1 = out.toByteArray();
        byte[] copy2 = out.getBuffer();

        // Verify content
        assertArrayEquals(src, copy1);
        assertArrayEquals(src, copy2);

        // Mutate returned array should not affect stream
        copy1[0] = 9;
        assertEquals(1, out.toByteArray()[0]);

        // Ensure both methods return independent copies
        copy2[1] = 8;
        assertEquals(2, out.toByteArray()[1]);
    }

    @Test
    public void testSizeAndToString() {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        assertEquals(0, out.size());
        assertEquals("", out.toString());

        out.write('A');
        out.write('B');
        out.write('C');
        assertEquals(3, out.size());
        assertEquals("ABC", out.toString());
    }

    @Test
    public void testWriteToWritesExactBytes() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.writeBytes("data".getBytes());

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        out.writeTo(sink);
        assertArrayEquals("data".getBytes(), sink.toByteArray());

        // Write additional bytes and write again
        out.writeBytes("123".getBytes());
        sink.reset();
        out.writeTo(sink);
        assertArrayEquals("data123".getBytes(), sink.toByteArray());
    }

    @Test
    public void testCloseDoesNothing() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream();
        out.write('x');
        out.close(); // should not throw
        assertEquals(1, out.size());
        assertEquals("x", out.toString());
    }
}
