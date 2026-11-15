package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_writeBytes_Test {

    private FastByteArrayOutputStream outputStream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        outputStream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent
        outputStream = null;
    }

    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// Typical non‑empty array
        Arguments.of("Write a typical non‑empty byte array; the stream should store all bytes in order.", new byte[] { 10, 20, 30 }, new byte[] { 10, 20, 30 }), // Empty array – no change
        Arguments.of("Write an empty byte array; no data should be added and no exception should be thrown.", new byte[0], new byte[0]), // Two successive calls – verify appending
        Arguments.of("Call writeBytes twice to verify that data is appended, not overwritten.", // First call array; the test will invoke a second call inside the lambda
        new byte[] { 1, 2, 3 }, // Expected after second call (first + second)
        new byte[] { 1, 2, 3, 4, 5 }));
    }

    @Test
    void testWriteBytesWritesFullArray() {
        byte[] data = "Hello".getBytes();
        outputStream.writeBytes(data);
        assertEquals(data.length, outputStream.size(), "Size should match the length of the written array");
        assertArrayEquals(data, outputStream.toByteArray(), "Buffer content should be exactly the written bytes");
    }

    @Test
    void testWriteBytesAppendsToExistingData() {
        byte[] first = "Hello".getBytes();
        byte[] second = "World".getBytes();
        // existing data
        outputStream.write(first);
        // append via writeBytes
        outputStream.writeBytes(second);
        byte[] expected = "HelloWorld".getBytes();
        assertEquals(expected.length, outputStream.size(), "Size after appending should be the sum of both arrays");
        assertArrayEquals(expected, outputStream.toByteArray(), "Buffer should contain the concatenated bytes");
    }

    @Test
    void testWriteBytesEmptyArrayDoesNotChangeState() {
        byte[] empty = new byte[0];
        outputStream.writeBytes(empty);
        assertEquals(0, outputStream.size(), "Writing an empty array must not change the size");
        assertArrayEquals(new byte[0], outputStream.toByteArray(), "Buffer should remain empty");
    }

    @Test
    void testWriteBytesAfterReset() {
        // Populate, then reset
        outputStream.writeBytes(new byte[] { 5, 6, 7 });
        outputStream.reset();
        // Write new data after reset
        byte[] newData = { 9, 10, 11 };
        outputStream.writeBytes(newData);
        assertEquals(newData.length, outputStream.size(), "Size after reset and new write should reflect only the new data");
        assertArrayEquals(newData, outputStream.toByteArray(), "Buffer should contain only the bytes written after reset");
    }

    @Test
    void testWriteBytesNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> outputStream.writeBytes(null), "Passing null should result in a NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Normal operation – parameterized
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalOperationProvider")
    void testWriteBytesNormalOperation(String description, byte[] input, byte[] expected) {
        // Act
        outputStream.writeBytes(input);
        // Assert
        assertEquals(expected.length, outputStream.size(), "Size should match the number of bytes written");
        assertArrayEquals(expected, outputStream.toByteArray(), "Buffer content must be exactly the bytes supplied");
    }

    @Test
    void testWriteBytesAppendsWhenCalledTwice() {
        // First call
        outputStream.writeBytes(new byte[] { 1, 2, 3 });
        // Second call (as described in the third normal‑operation case)
        outputStream.writeBytes(new byte[] { 4, 5 });
        byte[] expected = new byte[] { 1, 2, 3, 4, 5 };
        assertEquals(expected.length, outputStream.size(), "Size after two writes should be the sum of both arrays");
        assertArrayEquals(expected, outputStream.toByteArray(), "Buffer must contain the concatenated bytes");
    }

    /* -------------------------------------------------------------
     *  Buffer expansion – large array (100 bytes)
     * ------------------------------------------------------------- */
    @Test
    void testWriteBytesLargeArrayTriggersBufferExpansion() {
        // Build a 0‑99 byte array
        byte[] large = new byte[100];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) i;
        }
        outputStream.writeBytes(large);
        assertEquals(100, outputStream.size(), "Size should reflect the number of bytes written");
        assertArrayEquals(large, outputStream.toByteArray(), "All bytes must be stored in the original order");
    }

    /* -------------------------------------------------------------
     *  Post‑close behaviour – OutputStream contract allows writes after close
     * ------------------------------------------------------------- */
    @Test
    void testWriteBytesAfterClose() throws IOException {
        // close first
        outputStream.close();
        byte[] data = { 9, 8, 7 };
        // should still work
        outputStream.writeBytes(data);
        assertEquals(data.length, outputStream.size(), "Size after writeBytes post‑close should be updated");
        assertArrayEquals(data, outputStream.toByteArray(), "Buffer must contain the bytes written after close");
    }

    /* -------------------------------------------------------------
     *  Source array immutability – ensure defensive copy
     * ------------------------------------------------------------- */
    @Test
    void testWriteBytesDoesNotHoldReferenceToSourceArray() {
        byte[] source = { 1, 2, 3 };
        outputStream.writeBytes(source);
        // Mutate the original array after the call
        source[0] = 9;
        source[1] = 9;
        source[2] = 9;
        byte[] expected = { 1, 2, 3 };
        assertArrayEquals(expected, outputStream.toByteArray(), "Stream content must remain unchanged after the source array is mutated");
    }
}
