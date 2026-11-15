package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toFloat_Test {

    /**
     * Mocked Converter – the method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    private AtomicBoolean trueAtomic;

    private AtomicBoolean falseAtomic;

    @BeforeEach
    void setupBeforeEach() {
        trueAtomic = new AtomicBoolean(true);
        falseAtomic = new AtomicBoolean(false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and to ensure a clean state for the next test.
        trueAtomic = null;
        falseAtomic = null;
    }

    private static Stream<Arguments> provideAtomicBooleanAndExpected() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), Float.valueOf(1.0f)), Arguments.of(new AtomicBoolean(false), Float.valueOf(0.0f)));
    }

    @Test
    void toFloat_returnsOneWhenAtomicBooleanIsTrue() {
        Float result = AtomicBooleanConversions.toFloat(trueAtomic, mockConverter);
        assertEquals(Float.valueOf(1.0f), result, "Expected FLOAT_ONE for true AtomicBoolean");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toFloat_returnsZeroWhenAtomicBooleanIsFalse() {
        Float result = AtomicBooleanConversions.toFloat(falseAtomic, mockConverter);
        assertEquals(Float.valueOf(0.0f), result, "Expected FLOAT_ZERO for false AtomicBoolean");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "{index} => atomic={0}, expected={1}")
    @MethodSource("provideAtomicBooleanAndExpected")
    void toFloat_returnsExpectedValue(AtomicBoolean atomic, Float expected) {
        Float result = AtomicBooleanConversions.toFloat(atomic, mockConverter);
        assertEquals(expected, result, () -> "Expected " + expected + " for AtomicBoolean holding " + atomic.get());
        // The method must not touch the converter.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toFloat_nullFrom_throwsNullPointerException() {
        // The cast to AtomicBoolean triggers the NPE before any other logic.
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toFloat(null, mockConverter), "Null 'from' should cause NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toFloat_nonAtomicBooleanFrom_throwsClassCastException() {
        String notAtomic = "not an atomic boolean";
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toFloat(notAtomic, mockConverter), "Passing a non‑AtomicBoolean should cause ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "{index} => atomic={0}, expected={1}")
    @MethodSource("provideAtomicBooleanAndExpected")
    void toFloat_ignoresNonNullConverter(AtomicBoolean atomic, Float expected) {
        // Use a mock Converter to verify that the method does not interact with it.
        Float result = AtomicBooleanConversions.toFloat(atomic, mockConverter);
        assertEquals(expected, result, () -> "Even with a non‑null Converter, expected " + expected);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toFloat_doesNotMutateAtomicBoolean_true() {
        AtomicBoolean original = new AtomicBoolean(true);
        Float result = AtomicBooleanConversions.toFloat(original, mockConverter);
        assertEquals(Float.valueOf(1.0f), result);
        assertTrue(original.get(), "AtomicBoolean should remain true after conversion");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toFloat_doesNotMutateAtomicBoolean_false() {
        AtomicBoolean original = new AtomicBoolean(false);
        Float result = AtomicBooleanConversions.toFloat(original, mockConverter);
        assertEquals(Float.valueOf(0.0f), result);
        assertFalse(original.get(), "AtomicBoolean should remain false after conversion");
        verifyNoInteractions(mockConverter);
    }
}
