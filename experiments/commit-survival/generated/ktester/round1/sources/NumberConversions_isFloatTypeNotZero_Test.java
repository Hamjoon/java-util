package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

class NumberConversions_isFloatTypeNotZero_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default constructor provides a sensible
        // set of built‑in conversions that are sufficient for the numeric tests below.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> positiveNonZeroProvider() {
        return Stream.of(Arguments.of("Double 5.0d should be non‑zero", 5.0d), Arguments.of("Integer 10 should be non‑zero", 10), Arguments.of("Float 3.14f should be non‑zero", 3.14f), Arguments.of("String \"3.14\" should be parsed to non‑zero", "3.14"), Arguments.of("BigDecimal.ONE should be non‑zero", BigDecimal.ONE), Arguments.of("Double.POSITIVE_INFINITY should be non‑zero", Double.POSITIVE_INFINITY), Arguments.of("Double.NEGATIVE_INFINITY should be non‑zero", Double.NEGATIVE_INFINITY), Arguments.of("Double.NaN should be non‑zero (NaN != 0)", Double.NaN));
    }

    private static Stream<Arguments> zeroValuesProvider() {
        return Stream.of(Arguments.of("Double 0d should be zero", 0d), Arguments.of("Integer 0 should be zero", 0), Arguments.of("Float 0f should be zero", 0f), Arguments.of("String \"0\" should be parsed to zero", "0"), Arguments.of("BigDecimal.ZERO should be zero", BigDecimal.ZERO), Arguments.of("BigInteger.ZERO should be zero", BigInteger.ZERO), Arguments.of("Negative zero double -0.0 should be zero", -0.0d));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of("Plain Object cannot be converted to double", new Object()), Arguments.of("Boolean.TRUE cannot be converted to double", Boolean.TRUE));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("Non‑numeric string \"notANumber\" should cause NumberFormatException", "notANumber"), Arguments.of("Mixed alphanumeric string \"123abc\" should cause NumberFormatException", "123abc"));
    }

    @Test
    void givenZeroDouble_returnsFalse() {
        // given
        Double zero = 0.0;
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(zero, converter);
        // then
        assertFalse(result, "Zero Double should be reported as not‑non‑zero");
    }

    @Test
    void givenNonZeroDouble_returnsTrue() {
        // given
        Double value = 3.14;
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(value, converter);
        // then
        assertTrue(result, "Non‑zero Double should be reported as non‑zero");
    }

    @Test
    void givenZeroFloat_returnsFalse() {
        // given
        Float zero = 0.0f;
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(zero, converter);
        // then
        assertFalse(result);
    }

    @Test
    void givenNonZeroFloat_returnsTrue() {
        // given
        Float value = -2.5f;
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(value, converter);
        // then
        assertTrue(result);
    }

    @Test
    void givenStringRepresentingZero_returnsFalse() {
        // given
        String zero = "0";
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(zero, converter);
        // then
        assertFalse(result);
    }

    @Test
    void givenStringRepresentingNonZeroFloat_returnsTrue() {
        // given
        String value = "0.001";
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(value, converter);
        // then
        assertTrue(result);
    }

    @Test
    void givenBigDecimalZero_returnsFalse() {
        // given
        java.math.BigDecimal zero = java.math.BigDecimal.ZERO;
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(zero, converter);
        // then
        assertFalse(result);
    }

    @Test
    void givenBigDecimalNonZero_returnsTrue() {
        // given
        java.math.BigDecimal value = new java.math.BigDecimal("123.456");
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(value, converter);
        // then
        assertTrue(result);
    }

    @Test
    void givenNull_returnsFalse() {
        // given
        Object nullObj = null;
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(nullObj, converter);
        // then
        // toDouble(null, …) yields 0.0, therefore the method should return false
        assertFalse(result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("positiveNonZeroProvider")
    void givenPositiveNonZeroValues_returnsTrue(String description, Object from) {
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(from, converter);
        // then
        assertTrue(result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zeroValuesProvider")
    void givenZeroValues_returnsFalse(String description, Object from) {
        // when
        boolean result = NumberConversions.isFloatTypeNotZero(from, converter);
        // then
        assertFalse(result, description);
    }

    @Test
    @DisplayName("null source object should throw NullPointerException")
    void givenNullSource_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> NumberConversions.isFloatTypeNotZero(null, converter), "Passing a null 'from' argument should result in NPE");
    }

    @Test
    @DisplayName("null Converter instance should throw NullPointerException")
    void givenNullConverter_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> NumberConversions.isFloatTypeNotZero(5.0d, null), "Passing a null Converter should result in NPE");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedTypesProvider")
    void givenUnsupportedType_throwsIllegalArgumentException(String description, Object from) {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.isFloatTypeNotZero(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidStringProvider")
    void givenInvalidNumericString_throwsNumberFormatException(String description, String from) {
        // when / then
        assertThrows(NumberFormatException.class, () -> NumberConversions.isFloatTypeNotZero(from, converter), description);
    }
}
