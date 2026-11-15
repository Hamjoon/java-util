package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_writeBytes_Test {

    /**
     * Instance used in each test.
     */
    private FastByteArrayOutputStream outputStream;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for every test to guarantee isolation.
        outputStream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the GC in large test suites.
        outputStream = null;
    }

    /**
     * Provides test data extracted from the JSON description.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>the byte array to write</li>
     *   <li>the expected size after the write</li>
     *   <li>the expected content of the internal buffer</li>
     *   <li>a description used for the test name</li>
     * </ul>
     */
    private static Stream<Arguments> writeBytesProvider() {
        return Stream.of(// Normal behaviour – small non‑empty array
        Arguments.of(new byte[] { 1, 2, 3 }, 3, new byte[] { 1, 2, 3 }, "Writes a small non‑empty byte array"), // Empty array – should be a no‑op
        Arguments.of(new byte[0], 0, new byte[0], "Writes an empty byte array"), // Single zero byte – verifies handling of 0 value
        Arguments.of(new byte[] { 0 }, 1, new byte[] { 0 }, "Writes a single zero byte"), // Mixed signed values – ensures sign is preserved
        Arguments.of(new byte[] { 127, -128, 0, 1, -1 }, 5, new byte[] { 127, -128, 0, 1, -1 }, "Writes a mixture of positive, negative and zero bytes"));
    }

    @Test
    void writeBytes_writesEntireArray() {
        byte[] data = "FastByteArray".getBytes();
        outputStream.writeBytes(data);
        assertEquals(data.length, outputStream.size(), "Size should match the length of the written array");
        assertArrayEquals(data, outputStream.toByteArray(), "The internal buffer should contain exactly the written bytes");
    }

    @Test
    void writeBytes_multipleCalls_concatenatesData() {
        byte[] first = "Hello".getBytes();
        byte[] second = "World".getBytes();
        outputStream.writeBytes(first);
        outputStream.writeBytes(second);
        byte[] expected = "HelloWorld".getBytes();
        assertEquals(expected.length, outputStream.size(), "Size after two calls should be the sum of both arrays");
        assertArrayEquals(expected, outputStream.toByteArray(), "Data should be concatenated in the order of calls");
    }

    @Test
    void writeBytes_emptyArray_hasNoEffect() {
        byte[] empty = new byte[0];
        outputStream.writeBytes(empty);
        assertEquals(0, outputStream.size(), "Writing an empty array must not change the size");
        assertArrayEquals(new byte[0], outputStream.toByteArray(), "The internal buffer should remain empty");
    }

    @Test
    void writeBytes_nullArray_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> outputStream.writeBytes(null), "Passing null should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("writeBytesProvider")
    void writeBytes_variousInputs_correctlyAppended(byte[] input, int expectedSize, byte[] expectedContent, String description) {
        // Act
        outputStream.writeBytes(input);
        // Assert
        assertEquals(expectedSize, outputStream.size(), "Size should match the length of the written array");
        assertArrayEquals(expectedContent, outputStream.toByteArray(), "Internal buffer content must equal the written bytes");
    }

    @Test
    void writeBytes_multipleCalls_appendsData() {
        // First write
        outputStream.writeBytes(new byte[] { 9, 9, 9 });
        // Second write (the case from JSON where we write after a prior write)
        outputStream.writeBytes(new byte[] { 1, 2, 3 });
        byte[] expected = new byte[] { 9, 9, 9, 1, 2, 3 };
        assertEquals(expected.length, outputStream.size(), "Size after two writes must be the sum of both arrays");
        assertArrayEquals(expected, outputStream.toByteArray(), "Data must be concatenated in the order of calls");
    }

    /**
     * The original JSON test case expects the stream to handle an array
     * whose length is close to {@link Integer#MAX_VALUE}.  Allocating such
     * an array would require >2 GB of heap, which is unrealistic for a
     * typical unit‑test environment.  The test is therefore disabled
     * with a clear rationale.
     */
    @Disabled("Disabled to avoid OOM in CI; allocating an array of size Integer.MAX_VALUE is impractical.")
    @Test
    void writeBytes_veryLargeArray_behaviouralContract() {
        // NOTE: This test is illustrative only.
        // slightly smaller to avoid immediate OOM
        int hugeSize = Integer.MAX_VALUE - 8;
        // would normally OOM
        byte[] hugeArray = new byte[hugeSize];
        // Fill with a pattern if we ever enable this test.
        // Arrays.fill(hugeArray, (byte) 1);
        outputStream.writeBytes(hugeArray);
        assertEquals(hugeSize, outputStream.size(), "Size should reflect the full length of the huge array");
        // We cannot realistically compare the whole array; a spot‑check would be enough.
        assertEquals((byte) 1, outputStream.toByteArray()[0], "First byte should match the pattern used to fill the huge array");
    }
}
