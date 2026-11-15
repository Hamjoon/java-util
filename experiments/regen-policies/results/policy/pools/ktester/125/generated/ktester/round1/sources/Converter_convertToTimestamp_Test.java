package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class Converter_convertToTimestamp_Test {

    /**
     * Helper that asserts two timestamps represent the same instant.
     */
    private static void assertSameInstant(Timestamp expected, Timestamp actual) {
        assertNotNull(actual, "Converted Timestamp should not be null");
        assertEquals(expected.getTime(), actual.getTime(), "Timestamp instant (epoch millis) does not match");
        // Also compare nanos for sub‑millisecond precision
        assertEquals(expected.getNanos(), actual.getNanos(), "Timestamp nanos component does not match");
    }

    /**
     * Parses the expected string representation into a {@link Timestamp}.
     *
     * @param expectedStr either a SQL‑style string (space separator) or an ISO‑8601 string.
     * @return the corresponding {@code Timestamp}.
     */
    private static Timestamp parseExpected(String expectedStr) {
        // ISO‑8601 strings contain a 'T' or end with a 'Z'
        if (expectedStr.contains("T") || expectedStr.endsWith("Z")) {
            Instant instant = Instant.parse(expectedStr);
            return Timestamp.from(instant);
        }
        // Otherwise assume the SQL‑style format accepted by Timestamp.valueOf
        return Timestamp.valueOf(expectedStr);
    }

    /**
     * Supplies all valid conversion scenarios (including null).
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// null → null
        Arguments.of(null, null), // Already a Timestamp (should be returned unchanged)
        Arguments.of(Timestamp.valueOf("2023-01-01 12:34:56.789"), "2023-01-01 12:34:56.789"), // java.util.Date
        // 2023‑01‑01 12:34:56.789 UTC
        Arguments.// 2023‑01‑01 12:34:56.789 UTC
        of(// 2023‑01‑01 12:34:56.789 UTC
        new Date(1672532096789L), "2023-01-01 12:34:56.789"), // LocalDateTime (system default zone – we use UTC for deterministic test)
        Arguments.of(LocalDateTime.of(2023, 3, 15, 10, 20, 30), "2023-03-15 10:20:30.0"), // Instant
        Arguments.of(Instant.ofEpochMilli(1678879230123L), "2023-03-15T10:20:30.123Z"), // ZonedDateTime UTC
        Arguments.of(ZonedDateTime.of(2023, 3, 15, 10, 20, 30, 0, ZoneId.of("UTC")), "2023-03-15 10:20:30.0"), // ZonedDateTime with +02:00 offset (should be converted to UTC)
        Arguments.of(ZonedDateTime.of(2023, 3, 15, 10, 20, 30, 0, ZoneOffset.ofHours(2)), "2023-03-15 08:20:30.0"), // Long epoch‑millis
        Arguments.of(Long.valueOf(1678879230123L), "2023-03-15T10:20:30.123Z"), // ISO‑8601 string without offset
        Arguments.of("2023-03-15T10:20:30", "2023-03-15 10:20:30.0"), // SQL‑style string with milliseconds
        Arguments.of("2023-03-15 10:20:30.123", "2023-03-15 10:20:30.123"), // ISO‑8601 string with +02:00 offset
        Arguments.of("2023-03-15T10:20:30+02:00", "2023-03-15 08:20:30.0"), // Date‑only ISO‑8601 string
        Arguments.of("2023-03-15", "2023-03-15 00:00:00.0"), // java.sql.Date (date‑only)
        Arguments.of(java.sql.Date.valueOf("2023-03-15"), "2023-03-15 00:00:00.0"), // Calendar in Asia/Tokyo (UTC+9) – should become UTC
        Arguments.of(createTokyoCalendar(2023, Calendar.MARCH, 15, 10, 20, 30, 0), "2023-03-15 01:20:30.0"), // CharBuffer containing ISO‑8601 UTC instant
        Arguments.of(CharBuffer.wrap("2023-03-15T10:20:30Z"), "2023-03-15 10:20:30.0"), // ByteBuffer containing SQL‑style timestamp string
        Arguments.of(ByteBuffer.wrap("2023-03-15 10:20:30.123".getBytes(StandardCharsets.UTF_8)), "2023-03-15 10:20:30.123"), // AtomicLong epoch‑millis
        Arguments.of(new AtomicLong(1678879230123L), "2023-03-15T10:20:30.123Z"), // BigDecimal containing epoch‑millis
        Arguments.of(new BigDecimal("1678879230123"), "2023-03-15T10:20:30.123Z"));
    }

    /**
     * Supplies all inputs that must cause an {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// malformed strings
        Arguments.of("not-a-date"), Arguments.of(""), Arguments.of("   "), Arguments.of("2023/03/15 10:20:30"), Arguments.of("2023-03-15T10:20:30+0200"), // unsupported types
        Arguments.of(UUID.randomUUID()), Arguments.of(new ArrayList<>()), Arguments.of(Integer.valueOf(1678879230123)), Arguments.of(Double.valueOf(1678879230123.0)), Arguments.of(new java.math.BigInteger("1678879230123")), Arguments.of(LocalDate.of(2023, 3, 15)), Arguments.of(OffsetDateTime.of(2023, 3, 15, 10, 20, 30, 0, ZoneOffset.ofHours(2))), Arguments.of(YearMonth.of(2023, 3)), Arguments.of(CharBuffer.wrap("invalid-date")), Arguments.of(ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 })), Arguments.of(new java.util.concurrent.atomic.AtomicInteger(42)), Arguments.of(new java.util.concurrent.atomic.AtomicBoolean(true)), Arguments.of(Collections.singleton(1)), Arguments.of(Collections.emptyMap()), Arguments.of(new Object()));
    }

    /**
     * Creates a {@link Calendar} set to the specified date‑time in the Asia/Tokyo time‑zone.
     */
    private static Calendar createTokyoCalendar(int year, int month, int day, int hour, int minute, int second, int millis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    @Test
    @DisplayName("Convert ISO‑like String to Timestamp")
    void testConvertStringToTimestamp() {
        // Given
        String source = "2023-01-02 12:34:56.789";
        // When
        Timestamp result = Converter.convertToTimestamp(source);
        // Then
        // The Converter parses the string using the default DateFormat (yyyy‑MM‑dd HH:mm:ss[.SSS])
        // Build the expected Timestamp manually.
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 2, 12, 34, 56, 789_000_000);
        Timestamp expected = Timestamp.valueOf(ldt);
        assertSameInstant(expected, result);
    }

    @Test
    @DisplayName("Convert java.util.Date to Timestamp")
    void testConvertDateToTimestamp() {
        // Given
        Date source = new Date(Instant.parse("2023-03-15T08:20:30.123Z").toEpochMilli());
        // When
        Timestamp result = Converter.convertToTimestamp(source);
        // Then
        Timestamp expected = new Timestamp(source.getTime());
        // preserve nanos from the instant
        expected.setNanos(123_000_000);
        assertSameInstant(expected, result);
    }

    @Test
    @DisplayName("Convert epoch‑millis Long to Timestamp")
    void testConvertLongToTimestamp() {
        // Given
        long epochMillis = Instant.parse("2021-07-04T14:15:16.456Z").toEpochMilli();
        Long source = epochMillis;
        // When
        Timestamp result = Converter.convertToTimestamp(source);
        // Then
        Timestamp expected = new Timestamp(epochMillis);
        expected.setNanos(456_000_000);
        assertSameInstant(expected, result);
    }

    @Test
    @DisplayName("Convert LocalDateTime to Timestamp")
    void testConvertLocalDateTimeToTimestamp() {
        // Given
        LocalDateTime source = LocalDateTime.of(2022, 12, 31, 23, 59, 59, 999_000_000);
        // When
        Timestamp result = Converter.convertToTimestamp(source);
        // Then
        Timestamp expected = Timestamp.valueOf(source);
        assertSameInstant(expected, result);
    }

    @Test
    @DisplayName("Convert Calendar to Timestamp")
    void testConvertCalendarToTimestamp() {
        // Given
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2020, Calendar.FEBRUARY, 29, 10, 20, 30);
        cal.set(Calendar.MILLISECOND, 250);
        // Calendar stores milliseconds; nanos are zero.
        Date source = cal.getTime();
        // When
        Timestamp result = Converter.convertToTimestamp(cal);
        // Then
        Timestamp expected = new Timestamp(source.getTime());
        expected.setNanos(250_000_000);
        assertSameInstant(expected, result);
    }

    @Test
    @DisplayName("Convert null to Timestamp returns null")
    void testConvertNullReturnsNull() {
        // Given
        Object source = null;
        // When
        Timestamp result = Converter.convertToTimestamp(source);
        // Then
        assertNull(result, "Conversion of null should return null");
    }

    @ParameterizedTest(name = "[{index}] convertToTimestamp({0}) → {1}")
    @MethodSource("validConversionProvider")
    void testValidConversions(Object source, String expectedString) {
        Timestamp result = Converter.convertToTimestamp(source);
        if (expectedString == null) {
            assertNull(result, "Expected null for source: " + source);
        } else {
            Timestamp expected = parseExpected(expectedString);
            assertSameInstant(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] convertToTimestamp({0}) throws IllegalArgumentException")
    @MethodSource("invalidConversionProvider")
    void testInvalidConversions(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToTimestamp(source), "Expected IllegalArgumentException for source: " + source);
    }
}
