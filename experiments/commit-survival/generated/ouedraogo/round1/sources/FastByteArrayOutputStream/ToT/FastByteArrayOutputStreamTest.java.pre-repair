package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Comprehensive JUnit‑4 test suite for {@link FastByteArrayOutputStream}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction with default and explicit sizes, including illegal size.</li>
 *   <li>Single‑byte writes and automatic buffer growth.</li>
 *   <li>Byte‑array writes with valid parameters, edge cases (zero length,
 *       full‑array write, offset/length variations) and illegal parameters.</li>
 *   <li>Convenience {@code writeBytes(byte[])} method.</li>
 *   <li>State‑query methods {@code size()}, {@code toByteArray()},
 *       {@code getBuffer()}, {@code toString()}.</li>
 *   <li>Reset behaviour.</li>
 *   <li>Writing the internal buffer to another {@link OutputStream}.</li>
 *   <li>Close method (no‑op).</li>
 * </ul>
 */
public class FastByteArrayOutputStreamTest {

    private FastByteArrayOutputStream stream;

    @Before
    public void setUp() {
        stream = new FastByteArrayOutputStream(); // default capacity 32
    }

    /*--------------------------- Constructor Tests ---------------------------*/

    @Test
    public void testDefaultConstructorInitialCapacity() {
        // The default constructor should allocate a buffer of length 32.
        // We verify this indirectly by writing 32 bytes (no growth) and then
        // checking size and content.
        byte[] data = new byte[32];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        stream.writeBytes(data);
        assertEquals(32, stream.size());
        assertArrayEquals(data, stream.toByteArray());
    }

    @Test
    public void testExplicitConstructorPositiveSize() {
        FastByteArrayOutputStream s = new FastByteArrayOutputStream(10);
        // Write exactly 10 bytes – should not trigger growth.
        byte[] ten = new byte[10];
        Arrays.fill(ten, (byte) 7);
        s.writeBytes(ten);
        assertEquals(10, s.size());
        assertArrayEquals(ten, s.toByteArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeSizeThrows() {
        new FastByteArrayOutputStream(-5);
    }

    /*--------------------------- Single‑Byte Write Tests ---------------------------*/

    @Test
    public void testWriteSingleByteWithinInitialCapacity() {
        stream.write(0x41); // 'A'
        assertEquals(1, stream.size());
        assertEquals('A', stream.toByteArray()[0]);
        assertEquals("A", stream.toString());
    }

    @Test
    public void testWriteSingleByteTriggersGrowth() {
        // Fill the default buffer (32 bytes) then write one more.
        for (int i = 0; i < 32; i++) {
            stream.write(i);
        }
        assertEquals(32, stream.size());
        // Buffer should now grow when we write the 33rd byte.
        stream.write(0x55);
        assertEquals(33, stream.size());
        byte[] expected = new byte[33];
        for (int i = 0; i < 32; i++) expected[i] = (byte) i;
        expected[32] = 0x55;
        assertArrayEquals(expected, stream.toByteArray());
    }

    /*--------------------------- Byte‑Array Write Tests ---------------------------*/

    @Test
    public void testWriteByteArrayFullRange() {
        byte[] data = "Hello World".getBytes();
        stream.write(data, 0, data.length);
        assertEquals(data.length, stream.size());
        assertArrayEquals(data, stream.toByteArray());
        assertEquals("Hello World", stream.toString());
    }

    @Test
    public void testWriteByteArrayWithOffsetAndLength() {
        byte[] source = "ABCDEFGHIJ".getBytes(); // 10 bytes
        // Write "CDE" (offset 2, length 3)
        stream.write(source, 2, 3);
        assertEquals(3, stream.size());
        assertArrayEquals(new byte[]{'C', 'D', 'E'}, stream.toByteArray());
    }

    @Test
    public void testWriteByteArrayZeroLengthDoesNothing() {
        byte[] data = "XYZ".getBytes();
        stream.write(data, 1, 0); // length 0
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
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
        stream.write(new byte[4], 5, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetPlusLengthBeyondArrayThrows() {
        stream.write(new byte[4], 2, 3); // 2+3 > 4
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOverflowCalculationThrows() {
        // Construct parameters that would cause integer overflow in offset+len.
        // Using Integer.MAX_VALUE as length is not possible because array size
        // would be limited, but we can simulate the overflow check by using
        // a large len that makes (off + len) negative.
        byte[] data = new byte[10];
        int largeLen = Integer.MAX_VALUE;
        stream.write(data, -largeLen, largeLen); // off+len wraps around
    }

    /*--------------------------- Convenience writeBytes Tests ---------------------------*/

    @Test
    public void testWriteBytesConvenienceMethod() {
        byte[] payload = {1, 2, 3, 4, 5};
        stream.writeBytes(payload);
        assertEquals(5, stream.size());
        assertArrayEquals(payload, stream.toByteArray());
    }

    /*--------------------------- Reset Tests ---------------------------*/

    @Test
    public void testResetClearsCountButKeepsBuffer() {
        stream.writeBytes("Data".getBytes());
        assertEquals(4, stream.size());
        stream.reset();
        assertEquals(0, stream.size());
        // After reset, writing new data should start at index 0.
        stream.writeBytes("New".getBytes());
        assertEquals(3, stream.size());
        assertEquals("New", stream.toString());
    }

    /*--------------------------- Buffer Access Tests ---------------------------*/

    @Test
    public void testGetBufferReturnsCopy() {
        stream.writeBytes("Copy".getBytes());
        byte[] bufCopy = stream.getBuffer();
        // Modify the returned array – it must not affect the stream.
        bufCopy[0] = 'X';
        assertEquals("Copy", stream.toString());
    }

    @Test
    public void testToByteArrayReturnsCopy() {
        stream.writeBytes(new byte[]{9, 8, 7});
        byte[] arr = stream.toByteArray();
        arr[0] = 0;
        assertArrayEquals(new byte[]{9, 8, 7}, stream.toByteArray());
    }

    /*--------------------------- toString Tests ---------------------------*/

    @Test
    public void testToStringUsesDefaultCharset() {
        // The class uses the platform default charset via new String(byte[],int,int)
        // For ASCII characters this is deterministic.
        stream.writeBytes("ASCII".getBytes());
        assertEquals("ASCII", stream.toString());
    }

    @Test
    public void testToStringEmptyStream() {
        assertEquals("", stream.toString());
    }

    /*--------------------------- writeTo Tests ---------------------------*/

    private static class RecordingOutputStream extends OutputStream {
        private final byte[] buffer = new byte[1024];
        private int pos = 0;

        @Override
        public void write(int b) {
            buffer[pos++] = (byte) b;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            System.arraycopy(b, off, buffer, pos, len);
            pos += len;
        }

        byte[] getWritten() {
            return Arrays.copyOf(buffer, pos);
        }
    }

    @Test
    public void testWriteToWritesExactlyCountBytes() throws IOException {
        byte[] data = {10, 20, 30, 40, 50};
        stream.writeBytes(data);
        RecordingOutputStream out = new RecordingOutputStream();
        stream.writeTo(out);
        assertArrayEquals(data, out.getWritten());
    }

    @Test
    public void testWriteToWithZeroBytesWritesNothing() throws IOException {
        RecordingOutputStream out = new RecordingOutputStream();
        stream.writeTo(out); // stream is empty
        assertEquals(0, out.getWritten().length);
    }

    /*--------------------------- close Tests ---------------------------*/

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        // Close is a no‑op; just ensure it does not throw.
        stream.close();
    }

    /*--------------------------- Size Tests ---------------------------*/

    @Test
    public void testSizeReflectsNumberOfWrittenBytes() {
        assertEquals(0, stream.size());
        stream.write(1);
        assertEquals(1, stream.size());
        stream.writeBytes(new byte[]{2, 3, 4});
        assertEquals(4, stream.size());
    }
}
