package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class YearConversions_toDouble_Test {

    /**
     * Converter used for all tests.
     */
    private static Converter converter;

    /**
     * Converter used for the majority of tests – the real implementation.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // built‑in conversions we need in these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Allow the GC to reclaim the Converter instance.
        converter = null;
    }

    /**
     * Returns a Converter that always returns the supplied {@code result} when asked to convert to {@code Integer}.
     */
    private static Converter stubReturning(final Object result) {
        return new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                if (toClass == Integer.class) {
                    @SuppressWarnings("unchecked")
                    T r = (T) result;
                    return r;
                }
                // delegate to the real implementation for any other conversion
                return super.convert(from, toClass);
            }
        };
    }

    /**
     * Returns a Converter that throws the supplied exception when converting to {@code Integer}.
     */
    private static Converter stubThrowing(final RuntimeException ex) {
        return new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                if (toClass == Integer.class) {
                    throw ex;
                }
                return super.convert(from, toClass);
            }
        };
    }

    static Stream<Arguments> nullInputProvider() {
        return Stream.of(arguments(null, defaultConverter, 0.0, "null input should yield 0.0"));
    }

    static Stream<Arguments> numberInputProvider() {
        return Stream.of(arguments(42, defaultConverter, 42.0, "Integer should be converted directly"), arguments(3.14, defaultConverter, 3.0, "Floating point is truncated"));
    }

    static Stream<Arguments> stringNumericProvider() {
        return Stream.of(arguments("123", defaultConverter, 123.0, "Simple numeric string"), arguments("-7", defaultConverter, -7.0, "Signed numeric string"));
    }

    static Stream<Arguments> stringNonNumericProvider() {
        return Stream.of(arguments("abc", defaultConverter, NumberFormatException.class, "Non‑numeric string"));
    }

    static Stream<Arguments> yearInputProvider() {
        return Stream.of(arguments(Year.of(2020), defaultConverter, 2020.0, "Year instance"));
    }

    static Stream<Arguments> mapWithYearKeyProvider() {
        Map<String, Object> map = new HashMap<>();
        map.put("year", 1999);
        return Stream.of(arguments(map, defaultConverter, 1999.0, "Map containing YEAR key"));
    }

    static Stream<Arguments> unsupportedTypeProvider() {
        Converter stub = stubReturning(555);
        return Stream.of(arguments(new Object(), stub, 555.0, "Delegated conversion via stub"));
    }

    static Stream<Arguments> atomicTypesProvider() {
        return Stream.of(arguments(new AtomicInteger(7), defaultConverter, 7.0, "AtomicInteger"), arguments(new AtomicLong(5L), defaultConverter, 5.0, "AtomicLong within int range"), arguments(new AtomicBoolean(true), defaultConverter, 1.0, "AtomicBoolean true"), arguments(new AtomicBoolean(false), defaultConverter, 0.0, "AtomicBoolean false"));
    }

    static Stream<Arguments> bigNumberTypesProvider() {
        return Stream.of(arguments(new BigDecimal("12345.678"), defaultConverter, 12345.0, "BigDecimal truncated"), arguments(new BigInteger("2147483646"), defaultConverter, 2147483646.0, "BigInteger within int range"), arguments(new BigInteger("-2147483647"), defaultConverter, -2147483647.0, "Negative BigInteger"));
    }

    static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(arguments(Integer.MAX_VALUE, defaultConverter, (double) Integer.MAX_VALUE, "Max int"), arguments(Integer.MIN_VALUE, defaultConverter, (double) Integer.MIN_VALUE, "Min int"), arguments(Year.of(999_999_999), defaultConverter, 999_999_999.0, "Upper bound Year"), arguments(Year.of(-999_999_999), defaultConverter, -999_999_999.0, "Lower bound Year"));
    }

    static Stream<Arguments> mapEdgeCasesProvider() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("year", "2022");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("year", "abc");
        Map<String, Object> map3 = new HashMap<>();
        map3.put("other", 1234);
        Map<String, Object> map4 = new HashMap<>();
        map4.put("year", null);
        Converter stub777 = stubReturning(777);
        return Stream.of(arguments(map1, defaultConverter, 2022.0, "Map YEAR key as numeric String"), arguments(map2, defaultConverter, NumberFormatException.class, "Map YEAR key non‑numeric"), arguments(map3, stub777, 777.0, "Map without YEAR key – delegated"), arguments(map4, defaultConverter, NumberFormatException.class, "Map YEAR key null"));
    }

    static Stream<Arguments> stringEdgeCasesProvider() {
        return Stream.of(arguments("  42 ", defaultConverter, 42.0, "String with whitespace"), arguments("", defaultConverter, NumberFormatException.class, "Empty string"), arguments("+15", defaultConverter, 15.0, "Explicit plus sign"), arguments("000123", defaultConverter, 123.0, "Leading zeros"));
    }

    static Stream<Arguments> converterExceptionPropagationProvider() {
        Converter stub = stubThrowing(new RuntimeException("boom"));
        return Stream.of(arguments(new Object(), stub, RuntimeException.class, "Converter throws RuntimeException"));
    }

    static Stream<Arguments> bigIntegerOverflowProvider() {
        return Stream.of(arguments(new BigInteger("2147483648"), defaultConverter, ArithmeticException.class, "Overflow positive"), arguments(new BigInteger("-2147483649"), defaultConverter, ArithmeticException.class, "Overflow negative"));
    }

    static Stream<Arguments> stringOverflowProvider() {
        return Stream.of(arguments("2147483648", defaultConverter, NumberFormatException.class, "String overflow positive"), arguments("-2147483649", defaultConverter, NumberFormatException.class, "String overflow negative"));
    }

    static Stream<Arguments> mapYearFloatStringProvider() {
        Map<String, Object> map = new HashMap<>();
        map.put("year", "12.34");
        return Stream.of(arguments(map, defaultConverter, NumberFormatException.class, "Map YEAR key with float string"));
    }

    static Stream<Arguments> converterReturnsNullProvider() {
        Converter stub = stubReturning(null);
        return Stream.of(arguments(new Object(), stub, NullPointerException.class, "Converter returns null"));
    }

    static Stream<Arguments> converterReturnsUnsupportedProvider() {
        Converter stub = stubReturning(new Object());
        return Stream.of(arguments(new Object(), stub, ClassCastException.class, "Converter returns unsupported type"));
    }

    @Test
    void toDouble_fromYear_returnsYearValueAsDouble() {
        // GIVEN
        Year year = Year.of(2023);
        // WHEN
        double result = YearConversions.toDouble(year, converter);
        // THEN
        assertEquals(2023.0, result, 0.000001, "Year should be converted to its numeric value as a double");
    }

    @Test
    void toDouble_fromStringNumber_returnsParsedValue() {
        // GIVEN
        String number = "1999";
        // WHEN
        double result = YearConversions.toDouble(number, converter);
        // THEN
        assertEquals(1999.0, result, 0.000001, "String containing a number should be parsed and returned as double");
    }

    @Test
    void toDouble_fromInteger_returnsSameNumericValue() {
        // GIVEN
        Integer value = 42;
        // WHEN
        double result = YearConversions.toDouble(value, converter);
        // THEN
        assertEquals(42.0, result, 0.000001, "Integer should be converted to double preserving its value");
    }

    @Test
    void toDouble_fromNull_returnsZero() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        double result = YearConversions.toDouble(nullObj, converter);
        // THEN
        assertEquals(0.0, result, 0.000001, "Null input should be converted to 0.0 according to Converter's null handling policy");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullInputProvider")
    void testNullInput(Object from, Converter conv, double expected, String desc) {
        double result = YearConversions.toDouble(from, conv);
        assertEquals(expected, result, 0.000001);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("numberInputProvider")
    void testNumberInput(Object from, Converter conv, double expected, String desc) {
        double result = YearConversions.toDouble(from, conv);
        assertEquals(expected, result, 0.000001);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringNumericProvider")
    void testStringNumeric(Object from, Converter conv, double expected, String desc) {
        double result = YearConversions.toDouble(from, conv);
        assertEquals(expected, result, 0.000001);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringNonNumericProvider")
    void testStringNonNumeric(Object from, Converter conv, Class<? extends Throwable> exClass, String desc) {
        assertThrows(exClass, () -> YearConversions.toDouble(from, conv));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("yearInputProvider")
    void testYearInput(Object from, Converter conv, double expected, String desc) {
        double result = YearConversions.toDouble(from, conv);
        assertEquals(expected, result, 0.000001);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("mapWithYearKeyProvider")
    void testMapWithYearKey(Object from, Converter conv, double expected, String desc) {
        double result = YearConversions.toDouble(from, conv);
        assertEquals(expected, result, 0.000001);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unsupportedTypeProvider")
    void testUnsupportedTypeDelegated(Object from, Converter conv, double expected, String desc) {
        double result = YearConversions.toDouble(from, conv);
        assertEquals(expected, result, 0.000001);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("atomicTypesProvider")
    void testAtomicTypes(Object from, Converter conv, double expected, String desc) {
        double result = YearConversions.toDouble(from, conv);
        assertEquals(expected, result, 0.000001);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("bigNumberTypesProvider")
    void testBigNumberTypes(Object from, Converter conv, double expected, String desc) {
        double result = YearConversions.toDouble(from, conv);
        assertEquals(expected, result, 0.000001);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("boundaryValuesProvider")
    void testBoundaryValues(Object from, Converter conv, double expected, String desc) {
        double result = YearConversions.toDouble(from, conv);
        assertEquals(expected, result, 0.000001);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("mapEdgeCasesProvider")
    void testMapEdgeCases(Object from, Converter conv, Object expected, String desc) {
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expected;
            assertThrows(ex, () -> YearConversions.toDouble(from, conv));
        } else {
            double result = YearConversions.toDouble(from, conv);
            assertEquals(((Number) expected).doubleValue(), result, 0.000001);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringEdgeCasesProvider")
    void testStringEdgeCases(Object from, Converter conv, Object expected, String desc) {
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expected;
            assertThrows(ex, () -> YearConversions.toDouble(from, conv));
        } else {
            double result = YearConversions.toDouble(from, conv);
            assertEquals(((Number) expected).doubleValue(), result, 0.000001);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("converterExceptionPropagationProvider")
    void testConverterExceptionPropagation(Object from, Converter conv, Class<? extends Throwable> exClass, String desc) {
        assertThrows(exClass, () -> YearConversions.toDouble(from, conv));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("bigIntegerOverflowProvider")
    void testBigIntegerOverflow(Object from, Converter conv, Class<? extends Throwable> exClass, String desc) {
        assertThrows(exClass, () -> YearConversions.toDouble(from, conv));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringOverflowProvider")
    void testStringOverflow(Object from, Converter conv, Class<? extends Throwable> exClass, String desc) {
        assertThrows(exClass, () -> YearConversions.toDouble(from, conv));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("mapYearFloatStringProvider")
    void testMapYearFloatString(Object from, Converter conv, Class<? extends Throwable> exClass, String desc) {
        assertThrows(exClass, () -> YearConversions.toDouble(from, conv));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("converterReturnsNullProvider")
    void testConverterReturnsNull(Object from, Converter conv, Class<? extends Throwable> exClass, String desc) {
        assertThrows(exClass, () -> YearConversions.toDouble(from, conv));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("converterReturnsUnsupportedProvider")
    void testConverterReturnsUnsupported(Object from, Converter conv, Class<? extends Throwable> exClass, String desc) {
        assertThrows(exClass, () -> YearConversions.toDouble(from, conv));
    }
}
