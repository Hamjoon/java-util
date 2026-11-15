package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toMap_Test {

    private static class DummyConverter implements Converter {
        // Empty implementation – the test only needs an instance.
    }

    private static Stream<Arguments> validLocalDateProvider() {
        return Stream.of(// regular date
        Arguments.of(LocalDate.of(2023, 5, 17), "{\"DATE\":\"2023-05-17\"}"), // epoch day (1970‑01‑01)
        Arguments.of(LocalDate.ofEpochDay(0), "{\"DATE\":\"1970-01-01\"}"), // minimum representable date
        Arguments.of(LocalDate.MIN, "{\"DATE\":\"-999999999-01-01\"}"), // leap day
        Arguments.of(LocalDate.of(2000, 2, 29), "{\"DATE\":\"2000-02-29\"}"), // far‑future date
        Arguments.of(LocalDate.of(9999, 12, 31), "{\"DATE\":\"9999-12-31\"}"));
    }

    @Test
    void toMap_returnsMapContainingDateString() {
        // GIVEN
        LocalDate sampleDate = LocalDate.of(2023, 5, 17);
        // converter is not used by toMap
        Converter mockConverter = mock(Converter.class);
        // WHEN
        Map<String, Object> result = LocalDateConversions.toMap(sampleDate, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(sampleDate.toString(), result.get(MapConversions.DATE), "Map entry for MapConversions.DATE should be the ISO‑8601 string of the LocalDate");
    }

    @ParameterizedTest(name = "{index}: toMap({0}) → {1}")
    @MethodSource("validLocalDateProvider")
    void toMap_returnsCorrectMapForValidLocalDate(LocalDate input, String expectedJson) {
        // GIVEN
        // not used by the method
        Converter mockConverter = mock(Converter.class);
        // WHEN
        Map<String, Object> result = LocalDateConversions.toMap(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(input.toString(), result.get(MapConversions.DATE), "Map entry must be the ISO‑8601 string representation of the LocalDate");
        // Simple JSON string check for documentation purposes
        assertEquals(expectedJson, "{\"DATE\":\"" + result.get(MapConversions.DATE) + "\"}");
    }

    @Test
    void toMap_nullFrom_throwsNullPointerException() {
        // GIVEN
        Converter mockConverter = mock(Converter.class);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toMap(null, mockConverter), "Passing null as 'from' should result in NullPointerException");
    }

    @Test
    void toMap_invalidType_throwsClassCastException() {
        // GIVEN
        // a String, not a LocalDate
        Object notALocalDate = "2023-05-17";
        Converter mockConverter = mock(Converter.class);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toMap(notALocalDate, mockConverter), "Providing a non‑LocalDate object should cause ClassCastException");
    }

    @Test
    void toMap_nonNullConverter_isIgnored() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 5, 17);
        // non‑null converter
        Converter dummy = new DummyConverter();
        // WHEN
        Map<String, Object> result = LocalDateConversions.toMap(date, dummy);
        // THEN
        assertNotNull(result);
        assertEquals(date.toString(), result.get(MapConversions.DATE));
    }

    @Test
    void toMap_returnsModifiableMap() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 5, 17);
        Converter mockConverter = mock(Converter.class);
        // WHEN
        Map<String, Object> result = LocalDateConversions.toMap(date, mockConverter);
        // THEN – the map should be mutable; adding a new entry must succeed
        assertDoesNotThrow(() -> result.put("extra", 42), "Returned map should be modifiable (not an immutable wrapper)");
        assertEquals(42, result.get("extra"));
    }
}
