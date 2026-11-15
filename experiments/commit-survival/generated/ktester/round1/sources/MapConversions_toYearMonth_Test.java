package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;

@ExtendWith(MockitoExtension.class)
class MapConversions_toYearMonth_Test {

    @Mock
    private Converter // injected by MockitoExtension
    mockConverter;

    /**
     * Reset the mock before each test to avoid cross‑test interference.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates a fresh mock for each test method because of the @Mock field,
        // but we also clear any previous interactions for safety.
        clearInvocations(mockConverter);
    }

    private static Converter realConverter() {
        return new Converter(new ConverterOptions());
    }

    /**
     * Verifies that when both {@code year} and {@code month} keys are present,
     * the method returns the expected {@link YearMonth}.
     */
    @Test
    void toYearMonth_WithYearAndMonth_ReturnsCorrectYearMonth() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2023);
        source.put("month", 5);
        // The converter is expected to be asked to convert the raw map values
        // to {@code int}. We stub those calls to return the same numeric values.
        when(mockConverter.convert(2023, int.class)).thenReturn(2023);
        when(mockConverter.convert(5, int.class)).thenReturn(5);
        // WHEN
        YearMonth result = MapConversions.toYearMonth(source, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(YearMonth.of(2023, 5), result, "YearMonth should match the supplied year and month");
        // Verify that the converter was invoked exactly twice – once for each key.
        verify(mockConverter, times(1)).convert(2023, int.class);
        verify(mockConverter, times(1)).convert(5, int.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Verifies that when the map does not contain both required keys,
     * the method falls back to the private {@code fromMap} logic.
     * The private method ultimately attempts to construct a {@code YearMonth}
     * from alternative key sets (e.g., a single {@code value} entry).  We simulate
     * this by providing a map with a {@code value} key holding a {@link YearMonth}
     * instance and letting the real {@link Converter} perform the conversion.
     */
    @Test
    void toYearMonth_MissingYearOrMonth_UsesFromMapFallback() {
        // GIVEN: a map that only contains a "value" entry holding a YearMonth.
        YearMonth expected = YearMonth.of(1999, 12);
        Map<String, Object> source = new HashMap<>();
        source.put("value", expected);
        // Use a real Converter (no mocking) so that the fallback path can exercise
        // the actual conversion logic.  The default options are sufficient for this test.
        Converter realConverter = new Converter(new ConverterOptions());
        // WHEN
        YearMonth result = MapConversions.toYearMonth(source, realConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(expected, result, "Result should be the YearMonth supplied via the 'value' key");
    }

    @Test
    void toYearMonth_StringValues_ParsedByConverter() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", "1999");
        source.put("month", "12");
        when(mockConverter.convert("1999", int.class)).thenReturn(1999);
        when(mockConverter.convert("12", int.class)).thenReturn(12);
        YearMonth result = MapConversions.toYearMonth(source, mockConverter);
        assertEquals(YearMonth.of(1999, 12), result);
    }

    @Test
    void toYearMonth_LongValues_NarrowedByConverter() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2020L);
        source.put("month", 2L);
        when(mockConverter.convert(2020L, int.class)).thenReturn(2020);
        when(mockConverter.convert(2L, int.class)).thenReturn(2);
        YearMonth result = MapConversions.toYearMonth(source, mockConverter);
        assertEquals(YearMonth.of(2020, 2), result);
    }

    @Test
    void toYearMonth_BigDecimalValues_ConvertedToInt() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", new BigDecimal("2021"));
        source.put("month", new BigDecimal("11"));
        when(mockConverter.convert(new BigDecimal("2021"), int.class)).thenReturn(2021);
        when(mockConverter.convert(new BigDecimal("11"), int.class)).thenReturn(11);
        YearMonth result = MapConversions.toYearMonth(source, mockConverter);
        assertEquals(YearMonth.of(2021, 11), result);
    }

    @Test
    void toYearMonth_BigIntegerValues_ConvertedToInt() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", new BigInteger("1995"));
        source.put("month", new BigInteger("6"));
        when(mockConverter.convert(new BigInteger("1995"), int.class)).thenReturn(1995);
        when(mockConverter.convert(new BigInteger("6"), int.class)).thenReturn(6);
        YearMonth result = MapConversions.toYearMonth(source, mockConverter);
        assertEquals(YearMonth.of(1995, 6), result);
    }

    @Test
    void toYearMonth_ExtraIrrelevantEntries_Ignored() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2020);
        source.put("month", 9);
        source.put("day", 15);
        source.put("extra", "ignore");
        when(mockConverter.convert(2020, int.class)).thenReturn(2020);
        when(mockConverter.convert(9, int.class)).thenReturn(9);
        YearMonth result = MapConversions.toYearMonth(source, mockConverter);
        assertEquals(YearMonth.of(2020, 9), result);
    }

    @Test
    void toYearMonth_NegativeYearSupported() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", -44);
        source.put("month", 3);
        when(mockConverter.convert(-44, int.class)).thenReturn(-44);
        when(mockConverter.convert(3, int.class)).thenReturn(3);
        YearMonth result = MapConversions.toYearMonth(source, mockConverter);
        assertEquals(YearMonth.of(-44, 3), result);
    }

    @Test
    void toYearMonth_InvalidMonthZero_ThrowsDateTimeException() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2022);
        source.put("month", 0);
        when(mockConverter.convert(2022, int.class)).thenReturn(2022);
        when(mockConverter.convert(0, int.class)).thenReturn(0);
        assertThrows(DateTimeException.class, () -> MapConversions.toYearMonth(source, mockConverter));
    }

    @Test
    void toYearMonth_InvalidMonthThirteen_ThrowsDateTimeException() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2022);
        source.put("month", 13);
        when(mockConverter.convert(2022, int.class)).thenReturn(2022);
        when(mockConverter.convert(13, int.class)).thenReturn(13);
        assertThrows(DateTimeException.class, () -> MapConversions.toYearMonth(source, mockConverter));
    }

    @Test
    void toYearMonth_ConverterThrowsNumberFormatException_Propagated() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", "bad");
        source.put("month", 3);
        when(mockConverter.convert("bad", int.class)).thenThrow(new NumberFormatException("For input string: \"bad\""));
        assertThrows(NumberFormatException.class, () -> MapConversions.toYearMonth(source, mockConverter));
    }

    @Test
    void toYearMonth_ConverterThrowsNumberFormatExceptionForMonth_Propagated() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2022);
        source.put("month", "NaN");
        when(mockConverter.convert(2022, int.class)).thenReturn(2022);
        when(mockConverter.convert("NaN", int.class)).thenThrow(new NumberFormatException("For input string: \"NaN\""));
        assertThrows(NumberFormatException.class, () -> MapConversions.toYearMonth(source, mockConverter));
    }

    @Test
    void toYearMonth_ConverterThrowsIllegalArgumentException_Propagated() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2022);
        source.put("month", 4);
        when(mockConverter.convert(any(), eq(int.class))).thenThrow(new IllegalArgumentException("Conversion not supported"));
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(source, mockConverter));
    }

    @Test
    void toYearMonth_ConverterThrowsClassCastException_Propagated() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2020);
        source.put("month", new Object());
        when(mockConverter.convert(2020, int.class)).thenReturn(2020);
        when(mockConverter.convert(any(Object.class), eq(int.class))).thenThrow(new ClassCastException("Cannot cast month to int"));
        assertThrows(ClassCastException.class, () -> MapConversions.toYearMonth(source, mockConverter));
    }

    @Test
    void toYearMonth_NullConverter_DirectPath_NullPointerException() {
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2023);
        source.put("month", 5);
        assertThrows(NullPointerException.class, () -> MapConversions.toYearMonth(source, null));
    }

    @Test
    void toYearMonth_NullConverter_FallbackPath_NullPointerException() {
        Map<String, Object> source = new HashMap<>();
        // missing month triggers fallback
        source.put("year", 2021);
        assertThrows(NullPointerException.class, () -> MapConversions.toYearMonth(source, null));
    }

    @Test
    void toYearMonth_NullFrom_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toYearMonth(null, mockConverter));
    }

    @Test
    void toYearMonth_NonMapInput_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> MapConversions.toYearMonth("just a string", mockConverter));
    }

    @Test
    void toYearMonth_MapWithNonStringKeys_ThrowsClassCastException() {
        Map<Integer, Object> source = new HashMap<>();
        source.put(1, 2022);
        source.put(2, 5);
        // The raw map is passed as Object; the internal cast to Map<String,Object>
        // will fail at runtime.
        assertThrows(ClassCastException.class, () -> MapConversions.toYearMonth(source, mockConverter));
    }

    @Test
    void toYearMonth_MissingYear_UsesFromMapFallback() {
        YearMonth expected = YearMonth.of(2025, 5);
        Map<String, Object> source = new HashMap<>();
        source.put("value", expected);
        // month missing
        source.put("year", 2021);
        YearMonth result = MapConversions.toYearMonth(source, realConverter());
        assertEquals(expected, result);
    }

    @Test
    void toYearMonth_MissingMonth_UsesFromMapFallback() {
        YearMonth expected = YearMonth.of(1990, 8);
        Map<String, Object> source = new HashMap<>();
        source.put("value", expected);
        // year missing
        source.put("month", 8);
        YearMonth result = MapConversions.toYearMonth(source, realConverter());
        assertEquals(expected, result);
    }

    @Test
    void toYearMonth_EmptyMap_UsesFromMapFallback() {
        YearMonth expected = YearMonth.of(2000, 1);
        Map<String, Object> source = new HashMap<>();
        source.put("value", expected);
        YearMonth result = MapConversions.toYearMonth(source, realConverter());
        assertEquals(expected, result);
    }

    @Test
    void toYearMonth_NullYearAndMonth_UsesFromMapFallback() {
        YearMonth expected = YearMonth.of(2005, 7);
        Map<String, Object> source = new HashMap<>();
        source.put("value", expected);
        source.put("year", null);
        source.put("month", null);
        YearMonth result = MapConversions.toYearMonth(source, realConverter());
        assertEquals(expected, result);
    }

    @Test
    void toYearMonth_IncorrectKeyCase_FallbackToFromMap() {
        YearMonth expected = YearMonth.of(2015, 8);
        Map<String, Object> source = new HashMap<>();
        source.put("Year", 2023);
        source.put("Month", 4);
        // fallback will pick this
        source.put("value", expected);
        YearMonth result = MapConversions.toYearMonth(source, realConverter());
        assertEquals(expected, result);
    }

    @Test
    void toYearMonth_OnlyYearPresent_WithMockConverter_NoInteraction() {
        // The mock should not be used because fallback occurs.
        Map<String, Object> source = new HashMap<>();
        source.put("year", 2021);
        YearMonth expected = YearMonth.of(2025, 5);
        source.put("value", expected);
        YearMonth result = MapConversions.toYearMonth(source, mockConverter);
        assertEquals(expected, result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toYearMonth_OnlyMonthPresent_WithMockConverter_NoInteraction() {
        Map<String, Object> source = new HashMap<>();
        source.put("month", 8);
        YearMonth expected = YearMonth.of(1990, 8);
        source.put("value", expected);
        YearMonth result = MapConversions.toYearMonth(source, mockConverter);
        assertEquals(expected, result);
        verifyNoInteractions(mockConverter);
    }
}
