package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_writeBytes_Test {

    private FastByteArrayOutputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test to guarantee isolation.
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // The stream does not hold external resources, but we clear the reference
        // to help the garbage collector in long test suites.
        stream = null;
    }

    private static Stream<Arguments> singleWriteProvider() {
        return Stream.of(// "Hello" → [72,101,108,108,111]
        Arguments.of(new byte[] { 72, 101, 108, 108, 111 }, 5, new byte[] { 72, 101, 108, 108, 111 }));
    }

    private static Stream<Arguments> multipleWritesProvider() {
        return Stream.of(// first write {1,2,3}, then {4,5}
        Arguments.of(new byte[][] { new byte[] { 1, 2, 3 }, new byte[] { 4, 5 } }, 5, new byte[] { 1, 2, 3, 4, 5 }));
    }

    private static Stream<Arguments> largeArrayProvider() {
        // 100‑byte array with values 0..99
        byte[] hundred = new byte[100];
        for (int i = 0; i < hundred.length; i++) hundred[i] = (byte) i;
        return Stream.of(Arguments.of(hundred, 100, hundred));
    }

    private static Stream<Arguments> multipleExpansionsProvider() {
        // Build the 130‑byte array described in the JSON spec
        byte[] data = new byte[130];
        for (int i = 0; i < 128; i++) {
            // 0 .. 127
            data[i] = (byte) i;
        }
        // 0x80
        data[128] = (byte) -128;
        // 0x81
        data[129] = (byte) -127;
        return Stream.of(Arguments.of(data, 130, data));
    }

    @Test
    void writeBytes_writesEntireArray() {
        byte[] data = { 10, 20, 30, 40, 50 };
        stream.writeBytes(data);
        assertEquals(data.length, stream.size(), "Stream size should match the number of bytes written");
        assertArrayEquals(data, stream.toByteArray(), "The internal buffer should contain exactly the written bytes");
    }

    @Test
    void writeBytes_withEmptyArrayDoesNothing() {
        byte[] empty = new byte[0];
        stream.writeBytes(empty);
        assertEquals(0, stream.size(), "Writing an empty array must not change the size");
        assertArrayEquals(empty, stream.toByteArray(), "The resulting byte array should also be empty");
    }

    @Test
    void writeBytes_nullArrayThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> stream.writeBytes(null), "Passing a null array should result in NPE as per OutputStream contract");
    }

    @Test
    void writeBytes_largeArrayTriggersBufferGrowth() {
        // The default internal buffer is 32 bytes. Write 100 bytes to force at least
        // two growth operations.
        byte[] large = new byte[100];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) i;
        }
        stream.writeBytes(large);
        assertEquals(large.length, stream.size(), "Size must reflect the full length of the large array");
        assertArrayEquals(large, stream.toByteArray(), "All bytes must be present after buffer growth");
    }

    @Test
    void writeBytes_multipleCallsAccumulateData() {
        byte[] first = { 1, 2, 3 };
        byte[] second = { 4, 5 };
        stream.writeBytes(first);
        stream.writeBytes(second);
        byte[] expected = { 1, 2, 3, 4, 5 };
        assertEquals(expected.length, stream.size(), "Size should be the sum of both writes");
        assertArrayEquals(expected, stream.toByteArray(), "Data from successive calls must be concatenated in order");
    }

    /* --------------------------------------------------------------------- *
     *  Normal behavior – single write
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] writeBytes({0}) → size={1}")
    @MethodSource("singleWriteProvider")
    void writeBytes_normalSingleWrite(byte[] input, int expectedSize, byte[] expectedContent) {
        stream.writeBytes(input);
        assertEquals(expectedSize, stream.size(), "Stream size should match the number of bytes written");
        assertArrayEquals(expectedContent, stream.toByteArray(), "Internal buffer must contain exactly the written bytes");
    }

    /* --------------------------------------------------------------------- *
     *  Normal behavior – multiple sequential writes
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] sequential write {0}")
    @MethodSource("multipleWritesProvider")
    void writeBytes_multipleSequentialWrites(byte[][] inputs, int expectedSize, byte[] expectedContent) {
        for (byte[] part : inputs) {
            stream.writeBytes(part);
        }
        assertEquals(expectedSize, stream.size(), "Size should be the sum of all writes");
        assertArrayEquals(expectedContent, stream.toByteArray(), "Data must be concatenated in the order of writes");
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – empty array
     * --------------------------------------------------------------------- */
    @Test
    void writeBytes_emptyArrayDoesNothing() {
        byte[] empty = new byte[0];
        stream.writeBytes(empty);
        assertEquals(0, stream.size(), "Writing an empty array must not change the size");
        assertArrayEquals(empty, stream.toByteArray(), "Resulting byte array should also be empty");
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – large array causing buffer growth
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] write large array of length {1}")
    @MethodSource("largeArrayProvider")
    void writeBytes_largeArrayTriggersGrowth(byte[] input, int expectedSize, byte[] expectedContent) {
        stream.writeBytes(input);
        assertEquals(expectedSize, stream.size(), "Size must reflect the full length of the large array");
        assertArrayEquals(expectedContent, stream.toByteArray(), "All bytes must be present after internal buffer growth");
    }

    /* --------------------------------------------------------------------- *
     *  Exception handling – null argument
     * --------------------------------------------------------------------- */
    @Test
    void writeBytes_nullArrayThrowsNPE() {
        assertThrows(NullPointerException.class, () -> stream.writeBytes(null), "Passing a null array should result in NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  State reset behavior – write after reset
     * --------------------------------------------------------------------- */
    @Test
    void writeBytes_afterResetStartsFromEmptyState() {
        // first write
        stream.writeBytes(new byte[] { 10, 20, 30 });
        assertEquals(3, stream.size());
        assertArrayEquals(new byte[] { 10, 20, 30 }, stream.toByteArray());
        // reset the stream
        stream.reset();
        assertEquals(0, stream.size(), "After reset the size must be zero");
        assertArrayEquals(new byte[0], stream.toByteArray(), "After reset the buffer must be empty");
        // second write after reset
        stream.writeBytes(new byte[] { 40, 50 });
        assertEquals(2, stream.size(), "Size after second write must reflect only the new bytes");
        assertArrayEquals(new byte[] { 40, 50 }, stream.toByteArray(), "Buffer must contain only the bytes written after reset");
    }

    /* --------------------------------------------------------------------- *
     *  Immutability of stored data – modify source after write
     * --------------------------------------------------------------------- */
    @Test
    void writeBytes_copiesSourceArray() {
        byte[] source = new byte[] { 1, 2, 3, 4, 5 };
        stream.writeBytes(source);
        // mutate the original array
        for (int i = 0; i < source.length; i++) {
            source[i] = (byte) (source[i] + 10);
        }
        // the stream must retain the original values
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, stream.toByteArray(), "Internal buffer must be a copy; changes to the source array must not affect it");
    }

    /* --------------------------------------------------------------------- *
     *  Single‑byte array with negative value
     * --------------------------------------------------------------------- */
    @Test
    void writeBytes_singleNegativeByte() {
        // 0xFF
        byte[] single = new byte[] { -1 };
        stream.writeBytes(single);
        assertEquals(1, stream.size());
        assertArrayEquals(single, stream.toByteArray(), "Negative byte values must be stored unchanged");
    }

    /* --------------------------------------------------------------------- *
     *  Multiple writes causing several buffer expansions
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] write 130‑byte array")
    @MethodSource("multipleExpansionsProvider")
    void writeBytes_multipleExpansions(byte[] input, int expectedSize, byte[] expectedContent) {
        stream.writeBytes(input);
        assertEquals(expectedSize, stream.size(), "Size should equal the length of the input array");
        assertArrayEquals(expectedContent, stream.toByteArray(), "All bytes must be retained after several buffer growth steps");
    }

    /* --------------------------------------------------------------------- *
     *  Write after close – no exception expected
     * --------------------------------------------------------------------- */
    @Test
    void writeBytes_afterCloseStillWorks() throws IOException {
        // FastByteArrayOutputStream's close is a no‑op
        stream.close();
        byte[] payload = new byte[] { 9, 8, 7 };
        stream.writeBytes(payload);
        assertEquals(3, stream.size());
        assertArrayEquals(payload, stream.toByteArray(), "Writing after close should still store the data");
    }
}
