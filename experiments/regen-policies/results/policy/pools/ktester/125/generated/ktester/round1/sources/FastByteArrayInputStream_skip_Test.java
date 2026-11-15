package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_skip_Test {

    private FastByteArrayInputStream stream;

    private byte[] data;

    @BeforeEach
    void setupBeforeEach() {
        // default data used by most tests; individual tests may replace it
        data = new byte[] { 1, 2, 3, 4, 5 };
        stream = new FastByteArrayInputStream(data);
    }

    @AfterEach
    void teardownAfterEach() {
        // help GC and make sure each test starts clean
        stream = null;
        data = null;
    }

    /**
     * Provides arguments for the branch‑coverage group.
     * Each argument contains:
     *   - initialPos : starting position inside the stream
     *   - count      : total number of bytes the stream pretends to contain
     *   - n          : value passed to {@code skip}
     *   - expectedSkipped : value that {@code skip} should return
     *   - expectedPos    : position after the call (used for verification)
     */
    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// Negative skip while bytes are available
        Arguments.of(0, 10, -3L, 0L, 0), // Zero skip while bytes are available
        Arguments.of(0, 10, 0L, 0L, 0), // Positive n smaller than remaining bytes
        Arguments.of(2, 10, 5L, 5L, 7), // n exactly equals remaining bytes
        Arguments.of(3, 8, 5L, 5L, 8), // n larger than remaining bytes
        Arguments.of(4, 9, 10L, 5L, 9), // No bytes left to skip (pos == count)
        Arguments.of(7, 7, 3L, 0L, 7));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// Empty stream, positive skip
        Arguments.of(0, 0, 5L, 0L, 0), // Empty stream, negative skip
        Arguments.of(0, 0, -3L, 0L, 0), // Skip far exceeds remaining bytes (Long.MAX_VALUE)
        Arguments.of(0, 10, Long.MAX_VALUE, 10L, 10), // Skip from middle with Long.MAX_VALUE
        Arguments.of(5, 10, Long.MAX_VALUE, 5L, 10), // Skip with Long.MIN_VALUE (large negative)
        Arguments.of(0, 10, Long.MIN_VALUE, 0L, 0), // Only one byte left, n larger
        Arguments.of(9, 10, 5L, 1L, 10), // Skip with Integer.MAX_VALUE (exceeds remaining)
        Arguments.of(0, 10, Integer.MAX_VALUE, 10L, 10), // Stream already at end, negative skip
        Arguments.of(3, 3, -5L, 0L, 3));
    }

    /**
     * Sets a private {@code int} field on the given object.
     *
     * @param target    the object whose field should be modified
     * @param fieldName name of the private field
     * @param value     new value to set
     * @throws Exception if reflection fails
     */
    private static void setPrivateIntField(Object target, String fieldName, int value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    /**
     * Retrieves the value of a private {@code int} field.
     *
     * @param target    the object whose field should be read
     * @param fieldName name of the private field
     * @return the current int value of the field
     * @throws Exception if reflection fails
     */
    private static int getPrivateIntField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.getInt(target);
    }

    @Test
    @DisplayName("Skip a positive number within the stream bounds")
    void testSkipWithinBounds() {
        long skipped = stream.skip(2);
        assertEquals(2L, skipped, "Skipped bytes should equal the requested amount");
        assertEquals(3, stream.read(), "Next byte after skip should be the third element");
    }

    @Test
    @DisplayName("Skip zero bytes – stream position must stay unchanged")
    void testSkipZero() {
        long skipped = stream.skip(0);
        assertEquals(0L, skipped, "Skipping zero should return zero");
        assertEquals(1, stream.read(), "First byte should still be readable");
    }

    @Test
    @DisplayName("Skip a negative number – should behave as skipping zero")
    void testSkipNegative() {
        long skipped = stream.skip(-10);
        assertEquals(0L, skipped, "Negative skip should be treated as zero");
        assertEquals(1, stream.read(), "Stream position must not move on negative skip");
    }

    @Test
    @DisplayName("Skip more bytes than are available – should skip only remaining bytes")
    void testSkipBeyondEnd() {
        // request more than length (5)
        long skipped = stream.skip(10);
        assertEquals(5L, skipped, "Should skip only the remaining bytes");
        assertEquals(-1, stream.read(), "Stream should be at EOF after skipping past the end");
    }

    @Test
    @DisplayName("Skip on an empty stream – should return zero and stay at EOF")
    void testSkipOnEmptyStream() {
        stream = new FastByteArrayInputStream(new byte[0]);
        long skipped = stream.skip(3);
        assertEquals(0L, skipped, "Skipping any amount on empty stream must return zero");
        assertEquals(-1, stream.read(), "Read after skip on empty stream must stay at EOF");
    }

    @Test
    @DisplayName("Skip after some reads – ensure position is correctly updated")
    void testSkipAfterPartialRead() {
        // read first two bytes
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());
        // now skip one byte (the third)
        long skipped = stream.skip(1);
        assertEquals(1L, skipped);
        // next read should be the fourth element
        assertEquals(4, stream.read());
    }

    @ParameterizedTest(name = "[{index}] pos={0}, count={1}, n={2} → skip={3}, finalPos={4}")
    @MethodSource("branchCoverageProvider")
    @DisplayName("FastByteArrayInputStream.skip(long) – branch coverage")
    void testSkipBranchCoverage(int initialPos, int count, long n, long expectedSkipped, int expectedPos) throws Exception {
        // Prepare a byte array of the required length (content is irrelevant)
        byte[] buffer = new byte[count];
        stream = new FastByteArrayInputStream(buffer);
        // Set the private fields via reflection
        setPrivateIntField(stream, "pos", initialPos);
        // 'count' is final but set by the constructor; we already created the correct size array
        long actualSkipped = stream.skip(n);
        assertEquals(expectedSkipped, actualSkipped, () -> String.format("skip(%d) should return %d", n, expectedSkipped));
        int actualPos = getPrivateIntField(stream, "pos");
        assertEquals(expectedPos, actualPos, () -> String.format("After skip(%d) position should be %d", n, expectedPos));
    }

    @ParameterizedTest(name = "[{index}] pos={0}, count={1}, n={2} → skip={3}, finalPos={4}")
    @MethodSource("edgeCaseProvider")
    @DisplayName("FastByteArrayInputStream.skip(long) – additional edge cases")
    void testSkipAdditionalEdgeCases(int initialPos, int count, long n, long expectedSkipped, int expectedPos) throws Exception {
        byte[] buffer = new byte[count];
        stream = new FastByteArrayInputStream(buffer);
        setPrivateIntField(stream, "pos", initialPos);
        long actualSkipped = stream.skip(n);
        assertEquals(expectedSkipped, actualSkipped, () -> String.format("skip(%d) should return %d", n, expectedSkipped));
        int actualPos = getPrivateIntField(stream, "pos");
        assertEquals(expectedPos, actualPos, () -> String.format("After skip(%d) position should be %d", n, expectedPos));
    }
}
