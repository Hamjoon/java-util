package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class YearConversions_toLong_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Simple placeholder class used to verify that a custom {@link Converter}
     * can be plugged in.  The actual conversion logic is supplied by the mock.
     */
    private static final class CustomNumber {

        private final int value;

        CustomNumber(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance.  The default constructor provides
        // a sensible, empty configuration suitable for unit‑tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Allow the GC to reclaim the Converter (not strictly required).
        converter = null;
    }

    static Stream<Arguments> validNumericConversions() {
        return Stream.of(arguments(Integer.valueOf(42), converter, 42L, "Integer input should be converted to the same numeric value as a long."), arguments(Long.valueOf(123456789L), converter, 123456789L, "Long input is accepted; toLong delegates to toInt which truncates to int then widens back to long, preserving the value within int range."), arguments("2020", converter, 2020L, "String containing a valid integer representation should be parsed and returned as a long."), arguments(Year.of(1999), converter, 1999L, "java.time.Year instance should be converted to its numeric year value."), arguments(Double.valueOf(3.14), converter, 3L, "Floating‑point numbers are truncated to their integer part before being returned as a long."));
    }

    static Stream<Arguments> nullAndUnsupportedInputs() {
        return Stream.of(arguments(null, converter, IllegalArgumentException.class, "Passing null should cause the method to throw an IllegalArgumentException (as per conversion contract)."), arguments(Boolean.TRUE, converter, IllegalArgumentException.class, "A type that cannot be mapped to an integer (e.g., Boolean) must result in an IllegalArgumentException."));
    }

    static Stream<Arguments> boundaryNumericConversions() {
        return Stream.of(arguments(Long.MAX_VALUE, converter, 2147483647L, "Long value exceeding int range is truncated to Integer.MAX_VALUE when delegated to toInt, then widened back to long."), arguments(Long.MIN_VALUE, converter, -2147483648L, "Long value below int range is truncated to Integer.MIN_VALUE when delegated to toInt, then widened back to long."), arguments(new BigInteger("5000000000"), converter, 705032704L, "BigInteger larger than int max is cast to int (overflow) resulting in 705032704, then returned as long."), arguments(new BigDecimal("12345.67"), converter, 12345L, "BigDecimal with fractional part is truncated to its integer component before conversion to long."));
    }

    static Stream<Arguments> complexTypeConversions() {
        return Stream.of(arguments(new AtomicInteger(77), converter, 77L, "AtomicInteger value is extracted, converted to int, then returned as long."), arguments(new AtomicLong(1234567890123L), converter, 1912276171L, "AtomicLong beyond int range is truncated to int (1912276171) before being widened to long."), arguments(Collections.singletonMap(YEAR, Integer.valueOf(1995)), converter, 1995L, "Map containing the YEAR key with an integer value is converted to that integer and returned as long."));
    }

    static Stream<Arguments> malformedInputs() {
        return Stream.of(arguments("", converter, IllegalArgumentException.class, "Empty string cannot be parsed to an integer and should cause IllegalArgumentException."), arguments("  abc  ", converter, IllegalArgumentException.class, "String with non‑numeric characters (even with surrounding whitespace) should result in IllegalArgumentException."), arguments(Collections.emptyMap(), converter, IllegalArgumentException.class, "Map without the YEAR key cannot be converted to a numeric year and must throw IllegalArgumentException."), arguments(Boolean.FALSE, converter, IllegalArgumentException.class, "Boolean false, like any unsupported type, should trigger IllegalArgumentException."), arguments(new Object(), converter, IllegalArgumentException.class, "A completely unrelated object type cannot be converted and must cause IllegalArgumentException."));
    }

    static Stream<Arguments> floatingPointEdgeCases() {
        return Stream.of(arguments(Double.NaN, converter, IllegalArgumentException.class, "NaN cannot be converted to an integer and should cause IllegalArgumentException."), arguments(Double.POSITIVE_INFINITY, converter, IllegalArgumentException.class, "Positive infinity is not a finite number and should cause IllegalArgumentException."), arguments(Float.NEGATIVE_INFINITY, converter, IllegalArgumentException.class, "Negative infinity for Float should cause IllegalArgumentException."));
    }

    static Stream<Arguments> mapWithInvalidYearValue() {
        return Stream.of(arguments(Collections.singletonMap(YEAR, "notANumber"), converter, IllegalArgumentException.class, "Map contains YEAR key but value is a non‑numeric string; conversion must fail."), arguments(Collections.singletonMap(YEAR, null), converter, IllegalArgumentException.class, "Map contains YEAR key with a null value; should throw IllegalArgumentException."));
    }

    @Test
    void toLong_givenYear_returnsYearValueAsLong() {
        // GIVEN
        Year year = Year.of(2023);
        // WHEN
        long result = YearConversions.toLong(year, converter);
        // THEN
        assertEquals(2023L, result, "YearConversions.toLong should return the numeric year value as a long");
    }

    @Test
    void toLong_givenInteger_returnsSameValueAsLong() {
        // GIVEN
        Integer number = 1999;
        // WHEN
        long result = YearConversions.toLong(number, converter);
        // THEN
        assertEquals(1999L, result, "Converting an Integer should yield the same numeric value as a long");
    }

    @Test
    void toLong_givenStringNumber_returnsParsedLong() {
        // GIVEN
        String numericString = "1984";
        // WHEN
        long result = YearConversions.toLong(numericString, converter);
        // THEN
        assertEquals(1984L, result, "A numeric String should be parsed to its long representation");
    }

    @Test
    void toLong_givenNull_returnsZero() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        long result = YearConversions.toLong(nullObj, converter);
        // THEN
        assertEquals(0L, result, "A null input should be converted to 0 for primitive numeric targets");
    }

    @Test
    void toLong_givenUnsupportedType_throwsException() {
        // GIVEN
        // no conversion rule for plain Object → long
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> YearConversions.toLong(unsupported, converter), "Converting an unsupported type should result in a conversion exception");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validNumericConversions")
    void toLong_validNumericConversions(Object from, Converter conv, long expected, String description) {
        long result = YearConversions.toLong(from, conv);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullAndUnsupportedInputs")
    void toLong_nullOrUnsupported(Object from, Converter conv, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> YearConversions.toLong(from, conv), description);
    }

    @Test
    void toLong_customConverter_knowsHowToConvert() {
        // Arrange – a mock Converter that knows how to turn CustomNumber into an Integer
        Converter customConverter = Mockito.mock(Converter.class);
        // Simulate: convert(any CustomNumber, Integer.class) -> 777
        Mockito.when(customConverter.convert(any(), eq(Integer.class))).thenReturn(777);
        CustomNumber custom = new CustomNumber(777);
        // Act
        long result = YearConversions.toLong(custom, customConverter);
        // Assert
        assertEquals(777L, result, "When a user‑provided Converter knows how to turn a CustomNumber into an Integer, toLong should return the converted value.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("boundaryNumericConversions")
    void toLong_boundaryNumericConversions(Object from, Converter conv, long expected, String description) {
        long result = YearConversions.toLong(from, conv);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("complexTypeConversions")
    void toLong_complexTypeConversions(Object from, Converter conv, long expected, String description) {
        long result = YearConversions.toLong(from, conv);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("malformedInputs")
    void toLong_malformedInputs(Object from, Converter conv, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> YearConversions.toLong(from, conv), description);
    }

    @Test
    void toLong_customConverterReturnsNull_throwsIllegalArgument() {
        // Mock a Converter that returns null for the Integer conversion
        Converter customConverter = Mockito.mock(Converter.class);
        Mockito.when(customConverter.convert(any(), eq(Integer.class))).thenReturn(null);
        CustomNumber custom = new CustomNumber(777);
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toLong(custom, customConverter), "When a custom Converter returns null for the conversion, toLong should treat it as a failure and throw IllegalArgumentException.");
    }

    @Test
    void toLong_nullConverter_throwsNullPointerException() {
        CustomNumber custom = new CustomNumber(555);
        assertThrows(NullPointerException.class, () -> YearConversions.toLong(custom, null), "Passing a null Converter reference should result in a NullPointerException before any conversion logic is attempted.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("floatingPointEdgeCases")
    void toLong_floatingPointEdgeCases(Object from, Converter conv, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> YearConversions.toLong(from, conv), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("mapWithInvalidYearValue")
    void toLong_mapYearInvalidValue(Object from, Converter conv, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> YearConversions.toLong(from, conv), description);
    }

    @Test
    void toLong_whitespaceOnlyString_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toLong("   ", converter), "String consisting only of whitespace cannot be parsed to an integer and must raise IllegalArgumentException.");
    }

    @Test
    void toLong_converterThrowsUncheckedException_propagates() {
        // Mock a Converter that throws IllegalStateException when asked to convert
        Converter throwingConverter = Mockito.mock(Converter.class);
        Mockito.when(throwingConverter.convert(any(), eq(Integer.class))).thenThrow(new IllegalStateException("boom"));
        assertThrows(IllegalStateException.class, () -> YearConversions.toLong(Integer.valueOf(10), throwingConverter), "Custom Converter throws IllegalStateException; toLong should propagate the unchecked exception unchanged.");
    }
}
