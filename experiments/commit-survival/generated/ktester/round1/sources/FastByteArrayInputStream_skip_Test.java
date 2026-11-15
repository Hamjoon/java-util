package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_skip_Test {

    private static final byte[] DEFAULT_DATA = { 1, 2, 3, 4, 5 };

    private FastByteArrayInputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh stream for each test to guarantee isolation.
        // Clone the array to avoid accidental mutation across tests.
        stream = new FastByteArrayInputStream(DEFAULT_DATA.clone());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – not strictly required but keeps the pattern clear.
        stream = null;
    }

    /**
     * Creates a {@link FastByteArrayInputStream} whose internal {@code count}
     * and {@code pos} fields are set to the supplied values via reflection.
     *
     * @param count the value to assign to the private {@code count} field
     * @param pos   the value to assign to the private {@code pos} field
     * @return a fully‑initialised stream ready for testing
     */
    private FastByteArrayInputStream createStream(int count, int pos) throws Exception {
        // The actual byte array can be any size – {@code skip} does not read from it.
        // Use a minimal non‑null array to satisfy the constructor.
        byte[] data = new byte[Math.max(1, Math.min(count, 1024))];
        FastByteArrayInputStream s = new FastByteArrayInputStream(data);
        setField(s, "count", count);
        setField(s, "pos", pos);
        return s;
    }

    /**
     * Retrieves the current value of the private {@code pos} field.
     */
    private long getPos(FastByteArrayInputStream s) throws Exception {
        return (int) getField(s, "pos");
    }

    private void setField(Object target, String name, int value) throws Exception {
        Field f = FastByteArrayInputStream.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    private Object getField(Object target, String name) throws Exception {
        Field f = FastByteArrayInputStream.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static Stream<Arguments> branchCoverageCases() {
        return Stream.of(// n=5, remaining 10, pos=0, count=10
        arguments(5L, 0, 10, 5L, 5L, "Skip a positive number smaller than the remaining bytes."), // n=10, remaining 10, pos=0, count=10
        arguments(10L, 0, 10, 10L, 10L, "Skip exactly the number of remaining bytes."), // n=15, remaining 10, pos=0, count=10
        arguments(15L, 0, 10, 10L, 10L, "Skip a number larger than the remaining bytes – clamp to remaining."), // n=-3, pos=0, count=10
        arguments(-3L, 0, 10, 0L, 0L, "Skip with a negative value – treated as zero."), // n=0, pos=0, count=10
        arguments(0L, 0, 10, 0L, 0L, "Skip zero bytes – no movement."), // n=5, pos=10 (already at end), count=10
        arguments(5L, 10, 10, 0L, 10L, "Skip when the stream is already exhausted – always returns 0."));
    }

    private static Stream<Arguments> additionalCases() {
        return Stream.of(// Empty stream, count=0
        arguments(5L, 0, 0, 0L, 0L, "Skip on an empty stream – nothing can be skipped."), // count=10, pos=3, n=4
        arguments(4L, 3, 10, 4L, 7L, "Skip 4 bytes when 7 remain (pos=3, count=10)."), // count=10, pos=3, n=7
        arguments(7L, 3, 10, 7L, 10L, "Skip exactly the remaining bytes."), // count=10, pos=3, n=10 (beyond remaining)
        arguments(10L, 3, 10, 7L, 10L, "Skip beyond remaining – clamp to 7 bytes."), // count=10, pos=3, n=0
        arguments(0L, 3, 10, 0L, 3L, "Skip zero bytes – position unchanged."), // count=10, pos=3, n=-5
        arguments(-5L, 3, 10, 0L, 3L, "Negative skip – ignored."), // Stream exhausted, count=10, pos=10, huge n
        arguments(Long.MAX_VALUE, 10, 10, 0L, 10L, "Huge skip on exhausted stream – returns 0."), // Fresh stream, count=10, pos=0, huge n
        arguments(Long.MAX_VALUE, 0, 10, 10L, 10L, "Huge skip on non‑exhausted stream – clamped to 10."), // Very large stream, count=Integer.MAX_VALUE, pos=0, n=Integer.MAX_VALUE
        arguments((long) Integer.MAX_VALUE, 0, Integer.MAX_VALUE, (long) Integer.MAX_VALUE, (long) Integer.MAX_VALUE, "Skip entire large stream (Integer.MAX_VALUE bytes)."), // Negative Long.MIN_VALUE on normal stream
        arguments(Long.MIN_VALUE, 0, 10, 0L, 0L, "Extremely negative skip – treated as zero."));
    }

    @Test
    void skipWithinBounds() {
        // given
        long toSkip = 2L;
        // when
        long skipped = stream.skip(toSkip);
        // then
        assertEquals(toSkip, skipped, "skip should return the number of bytes actually skipped");
        assertEquals(3, stream.read(), "next byte after skip should be the third element");
    }

    @Test
    void skipZero() {
        // given
        long toSkip = 0L;
        // when
        long skipped = stream.skip(toSkip);
        // then
        assertEquals(0L, skipped, "skipping zero should return zero");
        assertEquals(1, stream.read(), "stream position must remain unchanged");
    }

    @Test
    void skipNegative() {
        // given
        long toSkip = -5L;
        // when
        long skipped = stream.skip(toSkip);
        // then
        assertEquals(0L, skipped, "negative skip should be treated as zero");
        assertEquals(1, stream.read(), "stream position must remain unchanged after negative skip");
    }

    @Test
    void skipBeyondEndOfStream() {
        // given
        // larger than the number of available bytes (5)
        long toSkip = 10L;
        // when
        long skipped = stream.skip(toSkip);
        // then
        assertEquals(5L, skipped, "skip should cap at the number of remaining bytes");
        assertEquals(-1, stream.read(), "reading after skipping past the end should return -1");
    }

    @Test
    void skipUpdatesAvailable() {
        // given
        assertEquals(5, stream.available(), "initial available bytes");
        // when
        long skipped = stream.skip(3L);
        // then
        assertEquals(3L, skipped);
        assertEquals(2, stream.available(), "available should reflect the new position after skip");
    }

    /**
     * Branch‑coverage group – exercises the different branches inside {@code skip}.
     */
    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource("branchCoverageCases")
    void skipBranchCoverage(long n, int initialPos, int initialCount, long expectedSkipped, long expectedPosAfter, String description) throws Exception {
        FastByteArrayInputStream s = createStream(initialCount, initialPos);
        long actualSkipped = s.skip(n);
        long actualPos = getPos(s);
        assertEquals(expectedSkipped, actualSkipped, () -> "Returned skip amount differs – " + description);
        assertEquals(expectedPosAfter, actualPos, () -> "Internal position after skip differs – " + description);
    }

    /**
     * Additional edge‑case group – covers empty streams, large skips, negative
     * values, and manually‑positioned streams.
     */
    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource("additionalCases")
    void skipAdditionalCases(long n, int initialPos, int initialCount, long expectedSkipped, long expectedPosAfter, String description) throws Exception {
        FastByteArrayInputStream s = createStream(initialCount, initialPos);
        long actualSkipped = s.skip(n);
        long actualPos = getPos(s);
        assertEquals(expectedSkipped, actualSkipped, () -> "Returned skip amount differs – " + description);
        assertEquals(expectedPosAfter, actualPos, () -> "Internal position after skip differs – " + description);
    }
}
