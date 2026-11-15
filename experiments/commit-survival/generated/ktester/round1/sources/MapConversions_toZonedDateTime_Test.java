package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toZonedDateTime_Test {

    @Mock
    Converter // mocked conversion utility
    converter;

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions so each test starts with a clean slate.
        clearInvocations(converter);
    }

    /*======================================================================
     *  1️⃣6️⃣  Parameterized tests for partial‑key scenarios that fall back
     *======================================================================*/
    private static Stream<Arguments> partialKeyScenarios() {
        return Stream.of(// date present, time missing, zone present
        Arguments.of(Map.of(MapConversions.DATE, "2021-12-31", MapConversions.ZONE, "Europe/London"), List.of(// we only expect zone conversion; date conversion is not used
        // because the branch requires both date & time.
        // The fallback will likely ignore these keys, resulting in null.
        // No interactions are verified for this case.
        "Europe/London")), // date and time present, zone missing
        Arguments.of(Map.of(MapConversions.DATE, "2022-01-15", MapConversions.TIME, "08:20:30"), List.of("2022-01-15", "08:20:30")), // only zone present
        Arguments.of(Map.of(MapConversions.ZONE, "Asia/Tokyo"), List.of("Asia/Tokyo")));
    }

    /**
     * -----------------------------------------------------------------
     *  1️⃣  Epoch‑millis branch
     *  -----------------------------------------------------------------
     */
    @Test
    void toZonedDateTime_usesEpochMillisWhenPresent() {
        // GIVEN
        // 2020‑09‑13T12:26:40Z
        long epoch = 1_600_000_000_000L;
        ZonedDateTime expected = Instant.ofEpochMilli(epoch).atZone(ZoneId.of("UTC"));
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        when(converter.convert(epoch, ZonedDateTime.class)).thenReturn(expected);
        // WHEN
        ZonedDateTime result = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertEquals(expected, result);
        verify(converter, times(1)).convert(epoch, ZonedDateTime.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * -----------------------------------------------------------------
     *  2️⃣  Full date‑time‑zone branch
     *  -----------------------------------------------------------------
     */
    @Test
    void toZonedDateTime_usesDateTimeAndZoneWhenAllThreePresent() {
        // GIVEN
        LocalDate date = LocalDate.of(2022, Month.MARCH, 15);
        LocalTime time = LocalTime.of(10, 30, 45);
        ZoneId zone = ZoneId.of("America/New_York");
        Map<String, Object> map = new HashMap<>();
        // values can be any object; converter mock decides
        map.put(MapConversions.DATE, "2022-03-15");
        map.put(MapConversions.TIME, "10:30:45");
        map.put(MapConversions.ZONE, "America/New_York");
        when(converter.convert("2022-03-15", LocalDate.class)).thenReturn(date);
        when(converter.convert("10:30:45", LocalTime.class)).thenReturn(time);
        when(converter.convert("America/New_York", ZoneId.class)).thenReturn(zone);
        // WHEN
        ZonedDateTime result = MapConversions.toZonedDateTime(map, converter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(date, time, zone);
        assertEquals(expected, result);
        verify(converter).convert("2022-03-15", LocalDate.class);
        verify(converter).convert("10:30:45", LocalTime.class);
        verify(converter).convert("America/New_York", ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * -----------------------------------------------------------------
     *  3️⃣  Zone + time (as LocalDateTime) branch
     *  -----------------------------------------------------------------
     */
    @Test
    void toZonedDateTime_usesZoneAndLocalDateTimeWhenDateMissing() {
        // GIVEN
        ZoneId zone = ZoneId.of("Europe/Paris");
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 59, 59);
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "Europe/Paris");
        // any object; mock decides
        map.put(MapConversions.TIME, "2021-12-31T23:59:59");
        when(converter.convert("Europe/Paris", ZoneId.class)).thenReturn(zone);
        when(converter.convert("2021-12-31T23:59:59", LocalDateTime.class)).thenReturn(ldt);
        // WHEN
        ZonedDateTime result = MapConversions.toZonedDateTime(map, converter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(ldt, zone);
        assertEquals(expected, result);
        verify(converter).convert("Europe/Paris", ZoneId.class);
        verify(converter).convert("2021-12-31T23:59:59", LocalDateTime.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * -----------------------------------------------------------------
     *  4️⃣  Fallback – none of the above keys present.
     *       The private `fromMap` method ultimately returns null when it
     *       cannot build a value, so we assert that behaviour.
     *  -----------------------------------------------------------------
     */
    @Test
    void toZonedDateTime_fallsBackWhenNoRecognisedKeys() {
        // GIVEN
        Map<String, Object> emptyMap = new HashMap<>();
        // No stubbing needed – the mock should not be called.
        // WHEN
        ZonedDateTime result = MapConversions.toZonedDateTime(emptyMap, converter);
        // THEN
        assertNull(result);
        verifyNoInteractions(converter);
    }

    /*======================================================================
     *  4️⃣  Fallback – empty map
     *======================================================================*/
    @Test
    void toZonedDateTime_fallbackReturnsNullWhenMapIsEmpty() {
        // GIVEN
        Map<String, Object> empty = new HashMap<>();
        // WHEN
        ZonedDateTime result = MapConversions.toZonedDateTime(empty, converter);
        // THEN
        assertNull(result);
        verifyNoInteractions(converter);
    }

    /*======================================================================
     *  5️⃣  Null epochMillis entry – should fall back
     *======================================================================*/
    @Test
    void toZonedDateTime_nullEpochMillisFallsBack() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // explicit null
        map.put(MapConversions.EPOCH_MILLIS, null);
        // WHEN
        ZonedDateTime result = MapConversions.toZonedDateTime(map, converter);
        // THEN
        // fallback currently yields null for this shape
        assertNull(result);
        verifyNoInteractions(converter);
    }

    /*======================================================================
     *  6️⃣  Null 'from' argument – ClassCastException
     *======================================================================*/
    @Test
    void toZonedDateTime_nullFromThrowsClassCastException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toZonedDateTime(from, converter));
        verifyNoInteractions(converter);
    }

    /*======================================================================
     *  7️⃣  Non‑Map 'from' argument – ClassCastException
     *======================================================================*/
    @Test
    void toZonedDateTime_nonMapFromThrowsClassCastException() {
        // GIVEN
        Object from = "just-a-string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toZonedDateTime(from, converter));
        verifyNoInteractions(converter);
    }

    /*======================================================================
     *  8️⃣  Null converter – NullPointerException
     *======================================================================*/
    @Test
    void toZonedDateTime_nullConverterThrowsNPE() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, 1609459200000L);
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toZonedDateTime(map, nullConverter));
    }

    /*======================================================================
     *  9️⃣  Invalid type for epochMillis – exception propagates
     *======================================================================*/
    @Test
    void toZonedDateTime_invalidEpochMillisPropagatesException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, "not-a-number");
        when(converter.convert("not-a-number", ZonedDateTime.class)).thenThrow(new NumberFormatException("Invalid number"));
        // WHEN / THEN
        NumberFormatException ex = assertThrows(NumberFormatException.class, () -> MapConversions.toZonedDateTime(map, converter));
        assertEquals("Invalid number", ex.getMessage());
        verify(converter).convert("not-a-number", ZonedDateTime.class);
        verifyNoMoreInteractions(converter);
    }

    /*======================================================================
     *  🔟  Exception in date conversion (date‑time‑zone branch)
     *======================================================================*/
    @Test
    void toZonedDateTime_exceptionInDateConversionPropagates() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2022-01-01");
        map.put(MapConversions.TIME, "12:34:56");
        map.put(MapConversions.ZONE, "America/New_York");
        when(converter.convert("2022-01-01", LocalDate.class)).thenThrow(new IllegalArgumentException("invalid date"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toZonedDateTime(map, converter));
        assertEquals("invalid date", ex.getMessage());
        // Only the date conversion should have been attempted.
        verify(converter).convert("2022-01-01", LocalDate.class);
        verifyNoMoreInteractions(converter);
    }

    /*======================================================================
     *  1️⃣1️⃣  Exception in zone conversion (zone‑time branch)
     *======================================================================*/
    @Test
    void toZonedDateTime_exceptionInZoneConversionPropagatesInZoneTimeBranch() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "Europe/Paris");
        map.put(MapConversions.TIME, "2023-03-15T09:45:00");
        when(converter.convert("Europe/Paris", ZoneId.class)).thenThrow(new IllegalArgumentException("invalid zone"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toZonedDateTime(map, converter));
        assertEquals("invalid zone", ex.getMessage());
        verify(converter).convert("Europe/Paris", ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    /*======================================================================
     *  1️⃣2️⃣  Null conversion result for date (date‑time‑zone branch)
     *======================================================================*/
    @Test
    void toZonedDateTime_nullDateConversionCausesNPE() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2022-05-10");
        map.put(MapConversions.TIME, "14:30:15");
        map.put(MapConversions.ZONE, "America/New_York");
        when(converter.convert("2022-05-10", LocalDate.class)).thenReturn(null);
        when(converter.convert("14:30:15", LocalTime.class)).thenReturn(LocalTime.of(14, 30, 15));
        when(converter.convert("America/New_York", ZoneId.class)).thenReturn(ZoneId.of("America/New_York"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toZonedDateTime(map, converter));
        verify(converter).convert("2022-05-10", LocalDate.class);
        verify(converter).convert("14:30:15", LocalTime.class);
        verify(converter).convert("America/New_York", ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    /*======================================================================
     *  1️⃣3️⃣  Null conversion result for time (date‑time‑zone branch)
     *======================================================================*/
    @Test
    void toZonedDateTime_nullTimeConversionCausesNPE() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2022-05-10");
        map.put(MapConversions.TIME, "14:30:15");
        map.put(MapConversions.ZONE, "America/New_York");
        when(converter.convert("2022-05-10", LocalDate.class)).thenReturn(LocalDate.of(2022, 5, 10));
        when(converter.convert("14:30:15", LocalTime.class)).thenReturn(null);
        when(converter.convert("America/New_York", ZoneId.class)).thenReturn(ZoneId.of("America/New_York"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toZonedDateTime(map, converter));
        verify(converter).convert("2022-05-10", LocalDate.class);
        verify(converter).convert("14:30:15", LocalTime.class);
        verify(converter).convert("America/New_York", ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    /*======================================================================
     *  1️⃣4️⃣  Null conversion result for zone (date‑time‑zone branch)
     *======================================================================*/
    @Test
    void toZonedDateTime_nullZoneConversionCausesNPE() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2022-05-10");
        map.put(MapConversions.TIME, "14:30:15");
        map.put(MapConversions.ZONE, "America/New_York");
        when(converter.convert("2022-05-10", LocalDate.class)).thenReturn(LocalDate.of(2022, 5, 10));
        when(converter.convert("14:30:15", LocalTime.class)).thenReturn(LocalTime.of(14, 30, 15));
        when(converter.convert("America/New_York", ZoneId.class)).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toZonedDateTime(map, converter));
        verify(converter).convert("2022-05-10", LocalDate.class);
        verify(converter).convert("14:30:15", LocalTime.class);
        verify(converter).convert("America/New_York", ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    /*======================================================================
     *  1️⃣5️⃣  Fallback using year/month/day/hour/minute/second/zone keys
     *
     *  The private {@code fromMap} method is exercised indirectly via
     *  {@code toZonedDateTime}.  We stub the converter to return the
     *  primitive values expected by the fallback logic.
     *======================================================================*/
    @Test
    void toZonedDateTime_fallbackBuildsZonedDateTimeFromYearMonthDayHourMinuteSecondAndZone() throws Exception {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("year", 2020);
        map.put("month", 1);
        map.put("day", 1);
        map.put("hour", 0);
        map.put("minute", 0);
        map.put("second", 0);
        map.put(MapConversions.ZONE, "UTC");
        // Stub conversions for each primitive component.
        when(converter.convert(2020, Integer.class)).thenReturn(2020);
        when(converter.convert(1, Integer.class)).thenReturn(1);
        when(converter.convert(1, Integer.class)).thenReturn(1);
        when(converter.convert(0, Integer.class)).thenReturn(0);
        when(converter.convert(0, Integer.class)).thenReturn(0);
        when(converter.convert(0, Integer.class)).thenReturn(0);
        when(converter.convert("UTC", ZoneId.class)).thenReturn(ZoneId.of("UTC"));
        // WHEN
        ZonedDateTime result = MapConversions.toZonedDateTime(map, converter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(expected, result);
        // Verify that the converter was asked for the zone (the other keys are
        // handled inside the private fromMap implementation – we cannot know the
        // exact conversion calls, but at least the zone conversion must happen.
        verify(converter).convert("UTC", ZoneId.class);
    }

    @ParameterizedTest
    @MethodSource("partialKeyScenarios")
    void toZonedDateTime_partialKeysFallBackToNull(Map<String, Object> map, List<String> expectedConversions) {
        // Stubbing any conversions that might be attempted by the fallback.
        // We simply return the same value (or a suitable type) to avoid NPEs.
        expectedConversions.forEach(key -> {
            if (key instanceof String) {
                when(converter.convert(key, ZoneId.class)).thenReturn(ZoneId.of((String) key));
                when(converter.convert(key, LocalDate.class)).thenReturn(null);
                when(converter.convert(key, LocalTime.class)).thenReturn(null);
                when(converter.convert(key, LocalDateTime.class)).thenReturn(null);
                when(converter.convert(key, Integer.class)).thenReturn(null);
            }
        });
        // WHEN
        ZonedDateTime result = MapConversions.toZonedDateTime(map, converter);
        // THEN
        // For these partial‑key maps the explicit branches are not taken,
        // and the generic fallback currently yields null.
        assertNull(result);
        // Verify that the converter was *not* called for a full date‑time‑zone
        // combination (i.e., no call to convert(date, LocalDate.class) when time is missing, etc.).
        // We only verify that at most the zone conversion was attempted.
        verify(converter, atMost(1)).convert(anyString(), eq(ZoneId.class));
        verifyNoMoreInteractions(converter);
    }
}
