package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLocalDateTime_Test {

    /**
     * The Converter instance required by the method under test.
     *  It is not used by the current implementation of {@code toLocalDateTime},
     *  but the signature requires it, so we provide a mock.
     */
    @Mock
    private Converter mockConverter;

    /**
     * The real converter used by the library.  It is required for most conversions.
     */
    private static final Converter DEFAULT_CONVERTER = DefaultConverter.INSTANCE;

    /**
     * Preserve the original default zone so we can restore it after the suite.
     */
    private static TimeZone originalDefaultZone;

    @BeforeAll
    static void setUpAll() {
        // Force UTC to make expectations independent of the machine's zone.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void tearDownAll() {
        // Restore the original default zone.
        TimeZone.setDefault(originalDefaultZone);
    }

    private static Stream<Arguments> validConversionProvider() {
        Instant instant = Instant.parse("2023-05-15T10:20:30Z");
        ZoneId utc = ZoneOffset.UTC;
        // Helper to convert an Instant to the expected LocalDateTime in UTC.
        LocalDateTime utcLdt = LocalDateTime.ofInstant(instant, utc);
        return Stream.of(Arguments.of("Instant should be converted to the same instant in the system default zone (UTC)", instant, utcLdt), Arguments.of("java.util.Date (epoch‑millis) should be interpreted as an instant", Date.from(instant), utcLdt), Arguments.of("Calendar with UTC zone should be converted to the same LocalDateTime", calendarWithZone(utc, instant), utcLdt), Arguments.of("Long representing epoch milliseconds should be converted to LocalDateTime", Long.valueOf(instant.toEpochMilli()), utcLdt), Arguments.of("ISO‑8601 string without zone should be parsed as a LocalDateTime", "2023-05-15T10:20:30", LocalDateTime.of(2023, 5, 15, 10, 20, 30)), Arguments.of("ZonedDateTime should lose its zone information and return the underlying LocalDateTime", ZonedDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.UTC), LocalDateTime.of(2023, 5, 15, 10, 20, 30)), Arguments.of("OffsetDateTime should be normalized to UTC then converted to LocalDateTime (offset applied)", OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.ofHours(2)), // 10:20:30+02:00 => 08:20:30Z
        LocalDateTime.of(2023, 5, 15, 8, 20, 30)), Arguments.of("When the source is already a LocalDateTime, the method should return it unchanged", LocalDateTime.of(2023, 5, 15, 10, 20, 30), LocalDateTime.of(2023, 5, 15, 10, 20, 30)), Arguments.of("Epoch instant should be converted to the equivalent LocalDateTime in UTC", Instant.EPOCH, LocalDateTime.ofInstant(Instant.EPOCH, utc)), Arguments.of("Negative epoch second (one second before the epoch) must be handled correctly", Instant.ofEpochSecond(-1), LocalDateTime.ofInstant(Instant.ofEpochSecond(-1), utc)), Arguments.of("java.sql.Timestamp should be converted to the same LocalDateTime", Timestamp.valueOf("2023-05-15 10:20:30"), LocalDateTime.of(2023, 5, 15, 10, 20, 30)), Arguments.of("java.sql.Date should yield midnight of that date", java.sql.Date.valueOf("2023-05-15"), LocalDateTime.of(2023, 5, 15, 0, 0, 0)), Arguments.of("BigDecimal containing epoch‑millis should be interpreted as an instant", new BigDecimal(instant.toEpochMilli()), utcLdt), Arguments.of("BigInteger containing epoch‑millis should be interpreted as an instant", new BigInteger(Long.toString(instant.toEpochMilli())), utcLdt), Arguments.of("AtomicLong holding epoch‑millis must be converted to the correct LocalDateTime", new AtomicLong(instant.toEpochMilli()), utcLdt), Arguments.of("LocalDate should be converted to a LocalDateTime at midnight", LocalDate.of(2023, 5, 15), LocalDateTime.of(2023, 5, 15, 0, 0, 0)), Arguments.of("Calendar in a non‑UTC zone but representing the same instant should still produce UTC‑based LocalDateTime", calendarWithZone(TimeZone.getTimeZone("America/New_York").toZoneId(), instant), utcLdt));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("String not following ISO‑8601 format should cause DateTimeParseException", "2023/05/15 10:20:30"), Arguments.of("Completely unparsable string must be rejected with DateTimeParseException", "not-a-date"), Arguments.of("Empty string cannot be parsed as a date‑time", ""), Arguments.of("String containing only whitespace is not a valid ISO‑8601 representation", "   "), Arguments.of("Impossible date (Feb 30) must be rejected with DateTimeParseException", "2023-02-30T10:20:30"));
    }

    private static Stream<Arguments> numericOverflowProvider() {
        return Stream.of(Arguments.of("Epoch‑millis value larger than the maximum supported instant should cause DateTimeException", Long.MAX_VALUE), Arguments.of("BigDecimal containing an epoch‑millis value that overflows the Instant range must raise DateTimeException", new BigDecimal(Long.MAX_VALUE)), Arguments.of("BigInteger with a value beyond the supported Instant range should result in DateTimeException", new BigInteger(Long.toString(Long.MAX_VALUE))));
    }

    /**
     * Creates a {@link Calendar} set to the supplied {@link Instant} and zone.
     */
    private static Calendar calendarWithZone(ZoneId zone, Instant instant) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zone));
        cal.setTime(Date.from(instant));
        return cal;
    }

    @Test
    void toLocalDateTime_givenInstant_returnsEquivalentLocalDateTime() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-15T12:34:56.789Z");
        // WHEN
        LocalDateTime result = InstantConversions.toLocalDateTime(instant, mockConverter);
        // THEN
        // The implementation delegates to toZonedDateTime which, for an Instant,
        // creates a ZonedDateTime in the system default zone.  To make the test
        // deterministic we compute the expected value using the same zone.
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = instant.atZone(systemZone).toLocalDateTime();
        assertEquals(expected, result, "The LocalDateTime produced from an Instant should match the system default zone conversion.");
    }

    @Test
    void toLocalDateTime_givenZonedDateTime_returnsSameLocalDateTime() {
        // GIVEN
        ZonedDateTime zoned = ZonedDateTime.of(2022, 11, 5, 8, 15, 30, 0, ZoneId.of("America/New_York"));
        // WHEN
        LocalDateTime result = InstantConversions.toLocalDateTime(zoned, mockConverter);
        // THEN
        assertEquals(zoned.toLocalDateTime(), result, "When the source is a ZonedDateTime the method should return its LocalDateTime part.");
    }

    @Test
    void toLocalDateTime_givenLocalDateTime_returnsSameInstance() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, 5, 20, 14, 45, 10);
        // WHEN
        LocalDateTime result = InstantConversions.toLocalDateTime(ldt, mockConverter);
        // THEN
        assertEquals(ldt, result, "When the source is already a LocalDateTime it should be returned unchanged.");
    }

    @Test
    void toLocalDateTime_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalDateTime result = InstantConversions.toLocalDateTime(source, mockConverter);
        // THEN
        assertNull(result, "A null source should result in a null LocalDateTime.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toLocalDateTime_validConversions(String description, Object source, LocalDateTime expected) {
        LocalDateTime result = InstantConversions.toLocalDateTime(source, DEFAULT_CONVERTER);
        assertEquals(expected, result, description);
    }

    @Test
    void toLocalDateTime_nullSource_throwsNPE() {
        assertThrows(NullPointerException.class, () -> InstantConversions.toLocalDateTime(null, DEFAULT_CONVERTER), "Passing a null source object should result in a NullPointerException.");
    }

    @Test
    void toLocalDateTime_nullConverter_throwsNPE() {
        Instant instant = Instant.parse("2023-05-15T10:20:30Z");
        assertThrows(NullPointerException.class, () -> InstantConversions.toLocalDateTime(instant, null), "Passing a null converter should result in a NullPointerException.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidStringProvider")
    void toLocalDateTime_invalidStringFormats(String description, String source) {
        assertThrows(DateTimeParseException.class, () -> InstantConversions.toLocalDateTime(source, DEFAULT_CONVERTER), description);
    }

    @Test
    void toLocalDateTime_localTimeOnly_throwsIllegalArgumentException() {
        LocalTime timeOnly = LocalTime.of(10, 20, 30);
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDateTime(timeOnly, DEFAULT_CONVERTER), "LocalTime alone does not contain a date component; conversion should fail.");
    }

    @Test
    void toLocalDateTime_unsupportedObject_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDateTime(unsupported, DEFAULT_CONVERTER), "Unsupported source type must result in IllegalArgumentException.");
    }

    @Test
    void toLocalDateTime_booleanWrapper_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDateTime(Boolean.TRUE, DEFAULT_CONVERTER), "Boolean is not a supported source type for conversion to LocalDateTime.");
    }

    @Test
    void toLocalDateTime_fractionalNumeric_throwsIllegalArgumentException() {
        BigDecimal fractional = new BigDecimal("1621081230.5");
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDateTime(fractional, DEFAULT_CONVERTER), "Fractional numeric values cannot represent a whole epoch‑millis instant.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericOverflowProvider")
    void toLocalDateTime_numericOverflow(String description, Number source) {
        assertThrows(DateTimeException.class, () -> InstantConversions.toLocalDateTime(source, DEFAULT_CONVERTER), description);
    }
}
