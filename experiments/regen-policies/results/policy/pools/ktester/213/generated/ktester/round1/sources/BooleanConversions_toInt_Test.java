package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toInt_Test {

    /**
     * A real {@link Converter} instance – the method under test does not depend on it.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options; this is cheap and safe.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for the Boolean true / false branches
     * ------------------------------------------------------------- */
    private static Stream<Arguments> booleanValuesProvider() {
        return Stream.of(arguments(Boolean.TRUE, CommonValues.INTEGER_ONE), arguments(Boolean.FALSE, CommonValues.INTEGER_ZERO));
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for non‑boolean inputs (ClassCastException)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> nonBooleanInputsProvider() {
        return Stream.of(arguments(123, "Integer"), arguments("true", "String"));
    }

    @Test
    void toInt_returnsOneWhenTrue() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        Integer result = BooleanConversions.toInt(source, converter);
        // THEN
        assertEquals(CommonValues.INTEGER_ONE, result, "Boolean true should be converted to Integer 1");
    }

    @Test
    void toInt_returnsZeroWhenFalse() {
        // GIVEN
        Boolean source = Boolean.FALSE;
        // WHEN
        Integer result = BooleanConversions.toInt(source, converter);
        // THEN
        assertEquals(CommonValues.INTEGER_ZERO, result, "Boolean false should be converted to Integer 0");
    }

    @Test
    void toInt_throwsClassCastExceptionForNonBoolean() {
        // GIVEN
        Object source = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toInt(source, converter), "Passing a non‑Boolean object should result in a ClassCastException");
    }

    @Test
    void toInt_acceptsNullSourceAndThrowsClassCastException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toInt(source, converter), "Passing null should result in a ClassCastException because of the cast to Boolean");
    }

    @Test
    void toInt_acceptsNullConverter() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        Integer result = BooleanConversions.toInt(source, null);
        // THEN
        assertEquals(CommonValues.INTEGER_ONE, result, "Method should work even when the Converter argument is null");
    }

    @ParameterizedTest(name = "toInt({0}) → {1}")
    @MethodSource("booleanValuesProvider")
    void toInt_returnsExpectedIntegerForBoolean(Boolean source, Integer expected) {
        // WHEN
        Integer result = BooleanConversions.toInt(source, converter);
        // THEN
        assertEquals(expected, result, () -> "Boolean " + source + " should be converted to " + expected);
    }

    /* -------------------------------------------------------------
     *  Null source – expect NullPointerException due to unboxing
     * ------------------------------------------------------------- */
    @Test
    void toInt_nullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toInt(source, converter), "Passing null should result in NullPointerException because of Boolean unboxing");
    }
}
