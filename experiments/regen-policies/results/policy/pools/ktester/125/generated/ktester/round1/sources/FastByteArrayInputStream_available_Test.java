package com.cedarsoftware.util;

import java.io.IOException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_available_Test {

    private FastByteArrayInputStream stream;

    private byte[] data;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh stream before each test
        data = new byte[] { 10, 20, 30, 40, 50 };
        stream = new FastByteArrayInputStream(data);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
        stream = null;
        data = null;
    }

    private static Stream<Arguments> provideSimpleAvailableScenarios() {
        return Stream.of(// Empty array → 0
        Arguments.of("Empty array should yield 0 available bytes", new byte[0], 0, false, 0), // Fresh 5‑byte array → 5
        Arguments.of("Fresh 5‑byte array should report 5 available bytes", new byte[] { 1, 2, 3, 4, 5 }, 0, false, 5), // After reading 3 bytes from a 5‑byte array → 2
        Arguments.of("After reading 3 bytes from a 5‑byte array, available() should be 2", new byte[] { 1, 2, 3, 4, 5 }, 3, false, 2), // After reading all bytes → 0
        Arguments.of("After consuming the whole stream, available() must be 0", new byte[] { 1, 2, 3, 4, 5 }, 5, false, 0), // Manually set pos == count on a non‑empty array → 0
        Arguments.of("When pos is manually set to count, available() must be 0", new byte[] { 1, 2, 3 }, 0, true, 0));
    }

    private static Stream<Arguments> provideOperationAvailableScenarios() {
        return Stream.of(// skip(4) on 10‑byte array → 6
        Arguments.of("skip(4) on a 10‑byte array leaves 6 bytes available", createSequentialArray(10), "skip", // opParam1 = bytes to skip
        4, 0, 6), // skip(15) on 10‑byte array → 0
        Arguments.of("skip(15) on a 10‑byte array caps at end, leaving 0 available", createSequentialArray(10), "skip", 15, 0, 0), // read(buf,0,3) on 8‑byte array → 5
        Arguments.of("read(buf,0,3) consumes 3 bytes of an 8‑byte array, leaving 5", createSequentialArray(8), "readArray", 3, 0, 5), // read(buf,0,5) then read(buf,0,10) on 8‑byte array → 0
        Arguments.of("read 5 then attempt to read 10 on an 8‑byte array consumes all bytes, leaving 0", createSequentialArray(8), "readArrayTwice", // first read length
        5, // second read length (exceeds remaining)
        10, 0), // mark/reset scenario on 6‑byte array → 4
        Arguments.of("mark after 2 bytes, read 2 more, reset → 4 bytes available", createSequentialArray(6), "markReset", // bytes read before mark
        2, // bytes read after mark before reset
        2, 4), // read until EOF on 5‑byte array → 0
        Arguments.of("read until EOF on a 5‑byte array leaves 0 available", createSequentialArray(5), "readUntilEOF", 0, 0, 0), // read(buf,0,0) on 5‑byte array → 5
        Arguments.of("read with length 0 does not consume bytes, available stays 5", createSequentialArray(5), "readZero", 0, 0, 5), // close on 3‑byte array → 3
        Arguments.of("close() does not affect state; available remains 3", createSequentialArray(3), "close", 0, 0, 3));
    }

    /**
     * Helper to create a byte array {@code [0,1,2,...,size-1]}
     */
    private static byte[] createSequentialArray(int size) {
        byte[] arr = new byte[size];
        for (int i = 0; i < size; i++) {
            arr[i] = (byte) i;
        }
        return arr;
    }

    @Test
    void availableInitiallyEqualsLength() {
        assertEquals(data.length, stream.available(), "available() should return the total number of bytes before any read operation");
    }

    @Test
    void availableDecreasesAfterRead() throws IOException {
        // reads 10
        int first = stream.read();
        assertEquals(10, first);
        assertEquals(data.length - 1, stream.available(), "available() should decrease by one after reading a single byte");
    }

    @Test
    void availableBecomesZeroAfterReadingAllBytes() throws IOException {
        for (int i = 0; i < data.length; i++) {
            assertNotEquals(-1, stream.read(), "read() should return a byte while data remains");
        }
        assertEquals(-1, stream.read(), "End of stream should be indicated by -1");
        assertEquals(0, stream.available(), "available() must be zero after the whole content has been consumed");
    }

    @Test
    void availableReflectsSkipOperation() throws IOException {
        // skips 10 and 20
        long skipped = stream.skip(2);
        assertEquals(2, skipped);
        assertEquals(data.length - 2, stream.available(), "available() should reflect the number of bytes left after a successful skip");
    }

    @Test
    void availableRemainsUnchangedAfterClose() throws IOException {
        // close is a no‑op for this class
        stream.close();
        assertEquals(data.length, stream.available(), "available() should still report the full length after close, as the stream remains readable");
    }

    @Test
    void availableOnEmptyStreamIsZero() {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        assertEquals(0, empty.available(), "available() must be zero when the underlying buffer is empty");
    }

    @Test
    void availableAfterMarkAndReset() throws IOException {
        assertTrue(stream.markSupported(), "markSupported() must be true");
        stream.mark(0);
        // consume one byte
        stream.read();
        // consume second byte
        stream.read();
        // go back to the mark
        stream.reset();
        assertEquals(data.length, stream.available(), "available() should be restored to the original count after reset to the mark");
    }

    /* --------------------------------------------------------------
     *  Group 1 – Simple available() expectations
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSimpleAvailableScenarios")
    void testSimpleAvailableScenarios(String description, byte[] initData, int bytesToRead, boolean forcePosToCount, int expected) throws Exception {
        FastByteArrayInputStream s = new FastByteArrayInputStream(initData);
        // Consume the requested number of bytes, if any
        for (int i = 0; i < bytesToRead; i++) {
            assertNotEquals(-1, s.read(), "read() should not return -1 before the expected EOF");
        }
        // For the special case where we need to set pos == count manually
        if (forcePosToCount) {
            Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
            posField.setAccessible(true);
            // count is a private final field; we obtain it via reflection as well
            Field countField = FastByteArrayInputStream.class.getDeclaredField("count");
            countField.setAccessible(true);
            int count = (int) countField.get(s);
            posField.setInt(s, count);
        }
        assertEquals(expected, s.available(), () -> "available() mismatch: " + description);
    }

    /* --------------------------------------------------------------
     *  Group 2 – available() after various stream operations
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideOperationAvailableScenarios")
    void testAvailableAfterOperations(String description, byte[] initData, String operation, int opParam1, int opParam2, int expected) throws IOException {
        FastByteArrayInputStream s = new FastByteArrayInputStream(initData);
        switch(operation) {
            case "skip":
                long skipped = s.skip(opParam1);
                assertEquals(Math.min(opParam1, initData.length), skipped, "skip() should return the actual number of bytes skipped");
                break;
            case "readArray":
                byte[] buf = new byte[opParam1];
                int read = s.read(buf, 0, opParam1);
                assertEquals(opParam1, read, "read(buf,0,len) should read the requested number of bytes");
                break;
            case "readArrayTwice":
                // first read
                byte[] first = new byte[opParam1];
                int r1 = s.read(first, 0, opParam1);
                assertEquals(opParam1, r1, "first read length mismatch");
                // second read (may be smaller than requested)
                byte[] second = new byte[opParam2];
                int r2 = s.read(second, 0, opParam2);
                // r2 can be less than opParam2 if not enough bytes remain
                assertTrue(r2 >= 0 && r2 <= opParam2, "second read should return a non‑negative number not exceeding the request");
                break;
            case "markReset":
                // read 'opParam1' bytes, mark, read 'opParam2' bytes, reset
                for (int i = 0; i < opParam1; i++) {
                    assertNotEquals(-1, s.read(), "pre‑mark read failed");
                }
                s.mark(0);
                for (int i = 0; i < opParam2; i++) {
                    assertNotEquals(-1, s.read(), "post‑mark read failed");
                }
                s.reset();
                break;
            case "readUntilEOF":
                int b;
                do {
                    b = s.read();
                } while (b != -1);
                break;
            case "readZero":
                byte[] zeroBuf = new byte[10];
                int zeroRead = s.read(zeroBuf, 0, 0);
                assertEquals(0, zeroRead, "read with length 0 must return 0");
                break;
            case "close":
                // No‑op for this class
                s.close();
                break;
            case "none":
                // No additional operation
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
        assertEquals(expected, s.available(), () -> "available() mismatch after operation: " + description);
    }
}
