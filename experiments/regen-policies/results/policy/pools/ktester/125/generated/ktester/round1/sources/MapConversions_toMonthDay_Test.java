package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.MonthDay;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.time.DateTimeException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class MapConversions_toMonthDay_Test {

    private static Converter converter;

    // used for the happy‑path tests
    private static Converter realConverter;

    /* --------------------------------------------------------------
     *  Simple holder class for parameterized test arguments
     * -------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Map<String, Object> map;

        final MonthDay expected;

        final String description;

        ArgumentsHolder(Map<String, Object> map, MonthDay expected, String description) {
            this.map = map;
            this.expected = expected;
            this.description = description;
        }

        // JUnit 5's MethodSource expects a stream of arguments; we expose the fields directly.
        public Map<String, Object> getMap() {
            return map;
        }

        public MonthDay getExpected() {
            return expected;
        }

        public String getDescription() {
            return description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter works with the default options and can handle simple
        // numeric conversions (String → int, Number → int, etc.).
        realConverter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Helper methods for building test data
     * -------------------------------------------------------------- */
    private static Map<String, Object> map(Object month, Object day) {
        Map<String, Object> m = new HashMap<>();
        if (month != null)
            m.put(MapConversions.MONTH, month);
        if (day != null)
            m.put(MapConversions.DAY, day);
        return m;
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the successful conversion scenarios
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> successfulScenarios() {
        return Stream.of(// integer values
        new ArgumentsHolder(map(3, 15), MonthDay.of(3, 15), "Both keys as Integer"), // numeric strings
        new ArgumentsHolder(map("04", "07"), MonthDay.of(4, 7), "Both keys as numeric String"), // Long values
        new ArgumentsHolder(map(9L, 21L), MonthDay.of(9, 21), "Both keys as Long"), // BigDecimal (represented as Double for simplicity – the real converter treats Number)
        new ArgumentsHolder(map(12.0, 31.0), MonthDay.of(12, 31), "Both keys as Double/BigDecimal"), // extra unrelated keys
        new ArgumentsHolder(mapWithExtra(11, 23), MonthDay.of(11, 23), "Extra keys ignored"));
    }

    private static Map<String, Object> mapWithExtra(Object month, Object day) {
        Map<String, Object> m = map(month, day);
        m.put("year", 2023);
        m.put("extra", "ignored");
        return m;
    }

    @Test
    void toMonthDay_withIntegerValues_returnsCorrectMonthDay() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // May
        map.put(MONTH, 5);
        // 10th
        map.put(DAY, 10);
        // WHEN
        MonthDay result = MapConversions.toMonthDay(map, converter);
        // THEN
        assertEquals(MonthDay.of(5, 10), result);
    }

    @Test
    void toMonthDay_withStringValues_convertsAndReturnsMonthDay() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // December (as String)
        map.put(MONTH, "12");
        // 31st (as String)
        map.put(DAY, "31");
        // WHEN
        MonthDay result = MapConversions.toMonthDay(map, converter);
        // THEN
        assertEquals(MonthDay.of(12, 31), result);
    }

    @Test
    void toMonthDay_missingMonthOrDay_returnsNull() {
        // GIVEN – only day is present
        Map<String, Object> map = new HashMap<>();
        map.put(DAY, 15);
        // WHEN
        MonthDay result = MapConversions.toMonthDay(map, converter);
        // THEN – the private fallback cannot construct a MonthDay, so we expect null
        assertNull(result);
    }

    @Test
    void toMonthDay_nullMap_throwsNullPointerException() {
        // GIVEN
        Map<String, Object> map = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toMonthDay(map, converter));
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulScenarios")
    void toMonthDay_successful(ArgumentsHolder holder) {
        // WHEN
        MonthDay result = MapConversions.toMonthDay(holder.map, realConverter);
        // THEN
        assertEquals(holder.expected, result);
    }

    /* --------------------------------------------------------------
     *  Tests that exercise the fallback path (fromMap) and its failures
     * -------------------------------------------------------------- */
    @Test
    void toMonthDay_missingDay_triggersFallbackException() {
        // only month present
        Map<String, Object> map = map(7, null);
        // The private fromMap throws IllegalArgumentException when required keys are missing.
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toMonthDay(map, realConverter));
    }

    @Test
    void toMonthDay_missingMonth_triggersFallbackException() {
        // only day present
        Map<String, Object> map = map(null, 22);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toMonthDay(map, realConverter));
    }

    @Test
    void toMonthDay_emptyMap_triggersFallbackException() {
        Map<String, Object> empty = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toMonthDay(empty, realConverter));
    }

    @Test
    void toMonthDay_nullMonthValue_triggersFallbackException() {
        Map<String, Object> map = map(null, 12);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toMonthDay(map, realConverter));
    }

    @Test
    void toMonthDay_nullDayValue_triggersFallbackException() {
        Map<String, Object> map = map(8, null);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toMonthDay(map, realConverter));
    }

    /* --------------------------------------------------------------
     *  Tests for invalid numeric values that cause DateTimeException
     * -------------------------------------------------------------- */
    @Test
    void toMonthDay_monthOutOfRange_throwsDateTimeException() {
        Map<String, Object> map = map(13, 5);
        assertThrows(DateTimeException.class, () -> MapConversions.toMonthDay(map, realConverter));
    }

    @Test
    void toMonthDay_invalidDayForMonth_throwsDateTimeException() {
        // Feb 30
        Map<String, Object> map = map(2, 30);
        assertThrows(DateTimeException.class, () -> MapConversions.toMonthDay(map, realConverter));
    }

    /* --------------------------------------------------------------
     *  Tests that verify exception propagation from a mocked Converter
     * -------------------------------------------------------------- */
    @Test
    void toMonthDay_converterThrowsNumberFormatException() {
        // Arrange a map with a non‑numeric month string
        Map<String, Object> map = map("April", 5);
        Converter mock = mock(Converter.class);
        when(mock.convert("April", int.class)).thenThrow(new NumberFormatException("Invalid number"));
        // The day conversion is never reached, but we stub it anyway
        when(mock.convert(5, int.class)).thenReturn(5);
        // Act & Assert
        assertThrows(NumberFormatException.class, () -> MapConversions.toMonthDay(map, mock));
    }

    @Test
    void toMonthDay_converterReturnsNullForMonth_causesNPE() {
        Map<String, Object> map = map(5, 10);
        Converter mock = mock(Converter.class);
        // null for month
        when(mock.convert(5, int.class)).thenReturn(null);
        when(mock.convert(10, int.class)).thenReturn(10);
        assertThrows(NullPointerException.class, () -> MapConversions.toMonthDay(map, mock));
    }

    @Test
    void toMonthDay_converterReturnsNullForDay_causesNPE() {
        Map<String, Object> map = map(5, 10);
        Converter mock = mock(Converter.class);
        when(mock.convert(5, int.class)).thenReturn(5);
        // null for day
        when(mock.convert(10, int.class)).thenReturn(null);
        assertThrows(NullPointerException.class, () -> MapConversions.toMonthDay(map, mock));
    }

    @Test
    void toMonthDay_converterIsNull_throwsNPE() {
        Map<String, Object> map = map(5, 12);
        assertThrows(NullPointerException.class, () -> MapConversions.toMonthDay(map, null));
    }

    @Test
    void toMonthDay_inputNotMap_throwsClassCastException() {
        String notAMap = "just a string";
        assertThrows(ClassCastException.class, () -> MapConversions.toMonthDay(notAMap, realConverter));
    }

    @Test
    void toMonthDay_nullSourceMap_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toMonthDay(null, realConverter));
    }

    @Test
    void toMonthDay_booleanValues_causeConverterClassCastException() {
        Map<String, Object> map = map(true, false);
        Converter mock = mock(Converter.class);
        // Simulate a converter that casts to Number – this will throw ClassCastException
        when(mock.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            // will fail for Boolean
            return ((Number) src).intValue();
        });
        assertThrows(ClassCastException.class, () -> MapConversions.toMonthDay(map, mock));
    }
}
