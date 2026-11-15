package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToLocalDate_Test {

    /**
     * A reference LocalDate used by several tests.
     */
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2023, Month.MARCH, 15);

    /**
     * A reference Instant that corresponds to the same day as REFERENCE_DATE in UTC.
     */
    private static final Instant REFERENCE_INSTANT = REFERENCE_DATE.atStartOfDay(ZoneOffset.UTC).toInstant();

    /**
     * A reference Date (java.util.Date) that corresponds to the same instant.
     */
    private static final Date REFERENCE_UTIL_DATE = Date.from(REFERENCE_INSTANT);

    /**
     * A reference java.sql.Date (which is a subclass of java.util.Date).
     */
    private static final java.sql.Date REFERENCE_SQL_DATE = java.sql.Date.valueOf(REFERENCE_DATE);

    /**
     * A reference Timestamp that corresponds to the same instant.
     */
    private static final Timestamp REFERENCE_TIMESTAMP = Timestamp.from(REFERENCE_INSTANT);

    /**
     * A reference Calendar set to the same instant.
     */
    private static final Calendar REFERENCE_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /**
     * A reference LocalDateTime (midnight of the reference day).
     */
    private static final LocalDateTime REFERENCE_LOCAL_DATE_TIME = REFERENCE_DATE.atStartOfDay();

    /**
     * A reference ZonedDateTime (UTC zone).
     */
    private static final ZonedDateTime REFERENCE_ZONED_DATE_TIME = REFERENCE_LOCAL_DATE_TIME.atZone(ZoneOffset.UTC);

    @BeforeAll
    static void setupBeforeAll() {
        // Force the JVM default zone to UTC to make date‑time conversions deterministic.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// LocalDate (should be returned unchanged)
        Arguments.of(LocalDate.of(2023, 5, 17), LocalDate.of(2023, 5, 17)), // LocalDateTime → LocalDate
        Arguments.of(LocalDateTime.of(2023, 5, 17, 12, 30, 45), LocalDate.of(2023, 5, 17)), // java.sql.Date → LocalDate
        Arguments.of(java.sql.Date.valueOf("2023-05-17"), LocalDate.of(2023, 5, 17)), // java.util.Date (Instant based) → LocalDate
        Arguments.of(Date.from(Instant.parse("2023-05-17T00:00:00Z")), LocalDate.of(2023, 5, 17)), // ISO‑8601 String → LocalDate
        Arguments.of("2023-05-17", LocalDate.of(2023, 5, 17)), // Instant → LocalDate
        Arguments.of(Instant.parse("2023-05-17T15:20:30Z"), LocalDate.of(2023, 5, 17)), // ZonedDateTime → LocalDate
        Arguments.of(ZonedDateTime.parse("2023-05-17T10:15:30+02:00[Europe/Paris]"), LocalDate.of(2023, 5, 17)), // Parsed LocalDate string → LocalDate (same instance semantics not required)
        Arguments.of(LocalDate.parse("2023-05-17"), LocalDate.of(2023, 5, 17)), // Calendar (UTC) → LocalDate
        Arguments.of(createUtcCalendar(2023, Calendar.MAY, 17, 5, 0, 0), LocalDate.of(2023, 5, 17)), // Timestamp → LocalDate
        Arguments.of(Timestamp.valueOf("2023-05-17 23:59:59"), LocalDate.of(2023, 5, 17)), // ISO‑8601 datetime string → LocalDate (time part ignored)
        Arguments.of("2023-05-17T00:00:00Z", LocalDate.of(2023, 5, 17)), // OffsetDateTime → LocalDate
        Arguments.of(OffsetDateTime.parse("2023-05-17T23:00:00+02:00"), LocalDate.of(2023, 5, 17)), // Boundary dates
        Arguments.of(LocalDate.MAX, LocalDate.MAX), Arguments.of(LocalDate.MIN, LocalDate.MIN), // Epoch instant → LocalDate (1970‑01‑01)
        Arguments.of(Instant.EPOCH, LocalDate.of(1970, 1, 1)), // CharBuffer containing ISO date string
        Arguments.of(CharBuffer.wrap("2023-05-17"), LocalDate.of(2023, 5, 17)), // ByteBuffer containing UTF‑8 bytes of ISO date string
        Arguments.of(ByteBuffer.wrap("2023-05-17".getBytes(StandardCharsets.UTF_8)), LocalDate.of(2023, 5, 17)));
    }

    private static Stream<Arguments> provideInvalidConversions() {
        return Stream.of(// Unsupported Boolean
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class), // Non‑ISO format string
        Arguments.of("05/17/2023", DateTimeParseException.class), // Unsupported numeric types
        Arguments.of(Integer.valueOf(20230517), IllegalArgumentException.class), Arguments.of(Double.valueOf(2023.05), IllegalArgumentException.class), Arguments.of(new BigDecimal("2023-05-17"), IllegalArgumentException.class), // UUID
        Arguments.of(UUID.randomUUID(), IllegalArgumentException.class), // Collection types
        Arguments.of(List.of("2023-05-17"), IllegalArgumentException.class), Arguments.of(Map.of("date", "2023-05-17"), IllegalArgumentException.class), // Malformed date strings
        Arguments.of("", DateTimeParseException.class), Arguments.of("2023-02-30", DateTimeParseException.class), Arguments.of("2023-13-01", DateTimeParseException.class), Arguments.of("2023-05-17T25:00:00", DateTimeParseException.class), Arguments.of("   ", DateTimeParseException.class), // Malformed CharBuffer / ByteBuffer
        Arguments.of(CharBuffer.wrap("2023-02-30"), DateTimeParseException.class), Arguments.of(ByteBuffer.wrap("invalid".getBytes(StandardCharsets.UTF_8)), DateTimeParseException.class), // Unsupported temporal types
        Arguments.of(LocalTime.of(12, 30), IllegalArgumentException.class), Arguments.of(YearMonth.of(2023, 5), IllegalArgumentException.class), Arguments.of(MonthDay.of(5, 17), IllegalArgumentException.class), Arguments.of(Duration.ofHours(5), IllegalArgumentException.class), Arguments.of(Period.ofDays(5), IllegalArgumentException.class));
    }

    /**
     * Helper to create a {@link Calendar} set to a specific UTC date‑time.
     *
     * @param year   year
     * @param month  {@link Calendar} month constant (e.g., {@link Calendar#MAY})
     * @param day    day of month
     * @param hour   hour of day (0‑23)
     * @param minute minute
     * @param second second
     * @return a {@link Calendar} instance in UTC
     */
    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void convert_null_returnsNull() {
        assertNull(Converter.convertToLocalDate(null));
    }

    @Test
    void convert_javaUtilDate_isConverted() {
        LocalDate result = Converter.convertToLocalDate(REFERENCE_UTIL_DATE);
        assertEquals(REFERENCE_DATE, result);
    }

    @Test
    void convert_javaSqlDate_isConverted() {
        LocalDate result = Converter.convertToLocalDate(REFERENCE_SQL_DATE);
        assertEquals(REFERENCE_DATE, result);
    }

    @Test
    void convert_timestamp_isConverted() {
        LocalDate result = Converter.convertToLocalDate(REFERENCE_TIMESTAMP);
        assertEquals(REFERENCE_DATE, result);
    }

    @Test
    void convert_instant_isConverted() {
        LocalDate result = Converter.convertToLocalDate(REFERENCE_INSTANT);
        assertEquals(REFERENCE_DATE, result);
    }

    @Test
    void convert_localDate_isReturnedDirectly() {
        LocalDate result = Converter.convertToLocalDate(REFERENCE_DATE);
        assertSame(REFERENCE_DATE, result);
    }

    @Test
    void convert_localDateTime_isConverted() {
        LocalDate result = Converter.convertToLocalDate(REFERENCE_LOCAL_DATE_TIME);
        assertEquals(REFERENCE_DATE, result);
    }

    @Test
    void convert_zonedDateTime_isConverted() {
        LocalDate result = Converter.convertToLocalDate(REFERENCE_ZONED_DATE_TIME);
        assertEquals(REFERENCE_DATE, result);
    }

    @Test
    void convert_calendar_isConverted() {
        LocalDate result = Converter.convertToLocalDate(REFERENCE_CALENDAR);
        assertEquals(REFERENCE_DATE, result);
    }

    @Test
    void convert_stringIsoDate_isConverted() {
        // The Converter supports ISO‑8601 strings for date conversion.
        // e.g., "2023-03-15"
        String isoDate = REFERENCE_DATE.toString();
        LocalDate result = Converter.convertToLocalDate(isoDate);
        assertEquals(REFERENCE_DATE, result);
    }

    @Test
    void convert_stringWithCustomPattern_isConverted() {
        // The library also parses common date patterns such as "yyyy/MM/dd".
        String custom = "2023/03/15";
        LocalDate result = Converter.convertToLocalDate(custom);
        assertEquals(REFERENCE_DATE, result);
    }

    @Test
    void convert_mapWithDateKeys_isConverted() {
        // Map conversion uses the "time" key for epoch‑millis or "value" for a string.
        Map<String, Object> map = new HashMap<>();
        map.put("time", REFERENCE_INSTANT.toEpochMilli());
        LocalDate result = Converter.convertToLocalDate(map);
        assertEquals(REFERENCE_DATE, result);
    }

    /**
     * Successful conversions that should return the expected {@link LocalDate}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideValidConversions")
    void convert_valid_returnsExpected(Object input, LocalDate expected) {
        assertEquals(expected, Converter.convertToLocalDate(input));
    }

    /**
     * Conversions that are expected to fail with a specific exception type.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideInvalidConversions")
    void convert_invalid_throwsException(Object input, Class<? extends Throwable> exceptionClass) {
        assertThrows(exceptionClass, () -> Converter.convertToLocalDate(input));
    }
}
