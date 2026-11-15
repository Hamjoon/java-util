package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.util.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class YearConversions_toBigDecimal_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // primitive and Year conversions we need in these tests.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Provides successful conversion scenarios.
     *
     * Each argument consists of:
     *   - the source object to convert,
     *   - the expected {@link BigDecimal} result.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Integer input → same numeric value
        arguments(2023, BigDecimal.valueOf(2023)), // String that can be parsed as an int year
        arguments("2025", BigDecimal.valueOf(2025)), // java.time.Year instance
        arguments(Year.of(2022), BigDecimal.valueOf(2022)), // Null source should be treated as 0
        arguments(null, BigDecimal.ZERO));
    }

    /**
     * Provides inputs that are expected to cause a {@link NumberFormatException}
     * because they cannot be interpreted as an integer year.
     */
    private static Stream<Arguments> numberFormatExceptionProvider() {
        return Stream.of(// java.util.Date is not convertible to a year number
        arguments(new Date()), // Non‑numeric string
        arguments("20AB"), // Numeric string outside the int range
        arguments("2147483648"));
    }

    @Test
    void toBigDecimal_FromYear_ReturnsYearAsBigDecimal() {
        Year year = Year.of(2023);
        BigDecimal result = YearConversions.toBigDecimal(year, converter);
        assertEquals(BigDecimal.valueOf(2023), result, "Year should be converted to its numeric value as BigDecimal");
    }

    @Test
    void toBigDecimal_FromInteger_ReturnsSameValueAsBigDecimal() {
        Integer source = 42;
        BigDecimal result = YearConversions.toBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(42), result, "Integer should be converted directly to BigDecimal");
    }

    @Test
    void toBigDecimal_FromStringNumeric_ReturnsParsedBigDecimal() {
        String source = "7";
        BigDecimal result = YearConversions.toBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(7), result, "Numeric String should be parsed and returned as BigDecimal");
    }

    @Test
    void toBigDecimal_FromNull_ReturnsZeroBigDecimal() {
        // According to Converter contract, null numeric conversion yields 0
        BigDecimal result = YearConversions.toBigDecimal(null, converter);
        assertEquals(BigDecimal.ZERO, result, "Null source should be converted to 0 as BigDecimal");
    }

    @Test
    void toBigDecimal_FromUnsupportedType_ThrowsException() {
        Object unsupported = new Object();
        assertThrows(RuntimeException.class, () -> YearConversions.toBigDecimal(unsupported, converter), "Unsupported source types should cause a conversion exception");
    }

    @ParameterizedTest(name = "toBigDecimal({0}) => {1}")
    @MethodSource("successfulConversionProvider")
    void toBigDecimal_SuccessfulConversions(Object from, BigDecimal expected) {
        BigDecimal result = YearConversions.toBigDecimal(from, converter);
        assertEquals(expected, result, () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should yield " + expected);
    }

    @ParameterizedTest(name = "toBigDecimal({0}) throws NumberFormatException")
    @MethodSource("numberFormatExceptionProvider")
    void toBigDecimal_NumberFormatException(Object from) {
        assertThrows(NumberFormatException.class, () -> YearConversions.toBigDecimal(from, converter), () -> "Converting " + from.getClass().getSimpleName() + " should throw NumberFormatException");
    }

    @Test
    void toBigDecimal_NullConverter_throwsNullPointerException() {
        // According to the JSON test case, passing a null Converter should
        // result in a NullPointerException when the method tries to dereference it.
        assertThrows(NullPointerException.class, () -> YearConversions.toBigDecimal("2024", null), "A null Converter must cause NullPointerException");
    }

    @Test
    void toBigDecimal_ConverterThrowsException_propagatesIllegalStateException() {
        // Create a mock Converter that throws IllegalStateException on any method call.
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new IllegalStateException("forced failure");
        });
        // The source is a valid numeric string; the conversion should fail because
        // the Converter itself throws.
        assertThrows(IllegalStateException.class, () -> YearConversions.toBigDecimal("2024", throwingConverter), "Exception from Converter must propagate as IllegalStateException");
    }
}
