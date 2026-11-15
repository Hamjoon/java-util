package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToLocalDateTime_Test {

    private static ZoneId originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a fixed default zone to make the conversion results deterministic
        originalDefaultZone = ZoneId.systemDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone
        TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        // ISO‑8601 string
        String isoString = "2023-05-17T14:30:45";
        LocalDateTime isoExpected = LocalDateTime.parse(isoString);
        // java.sql.Timestamp
        Timestamp ts = Timestamp.valueOf("2023-05-17 14:30:45");
        LocalDateTime tsExpected = LocalDateTime.of(2023, 5, 17, 14, 30, 45);
        // ZonedDateTime with offset – should be converted to UTC (system default)
        ZonedDateTime zdtOffset = ZonedDateTime.parse("2023-05-17T14:30:45+02:00[Europe/Paris]");
        LocalDateTime zdtOffsetExpected = zdtOffset.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        // Long epoch milliseconds
        // corresponds to 2023‑05‑17T14:30:45Z
        long epochMilli = 1684339845000L;
        LocalDateTime epochExpected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
        // CharBuffer containing ISO string
        CharBuffer charBuf = CharBuffer.wrap(isoString);
        // ByteBuffer containing UTF‑8 bytes of ISO string
        ByteBuffer byteBuf = ByteBuffer.wrap(isoString.getBytes());
        // OffsetDateTime – same handling as ZonedDateTime with offset
        OffsetDateTime offsetDt = OffsetDateTime.parse("2023-05-17T14:30:45+02:00");
        LocalDateTime offsetDtExpected = offsetDt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        // java.sql.Date (date only)
        Date sqlDate = Date.valueOf("2023-05-17");
        LocalDateTime sqlDateExpected = LocalDateTime.of(2023, 5, 17, 0, 0);
        // Calendar in UTC
        Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calUtc.set(2023, Calendar.MAY, 17, 14, 30, 45);
        calUtc.set(Calendar.MILLISECOND, 0);
        LocalDateTime calUtcExpected = LocalDateTime.ofInstant(Instant.ofEpochMilli(calUtc.getTimeInMillis()), ZoneId.systemDefault());
        // Calendar in America/New_York (UTC‑4 at the given date)
        Calendar calNy = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        calNy.set(2023, Calendar.MAY, 17, 14, 30, 45);
        calNy.set(Calendar.MILLISECOND, 0);
        LocalDateTime calNyExpected = LocalDateTime.ofInstant(Instant.ofEpochMilli(calNy.getTimeInMillis()), ZoneId.systemDefault());
        // Map with "time" key (epoch millis)
        Map<String, Object> mapTime = new HashMap<>();
        mapTime.put("time", epochMilli);
        LocalDateTime mapTimeExpected = epochExpected;
        return Stream.of(Arguments.of(isoString, isoExpected), Arguments.of(ts, tsExpected), Arguments.of(zdtOffset, zdtOffsetExpected), Arguments.of(epochMilli, epochExpected), Arguments.of(charBuf, isoExpected), Arguments.of(byteBuf, isoExpected), Arguments.of(offsetDt, offsetDtExpected), Arguments.of(sqlDate, sqlDateExpected), Arguments.of(calUtc, calUtcExpected), Arguments.of(calNy, calNyExpected), Arguments.of(mapTime, mapTimeExpected));
    }

    private static Stream<Arguments> provideExceptionConversions() {
        // Unsupported primitive wrapper
        Integer unsupportedInt = 42;
        // LocalTime – no date component
        LocalTime localTime = LocalTime.of(14, 30, 45);
        // Incorrectly formatted string
        String badString = "2023/05/17 14:30:45";
        // Boolean value
        Boolean boolVal = true;
        // java.sql.Time (time‑only)
        java.sql.Time sqlTime = java.sql.Time.valueOf("14:30:45");
        // OffsetTime (time‑only with offset)
        java.time.OffsetTime offsetTime = java.time.OffsetTime.parse("14:30:45+02:00");
        // YearMonth (year‑month only)
        YearMonth yearMonth = YearMonth.of(2023, 5);
        // UUID (non‑date type)
        UUID uuid = UUID.randomUUID();
        // Empty map (no recognizable keys)
        Map<String, Object> emptyMap = new HashMap<>();
        // List of numbers
        List<Integer> list = List.of(1, 2, 3);
        // BigDecimal
        BigDecimal bigDecimal = new BigDecimal("123.45");
        // CharBuffer with non‑date content
        CharBuffer badCharBuf = CharBuffer.wrap("not-a-date");
        // ByteBuffer with invalid bytes
        ByteBuffer badByteBuf = ByteBuffer.wrap("invalid".getBytes());
        // Empty string
        String emptyString = "";
        return Stream.of(Arguments.of(unsupportedInt), Arguments.of(localTime), Arguments.of(badString), Arguments.of(boolVal), Arguments.of(sqlTime), Arguments.of(offsetTime), Arguments.of(yearMonth), Arguments.of(uuid), Arguments.of(emptyMap), Arguments.of(list), Arguments.of(bigDecimal), Arguments.of(badCharBuf), Arguments.of(badByteBuf), Arguments.of(emptyString));
    }

    @Test
    void convertNullReturnsNull() {
        assertNull(Converter.convertToLocalDateTime(null));
    }

    @Test
    void convertFromLocalDateTimeReturnsSameInstance() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = Converter.convertToLocalDateTime(now);
        assertEquals(now, result);
    }

    @Test
    void convertFromLocalDateAtStartOfDay() {
        LocalDate date = LocalDate.of(2023, Month.MARCH, 15);
        LocalDateTime expected = date.atStartOfDay();
        LocalDateTime result = Converter.convertToLocalDateTime(date);
        assertEquals(expected, result);
    }

    @Test
    void convertFromInstant() {
        Instant instant = Instant.parse("2023-03-15T10:15:30Z");
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        LocalDateTime result = Converter.convertToLocalDateTime(instant);
        assertEquals(expected, result);
    }

    @Test
    void convertFromDate() {
        // 2023‑03‑15T10:15:30Z expressed as milliseconds since epoch
        long epochMilli = 1678878930000L;
        Date date = new Date(epochMilli);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
        LocalDateTime result = Converter.convertToLocalDateTime(date);
        assertEquals(expected, result);
    }

    @Test
    void convertFromTimestamp() {
        long epochMilli = 1678878930000L;
        Timestamp ts = new Timestamp(epochMilli);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
        LocalDateTime result = Converter.convertToLocalDateTime(ts);
        assertEquals(expected, result);
    }

    @Test
    void convertFromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1678878930000L);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(cal.getTimeInMillis()), ZoneId.systemDefault());
        LocalDateTime result = Converter.convertToLocalDateTime(cal);
        assertEquals(expected, result);
    }

    @Test
    void convertFromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 3, 15, 10, 15, 30, 0, ZoneId.of("Europe/Paris"));
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime result = Converter.convertToLocalDateTime(zdt);
        assertEquals(expected, result);
    }

    @Test
    void convertFromMapWithTimeKey() {
        // Map representation expected by the Converter for date/time values
        Map<String, Object> map = new HashMap<>();
        long epochMilli = 1678878930000L;
        map.put("time", epochMilli);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
        LocalDateTime result = Converter.convertToLocalDateTime(map);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] fromInstance={0}")
    @MethodSource("provideSuccessfulConversions")
    void testConvertToLocalDateTime_Success(Object fromInstance, LocalDateTime expected) {
        LocalDateTime actual = Converter.convertToLocalDateTime(fromInstance);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] exception fromInstance={0}")
    @MethodSource("provideExceptionConversions")
    void testConvertToLocalDateTime_Exceptions(Object fromInstance) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToLocalDateTime(fromInstance));
    }
}
