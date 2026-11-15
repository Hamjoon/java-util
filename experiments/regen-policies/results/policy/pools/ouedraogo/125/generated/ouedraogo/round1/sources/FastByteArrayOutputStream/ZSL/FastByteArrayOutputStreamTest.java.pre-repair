package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * JUnit4 test suite for {@link FastByteArrayOutputStream}.
 * Covers constructors, all write overloads, buffer accessors,
 * reset, size, toString, writeTo and close behavior as well as
 * error handling for illegal arguments.
 */
public class FastByteArrayOutputStreamTest {

    private FastByteArrayOutputStream stream;

    @Before
    public void setUp() {
        // default constructor uses an initial buffer of 32 bytes
        stream = new FastByteArrayOutputStream();
    }

    @After
    public void tearDown() {
        stream = null;
    }

    // -----------------------------------------------------------------
    // Constructor tests
    // -----------------------------------------------------------------
    @Test
    public void testDefaultConstructorCreatesEmptyStream() {
        assertEquals("Initial size should be zero", 0, stream.size());
        assertArrayEquals("toByteArray should be empty", new byte[0], stream.toByteArray());
    }

    @Test
    public void testConstructorWithPositiveSize() {
        FastByteArrayOutputStream s = new FastByteArrayOutputStream(10);
        assertEquals(0, s.size());
        s.write(0x41); // write 'A'
        assertEquals(1, s.size());
        assertEquals('A', s.toByteArray()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeSizeThrows() {
        new FastByteArrayOutputStream(-5);
    }

    // -----------------------------------------------------------------
    // write(int) tests
    // -----------------------------------------------------------------
    @Test
    public void testWriteIntWritesSingleByte() {
        stream.write(0x61); // 'a'
        stream.write(0x62); // 'b'
        assertEquals(2, stream.size());
        assertArrayEquals(new byte[]{0x61, 0x62}, stream.toByteArray());
    }

    @Test
    public void testWriteIntWithOverflowTriggersGrow() {
        // Create a stream with a tiny buffer to force growth
        FastByteArrayOutputStream s = new FastByteArrayOutputStream(2);
        // write 5 bytes, exceeding initial capacity
        for (int i = 0; i < 5; i++) {
            s.write(i);
        }
        assertEquals(5, s.size());

        byte[] expected = new byte[]{0,1,2,3,4};
        assertArrayEquals(expected, s.toByteArray());
    }

    // -----------------------------------------------------------------
    // write(byte[], int, int) tests
    // -----------------------------------------------------------------
    @Test
    public void testWriteByteArrayFull() {
        byte[] data = "hello".getBytes();
        stream.write(data, 0, data.length);
        assertEquals(data.length, stream.size());
        assertArrayEquals(data, stream.toByteArray());
    }

    @Test
    public void testWriteByteArrayPartial() {
        byte[] data = "abcdefgh".getBytes();
        // write "cde" (offset 2, length 3)
        stream.write(data, 2, 3);
        assertEquals(3, stream.size());
        assertArrayEquals(new byte[]{'c','d','e'}, stream.toByteArray());
    }

    @Test
    public void testWriteByteArrayMultipleCallsAccumulate() {
        byte[] part1 = "123".getBytes();
        byte[] part2 = "4567".getBytes();
        stream.write(part1, 0, part1.length);
        stream.write(part2, 0, part2.length);
        assertEquals(7, stream.size());
        assertArrayEquals("1234567".getBytes(), stream.toByteArray());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNullThrows() {
        stream.write(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeOffsetThrows() {
        stream.write(new byte[5], -1, 3);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeLengthThrows() {
        stream.write(new byte[5], 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetBeyondArrayThrows() {
        stream.write(new byte[5], 6, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetPlusLengthBeyondArrayThrows() {
        byte[] arr = new byte[5];
        stream.write(arr, 3, 3); // 3+3 > 5
    }

    // -----------------------------------------------------------------
    // writeBytes(byte[]) tests
    // -----------------------------------------------------------------
    @Test
    public void testWriteBytesConvenienceMethod() {
        byte[] data = "quick".getBytes();
        stream.writeBytes(data);
        assertEquals(data.length, stream.size());
        assertArrayEquals(data, stream.toByteArray());
    }

    // -----------------------------------------------------------------
    // reset() tests
    // -----------------------------------------------------------------
    @Test
    public void testResetClearsSizeButKeepsBufferIntact() {
        stream.writeBytes("data".getBytes());
        assertEquals(4, stream.size());

        stream.reset();
        assertEquals(0, stream.size());

        // After reset, writing new data should start at position 0
        stream.writeBytes("new".getBytes());
        assertEquals(3, stream.size());
        assertArrayEquals("new".getBytes(), stream.toByteArray());
    }

    // -----------------------------------------------------------------
    // toByteArray() and getBuffer() tests
    // -----------------------------------------------------------------
    @Test
    public void testToByteArrayReturnsCopy() {
        stream.writeBytes("copy".getBytes());
        byte[] copy = stream.toByteArray();

        // modify the returned array and verify stream's internal buffer is unchanged
        copy[0] = 'X';
        assertEquals('c', stream.toByteArray()[0]);
    }

    @Test
    public void testGetBufferReturnsCopy() {
        stream.writeBytes("buffer".getBytes());
        byte[] bufCopy = stream.getBuffer();

        bufCopy[0] = 'Z';
        assertEquals('b', stream.getBuffer()[0]);
    }

    // -----------------------------------------------------------------
    // size() tests
    // -----------------------------------------------------------------
    @Test
    public void testSizeReflectsWrittenBytes() {
        assertEquals(0, stream.size());
        stream.write(0x01);
        assertEquals(1, stream.size());
        stream.writeBytes(new byte[]{0x02, 0x03});
        assertEquals(3, stream.size());
    }

    // -----------------------------------------------------------------
    // toString() tests
    // -----------------------------------------------------------------
    @Test
    public void testToStringProducesCorrectString() {
        stream.writeBytes("Hello World".getBytes());
        assertEquals("Hello World", stream.toString());
    }

    @Test
    public void testToStringEmptyStream() {
        assertEquals("", stream.toString());
    }

    // -----------------------------------------------------------------
    // writeTo(OutputStream) tests
    // -----------------------------------------------------------------
    @Test
    public void testWriteToWritesAllBytesToTarget() throws IOException {
        stream.writeBytes("target".getBytes());

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        stream.writeTo(target);

        assertArrayEquals("target".getBytes(), target.toByteArray());
    }

    @Test
    public void testWriteToWithEmptyStreamWritesNothing() throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        stream.writeTo(target);
        assertEquals(0, target.size());
    }

    // -----------------------------------------------------------------
    // close() tests
    // -----------------------------------------------------------------
    @Test
    public void testCloseDoesNotThrow() throws IOException {
        // Should be a no‑op; just ensure no exception is thrown
        stream.close();
    }

    // -----------------------------------------------------------------
    // Additional integration style test: write mixture of methods
    // -----------------------------------------------------------------
    @Test
    public void testMixedWritesProduceCorrectSequence() throws IOException {
        // Write single byte
        stream.write('A');
        // Write a slice of an array
        byte[] part = "BCDEFG".getBytes();
        stream.write(part, 1, 3); // writes "CDE"
        // Write whole array via convenience method
        stream.writeBytes("HI".getBytes());

        // Expected content: "A" + "CDE" + "HI" = "ACDEHI"
        byte[] expected = "ACDEHI".getBytes();
        assertArrayEquals(expected, stream.toByteArray());
        assertEquals(expected.length, stream.size());
        assertEquals("ACDEHI", stream.toString());

        // Verify writeTo writes the exact same bytes
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        stream.writeTo(out);
        assertArrayEquals(expected, out.toByteArray());
    }
}
