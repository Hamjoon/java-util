package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

class YearConversions_toAtomicBoolean_Test {

    private Converter converter;

    /**
     * Converter that always returns the integer 42 for any conversion request.
     */
    static class MockConverterReturning42 extends Converter {

        MockConverterReturning42() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object source, Class<T> target) {
            // Force every conversion to the integer 42.
            return (T) Integer.valueOf(42);
        }
    }

    /**
     * Converter that always returns the integer 0 for any conversion request.
     */
    static class MockConverterReturning0 extends Converter {

        MockConverterReturning0() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object source, Class<T> target) {
            // Force every conversion to the integer 0.
            return (T) Integer.valueOf(0);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default ConverterOptions (empty) for a real Converter instance.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    /* --------------------------------------------------------------
     *  Parameter source for the normal (non‑exception) test cases.
     * -------------------------------------------------------------- */
    private static Stream<Arguments> normalCases() {
        return Stream.of(// ----- Zero conversion scenarios -----
        Arguments.of(0, false, "Integer zero should be false", null), Arguments.of("0", false, "String \"0\" should be false", null), Arguments.of(null, false, "Null source should be false", null), // ----- Non‑zero conversion scenarios -----
        Arguments.of(5, true, "Positive integer should be true", null), Arguments.of("123", true, "String numeric non‑zero should be true", null), Arguments.of(Integer.valueOf(-7), true, "Negative integer should be true", null), // ----- Boolean source scenarios -----
        Arguments.of(Boolean.TRUE, true, "Boolean true → true", null), Arguments.of(Boolean.FALSE, false, "Boolean false → false", null), // ----- Year and temporal types -----
        Arguments.of(Year.of(2023), true, "Year 2023 → true", null), Arguments.of(Year.of(0), false, "Year 0 → false", null), // ----- Numeric wrapper and primitive types -----
        Arguments.of(new BigInteger("42"), true, "BigInteger 42 → true", null), Arguments.of(new BigDecimal("0.0"), false, "BigDecimal 0.0 → false", null), Arguments.of(new AtomicInteger(9), true, "AtomicInteger 9 → true", null), Arguments.of(new AtomicLong(0L), false, "AtomicLong 0L → false", null), Arguments.of(3.14d, true, "Double 3.14 → true (int cast 3)", null), Arguments.of(0.0f, false, "Float 0.0f → false", null), Arguments.of((short) 0, false, "Short zero → false", null), // ----- Custom converter influence -----
        Arguments.of("any", true, "MockConverterReturning42 forces true", new MockConverterReturning42()), Arguments.of("any", false, "MockConverterReturning0 forces false", new MockConverterReturning0()));
    }

    @Test
    @DisplayName("Non‑zero numeric value yields true")
    void nonZeroNumberReturnsTrue() {
        AtomicBoolean result = YearConversions.toAtomicBoolean(42, converter);
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero int");
    }

    @Test
    @DisplayName("Zero numeric value yields false")
    void zeroNumberReturnsFalse() {
        AtomicBoolean result = YearConversions.toAtomicBoolean(0, converter);
        assertFalse(result.get(), "AtomicBoolean should be false for zero int");
    }

    @Test
    @DisplayName("Negative numeric value yields true")
    void negativeNumberReturnsTrue() {
        AtomicBoolean result = YearConversions.toAtomicBoolean(-7, converter);
        assertTrue(result.get(), "AtomicBoolean should be true for negative int");
    }

    @Test
    @DisplayName("Null source yields false")
    void nullSourceReturnsFalse() {
        AtomicBoolean result = YearConversions.toAtomicBoolean(null, converter);
        assertFalse(result.get(), "AtomicBoolean should be false when source is null");
    }

    @Test
    @DisplayName("Year instance yields true when year is non‑zero")
    void yearInstanceReturnsTrue() {
        Year year = Year.of(2023);
        AtomicBoolean result = YearConversions.toAtomicBoolean(year, converter);
        assertTrue(result.get(), "AtomicBoolean should be true for a non‑zero Year value");
    }

    @Test
    @DisplayName("String representing zero yields false")
    void stringZeroReturnsFalse() {
        AtomicBoolean result = YearConversions.toAtomicBoolean("0", converter);
        assertFalse(result.get(), "AtomicBoolean should be false for string \"0\"");
    }

    @Test
    @DisplayName("String representing non‑zero yields true")
    void stringNonZeroReturnsTrue() {
        AtomicBoolean result = YearConversions.toAtomicBoolean("123", converter);
        assertTrue(result.get(), "AtomicBoolean should be true for string \"123\"");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalCases")
    void testToAtomicBoolean(Object from, boolean expected, String description, Converter customConverter) {
        // Use the custom converter if supplied; otherwise fall back to the default one.
        Converter used = (customConverter != null) ? customConverter : converter;
        AtomicBoolean result = YearConversions.toAtomicBoolean(from, used);
        assertEquals(expected, result.get(), () -> String.format("Conversion of %s (using %s) should be %s", from, (customConverter != null ? customConverter.getClass().getSimpleName() : "default Converter"), expected));
    }

    /* --------------------------------------------------------------
     *  Exception scenarios – each verified with a dedicated test.
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("String that cannot be parsed as a number throws NumberFormatException")
    void stringNotANumberThrows() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicBoolean("notANumber", converter), "Parsing a non‑numeric string must propagate NumberFormatException");
    }

    @Test
    @DisplayName("Unsupported object type throws IllegalArgumentException")
    void unsupportedObjectThrows() {
        // plain Object has no conversion rule
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toAtomicBoolean(unsupported, converter), "Passing an unsupported type should result in IllegalArgumentException");
    }
}
