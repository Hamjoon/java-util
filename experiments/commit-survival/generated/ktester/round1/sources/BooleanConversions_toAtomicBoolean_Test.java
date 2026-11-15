package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toAtomicBoolean_Test {

    /**
     * Mocked {@link Converter} – the method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter converter;

    /**
     * Supplies the arguments for {@link #toAtomicBoolean_validBoolean(Object, Converter, boolean)}.
     * The {@code converter} field is the Mockito mock injected before each test.
     */
    Stream<Arguments> validBooleanProvider() {
        return Stream.of(// true / false with a null Converter
        Arguments.of(Boolean.TRUE, null, true), Arguments.of(Boolean.FALSE, null, false), // true / false with the injected mock Converter (non‑null)
        Arguments.of(Boolean.TRUE, converter, true), Arguments.of(Boolean.FALSE, converter, false));
    }

    /**
     * Arguments for {@link #toAtomicBoolean_invalidType_throwsClassCastException(Object)}.
     */
    Stream<Arguments> invalidBooleanProvider() {
        return Stream.of(// Integer
        Arguments.of(123), // String
        Arguments.of("some string"));
    }

    @Test
    void toAtomicBoolean_trueValue() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(input, converter);
        // then
        assertNotNull(result, "Resulting AtomicBoolean should not be null");
        assertTrue(result.get(), "AtomicBoolean should contain true");
    }

    @Test
    void toAtomicBoolean_falseValue() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(input, converter);
        // then
        assertNotNull(result, "Resulting AtomicBoolean should not be null");
        assertFalse(result.get(), "AtomicBoolean should contain false");
    }

    @Test
    void toAtomicBoolean_nullInput_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toAtomicBoolean(input, converter), "Passing null should result in NPE due to unboxing");
    }

    @Test
    void toAtomicBoolean_invalidType_throwsClassCastException() {
        // given
        Object input = "not a Boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicBoolean(input, converter), "Passing a non‑Boolean object should cause a ClassCastException");
    }

    /**
     * Parameterized test covering all valid combinations of input Boolean
     * values and both {@code null} and non‑null {@link Converter} instances.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converterIsNull={1}")
    @MethodSource("validBooleanProvider")
    void toAtomicBoolean_validBoolean(Object from, Converter suppliedConverter, boolean expectedValue) {
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(from, suppliedConverter);
        // then
        assertNotNull(result, "Resulting AtomicBoolean should not be null");
        assertEquals(expectedValue, result.get(), () -> "AtomicBoolean should contain " + expectedValue);
    }

    @Test
    void toAtomicBoolean_invalidType_string_throwsClassCastException() {
        // given
        Object input = "not a Boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicBoolean(input, converter), "Passing a non‑Boolean object should cause a ClassCastException");
    }
}
