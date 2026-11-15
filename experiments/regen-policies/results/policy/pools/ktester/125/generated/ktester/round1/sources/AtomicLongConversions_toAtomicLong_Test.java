package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicLongConversions_toAtomicLong_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    private static Stream<Arguments> errorScenarioProvider() {
        return Stream.of(// null 'from' (converter may also be null – the NPE occurs before converter is used)
        Arguments.of(null, null, NullPointerException.class), // non‑AtomicLong types – ClassCastException regardless of converter value
        Arguments.of("123", null, ClassCastException.class), Arguments.of(Integer.valueOf(10), null, ClassCastException.class));
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(Arguments.of(new AtomicLong(0L), 0L), Arguments.of(new AtomicLong(12345L), 12345L), Arguments.of(new AtomicLong(-9876L), -9876L));
    }

    private static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(Arguments.of(new AtomicLong(Long.MAX_VALUE), Long.MAX_VALUE), Arguments.of(new AtomicLong(Long.MIN_VALUE), Long.MIN_VALUE));
    }

    @Test
    void toAtomicLongCreatesNewInstanceWithSameValue() {
        // given
        long originalValue = 12345L;
        AtomicLong source = new AtomicLong(originalValue);
        // when
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(originalValue, result.get(), "Result should contain the same long value");
        assertNotSame(source, result, "Result should be a new AtomicLong instance, not the original");
    }

    @Test
    void toAtomicLongThrowsClassCastExceptionWhenSourceIsNotAtomicLong() {
        // given
        Object notAtomicLong = "not an atomic long";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicLongConversions.toAtomicLong(notAtomicLong, mockConverter), "Method should throw ClassCastException when the source is not an AtomicLong");
    }

    @Test
    void toAtomicLongThrowsNullPointerExceptionWhenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicLongConversions.toAtomicLong(nullSource, mockConverter), "Method should throw NullPointerException when the source is null");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → expect {2}")
    @MethodSource("errorScenarioProvider")
    void toAtomicLongErrorScenarios(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> AtomicLongConversions.toAtomicLong(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for from=" + from);
    }

    @ParameterizedTest(name = "[{index}] source={0} → expected value={1}")
    @MethodSource("normalConversionProvider")
    void toAtomicLongNormalConversion(AtomicLong source, long expectedValue) {
        // when
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, null);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedValue, result.get(), "Result should contain the same long value as the source");
        assertNotSame(source, result, "Result should be a new AtomicLong instance, not the original");
    }

    @ParameterizedTest(name = "[{index}] source={0} → expected value={1}")
    @MethodSource("boundaryValuesProvider")
    void toAtomicLongBoundaryValues(AtomicLong source, long expectedValue) {
        // when
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, null);
        // then
        assertNotNull(result);
        assertEquals(expectedValue, result.get(), "Result should correctly represent the boundary long value");
        assertNotSame(source, result, "Result must be a distinct instance");
    }

    @Test
    void toAtomicLongIndependenceOfInstances() {
        // given
        AtomicLong original = new AtomicLong(555L);
        // when
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, null);
        // then – result must be a copy
        assertNotSame(original, result, "Returned AtomicLong must be a different object");
        assertEquals(555L, result.get(), "Returned AtomicLong must contain the original value");
        // mutate the original after conversion
        original.set(999L);
        // ensure the result is unaffected
        assertEquals(555L, result.get(), "Modifying the source after conversion must not affect the returned instance");
    }

    @Test
    void toAtomicLongIgnoresNonNullConverter() {
        // given
        AtomicLong source = new AtomicLong(42L);
        // when – pass a non‑null mock converter
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(42L, result.get(), "Conversion must succeed regardless of the converter argument");
        assertNotSame(source, result, "Result must be a new instance even when converter is non‑null");
    }
}
