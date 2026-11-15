package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toTimestamp_Test {

    @Mock
    private Converter // Converter is not used by the method, but required by signature
    mockConverter;

    private static Stream<Arguments> provideSuccessfulCalendars() {
        // 1. Typical date‑time with milliseconds
        Calendar c1 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c1.set(2023, Calendar.MARCH, 15, 10, 20, 30);
        c1.set(Calendar.MILLISECOND, 123);
        long e1 = c1.getTimeInMillis();
        // 2. Unix epoch (0)
        Calendar c2 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c2.setTimeInMillis(0L);
        long e2 = 0L;
        // 3. Time‑zone handling – America/New_York (DST in March 2023, offset -4)
        Calendar c3 = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        c3.set(2023, Calendar.MARCH, 15, 10, 20, 30);
        c3.set(Calendar.MILLISECOND, 123);
        // absolute instant, independent of zone
        long e3 = c3.getTimeInMillis();
        // 4. Negative epoch (pre‑1970)
        Calendar c4 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c4.setTimeInMillis(-1L);
        long e4 = -1L;
        // 5. Maximum long value
        Calendar c5 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c5.setTimeInMillis(Long.MAX_VALUE);
        long e5 = Long.MAX_VALUE;
        // 6. Millisecond‑precision edge (999 ms)
        Calendar c6 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c6.set(2023, Calendar.MARCH, 15, 10, 20, 30);
        c6.set(Calendar.MILLISECOND, 999);
        long e6 = c6.getTimeInMillis();
        return Stream.of(Arguments.of("Typical date‑time with millis", c1, e1), Arguments.of("Unix epoch (0)", c2, e2), Arguments.of("Timezone handling – America/New_York", c3, e3), Arguments.of("Negative epoch (pre‑1970)", c4, e4), Arguments.of("Maximum long epoch", c5, e5), Arguments.of("Millisecond precision edge (999 ms)", c6, e6));
    }

    @Test
    void toTimestamp_WithValidCalendar_ReturnsTimestampWithSameEpochMillis() {
        // GIVEN – a Calendar set to a known instant (UTC)
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(2023, Calendar.JANUARY, 15, 10, 20, 30);
        calendar.set(Calendar.MILLISECOND, 123);
        long expectedMillis = calendar.getTimeInMillis();
        // WHEN – converting to Timestamp
        Timestamp ts = CalendarConversions.toTimestamp(calendar, mockConverter);
        // THEN – the Timestamp reflects the same epoch milliseconds
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(expectedMillis, ts.getTime(), "Timestamp epoch milliseconds must match the Calendar's timeInMillis");
    }

    @Test
    void toTimestamp_WithNullCalendar_ThrowsNullPointerException() {
        // GIVEN – a null Calendar reference
        Calendar nullCalendar = null;
        // WHEN / THEN – invoking the method should throw NPE
        assertThrows(NullPointerException.class, () -> CalendarConversions.toTimestamp(nullCalendar, mockConverter), "Passing null as the Calendar should result in NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Successful conversions – parameterized tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => description={0}")
    @MethodSource("provideSuccessfulCalendars")
    void toTimestamp_SuccessfulConversion(String description, Calendar calendar, long expectedEpochMillis) {
        // WHEN – convert the calendar to a Timestamp
        Timestamp ts = CalendarConversions.toTimestamp(calendar, mockConverter);
        // THEN – the Timestamp must contain exactly the same epoch milliseconds
        assertNotNull(ts, "Timestamp should never be null");
        assertEquals(expectedEpochMillis, ts.getTime(), "Timestamp epoch millis must match the Calendar's timeInMillis");
    }

    /* -------------------------------------------------------------
     *  Null input – expect NPE
     * ------------------------------------------------------------- */
    @Test
    void toTimestamp_NullCalendar_ThrowsNullPointerException() {
        // GIVEN a null reference
        Calendar nullCal = null;
        // WHEN / THEN – the method should throw NPE because it dereferences the argument
        assertThrows(NullPointerException.class, () -> CalendarConversions.toTimestamp(nullCal, mockConverter), "Passing null as the Calendar should result in NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Invalid type – expect ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void toTimestamp_InvalidType_ThrowsClassCastException() {
        // GIVEN an object that is not a Calendar
        Object notACalendar = "2023-03-15";
        // WHEN / THEN – the cast inside the method should fail
        assertThrows(ClassCastException.class, () -> CalendarConversions.toTimestamp(notACalendar, mockConverter), "Providing a non‑Calendar object must cause a ClassCastException");
    }
}
