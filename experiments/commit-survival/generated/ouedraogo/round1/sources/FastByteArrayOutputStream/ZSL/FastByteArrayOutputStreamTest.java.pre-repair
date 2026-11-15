package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

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

    @Test
    public void testDefaultConstructorHasPositiveCapacity() {
        // The default constructor creates a buffer of size 32.
        // Write less than 32 bytes and ensure they are stored correctly.
        byte[] data = "Hello World".getBytes();
        stream.write(data, 0, data.length);
        assertEquals(data.length, stream.size());
        assertArrayEquals(data, stream.toByteArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeSizeThrows() {
        new FastByteArrayOutputStream(-1);
    }

    @Test
    public void testWriteIntSingleBytes() {
        stream.write(0x41); // 'A'
        stream.write(0xFF); // -1 truncated to 0xFF
        stream.write(0x1234); // lower 8 bits -> 0x34
        byte[] expected = new byte[]{0x41, (byte) 0xFF, 0x34};
        assertArrayEquals(expected, stream.toByteArray());
        assertEquals(3, stream.size());
    }

    @Test
    public void testWriteByteArrayWithOffsetAndLength() {
        byte[] source = "ABCDEFGHIJ".getBytes(); // 10 bytes
        // Write from index 2, length 5 -> "CDEFG"
        stream.write(source, 2, 5);
        byte[] expected = Arrays.copyOfRange(source, 2, 7);
        assertArrayEquals(expected, stream.toByteArray());
        assertEquals(5, stream.size());
    }

    @Test
    public void testWriteByteArrayZeroLengthDoesNothing() {
        byte[] source = "XYZ".getBytes();
        stream.write(source, 1, 0);
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
    }

    @Test
    public void testWriteByteArrayInvalidArguments() {
        byte[] source = "test".getBytes();

        // null array
        try {
            stream.write(null, 0, 1);
            fail("Expected IndexOutOfBoundsException for null array");
        } catch (IndexOutOfBoundsException ignored) {
        }

        // negative offset
        try {
            stream.write(source, -1, 1);
            fail("Expected IndexOutOfBoundsException for negative offset");
        } catch (IndexOutOfBoundsException ignored) {
        }

        // negative length
        try {
            stream.write(source, 0, -1);
            fail("Expected IndexOutOfBoundsException for negative length");
        } catch (IndexOutOfBoundsException ignored) {
        }

        // offset beyond array length
        try {
            stream.write(source, source.length + 1, 0);
            fail("Expected IndexOutOfBoundsException for offset > length");
        } catch (IndexOutOfBoundsException ignored) {
        }

        // offset+len exceeds array length
        try {
            stream.write(source, 2, 3);
            fail("Expected IndexOutOfBoundsException for offset+len > array length");
        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    @Test
    public void testWriteBytesConvenienceMethod() {
        byte[] data = "Convenient".getBytes();
        stream.writeBytes(data);
        assertArrayEquals(data, stream.toByteArray());
        assertEquals(data.length, stream.size());
    }

    @Test
    public void testBufferGrowthBeyondInitialCapacity() {
        // Start with a tiny buffer to force growth.
        FastByteArrayOutputStream small = new FastByteArrayOutputStream(2);
        byte[] data = "1234567890".getBytes(); // 10 bytes
        small.write(data, 0, data.length);
        assertEquals(data.length, small.size());
        assertArrayEquals(data, small.toByteArray());
    }

    @Test
    public void testResetClearsSizeButKeepsBuffer() {
        byte[] data = "BeforeReset".getBytes();
        stream.write(data, 0, data.length);
        assertEquals(data.length, stream.size());

        stream.reset();
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());

        // After reset we can write new data
        byte[] newData = "After".getBytes();
        stream.write(newData, 0, newData.length);
        assertEquals(newData.length, stream.size());
        assertArrayEquals(newData, stream.toByteArray());
    }

    @Test
    public void testToByteArrayReturnsCopy() {
        byte[] data = "CopyTest".getBytes();
        stream.write(data, 0, data.length);
        byte[] copy = stream.toByteArray();
        assertArrayEquals(data, copy);

        // Mutate the returned array
        copy[0] = (byte) 'X';
        // Original stream data must stay unchanged
        assertArrayEquals(data, stream.toByteArray());
    }

    @Test
    public void testGetBufferReturnsCopy() {
        byte[] data = "BufferCopy".getBytes();
        stream.write(data, 0, data.length);
        byte[] bufferCopy = stream.getBuffer();
        assertArrayEquals(data, bufferCopy);

        // Mutate the copy and ensure stream is unaffected
        bufferCopy[1] = (byte) 'Y';
        assertArrayEquals(data, stream.getBuffer());
    }

    @Test
    public void testToStringProducesCorrectString() {
        String text = "StringRepresentation";
        stream.write(text.getBytes(), 0, text.length());
        assertEquals(text, stream.toString());
    }

    @Test
    public void testWriteToWritesExactBytes() throws IOException {
        String payload = "WriteToPayload";
        stream.write(payload.getBytes(), 0, payload.length());

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        stream.writeTo(target);
        assertArrayEquals(payload.getBytes(), target.toByteArray());
    }

    @Test
    public void testCloseDoesNotAffectUsability() throws IOException {
        // Close should be a no‑op; after closing we can still write.
        stream.close();
        byte[] data = "AfterClose".getBytes();
        stream.write(data, 0, data.length);
        assertArrayEquals(data, stream.toByteArray());
    }

    @Test
    public void testSizeReflectsNumberOfWrittenBytes() {
        assertEquals(0, stream.size());
        stream.write(0x01);
        assertEquals(1, stream.size());
        stream.write(new byte[]{0x02, 0x03, 0x04}, 0, 3);
        assertEquals(4, stream.size());
    }
}
