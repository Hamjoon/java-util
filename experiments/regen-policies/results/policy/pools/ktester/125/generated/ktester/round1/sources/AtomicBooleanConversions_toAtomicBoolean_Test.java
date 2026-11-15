package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toAtomicBoolean_Test {

    @Mock
    private Converter // the method does not use this, but a non‑null instance is required
    mockConverter;

    /* -------------------------------------------------------------
     *  Helper dummy Converter implementation (no behaviour needed)
     * ------------------------------------------------------------- */
    private static class DummyConverter implements Converter {
        // Intentionally empty – the conversion method under test does not invoke any Converter methods.
    }

    private static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), true), Arguments.of(new AtomicBoolean(false), false));
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(5)), Arguments.of("some string"));
    }

    @Test
    void toAtomicBoolean_copiesTrueValue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.get(), "Result should contain true");
        assertNotSame(source, result, "Result should be a new instance, not the original");
    }

    @Test
    void toAtomicBoolean_copiesFalseValue() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.get(), "Result should contain false");
        assertNotSame(source, result, "Result should be a new instance, not the original");
    }

    @Test
    void toAtomicBoolean_nullSource_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toAtomicBoolean(source, mockConverter), "Calling with a null source should throw NullPointerException");
    }

    @Test
    void toAtomicBoolean_ignoresConverterArgument() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // The mockConverter is not configured; the method should not interact with it.
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertTrue(result.get(), "Result should contain the same boolean value as the source");
        // No verification of mock interactions is needed because the method does not use the converter.
    }

    /* -------------------------------------------------------------
     *  Group 1 – Normal behavior (true / false) with a null converter
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] source={0}")
    @MethodSource("normalBehaviorProvider")
    void toAtomicBoolean_normalBehavior(AtomicBoolean source, Boolean expectedValue) {
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, null);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedValue, result.get(), "Result should contain the same boolean value as the source");
        assertNotSame(source, result, "Result must be a new instance, not the original");
    }

    /* -------------------------------------------------------------
     *  Group 2 – Null 'from' argument
     * ------------------------------------------------------------- */
    @Test
    void toAtomicBoolean_nullFrom_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toAtomicBoolean(source, null), "Passing a null 'from' should throw NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Group 3 – Invalid source type (ClassCastException)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] source type={0}")
    @MethodSource("invalidSourceProvider")
    void toAtomicBoolean_invalidSource(Object invalidSource) {
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicBoolean(invalidSource, null), "Casting a non‑AtomicBoolean source should throw ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Group 4 – Non‑null Converter argument (ignored)
     * ------------------------------------------------------------- */
    @Test
    void toAtomicBoolean_nonNullConverter_isIgnored() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // Using a real (dummy) Converter instance instead of the mock to prove the argument is ignored
        Converter dummyConverter = new DummyConverter();
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, dummyConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertTrue(result.get(), "Result should contain true, matching the source");
        assertNotSame(source, result, "Result must be a new instance");
    }

    /* -------------------------------------------------------------
     *  Group 5 – Returned instance independence from source mutation
     * ------------------------------------------------------------- */
    @Test
    void toAtomicBoolean_returnedInstanceIndependentFromSourceMutation() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, null);
        // mutate the original after conversion
        source.set(false);
        // then
        assertTrue(result.get(), "Result should retain the original true value even after source is mutated");
        assertFalse(source.get(), "Source should reflect the mutation to false");
    }
}
