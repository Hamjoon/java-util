package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toDouble_Test {

    /**
     * Converter is required by the method signature but not used in the implementation.
     */
    @Mock
    Converter converter;

    /**
     * Converter is required by the method signature but not used in the implementation.
     */
    @Mock
    Converter // injected by Mockito, may be {@code null} in tests
    mockConverter;

    private static class ThrowingConverter implements Converter {

        @Override
        public Object convert(Object src, Class<?> dest) {
            throw new RuntimeException("converter should not be called");
        }
    }

    private static class MockConverter implements Converter {

        @Override
        public Object convert(Object src, Class<?> dest) {
            // No‑op implementation; the method under test never calls this.
            return null;
        }
    }

    private static Stream<Arguments> trueFalseWithNullConverterProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), null, 1.0), Arguments.of(new AtomicBoolean(false), null, 0.0));
    }

    private static Stream<Arguments> trueFalseWithNonNullConverterProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), new MockConverter(), 1.0), Arguments.of(new AtomicBoolean(false), new MockConverter(), 0.0));
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(5)), Arguments.of(new AtomicInteger(42)), Arguments.of(new AtomicLong(123L)), Arguments.of(Boolean.TRUE), Arguments.of(Boolean.FALSE), Arguments.of("true"), Arguments.of(new Object()));
    }

    private static Stream<Arguments> trueFalseWithThrowingConverterProvider() {
        ThrowingConverter throwing = new ThrowingConverter();
        return Stream.of(Arguments.of(new AtomicBoolean(true), throwing, 1.0), Arguments.of(new AtomicBoolean(false), throwing, 0.0));
    }

    @Test
    void toDouble_returnsOneWhenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        // then
        assertEquals(1.0, result);
    }

    @Test
    void toDouble_returnsZeroWhenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        // then
        assertEquals(0.0, result);
    }

    @Test
    void toDouble_throwsClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        // given
        Object notAtomic = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toDouble(notAtomic, converter));
    }

    @ParameterizedTest(name = "toDouble({0}) → {2}")
    @MethodSource("trueFalseWithNullConverterProvider")
    void toDouble_trueFalse_withNullConverter(AtomicBoolean source, Converter converter, double expected) {
        // when
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        // then
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "toDouble({0}, non‑null converter) → {2}")
    @MethodSource("trueFalseWithNonNullConverterProvider")
    void toDouble_trueFalse_withNonNullConverter(AtomicBoolean source, Converter converter, double expected) {
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toDouble_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toDouble(null, null));
    }

    @ParameterizedTest(name = "toDouble({0}) → ClassCastException")
    @MethodSource("invalidSourceProvider")
    void toDouble_invalidSource_throwsClassCastException(Object invalidSource) {
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toDouble(invalidSource, null));
    }

    @ParameterizedTest(name = "toDouble({0}, throwing converter) → {2}")
    @MethodSource("trueFalseWithThrowingConverterProvider")
    void toDouble_ignoresThrowingConverter(AtomicBoolean source, Converter converter, double expected) {
        // The method must not call the converter; otherwise a RuntimeException would be thrown.
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toDouble_capturesValue_true_thenMutated() {
        AtomicBoolean atomic = new AtomicBoolean(true);
        Double result = AtomicBooleanConversions.toDouble(atomic, null);
        // mutate after conversion
        atomic.set(false);
        assertEquals(1.0, result);
    }

    @Test
    void toDouble_capturesValue_false_thenMutated() {
        AtomicBoolean atomic = new AtomicBoolean(false);
        Double result = AtomicBooleanConversions.toDouble(atomic, null);
        // mutate after conversion
        atomic.set(true);
        assertEquals(0.0, result);
    }
}
