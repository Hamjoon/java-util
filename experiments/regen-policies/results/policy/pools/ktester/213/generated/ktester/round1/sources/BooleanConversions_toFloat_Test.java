package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toFloat_Test {

    /**
     * Mocked Converter – not used by the method but required for the signature.
     */
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock before each test to avoid cross‑test interference.
        mockConverter = mock(Converter.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to clean any interactions that might have been recorded.
        clearInvocations(mockConverter);
    }

    /**
     * Provides a stream of valid Boolean objects (including constructor‑created
     * instances) together with the expected Float result.
     */
    private static Stream<Arguments> validBooleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, CommonValues.FLOAT_ONE), Arguments.of(Boolean.FALSE, CommonValues.FLOAT_ZERO), Arguments.of(new Boolean(true), CommonValues.FLOAT_ONE), Arguments.of(new Boolean(false), CommonValues.FLOAT_ZERO));
    }

    /**
     * Supplies inputs that are not {@link Boolean} and therefore cause a
     * {@link ClassCastException} when the method attempts to cast.
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("true", ClassCastException.class), Arguments.of(new Object(), ClassCastException.class));
    }

    @Test
    void toFloat_whenBooleanTrue_returnsFloatOne() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        Float result = BooleanConversions.toFloat(source, mockConverter);
        // THEN
        assertEquals(CommonValues.FLOAT_ONE, result, "Converting Boolean.TRUE should yield CommonValues.FLOAT_ONE (1.0f)");
    }

    @Test
    void toFloat_whenBooleanFalse_returnsFloatZero() {
        // GIVEN
        Boolean source = Boolean.FALSE;
        // WHEN
        Float result = BooleanConversions.toFloat(source, mockConverter);
        // THEN
        assertEquals(CommonValues.FLOAT_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.FLOAT_ZERO (0.0f)");
    }

    @Test
    void toFloat_whenNullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toFloat(source, mockConverter), "Passing null as the source should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validBooleanProvider")
    void toFloat_validBooleanInputs_returnsExpected(Object from, Float expected) {
        // WHEN
        Float result = BooleanConversions.toFloat(from, mockConverter);
        // THEN
        assertEquals(expected, result, () -> "Expected conversion of " + from + " to be " + expected);
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("invalidInputProvider")
    void toFloat_invalidInputs_throwClassCastException(Object from, Class<? extends Throwable> exceptionClass) {
        // WHEN / THEN
        assertThrows(exceptionClass, () -> BooleanConversions.toFloat(from, mockConverter), () -> "Passing a non‑Boolean (" + from + ") should throw " + exceptionClass.getSimpleName());
    }
}
