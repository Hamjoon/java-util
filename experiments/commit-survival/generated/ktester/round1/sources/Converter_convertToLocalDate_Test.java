package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(MockitoExtension.class)
class Converter_convertToLocalDate_Test {

    @Test
    void convertFromLocalDate_returnsSameInstance() {
        // given
        LocalDate original = LocalDate.of(2023, 5, 17);
        // when
        LocalDate result = Converter.convertToLocalDate(original);
        // then
        assertEquals(original, result, "A LocalDate should be returned unchanged");
    }

    @Test
    void convertFromLocalDateTime_truncatesToDate() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 15, 30, 45);
        // when
        LocalDate result = Converter.convertToLocalDate(ldt);
        // then
        assertEquals(LocalDate.of(2023, 5, 17), result, "LocalDateTime should be truncated to its date component");
    }

    @Test
    void convertFromUtilDate() {
        // given a java.util.Date representing 2023‑05‑17 00:00:00 in the system default zone
        Date date = Date.from(LocalDate.of(2023, 5, 17).atStartOfDay(ZoneId.systemDefault()).toInstant());
        // when
        LocalDate result = Converter.convertToLocalDate(date);
        // then
        assertEquals(LocalDate.of(2023, 5, 17), result, "java.util.Date should be converted to the corresponding LocalDate");
    }

    @Test
    void convertFromSqlTimestamp() {
        // given
        Timestamp ts = Timestamp.valueOf("2023-05-17 12:34:56.789");
        // when
        LocalDate result = Converter.convertToLocalDate(ts);
        // then
        assertEquals(LocalDate.of(2023, 5, 17), result, "SQL Timestamp should be converted to the date part only");
    }

    @Test
    void convertFromStringIsoFormat() {
        // given
        String iso = "2023-05-17";
        // when
        LocalDate result = Converter.convertToLocalDate(iso);
        // then
        assertEquals(LocalDate.of(2023, 5, 17), result, "ISO‑8601 date string should be parsed correctly");
    }

    @Test
    void convertFromStringWithTimePortion() {
        // given
        String isoDateTime = "2023-05-17T15:45:30";
        // when
        LocalDate result = Converter.convertToLocalDate(isoDateTime);
        // then
        // The time portion should be ignored/truncated
        assertEquals(LocalDate.of(2023, 5, 17), result, "String containing a time portion should be truncated to the date");
    }

    @Test
    void convertFromNullReturnsNull() {
        // given
        Object input = null;
        // when
        LocalDate result = Converter.convertToLocalDate(input);
        // then
        assertNull(result, "Null input must result in a null output");
    }

    @Test
    void convertUnsupportedTypeThrowsException() {
        // given
        Object unsupported = new Object();
        // when / then
        assertThrows(RuntimeException.class, () -> Converter.convertToLocalDate(unsupported), "Unsupported source types should trigger a conversion exception");
    }

    @ParameterizedTest(name = "Invalid date string \"{0}\" should cause a conversion exception")
    @ValueSource(strings = { // completely non‑date text
    "not-a-date", // impossible calendar date
    "2023-02-30", // unsupported separator format
    "2023/01/01" })
    void convertFromInvalidStringThrowsException(String invalidInput) {
        // when / then
        assertThrows(RuntimeException.class, () -> Converter.convertToLocalDate(invalidInput), () -> "Input \"" + invalidInput + "\" should not be parsable to a LocalDate");
    }
}
