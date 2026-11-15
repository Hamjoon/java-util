package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toOffsetDateTime_Test {

    /**
     * Converter instance required by the method signature.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter does not influence the toOffsetDateTime implementation,
        // but we provide a minimal, functional instance.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper to create a {@link GregorianCalendar} with the given zone and epoch ms.
     */
    private static Calendar calendarOf(String zoneId, long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /**
     * Helper to create a {@link Calendar} with a custom raw offset (in ms).
     */
    private static Calendar calendarWithRawOffset(int rawOffsetMillis) {
        TimeZone tz = new SimpleTimeZone(rawOffsetMillis, "CUSTOM");
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(0L);
        return cal;
    }

    static Stream<Arguments> validCalendarProvider() {
        return Stream.of(// UTC at epoch 0
        Arguments.of(calendarOf("UTC", 0L), "1970-01-01T00:00Z"), // Asia/Kolkata (+05:30) at epoch 0
        Arguments.of(calendarOf("Asia/Kolkata", 0L), "1970-01-01T05:30+05:30"), // America/New_York standard time (-05:00) at epoch 0
        Arguments.of(calendarOf("America/New_York", 0L), "1969-12-31T19:00-05:00"), // America/New_York DST (-04:00) summer instant
        Arguments.of(calendarOf("America/New_York", 1593569280000L), "2020-07-01T08:00-04:00"), // Asia/Kathmandu (+05:45) at epoch 0
        Arguments.of(calendarOf("Asia/Kathmandu", 0L), "1970-01-01T05:45+05:45"));
    }

    static Stream<Arguments> boundaryDatesProvider() {
        return Stream.of(// One day before Unix epoch (UTC)
        Arguments.of(calendarOf("UTC", -86_400_000L), "1969-12-31T00:00Z"), // Far future year 3000 (UTC)
        Arguments.of(calendarOf("UTC", 32_503_680_000_000L), "3000-01-01T00:00Z"), // Millisecond precision (123 ms after epoch)
        Arguments.of(calendarOf("UTC", 123L), "1970-01-01T00:00:00.123Z"));
    }

    static Stream<Arguments> extremeTimezoneProvider() {
        return Stream.of(// Pacific/Kiritimati (+14:00) at epoch 0
        Arguments.of(calendarOf("Pacific/Kiritimati", 0L), "1970-01-01T14:00+14:00"), // Etc/GMT+12 is actually -12:00 offset (the sign is inverted in the ID)
        Arguments.of(calendarOf("Etc/GMT+12", 0L), "1969-12-31T12:00-12:00"), // America/St_Johns (Newfoundland, -03:30) at epoch 0
        Arguments.of(calendarOf("America/St_Johns", 0L), "1969-12-31T20:30-03:30"));
    }

    static Stream<Arguments> dstTransitionProvider() {
        return Stream.of(// One second before spring‑forward (NY, -05:00)
        Arguments.of(calendarOf("America/New_York", 1_615_705_199_000L), "2021-03-14T01:59:59-05:00"), // First instant after spring‑forward (NY, -04:00)
        Arguments.of(calendarOf("America/New_York", 1_615_705_200_000L), "2021-03-14T03:00-04:00"));
    }

    @Test
    void toOffsetDateTime_UtcCalendar_ReturnsCorrectOffsetDateTime() {
        // GIVEN: a Calendar set to a known instant in UTC
        // 2021‑06‑01T12:00:00Z
        long epochMilli = 1_622_548_800_000L;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        // WHEN: converting to OffsetDateTime
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, converter);
        // THEN: the result should match the instant with a UTC offset
        OffsetDateTime expected = Instant.ofEpochMilli(epochMilli).atOffset(ZoneOffset.UTC);
        assertEquals(expected, result, "OffsetDateTime should represent the same instant with UTC offset");
    }

    @Test
    void toOffsetDateTime_NonUtcCalendar_ReturnsCorrectOffsetDateTime() {
        // GIVEN: a Calendar set to the same instant but in a non‑UTC zone (America/New_York)
        // 2021‑06‑01T12:00:00Z
        long epochMilli = 1_622_548_800_000L;
        // typically UTC‑04:00 in June
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(epochMilli);
        // WHEN: converting to OffsetDateTime
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, converter);
        // THEN: the offset should reflect the zone's offset at that instant
        // includes DST
        int offsetSeconds = tz.getOffset(epochMilli) / 1000;
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        OffsetDateTime expected = Instant.ofEpochMilli(epochMilli).atOffset(expectedOffset);
        assertEquals(expected, result, "OffsetDateTime should preserve the instant and use the calendar's zone offset");
    }

    @Test
    void toOffsetDateTime_DifferentInstant_ReturnsCorrespondingOffsetDateTime() {
        // GIVEN: a Calendar with a different instant and a custom offset (Asia/Tokyo, UTC+09:00)
        // 2020‑09‑13T12:26:40Z
        long epochMilli = 1_600_000_000_000L;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.setTimeInMillis(epochMilli);
        // WHEN: converting
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, converter);
        // THEN: verify the offset and instant
        int offsetSeconds = cal.getTimeZone().getOffset(epochMilli) / 1000;
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        OffsetDateTime expected = Instant.ofEpochMilli(epochMilli).atOffset(expectedOffset);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Valid conversion – expected {1}")
    @MethodSource("validCalendarProvider")
    void toOffsetDateTime_ValidCalendars_ReturnsExpected(Calendar cal, String expectedIso) {
        // WHEN
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, converter);
        // THEN
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_NonCalendarInput_ThrowsClassCastException() {
        Object notACalendar = "Just a plain String, not a Calendar";
        assertThrows(ClassCastException.class, () -> CalendarConversions.toOffsetDateTime(notACalendar, converter));
    }

    @Test
    void toOffsetDateTime_NullFrom_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toOffsetDateTime(null, converter));
    }

    @Test
    void toOffsetDateTime_NullConverter_DoesNotAffectResult() {
        Calendar cal = calendarOf("UTC", 0L);
        // Passing null as the converter – the method never uses it.
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, null);
        OffsetDateTime expected = OffsetDateTime.parse("1970-01-01T00:00Z");
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Boundary date – expected {1}")
    @MethodSource("boundaryDatesProvider")
    void toOffsetDateTime_BoundaryDates_ReturnsExpected(Calendar cal, String expectedIso) {
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, converter);
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Extreme offset – expected {1}")
    @MethodSource("extremeTimezoneProvider")
    void toOffsetDateTime_ExtremeOffsets_ReturnsExpected(Calendar cal, String expectedIso) {
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, converter);
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] DST edge – expected {1}")
    @MethodSource("dstTransitionProvider")
    void toOffsetDateTime_DstTransitions_ReturnsExpected(Calendar cal, String expectedIso) {
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, converter);
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_OffsetBeyondMaximum_ThrowsDateTimeException() {
        // +20:00 exceeds the +18:00 limit
        Calendar cal = calendarWithRawOffset(20 * 60 * 60 * 1000);
        assertThrows(DateTimeException.class, () -> CalendarConversions.toOffsetDateTime(cal, converter));
    }

    @Test
    void toOffsetDateTime_OffsetBeyondMinimum_ThrowsDateTimeException() {
        // -19:00 exceeds the -18:00 limit
        Calendar cal = calendarWithRawOffset(-19 * 60 * 60 * 1000);
        assertThrows(DateTimeException.class, () -> CalendarConversions.toOffsetDateTime(cal, converter));
    }
}
