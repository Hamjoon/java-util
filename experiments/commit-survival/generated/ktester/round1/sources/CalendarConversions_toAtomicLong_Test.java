package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toAtomicLong_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Calendar referenceCalendar;

    private static long expectedEpochMilli;

    class BadDate extends Date {

        @Override
        public long getTime() {
            throw new IllegalStateException("date failure");
        }
    }

    class BadDate extends Date {

        @Override
        public long getTime() {
            throw new IllegalStateException("date failure");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // deterministic UTC calendar for reusable reference values
        referenceCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 2020‑09‑13T12:26:40Z
        referenceCalendar.setTimeInMillis(1_600_000_000_000L);
        expectedEpochMilli = referenceCalendar.getTimeInMillis();
    }

    private static Stream<Arguments> validCalendarProvider() {
        // 2021‑05‑03T10:00:00Z  → 1620036000000
        Calendar utc1 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc1.setTimeInMillis(1620036000000L);
        // epoch start → 0
        Calendar utc2 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc2.setTimeInMillis(0L);
        return Stream.of(Arguments.of(utc1, 1620036000000L), Arguments.of(utc2, 0L));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(// String
        Arguments.of("2021-05-03"), // java.util.Date (not Calendar)
        Arguments.of(new Date()));
    }

    @Test
    void toAtomicLong_returnsAtomicLongWithCorrectValue() {
        // given
        Object from = referenceCalendar;
        // when
        AtomicLong result = CalendarConversions.toAtomicLong(from, mockConverter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(expectedEpochMilli, result.get(), "AtomicLong value should match the Calendar's epoch milliseconds");
    }

    @Test
    void toAtomicLong_nullInput_throwsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CalendarConversions.toAtomicLong(from, mockConverter), "Passing null should result in NullPointerException");
    }

    @Test
    void toAtomicLong_invalidType_throwsClassCastException() {
        // given
        Object from = "not a calendar";
        // when / then
        assertThrows(ClassCastException.class, () -> CalendarConversions.toAtomicLong(from, mockConverter), "Passing a non‑Calendar object should result in ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Group: Valid Calendar conversion
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} ⇒ epoch={1}")
    @MethodSource("validCalendarProvider")
    void toAtomicLong_validCalendar_returnsCorrectAtomicLong(Calendar calendar, long expected) {
        // given
        Object from = calendar;
        // when
        AtomicLong result = CalendarConversions.toAtomicLong(from, mockConverter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(expected, result.get(), "AtomicLong value must match the Calendar's epoch milliseconds");
    }

    /* --------------------------------------------------------------
     *  Group: Converter argument usage (no effect)
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_converterIgnored_evenIfMockThrows() {
        // a mock that throws on any method call – the method under test must never touch it
        Converter hostileConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter must not be used");
        });
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 2022‑01‑01T12:34:56Z
        calendar.setTimeInMillis(1641033296000L);
        // should succeed without any exception from the hostile mock
        AtomicLong result = CalendarConversions.toAtomicLong(calendar, hostileConverter);
        assertEquals(1641033296000L, result.get(), "AtomicLong value must equal the calendar's epoch milliseconds");
    }

    /* --------------------------------------------------------------
     *  Group: Pre‑epoch Calendar conversion
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_preEpochCalendar_returnsNegativeAtomicLong() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 1965‑12‑31T23:59:59Z
        cal.setTimeInMillis(-157_766_401_000L);
        AtomicLong result = CalendarConversions.toAtomicLong(cal, mockConverter);
        assertEquals(-157_766_401_000L, result.get(), "AtomicLong must contain the negative epoch milliseconds");
    }

    /* --------------------------------------------------------------
     *  Group: Timezone handling
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_timezoneNonUTC_correctEpochMillis() {
        // Calendar in GMT+02:00 representing 2021‑05‑03T12:00:00+02:00
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        Calendar cal = new GregorianCalendar(tz);
        cal.set(2021, Calendar.MAY, 3, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Expected epoch is the same instant as 2021‑05‑03T10:00:00Z
        long expected = 1620036000000L;
        AtomicLong result = CalendarConversions.toAtomicLong(cal, mockConverter);
        assertEquals(expected, result.get(), "Epoch milliseconds must be calculated in UTC regardless of the Calendar's time‑zone");
    }

    /* --------------------------------------------------------------
     *  Group: Millisecond precision
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_preservesMillisecondComponent() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 2021‑05‑03T10:00:00.123Z
        cal.setTimeInMillis(1620036000123L);
        AtomicLong result = CalendarConversions.toAtomicLong(cal, mockConverter);
        assertEquals(1620036000123L, result.get(), "AtomicLong must retain the full millisecond precision");
    }

    /* --------------------------------------------------------------
     *  Group: Calendar getTime() returns null
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_calendarGetTimeReturnsNull_throwsNullPointerException() {
        // Mockito mock that returns null for getTime()
        Calendar mockCal = mock(Calendar.class);
        when(mockCal.getTime()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CalendarConversions.toAtomicLong(mockCal, mockConverter), "If Calendar#getTime() returns null, a NullPointerException must be thrown");
    }

    /* --------------------------------------------------------------
     *  Group: Non‑null Converter ignored
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_nonNullConverter_hasNoEffect() {
        // Use a real (non‑mock) Converter instance if available; otherwise a simple mock works.
        // no stubbing – method does not use it
        Converter realConverter = mock(Converter.class);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 2022‑12‑31T23:59:59Z
        cal.setTimeInMillis(1672531199000L);
        AtomicLong result = CalendarConversions.toAtomicLong(cal, realConverter);
        assertEquals(1672531199000L, result.get(), "Result must be correct even when a non‑null Converter is supplied");
    }

    /* --------------------------------------------------------------
     *  Group: Calendar getTime() throws exception
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_calendarGetTimeThrowsException_propagates() {
        Calendar mockCal = mock(Calendar.class);
        when(mockCal.getTime()).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CalendarConversions.toAtomicLong(mockCal, mockConverter), "RuntimeException from Calendar#getTime() must propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  Group: Date getTime() throws exception
     * -------------------------------------------------------------- */
    @Test
    void toAtomicLong_dateGetTimeThrowsException_propagates() {
        // Date subclass that throws when its getTime() method is called
        class BadDate extends Date {

            @Override
            public long getTime() {
                throw new IllegalStateException("date failure");
            }
        }
        Calendar mockCal = mock(Calendar.class);
        when(mockCal.getTime()).thenReturn(new BadDate());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CalendarConversions.toAtomicLong(mockCal, mockConverter), "Exception from the Date returned by Calendar#getTime() must propagate");
        assertEquals("date failure", ex.getMessage());
    }
}
