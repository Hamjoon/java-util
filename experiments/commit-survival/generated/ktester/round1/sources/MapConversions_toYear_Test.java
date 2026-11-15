package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toYear_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance for the simple “pass‑through” cases.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the default constructor provides a
        // sensible set of options for the library’s built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // The real Converter works for most numeric → Year conversions, so we can reuse it.
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the test tidy.
        converter = null;
    }

    /**
     * Creates a mutable map containing a single entry {@code "year" -> value}.
     */
    private static Map<String, Object> mapWithYear(Object value) {
        Map<String, Object> m = new HashMap<>();
        m.put(MapConversions.YEAR, value);
        return m;
    }

    /**
     * Creates a map with an extra unrelated entry – used to verify that extra keys are ignored.
     */
    private static Map<String, Object> mapWithYearAndExtra(Object value) {
        Map<String, Object> m = mapWithYear(value);
        m.put("extra", "ignore me");
        return m;
    }

    /**
     * Data for {@link #toYear_mapSuccessful(Object, String, int)}.
     */
    private static Stream<Arguments> provideSuccessfulMapConversions() {
        return Stream.of(Arguments.of(2025, "Integer value", 2025), Arguments.of("1999", "String numeric", 1999), Arguments.of(Year.of(2021), "Year instance inside map", 2021), Arguments.of(2022L, "Long within range", 2022), Arguments.of((short) 1995, "Short value", 1995), Arguments.of((byte) 85, "Byte value", 85), Arguments.of(new BigInteger("2027"), "BigInteger within range", 2027), Arguments.of(new BigDecimal("2028"), "BigDecimal integer value", 2028), Arguments.of(-500, "Negative integer", -500), Arguments.of(0, "Zero year", 0), Arguments.of(" 2029 ", "String with surrounding whitespace", 2029), Arguments.of("+2030", "String with plus sign", 2030), Arguments.of(2023.0f, "Float representing an exact integer", 2023), Arguments.of(mapWithYearAndExtra(1999), "Map with extra unrelated keys", 1999));
    }

    private static Stream<Arguments> provideFailureMapConversions() {
        return Stream.of(// Long out of range
        Arguments.of(3000000000L, DateTimeException.class), // Double non‑integral
        Arguments.of(2022.5, DateTimeException.class), // Float non‑integral (but 2023.0f is integral – handled above as success)
        Arguments.of(2023.0f, DateTimeException.class), // Non‑numeric String
        Arguments.of("twenty", DateTimeException.class), // String with prefix
        Arguments.of("Year2023", DateTimeException.class), // String with suffix
        Arguments.of("2024AD", DateTimeException.class), // Out‑of‑range numeric string
        Arguments.of("1000000", DateTimeException.class), // BigInteger out of range
        Arguments.of(new BigInteger("999999999999"), DateTimeException.class), // BigDecimal non‑integral
        Arguments.of(new BigDecimal("2022.7"), DateTimeException.class), // NaN double
        Arguments.of(Double.NaN, DateTimeException.class), // NaN float
        Arguments.of(Float.NaN, DateTimeException.class), // Boolean
        Arguments.of(true, ClassCastException.class), // Character
        Arguments.of('A', ClassCastException.class), // Date
        Arguments.of(new Date(), ClassCastException.class), // Nested map
        Arguments.of(Collections.singletonMap("value", 2020), ClassCastException.class));
    }

    @Test
    void toYear_fromMapWithNumericYear_returnsYearInstance() {
        // GIVEN a map that contains the key "year" with an integer value
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.YEAR, 2023);
        // WHEN the conversion is performed
        Year result = MapConversions.toYear(source, converter);
        // THEN the result is a Year representing the supplied value
        assertNotNull(result, "Result should not be null");
        assertEquals(Year.of(2023), result, "Converted Year does not match expected value");
    }

    @Test
    void toYear_fromMapWithStringYear_returnsYearInstance() {
        // GIVEN a map that contains the key "year" with a string value that can be parsed as an int
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.YEAR, "1999");
        // WHEN the conversion is performed
        Year result = MapConversions.toYear(source, converter);
        // THEN the result is a Year representing the supplied value
        assertNotNull(result);
        assertEquals(Year.of(1999), result);
    }

    @Test
    void toYear_whenAlreadyYear_returnsSameInstance() {
        // GIVEN a Year object directly
        Year original = Year.of(2005);
        // WHEN the conversion is performed
        Year result = MapConversions.toYear(original, converter);
        // THEN the same instance (or an equal instance) is returned
        assertSame(original, result, "When a Year is supplied, the method should return it unchanged");
    }

    @Test
    void toYear_whenNullInput_returnsNull() {
        // GIVEN a null source
        Object source = null;
        // WHEN the conversion is performed
        Year result = MapConversions.toYear(source, converter);
        // THEN the result should be null
        assertNull(result, "Null input should produce a null result");
    }

    @ParameterizedTest(name = "[{index}] year={0} ({1}) → Year.of({2})")
    @MethodSource("provideSuccessfulMapConversions")
    void toYear_mapSuccessful(Object mapValue, String description, int expectedYear) {
        // GIVEN a map containing the key "year" with the supplied value
        Map<String, Object> source = mapWithYear(mapValue);
        // WHEN conversion is performed
        Year result = MapConversions.toYear(source, defaultConverter);
        // THEN we obtain the expected Year instance
        assertNotNull(result, description);
        assertEquals(Year.of(expectedYear), result, description);
    }

    @Test
    void toYear_mapMissingYearKey_throwsIllegalArgumentException() {
        Map<String, Object> source = new HashMap<>();
        source.put("month", 5);
        source.put("day", 12);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYear(source, defaultConverter), "Missing 'year' key should cause IllegalArgumentException");
    }

    @Test
    void toYear_mapUpperCaseKey_throwsIllegalArgumentException() {
        Map<String, Object> source = new HashMap<>();
        // wrong case
        source.put("YEAR", 2021);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYear(source, defaultConverter), "Upper‑case key should be treated as missing");
    }

    @Test
    void toYear_mapYearKeyNull_returnsNull() {
        Map<String, Object> source = mapWithYear(null);
        assertNull(MapConversions.toYear(source, defaultConverter), "Null value for 'year' key should result in null");
    }

    @ParameterizedTest(name = "[{index}] value={0} → {1}")
    @MethodSource("provideFailureMapConversions")
    void toYear_mapConversionFailure(Object mapValue, Class<? extends Throwable> expectedException) {
        Map<String, Object> source = mapWithYear(mapValue);
        assertThrows(expectedException, () -> MapConversions.toYear(source, defaultConverter), "Conversion of " + mapValue + " should throw " + expectedException.getSimpleName());
    }

    @Test
    void toYear_nonMapWithWorkingStringConverter_returnsYear() {
        // a simple Converter mock that knows how to turn a numeric string into a Year
        Converter mock = mock(Converter.class);
        when(mock.convert(eq("2022"), eq(Year.class))).thenReturn(Year.of(2022));
        Year result = MapConversions.toYear("2022", mock);
        assertEquals(Year.of(2022), result);
        verify(mock).convert("2022", Year.class);
    }

    @Test
    void toYear_nonMapWithNullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toYear("2023", null), "Null converter with non‑map source must NPE");
    }

    @Test
    void toYear_nonMapWithFailingConverter_propagatesConversionException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Year.class))).thenThrow(new ConversionException("cannot convert"));
        assertThrows(ConversionException.class, () -> MapConversions.toYear("bad-year", mock), "ConversionException should propagate");
    }

    @Test
    void toYear_nonMapWithRuntimeFailingConverter_propagatesRuntimeException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Year.class))).thenThrow(new RuntimeException("runtime failure"));
        assertThrows(RuntimeException.class, () -> MapConversions.toYear(Arrays.asList(2022), mock), "RuntimeException should propagate unchanged");
    }

    @Test
    void toYear_nonMapYearMonthWithCustomConverter_returnsYear() {
        Converter mock = mock(Converter.class);
        YearMonth ym = YearMonth.of(2024, 5);
        when(mock.convert(eq(ym), eq(Year.class))).thenReturn(Year.of(2024));
        Year result = MapConversions.toYear(ym, mock);
        assertEquals(Year.of(2024), result);
        verify(mock).convert(ym, Year.class);
    }

    @Test
    void toYear_nonMapLocalDateWithCustomConverter_returnsYear() {
        Converter mock = mock(Converter.class);
        LocalDate ld = LocalDate.of(2025, 8, 15);
        when(mock.convert(eq(ld), eq(Year.class))).thenReturn(Year.of(2025));
        Year result = MapConversions.toYear(ld, mock);
        assertEquals(Year.of(2025), result);
        verify(mock).convert(ld, Year.class);
    }

    @Test
    void toYear_listSourceWithoutConverter_throwsClassCastException() {
        List<Integer> source = Collections.singletonList(2021);
        assertThrows(ClassCastException.class, () -> MapConversions.toYear(source, null), "List source without converter should cause ClassCastException");
    }
}
