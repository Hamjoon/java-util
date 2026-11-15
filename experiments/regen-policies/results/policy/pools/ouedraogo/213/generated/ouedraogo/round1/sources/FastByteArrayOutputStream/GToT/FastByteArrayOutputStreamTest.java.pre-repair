package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FastByteArrayOutputStreamTest {

    private FastByteArrayOutputStream fastOut;
    private FastByteArrayOutputStream fastOutCustom;

    @Before
    public void setUp() {
        fastOut = new FastByteArrayOutputStream();            // default size 32
        fastOutCustom = new FastByteArrayOutputStream(4);    // small size to trigger growth
    }

    @After
    public void tearDown() {
        fastOut = null;
        fastOutCustom = null;
    }

    /*** 1. Constructor tests ***/

    @Test
    public void testDefaultConstructorCreatesNonNullBuffer() {
        assertNotNull(fastOut);
        assertEquals(0, fastOut.size());
        assertArrayEquals(new byte[0], fastOut.toByteArray());
    }

    @Test
    public void testConstructorWithPositiveSize() {
        FastByteArrayOutputStream s = new FastByteArrayOutputStream(10);
        assertNotNull(s);
        assertEquals(0, s.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeSizeThrows() {
        new FastByteArrayOutputStream(-5);
    }

    /*** 2. write(int) tests ***/

    @Test
    public void testWriteSingleByte() {
        fastOut.write(0x41); // 'A'
        assertEquals(1, fastOut.size());
        assertArrayEquals(new byte[]{0x41}, fastOut.toByteArray());
        assertEquals("A", fastOut.toString());
    }

    @Test
    public void testWriteMultipleBytesTriggersGrow() {
        // fastOutCustom starts with capacity 4, write 5 bytes to force growth
        for (int i = 0; i < 5; i++) {
            fastOutCustom.write(i);
        }
        assertEquals(5, fastOutCustom.size());
        byte[] expected = new byte[]{0,1,2,3,4};
        assertArrayEquals(expected, fastOutCustom.toByteArray());
    }

    /*** 3. write(byte[], int, int) tests ***/

    @Test
    public void testWriteByteArrayFull() {
        byte[] data = "hello".getBytes();
        fastOut.write(data, 0, data.length);
        assertEquals(data.length, fastOut.size());
        assertArrayEquals(data, fastOut.toByteArray());
    }

    @Test
    public void testWriteByteArrayPartial() {
        byte[] data = "abcdefgh".getBytes();
        fastOut.write(data, 2, 4); // write "cdef"
        assertEquals(4, fastOut.size());
        assertArrayEquals(new byte[]{'c','d','e','f'}, fastOut.toByteArray());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNullThrows() {
        fastOut.write(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeOffsetThrows() {
        fastOut.write(new byte[10], -1, 5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayNegativeLengthThrows() {
        fastOut.write(new byte[10], 0, -5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayOffsetBeyondArrayThrows() {
        fastOut.write(new byte[5], 6, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteByteArrayLengthBeyondArrayThrows() {
        fastOut.write(new byte[5], 3, 3); // 3+3 > 5
    }

    /*** 4. writeBytes(byte[]) tests ***/

    @Test
    public void testWriteBytesConvenienceMethod() {
        byte[] data = {1,2,3,4};
        fastOut.writeBytes(data);
        assertEquals(4, fastOut.size());
        assertArrayEquals(data, fastOut.toByteArray());
    }

    /*** 5. reset() tests ***/

    @Test
    public void testResetClearsBuffer() {
        fastOut.writeBytes("test".getBytes());
        assertEquals(4, fastOut.size());
        fastOut.reset();
        assertEquals(0, fastOut.size());
        assertArrayEquals(new byte[0], fastOut.toByteArray());
        assertEquals("", fastOut.toString());
    }

    /*** 6. toByteArray() tests ***/

    @Test
    public void testToByteArrayReturnsCopyNotReference() {
        fastOut.writeBytes("copy".getBytes());
        byte[] arr = fastOut.toByteArray();
        arr[0] = 'X';
        // internal buffer should remain unchanged
        assertEquals('c', fastOut.toByteArray()[0]);
    }

    /*** 7. getBuffer() tests (backwards compatibility) ***/

    @Test
    public void testGetBufferReturnsCopy() {
        fastOut.writeBytes("buf".getBytes());
        byte[] bufCopy = fastOut.getBuffer();
        bufCopy[0] = 'Z';
        assertEquals('b', fastOut.getBuffer()[0]);
    }

    /*** 8. size() tests ***/

    @Test
    public void testSizeReflectsWrittenBytes() {
        assertEquals(0, fastOut.size());
        fastOut.write(10);
        assertEquals(1, fastOut.size());
        fastOut.writeBytes(new byte[]{20,30});
        assertEquals(3, fastOut.size());
    }

    /*** 9. toString() tests ***/

    @Test
    public void testToStringUsesDefaultCharset() {
        fastOut.writeBytes("Hello".getBytes());
        assertEquals("Hello", fastOut.toString());
    }

    /*** 10. writeTo(OutputStream) tests ***/

    @Test
    public void testWriteToWritesAllData() throws IOException {
        fastOut.writeBytes("data".getBytes());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fastOut.writeTo(baos);

        assertArrayEquals("data".getBytes(), baos.toByteArray());
    }

    @Test(expected = NullPointerException.class)
    public void testWriteToNullOutputStreamThrows() throws IOException {
        fastOut.writeTo(null);
    }

    /*** 11. close() tests ***/

    @Test
    public void testCloseDoesNotThrow() throws IOException {
        fastOut.close(); // should be a no‑op and not throw
    }

    /*** 12. Edge case: writing zero bytes ***/

    @Test
    public void testWriteZeroLengthArrayDoesNothing() {
        fastOut.write(new byte[]{1,2,3}, 1, 0);
        assertEquals(0, fastOut.size());
    }

    /*** 13. Edge case: large write to trigger multiple grows ***/

    @Test
    public void testLargeWriteTriggersMultipleGrowths() {
        // start with small buffer to force several growth steps
        FastByteArrayOutputStream large = new FastByteArrayOutputStream(2);
        byte[] data = new byte[100]; // fill with incremental values
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;

        large.write(data, 0, data.length);
        assertEquals(100, large.size());
        assertArrayEquals(data, large.toByteArray());
    }

    /*** 14. Edge case: maximum theoretical size (near 1GB) – we cannot allocate 1GB in a unit test,
          but we can verify that the growth logic works for large values without overflow. ***/

    @Test
    public void testGrowthDoesNotOverflowIntegerCapacity() {
        // Use a size that will cause an intermediate capacity > Integer.MAX_VALUE/2 but still within int range.
        // Since we cannot allocate such a huge array in a test, we simulate by calling ensureCapacity via reflection.
        // However, per instruction we must not use reflection or other APIs outside the class.
        // Instead we test that writing a large amount that would require growth beyond the initial size works
        // up to a reasonable limit (e.g., 1 << 20 bytes).
        FastByteArrayOutputStream big = new FastByteArrayOutputStream(1 << 10); // 1KB start
        byte[] chunk = new byte[1 << 15]; // 32KB chunk
        for (int i = 0; i < 32; i++) { // total 1MB
            big.write(chunk, 0, chunk.length);
        }
        assertEquals(1 << 20, big.size());
    }
}
