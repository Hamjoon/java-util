package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToSqlDate_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // Force a deterministic default zone for all tests (UTC)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    private static Object buildSource(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String) {
            String s = (String) raw;
            // Prefix based factories
            if (s.startsWith("Calendar:")) {
                return parseCalendar(s.substring("Calendar:".length()));
            }
            if (s.startsWith("LocalDate:")) {
                return LocalDate.parse(s.substring("LocalDate:".length()));
            }
            if (s.startsWith("LocalDateTime:")) {
                return LocalDateTime.parse(s.substring("LocalDateTime:".length()));
            }
            if (s.startsWith("Instant:")) {
                return Instant.parse(s.substring("Instant:".length()));
            }
            if (s.startsWith("ZonedDateTime:")) {
                return ZonedDateTime.parse(s.substring("ZonedDateTime:".length()));
            }
            if (s.startsWith("SqlDate:")) {
                // java.sql.Date is a subclass of java.util.Date
                return new java.sql.Date(Timestamp.valueOf(s.substring("SqlDate:".length()).replace("T", " ").replace("Z", "")).getTime());
            }
            if (s.startsWith("CharBuffer:")) {
                return CharBuffer.wrap(s.substring("CharBuffer:".length()));
            }
            if (s.startsWith("ByteBuffer:")) {
                return ByteBuffer.wrap(s.substring("ByteBuffer:".length()).getBytes(StandardCharsets.UTF_8));
            }
            if (s.startsWith("UUID:")) {
                return UUID.fromString(s.substring("UUID:".length()));
            }
            if (s.startsWith("AtomicBoolean:")) {
                return new AtomicBoolean(Boolean.parseBoolean(s.substring("AtomicBoolean:".length())));
            }
            // Plain strings are returned as‑is
            return s;
        }
        // Numbers are returned unchanged (Long, Integer, etc.)
        return raw;
    }

    private static Calendar parseCalendar(String iso) {
        Instant instant = Instant.parse(iso);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(Date.from(instant));
        return cal;
    }

    private static Stream<Arguments> validStringProvider() {
        return Stream.of(Arguments.of("ISO‑8601 date only → midnight UTC", "2023-01-15", "2023-01-15T00:00:00Z"), Arguments.of("ISO‑8601 date‑time without zone → treated as UTC", "2023-01-15T08:45:30", "2023-01-15T08:45:30Z"), Arguments.of("ISO‑8601 with zone offset → converted to UTC", "2023-07-15T08:45:30+02:00", "2023-07-15T06:45:30Z"), Arguments.of("Epoch milliseconds as String", "1672531199000", "2023-01-01T00:59:59Z"));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("Empty string", "", IllegalArgumentException.class), Arguments.of("Whitespace only", "   ", IllegalArgumentException.class), Arguments.of("Unrecognizable format", "15/01/2023", IllegalArgumentException.class), Arguments.of("Impossible date (Feb 30)", "2023-02-30", IllegalArgumentException.class), Arguments.of("Month out of range", "2023-13-01", IllegalArgumentException.class), Arguments.of("Number out of Long range (too big)", "9223372036854775808", NumberFormatException.class), Arguments.of("Number out of Long range (too small)", "-9223372036854775809", NumberFormatException.class));
    }

    private static Stream<Arguments> invalidBufferProvider() {
        return Stream.of(Arguments.of("CharBuffer with bad format", CharBuffer.wrap("15/01/2023")), Arguments.of("Empty CharBuffer", CharBuffer.wrap("")), Arguments.of("ByteBuffer with bad format", ByteBuffer.wrap("invalid-date".getBytes(StandardCharsets.UTF_8))), Arguments.of("Empty ByteBuffer", ByteBuffer.wrap(new byte[0])));
    }

    private static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of("Boolean value", true), Arguments.of("UUID instance", UUID.fromString("123e4567-e89b-12d3-a456-426614174000")), Arguments.of("OffsetDateTime (unsupported)", OffsetDateTime.parse("2023-05-01T12:00:00+02:00")), Arguments.of("Double (unsupported numeric)", 12345.678), Arguments.of("Float (unsupported numeric)", 12345.6f), Arguments.of("Short (unsupported numeric)", (short) 12345), Arguments.of("Byte (unsupported numeric)", (byte) 12), Arguments.of("AtomicBoolean (unsupported)", new AtomicBoolean(true)), Arguments.of("Collection (List) – unsupported", List.of(1, 2, 3)));
    }

    @Test
    @DisplayName("convertToSqlDate returns null when source is null")
    void testNullInput() {
        Date result = Converter.convertToSqlDate(null);
        assertNull(result, "Conversion of null should produce null");
    }

    @Test
    @DisplayName("convertToSqlDate returns equivalent Date when source is a java.util.Date")
    void testDateInput() {
        Date now = new Date();
        Date result = Converter.convertToSqlDate(now);
        assertNotNull(result, "Result should not be null");
        assertEquals(now.getTime(), result.getTime(), "Converted Date should have the same epoch milliseconds as the source Date");
    }

    @Test
    @DisplayName("convertToSqlDate converts java.sql.Timestamp correctly")
    void testTimestampInput() {
        // some fixed epoch ms
        Timestamp ts = new Timestamp(1_600_000_000_000L);
        Date result = Converter.convertToSqlDate(ts);
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime(), "Converted Date should match the timestamp's epoch milliseconds");
    }

    @Test
    @DisplayName("convertToSqlDate converts Long (epoch millis) correctly")
    void testLongInput() {
        long epochMillis = 1_600_000_000_123L;
        Date result = Converter.convertToSqlDate(epochMillis);
        assertNotNull(result);
        assertEquals(epochMillis, result.getTime(), "Converted Date should represent the supplied epoch milliseconds");
    }

    @Test
    @DisplayName("convertToSqlDate converts Calendar correctly")
    void testCalendarInput() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2022, Calendar.MARCH, 15, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 250);
        Date expected = cal.getTime();
        Date result = Converter.convertToSqlDate(cal);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "Converted Date should have the same epoch milliseconds as the Calendar");
    }

    @Test
    @DisplayName("convertToSqlDate converts LocalDate correctly (start of day UTC)")
    void testLocalDateInput() {
        LocalDate localDate = LocalDate.of(2022, Month.APRIL, 5);
        // Converter uses the system default zone when converting LocalDate → Date
        ZoneId zone = ZoneId.systemDefault();
        long expectedMillis = localDate.atStartOfDay(zone).toInstant().toEpochMilli();
        Date result = Converter.convertToSqlDate(localDate);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "Converted Date should represent the start of the given LocalDate in the system default zone");
    }

    @Test
    @DisplayName("convertToSqlDate converts LocalDateTime correctly")
    void testLocalDateTimeInput() {
        LocalDateTime ldt = LocalDateTime.of(2022, Month.MAY, 10, 14, 45, 30, 123_000_000);
        ZoneId zone = ZoneId.systemDefault();
        long expectedMillis = ldt.atZone(zone).toInstant().toEpochMilli();
        Date result = Converter.convertToSqlDate(ldt);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "Converted Date should represent the same instant as the given LocalDateTime");
    }

    @Test
    @DisplayName("convertToSqlDate converts ZonedDateTime correctly")
    void testZonedDateTimeInput() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 6, 20, 9, 15, 0, 0, ZoneId.of("America/New_York"));
        long expectedMillis = zdt.toInstant().toEpochMilli();
        Date result = Converter.convertToSqlDate(zdt);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "Converted Date should represent the same instant as the given ZonedDateTime");
    }

    @Test
    @DisplayName("convertToSqlDate converts Integer (epoch millis) correctly")
    void testIntegerInput() {
        // fits in int
        int epochMillis = 1_600_000_000;
        Date result = Converter.convertToSqlDate(epochMillis);
        assertNotNull(result);
        assertEquals((long) epochMillis, result.getTime(), "Integer epoch millis should be treated as long");
    }

    @Test
    @DisplayName("convertToSqlDate converts java.sql.Date (sub‑class of java.util.Date) correctly")
    void testSqlDateInput() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-04-01");
        Date result = Converter.convertToSqlDate(sqlDate);
        assertNotNull(result);
        assertEquals(sqlDate.getTime(), result.getTime(), "SqlDate should be returned as an equivalent java.util.Date");
    }

    @Test
    @DisplayName("convertToSqlDate converts Instant correctly")
    void testInstantInput() {
        Instant instant = Instant.parse("2024-03-10T10:00:00Z");
        Date result = Converter.convertToSqlDate(instant);
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.getTime(), "Instant should be converted to Date with identical epoch");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validStringProvider")
    @DisplayName("convertToSqlDate parses valid String representations")
    void testValidStringInputs(String description, String source, String expectedIso) {
        Date result = Converter.convertToSqlDate(source);
        assertNotNull(result);
        Instant expected = Instant.parse(expectedIso);
        assertEquals(expected.toEpochMilli(), result.getTime(), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidStringProvider")
    @DisplayName("convertToSqlDate throws IllegalArgumentException for malformed strings")
    void testInvalidStringInputs(String description, String source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToSqlDate(source), description);
    }

    @Test
    @DisplayName("convertToSqlDate parses CharBuffer containing a valid ISO‑8601 date")
    void testCharBufferValid() {
        CharBuffer cb = CharBuffer.wrap("2023-08-01");
        Date result = Converter.convertToSqlDate(cb);
        assertNotNull(result);
        Instant expected = Instant.parse("2023-08-01T00:00:00Z");
        assertEquals(expected.toEpochMilli(), result.getTime());
    }

    @Test
    @DisplayName("convertToSqlDate parses ByteBuffer containing a valid ISO‑8601 date‑time")
    void testByteBufferValid() {
        ByteBuffer bb = ByteBuffer.wrap("2023-08-01T14:20:15Z".getBytes(StandardCharsets.UTF_8));
        Date result = Converter.convertToSqlDate(bb);
        assertNotNull(result);
        Instant expected = Instant.parse("2023-08-01T14:20:15Z");
        assertEquals(expected.toEpochMilli(), result.getTime());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidBufferProvider")
    @DisplayName("convertToSqlDate throws IllegalArgumentException for invalid Char/ByteBuffer")
    void testInvalidBufferInputs(String description, Object buffer) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToSqlDate(buffer), description);
    }

    @Test
    @DisplayName("convertToSqlDate converts BigDecimal containing epoch millis")
    void testBigDecimalInput() {
        // 2009‑02‑13T23:31:30.123Z
        BigDecimal bd = new BigDecimal("1234567890123");
        Date result = Converter.convertToSqlDate(bd);
        assertNotNull(result);
        Instant expected = Instant.parse("2009-02-13T23:31:30.123Z");
        assertEquals(expected.toEpochMilli(), result.getTime());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedProvider")
    @DisplayName("convertToSqlDate throws UnsupportedOperationException for unsupported types")
    void testUnsupportedTypes(String description, Object source) {
        assertThrows(UnsupportedOperationException.class, () -> Converter.convertToSqlDate(source), description);
    }

    @Test
    @DisplayName("convertToSqlDate always returns java.util.Date (or subclass)")
    void testReturnType() {
        Date date = Converter.convertToSqlDate("2023-01-01");
        assertTrue(date instanceof java.util.Date, "Result must be an instance of java.util.Date");
    }
}
