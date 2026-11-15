package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import java.util.concurrent.atomic.AtomicInteger;

class BooleanConversions_toAtomicInteger_Test {

    /**
     * The Converter instance is not used by the method under test, but we provide a
     *  non‑null placeholder to satisfy the method signature.
     */
    private static Converter dummyConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter constructor requires a ConverterOptions instance.
        // For the purpose of these tests we can pass a minimal options object.
        dummyConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        dummyConverter = null;
    }

    @Test
    void toAtomicInteger_whenTrue_returnsOne() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(input, dummyConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.get(), "AtomicInteger value should be 1 for Boolean.TRUE");
    }

    @Test
    void toAtomicInteger_whenFalse_returnsZero() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(input, dummyConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(0, result.get(), "AtomicInteger value should be 0 for Boolean.FALSE");
    }

    @Test
    void toAtomicInteger_whenNull_throwsClassCastException() {
        // given
        Object input = null;
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicInteger(input, dummyConverter), "Passing null should result in a ClassCastException due to the cast to Boolean");
    }

    @Test
    void toAtomicInteger_whenNonBoolean_throwsClassCastException() {
        // given
        Object input = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicInteger(input, dummyConverter), "Passing a non‑Boolean object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "toAtomicInteger({0}) → {1}")
    @CsvSource({ "true, 1", "false, 0" })
    void toAtomicInteger_validBooleans_returnExpectedValue(boolean boolValue, int expected) {
        // given
        Boolean input = boolValue;
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(input, dummyConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), "AtomicInteger value should match the Boolean conversion");
    }

    @Test
    void toAtomicInteger_whenNull_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toAtomicInteger(input, dummyConverter), "Casting null to Boolean and unboxing should throw NullPointerException");
    }

    @Test
    void toAtomicInteger_withConverterThatThrows_isIgnored() {
        // given: a mock Converter that would throw if any method were invoked
        Converter throwingConverter = mock(Converter.class);
        // Configure the mock to throw on any call – this is defensive; the method
        // under test must never call it.
        doThrow(new RuntimeException("Converter should not be used")).when(throwingConverter).convert(any(), any());
        Boolean input = Boolean.TRUE;
        // when
        AtomicInteger result = BooleanConversions.toAtomicInteger(input, throwingConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(1, result.get(), "AtomicInteger should contain 1 for Boolean.TRUE");
        // Verify that the mock was never interacted with
        verifyNoInteractions(throwingConverter);
    }
}
