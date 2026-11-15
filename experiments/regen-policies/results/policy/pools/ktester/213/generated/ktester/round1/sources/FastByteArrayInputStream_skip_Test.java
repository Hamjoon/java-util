package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_skip_Test {

    private FastByteArrayInputStream stream;

    private byte[] data;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh stream for each test
        data = new byte[] { 10, 20, 30, 40, 50 };
        stream = new FastByteArrayInputStream(data);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required; the stream does not hold external resources
        stream = null;
        data = null;
    }

    /**
     * Helper that sets the private {@code pos} and {@code count} fields via reflection.
     */
    private static void setPosAndCount(FastByteArrayInputStream s, int pos, int count) {
        try {
            Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
            posField.setAccessible(true);
            posField.setInt(s, pos);
            Field countField = FastByteArrayInputStream.class.getDeclaredField("count");
            countField.setAccessible(true);
            countField.setInt(s, count);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// n is positive and smaller than the remaining bytes (count‑pos). Branch: n < k is true, n < 0 is false.
        Arguments.of(4L, 0, 4L, 4), // n equals the remaining bytes. Branch: n < k is false, so k stays as count‑pos.
        Arguments.of(10L, 0, 10L, 10), // n is larger than the remaining bytes. Branch: n < k is false, k is capped to count‑pos.
        Arguments.of(15L, 0, 10L, 10), // n is negative. Branch: n < k is true, then n < 0 is true → k = 0.
        Arguments.of(-5L, 0, 0L, 0), // n is zero. Branch: n < k is true, n < 0 is false → k = n (0).
        Arguments.of(0L, 0, 0L, 0), // pos is pre‑advanced to 3 (e.g., by reading 3 bytes). Remaining = 7, n < k true, n >= 0 → k = n.
        Arguments.of(4L, 3, 4L, 7), // pos is 8, remaining = 2, n > remaining → n < k false, k = remaining.
        Arguments.of(5L, 8, 2L, 10), // pos already equals count (stream exhausted). Remaining = 0, n < k false, so k = 0.
        Arguments.of(3L, 10, 0L, 10), // pos at end, n negative. Remaining = 0, n < k true, n < 0 true → k = 0.
        Arguments.of(-2L, 10, 0L, 10), // n is Long.MAX_VALUE, far larger than remaining bytes (4). Branch: n < k false, k capped to count‑pos.
        Arguments.of(Long.MAX_VALUE, 0, 4L, 4));
    }

    private static Stream<Arguments> emptyStreamProvider() {
        return Stream.of(Arguments.of(5L, 0L, 0), Arguments.of(0L, 0L, 0), Arguments.of(-3L, 0L, 0));
    }

    private static Stream<Arguments> extremeLongProvider() {
        return Stream.of(// Long.MIN_VALUE is a large negative number; method should clamp to 0 (count=10, pos=0)
        Arguments.of(10, 0, Long.MIN_VALUE, 0L, 0), // Same negative extreme with stream half‑consumed (pos=5)
        Arguments.of(10, 5, Long.MIN_VALUE, 0L, 5), // Long.MAX_VALUE far exceeds remaining bytes (7) when count=10, pos=3
        Arguments.of(10, 3, Long.MAX_VALUE, 7L, 10));
    }

    private static Stream<Arguments> justBelowAboveProvider() {
        return Stream.of(// n = 7 (remaining‑1) → skip exactly 7, pos becomes 9
        Arguments.of(7L, 7L, 9), // n = 9 (remaining+1) → skip capped to 8, pos becomes 10
        Arguments.of(9L, 8L, 10));
    }

    @Test
    void skipWithinBounds() {
        // given: a stream positioned at the beginning
        // when: we skip 2 bytes
        long skipped = stream.skip(2);
        // then: exactly 2 bytes are skipped and the next read returns the 3rd element
        assertEquals(2L, skipped, "Skipped count should be 2");
        assertEquals(30, stream.read(), "Next byte after skip should be the 3rd element");
    }

    @Test
    void skipZeroBytes() {
        long skipped = stream.skip(0);
        assertEquals(0L, skipped, "Skipping zero should return 0");
        assertEquals(10, stream.read(), "Stream position should be unchanged");
    }

    @Test
    void skipNegativeBytesTreatedAsZero() {
        long skipped = stream.skip(-5);
        assertEquals(0L, skipped, "Negative skip should be treated as 0");
        assertEquals(10, stream.read(), "Stream position should remain at start");
    }

    @Test
    void skipPastEndOfStream() {
        // given: a stream of length 5
        // when: we request to skip more than available
        long skipped = stream.skip(100);
        // then: it should skip only up to the end and return the actual number skipped
        assertEquals(data.length, skipped, "Should skip exactly the number of remaining bytes");
        assertEquals(-1, stream.read(), "Reading after skipping past end should return -1");
    }

    @Test
    void successiveSkipsAccumulateCorrectly() {
        long first = stream.skip(1);
        long second = stream.skip(2);
        assertAll(() -> assertEquals(1L, first, "First skip should be 1"), () -> assertEquals(2L, second, "Second skip should be 2"), () -> assertEquals(40, stream.read(), "After skipping 1+2 bytes, next byte should be 40"));
    }

    @Test
    void skipWhenStreamAlreadyAtEnd() {
        // Exhaust the stream
        stream.skip(data.length);
        assertEquals(-1, stream.read(), "Stream should be at end");
        // Attempt another skip
        long skipped = stream.skip(10);
        assertEquals(0L, skipped, "Skipping when at end should return 0");
    }

    @ParameterizedTest(name = "[{index}] n={0}, initialPos={1} → skip={2}, finalPos={3}")
    @MethodSource("branchCoverageProvider")
    void skipBranchCoverage(long n, int initialPos, long expectedSkip, int expectedPos) {
        // Arrange: stream of length 10 (we use a dummy array of that size)
        byte[] dummy = new byte[10];
        FastByteArrayInputStream s = new FastByteArrayInputStream(dummy);
        setPosAndCount(s, initialPos, 10);
        // Act
        long actualSkip = s.skip(n);
        // Assert
        assertEquals(expectedSkip, actualSkip, () -> "skip(" + n + ") should return " + expectedSkip);
        // Verify internal position via reflection
        try {
            Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
            posField.setAccessible(true);
            int actualPos = posField.getInt(s);
            assertEquals(expectedPos, actualPos, () -> "after skip, internal pos should be " + expectedPos);
        } catch (Exception e) {
            fail("Reflection error while checking pos", e);
        }
    }

    @ParameterizedTest(name = "[{index}] empty stream, n={0} → skip={1}, pos={2}")
    @MethodSource("emptyStreamProvider")
    void skipEmptyStream(long n, long expectedSkip, int expectedPos) {
        // count = 0
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[0]);
        // No need to modify pos; it starts at 0
        long actual = s.skip(n);
        assertEquals(expectedSkip, actual, "skip on empty stream should return " + expectedSkip);
        // Verify pos stays 0
        try {
            Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
            posField.setAccessible(true);
            int pos = posField.getInt(s);
            assertEquals(expectedPos, pos, "pos after skip on empty stream");
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest(name = "[{index}] count={0}, pos={1}, n={2} → skip={3}, finalPos={4}")
    @MethodSource("extremeLongProvider")
    void skipExtremeLongValues(int count, int initialPos, long n, long expectedSkip, int expectedPos) {
        // Use a dummy array; we will overwrite count/pos via reflection.
        // size 0 or 1 just to satisfy ctor
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[Math.min(count, 1)]);
        setPosAndCount(s, initialPos, count);
        long actual = s.skip(n);
        assertEquals(expectedSkip, actual, "skip(" + n + ") should return " + expectedSkip);
        try {
            Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
            posField.setAccessible(true);
            int pos = posField.getInt(s);
            assertEquals(expectedPos, pos, "final pos");
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void skipLargeIntSizedStream() {
        // Simulate a stream whose logical length is Integer.MAX_VALUE.
        // We cannot allocate such an array, so we create a tiny one and then patch the fields.
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[1]);
        setPosAndCount(s, 0, Integer.MAX_VALUE);
        // skip exactly the whole length
        long n = Integer.MAX_VALUE;
        long actual = s.skip(n);
        assertEquals(n, actual, "Skipping Integer.MAX_VALUE bytes should return the same value");
        try {
            Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
            posField.setAccessible(true);
            int pos = posField.getInt(s);
            assertEquals(Integer.MAX_VALUE, pos, "pos should equal count after full skip");
        } catch (Exception e) {
            fail(e);
        }
    }

    @ParameterizedTest(name = "[{index}] initialPos=2, n={0} → skip={1}, finalPos={2}")
    @MethodSource("justBelowAboveProvider")
    void skipJustBelowAndAboveRemaining(long n, long expectedSkip, int expectedPos) {
        // Stream of length 10, start at position 2 (so 8 bytes remain)
        byte[] dummy = new byte[10];
        FastByteArrayInputStream s = new FastByteArrayInputStream(dummy);
        setPosAndCount(s, 2, 10);
        long actual = s.skip(n);
        assertEquals(expectedSkip, actual, "skip(" + n + ") result");
        try {
            Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
            posField.setAccessible(true);
            int pos = posField.getInt(s);
            assertEquals(expectedPos, pos, "final pos");
        } catch (Exception e) {
            fail(e);
        }
    }
}
