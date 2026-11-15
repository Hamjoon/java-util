package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtomicIntegerConversions_toAtomicInteger_Test {

    @Mock
    private Converter // not used by the method but required for the signature
    converter;

    @Mock
    private Converter // injected by Mockito, not used by the method
    unusedConverter;

    /**
     * Supplies a range of valid inputs together with the expected int value
     * and the type of {@link Converter} to pass.
     */
    private static Stream<Arguments> provideSuccessfulConversions() {
        // a plain mock (behaviour irrelevant)
        Converter mockConverter = mock(Converter.class);
        // a mock that would throw if any method were called (still unused)
        Converter throwingConverter = mock(Converter.class);
        // any method
        doThrow(new RuntimeException("Should not be called")).when(throwingConverter).toString();
        return Stream.of(// Simple positive value, null converter
        Arguments.of(new AtomicInteger(5), 5, null), // Zero value
        Arguments.of(new AtomicInteger(0), 0, null), // Max int
        Arguments.of(new AtomicInteger(Integer.MAX_VALUE), Integer.MAX_VALUE, null), // Min int
        Arguments.of(new AtomicInteger(Integer.MIN_VALUE), Integer.MIN_VALUE, null), // Non‑null but unused converter
        Arguments.of(new AtomicInteger(42), 42, mockConverter), // Another non‑null converter (dummy)
        Arguments.of(new AtomicInteger(5), 5, mockConverter), // Converter that would throw if used – method must ignore it
        Arguments.of(new AtomicInteger(123), 123, throwingConverter));
    }

    @Test
    void toAtomicIntegerReturnsNewInstanceWithSameValue() {
        // given
        AtomicInteger original = new AtomicInteger(42);
        // when
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(original.intValue(), result.intValue(), "Result should contain the same int value as the original");
        assertNotSame(original, result, "Result should be a new AtomicInteger instance, not the original");
    }

    @Test
    void toAtomicIntegerWithNullSourceThrowsNullPointerException() {
        // given
        AtomicInteger nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicIntegerConversions.toAtomicInteger(nullSource, unusedConverter), "Passing a null source should throw NullPointerException");
    }

    @Test
    void toAtomicIntegerWithIncompatibleTypeThrowsClassCastException() {
        // given
        Integer notAtomic = Integer.valueOf(10);
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicIntegerConversions.toAtomicInteger(notAtomic, unusedConverter), "Casting a non‑AtomicInteger should trigger ClassCastException");
    }

    @Test
    void returnedInstanceIsIndependentFromOriginalAfterMutation() {
        // given
        AtomicInteger original = new AtomicInteger(7);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, unusedConverter);
        // mutate the original after conversion
        original.set(99);
        // then
        assertEquals(7, result.intValue(), "Result must retain the original value even after the source is mutated");
        assertNotEquals(original.intValue(), result.intValue(), "Result must be independent of the mutated source");
    }

    @Test
    void returnedInstanceIsIndependentWhenOriginalIsMutatedLater() {
        // given
        AtomicInteger original = new AtomicInteger(5);
        Converter dummy = mock(Converter.class);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, dummy);
        // mutate original after conversion
        original.set(99);
        // then
        assertEquals(5, result.intValue(), "Returned AtomicInteger should still hold the original value (5) after source mutation");
    }
}
