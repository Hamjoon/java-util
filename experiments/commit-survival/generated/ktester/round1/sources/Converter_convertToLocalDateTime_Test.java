package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToLocalDateTime_Test {

    private static Stream<Arguments> successfulConversionProvider() {
        // Helper constants
        // 2023-05-15T10:20:30 (UTC)
        long epochMilli = 1684135230000L;
        ZoneId systemZone = ZoneId.systemDefault();
        // Build each source object according to the JSON spec
        LocalDateTime ldt = LocalDateTime.parse("2023-05-15T10:20:30");
        LocalDate ld = LocalDate.of(2023, 5, 15);
        ZonedDateTime zdt = ZonedDateTime.parse("2023-05-15T10:20:30+02:00[Europe/Paris]");
        Timestamp ts = new Timestamp(1684135230123L);
        ts.setNanos(123456789);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        // 2023-05-15 midnight UTC
        java.sql.Date sqlDate = new java.sql.Date(1684108800000L);
        Instant instant = Instant.ofEpochMilli(epochMilli);
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2023-05-15T10:20:30+02:00");
        // Expected values (system default zone is used for Date/Instant conversions)
        LocalDateTime expectedFromDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), systemZone);
        LocalDateTime expectedFromInstant = LocalDateTime.ofInstant(instant, systemZone);
        LocalDateTime expectedFromCalendar = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), systemZone);
        LocalDateTime expectedFromSqlDate = LocalDateTime.ofInstant(sqlDate.toInstant(), systemZone);
        LocalDateTime expectedFromOffsetDateTimeString = LocalDateTime.parse("2023-05-15T10:20:30");
        return Stream.of(// Null source
        Arguments.of("Null source should result in null", null, null), // Already a LocalDateTime
        Arguments.of("LocalDateTime instance should be returned unchanged", ldt, ldt), // java.util.Date
        Arguments.of("java.util.Date should be converted using system default zone", new Date(epochMilli), expectedFromDate), // java.sql.Timestamp with nanos
        Arguments.of("java.sql.Timestamp must preserve nanosecond precision", ts, LocalDateTime.of(2023, 5, 15, 10, 20, 30, 123456789)), // java.time.LocalDate
        Arguments.of("LocalDate should become start of day", ld, ld.atStartOfDay()), // java.time.ZonedDateTime (zone ignored)
        Arguments.of("ZonedDateTime should be converted to its local component", zdt, zdt.toLocalDateTime()), // ISO‑8601 LocalDateTime string
        Arguments.of("ISO‑8601 string should be parsed correctly", "2023-05-15T10:20:30", LocalDateTime.parse("2023-05-15T10:20:30")), // ISO‑8601 LocalDate string
        Arguments.of("ISO‑8601 date string should be parsed as midnight", "2023-05-15", LocalDate.parse("2023-05-15").atStartOfDay()), // java.util.Calendar (UTC)
        Arguments.of("Calendar (UTC) should be converted to equivalent LocalDateTime", cal, expectedFromCalendar), // java.sql.Date (date only)
        Arguments.of("java.sql.Date should become midnight of that date", sqlDate, expectedFromSqlDate), // java.time.Instant
        Arguments.of("Instant should be converted using system default zone", instant, expectedFromInstant), // OffsetDateTime string (offset ignored)
        Arguments.of("OffsetDateTime string should be parsed and offset ignored", "2023-05-15T10:20:30+02:00", expectedFromOffsetDateTimeString), // ISO‑8601 string with nanoseconds
        Arguments.of("String with nanosecond fraction should preserve full precision", "2023-05-15T10:20:30.123456789", LocalDateTime.parse("2023-05-15T10:20:30.123456789")));
    }

    private static Stream<Arguments> failureConversionProvider() {
        // Objects that are not supported as sources
        LocalTime localTime = LocalTime.parse("10:20:30");
        OffsetDateTime offsetDateTimeObj = OffsetDateTime.parse("2023-05-15T10:20:30+02:00");
        OffsetTime offsetTime = OffsetTime.parse("10:20:30+02:00");
        YearMonth yearMonth = YearMonth.parse("2023-05");
        MonthDay monthDay = MonthDay.parse("--05-15");
        // 10:20:30 UTC
        Time sqlTime = new Time(37230000L);
        BigDecimal bigDecimal = new BigDecimal("123.45");
        Optional<LocalDateTime> optional = Optional.of(LocalDateTime.parse("2023-05-15T10:20:30"));
        int[] intArray = new int[] { 2023, 5, 15 };
        return Stream.of(// Unsupported numeric type
        Arguments.of("Integer source should cause IllegalArgumentException", 12345, IllegalArgumentException.class), // Unparseable date string
        Arguments.of("Invalid format string should cause DateTimeParseException", "15/05/2023 10:20", DateTimeParseException.class), // String with space instead of 'T'
        Arguments.of("String with space should cause DateTimeParseException", "2023-05-15 10:20:30", DateTimeParseException.class), // Empty string
        Arguments.of("Empty string should cause DateTimeParseException", "", DateTimeParseException.class), // Whitespace‑only string
        Arguments.of("Whitespace only string should cause DateTimeParseException", "   ", DateTimeParseException.class), // Numeric‑only string
        Arguments.of("Numeric‑only string should cause DateTimeParseException", "20230515", DateTimeParseException.class), // LocalTime object (no date component)
        Arguments.of("LocalTime should cause IllegalArgumentException", localTime, IllegalArgumentException.class), // Numeric zero (treated as unsupported)
        Arguments.of("Zero (Long) should cause IllegalArgumentException", 0L, IllegalArgumentException.class), // OffsetDateTime object (unsupported source type)
        Arguments.of("OffsetDateTime object should cause IllegalArgumentException", offsetDateTimeObj, IllegalArgumentException.class), // OffsetTime object (unsupported source type)
        Arguments.of("OffsetTime should cause IllegalArgumentException", offsetTime, IllegalArgumentException.class), // YearMonth object (unsupported source type)
        Arguments.of("YearMonth should cause IllegalArgumentException", yearMonth, IllegalArgumentException.class), // MonthDay object (unsupported source type)
        Arguments.of("MonthDay should cause IllegalArgumentException", monthDay, IllegalArgumentException.class), // java.sql.Time (time‑only)
        Arguments.of("java.sql.Time should cause IllegalArgumentException", sqlTime, IllegalArgumentException.class), // BigDecimal (numeric wrapper)
        Arguments.of("BigDecimal should cause IllegalArgumentException", bigDecimal, IllegalArgumentException.class), // Optional wrapper
        Arguments.of("Optional should cause IllegalArgumentException", optional, IllegalArgumentException.class), // Array type
        Arguments.of("Array source should cause IllegalArgumentException", intArray, IllegalArgumentException.class));
    }

    @Test
    void convertFromLocalDateTime_returnsSameInstance() {
        // given
        LocalDateTime now = LocalDateTime.now();
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(now);
        // then
        assertNotNull(result);
        assertEquals(now, result);
    }

    @Test
    void convertFromDate_returnsEquivalentLocalDateTime() {
        // given
        // current instant
        Date date = new Date();
        LocalDateTime expected = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(date);
        // then
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void convertFromTimestamp_returnsEquivalentLocalDateTime() {
        // given
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        LocalDateTime expected = ts.toLocalDateTime();
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(ts);
        // then
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void convertFromString_parsesISODateTime() {
        // given
        String iso = "2023-08-15T13:45:30";
        LocalDateTime expected = LocalDateTime.parse(iso);
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(iso);
        // then
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void convertFromLocalDate_returnsStartOfDay() {
        // given
        LocalDate date = LocalDate.of(2022, 12, 31);
        LocalDateTime expected = date.atStartOfDay();
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(date);
        // then
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void convertFromZonedDateTime_preservesInstant() {
        // given
        ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("UTC"));
        LocalDateTime expected = zdt.toLocalDateTime();
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(zdt);
        // then
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void convertFromNull_returnsNull() {
        // given
        Object source = null;
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(source);
        // then
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void testSuccessfulConversions(String description, Object source, LocalDateTime expected) {
        // when
        LocalDateTime result = Converter.convertToLocalDateTime(source);
        // then
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("failureConversionProvider")
    void testFailureConversions(String description, Object source, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> Converter.convertToLocalDateTime(source), description);
    }
}
