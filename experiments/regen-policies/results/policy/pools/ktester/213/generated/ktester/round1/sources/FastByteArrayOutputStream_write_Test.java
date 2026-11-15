package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.lang.reflect.Field;

// Mockito not actually used but kept from template
@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_write_Test {

    private FastByteArrayOutputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no accidental reuse
        stream = null;
    }

    @Test
    void writeSinglePositiveByte() {
        // given
        // ASCII 'A'
        int value = 0x41;
        // when
        stream.write(value);
        // then
        assertEquals(1, stream.size(), "Size should be 1 after writing a single byte");
        assertArrayEquals(new byte[] { (byte) value }, stream.toByteArray(), "Buffer should contain the written byte");
    }

    @Test
    void writeZeroByte() {
        // given
        int value = 0;
        // when
        stream.write(value);
        // then
        assertEquals(1, stream.size());
        assertEquals(0, stream.toByteArray()[0] & 0xFF);
    }

    @Test
    void writeNegativeIntIsTruncatedToByte() {
        // given
        // 0xFFFFFFFF -> low 8 bits 0xFF
        int value = -1;
        // when
        stream.write(value);
        // then
        assertEquals(1, stream.size());
        assertEquals(0xFF, stream.toByteArray()[0] & 0xFF, "Negative int should be stored as its low‑order byte");
    }

    @Test
    void writeMultipleBytesTriggersGrowth() {
        // given
        FastByteArrayOutputStream smallStream = new FastByteArrayOutputStream(2);
        byte[] data = new byte[] { 1, 2, 3, 4, 5 };
        // when
        for (byte b : data) {
            smallStream.write(b);
        }
        // then
        assertEquals(data.length, smallStream.size(), "Size should reflect the number of bytes written");
        assertArrayEquals(data, smallStream.toByteArray(), "All bytes should be present after internal buffer growth");
    }

    @Test
    void writeAfterResetStartsFromZero() {
        // given
        stream.write(10);
        stream.write(20);
        assertEquals(2, stream.size());
        // when
        stream.reset();
        assertEquals(0, stream.size(), "Size should be zero after reset");
        stream.write(30);
        // then
        assertEquals(1, stream.size());
        assertArrayEquals(new byte[] { 30 }, stream.toByteArray(), "Buffer should contain only the byte written after reset");
    }

    @Test
    void writeLargeNumberOfBytesEnsuresCapacityLogic() {
        // given
        int initialSize = 4;
        FastByteArrayOutputStream largeStream = new FastByteArrayOutputStream(initialSize);
        // well beyond the default capacity to force several growth steps
        int totalBytes = 1024;
        byte[] expected = new byte[totalBytes];
        for (int i = 0; i < totalBytes; i++) {
            expected[i] = (byte) (i & 0xFF);
            largeStream.write(expected[i]);
        }
        // then
        assertEquals(totalBytes, largeStream.size(), "Size must match the number of bytes written");
        assertArrayEquals(expected, largeStream.toByteArray(), "All bytes must be correctly stored after multiple growth operations");
    }

    @Test
    void writeMaximumUnsignedByte() {
        // given
        // 0xFF
        int value = 255;
        // when
        stream.write(value);
        // then
        assertEquals(1, stream.size());
        assertEquals(0xFF, stream.toByteArray()[0] & 0xFF, "Maximum unsigned byte should be stored as 0xFF");
    }

    @Test
    void writeIntLargerThanByteIsMasked() {
        // given
        // 511 -> low 8 bits 0xFF
        int value = 0x1FF;
        // when
        stream.write(value);
        // then
        assertEquals(1, stream.size());
        assertEquals(0xFF, stream.toByteArray()[0] & 0xFF);
    }

    @Test
    void writeNegativeIntThatFitsInByte() {
        // given
        // 0x80
        int value = -128;
        // when
        stream.write(value);
        // then
        assertEquals(1, stream.size());
        assertEquals(0x80, stream.toByteArray()[0] & 0xFF);
    }

    @Test
    void writeIntegerMaxValueMasksToFF() {
        // given
        // 0x7FFFFFFF -> low 8 bits 0xFF
        int value = Integer.MAX_VALUE;
        // when
        stream.write(value);
        // then
        assertEquals(1, stream.size());
        assertEquals(0xFF, stream.toByteArray()[0] & 0xFF);
    }

    @Test
    void writeSeventyBytesTriggersGrowth() {
        // given
        FastByteArrayOutputStream smallStream = new FastByteArrayOutputStream(2);
        byte[] expected = new byte[70];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = 0x01;
        }
        // when
        for (int i = 0; i < 70; i++) {
            smallStream.write(0x01);
        }
        // then
        assertEquals(70, smallStream.size());
        assertArrayEquals(expected, smallStream.toByteArray(), "All 70 bytes should be present after buffer growth");
    }

    @Test
    void writeTwoHundredBytesMultipleExpansions() {
        // given
        FastByteArrayOutputStream stream200 = new FastByteArrayOutputStream(4);
        byte[] expected = new byte[200];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = 0x7F;
        }
        // when
        for (int i = 0; i < 200; i++) {
            stream200.write(0x7F);
        }
        // then
        assertEquals(200, stream200.size());
        assertArrayEquals(expected, stream200.toByteArray(), "All 200 bytes should be retained after several growth steps");
    }

    @Test
    void writeAfterResetStartsFromZeroWithDifferentByte() throws IOException {
        // given
        // 'A'
        stream.write(0x41);
        stream.reset();
        assertEquals(0, stream.size(), "Size should be zero after reset");
        // when
        // 'B'
        stream.write(0x42);
        // then
        assertEquals(1, stream.size());
        assertArrayEquals(new byte[] { 0x42 }, stream.toByteArray(), "Buffer should contain only the byte written after reset");
    }

    @Test
    void writeThreeDistinctBytesPreservesOrder() {
        // given
        int[] values = { 0x41, 0x42, 0x43 };
        // when
        for (int v : values) {
            stream.write(v);
        }
        // then
        assertEquals(3, stream.size());
        assertArrayEquals(new byte[] { 0x41, 0x42, 0x43 }, stream.toByteArray(), "Bytes must appear in the order they were written");
    }

    @Test
    void writeWhenInternalBufferIsNullAllocatesBuffer() throws Exception {
        // given: force the private buf field to null via reflection
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        bufField.set(stream, null);
        // also reset count to 0 for safety
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(stream, 0);
        // when
        stream.write(0x55);
        // then
        assertEquals(1, stream.size());
        assertArrayEquals(new byte[] { 0x55 }, stream.toByteArray(), "First write must allocate a buffer and store the byte");
    }

    @Test
    void writeWhenCountOverflowThrowsIllegalArgumentException() throws Exception {
        // given: set count to Integer.MAX_VALUE via reflection
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(stream, Integer.MAX_VALUE);
        // when / then
        assertThrows(IllegalArgumentException.class, () -> stream.write(0), "Overflow of count + 1 should cause IllegalArgumentException");
    }

    @Test
    @Disabled("Running this test would require allocating an array larger than the JVM can handle and is therefore disabled in normal CI environments.")
    void writeWhenCapacityGrowthOverflowsThrowsNegativeArraySizeException() throws Exception {
        // given: create a stream with a relatively small buffer but a count close to max int
        FastByteArrayOutputStream problematic = new FastByteArrayOutputStream(10);
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(problematic, Integer.MAX_VALUE - 5);
        // when / then
        assertThrows(NegativeArraySizeException.class, () -> problematic.write(0x7F), "Growth beyond Integer.MAX_VALUE should throw NegativeArraySizeException");
    }

    @Test
    @Disabled("Triggering an OutOfMemoryError in a unit test is unsafe; test is provided for completeness.")
    void writeWhenAllocationFailsThrowsOutOfMemoryError() throws Exception {
        // given: artificially set a huge buffer size that will cause the next growth to fail
        FastByteArrayOutputStream oomStream = new FastByteArrayOutputStream(1);
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        // Allocate a large array (may already cause OOM – this is why the test is disabled)
        int hugeSize = Integer.MAX_VALUE / 2;
        bufField.set(oomStream, new byte[hugeSize]);
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(oomStream, hugeSize);
        // when / then
        assertThrows(OutOfMemoryError.class, () -> oomStream.write(0x01), "Insufficient memory for buffer expansion should result in OutOfMemoryError");
    }

    @Test
    void writeAfterCloseThrowsIOException() throws IOException {
        // given
        stream.close();
        // when / then
        assertThrows(IOException.class, () -> stream.write(0x41), "Writing to a closed stream must throw IOException");
    }
}
