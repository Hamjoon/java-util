package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FastByteArrayInputStream_markSupported_Test {

    /**
     * Supplies the different scenarios required by the JSON test cases.
     *
     * @return a stream of arguments: description + operation.
     */
    static Stream<Arguments> markSupportedScenarios() {
        return Stream.of(// 1. after reading a single byte
        arguments("after reading a single byte", (Consumer<FastByteArrayInputStream>) s -> {
            try {
                s.read();
            } catch (IOException ignored) {
            }
        }), // 2. after reading into a buffer
        arguments("after reading into a byte array", (Consumer<FastByteArrayInputStream>) s -> {
            try {
                byte[] buf = new byte[2];
                s.read(buf, 0, buf.length);
            } catch (IOException ignored) {
            }
        }), // 3. after calling mark()
        arguments("after calling mark()", (Consumer<FastByteArrayInputStream>) s -> s.mark(10)), // 4. after calling reset()
        arguments("after calling reset()", (Consumer<FastByteArrayInputStream>) s -> {
            s.mark(10);
            s.reset();
        }), // 5. after multiple mark/reset cycles
        arguments("after multiple mark/reset cycles", (Consumer<FastByteArrayInputStream>) s -> {
            s.mark(5);
            s.reset();
            s.mark(3);
            s.reset();
            s.mark(8);
            s.reset();
        }), // 6. after close()
        arguments("after close()", (Consumer<FastByteArrayInputStream>) s -> {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }), // 7. after skip()
        arguments("after skip()", (Consumer<FastByteArrayInputStream>) s -> {
            try {
                s.skip(2);
            } catch (IOException ignored) {
            }
        }), // 8. after reading to end of stream
        arguments("after reading to end of stream", (Consumer<FastByteArrayInputStream>) s -> {
            try {
                while (s.read() != -1) {
                }
            } catch (IOException ignored) {
            }
        }), // 9. after calling mark() with a very large readLimit
        arguments("after calling mark() with large readLimit", (Consumer<FastByteArrayInputStream>) s -> s.mark(Integer.MAX_VALUE)), // 10. after calling mark() with zero readLimit
        arguments("after calling mark() with zero readLimit", (Consumer<FastByteArrayInputStream>) s -> s.mark(0)));
    }

    @Test
    void markSupported_returnsTrue() {
        // Given: a FastByteArrayInputStream with any non‑null buffer.
        byte[] data = { 0, 1, 2 };
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        // When / Then: markSupported must always return true.
        assertTrue(stream.markSupported(), "markSupported should always return true for a fresh instance");
    }

    /**
     * Parameterised test that runs {@link FastByteArrayInputStream#markSupported()}
     * after a series of state‑changing operations.
     *
     * Each argument supplies a description (used as the test name) and a {@link Consumer}
     * that performs the operation(s) on the stream before the assertion.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("markSupportedScenarios")
    void markSupported_afterVariousOperations(String description, Consumer<FastByteArrayInputStream> operation) {
        // Given a stream with a small deterministic buffer.
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[] { 0, 1, 2, 3, 4 });
        // Perform the scenario‑specific operation(s).
        operation.accept(stream);
        // The contract: markSupported must still return true.
        assertTrue(stream.markSupported(), () -> "markSupported should return true " + description);
    }
}
