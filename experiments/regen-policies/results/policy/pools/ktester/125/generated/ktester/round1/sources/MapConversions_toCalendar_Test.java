package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.format.DateTimeParseException;

@ExtendWith(MockitoExtension.class)
class MapConversions_toCalendar_Test {

    @Mock
    Converter // mocked Converter used in all tests
    mockConverter;

    // map supplied to MapConversions.toCalendar
    Map<String, Object> map;

    @Mock
    ConverterOptions // options returned by mockConverter.getOptions()
    mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        map = new HashMap<>();
        // Reset mock between tests to avoid cross‑test stubbing leakage
        reset(mockConverter, mockOptions);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    /**
     * When the source map contains the key {@code "epochMillis"} the method should delegate
     * directly to {@code Converter.convert(Object, Class)} and return the Calendar produced
     * by that conversion.
     */
    @Test
    void testEpochMillisConversion() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        Long epoch = 1_640_995_200_000L;
        map.put("epochMillis", epoch);
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.setTimeInMillis(epoch);
        when(mockConverter.convert(epoch, Calendar.class)).thenReturn(expected);
        // WHEN
        Calendar result = MapConversions.toCalendar(map, mockConverter);
        // THEN
        assertSame(expected, result, "The Calendar returned should be the one produced by the Converter");
        verify(mockConverter, times(1)).convert(epoch, Calendar.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * When the source map contains both {@code "date"} and {@code "time"} (and optionally a zone)
     * the method must:
     * <ul>
     *   <li>convert the date value to {@link LocalDate}</li>
     *   <li>convert the time value to {@link LocalTime}</li>
     *   <li>convert the zone value to {@link ZoneId} (or fall back to the Converter options)</li>
     *   <li>assemble a {@link Calendar} with the appropriate fields.</li>
     * </ul>
     */
    @Test
    void testDateAndTimeConversionWithExplicitZone() {
        // GIVEN
        Object dateKey = "myDate";
        Object timeKey = "myTime";
        Object zoneKey = "myZone";
        map.put("date", dateKey);
        map.put("time", timeKey);
        map.put("zone", zoneKey);
        LocalDate localDate = LocalDate.of(2021, Month.MAY, 20);
        // 123 ms
        LocalTime localTime = LocalTime.of(14, 30, 15, 123_000_000);
        ZoneId zoneId = ZoneId.of("America/New_York");
        // Stub the three conversions performed inside the method
        when(mockConverter.convert(dateKey, LocalDate.class)).thenReturn(localDate);
        when(mockConverter.convert(timeKey, LocalTime.class)).thenReturn(localTime);
        when(mockConverter.convert(zoneKey, ZoneId.class)).thenReturn(zoneId);
        // WHEN
        Calendar result = MapConversions.toCalendar(map, mockConverter);
        // THEN – verify calendar fields match the converted values
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(TimeZone.getTimeZone(zoneId).getID(), result.getTimeZone().getID(), "Calendar must use the supplied ZoneId");
        assertEquals(localDate.getYear(), result.get(Calendar.YEAR));
        // Calendar months are 0‑based
        assertEquals(localDate.getMonthValue() - 1, result.get(Calendar.MONTH));
        assertEquals(localDate.getDayOfMonth(), result.get(Calendar.DAY_OF_MONTH));
        assertEquals(localTime.getHour(), result.get(Calendar.HOUR_OF_DAY));
        assertEquals(localTime.getMinute(), result.get(Calendar.MINUTE));
        assertEquals(localTime.getSecond(), result.get(Calendar.SECOND));
        assertEquals(localTime.getNano() / 1_000_000, result.get(Calendar.MILLISECOND));
        // Verify the three expected conversions and no others
        verify(mockConverter, times(1)).convert(dateKey, LocalDate.class);
        verify(mockConverter, times(1)).convert(timeKey, LocalTime.class);
        verify(mockConverter, times(1)).convert(zoneKey, ZoneId.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * When the source map contains only a {@code "time"} entry (no {@code "date"}) the method
     * should treat the value as a date‑time string, parse it with {@link DateUtilities},
     * and set the Calendar to the resulting instant.
     *
     * This test uses a real {@link Converter} (not a mock) to avoid having to mock static
     * parsing utilities.
     */
    @Test
    void testTimeOnlyConversionUsesDateUtilities() {
        // GIVEN – a real Converter with default options
        Converter realConverter = new Converter(new ConverterOptions());
        // ISO‑8601 time string that includes a date component; DateUtilities will parse it.
        String isoString = "2023-08-01T12:34:56.789";
        map.put("time", isoString);
        // WHEN
        Calendar result = MapConversions.toCalendar(map, realConverter);
        // THEN – the Calendar should represent the same instant as the parsed ZonedDateTime
        ZonedDateTime expectedZdt = DateUtilities.parseDate(isoString, realConverter.getOptions().getZoneId(), true);
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(expectedZdt.getYear(), result.get(Calendar.YEAR));
        assertEquals(expectedZdt.getMonthValue() - 1, result.get(Calendar.MONTH));
        assertEquals(expectedZdt.getDayOfMonth(), result.get(Calendar.DAY_OF_MONTH));
        assertEquals(expectedZdt.getHour(), result.get(Calendar.HOUR_OF_DAY));
        assertEquals(expectedZdt.getMinute(), result.get(Calendar.MINUTE));
        assertEquals(expectedZdt.getSecond(), result.get(Calendar.SECOND));
        assertEquals(expectedZdt.getNano() / 1_000_000, result.get(Calendar.MILLISECOND));
    }

    /**
     * The map contains a null {@code "epochMillis"} entry together with valid
     * {@code "date"} and {@code "time"} entries.  The method must ignore the null
     * epoch value and continue with the date‑time handling branch.
     */
    @Test
    void testEpochMillisNullContinuesToDateTimeBranch() {
        // GIVEN
        map.put("epochMillis", null);
        map.put("date", "2021-06-01");
        map.put("time", "12:30:45");
        LocalDate expectedDate = LocalDate.of(2021, 6, 1);
        LocalTime expectedTime = LocalTime.of(12, 30, 45);
        ZoneId defaultZone = ZoneId.of("UTC");
        when(mockConverter.convert("2021-06-01", LocalDate.class)).thenReturn(expectedDate);
        when(mockConverter.convert("12:30:45", LocalTime.class)).thenReturn(expectedTime);
        when(mockOptions.getZoneId()).thenReturn(defaultZone);
        // WHEN
        Calendar result = MapConversions.toCalendar(map, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(TimeZone.getTimeZone(defaultZone).getID(), result.getTimeZone().getID());
        assertEquals(2021, result.get(Calendar.YEAR));
        // June -> 5 (0‑based)
        assertEquals(5, result.get(Calendar.MONTH));
        assertEquals(1, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(12, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, result.get(Calendar.MINUTE));
        assertEquals(45, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
        verify(mockConverter).convert("2021-06-01", LocalDate.class);
        verify(mockConverter).convert("12:30:45", LocalTime.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Time‑only map with an explicit zone.  The method should parse the time string
     * using {@link DateUtilities#parseDate(String, ZoneId, boolean)} and set the
     * Calendar to the resulting instant.
     */
    @Test
    void testTimeOnlyWithExplicitZone() {
        // GIVEN
        String timeString = "2023-01-01T08:15:30.250";
        map.put("time", timeString);
        map.put("zone", "Europe/Paris");
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        when(mockConverter.convert("Europe/Paris", ZoneId.class)).thenReturn(zoneId);
        // WHEN
        Calendar result = MapConversions.toCalendar(map, mockConverter);
        // THEN
        ZonedDateTime expected = DateUtilities.parseDate(timeString, zoneId, true);
        assertNotNull(result);
        assertEquals(TimeZone.getTimeZone(zoneId).getID(), result.getTimeZone().getID());
        assertEquals(expected.getYear(), result.get(Calendar.YEAR));
        assertEquals(expected.getMonthValue() - 1, result.get(Calendar.MONTH));
        assertEquals(expected.getDayOfMonth(), result.get(Calendar.DAY_OF_MONTH));
        assertEquals(expected.getHour(), result.get(Calendar.HOUR_OF_DAY));
        assertEquals(expected.getMinute(), result.get(Calendar.MINUTE));
        assertEquals(expected.getSecond(), result.get(Calendar.SECOND));
        assertEquals(expected.getNano() / 1_000_000, result.get(Calendar.MILLISECOND));
        verify(mockConverter).convert("Europe/Paris", ZoneId.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Time‑only map without a zone; the method must fall back to the default
     * {@link ZoneId} supplied by {@code ConverterOptions}.
     */
    @Test
    void testTimeOnlyWithoutZoneUsesDefaultZone() {
        // GIVEN
        String timeString = "2023-02-15T14:45:00";
        map.put("time", timeString);
        ZoneId defaultZone = ZoneId.of("Asia/Tokyo");
        when(mockOptions.getZoneId()).thenReturn(defaultZone);
        // WHEN
        Calendar result = MapConversions.toCalendar(map, mockConverter);
        // THEN
        ZonedDateTime expected = DateUtilities.parseDate(timeString, defaultZone, true);
        assertNotNull(result);
        assertEquals(TimeZone.getTimeZone(defaultZone).getID(), result.getTimeZone().getID());
        assertEquals(expected.getYear(), result.get(Calendar.YEAR));
        assertEquals(expected.getMonthValue() - 1, result.get(Calendar.MONTH));
        assertEquals(expected.getDayOfMonth(), result.get(Calendar.DAY_OF_MONTH));
        assertEquals(expected.getHour(), result.get(Calendar.HOUR_OF_DAY));
        assertEquals(expected.getMinute(), result.get(Calendar.MINUTE));
        assertEquals(expected.getSecond(), result.get(Calendar.SECOND));
        assertEquals(expected.getNano() / 1_000_000, result.get(Calendar.MILLISECOND));
        verifyNoInteractions(mockConverter);
    }

    /**
     * When the map does not contain any of the recognised keys, the method should
     * delegate to the generic {@code fromMap} fallback, which for an incomplete key set
     * returns {@code null} for {@link Calendar}.
     */
    @Test
    void testFallbackToFromMapReturnsNull() {
        // GIVEN – only a date key, no time or epochMillis
        map.put("date", "2023-01-15");
        // WHEN
        Calendar result = MapConversions.toCalendar(map, mockConverter);
        // THEN
        assertNull(result, "Fallback path should return null for Calendar when required keys are missing");
    }

    /**
     * Invalid time string should cause {@link DateUtilities#parseDate} to throw a
     * {@link DateTimeParseException}, which must be propagated.
     */
    @Test
    void testInvalidTimeFormatThrowsException() {
        // GIVEN
        map.put("time", "invalid‑time‑string");
        map.put("zone", "UTC");
        ZoneId utc = ZoneId.of("UTC");
        when(mockConverter.convert("UTC", ZoneId.class)).thenReturn(utc);
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> MapConversions.toCalendar(map, mockConverter), "Parsing an invalid time string should propagate DateTimeParseException");
    }

    /**
     * {@code "epochMillis"} supplied as a {@link String}.  The method forwards the
     * value to {@code Converter.convert(Object, Calendar.class)}; the mock converter
     * parses the string and returns a Calendar set to the appropriate epoch.
     */
    @Test
    void testEpochMillisStringConversion() {
        // GIVEN
        String epochString = "1622548800123";
        map.put("epochMillis", epochString);
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.setTimeInMillis(Long.parseLong(epochString));
        when(mockConverter.convert(epochString, Calendar.class)).thenReturn(expected);
        // WHEN
        Calendar result = MapConversions.toCalendar(map, mockConverter);
        // THEN
        assertSame(expected, result);
        verify(mockConverter).convert(epochString, Calendar.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Zone supplied as a string that can be converted to a {@link ZoneId}
     * (e.g., "Europe/London").
     */
    @Test
    void testZoneSuppliedAsZoneIdString() {
        // GIVEN
        map.put("date", "2021-07-04");
        map.put("time", "09:30:00");
        map.put("zone", "Europe/London");
        LocalDate date = LocalDate.of(2021, 7, 4);
        LocalTime time = LocalTime.of(9, 30, 0);
        ZoneId zoneId = ZoneId.of("Europe/London");
        when(mockConverter.convert("2021-07-04", LocalDate.class)).thenReturn(date);
        when(mockConverter.convert("09:30:00", LocalTime.class)).thenReturn(time);
        when(mockConverter.convert("Europe/London", ZoneId.class)).thenReturn(zoneId);
        // WHEN
        Calendar result = MapConversions.toCalendar(map, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(TimeZone.getTimeZone(zoneId).getID(), result.getTimeZone().getID());
        assertEquals(2021, result.get(Calendar.YEAR));
        // July -> 6 (0‑based)
        assertEquals(6, result.get(Calendar.MONTH));
        assertEquals(4, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(9, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
        verify(mockConverter).convert("2021-07-04", LocalDate.class);
        verify(mockConverter).convert("09:30:00", LocalTime.class);
        verify(mockConverter).convert("Europe/London", ZoneId.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Supplying a non‑Map as the first argument should result in a {@link ClassCastException}
     * before any interaction with the {@code Converter}.
     */
    @Test
    void testNonMapInputThrowsClassCastException() {
        // GIVEN
        String notAMap = "just a string, not a map";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toCalendar(notAMap, mockConverter), "Passing a non‑Map should cause a ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    /**
     * When the {@code Converter} throws an exception while converting {@code epochMillis},
     * the exception must propagate.
     */
    @Test
    void testEpochMillisConversionExceptionPropagates() {
        // GIVEN
        Long epoch = 1_234_567_890L;
        map.put("epochMillis", epoch);
        when(mockConverter.convert(epoch, Calendar.class)).thenThrow(new IllegalArgumentException("cannot convert epochMillis"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toCalendar(map, mockConverter));
        assertEquals("cannot convert epochMillis", ex.getMessage());
        verify(mockConverter).convert(epoch, Calendar.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * If conversion of the supplied {@code zone} value fails, the exception should bubble up.
     */
    @Test
    void testZoneConversionExceptionPropagates() {
        // GIVEN
        map.put("date", "2022-01-01");
        map.put("time", "12:00:00");
        map.put("zone", "invalid/zone");
        when(mockConverter.convert("invalid/zone", ZoneId.class)).thenThrow(new DateTimeException("Invalid zone ID"));
        when(mockConverter.convert("2022-01-01", LocalDate.class)).thenReturn(LocalDate.of(2022, 1, 1));
        when(mockConverter.convert("12:00:00", LocalTime.class)).thenReturn(LocalTime.of(12, 0, 0));
        // WHEN / THEN
        DateTimeException ex = assertThrows(DateTimeException.class, () -> MapConversions.toCalendar(map, mockConverter));
        assertEquals("Invalid zone ID", ex.getMessage());
        verify(mockConverter).convert("invalid/zone", ZoneId.class);
        // date and time conversions should not be reached because zone conversion fails first
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Propagation of a date‑parsing exception from the {@code Converter}.
     */
    @Test
    void testDateConversionExceptionPropagates() {
        // GIVEN
        map.put("date", "not-a-date");
        map.put("time", "10:15:30");
        map.put("zone", "UTC");
        when(mockConverter.convert("UTC", ZoneId.class)).thenReturn(ZoneId.of("UTC"));
        when(mockConverter.convert("not-a-date", LocalDate.class)).thenThrow(new DateTimeParseException("Text 'not-a-date' could not be parsed", "not-a-date", 0));
        when(mockConverter.convert("10:15:30", LocalTime.class)).thenReturn(LocalTime.of(10, 15, 30));
        // WHEN / THEN
        DateTimeParseException ex = assertThrows(DateTimeParseException.class, () -> MapConversions.toCalendar(map, mockConverter));
        assertTrue(ex.getMessage().contains("not-a-date"));
        verify(mockConverter).convert("not-a-date", LocalDate.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Propagation of a time‑parsing exception from the {@code Converter}.
     */
    @Test
    void testTimeConversionExceptionPropagates() {
        // GIVEN
        map.put("date", "2022-05-20");
        map.put("time", "bad-time");
        map.put("zone", "UTC");
        when(mockConverter.convert("UTC", ZoneId.class)).thenReturn(ZoneId.of("UTC"));
        when(mockConverter.convert("2022-05-20", LocalDate.class)).thenReturn(LocalDate.of(2022, 5, 20));
        when(mockConverter.convert("bad-time", LocalTime.class)).thenThrow(new DateTimeParseException("Text 'bad-time' could not be parsed", "bad-time", 0));
        // WHEN / THEN
        DateTimeParseException ex = assertThrows(DateTimeParseException.class, () -> MapConversions.toCalendar(map, mockConverter));
        assertTrue(ex.getMessage().contains("bad-time"));
        verify(mockConverter).convert("bad-time", LocalTime.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * If the {@code Converter} returns {@code null} for the date conversion,
     * {@link LocalDateTime#of} will throw a {@link NullPointerException} which must propagate.
     */
    @Test
    void testNullResultFromDateConversionThrowsNPE() {
        // GIVEN
        map.put("date", "2022-03-15");
        map.put("time", "08:00:00");
        map.put("zone", "UTC");
        when(mockConverter.convert("2022-03-15", LocalDate.class)).thenReturn(null);
        when(mockConverter.convert("08:00:00", LocalTime.class)).thenReturn(LocalTime.of(8, 0, 0));
        when(mockConverter.convert("UTC", ZoneId.class)).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toCalendar(map, mockConverter));
        verify(mockConverter).convert("2022-03-15", LocalDate.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * If the {@code Converter} returns {@code null} for the time conversion,
     * a {@link NullPointerException} should be thrown.
     */
    @Test
    void testNullResultFromTimeConversionThrowsNPE() {
        // GIVEN
        map.put("date", "2022-03-15");
        map.put("time", "08:00:00");
        map.put("zone", "UTC");
        when(mockConverter.convert("2022-03-15", LocalDate.class)).thenReturn(LocalDate.of(2022, 3, 15));
        when(mockConverter.convert("08:00:00", LocalTime.class)).thenReturn(null);
        when(mockConverter.convert("UTC", ZoneId.class)).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toCalendar(map, mockConverter));
        verify(mockConverter).convert("08:00:00", LocalTime.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * If the {@code Converter} returns {@code null} for the zone conversion,
     * a {@link NullPointerException} should be thrown.
     */
    @Test
    void testNullResultFromZoneConversionThrowsNPE() {
        // GIVEN
        map.put("date", "2022-03-15");
        map.put("time", "08:00:00");
        map.put("zone", "someZone");
        when(mockConverter.convert("2022-03-15", LocalDate.class)).thenReturn(LocalDate.of(2022, 3, 15));
        when(mockConverter.convert("08:00:00", LocalTime.class)).thenReturn(LocalTime.of(8, 0, 0));
        when(mockConverter.convert("someZone", ZoneId.class)).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toCalendar(map, mockConverter));
        verify(mockConverter).convert("someZone", ZoneId.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Time‑only branch where {@link DateUtilities#parseDate} throws an exception
     * because the time string is syntactically invalid.
     */
    @Test
    void testTimeOnlyParseDateExceptionPropagates() {
        // GIVEN
        // invalid hour/minute
        map.put("time", "25:61:00");
        map.put("zone", "UTC");
        ZoneId utc = ZoneId.of("UTC");
        when(mockConverter.convert("UTC", ZoneId.class)).thenReturn(utc);
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> MapConversions.toCalendar(map, mockConverter));
        verify(mockConverter).convert("UTC", ZoneId.class);
        verifyNoMoreInteractions(mockConverter);
    }
}
