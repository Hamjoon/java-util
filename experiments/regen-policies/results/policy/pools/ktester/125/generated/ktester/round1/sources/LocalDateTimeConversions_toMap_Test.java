package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toMap_Test {

    /**
     * A mock {@link Converter} – the method under test does not use it, but the signature requires it.
     */
    @Mock
    Converter mockConverter;

    /**
     * Asserts that the resulting map contains exactly the expected DATE and TIME entries
     * and that the iteration order matches {@code expectedOrder}.
     */
    private void assertMapContentAndOrder(Map<?, ?> result, String expectedDate, String expectedTime, List<String> expectedOrder) {
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        // Verify key/value pairs
        assertEquals(expectedDate, result.get(MapConversions.DATE), "DATE entry must match the expected ISO‑8601 date string");
        assertEquals(expectedTime, result.get(MapConversions.TIME), "TIME entry must match the expected ISO‑8601 time string");
        // Verify insertion order – CompactMap preserves insertion order
        Iterator<String> keyIter = result.keySet().iterator();
        for (String expectedKey : expectedOrder) {
            assertTrue(keyIter.hasNext(), "Map has fewer keys than expected");
            assertEquals(expectedKey, keyIter.next(), "Map insertion order does not match expected order");
        }
        assertFalse(keyIter.hasNext(), "Map has more keys than expected");
    }

    /**
     * Provides a stream of valid {@link LocalDateTime} instances together with the
     * expected DATE/TIME strings and the expected insertion order.
     */
    private static Stream<Arguments> validLocalDateTimeProvider() {
        return Stream.of(// Simple date‑time without nanoseconds
        arguments(LocalDateTime.of(2023, 5, 17, 14, 30, 45), "2023-05-17", "14:30:45", List.of(MapConversions.DATE, MapConversions.TIME)), // With nanoseconds – should be rendered with fractional seconds
        arguments(LocalDateTime.of(2021, 12, 31, 23, 59, 59, 123_000_000), "2021-12-31", "23:59:59.123", List.of(MapConversions.DATE, MapConversions.TIME)), // Minimum representable LocalDateTime
        arguments(LocalDateTime.MIN, "-999999999-01-01", "00:00", List.of(MapConversions.DATE, MapConversions.TIME)), // Maximum representable LocalDateTime
        arguments(LocalDateTime.MAX, "999999999-12-31", "23:59:59.999999999", List.of(MapConversions.DATE, MapConversions.TIME)), // Midnight edge case (no seconds)
        arguments(LocalDateTime.of(2022, 3, 15, 0, 0, 0), "2022-03-15", "00:00", List.of(MapConversions.DATE, MapConversions.TIME)), // Noon edge case (no seconds)
        arguments(LocalDateTime.of(2022, 3, 15, 12, 0, 0), "2022-03-15", "12:00", List.of(MapConversions.DATE, MapConversions.TIME)));
    }

    @Test
    void toMapProducesCorrectDateAndTimeEntries() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 14, 30, 45, 123_456_789);
        // WHEN
        Map<?, ?> result = LocalDateTimeConversions.toMap(ldt, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        // Expected keys are defined in MapConversions
        assertEquals(ldt.toLocalDate().toString(), result.get(MapConversions.DATE), "DATE entry must match the LocalDate part of the LocalDateTime");
        assertEquals(ldt.toLocalTime().toString(), result.get(MapConversions.TIME), "TIME entry must match the LocalTime part of the LocalDateTime");
    }

    @Test
    void toMapThrowsClassCastExceptionWhenSourceIsNotLocalDateTime() {
        // GIVEN
        Object notALocalDateTime = "2023-05-17T14:30:45";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toMap(notALocalDateTime, mockConverter), "A non‑LocalDateTime source should result in a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0} -> date={1}, time={2}")
    @MethodSource("validLocalDateTimeProvider")
    void toMapProducesCorrectEntriesForValidLocalDateTime(LocalDateTime input, String expectedDate, String expectedTime, List<String> expectedOrder) {
        // WHEN
        Map<?, ?> result = LocalDateTimeConversions.toMap(input, mockConverter);
        // THEN
        assertMapContentAndOrder(result, expectedDate, expectedTime, expectedOrder);
    }

    @Test
    void toMapIgnoresNonNullConverter() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 14, 30, 45);
        // a distinct mock, still unused
        Converter anotherMock = mock(Converter.class);
        // WHEN
        Map<?, ?> result = LocalDateTimeConversions.toMap(ldt, anotherMock);
        // THEN
        assertMapContentAndOrder(result, "2023-05-17", "14:30:45", List.of(MapConversions.DATE, MapConversions.TIME));
    }

    @Test
    void toMapThrowsNullPointerExceptionWhenFromIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toMap(nullSource, mockConverter), "Passing a null `from` argument should cause a NullPointerException");
    }
}
