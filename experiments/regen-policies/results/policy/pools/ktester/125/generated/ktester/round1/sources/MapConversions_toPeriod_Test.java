package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toPeriod_Test {

    @Mock
    Converter // mocked because we only need the simple numeric conversion
    converter;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the Converter.convert(...) used inside MapConversions.toPeriod(...)
        // It should return an Integer (or int) when the target type is int.class.
        // For the purposes of the Period conversion we only need to handle Number
        // and String inputs that represent integers.
        when(converter.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object source = invocation.getArgument(0);
            if (source instanceof Number) {
                return ((Number) source).intValue();
            }
            if (source instanceof String) {
                try {
                    return Integer.parseInt((String) source);
                } catch (NumberFormatException e) {
                    // fallback – the real Converter would throw, but this is sufficient for the test
                    return 0;
                }
            }
            // default for null or unexpected types
            return 0;
        });
    }

    @Test
    void toPeriod_WithExplicitYearsMonthsDays_ReturnsCorrectPeriod() {
        Map<String, Object> map = new HashMap<>();
        map.put("years", 2);
        map.put("months", 3);
        map.put("days", 5);
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(2, 3, 5), result);
    }

    @Test
    void toPeriod_WithMissingKeys_UsesDefaultsZero() {
        Map<String, Object> emptyMap = new HashMap<>();
        Period result = MapConversions.toPeriod(emptyMap, converter);
        assertEquals(Period.of(0, 0, 0), result);
    }

    @Test
    void toPeriod_WithPartialKeys_UsesZeroForMissing() {
        Map<String, Object> map = new HashMap<>();
        // months and days are omitted
        map.put("years", 7);
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(7, 0, 0), result);
    }

    @Test
    void toPeriod_WithValueKey_DelegatesToFromMap() {
        // When the map contains a "value" or "_v" key, MapConversions delegates to the
        // private fromMap method. We cannot call fromMap directly, but we can verify that
        // the converter is asked to convert the map itself to a Period.
        Map<String, Object> map = new HashMap<>();
        // content is irrelevant for this test
        map.put("_v", "ignored");
        // Stub a generic conversion for the private fromMap path:
        // The private method eventually calls converter.convert(...) for the target type.
        // We simulate that by making the mock return a specific Period when asked for Period.class.
        when(converter.convert(eq(map), eq(Period.class))).thenReturn(Period.of(1, 1, 1));
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(1, 1, 1), result);
        // Verify that the converter was indeed asked to convert the whole map to Period
        verify(converter).convert(eq(map), eq(Period.class));
    }

    @Test
    @DisplayName("All components provided – positive integers")
    void testToPeriod_AllComponentsProvided() {
        Map<String, Object> map = new HashMap<>();
        map.put("years", 2);
        map.put("months", 3);
        map.put("days", 5);
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(2, 3, 5), result);
    }

    @Test
    @DisplayName("Missing components default to zero")
    void testToPeriod_MissingComponentsDefaultToZero() {
        Map<String, Object> emptyMap = new HashMap<>();
        Period result = MapConversions.toPeriod(emptyMap, converter);
        assertEquals(Period.of(0, 0, 0), result);
    }

    @Test
    @DisplayName("Partial keys – months and days omitted")
    void testToPeriod_PartialKeys() {
        Map<String, Object> map = new HashMap<>();
        // months and days are omitted
        map.put("years", 7);
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(7, 0, 0), result);
    }

    @Test
    @DisplayName("Negative values are allowed")
    void testToPeriod_NegativeValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("years", -1);
        map.put("months", -2);
        map.put("days", -3);
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(-1, -2, -3), result);
    }

    @Test
    @DisplayName("String values are converted to int")
    void testToPeriod_StringValuesConvertedToInt() {
        Map<String, Object> map = new HashMap<>();
        map.put("years", "10");
        map.put("months", "4");
        map.put("days", "15");
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(10, 4, 15), result);
    }

    @Test
    @DisplayName("Zero and negative mixed values")
    void testToPeriod_NegativeZeroMixedValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("years", 0);
        map.put("months", -5);
        map.put("days", 0);
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(0, -5, 0), result);
    }

    @Test
    @DisplayName("Maximum int values – no overflow")
    void testToPeriod_OverflowIntValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("years", Integer.MAX_VALUE);
        map.put("months", Integer.MAX_VALUE);
        map.put("days", Integer.MAX_VALUE);
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), result);
    }

    @Test
    @DisplayName("Delegates to fromMap when 'value' key is present")
    void testToPeriod_DelegatesWhenValueKeyPresent() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Period.of(2, 6, 9));
        // Simulate the private fromMap path – the method eventually asks the
        // converter to convert the whole map to a Period.
        when(converter.convert(eq(map), eq(Period.class))).thenReturn(Period.of(2, 6, 9));
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(2, 6, 9), result);
        verify(converter).convert(eq(map), eq(Period.class));
    }

    @Test
    @DisplayName("Delegates to fromMap when '_v' key is present")
    void testToPeriod_DelegatesWhenUnderscoreVKeyPresent() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", Period.of(0, 0, 20));
        // Same stub as above – the private method will request conversion to Period.
        when(converter.convert(eq(map), eq(Period.class))).thenReturn(Period.of(0, 0, 20));
        Period result = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(0, 0, 20), result);
        verify(converter).convert(eq(map), eq(Period.class));
    }

    @Test
    @DisplayName("Null map argument throws NullPointerException")
    void testToPeriod_NullMapThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toPeriod(null, converter), "Expected NPE when 'from' argument is null");
    }

    @Test
    @DisplayName("Null converter argument throws NullPointerException")
    void testToPeriod_NullConverterThrowsNPE() {
        Map<String, Object> map = new HashMap<>();
        map.put("years", 1);
        assertThrows(NullPointerException.class, () -> MapConversions.toPeriod(map, null), "Expected NPE when 'converter' argument is null");
    }

    @Test
    @DisplayName("Non‑Map 'from' argument throws ClassCastException")
    void testToPeriod_NonMapFromThrowsClassCastException() {
        String notAMap = "I am not a map";
        assertThrows(ClassCastException.class, () -> MapConversions.toPeriod(notAMap, converter), "Expected ClassCastException when 'from' is not a Map");
    }

    @Test
    @DisplayName("Invalid number conversion propagates NumberFormatException")
    void testToPeriod_InvalidNumberConversionThrowsException() {
        Map<String, Object> map = new HashMap<>();
        // non‑numeric string
        map.put("years", "abc");
        // Override the default stub for this specific input to throw.
        when(converter.convert(eq("abc"), eq(int.class))).thenThrow(NumberFormatException.class);
        assertThrows(NumberFormatException.class, () -> MapConversions.toPeriod(map, converter), "Expected NumberFormatException for non‑numeric string conversion");
    }
}
