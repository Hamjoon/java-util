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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toAtomicLong_Test {

    /**
     * The Converter is not used by the method under test, but it must be supplied.
     */
    @Mock
    private Converter converter;

    /**
     * The mock injected by Mockito – used for the “null‑converter” cases.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Supplies all successful conversion combinations:
     * <ul>
     *   <li>Boolean.TRUE / Boolean.FALSE</li>
     *   <li>converter = null, a regular mock, and a mock that throws on any call</li>
     * </ul>
     */
    private static Stream<Arguments> provideValidInputs() {
        // a regular (non‑throwing) mock – the method never calls it
        Converter regularMock = mock(Converter.class);
        // a mock that throws on any method invocation – still never called
        Converter throwingMock = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter method should not be invoked");
        });
        return Stream.of(// null converter
        Arguments.of(Boolean.TRUE, null, 1L), Arguments.of(Boolean.FALSE, null, 0L), // non‑null, but harmless converter
        Arguments.of(Boolean.TRUE, regularMock, 1L), Arguments.of(Boolean.FALSE, regularMock, 0L), // non‑null, throwing converter (should be ignored)
        Arguments.of(Boolean.TRUE, throwingMock, 1L), Arguments.of(Boolean.FALSE, throwingMock, 0L));
    }

    /**
     * Supplies all inputs that should trigger an exception:
     * <ul>
     *   <li>null input – leads to {@link NullPointerException} because of unboxing.</li>
     *   <li>String input – leads to {@link ClassCastException}.</li>
     *   <li>Integer input – also leads to {@link ClassCastException}.</li>
     * </ul>
     */
    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(Arguments.of(null, null, NullPointerException.class), Arguments.of("true", null, ClassCastException.class), Arguments.of(1, null, ClassCastException.class));
    }

    @Test
    void toAtomicLong_trueReturnsOne() {
        // given
        Boolean from = Boolean.TRUE;
        // when
        AtomicLong result = BooleanConversions.toAtomicLong(from, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(1L, result.get(), "AtomicLong should contain 1 for Boolean.TRUE");
    }

    @Test
    void toAtomicLong_falseReturnsZero() {
        // given
        Boolean from = Boolean.FALSE;
        // when
        AtomicLong result = BooleanConversions.toAtomicLong(from, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(0L, result.get(), "AtomicLong should contain 0 for Boolean.FALSE");
    }

    @Test
    void toAtomicLong_nullInputThrowsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toAtomicLong(from, converter), "Passing null should result in NPE due to unboxing of null Boolean");
    }

    @Test
    void toAtomicLong_invalidTypeThrowsClassCastException() {
        // given
        Object from = "not a Boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicLong(from, converter), "Passing a non‑Boolean should cause a ClassCastException");
    }

    /**
     * Verifies that a {@link Boolean} input is correctly converted to an
     * {@link AtomicLong} with the expected numeric value, irrespective of the
     * {@link Converter} instance supplied.
     *
     * @param from          the input object (must be Boolean.TRUE or Boolean.FALSE)
     * @param converter     the Converter supplied to the method (may be null)
     * @param expectedValue the long value that should be stored inside the AtomicLong
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → expected={2}")
    @MethodSource("provideValidInputs")
    void toAtomicLong_returnsExpectedAtomicLong(Object from, Converter converter, long expectedValue) {
        // when
        AtomicLong result = BooleanConversions.toAtomicLong(from, converter);
        // then
        assertNotNull(result, "Result must never be null");
        assertEquals(expectedValue, result.get(), () -> "AtomicLong should contain " + expectedValue + " for input " + from);
    }

    /**
     * Verifies that inappropriate inputs result in the expected exception.
     *
     * @param from              the input object that should cause a failure
     * @param converter         the Converter supplied (null is sufficient)
     * @param expectedException the type of exception that must be thrown
     */
    @ParameterizedTest(name = "[{index}] from={0} → throws {2}")
    @MethodSource("provideInvalidInputs")
    void toAtomicLong_invalidInputsThrowException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> BooleanConversions.toAtomicLong(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
