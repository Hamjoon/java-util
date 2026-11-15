package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;
import java.time.DateTimeException;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLocalDate_Test {

    /**
     * Converter used in the “normal” (non‑mock) test.
     */
    private static Converter realConverter;

    /**
     * Mocked Converter used when we want to verify the conversion calls.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Map that will be supplied to the method under test.
     */
    private Map<String, Object> map;

    @BeforeAll
    static void setupBeforeAll() {
        // a real Converter with default options – no special configuration needed
        realConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Mockito will reset the mock automatically between tests because of the
        // MockitoExtension, but we clear the map for good measure.
        map.clear();
    }

    /**
     * When the map contains the keys {@code year}, {@code month} and {@code day},
     * {@code toLocalDate} should read those values, convert them to {@code int}
     * via the supplied {@link Converter}, and build a {@link LocalDate}.
     */
    @Test
    void toLocalDate_usesYearMonthDayKeys() {
        // arrange
        map.put(MapConversions.YEAR, 2022);
        map.put(MapConversions.MONTH, 3);
        map.put(MapConversions.DAY, 15);
        // mock the converter to return the primitive int values we expect
        when(mockConverter.convert(2022, int.class)).thenReturn(2022);
        when(mockConverter.convert(3, int.class)).thenReturn(3);
        when(mockConverter.convert(15, int.class)).thenReturn(15);
        // act
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // assert
        assertEquals(LocalDate.of(2022, 3, 15), result, "The LocalDate should be built from the supplied year/month/day values.");
        // verify that the converter was asked to convert each component exactly once
        verify(mockConverter, times(1)).convert(2022, int.class);
        verify(mockConverter, times(1)).convert(3, int.class);
        verify(mockConverter, times(1)).convert(15, int.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * When the map does not contain the three date parts, the method should fall back
     * to {@code fromMap}.  Supplying a map with a {@code "date"} entry that already
     * holds a {@link LocalDate} exercises that fallback path.
     */
    @Test
    void toLocalDate_fallsBackToFromMapWhenYearMonthDayMissing() {
        // arrange: only the "date" key is present
        LocalDate expected = LocalDate.of(1999, 1, 1);
        map.put(MapConversions.DATE, expected);
        // act
        LocalDate result = MapConversions.toLocalDate(map, realConverter);
        // assert
        assertEquals(expected, result, "When year/month/day keys are missing, the method should return the value associated with the \"date\" key.");
    }

    @Test
    @DisplayName("Uses YEAR, MONTH, DAY keys – String values converted to int")
    void toLocalDate_stringValuesConverted() {
        // arrange
        map.put(MapConversions.YEAR, "2022");
        map.put(MapConversions.MONTH, "12");
        map.put(MapConversions.DAY, "31");
        when(mockConverter.convert("2022", int.class)).thenReturn(2022);
        when(mockConverter.convert("12", int.class)).thenReturn(12);
        when(mockConverter.convert("31", int.class)).thenReturn(31);
        // act
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // assert
        assertEquals(LocalDate.of(2022, 12, 31), result);
        verify(mockConverter).convert("2022", int.class);
        verify(mockConverter).convert("12", int.class);
        verify(mockConverter).convert("31", int.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    @DisplayName("Invalid date components cause DateTimeException")
    void toLocalDate_invalidDate_throwsDateTimeException() {
        // arrange
        map.put(MapConversions.YEAR, 2022);
        map.put(MapConversions.MONTH, 2);
        map.put(MapConversions.DAY, 30);
        when(mockConverter.convert(2022, int.class)).thenReturn(2022);
        when(mockConverter.convert(2, int.class)).thenReturn(2);
        when(mockConverter.convert(30, int.class)).thenReturn(30);
        // act & assert
        assertThrows(DateTimeException.class, () -> MapConversions.toLocalDate(map, mockConverter), "LocalDate.of(2022,2,30) should throw DateTimeException");
    }

    @Test
    @DisplayName("Missing MONTH – fallback to DATE entry, converter not used")
    void toLocalDate_missingComponent_fallbackToDateKey() {
        // arrange: YEAR and DAY present, MONTH missing; also provide a DATE entry.
        map.put(MapConversions.YEAR, 2022);
        map.put(MapConversions.DAY, 15);
        map.put(MapConversions.DATE, LocalDate.of(2000, 1, 1));
        // act
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // assert
        assertEquals(LocalDate.of(2000, 1, 1), result);
        // Converter must not be called because the method should have taken the fallback.
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("All components null – fallback to DATE entry, converter not used")
    void toLocalDate_allNullComponents_fallbackToDateKey() {
        // arrange: keys exist but values are null; also provide a DATE entry.
        map.put(MapConversions.YEAR, null);
        map.put(MapConversions.MONTH, null);
        map.put(MapConversions.DAY, null);
        map.put(MapConversions.DATE, LocalDate.of(1999, 12, 31));
        // act
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // assert
        assertEquals(LocalDate.of(1999, 12, 31), result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("Null `from` argument throws NullPointerException")
    void toLocalDate_nullFrom_throwsNullPointerException() {
        // act & assert
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalDate(null, realConverter));
    }

    @Test
    @DisplayName("Non‑Map `from` argument throws ClassCastException")
    void toLocalDate_nonMapFrom_throwsClassCastException() {
        // act & assert
        assertThrows(ClassCastException.class, () -> MapConversions.toLocalDate("just a string", realConverter));
    }

    @Test
    @DisplayName("Converter throws NumberFormatException for non‑numeric string")
    void toLocalDate_converterThrowsNumberFormatException() {
        // arrange
        // invalid numeric string
        map.put(MapConversions.YEAR, "abc");
        map.put(MapConversions.MONTH, 5);
        map.put(MapConversions.DAY, 10);
        when(mockConverter.convert("abc", int.class)).thenThrow(new NumberFormatException("For input string: \"abc\""));
        // month and day conversions are not reached, but we can stub them anyway
        when(mockConverter.convert(5, int.class)).thenReturn(5);
        when(mockConverter.convert(10, int.class)).thenReturn(10);
        // act & assert
        assertThrows(NumberFormatException.class, () -> MapConversions.toLocalDate(map, mockConverter));
    }

    @Test
    @DisplayName("Fallback fromMap throws IllegalArgumentException when DATE value has wrong type")
    void toLocalDate_fallbackFromMapThrowsIllegalArgumentException() {
        // arrange: missing MONTH triggers fallback; DATE key is present but with wrong type.
        map.put(MapConversions.YEAR, 2022);
        map.put(MapConversions.DAY, 15);
        // invalid type
        map.put(MapConversions.DATE, "not a LocalDate");
        // act & assert
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalDate(map, realConverter), "fromMap should reject a DATE entry that is not a LocalDate");
    }

    @Test
    @DisplayName("Real Converter parses String values to int correctly")
    void toLocalDate_realConverterStringParsing() {
        // arrange
        map.put(MapConversions.YEAR, "2021");
        map.put(MapConversions.MONTH, "7");
        map.put(MapConversions.DAY, "4");
        // act
        LocalDate result = MapConversions.toLocalDate(map, realConverter);
        // assert
        assertEquals(LocalDate.of(2021, 7, 4), result);
    }
}
