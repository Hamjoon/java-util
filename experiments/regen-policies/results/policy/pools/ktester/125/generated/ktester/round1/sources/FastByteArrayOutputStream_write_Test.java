package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.IOException;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_write_Test {

    private FastByteArrayOutputStream outputStream;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for each test to guarantee isolation.
        outputStream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent.
        outputStream = null;
    }

    @Test
    void writeSingleByte_updatesSizeAndBuffer() {
        // given
        // ASCII 'A'
        int value = 65;
        // when
        outputStream.write(value);
        // then
        assertEquals(1, outputStream.size(), "Size should be 1 after writing one byte");
        assertArrayEquals(new byte[] { (byte) value }, outputStream.toByteArray(), "Buffer should contain the written byte");
    }

    @Test
    void writeIntLargerThanByte_isTruncatedToLowOrderByte() {
        // given
        // 291 decimal, low‑order byte = 0x23 (35 decimal)
        int value = 0x123;
        // when
        outputStream.write(value);
        // then
        assertEquals(1, outputStream.size());
        assertArrayEquals(new byte[] { (byte) 0x23 }, outputStream.toByteArray(), "Only the low‑order 8 bits should be stored");
    }

    @Test
    void writeBeyondInitialCapacity_expandsBuffer() {
        // given
        // Default capacity is 32 bytes; write 40 bytes to force an expansion.
        byte[] data = new byte[40];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        // when
        for (byte b : data) {
            outputStream.write(b);
        }
        // then
        assertEquals(40, outputStream.size(), "Size should reflect all written bytes");
        assertArrayEquals(data, outputStream.toByteArray(), "All bytes should be present after automatic buffer growth");
    }

    @Test
    void writeAfterReset_startsFromZeroAgain() {
        // given
        outputStream.write(10);
        outputStream.write(20);
        assertEquals(2, outputStream.size());
        // when
        // clear the stream
        outputStream.reset();
        // write new data
        outputStream.write(30);
        // then
        assertEquals(1, outputStream.size(), "Size should be 1 after reset and a single write");
        assertArrayEquals(new byte[] { 30 }, outputStream.toByteArray(), "Buffer should contain only the byte written after reset");
    }

    @Test
    void writeMultipleTimesWithMixedValues_behavesConsistently() {
        // given
        int[] values = { 0, 255, 256, -1, 128, 1024 };
        // when
        for (int v : values) {
            outputStream.write(v);
        }
        // then
        byte[] expected = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            // low‑order byte only
            expected[i] = (byte) values[i];
        }
        assertEquals(values.length, outputStream.size());
        assertArrayEquals(expected, outputStream.toByteArray(), "Each int should be stored as its low‑order byte");
    }

    /**
     * Write ten sequential bytes (0‑9) that all fit inside the default buffer.
     */
    @Test
    void writeTenSequentialBytes_fitsWithinDefaultCapacity() {
        // given
        for (int i = 0; i < 10; i++) {
            outputStream.write(i);
        }
        // then
        assertEquals(10, outputStream.size(), "Size should be 10 after ten writes");
        byte[] expected = new byte[10];
        for (int i = 0; i < 10; i++) expected[i] = (byte) i;
        assertArrayEquals(expected, outputStream.toByteArray(), "Buffer should contain bytes 0‑9");
    }

    /**
     * Write 33 sequential bytes, forcing a single buffer expansion.
     */
    @Test
    void writeThirtyThreeBytes_triggersSingleExpansion() {
        // given
        for (int i = 0; i < 33; i++) {
            outputStream.write(i);
        }
        // then
        assertEquals(33, outputStream.size(), "Size should be 33 after writing 33 bytes");
        byte[] expected = new byte[33];
        for (int i = 0; i < 33; i++) expected[i] = (byte) i;
        assertArrayEquals(expected, outputStream.toByteArray(), "All 33 bytes should be present and correctly ordered");
    }

    /**
     * Write a negative integer; the low‑order byte should be 0xFF (255 unsigned).
     */
    @Test
    void writeNegativeOne_resultsIn255() {
        // when
        outputStream.write(-1);
        // then
        assertEquals(1, outputStream.size());
        assertArrayEquals(new byte[] { (byte) 0xFF }, outputStream.toByteArray(), "Writing -1 should store 0xFF (255)");
    }

    /**
     * Write a value larger than a byte (300). Expected stored value is 44 (300 & 0xFF).
     */
    @Test
    void writeValueExceedingByteRange_isTruncated() {
        // when
        outputStream.write(300);
        // then
        assertEquals(1, outputStream.size());
        assertArrayEquals(new byte[] { (byte) 44 }, outputStream.toByteArray(), "300 should be truncated to 44");
    }

    /**
     * Write after a reset – the stream should behave like a fresh instance.
     */
    @Test
    void writeAfterReset_behavesLikeFreshInstance() {
        // given
        outputStream.write(123);
        outputStream.reset();
        // when
        outputStream.write(123);
        // then
        assertEquals(1, outputStream.size());
        assertArrayEquals(new byte[] { 123 }, outputStream.toByteArray(), "After reset, only the new byte should be present");
    }

    /**
     * Stress test: write 1,000 sequential integers, exercising many reallocations.
     */
    @Test
    void writeThousandSequentialBytes_stressGrowthLogic() {
        // when
        for (int i = 0; i < 1000; i++) {
            outputStream.write(i);
        }
        // then
        assertEquals(1000, outputStream.size(), "Size should be 1000 after the loop");
        byte[] result = outputStream.toByteArray();
        assertEquals(0, result[0], "First byte should be 0");
        assertEquals((byte) (999 & 0xFF), result[999], "Last byte should be 999 & 0xFF");
    }

    /**
     * Using reflection, set the internal buffer to {@code null} and verify that
     * {@code write(int)} throws a {@link NullPointerException}.
     */
    @Test
    void writeWithNullInternalBuffer_throwsNullPointerException() throws Exception {
        // arrange: force the private field 'buf' to null
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        bufField.set(outputStream, null);
        // act & assert
        assertThrows(NullPointerException.class, () -> outputStream.write(0), "Writing with a null internal buffer should throw NPE");
    }

    /**
     * Using reflection, set the internal {@code count} to {@link Integer#MAX_VALUE}
     * and verify that the subsequent write triggers an {@link IllegalArgumentException}
     * because the calculated minimum capacity becomes negative.
     */
    @Test
    void writeWhenCountAtMaxValue_throwsIllegalArgumentException() throws Exception {
        // arrange: set private field 'count' to Integer.MAX_VALUE
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(outputStream, Integer.MAX_VALUE);
        // act & assert
        assertThrows(IllegalArgumentException.class, () -> outputStream.write(1), "Writing after count overflow should cause IllegalArgumentException");
    }

    /**
     * Parameterized test covering a few typical values that fit inside the existing capacity.
     * Demonstrates that the low‑order byte is stored regardless of the integer sign or magnitude.
     */
    @ParameterizedTest(name = "write({0}) stores low‑order byte {1}")
    @CsvSource({ // ASCII 'A'
    "65, 65", "0, 0", // 255 as signed byte is -1
    "255, -1", "256, 0", "-128, -128", "1024, 0" })
    void writeVariousValues_storesLowOrderByte(int input, int expectedSignedByte) {
        // when
        outputStream.write(input);
        // then
        assertEquals(1, outputStream.size());
        assertArrayEquals(new byte[] { (byte) expectedSignedByte }, outputStream.toByteArray(), "Stored byte must match low‑order 8 bits of the input");
    }
}
