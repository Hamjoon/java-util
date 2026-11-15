package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class YearConversions_toFloat_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a real Converter – the implementation of Converter is not
        // exercised by toFloat, but a non‑null instance satisfies the method signature.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – not strictly required but keeps the pattern clear.
        converter = null;
    }

    private static Stream<Arguments> numericConversionProvider() {
        return Stream.of(// Year instance
        arguments(Year.of(2023), 2023.0f), // String containing integer (with and without whitespace)
        arguments("1999", 1999.0f), arguments("   42   ", 42.0f), // Primitive / wrapper numbers
        arguments(12345, 12345.0f), arguments(12345L, 12345.0f), arguments(12.99d, 12.0f), arguments(5.5f, 5.0f), // BigDecimal / BigInteger
        arguments(new BigDecimal("12.34"), 12.0f), // overflow case
        arguments(new BigDecimal("2147483648.9"), -2147483648.0f), // far overflow
        arguments(new BigDecimal("1E40"), -2147483648.0f), arguments(new BigInteger("5678"), 5678.0f), arguments(new BigInteger("2147483648"), -2147483648.0f), // Atomic types
        arguments(new AtomicInteger(42), 42.0f), arguments(new AtomicLong(99L), 99.0f), arguments(new AtomicBoolean(true), 1.0f), arguments(new AtomicBoolean(false), 0.0f), // Boolean primitives
        arguments(true, 1.0f), arguments(false, 0.0f), // Map containing the YEAR key (using the constant from MapConversions)
        arguments(Map.of(MapConversions.YEAR, Year.of(1995)), 1995.0f), // Edge cases: NaN and infinities
        arguments(Float.NaN, 0.0f), arguments(Float.POSITIVE_INFINITY, 0.0f), arguments(Float.NEGATIVE_INFINITY, 0.0f), arguments(Double.NaN, 0.0f), arguments(Double.POSITIVE_INFINITY, 0.0f), arguments(Double.NEGATIVE_INFINITY, 0.0f), // Integer.MAX_VALUE and overflow via long
        arguments(Integer.MAX_VALUE, (float) Integer.MAX_VALUE), // long value one greater than int max
        arguments(2147483648L, -2147483648.0f));
    }

    private static Stream<Arguments> malformedStringProvider() {
        return Stream.of(arguments("not a number"), arguments(""), arguments("   "), arguments("+"));
    }

    @Test
    void toFloatFromYear_returnsYearAsFloat() {
        // given
        Year year = Year.of(2023);
        // when
        float result = YearConversions.toFloat(year, converter);
        // then
        assertEquals(2023.0f, result, "Year should be converted to its numeric value as a float");
    }

    @Test
    void toFloatFromStringNumeric_returnsParsedFloat() {
        // given
        String numericString = "1234";
        // when
        float result = YearConversions.toFloat(numericString, converter);
        // then
        assertEquals(1234.0f, result, "String containing an integer should be parsed and returned as float");
    }

    @Test
    void toFloatFromInteger_returnsFloatValue() {
        // given
        Integer intValue = 42;
        // when
        float result = YearConversions.toFloat(intValue, converter);
        // then
        assertEquals(42.0f, result, "Integer should be converted to the same numeric value as float");
    }

    @Test
    void toFloatFromNull_returnsZero() {
        // given
        Object nullValue = null;
        // when
        float result = YearConversions.toFloat(nullValue, converter);
        // then
        assertEquals(0.0f, result, "Null should be converted to 0.0f for primitive numeric targets");
    }

    @Test
    void toFloatFromMapWithYearKey_returnsYearAsFloat() {
        // given
        // The MapConversions.YEAR constant is used by the conversion engine to locate the value.
        // We simulate that by providing a map with the appropriate key.
        java.util.Map<String, Object> map = java.util.Map.of("year", 1999);
        // when
        float result = YearConversions.toFloat(map, converter);
        // then
        assertEquals(1999.0f, result, "Map containing a numeric year should be converted to that value as float");
    }

    @ParameterizedTest(name = "{index}: from={0} → expected={1}")
    @MethodSource("numericConversionProvider")
    void toFloat_numericConversions(Object from, float expected) {
        // when
        float result = YearConversions.toFloat(from, converter);
        // then
        assertEquals(expected, result, () -> "Conversion of " + from + " should yield " + expected);
    }

    @ParameterizedTest(name = "{index}: malformed string ''{0}'' should throw NumberFormatException")
    @MethodSource("malformedStringProvider")
    void toFloat_malformedString_throwsNumberFormatException(String malformed) {
        assertThrows(NumberFormatException.class, () -> YearConversions.toFloat(malformed, converter), "Parsing '" + malformed + "' should result in NumberFormatException");
    }

    @Test
    void toFloat_nullFromAndNullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> YearConversions.toFloat(null, null), "Both arguments null should cause NullPointerException");
    }

    @Test
    void toFloat_nullFrom_nonNullConverter_throwsNPE() {
        // a mock Converter is sufficient – the method never reaches it.
        Converter mockConverter = org.mockito.Mockito.mock(Converter.class);
        assertThrows(NullPointerException.class, () -> YearConversions.toFloat(null, mockConverter), "Null source with a non‑null converter should still throw NPE");
    }

    @Test
    void toFloat_unsupportedObject_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toFloat(unsupported, converter), "Unsupported object type should cause IllegalArgumentException");
    }

    @Test
    void toFloat_unsupportedArray_throwsIllegalArgumentException() {
        Object[] arr = new Object[] { "unexpected", 123 };
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toFloat(arr, converter), "Array of unsupported element types should cause IllegalArgumentException");
    }

    @Test
    void toFloat_dateInstance_throwsIllegalArgumentException() {
        java.util.Date date = new java.util.Date();
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toFloat(date, converter), "java.util.Date is not a supported source type");
    }
}
