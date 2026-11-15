package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToDate_Test {

    private static Stream<Arguments> validConversionProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        // Direct Date instance
        Date epochDate = new Date(0L);
        // java.sql.Timestamp
        Timestamp ts = Timestamp.valueOf("2023-03-15 12:34:56.789");
        // java.time.Instant
        Instant instant = Instant.parse("2022-01-01T00:00:00Z");
        // java.time.LocalDateTime (system default zone)
        LocalDateTime ldt = LocalDateTime.of(2022, 5, 10, 15, 30, 45);
        Date ldtExpected = Date.from(ldt.atZone(systemZone).toInstant());
        // java.time.ZonedDateTime
        ZonedDateTime zdt = ZonedDateTime.parse("2021-12-31T23:59:59+02:00[Europe/Paris]");
        Date zdtExpected = Date.from(zdt.toInstant());
        // java.time.LocalDate
        LocalDate ld = LocalDate.of(2020, 2, 29);
        Date ldExpected = Date.from(ld.atStartOfDay(systemZone).toInstant());
        // Long epoch millis
        // 2020-09-13T12:26:40Z
        Long longEpoch = 1_600_000_000_000L;
        Date longExpected = new Date(longEpoch);
        // Integer epoch millis
        // 1970-01-19T13:46:40Z
        Integer intEpoch = 1_600_000_000;
        Date intExpected = new Date(intEpoch.longValue());
        // BigDecimal epoch millis
        BigDecimal bd = new BigDecimal("1620000000000");
        Date bdExpected = new Date(bd.longValueExact());
        // Calendar (UTC)
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.set(2021, Calendar.JANUARY, 1, 0, 0, 0);
        utcCal.set(Calendar.MILLISECOND, 0);
        Date utcCalExpected = utcCal.getTime();
        // String ISO‑8601 UTC
        String isoUtc = "2022-08-15T10:20:30Z";
        Date isoUtcExpected = Date.from(Instant.parse(isoUtc));
        // String non‑ISO format (system default zone)
        String nonIso = "2022/08/15 10:20:30";
        LocalDateTime nonIsoLdt = LocalDateTime.parse("2022-08-15T10:20:30");
        Date nonIsoExpected = Date.from(nonIsoLdt.atZone(systemZone).toInstant());
        // Numeric string
        String numericString = "1620000000000";
        Date numericStringExpected = new Date(Long.parseLong(numericString));
        // java.sql.Date
        java.sql.Date sqlDate = new java.sql.Date(Instant.parse("2021-12-01T00:00:00Z").toEpochMilli());
        Date sqlDateExpected = new Date(sqlDate.getTime());
        // OffsetDateTime
        OffsetDateTime odt = OffsetDateTime.parse("2022-07-04T15:30:45+03:00");
        Date odtExpected = Date.from(odt.toInstant());
        // Calendar with non‑UTC zone (America/New_York)
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCal.set(2022, Calendar.MARCH, 15, 12, 0, 0);
        nyCal.set(Calendar.MILLISECOND, 0);
        Date nyCalExpected = nyCal.getTime();
        // Short epoch millis
        Short shortEpoch = Short.valueOf((short) 1_600_000_000);
        Date shortExpected = new Date(shortEpoch.longValue());
        // Byte epoch millis (value 1000)
        Byte byteEpoch = Byte.valueOf((byte) 1000);
        // byte is signed, but value 1000 overflows to 232; using &0xFF to get unsigned 1000%256? For test we just use 1000L
        Date byteExpected = new Date(byteEpoch.longValue() & 0xFF);
        // Actually we want 1000 ms, so we use 1000L directly
        Date byteExactExpected = new Date(1000L);
        // BigInteger epoch millis
        BigInteger bi = new BigInteger("1620000000000");
        Date biExpected = new Date(bi.longValueExact());
        return Stream.of(Arguments.of("Direct Date instance", epochDate, epochDate), Arguments.of("java.sql.Timestamp", ts, new Date(ts.getTime())), Arguments.of("java.time.Instant", instant, Date.from(instant)), Arguments.of("java.time.LocalDateTime (system zone)", ldt, ldtExpected), Arguments.of("java.time.ZonedDateTime", zdt, zdtExpected), Arguments.of("java.time.LocalDate (system zone)", ld, ldExpected), Arguments.of("Long epoch milliseconds", longEpoch, longExpected), Arguments.of("Integer epoch milliseconds", intEpoch, intExpected), Arguments.of("BigDecimal epoch milliseconds", bd, bdExpected), Arguments.of("Calendar (UTC)", utcCal, utcCalExpected), Arguments.of("ISO‑8601 UTC string", isoUtc, isoUtcExpected), Arguments.of("Non‑ISO date string", nonIso, nonIsoExpected), Arguments.of("Numeric string", numericString, numericStringExpected), Arguments.of("java.sql.Date", sqlDate, sqlDateExpected), Arguments.of("OffsetDateTime", odt, odtExpected), Arguments.of("Calendar (America/New_York)", nyCal, nyCalExpected), Arguments.of("Short epoch milliseconds", shortEpoch, shortExpected), Arguments.of("Byte epoch milliseconds (1000)", byteEpoch, byteExactExpected), Arguments.of("BigInteger epoch milliseconds", bi, biExpected));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        // Unsupported object types
        Object plainObject = new Object();
        UUID uuid = UUID.randomUUID();
        // Malformed strings
        String notADate = "not-a-date";
        String invalidMonth = "2022-13-01";
        // Unsupported temporal types
        LocalTime localTime = LocalTime.of(12, 34, 56);
        OffsetTime offsetTime = OffsetTime.of(12, 34, 56, 0, ZoneOffset.UTC);
        // Empty / whitespace strings
        String empty = "";
        String whitespace = "   ";
        // Unsupported java.sql.Time
        java.sql.Time sqlTime = new java.sql.Time(Instant.parse("2022-01-01T12:00:00Z").toEpochMilli());
        // Duration
        Duration duration = Duration.ofHours(5);
        // NaN / Infinity
        Double nan = Double.NaN;
        Float posInf = Float.POSITIVE_INFINITY;
        // BigDecimal with fractional part
        BigDecimal bdFraction = new BigDecimal("1620000000.5");
        // BigInteger out of long range
        // Long.MAX_VALUE + 1
        BigInteger biOutOfRange = new BigInteger("9223372036854775808");
        // CharBuffer with non‑date content
        CharBuffer charBufBad = CharBuffer.wrap("not-a-date");
        // Empty CharBuffer
        CharBuffer charBufEmpty = CharBuffer.allocate(0);
        // ByteBuffer too short
        ByteBuffer byteBufShort = ByteBuffer.wrap(new byte[] { 0x00, 0x01 });
        // Empty ByteBuffer
        ByteBuffer byteBufEmpty = ByteBuffer.allocate(0);
        // YearMonth
        YearMonth ym = YearMonth.of(2022, 5);
        // Pattern (regex)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*");
        // AtomicBoolean
        java.util.concurrent.atomic.AtomicBoolean atomicBool = java.util.concurrent.atomic.AtomicBoolean.TRUE;
        return Stream.of(Arguments.of("Plain Object (unsupported)", plainObject), Arguments.of("UUID (unsupported)", uuid), Arguments.of("Malformed string (non‑date)", notADate), Arguments.of("Malformed string (invalid month)", invalidMonth), Arguments.of("LocalTime (no date component)", localTime), Arguments.of("OffsetTime (no date component)", offsetTime), Arguments.of("Empty string", empty), Arguments.of("Whitespace‑only string", whitespace), Arguments.of("java.sql.Time (no date component)", sqlTime), Arguments.of("Duration (temporal amount)", duration), Arguments.of("Double.NaN", nan), Arguments.of("Float.POSITIVE_INFINITY", posInf), Arguments.of("BigDecimal with fractional part", bdFraction), Arguments.of("BigInteger out of long range", biOutOfRange), Arguments.of("CharBuffer with non‑date content", charBufBad), Arguments.of("Empty CharBuffer", charBufEmpty), Arguments.of("ByteBuffer with insufficient bytes", byteBufShort), Arguments.of("Empty ByteBuffer", byteBufEmpty), Arguments.of("YearMonth (no day component)", ym), Arguments.of("Pattern (unrelated type)", pattern), Arguments.of("AtomicBoolean (unsupported)", atomicBool));
    }

    @Test
    void shouldConvertStringToDate() {
        // given
        String iso = "2023-04-15T10:20:30";
        // when
        Date result = Converter.convertToDate(iso);
        // then
        assertNotNull(result);
        // The string is parsed as a LocalDateTime (the default format used by the
        // Converter) and then converted to a Date in the system default zone.
        LocalDateTime ldt = LocalDateTime.parse(iso);
        Date expected = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        assertEquals(expected.getTime(), result.getTime());
    }

    @Test
    void shouldConvertLongTimestampToDate() {
        // given
        // 2022-01-01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        // when
        Date result = Converter.convertToDate(epochMilli);
        // then
        assertNotNull(result);
        assertEquals(epochMilli, result.getTime());
    }

    @Test
    void shouldConvertInstantToDate() {
        // given
        Instant instant = Instant.parse("2021-12-31T23:59:59Z");
        // when
        Date result = Converter.convertToDate(instant);
        // then
        assertNotNull(result);
        assertEquals(Date.from(instant), result);
    }

    @Test
    void shouldConvertCalendarToDate() {
        // given
        Calendar cal = Calendar.getInstance();
        cal.set(2020, Calendar.JUNE, 15, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        // when
        Date result = Converter.convertToDate(cal);
        // then
        assertNotNull(result);
        assertEquals(cal.getTimeInMillis(), result.getTime());
    }

    @Test
    void shouldConvertZonedDateTimeToDate() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2019, 5, 10, 8, 15, 30, 0, ZoneId.of("UTC"));
        // when
        Date result = Converter.convertToDate(zdt);
        // then
        assertNotNull(result);
        assertEquals(Date.from(zdt.toInstant()), result);
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        // given
        Object input = null;
        // when
        Date result = Converter.convertToDate(input);
        // then
        assertNull(result);
    }

    @Test
    void shouldThrowExceptionForUnsupportedType() {
        // given
        // no conversion defined for plain Object -> Date
        Object unsupported = new Object();
        // when / then
        assertThrows(RuntimeException.class, () -> Converter.convertToDate(unsupported));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void shouldConvertVariousValidInputs(String description, Object input, Date expected) {
        Date actual = Converter.convertToDate(input);
        assertNotNull(actual, "Result should not be null for: " + description);
        assertEquals(expected.getTime(), actual.getTime(), "Converted date does not match expected for: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void shouldRejectUnsupportedOrMalformedInputs(String description, Object input) {
        assertThrows(RuntimeException.class, () -> Converter.convertToDate(input), "Expected conversion to fail for: " + description);
    }
}
