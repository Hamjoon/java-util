package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import java.lang.reflect.Field;
import java.time.format.DateTimeParseException;

@ExtendWith(MockitoExtension.class)
class MapConversions_toCalendar_Test {

    @Mock
    private Converter // mock of the Converter used by MapConversions
    converter;

    @Mock
    private ConverterOptions // mock of the options returned by converter.getOptions()
    options;

    @BeforeEach
    void setupBeforeEach() {
        // Wire the mocked Converter to return the mocked options
        when(converter.getOptions()).thenReturn(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions after each test
        reset(converter, options);
    }

    /**
     * When the source map contains the key {@code epochMillis},
     * {@code MapConversions.toCalendar} should delegate directly to
     * {@code converter.convert(epochMillis, Calendar.class)} and return that result.
     */
    @Test
    void testToCalendar_WithEpochMillis() {
        // GIVEN
        // arbitrary epoch millis
        long epoch = 1_620_000_000_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.setTimeInMillis(epoch);
        // mock the conversion that the method under test will invoke
        when(converter.convert(epoch, Calendar.class)).thenReturn(expected);
        // WHEN
        Calendar actual = MapConversions.toCalendar(map, converter);
        // THEN
        assertSame(expected, actual, "The returned Calendar should be the one produced by the Converter");
        verify(converter).convert(epoch, Calendar.class);
        verifyNoMoreInteractions(converter);
    }

    /**
     * When the source map contains both {@code date} and {@code time},
     * {@code MapConversions.toCalendar} should build a {@link Calendar}
     * using the supplied {@link LocalDate}, {@link LocalTime} and the
     * {@link ZoneId} from the ConverterOptions (or the map if provided).
     */
    @Test
    void testToCalendar_WithDateAndTime() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, Month.MAY, 10);
        // 123 ms
        LocalTime time = LocalTime.of(14, 30, 15, 123_000_000);
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, date);
        map.put(MapConversions.TIME, time);
        // No explicit zone in the map – the method will ask the options
        ZoneId zoneId = ZoneId.of("UTC");
        when(options.getZoneId()).thenReturn(zoneId);
        // The method calls converter.convert for each component
        when(converter.convert(date, LocalDate.class)).thenReturn(date);
        when(converter.convert(time, LocalTime.class)).thenReturn(time);
        // WHEN
        Calendar cal = MapConversions.toCalendar(map, converter);
        // THEN
        assertNotNull(cal, "Calendar should not be null");
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH));
        assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND));
        // Verify the expected interactions with the mock
        verify(converter).convert(date, LocalDate.class);
        verify(converter).convert(time, LocalTime.class);
        verify(converter).getOptions();
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testEpochMillisBranch() {
        // GIVEN
        long epoch = 1_622_548_800_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.setTimeInMillis(epoch);
        when(converter.convert(epoch, Calendar.class)).thenReturn(expected);
        // WHEN
        Calendar actual = MapConversions.toCalendar(map, converter);
        // THEN
        assertSame(expected, actual, "Should return the Calendar supplied by the converter");
        verify(converter).convert(epoch, Calendar.class);
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDateAndTimeWithExplicitZone() {
        // GIVEN
        LocalDate date = LocalDate.of(2021, 6, 1);
        LocalTime time = LocalTime.of(12, 30, 15, 123_000_000);
        String zoneStr = "America/New_York";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, date);
        map.put(MapConversions.TIME, time);
        map.put(MapConversions.ZONE, zoneStr);
        ZoneId zoneId = ZoneId.of(zoneStr);
        when(converter.convert(zoneStr, ZoneId.class)).thenReturn(zoneId);
        when(converter.convert(date, LocalDate.class)).thenReturn(date);
        when(converter.convert(time, LocalTime.class)).thenReturn(time);
        // WHEN
        Calendar cal = MapConversions.toCalendar(map, converter);
        // THEN
        assertNotNull(cal);
        assertEquals(2021, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND));
        assertEquals(TimeZone.getTimeZone(zoneId), cal.getTimeZone());
        verify(converter).convert(zoneStr, ZoneId.class);
        verify(converter).convert(date, LocalDate.class);
        verify(converter).convert(time, LocalTime.class);
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDateAndTimeWithDefaultZone() {
        // GIVEN
        LocalDate date = LocalDate.of(2021, 12, 31);
        LocalTime time = LocalTime.of(23, 59, 59, 999_000_000);
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, date);
        map.put(MapConversions.TIME, time);
        ZoneId defaultZone = ZoneId.of("UTC");
        when(options.getZoneId()).thenReturn(defaultZone);
        when(converter.convert(date, LocalDate.class)).thenReturn(date);
        when(converter.convert(time, LocalTime.class)).thenReturn(time);
        // WHEN
        Calendar cal = MapConversions.toCalendar(map, converter);
        // THEN
        assertNotNull(cal);
        assertEquals(2021, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        assertEquals(999, cal.get(Calendar.MILLISECOND));
        assertEquals(TimeZone.getTimeZone(defaultZone), cal.getTimeZone());
        verify(converter).getOptions();
        verify(converter).convert(date, LocalDate.class);
        verify(converter).convert(time, LocalTime.class);
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTimeOnlyStringParsing_NoZone() {
        // GIVEN
        String iso = "2021-06-01T12:30:15.123Z";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, iso);
        ZoneId defaultZone = ZoneId.of("UTC");
        when(options.getZoneId()).thenReturn(defaultZone);
        // WHEN
        Calendar cal = MapConversions.toCalendar(map, converter);
        // THEN
        assertNotNull(cal);
        long expectedEpoch = Instant.parse(iso).toEpochMilli();
        assertEquals(expectedEpoch, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone(defaultZone), cal.getTimeZone());
        verify(converter).getOptions();
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTimeOnlyStringParsing_WithZone() {
        // GIVEN
        String iso = "2021-06-01T08:30:15.123-04:00";
        String zoneStr = "America/New_York";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, iso);
        map.put(MapConversions.ZONE, zoneStr);
        ZoneId zoneId = ZoneId.of(zoneStr);
        when(converter.convert(zoneStr, ZoneId.class)).thenReturn(zoneId);
        // WHEN
        Calendar cal = MapConversions.toCalendar(map, converter);
        // THEN
        assertNotNull(cal);
        // same instant
        long expectedEpoch = Instant.parse("2021-06-01T12:30:15.123Z").toEpochMilli();
        assertEquals(expectedEpoch, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone(zoneId), cal.getTimeZone());
        verify(converter).convert(zoneStr, ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testFallbackToFromMap() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("someKey", "someValue");
        // The private fromMap method returns null in the current implementation,
        // so we assert that toCalendar also returns null.
        Calendar result = MapConversions.toCalendar(map, converter);
        assertNull(result, "When no known keys are present, toCalendar should fall back to fromMap and return its result (null here).");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testEpochMillisPrecedence() {
        // GIVEN
        long epoch = 1_609_459_200_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        map.put(MapConversions.DATE, "2021-01-01");
        map.put(MapConversions.TIME, "00:00:00");
        map.put(MapConversions.ZONE, "Europe/London");
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.setTimeInMillis(epoch);
        when(converter.convert(epoch, Calendar.class)).thenReturn(expected);
        // WHEN
        Calendar actual = MapConversions.toCalendar(map, converter);
        // THEN
        assertSame(expected, actual);
        verify(converter).convert(epoch, Calendar.class);
        verifyNoMoreInteractions(converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDateOnlyFallsBackToFromMap() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2022-02-02");
        Calendar result = MapConversions.toCalendar(map, converter);
        assertNull(result, "Only DATE present – should delegate to fromMap (null in this test).");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testNullDefaultZoneIdCausesNPE() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, LocalDate.of(2021, 12, 31));
        map.put(MapConversions.TIME, LocalTime.of(23, 59, 59, 999_000_000));
        when(options.getZoneId()).thenReturn(null);
        when(converter.convert(any(), eq(LocalDate.class))).thenReturn(LocalDate.of(2021, 12, 31));
        when(converter.convert(any(), eq(LocalTime.class))).thenReturn(LocalTime.of(23, 59, 59, 999_000_000));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toCalendar(map, converter), "A null ZoneId from options should cause NPE when creating the Calendar.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTimeOnlyNonStringCausesClassCastException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // not a String
        map.put(MapConversions.TIME, new HashMap<>());
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toCalendar(map, converter), "Non‑String TIME value should cause ClassCastException.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDateConversionExceptionPropagation() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "invalid-date");
        map.put(MapConversions.TIME, "12:00:00");
        when(converter.convert("invalid-date", LocalDate.class)).thenThrow(DateTimeParseException.class);
        when(converter.convert("12:00:00", LocalTime.class)).thenReturn(LocalTime.of(12, 0, 0));
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> MapConversions.toCalendar(map, converter), "Date conversion exception must propagate.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTimeOnlyIsoOffsetNoZone() {
        // GIVEN
        String iso = "2021-06-01T08:30:15.123-04:00";
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, iso);
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN
        Calendar cal = MapConversions.toCalendar(map, converter);
        // THEN
        assertNotNull(cal);
        long expectedEpoch = Instant.parse("2021-06-01T12:30:15.123Z").toEpochMilli();
        assertEquals(expectedEpoch, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone(ZoneId.of("UTC")), cal.getTimeZone());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testExplicitZoneEmptyTimeString() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // empty string
        map.put(MapConversions.TIME, "");
        map.put(MapConversions.ZONE, "America/Los_Angeles");
        ZoneId zoneId = ZoneId.of("America/Los_Angeles");
        when(converter.convert("America/Los_Angeles", ZoneId.class)).thenReturn(zoneId);
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> MapConversions.toCalendar(map, converter), "Parsing an empty TIME string should result in a RuntimeException from DateUtilities.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testNonMapFromArgument() {
        // GIVEN
        String notAMap = "I am not a map";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toCalendar(notAMap, converter), "Passing a non‑Map should cause a ClassCastException.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testEpochMillisConversionException() {
        // GIVEN
        long epoch = 1_622_548_800_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        when(converter.convert(epoch, Calendar.class)).thenThrow(new IllegalArgumentException("cannot convert epochMillis"));
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toCalendar(map, converter), "Exception from converter.convert should propagate.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testZoneConversionException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2021-06-01");
        map.put(MapConversions.TIME, "12:30:15");
        map.put(MapConversions.ZONE, "invalid/zone");
        when(converter.convert("invalid/zone", ZoneId.class)).thenThrow(new DateTimeException("Invalid zone ID"));
        when(converter.convert("2021-06-01", LocalDate.class)).thenReturn(LocalDate.of(2021, 6, 1));
        when(converter.convert("12:30:15", LocalTime.class)).thenReturn(LocalTime.of(12, 30, 15));
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> MapConversions.toCalendar(map, converter), "Invalid zone conversion should propagate the exception.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testDateConversionReturnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2021-06-01");
        map.put(MapConversions.TIME, "12:30:15");
        when(converter.convert("2021-06-01", LocalDate.class)).thenReturn(null);
        when(converter.convert("12:30:15", LocalTime.class)).thenReturn(LocalTime.of(12, 30, 15));
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toCalendar(map, converter), "Null LocalDate should cause NPE in LocalDateTime.of.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTimeConversionReturnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2021-06-01");
        map.put(MapConversions.TIME, "12:30:15");
        when(converter.convert("2021-06-01", LocalDate.class)).thenReturn(LocalDate.of(2021, 6, 1));
        when(converter.convert("12:30:15", LocalTime.class)).thenReturn(null);
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toCalendar(map, converter), "Null LocalTime should cause NPE in LocalDateTime.of.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testZoneConversionReturnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2021-06-01");
        map.put(MapConversions.TIME, "12:30:15");
        map.put(MapConversions.ZONE, "America/New_York");
        when(converter.convert("America/New_York", ZoneId.class)).thenReturn(null);
        when(converter.convert("2021-06-01", LocalDate.class)).thenReturn(LocalDate.of(2021, 6, 1));
        when(converter.convert("12:30:15", LocalTime.class)).thenReturn(LocalTime.of(12, 30, 15));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toCalendar(map, converter), "Null ZoneId should cause NPE when calling atZone.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTimeConversionExceptionPropagation() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2021-06-01");
        map.put(MapConversions.TIME, "invalid-time");
        when(converter.convert("2021-06-01", LocalDate.class)).thenReturn(LocalDate.of(2021, 6, 1));
        when(converter.convert("invalid-time", LocalTime.class)).thenThrow(new DateTimeParseException("Invalid time format", "invalid-time", 0));
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> MapConversions.toCalendar(map, converter), "Invalid time conversion exception must propagate.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTimeOnlyNullDefaultZoneId() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, "2021-06-01T12:30:15.123Z");
        when(options.getZoneId()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toCalendar(map, converter), "Null default ZoneId should cause NPE when creating Calendar.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testTimeOnlyParseException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, "not-a-valid-iso-date");
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> MapConversions.toCalendar(map, converter), "Unparsable TIME string should cause RuntimeException from DateUtilities.");
    }
}
