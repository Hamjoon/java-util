package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToSqlDate_Test {

    private static TimeZone originalDefaultZone;

    @BeforeAll
    static void setUpAll() {
        // Preserve the original default zone and force UTC for deterministic results.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void tearDownAll() {
        // Restore the original default zone.
        TimeZone.setDefault(originalDefaultZone);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// java.util.Date – should be returned unchanged (same instance)
        Arguments.of("Date instance unchanged", new Date(1692105600000L), new Date(1692105600000L)), // java.sql.Timestamp – same instant as the timestamp
        Arguments.of("Timestamp converted to Date", Timestamp.valueOf("2023-08-15 12:34:56.789"), new Date(Timestamp.valueOf("2023-08-15 12:34:56.789").getTime())), // java.time.LocalDate – start of day in the default (UTC) zone
        Arguments.of("LocalDate converted to start‑of‑day Date", LocalDate.of(2023, 8, 15), Date.from(LocalDate.of(2023, 8, 15).atStartOfDay(ZoneOffset.UTC).toInstant())), // java.time.LocalDateTime – interpreted as local date‑time in default zone
        Arguments.of("LocalDateTime converted to Date", LocalDateTime.of(2023, 8, 15, 12, 34, 56, 123_000_000), Date.from(LocalDateTime.of(2023, 8, 15, 12, 34, 56, 123_000_000).atZone(ZoneId.systemDefault()).toInstant())), // java.time.Instant – direct mapping
        Arguments.of("Instant converted to Date", Instant.parse("2023-08-15T12:34:56.789Z"), Date.from(Instant.parse("2023-08-15T12:34:56.789Z"))), // java.util.Calendar – uses the stored epoch millis
        Arguments.of("Calendar converted to Date", calendarWithMillis(1692105600000L), new Date(1692105600000L)), // Long epoch millis
        Arguments.of("Long epoch millis converted to Date", 1692105600000L, new Date(1692105600000L)), // ISO‑8601 date string (date only)
        Arguments.of("ISO‑8601 date string converted to Date", "2023-08-15", Date.from(LocalDate.parse("2023-08-15").atStartOfDay(ZoneOffset.UTC).toInstant())), // ISO‑8601 date‑time string (no zone)
        Arguments.of("ISO‑8601 date‑time string converted to Date", "2023-08-15T12:34:56", Date.from(LocalDateTime.parse("2023-08-15T12:34:56").atZone(ZoneId.systemDefault()).toInstant())), // java.sql.Date – subclass of java.util.Date, same instant
        Arguments.of("java.sql.Date instance", java.sql.Date.valueOf("2023-08-15"), new Date(java.sql.Date.valueOf("2023-08-15").getTime())), // ZonedDateTime – absolute instant must be preserved
        Arguments.of("ZonedDateTime (America/New_York) converted to UTC Date", ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneId.of("America/New_York")), Date.from(ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneId.of("America/New_York")).toInstant())), // OffsetDateTime – absolute instant must be preserved
        Arguments.of("OffsetDateTime (+02:00) converted to UTC Date", OffsetDateTime.parse("2023-08-15T12:34:56+02:00"), Date.from(OffsetDateTime.parse("2023-08-15T12:34:56+02:00").toInstant())), // Calendar with non‑UTC zone (epoch millis stay the same)
        Arguments.of("Calendar with America/Los_Angeles zone", calendarWithMillisAndZone(1692105600000L, "America/Los_Angeles"), new Date(1692105600000L)), // CharBuffer – treated like a CharSequence/String
        Arguments.of("CharBuffer containing ISO‑8601 date‑time", CharBuffer.wrap("2023-08-15T12:34:56"), Date.from(LocalDateTime.parse("2023-08-15T12:34:56").atZone(ZoneId.systemDefault()).toInstant())), // Long.MAX_VALUE – extreme epoch value
        Arguments.of("Long.MAX_VALUE epoch millis", Long.MAX_VALUE, new Date(Long.MAX_VALUE)), // Negative epoch (pre‑1970)
        Arguments.of("Negative epoch millis", -1L, new Date(-1L)), // null source – should return null (handled separately below)
        Arguments.of("null source returns null", null, null));
    }

    private static Stream<Arguments> parsingExceptionProvider() {
        return Stream.of(Arguments.of("Empty string", ""), Arguments.of("Invalid literal string", "invalid-date"), Arguments.of("Non‑existent leap day", "2023-02-29"), Arguments.of("CharBuffer empty", CharBuffer.wrap("")), Arguments.of("CharBuffer non‑date", CharBuffer.wrap("not-a-date")), Arguments.of("CharBuffer invalid month", CharBuffer.wrap("2023-13-01")), Arguments.of("CharBuffer invalid day", CharBuffer.wrap("2023-02-30")));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of("LocalTime (no date component)", LocalTime.of(12, 34, 56)), Arguments.of("UUID", UUID.randomUUID()), Arguments.of("ByteBuffer", ByteBuffer.wrap("2023-08-15".getBytes())), Arguments.of("Boolean.TRUE", Boolean.TRUE), Arguments.of("Double value", Double.valueOf(123.45)), Arguments.of("Float value", Float.valueOf(12.3f)), Arguments.of("BigDecimal", new java.math.BigDecimal("12345")), Arguments.of("BigInteger", new java.math.BigInteger("12345")), Arguments.of("java.sql.Time", java.sql.Time.valueOf("12:34:56")), Arguments.of("Year", Year.of(2023)), Arguments.of("YearMonth", YearMonth.of(2023, 2)), Arguments.of("MonthDay", MonthDay.of(2, 29)), Arguments.of("List<Integer>", Arrays.asList(1, 2, 3)), Arguments.of("Array of String", new String[] { "2023-08-15" }), Arguments.of("Map<String,String>", Collections.singletonMap("key", "2023-08-15")), Arguments.of("Short wrapper", Short.valueOf((short) 123)), Arguments.of("Byte wrapper", Byte.valueOf((byte) 10)));
    }

    /* -------------------------------------------------------------
     *  Helper methods
     * ------------------------------------------------------------- */
    private static Calendar calendarWithMillis(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Calendar calendarWithMillisAndZone(long epochMilli, String zoneId) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    @Test
    void shouldConvertDateInstanceUnchanged() {
        // given
        Date now = new Date();
        // when
        Date result = Converter.convertToSqlDate(now);
        // then
        assertSame(now, result, "A Date instance should be returned unchanged");
    }

    @Test
    void shouldConvertTimestampToDate() {
        // given
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        // when
        Date result = Converter.convertToSqlDate(ts);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(ts.getTime(), result.getTime(), "Timestamp should be converted to Date with the same epoch milliseconds");
    }

    @Test
    void shouldConvertLongToDate() {
        // given
        long epochMilli = Instant.now().toEpochMilli();
        // when
        Date result = Converter.convertToSqlDate(epochMilli);
        // then
        assertNotNull(result);
        assertEquals(epochMilli, result.getTime(), "Long epoch milliseconds should be converted to a Date with matching time");
    }

    @Test
    void shouldConvertStringToDate() {
        // given
        // The Converter understands ISO‑8601 and a few common patterns.
        // Using a format that the library supports, e.g., "2023-01-15T12:34:56"
        String iso = "2023-01-15T12:34:56";
        // when
        Date result = Converter.convertToSqlDate(iso);
        // then
        assertNotNull(result);
        // Verify that the parsed instant matches the string representation.
        // assume UTC
        long expected = Instant.parse(iso + "Z").toEpochMilli();
        assertEquals(expected, result.getTime(), "String ISO‑8601 should be parsed to the correct epoch milliseconds");
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        Date result = Converter.convertToSqlDate(source);
        // then
        assertNull(result, "Null source should result in null Date");
    }

    @Test
    void shouldThrowExceptionForUnsupportedType() {
        // given
        // no conversion defined from Object to Date
        Object unsupported = new Object();
        // when / then
        assertThrows(RuntimeException.class, () -> Converter.convertToSqlDate(unsupported), "Converting an unsupported type should throw a RuntimeException");
    }

    /* -------------------------------------------------------------
     *  Positive conversion tests – parameterised
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void shouldConvertVariousSources(String description, Object source, Date expected) {
        Date actual = Converter.convertToSqlDate(source);
        assertNotNull(actual, "Result must not be null for: " + description);
        assertEquals(expected.getTime(), actual.getTime(), "Epoch millis should match for: " + description);
    }

    /* -------------------------------------------------------------
     *  Expected parsing / illegal‑argument failures
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("parsingExceptionProvider")
    void shouldThrowDateTimeParseException(String description, Object source) {
        // the library throws RuntimeException wrapping DateTimeParseException
        assertThrows(// the library throws RuntimeException wrapping DateTimeParseException
        RuntimeException.class, () -> Converter.convertToSqlDate(source), "Expected parsing exception for: " + description);
    }

    /* -------------------------------------------------------------
     *  Unsupported source types – IllegalArgumentException
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedSourceProvider")
    void shouldThrowIllegalArgumentExceptionForUnsupportedTypes(String description, Object source) {
        // library uses RuntimeException for unsupported conversion
        assertThrows(// library uses RuntimeException for unsupported conversion
        RuntimeException.class, () -> Converter.convertToSqlDate(source), "Expected IllegalArgumentException for: " + description);
    }
}
