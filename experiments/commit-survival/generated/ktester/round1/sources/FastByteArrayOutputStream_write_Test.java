package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class FastByteArrayOutputStream_write_Test {

    /**
     * Instance created before each test that uses the default constructor.
     */
    private FastByteArrayOutputStream defaultStream;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for every test to avoid cross‑test contamination.
        defaultStream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern symmetric.
        defaultStream = null;
    }

    /**
     * Verify that a single byte written via {@code write(int)} is stored correctly
     * and that the reported size matches the number of writes.
     */
    @Test
    void writeSingleByte_updatesSizeAndContent() {
        // given
        // ASCII 'A'
        int value = 0x41;
        // when
        defaultStream.write(value);
        // then
        assertEquals(1, defaultStream.size(), "Size should be 1 after writing a single byte");
        assertArrayEquals(new byte[] { (byte) value }, defaultStream.toByteArray(), "Internal buffer should contain the written byte");
    }

    /**
     * Verify that values outside the range 0‑255 are truncated to the low‑order byte,
     * which is the contract of {@code OutputStream.write(int)}.
     */
    @Test
    void writeIntBeyondByteRange_isTruncatedToLowOrderByte() {
        // given
        // 511 decimal, low‑order byte is 0xFF (-1 as signed byte)
        int value = 0x1FF;
        // when
        defaultStream.write(value);
        // then
        assertEquals(1, defaultStream.size());
        assertArrayEquals(new byte[] { (byte) value }, defaultStream.toByteArray(), "Only the low‑order 8 bits should be stored");
    }

    /**
     * Verify that the internal buffer grows automatically when the initial capacity
     * is insufficient for the number of bytes written.
     */
    @Test
    void writeBeyondInitialCapacity_triggersBufferExpansion() {
        // given: a stream with a deliberately tiny initial buffer
        FastByteArrayOutputStream smallStream = new FastByteArrayOutputStream(2);
        // 5 bytes > initial capacity of 2
        byte[] data = new byte[] { 10, 20, 30, 40, 50 };
        // when: write each byte individually using write(int)
        for (byte b : data) {
            smallStream.write(b);
        }
        // then
        assertEquals(data.length, smallStream.size(), "Size should reflect the total number of bytes written");
        assertArrayEquals(data, smallStream.toByteArray(), "All bytes should be present after automatic buffer expansion");
    }

    /**
     * Verify that calling {@code write(int)} does not throw any checked or unchecked
     * exceptions for normal usage.
     */
    @Test
    void writeDoesNotThrowException() {
        assertDoesNotThrow(() -> defaultStream.write(123), "write(int) should not throw any exception for a valid byte value");
    }

    /**
     * Basic functionality – write a single byte and verify content/size.
     * Covers values 0, 255 and -1 (which maps to 0xFF after casting).
     */
    @ParameterizedTest(name = "write({0}) → expected byte 0x{1}")
    @CsvSource({ "0,   00", "255, FF", "-1,  FF" })
    void writeBasicFunctionality(int input, String expectedHex) {
        defaultStream.write(input);
        assertEquals(1, defaultStream.size(), "Size must be 1 after a single write");
        byte expected = (byte) Integer.parseInt(expectedHex, 16);
        assertArrayEquals(new byte[] { expected }, defaultStream.toByteArray(), "Buffer should contain the expected byte");
    }

    /**
     * Buffer expansion – repeatedly write the same byte many times to force growth.
     */
    @ParameterizedTest(name = "repeat {1} times value {0}")
    @CsvSource({ "0,   1000", "255, 5000" })
    void writeRepeatedValuesTriggersExpansion(int value, int repeat) {
        // small start
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream(16);
        for (int i = 0; i < repeat; i++) {
            stream.write(value);
        }
        assertEquals(repeat, stream.size(), "Size must equal number of writes");
        byte expected = (byte) value;
        byte[] expectedArray = new byte[repeat];
        Arrays.fill(expectedArray, expected);
        assertArrayEquals(expectedArray, stream.toByteArray(), "All bytes must match the written value");
        // also ensure internal buffer length is at least repeat (implementation detail)
        assertTrue(stream.getBuffer().length >= repeat, "Internal buffer should have grown to accommodate the data");
    }

    /**
     * Multiple sequential writes – verify ordering and handling of edge values.
     */
    @Test
    void writeSequentialIncreasingValues() {
        int[] values = { 0, 1, 2, 3 };
        for (int v : values) {
            defaultStream.write(v);
        }
        assertEquals(values.length, defaultStream.size());
        byte[] expected = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            expected[i] = (byte) values[i];
        }
        assertArrayEquals(expected, defaultStream.toByteArray(), "Bytes must be stored in the order written");
    }

    /**
     * Multiple sequential writes – edge signed values and a value that overflows a byte.
     */
    @Test
    void writeSequentialEdgeValues() {
        int[] values = { -128, 127, -1, 256 };
        for (int v : values) {
            defaultStream.write(v);
        }
        assertEquals(values.length, defaultStream.size());
        byte[] expected = new byte[] { // 0x80
        (byte) -128, // 0x7F
        (byte) 127, // 0xFF
        (byte) -1, // low‑order byte 0x00
        (byte) 256 };
        assertArrayEquals(expected, defaultStream.toByteArray(), "Edge values must be truncated to low‑order byte correctly");
    }

    /**
     * High‑order bits truncation – only the least‑significant byte should be kept.
     */
    @ParameterizedTest(name = "write({0}) → low‑order byte 0x{1}")
    @CsvSource({ "0x12345678, 78", "-2147483648, 00" })
    void writeHighOrderBitsTruncation(int input, String expectedHex) {
        defaultStream.write(input);
        assertEquals(1, defaultStream.size());
        byte expected = (byte) Integer.parseInt(expectedHex, 16);
        assertArrayEquals(new byte[] { expected }, defaultStream.toByteArray(), "Only low‑order 8 bits should be stored");
    }

    /**
     * After reset – ensure the stream behaves like a fresh instance.
     */
    @Test
    void writeAfterReset_clearsPreviousData() {
        defaultStream.write(10);
        // clear internal count
        defaultStream.reset();
        defaultStream.write(20);
        assertEquals(1, defaultStream.size(), "Size should be 1 after reset and a single new write");
        assertArrayEquals(new byte[] { (byte) 20 }, defaultStream.toByteArray(), "Buffer should contain only the byte written after reset");
    }

    /**
     * After close – OutputStream contract does not forbid writes after close.
     * The implementation should still store the byte.
     */
    @Test
    void writeAfterClose_stillWrites() throws IOException {
        defaultStream.write(33);
        // default close does nothing special
        defaultStream.close();
        defaultStream.write(44);
        assertEquals(2, defaultStream.size(), "Size must reflect both writes even after close");
        assertArrayEquals(new byte[] { 33, 44 }, defaultStream.toByteArray(), "Both bytes must be present");
    }

    /**
     * Null internal buffer – force {@code buf} to null via reflection and expect NPE.
     */
    @Test
    void writeWithNullInternalBuffer_throwsNPE() throws Exception {
        // set the private field 'buf' to null
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        bufField.set(defaultStream, null);
        assertThrows(NullPointerException.class, () -> defaultStream.write(0), "Writing with a null internal buffer should throw NPE");
    }

    /**
     * Integer overflow – set {@code count} to Integer.MAX_VALUE, then write.
     * The subsequent ensureCapacity call receives a negative minCapacity and should
     * throw IllegalArgumentException (as per typical ByteArrayOutputStream behaviour).
     */
    @Test
    void writeWithCountAtMaxValue_throwsIllegalArgumentException() throws Exception {
        // set count to Integer.MAX_VALUE via reflection
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(defaultStream, Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> defaultStream.write(123), "Writing when count is at MAX_VALUE should cause illegal capacity");
    }

    /**
     * Out‑of‑memory during buffer expansion – set count close to Integer.MAX_VALUE/2
     * so that the next ensureCapacity attempts to allocate an array larger than the JVM can handle.
     * Expect an OutOfMemoryError.
     */
    @Test
    void writeTriggersOutOfMemoryErrorDuringExpansion() throws Exception {
        // Use a fresh instance with a small initial buffer
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream(8);
        // set count to a large value that will cause the next ensureCapacity to request
        // an array larger than the heap (approximately 2 GB). This is deliberately risky;
        // the test expects an OOME.
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        // ~1GB (Integer.MAX_VALUE/2)
        countField.setInt(stream, 1_073_741_823);
        // The write should now try to grow the buffer beyond available memory.
        assertThrows(OutOfMemoryError.class, () -> stream.write(0), "Writing with a near‑max count should cause OutOfMemoryError");
    }
}
