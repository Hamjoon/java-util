package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.Year;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

class YearConversions_toAtomicBoolean_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default configuration – it contains the built‑in int conversions.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> positiveNonZeroProvider() {
        return Stream.of(// Integer 5
        Arguments.of(5), // String "123"
        Arguments.of("123"), // Year object → 2022
        Arguments.of(Year.of(2022)));
    }

    private static Stream<Arguments> zeroConversionProvider() {
        return Stream.of(// Integer zero
        Arguments.of(0), // String zero
        Arguments.of("0"), // Null source (treated as 0)
        Arguments.of(null));
    }

    private static Stream<Arguments> negativeNumericProvider() {
        return Stream.of(// Negative integer
        Arguments.of(-7), // Negative numeric string
        Arguments.of("-42"));
    }

    @Test
    void toAtomicBoolean_returnsFalseWhenZero() {
        // GIVEN
        Integer source = 0;
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(source, converter);
        // THEN
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false for integer value 0");
    }

    @Test
    void toAtomicBoolean_returnsTrueWhenNonZeroInteger() {
        // GIVEN
        Integer source = 42;
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(source, converter);
        // THEN
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero integer");
    }

    @Test
    void toAtomicBoolean_returnsTrueWhenStringRepresentsNonZeroNumber() {
        // GIVEN
        String source = "7";
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(source, converter);
        // THEN
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true for string \"7\"");
    }

    @Test
    void toAtomicBoolean_returnsFalseWhenStringRepresentsZero() {
        // GIVEN
        String source = "0";
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(source, converter);
        // THEN
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false for string \"0\"");
    }

    @Test
    void toAtomicBoolean_returnsFalseWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(source, converter);
        // THEN
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source is null (converted to 0)");
    }

    @Test
    void toAtomicBoolean_returnsTrueWhenBooleanTrueConvertedToInt() {
        // GIVEN
        // Converter should map true -> 1
        Boolean source = true;
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(source, converter);
        // THEN
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true when source Boolean true converts to 1");
    }

    @Test
    void toAtomicBoolean_returnsFalseWhenBooleanFalseConvertedToInt() {
        // GIVEN
        // Converter should map false -> 0
        Boolean source = false;
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(source, converter);
        // THEN
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source Boolean false converts to 0");
    }

    /* ------------------------------------------------------------
     *  Positive – non‑zero conversion results
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] from={0} → true")
    @MethodSource("positiveNonZeroProvider")
    void toAtomicBoolean_positiveNonZero(Object from) {
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(from, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero conversion of " + from);
    }

    /* ------------------------------------------------------------
     *  Negative – zero conversion results
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] from={0} → false")
    @MethodSource("zeroConversionProvider")
    void toAtomicBoolean_zeroConversion(Object from) {
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(from, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertFalse(result.get(), "AtomicBoolean should be false for zero conversion of " + from);
    }

    /* ------------------------------------------------------------
     *  Negative numeric conversion (non‑zero negative values)
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] from={0} → true")
    @MethodSource("negativeNumericProvider")
    void toAtomicBoolean_negativeNumeric(Object from) {
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(from, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero negative conversion of " + from);
    }

    /* ------------------------------------------------------------
     *  Error handling – null converter
     * ------------------------------------------------------------ */
    @Test
    void toAtomicBoolean_nullConverter_throwsNPE() {
        // GIVEN
        Object source = 1;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> YearConversions.toAtomicBoolean(source, null), "Passing a null Converter should throw NullPointerException");
    }

    /* ------------------------------------------------------------
     *  Error handling – non‑numeric string
     * ------------------------------------------------------------ */
    @Test
    void toAtomicBoolean_nonNumericString_throwsNumberFormatException() {
        // GIVEN
        String source = "notANumber";
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicBoolean(source, converter), "A non‑numeric string should cause NumberFormatException");
    }

    /* ------------------------------------------------------------
     *  Additional explicit tests (kept for readability)
     * ------------------------------------------------------------ */
    @Test
    void toAtomicBoolean_booleanTrue_convertsToTrue() {
        // GIVEN
        // Expected to map to int 1
        Boolean source = true;
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(source, converter);
        // THEN
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true when source Boolean true converts to 1");
    }

    @Test
    void toAtomicBoolean_booleanFalse_convertsToFalse() {
        // GIVEN
        // Expected to map to int 0
        Boolean source = false;
        // WHEN
        AtomicBoolean result = YearConversions.toAtomicBoolean(source, converter);
        // THEN
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source Boolean false converts to 0");
    }
}
