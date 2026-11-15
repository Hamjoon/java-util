package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toShort_Test {

    private static Stream<Arguments> shortProvider() {
        // a mocked Converter – the method should ignore it
        Converter mockConverter = Mockito.mock(Converter.class);
        // normal true/false cases
        Arguments trueCase = arguments(new AtomicBoolean(true), null, CommonValues.SHORT_ONE);
        Arguments falseCase = arguments(new AtomicBoolean(false), null, CommonValues.SHORT_ZERO);
        // same cases with a non‑null Converter
        Arguments trueWithConverter = arguments(new AtomicBoolean(true), mockConverter, CommonValues.SHORT_ONE);
        Arguments falseWithConverter = arguments(new AtomicBoolean(false), mockConverter, CommonValues.SHORT_ZERO);
        // mutate after construction
        AtomicBoolean mutatedTrue = new AtomicBoolean(true);
        mutatedTrue.set(false);
        Arguments mutatedToFalse = arguments(mutatedTrue, null, CommonValues.SHORT_ZERO);
        AtomicBoolean mutatedFalse = new AtomicBoolean(false);
        mutatedFalse.set(true);
        Arguments mutatedToTrue = arguments(mutatedFalse, null, CommonValues.SHORT_ONE);
        // anonymous subclass (still behaves like a normal AtomicBoolean)
        AtomicBoolean anonTrue = new AtomicBoolean(true) {
        };
        Arguments anonTrueCase = arguments(anonTrue, null, CommonValues.SHORT_ONE);
        AtomicBoolean anonFalse = new AtomicBoolean(false) {
        };
        Arguments anonFalseCase = arguments(anonFalse, null, CommonValues.SHORT_ZERO);
        return Stream.of(trueCase, falseCase, trueWithConverter, falseWithConverter, mutatedToFalse, mutatedToTrue, anonTrueCase, anonFalseCase);
    }

    private static Stream<Arguments> exceptionProvider() {
        // null reference
        Arguments nullCase = arguments(null, null, ClassCastException.class);
        // non‑AtomicBoolean object
        Arguments notAtomicCase = arguments("not an atomic boolean", null, ClassCastException.class);
        // subclass that overrides get() to throw an exception
        AtomicBoolean throwingSubclass = new AtomicBoolean(true) {

            @Override
            public boolean get() {
                throw new IllegalStateException("forced");
            }
        };
        Arguments throwingCase = arguments(throwingSubclass, null, IllegalStateException.class);
        return Stream.of(nullCase, notAtomicCase, throwingCase);
    }

    @Test
    void givenAtomicBooleanTrue_whenToShort_thenReturnsShortOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // not used by the method
        Converter converter = null;
        // when
        Short result = AtomicBooleanConversions.toShort(source, converter);
        // then
        assertEquals(CommonValues.SHORT_ONE, result, "AtomicBoolean true should be converted to SHORT_ONE");
    }

    @Test
    void givenAtomicBooleanFalse_whenToShort_thenReturnsShortZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // not used by the method
        Converter converter = null;
        // when
        Short result = AtomicBooleanConversions.toShort(source, converter);
        // then
        assertEquals(CommonValues.SHORT_ZERO, result, "AtomicBoolean false should be converted to SHORT_ZERO");
    }

    @Test
    void givenNullAtomicBoolean_whenToShort_thenThrowsClassCastException() {
        // given
        Object source = null;
        Converter converter = null;
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toShort(source, converter), "Passing a null (or non‑AtomicBoolean) should result in a ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for successful conversions (return Short)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1} => expected={2}")
    @MethodSource("shortProvider")
    void givenAtomicBoolean_whenToShort_thenReturnsExpectedShort(Object from, Converter converter, Short expected) {
        // when
        Short result = AtomicBooleanConversions.toShort(from, converter);
        // then
        assertEquals(expected, result, () -> "Expected " + expected + " but got " + result);
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for expected exceptions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} expects {2}")
    @MethodSource("exceptionProvider")
    void givenInvalidInput_whenToShort_thenThrowsException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> AtomicBooleanConversions.toShort(from, converter), () -> "Expected exception " + expectedException.getSimpleName());
    }
}
