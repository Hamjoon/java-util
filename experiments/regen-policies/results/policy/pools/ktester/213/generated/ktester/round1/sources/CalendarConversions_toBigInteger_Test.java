package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toBigInteger_Test {

    /**
     * Shared Converter instance – not used by the method under test but required by the signature.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Default options are sufficient – the method does not depend on any Converter behaviour.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Create a {@link Calendar} with the supplied fields in the given time‑zone.
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are zero‑based, so subtract 1
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    @Test
    void testCalendarToBigInteger_Tokyo() {
        Calendar cal = createCalendar(2023, 6, 15, 12, 30, 45, 123, TimeZone.getTimeZone("Asia/Tokyo"));
        BigInteger expected = BigInteger.valueOf(cal.getTimeInMillis() * 1_000_000L);
        assertEquals(expected, CalendarConversions.toBigInteger(cal, converter));
    }

    @Test
    void testCalendarToBigInteger_Paris() {
        Calendar cal = createCalendar(1969, 12, 31, 23, 59, 59, 999, TimeZone.getTimeZone("Europe/Paris"));
        BigInteger expected = BigInteger.valueOf(cal.getTimeInMillis() * 1_000_000L);
        assertEquals(expected, CalendarConversions.toBigInteger(cal, converter));
    }

    @Test
    void testCalendarToBigInteger_NewYork() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, TimeZone.getTimeZone("America/New_York"));
        BigInteger expected = BigInteger.valueOf(cal.getTimeInMillis() * 1_000_000L);
        assertEquals(expected, CalendarConversions.toBigInteger(cal, converter));
    }

    @Test
    void testCalendarToBigInteger_EpochZero() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.setTimeInMillis(0L);
        BigInteger expected = BigInteger.valueOf(0L);
        assertEquals(expected, CalendarConversions.toBigInteger(cal, converter));
    }

    /**
     * {@code from} is {@code null}. The implementation casts the argument to {@link Calendar}
     * and immediately dereferences it, so a {@link NullPointerException} is expected.
     *
     * The {@code converter} argument is also {@code null} in this test case to reflect the
     * JSON‑driven scenario, but it does not affect the outcome.
     */
    @Test
    void testToBigInteger_NullFrom() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toBigInteger(null, null));
    }
}
