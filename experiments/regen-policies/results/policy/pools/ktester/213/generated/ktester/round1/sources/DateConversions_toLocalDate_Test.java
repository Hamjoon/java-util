package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLocalDate_Test {

    /**
     * Converter used for all test cases – created once for the whole test suite.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not need any special options for the built‑in date/time conversions.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    void toLocalDate_fromLocalDate_returnsSameInstance() {
        // When the source is already a LocalDate, the method should simply return it.
        LocalDate expected = LocalDate.of(2023, Month.MARCH, 15);
        LocalDate actual = DateConversions.toLocalDate(expected, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromLocalDateTime_truncatesTime() {
        // LocalDateTime should be truncated to its date component.
        LocalDateTime ldt = LocalDateTime.of(2022, 12, 31, 23, 45, 12);
        LocalDate expected = ldt.toLocalDate();
        LocalDate actual = DateConversions.toLocalDate(ldt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromZonedDateTime_truncatesTime() {
        // ZonedDateTime should be truncated to its date component, preserving the zone.
        ZonedDateTime zdt = ZonedDateTime.of(2021, 6, 5, 8, 30, 0, 0, ZoneId.of("UTC"));
        LocalDate expected = zdt.toLocalDate();
        LocalDate actual = DateConversions.toLocalDate(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromDate_convertsCorrectly() {
        // Date representing 2020‑01‑02 10:15:30 in the system default zone
        ZonedDateTime zdt = ZonedDateTime.of(2020, 1, 2, 10, 15, 30, 0, ZoneId.systemDefault());
        Date date = Date.from(zdt.toInstant());
        LocalDate expected = zdt.toLocalDate();
        LocalDate actual = DateConversions.toLocalDate(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromTimestamp_convertsCorrectly() {
        // java.sql.Timestamp should be converted via its LocalDateTime representation.
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2019, 11, 20, 5, 0, 0));
        LocalDate expected = ts.toLocalDateTime().toLocalDate();
        LocalDate actual = DateConversions.toLocalDate(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromInstant_convertsCorrectly() {
        Instant instant = Instant.parse("2024-04-01T12:34:56Z");
        // Convert using the system default zone for the expected LocalDate
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate actual = DateConversions.toLocalDate(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromCalendar_convertsCorrectly() {
        // Calendar (with UTC zone) should yield the correct LocalDate.
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2018, Calendar.JULY, 4, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        LocalDate expected = LocalDate.of(2018, Month.JULY, 4);
        LocalDate actual = DateConversions.toLocalDate(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_fromMapWithTimeKey_convertsCorrectly() {
        // A map containing a "time" entry (epoch milliseconds) should be interpreted as a date.
        long epochMilli = LocalDate.of(2022, 2, 2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("time", epochMilli);
        LocalDate expected = LocalDate.of(2022, 2, 2);
        LocalDate actual = DateConversions.toLocalDate(map, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDate_nullFrom_throwsNullPointerException() {
        // According to the specification, passing a null source should result in NPE.
        assertThrows(NullPointerException.class, () -> DateConversions.toLocalDate(null, converter), "Expected NullPointerException when 'from' argument is null");
    }
}
