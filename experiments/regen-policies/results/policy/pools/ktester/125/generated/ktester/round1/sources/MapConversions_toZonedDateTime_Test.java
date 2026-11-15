package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toZonedDateTime_Test {

    /**
     * The mock Converter used for all test cases.
     */
    @Mock
    private Converter converter;

    /**
     * A reusable map instance – each test populates it with the keys it needs.
     */
    private Map<String, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for every test to avoid cross‑test contamination.
        map = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test.
        clearInvocations(converter);
    }

    /**
     * Scenario 1 – the map contains an {@code epochMillis} entry.
     * The method should delegate directly to {@code converter.convert(epochMillis, ZonedDateTime.class)}.
     */
    @Test
    void toZonedDateTime_usesEpochMillis_whenPresent() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epoch = 1_640_995_200_000L;
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
        when(converter.convert(eq(epoch), eq(ZonedDateTime.class))).thenReturn(expected);
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertEquals(expected, actual);
        verify(converter, times(1)).convert(eq(epoch), eq(ZonedDateTime.class));
        verifyNoMoreInteractions(converter);
    }

    /**
     * Scenario 2 – the map contains {@code date}, {@code time} and {@code zone} entries.
     * The method must convert each component and combine them with {@code ZonedDateTime.of}.
     */
    @Test
    void toZonedDateTime_combinesDateTimeAndZone_whenAllThreePresent() {
        // GIVEN
        LocalDate date = LocalDate.of(2022, Month.MARCH, 15);
        LocalTime time = LocalTime.of(13, 45, 30);
        ZoneId zone = ZoneId.of("Europe/Paris");
        // values are arbitrary – conversion is mocked
        map.put(MapConversions.DATE, "2022-03-15");
        map.put(MapConversions.TIME, "13:45:30");
        map.put(MapConversions.ZONE, "Europe/Paris");
        when(converter.convert(any(), eq(LocalDate.class))).thenReturn(date);
        when(converter.convert(any(), eq(LocalTime.class))).thenReturn(time);
        when(converter.convert(any(), eq(ZoneId.class))).thenReturn(zone);
        ZonedDateTime expected = ZonedDateTime.of(date, time, zone);
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertEquals(expected, actual);
        verify(converter).convert(any(), eq(LocalDate.class));
        verify(converter).convert(any(), eq(LocalTime.class));
        verify(converter).convert(any(), eq(ZoneId.class));
        verifyNoMoreInteractions(converter);
    }

    /**
     * Scenario 3 – the map contains {@code time} and {@code zone} but no {@code date}.
     * The method should treat {@code time} as a {@code LocalDateTime}.
     */
    @Test
    void toZonedDateTime_usesLocalDateTimeAndZone_whenDateMissing() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2022, Month.DECEMBER, 5, 9, 15, 0);
        ZoneId zone = ZoneId.of("America/New_York");
        map.put(MapConversions.TIME, "2022-12-05T09:15:00");
        map.put(MapConversions.ZONE, "America/New_York");
        when(converter.convert(any(), eq(LocalDateTime.class))).thenReturn(ldt);
        when(converter.convert(any(), eq(ZoneId.class))).thenReturn(zone);
        ZonedDateTime expected = ZonedDateTime.of(ldt, zone);
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertEquals(expected, actual);
        verify(converter).convert(any(), eq(LocalDateTime.class));
        verify(converter).convert(any(), eq(ZoneId.class));
        verifyNoMoreInteractions(converter);
    }

    /**
     * Fallback branch – map does not contain any of the keys that trigger the explicit paths.
     * The private {@code fromMap} implementation returns {@code null} for such input.
     */
    @Test
    void toZonedDateTime_fallbackReturnsNull_whenNoRelevantKeys() {
        // GIVEN
        map.put("unrelatedKey", "someValue");
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertNull(actual);
        verifyNoInteractions(converter);
    }

    /**
     * EpochMillis has precedence over other keys.
     */
    @Test
    void toZonedDateTime_epochMillisPrecedence_overDateTimeZone() {
        // GIVEN
        // 2021‑06‑01T00:00:00Z
        long epoch = 1_622_505_600_000L;
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        // also populate other keys – they must be ignored
        map.put(MapConversions.DATE, "2022-06-01");
        map.put(MapConversions.TIME, "12:00:00");
        map.put(MapConversions.ZONE, "Asia/Tokyo");
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
        when(converter.convert(eq(epoch), eq(ZonedDateTime.class))).thenReturn(expected);
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertEquals(expected, actual);
        verify(converter, times(1)).convert(eq(epoch), eq(ZonedDateTime.class));
        verifyNoMoreInteractions(converter);
    }

    /**
     * Missing {@code zone} while having {@code date} and {@code time} leads to fallback (null).
     */
    @Test
    void toZonedDateTime_fallbackWhenZoneMissing() {
        // GIVEN
        map.put(MapConversions.DATE, "2022-12-31");
        map.put(MapConversions.TIME, "23:59:59");
        when(converter.convert(any(), eq(LocalDate.class))).thenReturn(LocalDate.of(2022, 12, 31));
        when(converter.convert(any(), eq(LocalTime.class))).thenReturn(LocalTime.of(23, 59, 59));
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertNull(actual);
        // No conversion to ZoneId should happen, but date/time conversions are still invoked
        verify(converter).convert(any(), eq(LocalDate.class));
        verify(converter).convert(any(), eq(LocalTime.class));
        verifyNoMoreInteractions(converter);
    }

    /**
     * Only {@code zone} present – fallback returns null and no conversion calls.
     */
    @Test
    void toZonedDateTime_fallbackWhenOnlyZonePresent() {
        // GIVEN
        map.put(MapConversions.ZONE, "Europe/London");
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertNull(actual);
        verifyNoInteractions(converter);
    }

    /**
     * Only {@code date} present – fallback returns null.
     */
    @Test
    void toZonedDateTime_fallbackWhenOnlyDatePresent() {
        // GIVEN
        map.put(MapConversions.DATE, "2020-02-29");
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertNull(actual);
        verifyNoInteractions(converter);
    }

    /**
     * Only {@code time} present – fallback returns null.
     */
    @Test
    void toZonedDateTime_fallbackWhenOnlyTimePresent() {
        // GIVEN
        map.put(MapConversions.TIME, "10:15:30");
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertNull(actual);
        verifyNoInteractions(converter);
    }

    /**
     * {@code epochMillis} is present but null – should be ignored and the method should continue
     * with the date/time/zone branch.
     */
    @Test
    void toZonedDateTime_nullEpochMillis_isIgnored() {
        // GIVEN
        map.put(MapConversions.EPOCH_MILLIS, null);
        map.put(MapConversions.DATE, "2023-01-01");
        map.put(MapConversions.TIME, "00:00:00");
        map.put(MapConversions.ZONE, "UTC");
        LocalDate date = LocalDate.of(2023, 1, 1);
        LocalTime time = LocalTime.MIDNIGHT;
        ZoneId zone = ZoneId.of("UTC");
        when(converter.convert(any(), eq(LocalDate.class))).thenReturn(date);
        when(converter.convert(any(), eq(LocalTime.class))).thenReturn(time);
        when(converter.convert(any(), eq(ZoneId.class))).thenReturn(zone);
        ZonedDateTime expected = ZonedDateTime.of(date, time, zone);
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertEquals(expected, actual);
        verify(converter).convert(any(), eq(LocalDate.class));
        verify(converter).convert(any(), eq(LocalTime.class));
        verify(converter).convert(any(), eq(ZoneId.class));
        verifyNoMoreInteractions(converter);
    }

    /**
     * Valid date/time/zone with extra unrelated keys – extra entries must not affect conversion.
     */
    @Test
    void toZonedDateTime_ignoresExtraKeys() {
        // GIVEN
        map.put(MapConversions.DATE, "2022-07-04");
        map.put(MapConversions.TIME, "16:20:00");
        map.put(MapConversions.ZONE, "America/Los_Angeles");
        map.put("extraKey1", "foo");
        map.put("extraKey2", 123);
        LocalDate date = LocalDate.of(2022, 7, 4);
        LocalTime time = LocalTime.of(16, 20, 0);
        ZoneId zone = ZoneId.of("America/Los_Angeles");
        when(converter.convert(any(), eq(LocalDate.class))).thenReturn(date);
        when(converter.convert(any(), eq(LocalTime.class))).thenReturn(time);
        when(converter.convert(any(), eq(ZoneId.class))).thenReturn(zone);
        ZonedDateTime expected = ZonedDateTime.of(date, time, zone);
        // WHEN
        ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
        // THEN
        assertEquals(expected, actual);
        verify(converter).convert(any(), eq(LocalDate.class));
        verify(converter).convert(any(), eq(LocalTime.class));
        verify(converter).convert(any(), eq(ZoneId.class));
        verifyNoMoreInteractions(converter);
    }

    /**
     * Passing {@code null} as the {@code from} argument should result in a {@link NullPointerException}
     * because the method casts the argument to a {@code Map} and dereferences it.
     */
    @Test
    void toZonedDateTime_nullFrom_throwsNPE() {
        // GIVEN
        Object nullFrom = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toZonedDateTime(nullFrom, converter));
        // No interaction with the mock should happen.
        verifyNoInteractions(converter);
    }
}
