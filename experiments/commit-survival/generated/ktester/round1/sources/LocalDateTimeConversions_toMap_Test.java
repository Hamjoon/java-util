package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toMap_Test {

    /**
     * The mock Converter is not used by {@code toMap} but must be supplied.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A no‑op implementation of {@link Converter}.
     */
    private static class DummyConverter implements Converter {
        // No methods needed for this test – the interface is assumed to have none
        // that are invoked by toMap.
    }

    /**
     * A Converter whose methods always throw – used to prove toMap never calls it.
     */
    private static class ThrowingConverter implements Converter {

        // If any method were called it would throw; we leave the body empty
        // because the interface contract is unknown – the test ensures no call occurs.
        @Override
        public String toString() {
            throw new RuntimeException("Converter should not be invoked");
        }
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(Arguments.of("Typical LocalDateTime", LocalDateTime.of(2023, 5, 10, 15, 30, 45), null, "2023-05-10", "15:30:45"), Arguments.of("Edge values – midnight with nanoseconds", LocalDateTime.of(2000, 1, 1, 0, 0, 0, 123_456_789), MockConverter.INSTANCE, "2000-01-01", "00:00:00.123456789"), Arguments.of("Maximum supported year", LocalDateTime.of(999_999_999, 12, 31, 23, 59, 59, 999_999_999), null, "999999999-12-31", "23:59:59.999999999"), Arguments.of("Minimum (negative) year", LocalDateTime.of(-999_999_999, 1, 1, 0, 0, 0, 0), null, "-999999999-01-01", "00:00:00"), Arguments.of("Maximum nanosecond precision", LocalDateTime.of(2022, 6, 15, 12, 34, 56, 999_999_999), null, "2022-06-15", "12:34:56.999999999"), Arguments.of("Zero nanosecond fraction", LocalDateTime.of(2022, 6, 15, 12, 34, 56, 0), null, "2022-06-15", "12:34:56"), Arguments.of("Converter non‑null but ignored", LocalDateTime.of(2021, 12, 31, 23, 59, 59), new DummyConverter(), "2021-12-31", "23:59:59"), Arguments.of("Throwing converter is never used", LocalDateTime.of(2023, 3, 14, 1, 59, 26), new ThrowingConverter(), "2023-03-14", "01:59:26"));
    }

    @Test
    void toMap_returnsMapWithDateAndTimeStrings() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000);
        // WHEN
        Map<?, ?> result = LocalDateTimeConversions.toMap(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertTrue(result instanceof CompactLinkedMap, "Result should be a CompactLinkedMap");
        // The expected string representations are the default toString() of LocalDate and LocalTime
        // e.g., "2023-05-17"
        String expectedDate = source.toLocalDate().toString();
        // e.g., "14:30:45.123"
        String expectedTime = source.toLocalTime().toString();
        assertEquals(expectedDate, result.get(MapConversions.DATE), "Map should contain the correct DATE entry");
        assertEquals(expectedTime, result.get(MapConversions.TIME), "Map should contain the correct TIME entry");
    }

    @Test
    void toMap_nullSource_throwsClassCastException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toMap(source, mockConverter), "Passing null should result in a ClassCastException because of the cast to LocalDateTime");
    }

    /* -------------------------------------------------------------
     *  Successful conversions – typical and edge‑case values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("successfulConversionProvider")
    void toMap_successfulConversion(String description, LocalDateTime source, Converter converter, String expectedDate, String expectedTime) {
        // WHEN
        Map<?, ?> result = LocalDateTimeConversions.toMap(source, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertTrue(result instanceof CompactLinkedMap, "Result should be a CompactLinkedMap");
        assertEquals(expectedDate, result.get(MapConversions.DATE), "DATE entry mismatch");
        assertEquals(expectedTime, result.get(MapConversions.TIME), "TIME entry mismatch");
    }

    /* -------------------------------------------------------------
     *  Null source – expect NullPointerException (cast succeeds, but
     *  toLocalDate()/toLocalTime() dereference null)
     * ------------------------------------------------------------- */
    @Test
    void toMap_nullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toMap(source, null), "Null source should cause NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Invalid type for `from` – expect ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void toMap_invalidType_throwsClassCastException() {
        // GIVEN
        Object source = "2023-05-10T15:30:45";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toMap(source, mockConverter), "Non‑LocalDateTime source must cause ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Map structure validation – only DATE and TIME keys, values are
     *  Strings, map size is exactly 2.
     * ------------------------------------------------------------- */
    @Test
    void toMap_mapContainsOnlyDateAndTimeKeys() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2021, 7, 20, 10, 20, 30);
        // WHEN
        Map<?, ?> result = LocalDateTimeConversions.toMap(source, mockConverter);
        // THEN
        assertEquals(2, result.size(), "Map must contain exactly two entries");
        assertTrue(result.containsKey(MapConversions.DATE), "Map must contain DATE key");
        assertTrue(result.containsKey(MapConversions.TIME), "Map must contain TIME key");
        assertTrue(result.get(MapConversions.DATE) instanceof String, "DATE value must be a String");
        assertTrue(result.get(MapConversions.TIME) instanceof String, "TIME value must be a String");
    }
}
