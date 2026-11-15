package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

class CalendarConversions_toLocalTime_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the default options are sufficient for our tests
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> positiveConversionProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        // Helper to build expected LocalTime from an Instant
        java.util.function.Function<Instant, LocalTime> instantToLocalTime = i -> ZonedDateTime.ofInstant(i, systemZone).toLocalTime();
        // 1. LocalTime (should be returned unchanged)
        LocalTime lt = LocalTime.of(13, 45, 30);
        // 2. LocalDateTime
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 9, 15, 0);
        // 3. ZonedDateTime (UTC)
        ZonedDateTime zdtUtc = ZonedDateTime.of(2023, 5, 10, 22, 30, 45, 0, ZoneId.of("UTC"));
        // 4. Date (Instant)
        Instant instantDate = Instant.parse("2023-05-10T12:00:00Z");
        Date date = Date.from(instantDate);
        // 5. Timestamp
        Timestamp ts = Timestamp.valueOf("2023-05-10 18:20:15");
        // 6. Long epoch milli
        // 12.345 seconds after epoch
        Long epochMilli = 12345L;
        // 7. BigDecimal epoch milli
        BigDecimal bd = BigDecimal.valueOf(12345);
        // 8. OffsetDateTime (offset ignored after conversion)
        OffsetDateTime odt = OffsetDateTime.parse("2023-05-10T07:45:30+02:00");
        // 9. Instant (system zone)
        Instant instant = Instant.parse("2023-05-10T23:59:59Z");
        // 10. String representation of time
        String timeString = "13:45:30";
        // 11. BigInteger epoch milli (exactly one day after epoch)
        BigInteger bi = BigInteger.valueOf(86_400_000L);
        // 12. Calendar with explicit zone (America/New_York) set to 2023-05-10 15:30:45
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.set(2023, Calendar.MAY, 10, 15, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);
        // 13. LocalDate (midnight)
        LocalDate localDate = LocalDate.of(2023, 5, 10);
        // 14. java.sql.Date (midnight)
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-05-10");
        // 15. Instant of negative epoch second (-1)
        Instant negativeInstant = Instant.ofEpochSecond(-1L);
        // 16. String with full ISO‑8601 offset datetime
        String isoOffsetString = "2023-05-10T07:45:30+02:00";
        // 17. ZonedDateTime with non‑UTC zone (Asia/Tokyo)
        ZonedDateTime zdtTokyo = ZonedDateTime.of(2023, 5, 10, 23, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        // 18. java.util.Date representing epoch (0L)
        Date epochDate = new Date(0L);
        return Stream.of(// LocalTime
        Arguments.of(lt, lt), // LocalDateTime
        Arguments.of(ldt, ldt.toLocalTime()), // ZonedDateTime
        Arguments.of(zdtUtc, zdtUtc.toLocalTime()), // Date
        Arguments.of(date, instantToLocalTime.apply(instantDate)), // Timestamp
        Arguments.of(ts, instantToLocalTime.apply(ts.toInstant())), // Long epoch milli
        Arguments.of(epochMilli, instantToLocalTime.apply(Instant.ofEpochMilli(epochMilli))), // BigDecimal epoch milli
        Arguments.of(bd, instantToLocalTime.apply(Instant.ofEpochMilli(bd.longValue()))), // OffsetDateTime
        Arguments.of(odt, odt.toLocalTime()), // Instant
        Arguments.of(instant, instantToLocalTime.apply(instant)), // String time
        Arguments.of(timeString, LocalTime.parse(timeString)), // BigInteger epoch milli
        Arguments.of(bi, instantToLocalTime.apply(Instant.ofEpochMilli(bi.longValue()))), // Calendar (America/New_York)
        Arguments.of(cal, instantToLocalTime.apply(cal.toInstant())), // LocalDate
        Arguments.of(localDate, LocalTime.MIDNIGHT), // java.sql.Date
        Arguments.of(sqlDate, LocalTime.MIDNIGHT), // Negative Instant
        Arguments.of(negativeInstant, instantToLocalTime.apply(negativeInstant)), // ISO offset string
        Arguments.of(isoOffsetString, LocalTime.of(7, 45, 30)), // ZonedDateTime Asia/Tokyo
        Arguments.of(zdtTokyo, zdtTokyo.toLocalTime()), // Epoch java.util.Date
        Arguments.of(epochDate, instantToLocalTime.apply(Instant.ofEpochMilli(0L))));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null input – NullPointerException from toZonedDateTime
        Arguments.of(null, NullPointerException.class), // Unsupported type: ZoneId
        Arguments.of(ZoneId.of("UTC"), IllegalArgumentException.class), // Unsupported type: Duration
        Arguments.of(Duration.ofHours(5), IllegalArgumentException.class), // Unsupported type: AtomicLong
        Arguments.of(new AtomicLong(12345L), IllegalArgumentException.class), // Unsupported type: OffsetTime (no date component)
        Arguments.of(OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC), IllegalArgumentException.class), // Invalid date string (non‑existent date)
        Arguments.of("2023-02-30T10:00:00", DateTimeParseException.class), // Completely unparsable string
        Arguments.of("not-a-date", DateTimeParseException.class), // Empty string
        Arguments.of("", DateTimeParseException.class), // Unsupported Boolean
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class));
    }

    @Test
    void toLocalTime_fromLocalTime() {
        LocalTime source = LocalTime.of(13, 45, 30);
        LocalTime result = CalendarConversions.toLocalTime(source, converter);
        assertEquals(source, result);
    }

    @Test
    void toLocalTime_fromLocalDateTime() {
        LocalDateTime source = LocalDateTime.of(2023, 5, 10, 13, 45, 30);
        LocalTime expected = source.toLocalTime();
        LocalTime result = CalendarConversions.toLocalTime(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalTime_fromZonedDateTime() {
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2023, 5, 10, 13, 45, 30), ZoneId.of("UTC"));
        LocalTime expected = source.toLocalTime();
        LocalTime result = CalendarConversions.toLocalTime(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalTime_fromInstant() {
        // Instant representing 13:45:30 on the epoch day (1970‑01‑01)
        Instant instant = Instant.ofEpochSecond(13 * 3600 + 45 * 60 + 30);
        LocalTime expected = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
        LocalTime result = CalendarConversions.toLocalTime(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalTime_fromDate() {
        Instant instant = Instant.ofEpochSecond(13 * 3600 + 45 * 60 + 30);
        Date source = Date.from(instant);
        LocalTime expected = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
        LocalTime result = CalendarConversions.toLocalTime(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalTime_fromCalendar() {
        Instant instant = Instant.ofEpochSecond(13 * 3600 + 45 * 60 + 30);
        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.from(instant));
        LocalTime expected = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
        LocalTime result = CalendarConversions.toLocalTime(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalTime_fromTimestamp() {
        Instant instant = Instant.ofEpochSecond(13 * 3600 + 45 * 60 + 30);
        java.sql.Timestamp source = new java.sql.Timestamp(instant.toEpochMilli());
        LocalTime expected = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
        LocalTime result = CalendarConversions.toLocalTime(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalTime_fromLongEpochMilli() {
        Instant instant = Instant.ofEpochSecond(13 * 3600 + 45 * 60 + 30);
        Long source = instant.toEpochMilli();
        LocalTime expected = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime();
        LocalTime result = CalendarConversions.toLocalTime(source, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} => expected={1}")
    @MethodSource("positiveConversionProvider")
    void toLocalTime_positive(Object from, LocalTime expected) {
        // Act
        LocalTime result = CalendarConversions.toLocalTime(from, converter);
        // Assert
        assertEquals(expected, result, () -> "Conversion of " + from.getClass().getSimpleName() + " did not produce the expected LocalTime");
    }

    @ParameterizedTest(name = "[{index}] from={0} => throws {1}")
    @MethodSource("exceptionProvider")
    void toLocalTime_exceptions(Object from, Class<? extends Throwable> expectedException) {
        // Act & Assert
        assertThrows(expectedException, () -> CalendarConversions.toLocalTime(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }

    @Test
    void toLocalTime_fromLocalTime_direct() {
        LocalTime source = LocalTime.of(13, 45, 30);
        LocalTime result = CalendarConversions.toLocalTime(source, converter);
        assertEquals(source, result);
    }

    @Test
    void toLocalTime_fromString_parsed() {
        String source = "13:45:30";
        LocalTime expected = LocalTime.of(13, 45, 30);
        LocalTime result = CalendarConversions.toLocalTime(source, converter);
        assertEquals(expected, result);
    }
}
