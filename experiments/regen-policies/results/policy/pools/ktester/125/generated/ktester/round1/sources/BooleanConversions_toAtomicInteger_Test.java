package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toAtomicInteger_Test {

    /**
     * Mocked Converter – not used by the method under test but required for the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Mocked Converter – not used by the method under test but required for the signature.
     */
    @Mock
    private Converter defaultConverter;

    /**
     * Provides a stream of arguments for the happy‑path tests.
     *
     * Each argument consists of:
     *   - the source object (Boolean, null, or a String representation)
     *   - a Converter instance (null, the default mock, or a throwing mock)
     *   - the expected integer value inside the resulting AtomicInteger
     *
     * The cases correspond to the JSON test specifications.
     */
    private static Stream<Arguments> happyPathProvider() {
        // a dummy (non‑null) converter – behaviour does not matter
        Converter dummyConverter = mock(Converter.class);
        // a converter that would throw if any method were invoked
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter should not be used");
        });
        return Stream.of(// Boolean.TRUE with default (null) converter
        Arguments.of(Boolean.TRUE, null, 1), // Boolean.FALSE with default (null) converter
        Arguments.of(Boolean.FALSE, null, 0), // Boolean.TRUE with a non‑null dummy converter
        Arguments.of(Boolean.TRUE, dummyConverter, 1), // new Boolean(true) (explicit constructor) with null converter
        Arguments.of(new Boolean(true), null, 1), // Boolean.FALSE with a converter that would throw if called
        Arguments.of(Boolean.FALSE, throwingConverter, 0));
    }

    @Test
    void toAtomicInteger_givenTrue_returnsOne() {
        // given
        Object from = Boolean.TRUE;
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(from, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(1, result.get(), "AtomicInteger should contain 1 for Boolean.TRUE");
    }

    @Test
    void toAtomicInteger_givenFalse_returnsZero() {
        // given
        Object from = Boolean.FALSE;
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(from, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(0, result.get(), "AtomicInteger should contain 0 for Boolean.FALSE");
    }

    @Test
    void toAtomicInteger_givenNull_throwsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toAtomicInteger(from, defaultConverter), "Passing null should result in NullPointerException");
    }

    @Test
    void toAtomicInteger_givenNonBoolean_throwsClassCastException() {
        // given
        Object from = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicInteger(from, defaultConverter), "Passing a non‑Boolean should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("happyPathProvider")
    void toAtomicInteger_happyPath_returnsExpectedAtomicInteger(Object from, Converter converter, int expected) {
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(from, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "AtomicInteger should contain " + expected + " for input: " + from);
    }
}
