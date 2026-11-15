package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.YearMonth;
import com.cedarsoftware.util.CompactMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearMonthConversions_toMap_Test {

    /**
     * Converter instance used for the tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter is not used by the method under test, but we provide a real instance
        // to satisfy the method signature.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Cleanup static resources if needed.
        converter = null;
    }

    /**
     * Normal conversion scenarios – both with a {@code null} converter and a mocked one.
     */
    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(Arguments.of(YearMonth.of(2023, 5), null, Map.of("year", 2023, "month", 5)), Arguments.of(YearMonth.of(1999, 12), mock(Converter.class), Map.of("year", 1999, "month", 12)));
    }

    /**
     * Extreme year values (very large positive and negative years).
     */
    private static Stream<Arguments> extremeYearProvider() {
        return Stream.of(Arguments.of(YearMonth.of(999_999_999, 12), Map.of("year", 999_999_999, "month", 12)), Arguments.of(YearMonth.of(-999_999_999, 1), Map.of("year", -999_999_999, "month", 1)));
    }

    @Test
    void toMap_ReturnsMapWithCorrectYearAndMonth() {
        // GIVEN
        // July 2023
        YearMonth yearMonth = YearMonth.of(2023, 7);
        // WHEN
        Map<String, Object> result = YearMonthConversions.toMap(yearMonth, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertEquals(2023, result.get("year"), "Year entry should match the YearMonth year");
        assertEquals(7, result.get("month"), "Month entry should match the YearMonth month value");
    }

    @Test
    void toMap_WithNonYearMonthObject_ThrowsClassCastException() {
        // GIVEN
        Object notAYearMonth = "2023-07";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> YearMonthConversions.toMap(notAYearMonth, converter), "Passing a non‑YearMonth object should result in ClassCastException");
    }

    @Test
    void toMap_WithNullFrom_ThrowsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> YearMonthConversions.toMap(nullObj, converter), "Passing null as the source object should result in NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] YearMonth={0}, converter={1}")
    @MethodSource("normalConversionProvider")
    void toMap_NormalConversion_ReturnsCorrectMap(YearMonth input, Converter suppliedConverter, Map<String, Integer> expected) {
        // GIVEN
        // (input and expected are supplied by the method source)
        // WHEN
        Map<String, Object> result = YearMonthConversions.toMap(input, suppliedConverter);
        // THEN
        assertNotNull(result, "Result map must not be null");
        assertEquals(2, result.size(), "Result map must contain exactly two entries");
        assertEquals(expected.get("year"), result.get("year"), "Year entry mismatch");
        assertEquals(expected.get("month"), result.get("month"), "Month entry mismatch");
        // Verify insertion order: year first, month second
        Iterator<String> keyIter = result.keySet().iterator();
        assertEquals("year", keyIter.next(), "First key should be 'year'");
        assertEquals("month", keyIter.next(), "Second key should be 'month'");
        assertFalse(keyIter.hasNext(), "No more keys should be present");
    }

    @Test
    void toMap_NullConverter_Ignored() {
        // GIVEN
        YearMonth ym = YearMonth.of(2000, 1);
        // WHEN
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);
        // THEN
        assertNotNull(result);
        assertEquals(2000, result.get("year"));
        assertEquals(1, result.get("month"));
    }

    @ParameterizedTest(name = "[{index}] Extreme YearMonth={0}")
    @MethodSource("extremeYearProvider")
    void toMap_ExtremeYearValues_ReturnsCorrectMap(YearMonth input, Map<String, Integer> expected) {
        // WHEN
        Map<String, Object> result = YearMonthConversions.toMap(input, null);
        // THEN
        assertEquals(expected.get("year"), result.get("year"));
        assertEquals(expected.get("month"), result.get("month"));
    }

    @Test
    void toMap_InvalidType_ThrowsClassCastException() {
        // GIVEN
        Object notYearMonth = "2023-05";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> YearMonthConversions.toMap(notYearMonth, null), "Casting a non‑YearMonth should throw ClassCastException");
    }

    @Test
    void toMap_DateInstance_ThrowsClassCastException() {
        // GIVEN
        Object date = Date.from(java.time.Instant.now());
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> YearMonthConversions.toMap(date, null), "Casting a Date should throw ClassCastException");
    }

    @Test
    void toMap_NullFrom_ThrowsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> YearMonthConversions.toMap(nullObj, null), "Passing null as the source object should result in NullPointerException");
    }

    @Test
    void toMap_ReturnedMapIsMutable() {
        // GIVEN
        YearMonth ym = YearMonth.of(2022, 8);
        Map<String, Object> map = YearMonthConversions.toMap(ym, null);
        // WHEN
        map.put("day", 15);
        // THEN
        assertEquals(3, map.size(), "Map should contain three entries after mutation");
        assertEquals(15, map.get("day"));
        assertEquals(2022, map.get("year"));
        assertEquals(8, map.get("month"));
    }

    @Test
    void toMap_ConverterSideEffectsIgnored() {
        // GIVEN
        Converter badConverter = mock(Converter.class);
        // If the method ever called convert(...), we want the test to fail.
        doThrow(new AssertionError("Converter should never be invoked")).when(badConverter).convert(any(), any());
        YearMonth ym = YearMonth.of(2021, 3);
        // WHEN / THEN
        assertDoesNotThrow(() -> YearMonthConversions.toMap(ym, badConverter), "Method must not invoke the supplied Converter");
    }

    @Test
    void toMap_ReturnedMapIsCompactMapImplementation() {
        // GIVEN
        YearMonth ym = YearMonth.of(2005, 11);
        // WHEN
        Map<String, Object> result = YearMonthConversions.toMap(ym, null);
        // THEN
        assertTrue(result instanceof CompactMap, "Result should be an instance of CompactMap (or a subclass)");
    }
}
