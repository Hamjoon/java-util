package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

public class FastByteArrayOutputStreamTest {

    @Test
    public void testDefaultConstructorInitialCapacity() {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        // default capacity is 32, but we cannot access it directly.
        // Verify that we can write up to 32 bytes without triggering growth.
        byte[] data = new byte[32];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        baos.writeBytes(data);
        assertEquals(32, baos.size());
        // Verify content matches
        assertArrayEquals(data, baos.toByteArray());
    }

    @Test
    public void testConstructorWithNegativeSize() {
        try {
            new FastByteArrayOutputStream(-1);
            fail("Expected IllegalArgumentException for negative size");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Negative initial size"));
        }
    }

    @Test
    public void testWriteSingleByteAndResize() {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream(2);
        baos.write('A');
        baos.write('B');
        // At this point buffer is full; next write should trigger growth
        baos.write('C');
        assertEquals(3, baos.size());
        assertEquals("ABC", baos.toString());
    }

    @Test
    public void testWriteByteArrayWithOffsetAndLength() {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream(4);
        byte[] src = "HelloWorld".getBytes();
        // write "Hello"
        baos.write(src, 0, 5);
        assertEquals(5, baos.size());
        assertEquals("Hello", baos.toString());

        // write "World" using offset
        baos.write(src, 5, 5);
        assertEquals(10, baos.size());
        assertEquals("HelloWorld", baos.toString());
    }

    @Test
    public void testWriteByteArrayInvalidParameters() {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        byte[] data = {1, 2, 3};

        // null array
        try {
            baos.write(null, 0, 1);
            fail("Expected IndexOutOfBoundsException for null array");
        } catch (IndexOutOfBoundsException ignored) {}

        // negative offset
        try {
            baos.write(data, -1, 1);
            fail("Expected IndexOutOfBoundsException for negative offset");
        } catch (IndexOutOfBoundsException ignored) {}

        // negative length
        try {
            baos.write(data, 0, -1);
            fail("Expected IndexOutOfBoundsException for negative length");
        } catch (IndexOutOfBoundsException ignored) {}

        // offset beyond array length
        try {
            baos.write(data, 4, 1);
            fail("Expected IndexOutOfBoundsException for offset beyond length");
        } catch (IndexOutOfBoundsException ignored) {}

        // offset+len exceeds array length
        try {
            baos.write(data, 2, 2);
            fail("Expected IndexOutOfBoundsException for offset+len overflow");
        } catch (IndexOutOfBoundsException ignored) {}

        // overflow check (off+len < 0)
        try {
            baos.write(data, Integer.MAX_VALUE, Integer.MAX_VALUE);
            fail("Expected IndexOutOfBoundsException for integer overflow");
        } catch (IndexOutOfBoundsException ignored) {}
    }

    @Test
    public void testWriteBytesConvenienceMethod() {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        byte[] data = "Test".getBytes();
        baos.writeBytes(data);
        assertEquals(4, baos.size());
        assertArrayEquals(data, baos.toByteArray());
    }

    @Test
    public void testResetClearsBuffer() {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        baos.writeBytes("abc".getBytes());
        assertEquals(3, baos.size());
        baos.reset();
        assertEquals(0, baos.size());
        assertEquals("", baos.toString());

        // after reset we can write again
        baos.write('Z');
        assertEquals(1, baos.size());
        assertEquals("Z", baos.toString());
    }

    @Test
    public void testToByteArrayReturnsCopy() {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        baos.writeBytes("copy".getBytes());
        byte[] arr1 = baos.toByteArray();
        byte[] arr2 = baos.getBuffer(); // also returns a copy
        assertArrayEquals(arr1, arr2);
        // modify returned array should not affect internal state
        arr1[0] = 'X';
        assertEquals('c', baos.toString().charAt(0));
    }

    @Test
    public void testSizeAndToString() {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        assertEquals(0, baos.size());
        assertEquals("", baos.toString());

        baos.write('J');
        baos.write('a');
        baos.write('v');
        baos.write('a');
        assertEquals(4, baos.size());
        assertEquals("Java", baos.toString());
    }

    @Test
    public void testWriteToOtherOutputStream() throws IOException {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        baos.writeBytes("stream".getBytes());

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        baos.writeTo(target);

        assertArrayEquals("stream".getBytes(), target.toByteArray());
    }

    @Test
    public void testCloseDoesNothing() throws IOException {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        baos.writeBytes("data".getBytes());
        baos.close(); // should not throw
        // after close we can still use the stream
        baos.write('X');
        assertEquals("dataX", baos.toString());
    }

    @Test
    public void testGrowthLogicBeyondInitialCapacity() {
        // start with small capacity to force multiple grows
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream(1);
        // write 10 bytes, which will trigger several growth steps
        for (int i = 0; i < 10; i++) {
            baos.write(i);
        }
        assertEquals(10, baos.size());
        byte[] expected = new byte[10];
        for (int i = 0; i < 10; i++) expected[i] = (byte) i;
        assertArrayEquals(expected, baos.toByteArray());
    }
}
