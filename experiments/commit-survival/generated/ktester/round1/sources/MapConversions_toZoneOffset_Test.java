package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.DateTimeException;

@ExtendWith(MockitoExtension.class)
class MapConversions_toZoneOffset_Test {

    /**
     * The mock Converter used for all tests.
     */
    @Mock
    Converter converter;

    /**
     * Common test data – a map that will be reused (cleared) between tests.
     */
    private Map<String, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test to avoid cross‑test contamination.
        map = new HashMap<>();
        /* The Converter is used only for the `convert` calls inside
         * `MapConversions.toZoneOffset`.  We make it behave like an identity
         * conversion for `int` values – it simply returns the supplied value.
         * This mirrors the real Converter behaviour for primitive types
         * without pulling in the heavy conversion machinery. */
        when(converter.convert(any(), eq(int.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to ensure a clean slate for the next test.
        reset(converter);
    }

    /**
     * Verify that when the source map contains {@code hours}, {@code minutes}
     * and {@code seconds} the method builds the correct {@link ZoneOffset}
     * using {@link ZoneOffset#ofHoursMinutesSeconds(int, int, int)}.
     */
    @Test
    void toZoneOffset_WithAllComponents_ShouldReturnExactOffset() {
        // GIVEN
        map.put(MapConversions.HOURS, 5);
        map.put(MapConversions.MINUTES, 30);
        map.put(MapConversions.SECONDS, 15);
        // WHEN
        ZoneOffset result = MapConversions.toZoneOffset(map, converter);
        // THEN
        ZoneOffset expected = ZoneOffset.ofHoursMinutesSeconds(5, 30, 15);
        assertEquals(expected, result, "ZoneOffset should be built from the supplied hours, minutes and seconds");
    }

    /**
     * Verify that when only {@code hours} is supplied the optional
     * {@code minutes} and {@code seconds} default to {@code 0}.
     */
    @Test
    void toZoneOffset_WithOnlyHours_ShouldDefaultMinutesAndSecondsToZero() {
        // GIVEN
        // negative offset is also valid
        map.put(MapConversions.HOURS, -3);
        // WHEN
        ZoneOffset result = MapConversions.toZoneOffset(map, converter);
        // THEN
        // minutes and seconds are zero
        ZoneOffset expected = ZoneOffset.ofHours(-3);
        assertEquals(expected, result, "Missing minutes/seconds should default to zero, yielding a simple hour‑only offset");
    }

    @Test
    @DisplayName("Missing HOURS key – fallback should throw IllegalArgumentException")
    void toZoneOffset_MissingHoursKey_ShouldThrowIllegalArgumentException() {
        map.put(MapConversions.MINUTES, 20);
        map.put(MapConversions.SECONDS, 10);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneOffset(map, converter), "When HOURS is absent the method falls back to fromMap which should reject the input");
    }

    @Test
    @DisplayName("Empty map – fallback should throw IllegalArgumentException")
    void toZoneOffset_EmptyMap_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneOffset(map, converter), "Empty map provides no conversion data and must result in IllegalArgumentException");
    }

    @Test
    @DisplayName("Non‑map input (String) – ClassCastException expected")
    void toZoneOffset_NonMapString_ShouldThrowClassCastException() {
        String notAMap = "plain string";
        assertThrows(ClassCastException.class, () -> MapConversions.toZoneOffset(notAMap, converter), "Casting a non‑map argument to Map<String,Object> should raise ClassCastException");
    }

    @Test
    @DisplayName("Non‑map input (Integer) – ClassCastException expected")
    void toZoneOffset_NonMapInteger_ShouldThrowClassCastException() {
        Integer notAMap = 12345;
        assertThrows(ClassCastException.class, () -> MapConversions.toZoneOffset(notAMap, converter), "Casting a non‑map argument to Map<String,Object> should raise ClassCastException");
    }

    @Test
    @DisplayName("Null `from` argument – NullPointerException expected")
    void toZoneOffset_NullFrom_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toZoneOffset(null, converter), "`from` is null; method should throw NullPointerException when accessing it");
    }

    @Test
    @DisplayName("Null `converter` argument – NullPointerException expected")
    void toZoneOffset_NullConverter_ShouldThrowNullPointerException() {
        map.put(MapConversions.HOURS, 2);
        assertThrows(NullPointerException.class, () -> MapConversions.toZoneOffset(map, null), "`converter` is null; calling convert on it should raise NullPointerException");
    }

    @Test
    @DisplayName("Non‑numeric hour value – converter throws IllegalArgumentException")
    void toZoneOffset_NonNumericHour_ShouldPropagateIllegalArgumentException() {
        map.put(MapConversions.HOURS, "five");
        map.put(MapConversions.MINUTES, 0);
        map.put(MapConversions.SECONDS, 0);
        // Configure the mock to throw when trying to convert the string "five".
        when(converter.convert(eq("five"), eq(int.class))).thenThrow(new IllegalArgumentException("Cannot convert 'five' to int"));
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneOffset(map, converter), "Converter failure should propagate as IllegalArgumentException");
    }

    @Test
    @DisplayName("Null hour value in map – converter throws IllegalArgumentException")
    void toZoneOffset_NullHourValue_ShouldPropagateIllegalArgumentException() {
        map.put(MapConversions.HOURS, null);
        map.put(MapConversions.MINUTES, 0);
        map.put(MapConversions.SECONDS, 0);
        when(converter.convert(isNull(), eq(int.class))).thenThrow(new IllegalArgumentException("Hour value is null"));
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneOffset(map, converter), "Converter should reject null hour value with IllegalArgumentException");
    }

    @Test
    @DisplayName("Hour value out of allowed range – DateTimeException expected")
    void toZoneOffset_HourOutOfRange_ShouldThrowDateTimeException() {
        // valid range is -18..+18
        map.put(MapConversions.HOURS, 20);
        map.put(MapConversions.MINUTES, 0);
        map.put(MapConversions.SECONDS, 0);
        assertThrows(DateTimeException.class, () -> MapConversions.toZoneOffset(map, converter), "Hour offset beyond +/-18 should cause DateTimeException");
    }

    @Test
    @DisplayName("Minute value out of allowed range – DateTimeException expected")
    void toZoneOffset_MinuteOutOfRange_ShouldThrowDateTimeException() {
        map.put(MapConversions.HOURS, 5);
        // valid range 0..59
        map.put(MapConversions.MINUTES, 75);
        map.put(MapConversions.SECONDS, 0);
        assertThrows(DateTimeException.class, () -> MapConversions.toZoneOffset(map, converter), "Minute offset beyond 59 should cause DateTimeException");
    }

    @Test
    @DisplayName("Second value out of allowed range – DateTimeException expected")
    void toZoneOffset_SecondOutOfRange_ShouldThrowDateTimeException() {
        map.put(MapConversions.HOURS, -4);
        map.put(MapConversions.MINUTES, 30);
        // valid range 0..59
        map.put(MapConversions.SECONDS, 70);
        assertThrows(DateTimeException.class, () -> MapConversions.toZoneOffset(map, converter), "Second offset beyond 59 should cause DateTimeException");
    }
}
