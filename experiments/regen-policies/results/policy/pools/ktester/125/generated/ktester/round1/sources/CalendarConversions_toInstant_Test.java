package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toInstant_Test {

    /**
     * Converter instance required by the method signature (not used internally).
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter constructor requires a ConverterOptions instance.
        converter = new Converter(new ConverterOptions());
    }

    static Stream<Arguments> validCalendarProvider() {
        // 2023-01-01T12:34:56.789Z  => epoch milli 1672576496789L
        long epochMilli = 1672576496789L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        Calendar utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(epochMilli);
        Calendar laCal = new GregorianCalendar(TimeZone.getTimeZone("America/Los_Angeles"));
        // 2023‑01‑01T04:34:56.789‑08:00 is the same instant as above
        laCal.set(2023, Calendar.JANUARY, 1, 4, 34, 56);
        laCal.set(Calendar.MILLISECOND, 789);
        // Ensure the underlying epoch millis matches the expected instant
        laCal.setTimeInMillis(epochMilli);
        // default zone
        Calendar defaultCal = new GregorianCalendar();
        // epoch 0
        defaultCal.setTimeInMillis(0L);
        return Stream.of(Arguments.of("UTC GregorianCalendar with known epoch millis", utcCal, instant), Arguments.of("Los_Angeles GregorianCalendar representing same instant", laCal, instant), Arguments.of("Default‑zone GregorianCalendar at epoch 0", defaultCal, Instant.EPOCH));
    }

    static Stream<Arguments> boundaryEpochProvider() {
        return Stream.of(Arguments.of("Maximum positive epoch milli (Long.MAX_VALUE)", Long.MAX_VALUE, Instant.ofEpochMilli(Long.MAX_VALUE)), Arguments.of("Minimum (most negative) epoch milli (Long.MIN_VALUE)", Long.MIN_VALUE, Instant.ofEpochMilli(Long.MIN_VALUE)));
    }

    @Test
    void toInstant_returnsExactInstant_forGregorianCalendarInUTC() {
        // GIVEN
        Instant expectedInstant = Instant.parse("2023-01-01T12:34:56Z");
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(expectedInstant.toEpochMilli());
        // WHEN
        Instant result = CalendarConversions.toInstant(calendar, converter);
        // THEN
        assertEquals(expectedInstant, result, "The Instant returned by toInstant should match the Calendar's epoch milli value.");
    }

    @Test
    void toInstant_respectsCalendarTimeZone() {
        // GIVEN
        // Calendar set to a non‑UTC zone (e.g., America/New_York). The instant should be the same
        // absolute point in time regardless of the zone.
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar calendar = new GregorianCalendar(tz);
        // 2023‑03‑15T08:00:00-04:00 (EDT) => 2023‑03‑15T12:00:00Z
        calendar.set(2023, Calendar.MARCH, 15, 8, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Instant expectedInstant = Instant.ofEpochMilli(calendar.getTimeInMillis());
        // WHEN
        Instant result = CalendarConversions.toInstant(calendar, converter);
        // THEN
        assertEquals(expectedInstant, result, "Instant should represent the same absolute moment irrespective of the Calendar's time zone.");
    }

    @Test
    void toInstant_throwsNullPointerException_whenCalendarIsNull() {
        // GIVEN
        Calendar nullCalendar = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toInstant(nullCalendar, converter), "Passing a null Calendar should result in a NullPointerException.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validCalendarProvider")
    void toInstant_validCalendars(String description, Calendar calendar, Instant expected) {
        // WHEN
        Instant result = CalendarConversions.toInstant(calendar, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    @Test
    void toInstant_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toInstant(null, converter), "Passing a null Calendar should result in a NullPointerException.");
    }

    @Test
    void toInstant_nonCalendar_fromDate_throwsClassCastException() {
        Date date = new Date();
        assertThrows(ClassCastException.class, () -> CalendarConversions.toInstant(date, converter), "Passing a Date (not a Calendar) should throw ClassCastException.");
    }

    @Test
    void toInstant_nonCalendar_fromString_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> CalendarConversions.toInstant("just a string", converter), "Passing a String (not a Calendar) should throw ClassCastException.");
    }

    @Test
    void toInstant_ignoresConverterEvenIfItThrows() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(123456789L);
        // Mock Converter that throws on any method call.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("forced failure");
        });
        // The method should never touch the converter, so the instant is still returned.
        Instant result = CalendarConversions.toInstant(cal, throwingConverter);
        assertEquals(Instant.ofEpochMilli(123456789L), result, "Converter should be ignored; the correct Instant must be returned.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("boundaryEpochProvider")
    void toInstant_boundaryEpochs(String description, long epochMillis, Instant expected) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        Instant result = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void toInstant_dstFallBackAmbiguousTime() {
        // America/New_York switches from EDT (UTC‑4) to EST (UTC‑5) at 2:00 local time on 2021‑11‑07.
        // 2021‑11‑07 01:30 occurs twice; Calendar resolves to the earlier (EDT) offset.
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        cal.set(2021, Calendar.NOVEMBER, 7, 1, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Calendar will interpret this as the earlier offset (UTC‑4)
        // 2021‑11‑07T05:30:00Z
        long expectedEpoch = 1636266600000L;
        Instant expected = Instant.ofEpochMilli(expectedEpoch);
        Instant result = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, result, "During the DST fall‑back hour the Calendar should resolve to the earlier offset.");
    }

    @Test
    void toInstant_customCalendarOverridesToInstant() {
        Calendar custom = new Calendar() {

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public void roll(int field, boolean up) {
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
            public void clear() {
            }

            @Override
            public void clear(int field) {
            }

            @Override
            public void set(int field, int value) {
            }

            @Override
            public int get(int field) {
                return 0;
            }

            @Override
            public boolean isSet(int field) {
                return false;
            }

            @Override
            public void setTime(Date date) {
            }

            @Override
            public Date getTime() {
                return new Date(0);
            }

            @Override
            public void setTimeInMillis(long millis) {
            }

            @Override
            public long getTimeInMillis() {
                return 0L;
            }

            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Calendar setZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant toInstant() {
                return Instant.EPOCH;
            }
        };
        Instant result = CalendarConversions.toInstant(custom, converter);
        assertEquals(Instant.EPOCH, result, "Custom Calendar's overridden toInstant() should be returned.");
    }

    @Test
    void toInstant_calendarWithOffsetZoneAndEpochZero_returnsEpoch() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"));
        // epoch 0
        cal.setTimeInMillis(0L);
        Instant result = CalendarConversions.toInstant(cal, converter);
        assertEquals(Instant.EPOCH, result, "Regardless of the Calendar's zone, epoch‑millis 0 must map to Instant.EPOCH.");
    }

    @Test
    void toInstant_strictCalendarInvalidDate_throwsException() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        // February 30th is invalid.
        cal.set(2023, Calendar.FEBRUARY, 30, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Calendar.toInstant() throws IllegalArgumentException for an invalid date.
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toInstant(cal, converter), "Non‑lenient Calendar with impossible date should throw IllegalArgumentException.");
    }

    @Test
    void toInstant_overflowEpoch_throwsDateTimeException() {
        // Instant.MAX is about 31556889864403199L; add 1 to overflow.
        long overflow = 31556889864403200L;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(overflow);
        assertThrows(DateTimeException.class, () -> CalendarConversions.toInstant(cal, converter), "Overflowing the supported Instant range must result in DateTimeException.");
    }

    @Test
    void toInstant_customCalendarToInstantThrowsRuntimeException() {
        Calendar throwingCal = new Calendar() {

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public void roll(int field, boolean up) {
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
            public void clear() {
            }

            @Override
            public void clear(int field) {
            }

            @Override
            public void set(int field, int value) {
            }

            @Override
            public int get(int field) {
                return 0;
            }

            @Override
            public boolean isSet(int field) {
                return false;
            }

            @Override
            public void setTime(Date date) {
            }

            @Override
            public Date getTime() {
                return new Date();
            }

            @Override
            public void setTimeInMillis(long millis) {
            }

            @Override
            public long getTimeInMillis() {
                return 0L;
            }

            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Calendar setZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant toInstant() {
                throw new RuntimeException("forced failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CalendarConversions.toInstant(throwingCal, converter), "Exception from overridden toInstant() must propagate.");
        assertEquals("forced failure", ex.getMessage());
    }

    @Test
    void toInstant_customCalendarToInstantReturnsNull() {
        Calendar nullReturningCal = new Calendar() {

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public void roll(int field, boolean up) {
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
            public void clear() {
            }

            @Override
            public void clear(int field) {
            }

            @Override
            public void set(int field, int value) {
            }

            @Override
            public int get(int field) {
                return 0;
            }

            @Override
            public boolean isSet(int field) {
                return false;
            }

            @Override
            public void setTime(Date date) {
            }

            @Override
            public Date getTime() {
                return new Date();
            }

            @Override
            public void setTimeInMillis(long millis) {
            }

            @Override
            public long getTimeInMillis() {
                return 0L;
            }

            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Calendar setZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant toInstant() {
                return null;
            }
        };
        Instant result = CalendarConversions.toInstant(nullReturningCal, converter);
        assertNull(result, "If the Calendar's toInstant() returns null, the method should return null.");
    }
}
