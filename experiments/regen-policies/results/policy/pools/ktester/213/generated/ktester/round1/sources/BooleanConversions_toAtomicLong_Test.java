package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toAtomicLong_Test {

    /**
     * Mocked {@link Converter} – not used by the method under test but required by the signature.
     */
    @Mock
    private Converter mockConverter;

    static Stream<Arguments> validBooleanProvider() {
        Converter mock = mock(Converter.class);
        return Stream.of(Arguments.of(Boolean.TRUE, null, 1L), Arguments.of(Boolean.FALSE, null, 0L), Arguments.of(Boolean.TRUE, mock, 1L));
    }

    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of(null, null, NullPointerException.class), Arguments.of("true", null, ClassCastException.class), Arguments.of(Integer.valueOf(1), null, ClassCastException.class));
    }

    static Stream<Arguments> throwingConverterProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, 1L), Arguments.of(Boolean.FALSE, 0L));
    }

    static Stream<Arguments> differentBooleanRepresentationsProvider() {
        return Stream.of(Arguments.of(Boolean.valueOf(true), 1L), Arguments.of(new Boolean(false), 0L));
    }

    @Test
    void toAtomicLong_trueReturnsOne() {
        // GIVEN
        Boolean input = Boolean.TRUE;
        // WHEN
        AtomicLong result = BooleanConversions.toAtomicLong(input, mockConverter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(1L, result.get(), "true should be converted to AtomicLong value 1");
    }

    @Test
    void toAtomicLong_falseReturnsZero() {
        // GIVEN
        Boolean input = Boolean.FALSE;
        // WHEN
        AtomicLong result = BooleanConversions.toAtomicLong(input, mockConverter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(0L, result.get(), "false should be converted to AtomicLong value 0");
    }

    @Test
    void toAtomicLong_nullInputThrowsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toAtomicLong(input, mockConverter), "Passing null should result in NPE because the Boolean is unboxed");
    }

    @Test
    void toAtomicLong_nonBooleanInputThrowsClassCastException() {
        // GIVEN
        Object input = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicLong(input, mockConverter), "A non‑Boolean input must cause a ClassCastException");
    }

    /**
     * Valid Boolean inputs – the method must return an {@link AtomicLong}
     * containing {@code 1} for {@code true} and {@code 0} for {@code false}.
     * The supplied {@link Converter} may be {@code null}, a regular mock,
     * or any other instance; it is ignored by the implementation.
     */
    @ParameterizedTest(name = "{index}: from={0}, converter={1}")
    @MethodSource("validBooleanProvider")
    void toAtomicLong_validBoolean_returnsExpected(Object from, Converter converter, long expected) {
        // WHEN
        AtomicLong result = BooleanConversions.toAtomicLong(from, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "AtomicLong value for input " + from + " should be " + expected);
    }

    /**
     * Invalid or edge‑case inputs – the method is expected to throw the
     * appropriate exception because it casts the argument to {@link Boolean}
     * before unboxing.
     */
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("invalidInputProvider")
    void toAtomicLong_invalidInputs_throwsException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> BooleanConversions.toAtomicLong(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }

    /**
     * Supplying a non‑null {@link Converter} that would throw if any of its
     * methods were invoked. The conversion method must never touch the converter,
     * therefore the call should succeed and return the correct {@link AtomicLong}.
     */
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("throwingConverterProvider")
    void toAtomicLong_converterSideEffectsIgnored(Object from, long expected) {
        // a Converter that throws on any interaction
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter method should not be called");
        });
        AtomicLong result = BooleanConversions.toAtomicLong(from, throwingConverter);
        assertNotNull(result);
        assertEquals(expected, result.get(), () -> "AtomicLong value for input " + from + " should be " + expected);
    }

    /**
     * Different ways of creating {@link Boolean} objects – the conversion must
     * treat them exactly as the primitive values they represent.
     */
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("differentBooleanRepresentationsProvider")
    void toAtomicLong_differentBooleanRepresentations(Object from, long expected) {
        AtomicLong result = BooleanConversions.toAtomicLong(from, mock(Converter.class));
        assertNotNull(result);
        assertEquals(expected, result.get(), () -> "AtomicLong value for input " + from + " should be " + expected);
    }
}
