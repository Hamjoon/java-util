package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToTimestamp_Test {

    /**
     * Holds the original default time‑zone so we can restore it after the tests.
     */
    private static TimeZone originalTimeZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure a deterministic zone for all conversions that rely on the default zone.
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time‑zone.
        TimeZone.setDefault(originalTimeZone);
    }

    private static Stream<Arguments> validConversionsProvider() {
        // Helper constants
        // 2023‑01‑01 00:00:00 UTC
        long epochMillis2023 = 1_672_531_200_000L;
        Timestamp tsExact = Timestamp.valueOf("2023-01-01 00:00:00.000");
        Timestamp tsWithMillis = Timestamp.valueOf("2023-01-01 00:00:00.123");
        Timestamp tsWithNanos = Timestamp.valueOf("2023-01-01 00:00:00.999999999");
        Timestamp tsNegative = Timestamp.valueOf("1969-12-31 00:00:00.000");
        // Calendar in UTC
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(epochMillis2023);
        // Calendar in America/Los_Angeles (same instant)
        Calendar laCal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        laCal.setTimeInMillis(epochMillis2023);
        // ZonedDateTime in New York (UTC‑5)
        ZonedDateTime nyZdt = ZonedDateTime.of(2023, 1, 1, 12, 30, 45, 123_000_000, ZoneId.of("America/New_York"));
        return Stream.of(// Already a Timestamp (same instance)
        Arguments.of(tsExact, tsExact), // java.util.Date
        Arguments.of(new Date(epochMillis2023), tsExact), // java.util.Calendar (UTC)
        Arguments.of(utcCal, tsExact), // Long epoch millis
        Arguments.of(Long.valueOf(epochMillis2023), tsExact), // ISO‑8601 UTC string
        Arguments.of("2023-01-01T00:00:00.000Z", tsExact), // LocalDateTime interpreted in default zone (UTC)
        Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0, 0, 0), tsExact), // LocalDate (midnight)
        Arguments.of(LocalDate.of(2023, 1, 1), tsExact), // ZonedDateTime UTC
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), tsExact), // BigDecimal containing epoch millis
        Arguments.of(new BigDecimal("1672531200000"), tsExact), // java.sql.Date (date‑only)
        Arguments.of(java.sql.Date.valueOf("2023-01-01"), tsExact), // Instant with millisecond precision
        Arguments.of(Instant.ofEpochMilli(1_672_531_200_123L), tsWithMillis), // OffsetDateTime with -05:00 offset (adds 5 hours)
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5)), tsExact.plusSeconds(5 * 3600)), // ZonedDateTime America/New_York (UTC‑5) with nanos
        Arguments.of(nyZdt, Timestamp.valueOf("2023-01-01 17:30:45.123")), // BigInteger containing epoch millis
        Arguments.of(new BigInteger("1672531200000"), tsExact), // Calendar in America/Los_Angeles (same instant as UTC)
        Arguments.of(laCal, tsExact), // LocalDateTime with nanoseconds (truncated to millis)
        Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0, 0, 123_456_789), tsWithMillis), // Timestamp with full nanosecond precision (should be preserved)
        Arguments.of(tsWithNanos, tsWithNanos), // Negative epoch millis (one day before epoch)
        Arguments.of(Long.valueOf(-86_400_000L), tsNegative));
    }

    private static Stream<Arguments> invalidConversionsProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, IllegalArgumentException.class), Arguments.of(new Object(), IllegalArgumentException.class), Arguments.of("", IllegalArgumentException.class), Arguments.of("not-a-date", IllegalArgumentException.class), Arguments.of(LocalTime.of(12, 34, 56), IllegalArgumentException.class), Arguments.of(UUID.randomUUID(), IllegalArgumentException.class), Arguments.of(Float.valueOf(123.45f), IllegalArgumentException.class), Arguments.of(Optional.empty(), IllegalArgumentException.class), Arguments.of(Year.of(2023), IllegalArgumentException.class), Arguments.of(Month.APRIL, IllegalArgumentException.class), Arguments.of(new int[] { 2023, 1, 1 }, IllegalArgumentException.class), Arguments.of(Collections.emptyList(), IllegalArgumentException.class), Arguments.of(Map.of("year", 2023, "month", 1, "day", 1), IllegalArgumentException.class), Arguments.of(Duration.ofHours(5), IllegalArgumentException.class), Arguments.of(Period.ofDays(1), IllegalArgumentException.class), Arguments.of(Character.valueOf('A'), IllegalArgumentException.class));
    }

    @Test
    void convertNullReturnsNull() {
        Timestamp ts = Converter.convertToTimestamp(null);
        assertNull(ts, "Converting a null value should return null");
    }

    @Test
    void convertFromSqlTimestampReturnsSameInstance() {
        // 2021‑10‑01 00:00:00 UTC
        Timestamp original = new Timestamp(1_632_024_800_000L);
        Timestamp result = Converter.convertToTimestamp(original);
        assertSame(original, result, "When the source is already a Timestamp the same instance should be returned");
    }

    @Test
    void convertFromUtilDate() {
        // 2021‑10‑01 00:00:00 UTC
        Date date = new Date(1_632_024_800_000L);
        Timestamp result = Converter.convertToTimestamp(date);
        assertNotNull(result);
        assertEquals(date.getTime(), result.getTime(), "Timestamp should represent the same instant as the java.util.Date source");
    }

    @Test
    void convertFromLongEpochMillis() {
        // 2021‑10‑01 00:00:00 UTC
        Long epochMillis = 1_632_024_800_000L;
        Timestamp result = Converter.convertToTimestamp(epochMillis);
        assertNotNull(result);
        assertEquals(epochMillis.longValue(), result.getTime(), "Timestamp should represent the same instant as the supplied epoch milliseconds");
    }

    @Test
    void convertFromLocalDateTime() {
        // LocalDateTime has no zone, the Converter uses the default zone (UTC in our setup)
        LocalDateTime ldt = LocalDateTime.of(2021, 10, 1, 0, 0, 0);
        Timestamp result = Converter.convertToTimestamp(ldt);
        assertNotNull(result);
        long expected = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, result.getTime(), "Timestamp should represent the same instant as the supplied LocalDateTime in the default zone");
    }

    @Test
    void convertFromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 10, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        Timestamp result = Converter.convertToTimestamp(zdt);
        assertNotNull(result);
        long expected = zdt.toInstant().toEpochMilli();
        assertEquals(expected, result.getTime(), "Timestamp should represent the same instant as the supplied ZonedDateTime");
    }

    @Test
    void convertFromCalendar() {
        // Calendar respects its own time‑zone; we create one in UTC for determinism.
        java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2021‑10‑01 00:00:00 UTC
        cal.setTimeInMillis(1_632_024_800_000L);
        Timestamp result = Converter.convertToTimestamp(cal);
        assertNotNull(result);
        assertEquals(cal.getTimeInMillis(), result.getTime(), "Timestamp should represent the same instant as the supplied Calendar");
    }

    /**
     * Valid conversion scenarios – each entry supplies the source object and the
     * expected Timestamp (including nanosecond precision where applicable).
     */
    @ParameterizedTest(name = "{index}: convert {0} → {1}")
    @MethodSource("validConversionsProvider")
    void convertValid(Object source, Timestamp expected) {
        Timestamp result = Converter.convertToTimestamp(source);
        assertNotNull(result, "Result should not be null for valid conversion");
        assertEquals(expected.getTime(), result.getTime(), "Instant (millis) does not match the expected value");
        assertEquals(expected.getNanos(), result.getNanos(), "Nanosecond component does not match the expected value");
    }

    /**
     * Invalid conversion scenarios – the method is expected to throw the
     * supplied exception type (always IllegalArgumentException for the current
     * test data).
     */
    @ParameterizedTest(name = "{index}: convert {0} throws {1}")
    @MethodSource("invalidConversionsProvider")
    void convertInvalid(Object source, Class<? extends Exception> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToTimestamp(source), "Expected conversion to fail with " + expectedException.getSimpleName());
    }
}
