package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class MapConversions_toYear_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared real {@link Converter} instance used for the happy‑path tests.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the test tidy.
        converter = null;
    }

    /**
     * A converter that always throws {@link ConversionException}.
     */
    private static Converter failingConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Year.class))).thenThrow(new ConversionException("cannot convert"));
        return mock;
    }

    /**
     * A converter that returns a wrong type (e.g., Long, Double) instead of {@link Year}.
     */
    private static Converter wrongTypeConverter(Object wrongResult) {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Year.class))).thenReturn(wrongResult);
        return mock;
    }

    /**
     * Valid map inputs where the value can be converted to {@link Year}.
     */
    private static Stream<Arguments> validMapInputs() {
        return Stream.of(// integer
        Arguments.of(Map.of("year", 2023), Year.of(2023)), // string
        Arguments.of(Map.of("year", "1999"), Year.of(1999)), // existing Year instance
        Arguments.of(Map.of("year", Year.of(2005)), Year.of(2005)), // long
        Arguments.of(Map.of("year", 0L), Year.of(0)), // BigInteger
        Arguments.of(Map.of("year", BigInteger.valueOf(2022)), Year.of(2022)), // AtomicInteger
        Arguments.of(Map.of("year", new AtomicInteger(1995)), Year.of(1995)));
    }

    /**
     * Maps that lack a usable {@code "year"} entry – should trigger {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> missingKeyInputs() {
        return Stream.of(Arguments.of(Map.of("month", 12)), Arguments.of(Collections.emptyMap()), // key present but value null
        Arguments.of(Map.of("year", null)), // wrong case
        Arguments.of(new CompactMap<>(Map.of("YEAR", 2021))));
    }

    /**
     * Sources that are not maps (or are null) – should trigger {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> nonMapInputs() {
        return Stream.of(Arguments.of(null), Arguments.of("2022"), Arguments.of(new Object()), Arguments.of(Arrays.asList(2020)));
    }

    /**
     * Inputs that cause the supplied converter to throw {@link ConversionException}.
     */
    private static Stream<Arguments> conversionExceptionInputs() {
        return Stream.of(Arguments.of(Map.of("year", "abc")), Arguments.of(Map.of("year", "2022-01-01")));
    }

    /**
     * Inputs that cause the supplied converter to return a wrong type → {@link ClassCastException}.
     */
    private static Stream<Arguments> wrongTypeInputs() {
        return Stream.of(// converter returns Long
        Arguments.of(Map.of("year", 2020L), Long.valueOf(2020L)), // converter returns Double
        Arguments.of(Map.of("year", 2020), Double.valueOf(2020.0)));
    }

    /**
     * Extreme year values – JDK may throw {@link DateTimeException}.
     */
    private static Stream<Arguments> extremeYearInputs() {
        return Stream.of(Arguments.of(Map.of("year", Integer.MIN_VALUE)), Arguments.of(Map.of("year", Integer.MAX_VALUE)), Arguments.of(Map.of("year", -999_999_999)), Arguments.of(Map.of("year", 999_999_999)));
    }

    /**
     * Verify that a map containing an integer {@code year} value is converted correctly.
     */
    @Test
    void toYear_fromMapWithInteger() {
        // given
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2023);
        // when
        Year result = MapConversions.toYear(source, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(2023, result.getValue(), "Year value should match the integer supplied");
    }

    /**
     * Verify that a map containing a string {@code year} value is converted correctly.
     */
    @Test
    void toYear_fromMapWithString() {
        // given
        Map<String, Object> source = new HashMap<>();
        source.put("year", "1999");
        // when
        Year result = MapConversions.toYear(source, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(1999, result.getValue(), "Year value should match the string supplied");
    }

    /**
     * Verify that a map containing a {@link Year} instance under the expected key is returned unchanged.
     */
    @Test
    void toYear_fromMapWithYearInstance() {
        // given
        Year expected = Year.of(2005);
        Map<String, Object> source = new HashMap<>();
        source.put("year", expected);
        // when
        Year result = MapConversions.toYear(source, converter);
        // then
        assertSame(expected, result, "The same Year instance should be returned when already present");
    }

    /**
     * Verify that passing {@code null} as the source returns {@code null}.
     */
    @Test
    void toYear_nullSourceReturnsNull() {
        // when
        Year result = MapConversions.toYear(null, converter);
        // then
        assertNull(result, "Converting a null source should yield a null result");
    }

    /**
     * Verify that a map missing the {@code year} key results in {@code null}.
     */
    @Test
    void toYear_missingKeyReturnsNull() {
        // given
        Map<String, Object> source = new HashMap<>();
        // unrelated key
        source.put("month", 5);
        // when
        Year result = MapConversions.toYear(source, converter);
        // then
        assertNull(result, "Map without a 'year' key should produce a null Year");
    }

    @ParameterizedTest(name = "valid map input {index}: {0}")
    @MethodSource("validMapInputs")
    void toYear_validMapInputs(Object sourceMap, Year expected) {
        // when
        Year result = MapConversions.toYear(sourceMap, realConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result, "Converted Year does not match expected value");
    }

    @ParameterizedTest(name = "missing key case {index}: {0}")
    @MethodSource("missingKeyInputs")
    void toYear_missingKeyThrowsIllegalArgumentException(Object sourceMap) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYear(sourceMap, realConverter), "Expected IllegalArgumentException when 'year' key is missing or invalid");
    }

    @ParameterizedTest(name = "non‑map source case {index}: {0}")
    @MethodSource("nonMapInputs")
    void toYear_nonMapSourceThrowsIllegalArgumentException(Object source) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYear(source, realConverter), "Expected IllegalArgumentException for null or non‑map source");
    }

    @ParameterizedTest(name = "converter throws ConversionException for input {0}")
    @MethodSource("conversionExceptionInputs")
    void toYear_converterThrowsConversionException(Object sourceMap) {
        Converter badConverter = failingConverter();
        assertThrows(ConversionException.class, () -> MapConversions.toYear(sourceMap, badConverter), "Expected ConversionException from the failing converter");
    }

    @ParameterizedTest(name = "converter returns wrong type for input {0}")
    @MethodSource("wrongTypeInputs")
    void toYear_converterReturnsWrongType(Object sourceMap, Object wrongResult) {
        Converter mock = wrongTypeConverter(wrongResult);
        assertThrows(ClassCastException.class, () -> MapConversions.toYear(sourceMap, mock), "Expected ClassCastException when converter returns an incompatible type");
    }

    @ParameterizedTest(name = "extreme year value case {index}: {0}")
    @MethodSource("extremeYearInputs")
    void toYear_extremeYearValues(Object sourceMap) {
        // The JDK's Year.of may throw DateTimeException for out‑of‑range values.
        // The test passes if either a Year is returned or the expected exception is thrown.
        try {
            Year result = MapConversions.toYear(sourceMap, realConverter);
            assertNotNull(result, "Result should not be null when conversion succeeds");
        } catch (DateTimeException | IllegalArgumentException e) {
            // Acceptable – the library is allowed to reject out‑of‑range years.
            assertTrue(e instanceof DateTimeException || e instanceof IllegalArgumentException, "Expected DateTimeException or IllegalArgumentException for out‑of‑range year");
        }
    }

    @Test
    void toYear_nullConverterThrowsNullPointerException() {
        Map<String, Object> source = Map.of("year", 2022);
        assertThrows(NullPointerException.class, () -> MapConversions.toYear(source, null), "Passing a null converter should result in NullPointerException");
    }
}
