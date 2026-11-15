package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.time.DateTimeException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toYearMonth_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // By default we use a real Converter – individual tests replace it with a mock when needed.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * A mock Converter that returns the same integer it receives, parsing strings when needed.
     */
    private static Converter mockIdentityConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg == null) {
                return null;
            }
            if (arg instanceof Number) {
                return ((Number) arg).intValue();
            }
            if (arg instanceof String) {
                return Integer.parseInt((String) arg);
            }
            // For any other type we simply return null – the test will fail if this path is taken.
            return null;
        });
        return mock;
    }

    /**
     * A mock Converter that always throws a RuntimeException when {@code convert} is called.
     */
    private static Converter mockExceptionThrowingConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), any())).thenThrow(new RuntimeException("Converter failure"));
        return mock;
    }

    /**
     * A mock Converter that returns {@code null} for any conversion request.
     */
    private static Converter mockNullReturningConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), any())).thenReturn(null);
        return mock;
    }

    /**
     * When the map contains both {@code "year"} and {@code "month"} entries,
     * {@code toYearMonth} should return a {@link YearMonth} built from those values.
     */
    @Test
    void toYearMonth_WithYearAndMonth_ReturnsCorrectYearMonth() {
        // given
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.YEAR, 2023);
        source.put(MapConversions.MONTH, 5);
        // when
        YearMonth result = MapConversions.toYearMonth(source, converter);
        // then
        assertNotNull(result, "Result should not be null when both keys are present");
        assertEquals(YearMonth.of(2023, 5), result, "YearMonth should be constructed from the supplied year and month");
    }

    /**
     * When the map is missing either the {@code "year"} or {@code "month"} key,
     * the method falls back to {@code fromMap}. For {@code YearMonth} the fallback
     * yields {@code null} because the required key set cannot be satisfied.
     */
    @Test
    void toYearMonth_MissingMonth_ReturnsNull() {
        // given – only the year is supplied
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.YEAR, 2023);
        // month key is deliberately omitted
        // when
        YearMonth result = MapConversions.toYearMonth(source, converter);
        // then
        assertNull(result, "Result should be null when required month key is missing");
    }

    /**
     * When the map is missing the {@code "year"} key, the result should also be {@code null}.
     */
    @Test
    void toYearMonth_MissingYear_ReturnsNull() {
        // given – only the month is supplied
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.MONTH, 7);
        // year key is deliberately omitted
        // when
        YearMonth result = MapConversions.toYearMonth(source, converter);
        // then
        assertNull(result, "Result should be null when required year key is missing");
    }

    /**
     * String values for year and month are converted to ints by the converter.
     */
    @Test
    void toYearMonth_StringValuesConvertedToInt_ReturnsCorrectYearMonth() {
        // given
        Map<String, Object> source = Map.of(MapConversions.YEAR, "1999", MapConversions.MONTH, "12");
        Converter mock = mockIdentityConverter();
        // when
        YearMonth result = MapConversions.toYearMonth(source, mock);
        // then
        assertEquals(YearMonth.of(1999, 12), result, "String values should be parsed to ints and produce the correct YearMonth");
    }

    /**
     * The method should ignore unrelated entries in the map.
     */
    @Test
    void toYearMonth_MapWithExtraEntries_IgnoresIrrelevantData() {
        // given
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.YEAR, 2024);
        source.put(MapConversions.MONTH, 3);
        source.put("day", 15);
        source.put("extra", "value");
        Converter mock = mockIdentityConverter();
        // when
        YearMonth result = MapConversions.toYearMonth(source, mock);
        // then
        assertEquals(YearMonth.of(2024, 3), result, "Only YEAR and MONTH should be considered even when extra keys exist");
    }

    @Test
    void toYearMonth_MissingYear_ThrowsIllegalArgumentException() {
        Map<String, Object> source = Map.of(MapConversions.MONTH, 7);
        Converter mock = mockIdentityConverter();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(source, mock), "Missing YEAR should cause fallback to fromMap which throws IllegalArgumentException");
    }

    @Test
    void toYearMonth_MissingMonth_ThrowsIllegalArgumentException() {
        Map<String, Object> source = Map.of(MapConversions.YEAR, 2022);
        Converter mock = mockIdentityConverter();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(source, mock), "Missing MONTH should cause fallback to fromMap which throws IllegalArgumentException");
    }

    @Test
    void toYearMonth_BothKeysNull_ThrowsIllegalArgumentException() {
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.YEAR, null);
        source.put(MapConversions.MONTH, null);
        Converter mock = mockIdentityConverter();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(source, mock), "Both values null should trigger fallback and result in IllegalArgumentException");
    }

    @Test
    void toYearMonth_EmptyMap_ThrowsIllegalArgumentException() {
        Map<String, Object> source = new HashMap<>();
        Converter mock = mockIdentityConverter();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(source, mock), "Empty map should cause fallback to fromMap which throws IllegalArgumentException");
    }

    @Test
    void toYearMonth_YearNull_ThrowsIllegalArgumentException() {
        Map<String, Object> source = Map.of(MapConversions.YEAR, null, MapConversions.MONTH, 8);
        Converter mock = mockIdentityConverter();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(source, mock), "Null YEAR value should lead to fallback and IllegalArgumentException");
    }

    @Test
    void toYearMonth_MonthNull_ThrowsIllegalArgumentException() {
        Map<String, Object> source = Map.of(MapConversions.YEAR, 2021, MapConversions.MONTH, null);
        Converter mock = mockIdentityConverter();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toYearMonth(source, mock), "Null MONTH value should lead to fallback and IllegalArgumentException");
    }

    @Test
    void toYearMonth_InvalidMonthRange_PropagatesDateTimeException() {
        Map<String, Object> source = Map.of(MapConversions.YEAR, 2021, // invalid month
        MapConversions.MONTH, // invalid month
        13);
        Converter mock = mockIdentityConverter();
        assertThrows(DateTimeException.class, () -> MapConversions.toYearMonth(source, mock), "Month value 13 should cause YearMonth.of to throw DateTimeException");
    }

    @Test
    void toYearMonth_ConverterThrowsRuntimeException_PropagatesException() {
        Map<String, Object> source = Map.of(MapConversions.YEAR, 2020, MapConversions.MONTH, 6);
        Converter mock = mockExceptionThrowingConverter();
        assertThrows(RuntimeException.class, () -> MapConversions.toYearMonth(source, mock), "Converter exception should bubble up unchanged");
    }

    @Test
    void toYearMonth_NonMapInput_ThrowsClassCastException() {
        String notAMap = "I am not a map";
        // any converter – it will never be used because the cast fails first
        Converter anyConverter = mock(Converter.class);
        assertThrows(ClassCastException.class, () -> MapConversions.toYearMonth(notAMap, anyConverter), "Passing a non‑Map should trigger a ClassCastException");
    }

    @Test
    void toYearMonth_NullFromArgument_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toYearMonth(null, converter), "Null 'from' argument should cause a NullPointerException when the method tries to dereference it");
    }

    @Test
    void toYearMonth_NullConverter_ThrowsNullPointerException() {
        Map<String, Object> source = Map.of(MapConversions.YEAR, 2022, MapConversions.MONTH, 4);
        assertThrows(NullPointerException.class, () -> MapConversions.toYearMonth(source, null), "Null converter should cause a NullPointerException when its convert method is invoked");
    }

    @Test
    void toYearMonth_ConverterReturnsNull_ThrowsNullPointerException() {
        Map<String, Object> source = Map.of(MapConversions.YEAR, 2020, MapConversions.MONTH, 9);
        Converter mock = mockNullReturningConverter();
        assertThrows(NullPointerException.class, () -> MapConversions.toYearMonth(source, mock), "If the converter returns null, unboxing to primitive int triggers NullPointerException");
    }
}
