package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class BooleanConversions_toFloat_Test {

    /**
     * Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test – the Converter does not maintain state
        // that would affect the simple Boolean‑to‑Float conversion.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference; not strictly required but keeps the pattern clear.
        converter = null;
    }

    /**
     * Provides successful conversion scenarios.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the {@code from} object</li>
     *   <li>a {@link Converter} instance (real or mocked)</li>
     *   <li>the expected {@link Float} result</li>
     * </ul>
     */
    private static Stream<Arguments> successfulConversionProvider() {
        Converter realConverter = new Converter(new ConverterOptions());
        // behavior is irrelevant
        Converter mockConverter = mock(Converter.class);
        return Stream.of(// Boolean constants
        Arguments.of(Boolean.TRUE, realConverter, CommonValues.FLOAT_ONE), Arguments.of(Boolean.FALSE, realConverter, CommonValues.FLOAT_ZERO), // Boolean constants with a mock converter
        Arguments.of(Boolean.TRUE, mockConverter, CommonValues.FLOAT_ONE), Arguments.of(Boolean.FALSE, mockConverter, CommonValues.FLOAT_ZERO), // New Boolean objects (explicit constructor)
        Arguments.of(new Boolean(true), realConverter, CommonValues.FLOAT_ONE), Arguments.of(new Boolean(false), realConverter, CommonValues.FLOAT_ZERO));
    }

    /**
     * Provides inputs that should cause a {@link ClassCastException}
     * because they are not {@link Boolean} instances.
     */
    private static Stream<Arguments> classCastExceptionProvider() {
        Converter dummy = new Converter(new ConverterOptions());
        return Stream.of(Arguments.of("true", dummy), Arguments.of("false", dummy), Arguments.of(Integer.valueOf(1), dummy), Arguments.of(new Object(), dummy));
    }

    @Test
    void toFloat_whenTrue_returnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Float result = BooleanConversions.toFloat(source, converter);
        // then
        assertEquals(CommonValues.FLOAT_ONE, result, "Converting Boolean.TRUE should yield CommonValues.FLOAT_ONE");
    }

    @Test
    void toFloat_whenFalse_returnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Float result = BooleanConversions.toFloat(source, converter);
        // then
        assertEquals(CommonValues.FLOAT_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.FLOAT_ZERO");
    }

    @Test
    void toFloat_whenNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toFloat(source, converter), "Converting a null Boolean should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] toFloat({0}) with {1.getClass().getSimpleName()} => {2}")
    @MethodSource("successfulConversionProvider")
    void toFloat_successfulScenarios(Object from, Converter conv, Float expected) {
        // when
        Float result = BooleanConversions.toFloat(from, conv);
        // then
        assertEquals(expected, result, () -> "Expected conversion of " + from + " to yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] toFloat({0}) should throw ClassCastException")
    @MethodSource("classCastExceptionProvider")
    void toFloat_invalidType_throwsClassCastException(Object from, Converter conv) {
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toFloat(from, conv), () -> "Converting non‑Boolean value " + from + " should throw ClassCastException");
    }
}
