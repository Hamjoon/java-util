package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MapConversions_toDate_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * A mock that throws a {@link RuntimeException} on any conversion request.
     */
    @Mock
    private Converter throwingConverter;

    /**
     * One‑time set‑up for the whole test class.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Default converter – sufficient for the “identity” scenarios.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // Force UTC so that date‑only or time‑only maps have a predictable epoch.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Per‑test set‑up – currently nothing to initialise.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Configure the throwing mock to raise an exception for any conversion call.
        Mockito.when(throwingConverter.convert(any(), any())).thenThrow(new RuntimeException("Converter failure"));
    }

    private static Stream<Arguments> epochMillisSimpleProvider() {
        return Stream.of(Arguments.of(1622548800000L, 1622548800000L, "Long epoch milliseconds – 2021‑06‑01T12:00:00.000Z"), Arguments.of(1622548800123L, 1622548800123L, "Long epoch milliseconds with milliseconds part – 2021‑06‑01T12:00:00.123Z"));
    }

    private static Stream<Arguments> epochMillisNanosProvider() {
        return Stream.of(// Instant.ofEpochSecond(1622548800, 500_000_000) → 1622548800 s + 0.5 s = 1622548800500 ms
        Arguments.of(Instant.ofEpochSecond(1622548800L, 500_000_000L), 1622548800500L, "Instant with 500 ms nanos"), // Instant.ofEpochSecond(0, 999_999_999) → 0 ms + 999 ms (floor) = 999 ms
        Arguments.of(Instant.ofEpochSecond(0L, 999_999_999L), 999L, "Instant just below one millisecond"));
    }

    /**
     * Verify that {@code MapConversions.toDate} correctly converts a map that contains an
     * {@code epochMillis} entry (the fast path). The resulting {@link Date} should represent
     * the same instant as the supplied epoch milliseconds.
     */
    @Test
    void toDate_usesEpochMillis_whenPresent() {
        // GIVEN – a map with an explicit epochMillis value (2021‑01‑01T00:00:00Z)
        long epochMillis = 1_609_459_200_000L;
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.EPOCH_MILLIS, epochMillis);
        // WHEN
        Date result = MapConversions.toDate(source, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(epochMillis, result.getTime(), "Date should represent the exact epoch milliseconds supplied in the map");
    }

    /**
     * Verify that {@code MapConversions.toDate} falls back to the generic map‑conversion
     * logic when {@code epochMillis} is not present.  The map supplies a {@code date}
     * string that the {@link Converter} can parse.  The resulting {@link Date} should be
     * equivalent to the parsed value.
     */
    @Test
    void toDate_usesFromMap_whenEpochMillisMissing() {
        // GIVEN – a map with a date string (ISO‑8601 format)
        String isoDate = "2023-03-15";
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.DATE, isoDate);
        // WHEN
        Date result = MapConversions.toDate(source, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        // The Converter parses the ISO date as midnight in the default time‑zone.
        // We compare the formatted value to avoid time‑zone dependent millisecond checks.
        String expected = isoDate + " 00:00:00";
        String actual = DateUtilities.formatDate(result, "yyyy-MM-dd HH:mm:ss");
        assertEquals(expected, actual, "Date parsed from the map should correspond to the supplied ISO‑date string");
    }

    /**
     * Verify that a {@code null} source returns {@code null}.  This exercises the
     * early‑return path where {@code toEpochMillis} returns {@code null} and
     * {@code fromMap} also yields {@code null}.
     */
    @Test
    void toDate_returnsNull_whenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = MapConversions.toDate(source, converter);
        // THEN
        assertNull(result, "Converting a null source should produce a null Date");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("epochMillisSimpleProvider")
    void toDate_epochMillisSimple(Object source, long expectedMillis, String description) {
        Date result = MapConversions.toDate(source, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedMillis, result.getTime(), "Date epoch millis should match the expected value");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("epochMillisNanosProvider")
    void toDate_epochMillisWithNanos(Object source, long expectedMillis, String description) {
        Date result = MapConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "Date should incorporate nanoseconds as whole milliseconds");
    }

    @Test
    void toDate_mapWithEpochMillisKey() {
        Map<String, Object> map = Map.of(MapConversions.EPOCH_MILLIS, 1622548800000L);
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        assertEquals(1622548800000L, result.getTime());
    }

    @Test
    void toDate_mapDateTimeZone() {
        Map<String, Object> map = Map.of(MapConversions.DATE, "2021-06-01", MapConversions.TIME, "12:30:45", MapConversions.ZONE + MapConversions.OPTIONAL, "UTC");
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        ZonedDateTime zdt = ZonedDateTime.of(LocalDate.of(2021, 6, 1), LocalTime.of(12, 30, 45), ZoneOffset.UTC);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime());
    }

    @Test
    void toDate_mapDateTimeNoZone() {
        Map<String, Object> map = Map.of(MapConversions.DATE, "2021-12-31", MapConversions.TIME, "23:59:59");
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        // System default zone is forced to UTC in @BeforeAll
        ZonedDateTime zdt = ZonedDateTime.of(LocalDate.of(2021, 12, 31), LocalTime.of(23, 59, 59), ZoneOffset.UTC);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime());
    }

    @Test
    void toDate_mapTimeOnlyWithZone() {
        Map<String, Object> map = Map.of(MapConversions.TIME, "00:00:01", MapConversions.ZONE + MapConversions.OPTIONAL, "UTC");
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        ZonedDateTime zdt = ZonedDateTime.of(// 1970‑01‑01
        LocalDate.ofEpochDay(0), LocalTime.of(0, 0, 1), ZoneOffset.UTC);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime());
    }

    @Test
    void toDate_mapTimeOnlyNoZone() {
        Map<String, Object> map = Map.of(MapConversions.TIME, "00:00:02");
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        ZonedDateTime zdt = ZonedDateTime.of(LocalDate.ofEpochDay(0), LocalTime.of(0, 0, 2), // default zone forced to UTC
        ZoneOffset.UTC);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime());
    }

    @Test
    void toDate_mapEpochMillisString() {
        Map<String, Object> map = Map.of(MapConversions.EPOCH_MILLIS, "1622548800000");
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        assertEquals(1622548800000L, result.getTime());
    }

    @Test
    void toDate_mapDateOnly() {
        Map<String, Object> map = Map.of(MapConversions.DATE, "2021-01-01");
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        ZonedDateTime zdt = ZonedDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime());
    }

    @Test
    void toDate_mapDateWithOptionalZone() {
        Map<String, Object> map = Map.of(MapConversions.DATE, "2021-01-01", MapConversions.ZONE + MapConversions.OPTIONAL, "UTC");
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        ZonedDateTime zdt = ZonedDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime());
    }

    @Test
    void toDate_directDateInstance() {
        Date source = new Date(1622548800123L);
        Date result = MapConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(1622548800123L, result.getTime());
    }

    @Test
    void toDate_directTimestampWithNanos() {
        Timestamp ts = Timestamp.valueOf("2021-06-01 12:00:00.123456789");
        // Expected epoch: 1622548800123 ms + 456 ms (nanoseconds/1_000_000)
        long expected = 1622548800123L + 456L;
        Date result = MapConversions.toDate(ts, converter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "Timestamp nanos should be added as whole milliseconds");
    }

    @Test
    void toDate_negativeEpoch() {
        // 1969‑12‑31T00:00:00Z
        long epoch = -86_400_000L;
        Date result = MapConversions.toDate(epoch, converter);
        assertNotNull(result);
        assertEquals(epoch, result.getTime());
    }

    @Test
    void toDate_extremeFutureEpoch() {
        long epoch = Long.MAX_VALUE;
        Date result = MapConversions.toDate(epoch, converter);
        assertNotNull(result);
        assertEquals(epoch, result.getTime(), "Date should retain Long.MAX_VALUE without overflow");
    }

    @Test
    void toDate_nullSourceReturnsNull() {
        Date result = MapConversions.toDate(null, converter);
        assertNull(result);
    }

    @Test
    void toDate_unsupportedStringInput() {
        assertThrows(RuntimeException.class, () -> MapConversions.toDate("not a date", converter), "Non‑map, non‑numeric input should cause a conversion exception");
    }

    @Test
    void toDate_nullConverterThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toDate(1622548800000L, null), "Passing a null Converter must result in NPE");
    }

    @Test
    void toDate_converterThrowsRuntimeException() {
        assertThrows(RuntimeException.class, () -> MapConversions.toDate("1622548800000", throwingConverter), "Exception from the supplied Converter should propagate");
    }

    @Test
    void toDate_mapWithUnsupportedKeys() {
        Map<String, Object> map = Map.of("foo", "bar");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDate(map, converter), "Map lacking any recognised key set must cause IllegalArgumentException");
    }

    @Test
    void toDate_invalidEpochMillisString() {
        Map<String, Object> map = Map.of(MapConversions.EPOCH_MILLIS, "not-a-number");
        assertThrows(NumberFormatException.class, () -> MapConversions.toDate(map, converter), "Non‑numeric epochMillis string should raise NumberFormatException");
    }

    @Test
    void toDate_invalidDateFormat() {
        Map<String, Object> map = Map.of(MapConversions.DATE, "2021/06/01", MapConversions.TIME, "12:00:00");
        assertThrows(DateTimeParseException.class, () -> MapConversions.toDate(map, converter), "Malformed date string must trigger DateTimeParseException");
    }

    @Test
    void toDate_invalidTimeFormat() {
        Map<String, Object> map = Map.of(MapConversions.DATE, "2021-06-01", MapConversions.TIME, "25:61:61");
        assertThrows(DateTimeParseException.class, () -> MapConversions.toDate(map, converter), "Malformed time string must trigger DateTimeParseException");
    }

    @Test
    void toDate_invalidZoneIdentifier() {
        Map<String, Object> map = Map.of(MapConversions.DATE, "2021-06-01", MapConversions.TIME, "12:00:00", MapConversions.ZONE + MapConversions.OPTIONAL, "NotAZone");
        assertThrows(ZoneRulesException.class, () -> MapConversions.toDate(map, converter), "Unknown zone identifier must raise ZoneRulesException");
    }

    @Test
    void toDate_nullValueForRequiredKey() {
        Map<String, Object> map = Map.of(MapConversions.DATE, null, MapConversions.TIME, "12:00:00");
        assertThrows(NullPointerException.class, () -> MapConversions.toDate(map, converter), "Null value for a required key should cause NPE");
    }

    @Test
    void toDate_nonNumericEpochMillisDouble() {
        Map<String, Object> map = Map.of(MapConversions.EPOCH_MILLIS, 1622548800.123);
        assertThrows(ClassCastException.class, () -> MapConversions.toDate(map, converter), "Double epochMillis should result in ClassCastException");
    }

    @Test
    void toDate_epochMillisStringCannotParse() {
        Map<String, Object> map = Map.of(MapConversions.EPOCH_MILLIS, "16225488ABC");
        assertThrows(NumberFormatException.class, () -> MapConversions.toDate(map, converter), "Malformed numeric string should raise NumberFormatException");
    }

    @Test
    void toDate_customStringToLongConverter() {
        // The default Converter already parses numeric strings, so we reuse it.
        Date result = MapConversions.toDate("1622548800000", converter);
        assertNotNull(result);
        assertEquals(1622548800000L, result.getTime(), "String representing epoch millis should be converted to Date");
    }
}
