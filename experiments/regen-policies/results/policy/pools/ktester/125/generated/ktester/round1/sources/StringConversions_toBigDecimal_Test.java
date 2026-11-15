package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toBigDecimal_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /* --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final BigDecimal expectedResult;

        final Class<? extends Throwable> expectedException;

        final String description;

        ArgumentsHolder(Object from, Converter converter, BigDecimal expectedResult, Class<? extends Throwable> expectedException, String description) {
            this.from = from;
            this.converter = converter;
            this.expectedResult = expectedResult;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor
        // provides a sensible configuration for the tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference (helps GC in large test suites).
        converter = null;
    }

    /**
     * Group: Empty and Null inputs
     */
    static Stream<ArgumentsHolder> emptyAndNullProvider() {
        return Stream.of(// Empty string, converter can be null (method does not use it)
        new ArgumentsHolder("", null, BigDecimal.ZERO, null, "Empty string should be considered empty and return BigDecimal.ZERO."), // Null input, also treated as empty
        new ArgumentsHolder(null, null, BigDecimal.ZERO, null, "Null input should be treated as empty and return BigDecimal.ZERO."));
    }

    /**
     * Group: Invalid type inputs
     */
    static Stream<ArgumentsHolder> invalidTypeProvider() {
        return Stream.of(new ArgumentsHolder(123, null, null, ClassCastException.class, "Passing a non‑String object (Integer) should cause ClassCastException."));
    }

    /**
     * Group: Non‑parseable strings
     */
    static Stream<ArgumentsHolder> nonParseableProvider() {
        return Stream.of(new ArgumentsHolder("abc", null, null, IllegalArgumentException.class, "Alphabetic string should trigger IllegalArgumentException."), new ArgumentsHolder("   ", null, null, IllegalArgumentException.class, "Whitespace‑only string should trigger IllegalArgumentException."), new ArgumentsHolder("+", null, null, IllegalArgumentException.class, "String containing only a sign should trigger IllegalArgumentException."));
    }

    @Test
    void emptyStringReturnsZero() {
        // GIVEN
        String input = "";
        // WHEN
        BigDecimal result = StringConversions.toBigDecimal(input, converter);
        // THEN
        assertEquals(BigDecimal.ZERO, result, "An empty string should be converted to BigDecimal.ZERO");
    }

    @Test
    void validNumberStringIsConverted() {
        // GIVEN
        String input = "12345.6789";
        // WHEN
        BigDecimal result = StringConversions.toBigDecimal(input, converter);
        // THEN
        assertEquals(new BigDecimal("12345.6789"), result, "The string should be parsed into the exact BigDecimal value");
    }

    @Test
    void invalidNumberStringThrowsIllegalArgumentException() {
        // GIVEN
        String input = "not-a-number";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigDecimal(input, converter), "A non‑numeric string must cause IllegalArgumentException");
        // The cause should be a NumberFormatException
        assertTrue(ex.getCause() instanceof NumberFormatException, "The cause of the IllegalArgumentException should be NumberFormatException");
        assertTrue(ex.getMessage().contains(input), "Exception message should contain the offending value");
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("emptyAndNullProvider")
    void emptyOrNullInputs(ArgumentsHolder holder) {
        // WHEN
        BigDecimal result = StringConversions.toBigDecimal(holder.from, holder.converter);
        // THEN
        assertEquals(holder.expectedResult, result, holder.description);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("invalidTypeProvider")
    void invalidTypeInputs(ArgumentsHolder holder) {
        // WHEN / THEN
        assertThrows(holder.expectedException, () -> StringConversions.toBigDecimal(holder.from, holder.converter), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("nonParseableProvider")
    void nonParseableStrings(ArgumentsHolder holder) {
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigDecimal(holder.from, holder.converter), holder.description);
        // Verify that the root cause is a NumberFormatException (as per implementation)
        assertTrue(ex.getCause() instanceof NumberFormatException, "Root cause should be NumberFormatException for input: " + holder.from);
        assertTrue(ex.getMessage().contains(String.valueOf(holder.from)), "Exception message should contain the offending value");
    }
}
