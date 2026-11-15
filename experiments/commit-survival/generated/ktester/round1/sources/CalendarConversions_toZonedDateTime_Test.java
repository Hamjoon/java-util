package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toZonedDateTime_Test {

    /**
     * Mocked Converter – not used by the method under test.
     */
    @Mock
    private Converter converter;

    /**
     * Shared Calendar instance for the “default” time‑zone test.
     */
    private static Calendar utcCalendar;

    /**
     * Shared Calendar instance for a non‑UTC time‑zone test.
     */
    private static Calendar pstCalendar;

    /**
     * Calendar subclass whose {@code getTimeZone()} returns {@code null}.
     */
    private static final class NullTimeZoneCalendar extends Calendar {

        private final long epochMilli;

        NullTimeZoneCalendar(long epochMilli) {
            this.epochMilli = epochMilli;
        }

        @Override
        public TimeZone getTimeZone() {
            return null;
        }

        @Override
        public void setTimeZone(TimeZone tz) {
        }

        @Override
        public void setTime(Date date) {
            setTimeInMillis(date.getTime());
        }

        @Override
        public void setTimeInMillis(long millis) {
        }

        @Override
        public Date getTime() {
            return new Date(epochMilli);
        }

        @Override
        public long getTimeInMillis() {
            return epochMilli;
        }

        @Override
        public void add(int field, int amount) {
        }

        @Override
        public void roll(int field, boolean up) {
        }

        @Override
        public int get(int field) {
            return 0;
        }

        @Override
        public void set(int field, int value) {
        }

        @Override
        public int getMinimum(int field) {
            return 0;
        }

        @Override
        public int getMaximum(int field) {
            return 0;
        }

        @Override
        public int getGreatestMinimum(int field) {
            return 0;
        }

        @Override
        public int getLeastMaximum(int field) {
            return 0;
        }

        @Override
        public boolean isSet(int field) {
            return false;
        }

        @Override
        public void clear(int field) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Object clone() {
            return this;
        }
    }

    /**
     * Calendar subclass whose {@code toInstant()} throws {@link DateTimeException}.
     */
    private static final class ThrowingInstantCalendar extends Calendar {

        private final long epochMilli;

        ThrowingInstantCalendar(long epochMilli) {
            this.epochMilli = epochMilli;
        }

        @Override
        public Instant toInstant() {
            throw new DateTimeException("forced");
        }

        @Override
        public TimeZone getTimeZone() {
            return TimeZone.getTimeZone("UTC");
        }

        @Override
        public void setTimeZone(TimeZone tz) {
        }

        @Override
        public void setTime(Date date) {
            setTimeInMillis(date.getTime());
        }

        @Override
        public void setTimeInMillis(long millis) {
        }

        @Override
        public Date getTime() {
            return new Date(epochMilli);
        }

        @Override
        public long getTimeInMillis() {
            return epochMilli;
        }

        @Override
        public void add(int field, int amount) {
        }

        @Override
        public void roll(int field, boolean up) {
        }

        @Override
        public int get(int field) {
            return 0;
        }

        @Override
        public void set(int field, int value) {
        }

        @Override
        public int getMinimum(int field) {
            return 0;
        }

        @Override
        public int getMaximum(int field) {
            return 0;
        }

        @Override
        public int getGreatestMinimum(int field) {
            return 0;
        }

        @Override
        public int getLeastMaximum(int field) {
            return 0;
        }

        @Override
        public boolean isSet(int field) {
            return false;
        }

        @Override
        public void clear(int field) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Object clone() {
            return this;
        }
    }

    /**
     * TimeZone subclass whose {@code getID()} returns {@code null}.
     */
    private static final class NullIdTimeZone extends TimeZone {

        @Override
        public String getID() {
            return null;
        }

        @Override
        public void setID(String ID) {
        }

        @Override
        public int getRawOffset() {
            return 0;
        }

        @Override
        public void setRawOffset(int offsetMillis) {
        }

        @Override
        public boolean useDaylightTime() {
            return false;
        }

        @Override
        public boolean inDaylightTime(Date date) {
            return false;
        }

        @Override
        public int getDSTSavings() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NullIdTimeZone;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    /**
     * Calendar subclass that returns a TimeZone with a null ID.
     */
    private static final class CalendarWithNullIdTZ extends Calendar {

        private final long epochMilli;

        CalendarWithNullIdTZ(long epochMilli) {
            this.epochMilli = epochMilli;
        }

        @Override
        public TimeZone getTimeZone() {
            return new NullIdTimeZone();
        }

        @Override
        public void setTimeZone(TimeZone tz) {
        }

        @Override
        public void setTime(Date date) {
            setTimeInMillis(date.getTime());
        }

        @Override
        public void setTimeInMillis(long millis) {
        }

        @Override
        public Date getTime() {
            return new Date(epochMilli);
        }

        @Override
        public long getTimeInMillis() {
            return epochMilli;
        }

        @Override
        public void add(int field, int amount) {
        }

        @Override
        public void roll(int field, boolean up) {
        }

        @Override
        public int get(int field) {
            return 0;
        }

        @Override
        public void set(int field, int value) {
        }

        @Override
        public int getMinimum(int field) {
            return 0;
        }

        @Override
        public int getMaximum(int field) {
            return 0;
        }

        @Override
        public int getGreatestMinimum(int field) {
            return 0;
        }

        @Override
        public int getLeastMaximum(int field) {
            return 0;
        }

        @Override
        public boolean isSet(int field) {
            return false;
        }

        @Override
        public void clear(int field) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Object clone() {
            return this;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Calendar in UTC – epoch milli 1 600 000 000 000L (2020‑09‑13T12:26:40Z)
        utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.setTimeInMillis(1_600_000_000_000L);
        // Calendar in America/Los_Angeles (PST/PDT) – same instant as above
        pstCalendar = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        pstCalendar.setTimeInMillis(1_600_000_000_000L);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Null out static references to aid GC (optional).
        utcCalendar = null;
        pstCalendar = null;
    }

    /**
     * Creates a {@link Calendar} with the supplied zone and epoch milliseconds.
     */
    private static Calendar calendarOf(String zoneId, long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /**
     * Parses the expected {@link ZonedDateTime} from the description used in the JSON spec.
     */
    private static ZonedDateTime parseZdt(String iso) {
        // The strings in the spec are already ISO‑8601 compatible.
        return ZonedDateTime.parse(iso);
    }

    /**
     * Data for the “Valid Calendar conversion - UTC timezone” group.
     */
    private static Stream<Arguments> utcConversionProvider() {
        return Stream.of(Arguments.of(calendarOf("UTC", 0L), parseZdt("1970-01-01T00:00Z[UTC]")), Arguments.of(calendarOf("UTC", 1_600_000_000_000L), parseZdt("2020-09-13T12:26:40Z[UTC]")));
    }

    /**
     * Data for the “Valid Calendar conversion - non‑UTC timezone” group.
     */
    private static Stream<Arguments> nonUtcConversionProvider() {
        return Stream.of(Arguments.of(calendarOf("America/New_York", 1_600_000_000_000L), parseZdt("2020-09-13T08:26:40-04:00[America/New_York]")), Arguments.of(calendarOf("Asia/Tokyo", 1_600_000_000_000L), parseZdt("2020-09-13T21:26:40+09:00[Asia/Tokyo]")));
    }

    @Test
    void toZonedDateTime_ShouldReturnZonedDateTimeWithSameInstantAndZone_Utc() {
        // GIVEN
        Calendar input = utcCalendar;
        // WHEN
        ZonedDateTime result = CalendarConversions.toZonedDateTime(input, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(ZoneId.of("UTC"), result.getZone(), "Zone should be UTC");
        assertEquals(utcCalendar.toInstant(), result.toInstant(), "Instant derived from Calendar must match the ZonedDateTime instant");
    }

    @Test
    void toZonedDateTime_ShouldReturnZonedDateTimeWithSameInstantAndZone_Pacific() {
        // GIVEN
        Calendar input = pstCalendar;
        // WHEN
        ZonedDateTime result = CalendarConversions.toZonedDateTime(input, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(ZoneId.of("America/Los_Angeles"), result.getZone(), "Zone should be America/Los_Angeles");
        assertEquals(pstCalendar.toInstant(), result.toInstant(), "Instant derived from Calendar must match the ZonedDateTime instant");
    }

    @ParameterizedTest(name = "[{index}] UTC epoch {0.timeInMillis}")
    @MethodSource("utcConversionProvider")
    void toZonedDateTime_UtcConversions(Calendar input, ZonedDateTime expected) {
        ZonedDateTime result = CalendarConversions.toZonedDateTime(input, converter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Zone {0.timeZone.getID()} epoch {0.timeInMillis}")
    @MethodSource("nonUtcConversionProvider")
    void toZonedDateTime_NonUtcConversions(Calendar input, ZonedDateTime expected) {
        ZonedDateTime result = CalendarConversions.toZonedDateTime(input, converter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    /**
     * GregorianCalendar subclass test (Europe/Paris).
     */
    @Test
    void toZonedDateTime_GregorianCalendarSubclass() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris"));
        cal.setTimeInMillis(1_600_000_000_000L);
        ZonedDateTime expected = parseZdt("2020-09-13T14:26:40+02:00[Europe/Paris]");
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, result);
    }

    /**
     * Null `from` argument should raise NPE.
     */
    @Test
    void toZonedDateTime_NullFrom_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toZonedDateTime(null, converter));
    }

    /**
     * Invalid type for `from` – String.
     */
    @Test
    void toZonedDateTime_InvalidTypeString_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> CalendarConversions.toZonedDateTime("I am not a Calendar", converter));
    }

    /**
     * Invalid type for `from` – java.util.Date.
     */
    @Test
    void toZonedDateTime_InvalidTypeDate_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> CalendarConversions.toZonedDateTime(new Date(), converter));
    }

    /**
     * Non‑null converter argument (unused).
     */
    @Test
    void toZonedDateTime_NonNullConverter_Ignored() {
        Calendar cal = calendarOf("UTC", 1_600_000_000_000L);
        // 'converter' is already a Mockito mock; we just pass it.
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(parseZdt("2020-09-13T12:26:40Z[UTC]"), result);
    }

    /**
     * Negative epoch (pre‑1970 UTC).
     */
    @Test
    void toZonedDateTime_NegativeEpoch_Utc() {
        Calendar cal = calendarOf("UTC", -1_000L);
        ZonedDateTime expected = parseZdt("1969-12-31T23:59:59Z[UTC]");
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, result);
    }

    /**
     * DST transition – just before the spring‑forward (America/New_York).
     */
    @Test
    void toZonedDateTime_DstBeforeTransition() {
        // 2021‑03‑14 01:59:59‑05:00  => UTC 2021‑03‑14 06:59:59
        Calendar cal = calendarOf("America/New_York", 1_615_708_799_000L);
        ZonedDateTime expected = parseZdt("2021-03-14T01:59:59-05:00[America/New_York]");
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, result);
    }

    /**
     * DST transition – just after the spring‑forward (America/New_York).
     */
    @Test
    void toZonedDateTime_DstAfterTransition() {
        // 2021‑03‑14 03:00:00‑04:00  => UTC 2021‑03‑14 07:00:00
        Calendar cal = calendarOf("America/New_York", 1_615_708_800_000L);
        ZonedDateTime expected = parseZdt("2021-03-14T03:00:00-04:00[America/New_York]");
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, result);
    }

    /**
     * Europe/London summer offset (British Summer Time).
     */
    @Test
    void toZonedDateTime_EuropeLondonSummer() {
        Calendar cal = calendarOf("Europe/London", 1_593_561_600_000L);
        ZonedDateTime expected = parseZdt("2020-07-01T01:00:00+01:00[Europe/London]");
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, result);
    }

    /**
     * Leap day conversion (GregorianCalendar Europe/Paris).
     */
    @Test
    void toZonedDateTime_LeapDay_Paris() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris"));
        // 2000‑02‑29T00:00:00Z
        cal.setTimeInMillis(951_782_400_000L);
        ZonedDateTime expected = parseZdt("2000-02-29T01:00:00+01:00[Europe/Paris]");
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, result);
    }

    /**
     * Immutability – the Calendar must stay unchanged after conversion.
     */
    @Test
    void toZonedDateTime_DoesNotMutateCalendar() {
        Calendar cal = calendarOf("Asia/Tokyo", 1_622_505_600_000L);
        long beforeMillis = cal.getTimeInMillis();
        TimeZone beforeTz = cal.getTimeZone();
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(beforeMillis, cal.getTimeInMillis(), "Calendar epoch milli must remain unchanged");
        assertEquals(beforeTz, cal.getTimeZone(), "Calendar TimeZone must remain unchanged");
        // sanity check on the result
        assertEquals(parseZdt("2021-06-01T00:00:00+09:00[Asia/Tokyo]"), result);
    }

    /**
     * Extreme epoch value (Long.MAX_VALUE) in UTC.
     */
    @Test
    void toZonedDateTime_ExtremeEpoch_Utc() {
        Calendar cal = calendarOf("UTC", Long.MAX_VALUE);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.MAX_VALUE), ZoneId.of("UTC"));
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void toZonedDateTime_NullTimeZone_ThrowsNPE() {
        Calendar cal = new NullTimeZoneCalendar(0L);
        assertThrows(NullPointerException.class, () -> CalendarConversions.toZonedDateTime(cal, converter));
    }

    @Test
    void toZonedDateTime_ToInstantThrows_PropagatesException() {
        Calendar cal = new ThrowingInstantCalendar(0L);
        assertThrows(DateTimeException.class, () -> CalendarConversions.toZonedDateTime(cal, converter));
    }

    @Test
    void toZonedDateTime_TimeZoneWithNullId_ThrowsNPE() {
        Calendar cal = new CalendarWithNullIdTZ(0L);
        assertThrows(NullPointerException.class, () -> CalendarConversions.toZonedDateTime(cal, converter));
    }
}
