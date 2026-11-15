package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toBigInteger_Test {

    @Mock
    private Converter // not used by the method under test
    mockConverter;

    private static Stream<Arguments> standardBooleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, BigInteger.ONE), Arguments.of(Boolean.FALSE, BigInteger.ZERO));
    }

    private static Stream<Arguments> nonBooleanProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(42), "Integer should cause ClassCastException"), Arguments.of("true", "String should cause ClassCastException"));
    }

    @Test
    void toBigInteger_givenTrue_returnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        BigInteger result = BooleanConversions.toBigInteger(source, mockConverter);
        // then
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    void toBigInteger_givenFalse_returnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        BigInteger result = BooleanConversions.toBigInteger(source, mockConverter);
        // then
        assertEquals(BigInteger.ZERO, result);
    }

    @Test
    void toBigInteger_givenNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toBigInteger(source, mockConverter));
        // Ensure the converter is never touched even when NPE occurs
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBigInteger_givenNonBoolean_throwsClassCastException() {
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toBigInteger(from, mockConverter), description);
        // Verify that the converter is not used
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for the "standard boolean values" group
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, expected={1}")
    @MethodSource("standardBooleanProvider")
    void toBigInteger_givenStandardBoolean_returnsExpected(Object from, BigInteger expected) {
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, mockConverter);
        // then
        assertEquals(expected, result);
        // The converter must remain untouched
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for the "non-null converter ignored" group
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, converter=non‑null")
    @MethodSource("standardBooleanProvider")
    void toBigInteger_givenNonNullConverter_ignored(Object from, BigInteger expected) {
        // The mockConverter is already a non‑null mock; we just reuse it.
        BigInteger result = BooleanConversions.toBigInteger(from, mockConverter);
        assertEquals(expected, result);
        // The method must not interact with the converter at all.
        verifyNoInteractions(mockConverter);
    }
}
