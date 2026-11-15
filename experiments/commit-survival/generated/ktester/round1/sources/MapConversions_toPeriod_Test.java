package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toPeriod_Test {

    /**
     * The mock Converter that will be supplied to {@code MapConversions.toPeriod}.
     */
    @Mock
    Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock to behave like a simple pass‑through for numeric conversions.
        // The real Converter would convert the supplied value to an int; we mimic that.
        when(converter.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object source = invocation.getArgument(0);
            if (source instanceof Number) {
                return ((Number) source).intValue();
            }
            // When the key is missing the map supplies a default of 0 (see toPeriod source).
            return 0;
        });
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions so each test starts with a clean slate.
        reset(converter);
    }

    /**
     * When the source map contains a {@code "value"} (or {@code "_v"}) entry,
     * {@code toPeriod} should return that value unchanged.
     */
    @Test
    void toPeriod_usesValueKeyIfPresent() {
        // Arrange
        Period expected = Period.of(1, 2, 3);
        Map<String, Object> map = new HashMap<>();
        // could also use MapConversions.V
        map.put(MapConversions.VALUE, expected);
        // Act
        Period actual = MapConversions.toPeriod(map, converter);
        // Assert
        assertEquals(expected, actual, "When a map contains the '" + MapConversions.VALUE + "' key, the period should be returned unchanged.");
    }

    /**
     * When the source map does not contain a {@code "value"} key, the method should build a
     * {@code Period} from the {@code years}, {@code months} and {@code days} entries,
     * using the supplied {@code Converter} to obtain integer values.
     */
    @Test
    void toPeriod_buildsFromYearsMonthsDays() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, 5);
        map.put(MapConversions.MONTHS, 6);
        map.put(MapConversions.DAYS, 7);
        // Act
        Period result = MapConversions.toPeriod(map, converter);
        // Assert
        Period expected = Period.of(5, 6, 7);
        assertEquals(expected, result, "Period should be constructed from the map's years, months and days entries.");
        // Verify that the converter was asked to convert each entry.
        verify(converter, times(1)).convert(eq(5), eq(int.class));
        verify(converter, times(1)).convert(eq(6), eq(int.class));
        verify(converter, times(1)).convert(eq(7), eq(int.class));
    }

    /**
     * When the map does not contain any of the expected keys, the method should fall back
     * to the defaults (0 for each component) and return {@code Period.of(0,0,0)}.
     */
    @Test
    void toPeriod_defaultsWhenKeysMissing() {
        // Arrange
        Map<String, Object> emptyMap = new HashMap<>();
        // Act
        Period result = MapConversions.toPeriod(emptyMap, converter);
        // Assert
        Period expected = Period.of(0, 0, 0);
        assertEquals(expected, result, "An empty map should result in a zero-length Period.");
        // The converter should have been called three times with the default values (0).
        verify(converter, times(3)).convert(eq(0), eq(int.class));
    }

    @Test
    void toPeriod_usesVKeyIfPresent() {
        // Arrange
        Period expected = Period.of(4, 5, 6);
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.V, expected);
        // Act
        Period actual = MapConversions.toPeriod(map, converter);
        // Assert
        assertEquals(expected, actual, "When a map contains the '" + MapConversions.V + "' key, the period should be returned unchanged.");
    }

    @Test
    void toPeriod_allKeysPresent() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, 2);
        map.put(MapConversions.MONTHS, 3);
        map.put(MapConversions.DAYS, 4);
        // Act
        Period result = MapConversions.toPeriod(map, converter);
        // Assert
        assertEquals(Period.of(2, 3, 4), result);
    }

    @Test
    void toPeriod_missingKeysPartial_yearOnly() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, 7);
        // Act
        Period result = MapConversions.toPeriod(map, converter);
        // Assert
        assertEquals(Period.of(7, 0, 0), result);
    }

    @Test
    void toPeriod_missingKeysPartial_monthAndDay() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.MONTHS, 8);
        map.put(MapConversions.DAYS, 9);
        // Act
        Period result = MapConversions.toPeriod(map, converter);
        // Assert
        assertEquals(Period.of(0, 8, 9), result);
    }

    @Test
    void toPeriod_negativeValues() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, -1);
        map.put(MapConversions.MONTHS, -2);
        map.put(MapConversions.DAYS, -3);
        // Act
        Period result = MapConversions.toPeriod(map, converter);
        // Assert
        assertEquals(Period.of(-1, -2, -3), result);
    }

    @Test
    void toPeriod_nonIntegerValues() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        // String
        map.put(MapConversions.YEARS, "5");
        // Long
        map.put(MapConversions.MONTHS, 6L);
        // Double
        map.put(MapConversions.DAYS, 7.0);
        // Mock converter to handle the mixed types.
        when(converter.convert(anyString(), eq(int.class))).thenAnswer(inv -> Integer.valueOf((String) inv.getArgument(0)));
        when(converter.convert(any(Long.class), eq(int.class))).thenAnswer(inv -> ((Number) inv.getArgument(0)).intValue());
        when(converter.convert(any(Double.class), eq(int.class))).thenAnswer(inv -> ((Number) inv.getArgument(0)).intValue());
        // Act
        Period result = MapConversions.toPeriod(map, converter);
        // Assert
        assertEquals(Period.of(5, 6, 7), result);
    }

    @Test
    void toPeriod_extraKeysIgnored() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, 1);
        map.put(MapConversions.MONTHS, 2);
        map.put(MapConversions.DAYS, 3);
        // unrelated key
        map.put("extra", 999);
        // Act
        Period result = MapConversions.toPeriod(map, converter);
        // Assert
        assertEquals(Period.of(1, 2, 3), result);
    }

    @Test
    void toPeriod_nullValuesDefaultZero() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, null);
        map.put(MapConversions.MONTHS, null);
        map.put(MapConversions.DAYS, null);
        // Converter returns 0 when the source is null.
        when(converter.convert(isNull(), eq(int.class))).thenReturn(0);
        // Act
        Period result = MapConversions.toPeriod(map, converter);
        // Assert
        assertEquals(Period.of(0, 0, 0), result);
        // default 0 passed to convert
        verify(converter, times(3)).convert(eq(0), eq(int.class));
    }

    @Test
    void toPeriod_invalidFromNotMap_throwsClassCastException() {
        // Arrange
        String notAMap = "not a map";
        // Act & Assert
        assertThrows(ClassCastException.class, () -> MapConversions.toPeriod(notAMap, converter), "Passing a non‑Map should cause a ClassCastException.");
    }

    @Test
    void toPeriod_nullFrom_throwsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> MapConversions.toPeriod(null, converter), "A null 'from' argument should result in a NullPointerException.");
    }

    @Test
    void toPeriod_nullConverter_throwsNullPointerException() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, 1);
        map.put(MapConversions.MONTHS, 2);
        map.put(MapConversions.DAYS, 3);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> MapConversions.toPeriod(map, null), "A null converter should cause a NullPointerException when used.");
    }

    @Test
    void toPeriod_converterReturnsNull_throwsNullPointerException() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, 5);
        map.put(MapConversions.MONTHS, 6);
        map.put(MapConversions.DAYS, 7);
        // Converter returns null for any conversion.
        when(converter.convert(any(), eq(int.class))).thenReturn(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> MapConversions.toPeriod(map, converter), "If the converter returns null, a NullPointerException should be thrown when intValue() is accessed.");
    }

    @Test
    void toPeriod_converterThrowsException_propagates() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        // non‑numeric string
        map.put(MapConversions.YEARS, "invalid");
        map.put(MapConversions.MONTHS, 2);
        map.put(MapConversions.DAYS, 3);
        // Converter throws IllegalArgumentException for the bad string.
        when(converter.convert(eq("invalid"), eq(int.class))).thenThrow(new IllegalArgumentException("Cannot convert 'invalid' to int"));
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toPeriod(map, converter), "Conversion exception should propagate out of toPeriod.");
        assertEquals("Cannot convert 'invalid' to int", ex.getMessage());
    }

    @Test
    void toPeriod_valueKeyWithNonMap_throwsClassCastException() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        // inner value is a String, not a Map
        map.put(MapConversions.VALUE, "not a map");
        // Act & Assert
        assertThrows(ClassCastException.class, () -> MapConversions.toPeriod(map, converter), "When 'value' is present but not a Map, fromMap should cause a ClassCastException.");
    }

    @Test
    void toPeriod_vKeyWithNullInner_throwsNullPointerException() {
        // Arrange
        Map<String, Object> map = new HashMap<>();
        // null inner value
        map.put(MapConversions.V, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> MapConversions.toPeriod(map, converter), "When '_v' is present with a null value, fromMap should throw NullPointerException.");
    }
}
