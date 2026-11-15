package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toAtomicInteger_Test {

    /**
     * A mock Converter – the method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A simple non‑null converter used in a few test cases.
     */
    private static final class DummyConverter implements Converter {
        // No behaviour needed – the conversion method never calls any method on the converter.
    }

    private static Stream<Arguments> trueFalseProvider() {
        return Stream.of(// from,                converter,          expected int value, description
        Arguments.of(new AtomicBoolean(true), null, 1, "true → 1 (null converter)"), Arguments.of(new AtomicBoolean(false), null, 0, "false → 0 (null converter)"), Arguments.of(new AtomicBoolean(true), new DummyConverter(), 1, "true → 1 (non‑null converter)"), Arguments.of(new AtomicBoolean(false), new DummyConverter(), 0, "false → 0 (non‑null converter)"));
    }

    @Test
    void toAtomicInteger_whenAtomicBooleanTrue_returnsAtomicIntegerWithOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.get(), "AtomicBoolean true should map to AtomicInteger value 1");
    }

    @Test
    void toAtomicInteger_whenAtomicBooleanFalse_returnsAtomicIntegerWithZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(0, result.get(), "AtomicBoolean false should map to AtomicInteger value 0");
    }

    @Test
    void toAtomicInteger_whenSourceIsNotAtomicBoolean_throwsClassCastException() {
        // given
        Object notAtomicBoolean = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicInteger(notAtomicBoolean, mockConverter), "Passing a non‑AtomicBoolean source should result in ClassCastException");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("trueFalseProvider")
    void toAtomicInteger_variousInputs_returnsExpectedAtomicInteger(AtomicBoolean source, Converter converter, int expected, String _description) {
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), "AtomicBoolean " + source.get() + " should map to AtomicInteger value " + expected);
    }

    @Test
    void toAtomicInteger_whenSourceIsNull_throwsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toAtomicInteger(nullSource, mockConverter), "Passing null as the 'from' argument should cause NullPointerException");
    }

    @Test
    void toAtomicInteger_resultCapturesValueEvenIfOriginalIsMutated() {
        // given
        AtomicBoolean original = new AtomicBoolean(true);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(original, mockConverter);
        // mutate after conversion
        original.set(false);
        // then
        assertEquals(1, result.get(), "Returned AtomicInteger must retain the original value (1) even after the source is changed");
    }

    @Test
    void toAtomicInteger_resultRemainsUnchangedWhenOriginalIsMutatedFromFalseToTrue() {
        // given
        AtomicBoolean original = new AtomicBoolean(false);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(original, mockConverter);
        // mutate after conversion
        original.set(true);
        // then
        assertEquals(0, result.get(), "Returned AtomicInteger must retain the original value (0) even after source mutation");
    }

    @Test
    void toAtomicInteger_modifyingReturnedAtomicIntegerDoesNotAffectOriginalAtomicBoolean() {
        // given
        AtomicBoolean original = new AtomicBoolean(true);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(original, mockConverter);
        // when
        // change the result
        result.set(5);
        // then
        assertTrue(original.get(), "Changing the returned AtomicInteger must not modify the original AtomicBoolean");
        assertEquals(5, result.get(), "Returned AtomicInteger should reflect the value we set (5)");
    }

    @Test
    void toAtomicInteger_eachInvocationCreatesNewAtomicIntegerInstance() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicInteger first = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        AtomicInteger second = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        // then
        assertNotSame(first, second, "Each call must return a distinct AtomicInteger instance");
        // sanity check that both contain the correct value
        assertEquals(1, first.get());
        assertEquals(1, second.get());
    }
}
