package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toBigInteger_Test {

    /**
     * Mocked Converter – the method under test does not use it, but it must be supplied.
     */
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for each test to avoid cross‑test interference.
        mockConverter = mock(Converter.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions – good practice when using a shared mock instance.
        clearInvocations(mockConverter);
    }

    private static Stream<Arguments> booleanAndExpectedProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, BigInteger.ONE, "true → ONE"), Arguments.of(Boolean.FALSE, BigInteger.ZERO, "false → ZERO"));
    }

    @Test
    void toBigInteger_trueReturnsOne() {
        // GIVEN
        Boolean input = Boolean.TRUE;
        // WHEN
        BigInteger result = BooleanConversions.toBigInteger(input, mockConverter);
        // THEN
        assertEquals(BigInteger.ONE, result, "True should be converted to BigInteger.ONE");
    }

    @Test
    void toBigInteger_falseReturnsZero() {
        // GIVEN
        Boolean input = Boolean.FALSE;
        // WHEN
        BigInteger result = BooleanConversions.toBigInteger(input, mockConverter);
        // THEN
        assertEquals(BigInteger.ZERO, result, "False should be converted to BigInteger.ZERO");
    }

    @Test
    void toBigInteger_nullThrowsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toBigInteger(input, mockConverter), "Passing null should throw NullPointerException");
    }

    @Test
    void toBigInteger_nonBooleanThrowsClassCastException() {
        // GIVEN
        Object input = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toBigInteger(input, mockConverter), "Passing a non‑Boolean should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("booleanAndExpectedProvider")
    void toBigInteger_booleanValues_returnExpected(BigInteger expected, Boolean input, String _desc) {
        // WHEN
        BigInteger result = BooleanConversions.toBigInteger(input, mockConverter);
        // THEN
        assertEquals(expected, result, () -> "Boolean value " + input + " should be converted to " + expected);
        // The converter must never be touched.
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Null input – expect NPE because of the cast to Boolean
     * ------------------------------------------------------------- */
    @Test
    void toBigInteger_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toBigInteger(input, mockConverter), "Passing null should throw NullPointerException due to the cast to Boolean");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Non‑Boolean input – expect ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void toBigInteger_nonBooleanFrom_throwsClassCastException() {
        // GIVEN
        Object input = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toBigInteger(input, mockConverter), "Passing a non‑Boolean should throw ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Tests that verify the supplied Converter is never used,
     *  even when it is a concrete implementation that would throw
     *  or return a dummy value if called.
     * ------------------------------------------------------------- */
    @Test
    void toBigInteger_trueWithNonNullConverter_ignoresConverter() {
        // GIVEN
        Boolean input = Boolean.TRUE;
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // WHEN
        BigInteger result = BooleanConversions.toBigInteger(input, explodingConverter);
        // THEN
        assertEquals(BigInteger.ONE, result, "True should still be converted to ONE");
    }

    @Test
    void toBigInteger_falseWithNonNullConverter_ignoresConverter() {
        // GIVEN
        Boolean input = Boolean.FALSE;
        // A mock that returns a dummy value if called – we will verify it is never called.
        Converter dummyConverter = mock(Converter.class);
        when(dummyConverter.convert(any(), any())).thenReturn("ignored");
        // WHEN
        BigInteger result = BooleanConversions.toBigInteger(input, dummyConverter);
        // THEN
        assertEquals(BigInteger.ZERO, result, "False should still be converted to ZERO");
        // Ensure the method never delegated to the converter.
        verifyNoInteractions(dummyConverter);
    }

    @Test
    void toBigInteger_booleanValueOfTrue_returnsOne() {
        Boolean input = Boolean.valueOf(true);
        BigInteger result = BooleanConversions.toBigInteger(input, mockConverter);
        assertEquals(BigInteger.ONE, result, "Boolean.valueOf(true) should yield ONE");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBigInteger_booleanValueOfFalse_returnsZero() {
        Boolean input = Boolean.valueOf(false);
        BigInteger result = BooleanConversions.toBigInteger(input, mockConverter);
        assertEquals(BigInteger.ZERO, result, "Boolean.valueOf(false) should yield ZERO");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBigInteger_atomicBoolean_throwsClassCastException() {
        // AtomicBoolean is not a Boolean, so the explicit cast fails.
        Object input = new AtomicBoolean(true);
        assertThrows(ClassCastException.class, () -> BooleanConversions.toBigInteger(input, mockConverter), "AtomicBoolean should cause ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    /**
     * The Java language forbids subclassing {@link Boolean} because it is {@code final}.
     * Therefore we cannot create an anonymous subclass as the original test description
     * suggests.  This test documents that attempting to do so results in a compilation
     * error, and we simply confirm that a regular {@link Boolean} instance behaves as
     * expected.
     */
    @Test
    void toBigInteger_regularBooleanInstance_behavesCorrectly() {
        // Equivalent to the hypothetical subclass scenario.
        Boolean input = Boolean.TRUE;
        BigInteger result = BooleanConversions.toBigInteger(input, mockConverter);
        assertEquals(BigInteger.ONE, result, "Regular Boolean.TRUE should yield ONE");
        verifyNoInteractions(mockConverter);
    }
}
