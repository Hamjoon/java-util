package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toSqlDate_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for the simple epoch‑millis path
     * ------------------------------------------------------------- */
    private static Stream<Arguments> simpleEpochSources() {
        // 2023‑01‑01T00:00:00Z
        long epoch = 1_672_531_200_000L;
        return Stream.of(Arguments.of(new Date(epoch), epoch, "java.util.Date"), Arguments.of(epoch, epoch, "Long epoch millis"), Arguments.of(Instant.ofEpochMilli(epoch), epoch, "Instant"), Arguments.of(Timestamp.valueOf("2023-01-01 00:00:00.000"), epoch, "Timestamp"));
    }

    @Test
    void toSqlDate_fromDate_returnsSameInstant() {
        // GIVEN
        // 2021‑09‑30T12:00:00Z
        long epochMillis = 1_632_072_800_000L;
        Date source = new Date(epochMillis);
        // WHEN
        Date result = MapConversions.toSqlDate(source, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(epochMillis, result.getTime(), "Date produced from a java.util.Date should have the same epoch milliseconds");
    }

    @Test
    void toSqlDate_fromInstant_returnsCorrespondingDate() {
        // GIVEN
        long epochMillis = 1_632_072_800_000L;
        Instant source = Instant.ofEpochMilli(epochMillis);
        // WHEN
        Date result = MapConversions.toSqlDate(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMillis, result.getTime(), "Date produced from an Instant should match the Instant's epoch milliseconds");
    }

    @Test
    void toSqlDate_fromMapWithEpochMillis_returnsDate() {
        // GIVEN
        long epochMillis = 1_632_072_800_000L;
        Map<String, Object> map = new CompactLinkedMap<>();
        // the key name expected by toEpochMillis
        map.put("epochMillis", epochMillis);
        // WHEN
        Date result = MapConversions.toSqlDate(map, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMillis, result.getTime(), "Date produced from a map containing 'epochMillis' should reflect that value");
    }

    @ParameterizedTest(name = "[{index}] from {2}")
    @MethodSource("simpleEpochSources")
    void toSqlDate_simpleEpochSources(Object source, long expectedMillis, String description) {
        // WHEN
        Date result = MapConversions.toSqlDate(source, converter);
        // THEN
        assertNotNull(result, "Result should never be null for a valid source");
        assertEquals(expectedMillis, result.getTime(), () -> "Converted Date from " + description + " should match the expected epoch millis");
    }

    /* -------------------------------------------------------------
     *  Map with date, time and optional zone
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_fromMapWithDateTimeAndZone_returnsDate() {
        // 2023‑01‑01T12:34:56Z
        long expected = Instant.parse("2023-01-01T12:34:56Z").toEpochMilli();
        Map<String, Object> map = Map.of("date", "2023-01-01", "time", "12:34:56", "zone", "UTC");
        Date result = MapConversions.toSqlDate(map, converter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "Map with date, time and zone should be interpreted as the corresponding instant");
    }

    /* -------------------------------------------------------------
     *  Map with only time (optional zone omitted)
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_fromMapWithTimeOnlyAndOptionalZone_returnsDateAtToday() {
        // Build the expected instant: today (system default zone) at 23:59:59
        LocalDate today = LocalDate.now();
        LocalTime time = LocalTime.of(23, 59, 59);
        ZonedDateTime zdt = ZonedDateTime.of(today, time, ZoneId.systemDefault());
        long expected = zdt.toInstant().toEpochMilli();
        Map<String, Object> map = Map.of("time", "23:59:59");
        Date result = MapConversions.toSqlDate(map, converter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "Map with only time should combine with today's date in the default JVM zone");
    }

    /* -------------------------------------------------------------
     *  Null source input
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_nullInput_returnsNull() {
        Date result = MapConversions.toSqlDate(null, converter);
        assertNull(result, "A null source should result in a null return value");
    }

    /* -------------------------------------------------------------
     *  Invalid map structures – missing required keys
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_invalidMapStructure_throwsIllegalArgumentException() {
        Map<String, Object> map = Map.of("foo", "bar");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toSqlDate(map, converter), "Map without any recognised key set should cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Null converter argument
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_nullConverter_throwsNullPointerException() {
        Date source = new Date(1_672_531_200_000L);
        assertThrows(NullPointerException.class, () -> MapConversions.toSqlDate(source, null), "Passing a null Converter should result in NPE");
    }

    /* -------------------------------------------------------------
     *  Map with epochMillis of wrong type
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_invalidEpochMillisType_throwsIllegalArgumentException() {
        Map<String, Object> map = Map.of("epochMillis", "notANumber");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toSqlDate(map, converter), "Non‑numeric epochMillis should cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Map with malformed date string
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_invalidDateFormat_throwsDateTimeParseException() {
        Map<String, Object> map = Map.of(// invalid month
        "date", // invalid month
        "2023-13-01", "time", "12:00:00");
        assertThrows(DateTimeParseException.class, () -> MapConversions.toSqlDate(map, converter), "Invalid date format should propagate DateTimeParseException");
    }

    /* -------------------------------------------------------------
     *  Map with malformed time string
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_invalidTimeFormat_throwsDateTimeParseException() {
        Map<String, Object> map = Map.of("date", "2023-01-01", // invalid time
        "time", // invalid time
        "25:61:61");
        assertThrows(DateTimeParseException.class, () -> MapConversions.toSqlDate(map, converter), "Invalid time format should propagate DateTimeParseException");
    }

    /* -------------------------------------------------------------
     *  Map with an unknown zone identifier
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_invalidZone_throwsDateTimeException() {
        Map<String, Object> map = Map.of("date", "2023-01-01", "time", "12:00:00", "zone", "Not/A_Zone");
        assertThrows(DateTimeException.class, () -> MapConversions.toSqlDate(map, converter), "Unknown zone ID should result in DateTimeException");
    }
}
