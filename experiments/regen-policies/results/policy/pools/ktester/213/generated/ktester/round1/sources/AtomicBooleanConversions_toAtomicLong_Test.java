package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toAtomicLong_Test {

    @Mock
    private Converter // Converter is not used by the method, but required as a parameter
    mockConverter;

    @Mock
    private Converter // normal mock – not used by the method
    normalMockConverter;

    /**
     * Provides all test scenarios described in the JSON specification.
     *
     * Each {@link org.junit.jupiter.params.provider.Arguments} contains:
     * <ul>
     *   <li>the {@code from} argument (may be {@code null})</li>
     *   <li>the {@code Converter} argument (may be {@code null})</li>
     *   <li>the expected result – either a {@link Long} value for a successful conversion
     *       or a {@link Class} of the expected exception.</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> toAtomicLongProvider() {
        // a mis‑behaving converter that throws RuntimeException on any method call
        Converter explodingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter misbehaviour");
        });
        // a well‑behaved (recording) converter – just a regular mock
        Converter recordingConverter = Mockito.mock(Converter.class);
        return Stream.of(// true → 1, null converter
        arguments(new AtomicBoolean(true), null, 1L), // false → 0, null converter
        arguments(new AtomicBoolean(false), null, 0L), // true → 1, normal (non‑null) mock converter
        arguments(new AtomicBoolean(true), Mockito.mock(Converter.class), 1L), // true → 1, mis‑behaving converter (should be ignored)
        arguments(new AtomicBoolean(true), explodingConverter, 1L), // false → 0, mis‑behaving converter
        arguments(new AtomicBoolean(false), explodingConverter, 0L), // true → 1, recording converter
        arguments(new AtomicBoolean(true), recordingConverter, 1L), // false → 0, recording converter
        arguments(new AtomicBoolean(false), recordingConverter, 0L), // null from, null converter → NPE
        arguments(null, null, NullPointerException.class), // null from, non‑null converter → NPE
        arguments(null, recordingConverter, NullPointerException.class), // plain Boolean → ClassCastException
        arguments(Boolean.TRUE, null, ClassCastException.class), // AtomicInteger → ClassCastException
        arguments(new AtomicInteger(42), null, ClassCastException.class), // String → ClassCastException
        arguments("true", null, ClassCastException.class), // Integer → ClassCastException
        arguments(Integer.valueOf(1), null, ClassCastException.class));
    }

    @Test
    void toAtomicLong_returnsOneWhenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, normalMockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(1L, result.get(), "AtomicLong value should be 1 for true AtomicBoolean");
    }

    @Test
    void toAtomicLong_returnsZeroWhenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, normalMockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(0L, result.get(), "AtomicLong value should be 0 for false AtomicBoolean");
    }

    @Test
    void toAtomicLong_throwsClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        // given
        Object notAtomicBoolean = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicLong(notAtomicBoolean, normalMockConverter), "Method should throw ClassCastException when source is not an AtomicBoolean");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("toAtomicLongProvider")
    void toAtomicLong_variousScenarios(Object from, Converter converter, Object expected) {
        if (expected instanceof Class<?>) {
            // Expected an exception
            Class<? extends Throwable> exceptionClass = (Class<? extends Throwable>) expected;
            assertThrows(exceptionClass, () -> AtomicBooleanConversions.toAtomicLong(from, converter), () -> "Expected " + exceptionClass.getSimpleName() + " for input: " + from);
        } else {
            // Expected a successful conversion to AtomicLong with a specific long value
            AtomicLong result = AtomicBooleanConversions.toAtomicLong(from, converter);
            assertNotNull(result, "Resulting AtomicLong must not be null");
            long expectedValue = (Long) expected;
            assertEquals(expectedValue, result.get(), () -> "AtomicLong value mismatch for input: " + from);
        }
    }
}
