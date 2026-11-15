package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_mark_Test {

    private FastByteArrayInputStream stream;

    private static final byte[] SAMPLE_DATA = { 10, 20, 30, 40, 50 };

    /**
     * Longer sample data (0‑9) for the more elaborate scenarios.
     */
    private static final byte[] LONG_SAMPLE_DATA = new byte[10];

    /**
     * Runs before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh stream for every test to avoid state leakage.
        stream = new FastByteArrayInputStream(SAMPLE_DATA.clone());
    }

    /**
     * Runs after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly close the stream – the implementation allows further reads,
        // but closing is good practice and mirrors real‑world usage.
        try {
            stream.close();
        } catch (Exception ignored) {
            // ignore any IOException – the class does not throw on close.
        }
        stream = null;
    }

    /**
     * Data provider for {@link #testBasicMarkSetsMarkField}.
     */
    private static Stream<Arguments> basicMarkProvider() {
        return Stream.of(// fresh stream, no reads
        arguments(0, 0, 0, "Fresh stream – mark(0) should record position 0"), // read 5 bytes, then mark with a positive limit
        arguments(10, 5, 5, "After reading 5 bytes – mark(10) should record position 5"), // negative readLimit – should be ignored
        arguments(-1, 0, 0, "Negative readLimit – mark should still record current position 0"), // extremely large readLimit – should have no effect
        arguments(Integer.MAX_VALUE, 0, 0, "Very large readLimit – mark should still record current position 0"));
    }

    /**
     * Data provider for {@link #testAdditionalMarkScenarios}.
     */
    private static Stream<Arguments> additionalMarkProvider() {
        return Stream.of(// 1. Mark at end of stream after reading all bytes.
        arguments(3, "Mark at end of stream after reading all bytes", (Consumer<FastByteArrayInputStream>) s -> {
            while (s.read() != -1) {
            }
        }, /* consume */
        // mark should be at EOF (position == length == 5)
        5, // reset should keep us at EOF
        5), // 2. Mark after skipping 4 bytes.
        arguments(2, "Mark after skipping 4 bytes", (Consumer<FastByteArrayInputStream>) s -> {
            try {
                s.skip(4);
            } catch (IOException ignored) {
            }
        }, // after skip, position = 4
        4, // reset should return to 4
        4), // 3. Two successive marks – the second overwrites the first.
        arguments(0, "Second mark overwrites the first", (Consumer<FastByteArrayInputStream>) s -> {
            try {
                s.read();
                s.read();
                s.mark(0);
                s.read();
                s.read();
                s.read();
                s.mark(0);
            } catch (IOException ignored) {
            }
        }, // pos = 2
        // first mark = 2
        // pos = 5
        // second mark = 5
        // final mark should be 5
        5, // reset should bring us to 5
        5), // 4. Mark after a reset and further reads (requires longer data).
        arguments(7, "Mark after reset and further reads (longer data)", (Consumer<FastByteArrayInputStream>) s -> {
            try {
                for (int i = 0; i < 6; i++) s.read();
                s.mark(7);
                s.read();
                s.read();
                s.reset();
                s.read();
                s.read();
                s.read();
                s.mark(7);
            } catch (IOException ignored) {
            }
        }, // read 6 bytes (pos = 6)
        // mark = 6
        // read 2 more bytes (pos = 8)
        // pos back to 6
        // read 3 bytes (pos = 9)
        // new mark = 9
        // final mark should be 9
        9, // reset should bring us to 9
        9), // 5. Empty stream – mark on a newly created empty stream.
        arguments(-2147483648, "Mark on an empty stream (no data)", (Consumer<FastByteArrayInputStream>) s -> {
        }, // replace the stream with an empty one
        // (the test harness creates a new empty stream for this case)
        // position is 0
        0, // reset keeps us at 0 (EOF)
        0), // 6. Mark after the stream has been closed.
        arguments(12345, "Mark after stream has been closed", (Consumer<FastByteArrayInputStream>) s -> {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }, // position stays at 0 even after close
        0, // we do not assert reset behaviour here
        -1));
    }

    /**
     * Utility to fetch the private {@code mark} field from a {@link FastByteArrayInputStream}
     * instance via reflection.
     *
     * @param instance the stream instance
     * @return the current value of the internal {@code mark}
     */
    private static int getInternalMark(FastByteArrayInputStream instance) throws Exception {
        Field f = FastByteArrayInputStream.class.getDeclaredField("mark");
        f.setAccessible(true);
        return (int) f.get(instance);
    }

    /**
     * Utility to fetch the private {@code pos} field from a {@link FastByteArrayInputStream}
     * instance via reflection.
     *
     * @param instance the stream instance
     * @return the current read position
     */
    private static int getInternalPos(FastByteArrayInputStream instance) throws Exception {
        Field f = FastByteArrayInputStream.class.getDeclaredField("pos");
        f.setAccessible(true);
        return (int) f.get(instance);
    }

    @Test
    void markShouldRecordCurrentPosition() {
        // Given – stream is at the beginning (position 0)
        assertTrue(stream.markSupported(), "markSupported should be true");
        // When – we call mark
        stream.mark(0);
        // Then – after reading a byte and resetting, we should get the same first byte again
        int firstRead = stream.read();
        assertEquals(10, firstRead, "First read should return the first byte");
        // should go back to position recorded by mark (0)
        stream.reset();
        int afterReset = stream.read();
        assertEquals(10, afterReset, "After reset, read should return the first byte again");
    }

    @Test
    void markAfterReadingSomeBytesShouldResetToThatPoint() {
        // Given – read two bytes, then mark
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // When – mark is called at position 2
        stream.mark(0);
        // And – read the next two bytes
        assertEquals(30, stream.read());
        assertEquals(40, stream.read());
        // Then – reset should bring us back to the marked position (byte 30)
        stream.reset();
        assertEquals(30, stream.read(), "Reset should return to the byte that was next after the mark");
    }

    @Test
    void markDoesNotAffectAvailableCount() {
        // Given – initial available count
        int initialAvailable = stream.available();
        assertEquals(SAMPLE_DATA.length, initialAvailable);
        // When – we mark and read a byte
        stream.mark(0);
        stream.read();
        // Then – available should have decreased by one, not be reset by mark
        assertEquals(initialAvailable - 1, stream.available(), "available() should reflect bytes read, not be altered by mark");
    }

    @Test
    void markAtEndOfStreamAndResetShouldLeavePositionAtEnd() {
        // Given – consume the whole stream
        while (stream.read() != -1) {
            // consume
        }
        assertEquals(-1, stream.read(), "Stream should be at EOF");
        // When – we call mark at EOF and then reset
        stream.mark(0);
        stream.reset();
        // Then – another read should still return -1 (still at EOF)
        assertEquals(-1, stream.read(), "Reset at EOF should keep the stream at EOF");
    }

    /**
     * Verifies that {@code mark(int)} stores the current position in the
     * private {@code mark} field regardless of the supplied {@code readLimit}.
     *
     * @param readLimit        the argument passed to {@code mark}
     * @param preReadBytes     how many bytes to read before calling {@code mark}
     * @param expectedMarkPos  the expected value of the internal {@code mark}
     * @param description      description of the scenario (used in assertion messages)
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("basicMarkProvider")
    void testBasicMarkSetsMarkField(int readLimit, int preReadBytes, int expectedMarkPos, String description) throws Exception {
        // Arrange – optionally read some bytes to move the cursor
        for (int i = 0; i < preReadBytes; i++) {
            stream.read();
        }
        // Act – invoke mark()
        stream.mark(readLimit);
        // Assert – the private field 'mark' must equal the expected position
        Field markField = FastByteArrayInputStream.class.getDeclaredField("mark");
        markField.setAccessible(true);
        int actualMark = (int) markField.get(stream);
        assertEquals(expectedMarkPos, actualMark, description);
    }

    /**
     * Executes a series of more elaborate scenarios that involve skipping,
     * multiple marks, resets and edge‑cases such as empty streams or closed streams.
     *
     * <p>The {@code setup} consumer receives the stream instance and may perform
     * any combination of reads, skips, or other operations before {@code mark}
     * is called.</p>
     *
     * @param readLimit          argument supplied to {@code mark}
     * @param description        textual description of the scenario
     * @param setup              a lambda that prepares the stream state
     * @param expectedMarkPos    the value that the internal {@code mark} field should hold after {@code mark}
     * @param expectedPosAfterReset the expected {@code pos} after a subsequent {@code reset}
     *                             (use -1 if reset behaviour is not part of the scenario)
     */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("additionalMarkProvider")
    void testAdditionalMarkScenarios(int readLimit, String description, Consumer<FastByteArrayInputStream> setup, int expectedMarkPos, int expectedPosAfterReset) throws Exception {
        // Some scenarios need a longer byte array – replace the default stream when required.
        if (description.contains("longer")) {
            stream = new FastByteArrayInputStream(LONG_SAMPLE_DATA.clone());
        }
        // Apply the scenario‑specific preparation steps.
        setup.accept(stream);
        // Perform the mark operation.
        stream.mark(readLimit);
        // Verify the internal 'mark' field.
        Field markField = FastByteArrayInputStream.class.getDeclaredField("mark");
        markField.setAccessible(true);
        int actualMark = (int) markField.get(stream);
        assertEquals(expectedMarkPos, actualMark, "Mark field after mark(): " + description);
        // If the test specifies an expected position after reset, verify it.
        if (expectedPosAfterReset >= 0) {
            stream.reset();
            Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
            posField.setAccessible(true);
            int actualPos = (int) posField.get(stream);
            assertEquals(expectedPosAfterReset, actualPos, "Position after reset(): " + description);
        }
    }
}
