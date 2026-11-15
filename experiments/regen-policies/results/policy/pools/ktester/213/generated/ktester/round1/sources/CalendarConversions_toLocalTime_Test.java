package com.cedarsoftware.util.convert;

import java.util.Calendar;
import java.util.TimeZone;
import java.sql.Timestamp;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toLocalTime_Test {

    private Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1 / UTC+2 (DST)
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5 / UTC‑4 (DST)
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – this mirrors the behaviour of the existing test suite
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper to create a Calendar with the given fields (month is 1‑based for callers).
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Temporarily force the JVM default time‑zone to {@code zoneId}.
     */
    private static void withSystemDefault(ZoneId zoneId, Runnable block) {
        String original = System.getProperty("user.timezone");
        try {
            System.setProperty("user.timezone", zoneId.getId());
            // Re‑initialise the cache used by ZoneId.systemDefault()
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
            block.run();
        } finally {
            System.setProperty("user.timezone", original);
            TimeZone.setDefault(TimeZone.getTimeZone(original));
        }
    }

    private static Stream<Arguments> directTimeTypes() {
        return Stream.of(// LocalTime → identity
        Arguments.of(LocalTime.of(13, 45, 30), null, LocalTime.of(13, 45, 30)), // LocalDateTime → time component (system default zone, deterministic by forcing UTC)
        Arguments.of(LocalDateTime.of(2023, 5, 10, 9, 15, 0), null, LocalTime.of(9, 15, 0)), // ZonedDateTime → same local time
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 22, 30, 45, 0, ZoneId.of("Europe/Paris")), null, LocalTime.of(22, 30, 45)), // OffsetDateTime → same local time
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 6, 0, 0, 0, ZoneOffset.ofHours(-5)), null, LocalTime.of(6, 0, 0)));
    }

    private static Stream<Arguments> legacyDateTypes() {
        return Stream.of(// java.util.Date (epoch millis) → 12:34:56 UTC
        Arguments.of(Date.from(Instant.parse("2023-01-01T12:34:56Z")), null, LocalTime.of(12, 34, 56)), // java.sql.Timestamp → 23:59:59 UTC
        Arguments.of(Timestamp.from(Instant.parse("2023-01-01T23:59:59Z")), null, LocalTime.of(23, 59, 59)), // Calendar in UTC → 08:20:15
        Arguments.of(createCalendar(2023, 3, 15, 8, 20, 15, 0, TimeZone.getTimeZone("UTC")), null, LocalTime.of(8, 20, 15)));
    }

    private static Stream<Arguments> numericAndStringConversions() {
        return Stream.of(// Long epoch millis → 14:00:00 UTC
        Arguments.of(Long.valueOf(Instant.parse("2023-04-01T14:00:00Z").toEpochMilli()), null, LocalTime.of(14, 0, 0)), // Simple time string
        Arguments.of("15:45:30", null, LocalTime.of(15, 45, 30)), // ISO‑8601 datetime with offset → 05:30:00 (offset ignored)
        Arguments.of("2023-07-20T05:30:00+02:00", null, LocalTime.of(5, 30, 0)));
    }

    private static Stream<Arguments> epochEdgeCases() {
        return Stream.of(// 0L → midnight UTC
        Arguments.of(Long.valueOf(0L), null, LocalTime.of(0, 0, 0)), // Instant one day before epoch → midnight UTC
        Arguments.of(Instant.ofEpochMilli(-86_400_000L), null, LocalTime.of(0, 0, 0)));
    }

    private static Stream<Arguments> alternativeStringFormats() {
        return Stream.of(// HH:mm → seconds default to 0
        Arguments.of("08:05", LocalTime.of(8, 5, 0)), // HH:mm:ss.SSS → fraction discarded
        Arguments.of("08:05:30.123", LocalTime.of(8, 5, 30)), // ISO‑8601 instant with Z
        Arguments.of("2023-12-31T23:59:59Z", LocalTime.of(23, 59, 59)), // Space‑separated datetime
        Arguments.of("2023-12-31 23:59:59", LocalTime.of(23, 59, 59)));
    }

    private static Stream<Arguments> otherNumericTypes() {
        // 1672531199000 ms = 2023‑01‑01T00:59:59Z
        return Stream.of(Arguments.of(new BigDecimal("1672531199000"), LocalTime.of(0, 59, 59)), Arguments.of(new AtomicLong(1672531199000L), LocalTime.of(0, 59, 59)), Arguments.of(new BigInteger("1672531199000"), LocalTime.of(0, 59, 59)));
    }

    private static Stream<Arguments> temporalNoTime() {
        return Stream.of(Arguments.of(LocalDate.of(2023, 5, 10), LocalTime.of(0, 0, 0)), Arguments.of(YearMonth.of(2023, 5), LocalTime.of(0, 0, 0)), Arguments.of(Year.of(2023), LocalTime.of(0, 0, 0)), // MonthDay maps to current year – we fix the year to 2023 for repeatability
        Arguments.of(MonthDay.of(5, 10), LocalTime.of(0, 0, 0)));
    }

    private static Stream<Arguments> malformedStrings() {
        return Stream.of(Arguments.of(""), Arguments.of("not-a-time"), Arguments.of("25:00:00"));
    }

    private static Stream<Arguments> unsupportedNumeric() {
        return Stream.of(Arguments.of(Double.valueOf(12345.67)), Arguments.of(Integer.valueOf(1672531199)), Arguments.of(Short.valueOf((short) 12345)), Arguments.of(Byte.valueOf((byte) 7)));
    }

    private static Stream<Arguments> malformedStringVariations() {
        return Stream.of(// missing colon in offset
        Arguments.of("2023-07-20T05:30:00+0200"), // trailing garbage
        Arguments.of("15:45:30XYZ"), // invalid month
        Arguments.of("2023-13-01T00:00:00"));
    }

    @Test
    void calendarToLocalTime_Tokyo() {
        Calendar cal = createCalendar(2023, 6, 15, 13, 45, 30, 0, TOKYO);
        // Calendar is interpreted in its own zone, so the LocalTime should be 13:45:30
        LocalTime expected = LocalTime.of(13, 45, 30);
        assertEquals(expected, CalendarConversions.toLocalTime(cal, converter));
    }

    @Test
    void calendarToLocalTime_Paris() {
        Calendar cal = createCalendar(2023, 12, 31, 23, 59, 59, 999, PARIS);
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        assertEquals(expected, CalendarConversions.toLocalTime(cal, converter));
    }

    @Test
    void calendarToLocalTime_NewYork_DaylightSaving() {
        // 2023‑03‑12 02:30 does not exist in New York (DST start). Calendar will adjust to 03:30.
        Calendar cal = createCalendar(2023, 3, 12, 2, 30, 0, 0, NEW_YORK);
        // After the adjustment the hour becomes 3.
        LocalTime expected = LocalTime.of(3, 30, 0);
        assertEquals(expected, CalendarConversions.toLocalTime(cal, converter));
    }

    @Test
    void localTimeIsReturnedUnchanged() {
        LocalTime src = LocalTime.of(8, 15, 42, 123_456_789);
        assertEquals(src, CalendarConversions.toLocalTime(src, converter));
    }

    @Test
    void dateToLocalTime() {
        // 2023‑06‑15T13:45:30.250 in UTC
        Instant instant = Instant.parse("2023-06-15T13:45:30.250Z");
        Date date = Date.from(instant);
        // Converter will treat Date as an instant in the system default zone.
        // To make the test deterministic we force the default zone to UTC.
        ZoneId original = ZoneId.systemDefault();
        try {
            System.setProperty("user.timezone", "UTC");
            LocalTime expected = LocalTime.of(13, 45, 30, 250_000_000);
            assertEquals(expected, CalendarConversions.toLocalTime(date, converter));
        } finally {
            System.setProperty("user.timezone", original.getId());
        }
    }

    @Test
    void timestampToLocalTime() {
        Timestamp ts = Timestamp.valueOf("2023-06-15 07:20:45.987654321");
        // Timestamp is interpreted as a local date‑time without zone.
        // The conversion uses the system default zone – we fix it to UTC for repeatability.
        ZoneId original = ZoneId.systemDefault();
        try {
            System.setProperty("user.timezone", "UTC");
            LocalTime expected = LocalTime.of(7, 20, 45, 987_654_321);
            assertEquals(expected, CalendarConversions.toLocalTime(ts, converter));
        } finally {
            System.setProperty("user.timezone", original.getId());
        }
    }

    @Test
    void instantToLocalTime() {
        Instant instant = Instant.parse("2023-06-15T04:12:59.001Z");
        // With system default zone set to UTC, the LocalTime should be 04:12:59.001
        ZoneId original = ZoneId.systemDefault();
        try {
            System.setProperty("user.timezone", "UTC");
            LocalTime expected = LocalTime.of(4, 12, 59, 1_000_000);
            assertEquals(expected, CalendarConversions.toLocalTime(instant, converter));
        } finally {
            System.setProperty("user.timezone", original.getId());
        }
    }

    @Test
    void localDateTimeToLocalTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, 6, 15, 22, 10, 5, 555_000_000);
        // No zone information – conversion yields the same time component.
        LocalTime expected = LocalTime.of(22, 10, 5, 555_000_000);
        assertEquals(expected, CalendarConversions.toLocalTime(ldt, converter));
    }

    @Test
    void offsetDateTimeToLocalTime() {
        OffsetDateTime odt = OffsetDateTime.parse("2023-06-15T09:30:15+02:00");
        // The offset is ignored for the LocalTime component.
        LocalTime expected = LocalTime.of(9, 30, 15);
        assertEquals(expected, CalendarConversions.toLocalTime(odt, converter));
    }

    @Test
    void zonedDateTimeToLocalTime() {
        ZonedDateTime zdt = ZonedDateTime.parse("2023-06-15T18:45:00+09:00[Asia/Tokyo]");
        LocalTime expected = LocalTime.of(18, 45, 0);
        assertEquals(expected, CalendarConversions.toLocalTime(zdt, converter));
    }

    @Test
    void isoStringToLocalTime() {
        String iso = "12:34:56.789";
        LocalTime expected = LocalTime.of(12, 34, 56, 789_000_000);
        assertEquals(expected, CalendarConversions.toLocalTime(iso, converter));
    }

    @Test
    void isoDateTimeStringToLocalTime() {
        // Full ISO date‑time; only the time part is relevant.
        String iso = "2023-06-15T23:59:01";
        LocalTime expected = LocalTime.of(23, 59, 1);
        assertEquals(expected, CalendarConversions.toLocalTime(iso, converter));
    }

    @ParameterizedTest(name = "[{index}] direct type {0}")
    @MethodSource("directTimeTypes")
    void testDirectTimeTypes(Object source, Converter conv, LocalTime expected) {
        // Force UTC to make the conversion deterministic for LocalDateTime (no zone)
        withSystemDefault(ZoneOffset.UTC, () -> {
            Converter used = conv != null ? conv : converter;
            assertEquals(expected, CalendarConversions.toLocalTime(source, used));
        });
    }

    @ParameterizedTest(name = "[{index}] legacy type {0}")
    @MethodSource("legacyDateTypes")
    void testLegacyDateTypes(Object source, Converter conv, LocalTime expected) {
        withSystemDefault(ZoneOffset.UTC, () -> {
            Converter used = conv != null ? conv : converter;
            assertEquals(expected, CalendarConversions.toLocalTime(source, used));
        });
    }

    @ParameterizedTest(name = "[{index}] numeric/string {0}")
    @MethodSource("numericAndStringConversions")
    void testNumericAndStringConversions(Object source, Converter conv, LocalTime expected) {
        withSystemDefault(ZoneOffset.UTC, () -> {
            Converter used = conv != null ? conv : converter;
            assertEquals(expected, CalendarConversions.toLocalTime(source, used));
        });
    }

    @ParameterizedTest(name = "[{index}] epoch edge {0}")
    @MethodSource("epochEdgeCases")
    void testEpochEdgeCases(Object source, Converter conv, LocalTime expected) {
        withSystemDefault(ZoneOffset.UTC, () -> {
            Converter used = conv != null ? conv : converter;
            assertEquals(expected, CalendarConversions.toLocalTime(source, used));
        });
    }

    @ParameterizedTest(name = "[{index}] alt string {0}")
    @MethodSource("alternativeStringFormats")
    void testAlternativeStringFormats(String source, LocalTime expected) {
        withSystemDefault(ZoneOffset.UTC, () -> {
            assertEquals(expected, CalendarConversions.toLocalTime(source, converter));
        });
    }

    @ParameterizedTest(name = "[{index}] other numeric {0}")
    @MethodSource("otherNumericTypes")
    void testOtherNumericTypes(Object source, LocalTime expected) {
        withSystemDefault(ZoneOffset.UTC, () -> {
            assertEquals(expected, CalendarConversions.toLocalTime(source, converter));
        });
    }

    @ParameterizedTest(name = "[{index}] temporal no‑time {0}")
    @MethodSource("temporalNoTime")
    void testTemporalNoTime(Object source, LocalTime expected) {
        // The conversion uses the system default zone; we force UTC for determinism.
        withSystemDefault(ZoneOffset.UTC, () -> {
            assertEquals(expected, CalendarConversions.toLocalTime(source, converter));
        });
    }

    @Test
    void nullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLocalTime(null, converter));
    }

    @Test
    void unsupportedObjectThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalTime(new Object(), converter));
    }

    @ParameterizedTest(name = "[{index}] malformed string \"{0}\"")
    @MethodSource("malformedStrings")
    void testMalformedStrings(String source) {
        assertThrows(DateTimeParseException.class, () -> CalendarConversions.toLocalTime(source, converter));
    }

    @ParameterizedTest(name = "[{index}] unsupported numeric {0}")
    @MethodSource("unsupportedNumeric")
    void testUnsupportedNumeric(Object source) {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalTime(source, converter));
    }

    @Test
    void offsetTimeNotSupported() {
        OffsetTime ot = OffsetTime.of(12, 30, 45, 0, ZoneOffset.ofHours(2));
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalTime(ot, converter));
    }

    @Test
    void instantNotSupportedDirectly() {
        // The library expects epoch‑millis handling, not raw Instant.
        Instant instant = Instant.parse("2023-08-01T12:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalTime(instant, converter));
    }

    @Test
    void stringBuilderNotSupported() {
        StringBuilder sb = new StringBuilder("12:34:56");
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalTime(sb, converter));
    }

    @Test
    void atomicReferenceNotSupported() {
        java.util.concurrent.atomic.AtomicReference<String> ref = new java.util.concurrent.atomic.AtomicReference<>("08:00:00");
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalTime(ref, converter));
    }

    @ParameterizedTest(name = "[{index}] malformed variation \"{0}\"")
    @MethodSource("malformedStringVariations")
    void testMalformedStringVariations(String source) {
        assertThrows(DateTimeParseException.class, () -> CalendarConversions.toLocalTime(source, converter));
    }

    @Test
    void nullSourceWithCustomConverterThrowsNPE() {
        Converter custom = new Converter(new DefaultConverterOptions()) {

            @Override
            public Object convert(Object o, Class<?> c) {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLocalTime(null, custom));
    }
}
