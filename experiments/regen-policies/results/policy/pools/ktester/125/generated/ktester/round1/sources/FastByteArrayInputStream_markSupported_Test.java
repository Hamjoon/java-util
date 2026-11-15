package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.stream.Stream;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_markSupported_Test {

    /**
     * Instance used by the tests.
     */
    private FastByteArrayInputStream stream;

    /**
     * Runs before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Initialise the stream with a non‑empty buffer so that the object is fully functional.
        byte[] data = { 10, 20, 30, 40, 50 };
        stream = new FastByteArrayInputStream(data);
    }

    /**
     * Runs after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the garbage collector in long test suites.
        stream = null;
    }

    /**
     * Supplies the different operations for {@link #markSupportedAlwaysTrue(String, Consumer)}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> operationProvider() {
        return Stream.of(// 1. Fresh stream – no operation performed
        org.junit.jupiter.params.provider.Arguments.of("newly created stream", (Consumer<FastByteArrayInputStream>) s -> {
        }), // 2. After invoking mark(int) with a positive limit
        org.junit.jupiter.params.provider.Arguments.of("after mark(int) with positive limit", (Consumer<FastByteArrayInputStream>) s -> s.mark(10)), // 3. After invoking reset() (should be a no‑op if no mark has been set)
        org.junit.jupiter.params.provider.Arguments.of("after reset()", (Consumer<FastByteArrayInputStream>) s -> s.reset()), // 4. After reading a couple of bytes
        org.junit.jupiter.params.provider.Arguments.of("after reading some bytes", (Consumer<FastByteArrayInputStream>) s -> {
            s.read();
            s.read();
        }), // 5. After skipping bytes
        org.junit.jupiter.params.provider.Arguments.of("after skipping bytes", (Consumer<FastByteArrayInputStream>) s -> s.skip(2L)));
    }

    /**
     * The method under test should always return {@code true}.
     */
    @Test
    void markSupportedReturnsTrue() {
        assertTrue(stream.markSupported(), "FastByteArrayInputStream.markSupported() must always return true");
    }

    /**
     * Verify that the result stays {@code true} after reading some bytes.
     */
    @Test
    void markSupportedRemainsTrueAfterRead() {
        // consume a couple of bytes
        stream.read();
        stream.read();
        assertTrue(stream.markSupported(), "markSupported() should still return true after reading bytes");
    }

    /**
     * Verify that the result stays {@code true} after a skip operation.
     */
    @Test
    void markSupportedRemainsTrueAfterSkip() {
        stream.skip(2L);
        assertTrue(stream.markSupported(), "markSupported() should still return true after skip()");
    }

    /**
     * Verify that the result stays {@code true} after the stream is closed.
     */
    @Test
    void markSupportedRemainsTrueAfterClose() throws java.io.IOException {
        stream.close();
        assertTrue(stream.markSupported(), "markSupported() should still return true after close()");
    }

    /**
     * Verify that the result stays {@code true} after a mark/reset cycle.
     */
    @Test
    void markSupportedRemainsTrueAfterMarkReset() {
        stream.mark(0);
        // move position
        stream.read();
        // back to mark
        stream.reset();
        assertTrue(stream.markSupported(), "markSupported() should still return true after mark/reset");
    }

    /**
     * Parameterized test that executes a series of state‑changing operations
     * on the stream and then verifies that {@code markSupported()} still returns {@code true}.
     *
     * <p>The {@code operationProvider()} supplies a {@link Consumer} for each scenario:
     * <ul>
     *   <li>No operation (fresh stream)</li>
     *   <li>Calling {@code mark(int)}</li>
     *   <li>Calling {@code reset()}</li>
     *   <li>Reading a couple of bytes</li>
     *   <li>Skipping bytes</li>
     * </ul>
     * </p>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("operationProvider")
    void markSupportedAlwaysTrue(String description, Consumer<FastByteArrayInputStream> operation) {
        // Apply the state‑changing operation (if any)
        operation.accept(stream);
        // The method under test must always return true
        assertTrue(stream.markSupported(), "FastByteArrayInputStream.markSupported() must always return true – " + description);
    }
}
