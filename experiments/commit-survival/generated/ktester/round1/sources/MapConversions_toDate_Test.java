package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toDate_Test {

    @Mock
    Converter // a mock is sufficient – the conversion logic under test does not depend on its behavior
    converter;

    private static Stream<Arguments> epochMillisPresentProvider() {
        return Stream.of(// 1. java.util.Date input – epochMillis = date.getTime(), nanos = 0
        Arguments.of("Date input should produce a Date with the same instant", new Date(1622548800000L), new Date(1622548800000L)), // 2. java.time.Instant with nanos – nanos are truncated to milliseconds
        Arguments.of("Instant with nanos should be truncated to milliseconds", Instant.ofEpochMilli(1622548800123L).plusNanos(456_000), new Date(1622548800123L)), // 3. java.sql.Timestamp – nanoseconds are truncated to milliseconds
        Arguments.of("Timestamp nanoseconds are truncated to milliseconds", Timestamp.valueOf("2021-06-01 12:00:00.123456789"), new Date(1622548800123L)));
    }

    private static Stream<Arguments> invalidMapMissingKeysProvider() {
        return Stream.of(Arguments.of("Map with only 'epochMillis' should be considered invalid", Collections.singletonMap("epochMillis", 1622548800000L)), Arguments.of("Empty map should be considered invalid", new HashMap<String, Object>()));
    }

    @Test
    @DisplayName("toDate returns the same Date when the source object is a java.util.Date")
    void toDate_givenDate_returnsSameDate() {
        // GIVEN
        Date now = new Date();
        // WHEN
        Date result = MapConversions.toDate(now, converter);
        // THEN
        assertEquals(now, result, "The returned Date should be equal to the input Date");
    }

    @Test
    @DisplayName("toDate creates a Date from a map containing the 'date' key")
    void toDate_givenMapWithDateKey_returnsDateFromMap() {
        // GIVEN
        Date expected = new Date(123_456_789L);
        Map<String, Object> sourceMap = new HashMap<>();
        // key that fromMap recognises for Date conversion
        sourceMap.put("date", expected);
        // WHEN
        Date result = MapConversions.toDate(sourceMap, converter);
        // THEN
        assertEquals(expected, result, "The returned Date should be the one stored under the 'date' key");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("epochMillisPresentProvider")
    @DisplayName("toDate builds a Date from epochMillis + nanos when epoch information is present")
    void toDate_EpochMillisPresent(String description, Object from, Date expected) {
        // WHEN
        // converter not required for these inputs
        Date result = MapConversions.toDate(from, null);
        // THEN
        assertEquals(expected, result, description);
    }

    @Test
    @DisplayName("toDate throws NullPointerException when 'from' is null")
    void toDate_NullFrom_throwsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toDate(null, null), "A null source object should cause a NullPointerException");
    }

    @Test
    @DisplayName("toDate throws NullPointerException when a map needs a Converter but null is supplied")
    void toDate_MapNeedsConverter_nullConverter_throwsNPE() {
        Map<String, Object> map = Collections.singletonMap("date", "2021-06-01");
        assertThrows(NullPointerException.class, () -> MapConversions.toDate(map, null), "When a map contains a date string the converter is required; null should cause NPE");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidMapMissingKeysProvider")
    @DisplayName("toDate throws IllegalArgumentException for maps missing required key sets")
    void toDate_InvalidMapMissingRequiredKeys(String description, Map<String, Object> map) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDate(map, null), description);
    }

    @Test
    @DisplayName("toDate throws ClassCastException when 'epochMillis' is not a Number")
    void toDate_InvalidEpochMillisType_throwsClassCastException() {
        Map<String, Object> map = Collections.singletonMap("epochMillis", "not-a-number");
        assertThrows(ClassCastException.class, () -> MapConversions.toDate(map, null), "A non‑numeric epochMillis value should cause a ClassCastException");
    }

    @Test
    @DisplayName("toDate throws ClassCastException when 'nanos' is not an Integer")
    void toDate_InvalidNanosType_throwsClassCastException() {
        Map<String, Object> map = Map.of("epochMillis", 1622548800000L, "nanos", "123456");
        assertThrows(ClassCastException.class, () -> MapConversions.toDate(map, null), "A non‑integer nanos value should cause a ClassCastException");
    }

    @Test
    @DisplayName("toDate throws IllegalArgumentException for negative epochMillis")
    void toDate_NegativeEpochMillis_throwsIllegalArgumentException() {
        Map<String, Object> map = Collections.singletonMap("epochMillis", -1L);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDate(map, null), "Negative epochMillis should be rejected with IllegalArgumentException");
    }
}
