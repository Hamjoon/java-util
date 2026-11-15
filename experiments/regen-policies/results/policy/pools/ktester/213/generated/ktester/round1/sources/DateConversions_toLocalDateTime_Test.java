package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toLocalDateTime_Test {

    /**
     * Shared {@link Converter} used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter is fully functional out of the box.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // help GC, not strictly required
        converter = null;
    }

    private static Stream<Arguments> conversionScenarios() {
        ZoneId systemZone = ZoneId.systemDefault();
        return Stream.of(// 1. ZonedDateTime (explicit zone)
        Arguments.of(ZonedDateTime.parse("2023-05-15T10:15:30+02:00[Europe/Paris]"), LocalDateTime.of(2023, 5, 15, 10, 15, 30), "Direct conversion from a ZonedDateTime should return its LocalDateTime part."), // 2. OffsetDateTime (system default zone applied)
        Arguments.of(OffsetDateTime.parse("2023-05-15T10:15:30+02:00"), LocalDateTime.of(2023, 5, 15, 10, 15, 30), "OffsetDateTime is first converted to ZonedDateTime (using system default zone) then to LocalDateTime."), // 3. Instant (system default zone applied)
        Arguments.of(Instant.parse("2023-05-15T08:15:30Z"), LocalDateTime.ofInstant(Instant.parse("2023-05-15T08:15:30Z"), systemZone), "Instant is converted to ZonedDateTime using system default zone, yielding the expected LocalDateTime."), // 4. java.util.Date (via Instant)
        Arguments.of(Date.from(Instant.parse("2023-05-15T08:15:30Z")), LocalDateTime.ofInstant(Instant.parse("2023-05-15T08:15:30Z"), systemZone), "java.util.Date is converted via Instant to ZonedDateTime then to LocalDateTime."), // 5. java.sql.Timestamp (preserve nanoseconds)
        Arguments.of(Timestamp.valueOf("2023-05-15 10:15:30.123"), LocalDateTime.of(2023, 5, 15, 10, 15, 30, 123_000_000), "Timestamp contains nanoseconds; conversion should preserve the fractional second."), // 6. ISO_LOCAL_DATE_TIME string
        Arguments.of("2023-05-15T10:15:30", LocalDateTime.of(2023, 5, 15, 10, 15, 30), "String in ISO_LOCAL_DATE_TIME format should be parsed to LocalDateTime."), // 7. Space‑separated date‑time string (fallback format)
        Arguments.of("2023-05-15 10:15:30", LocalDateTime.of(2023, 5, 15, 10, 15, 30), "String with space separator should also be parsed (fallback format)."), // 8. Null input → null result
        Arguments.of(null, null, "Null input should result in a null LocalDateTime (toZonedDateTime returns null)."), // 9. Long epoch milliseconds
        Arguments.of(1_234_567_890L, LocalDateTime.ofInstant(Instant.ofEpochMilli(1_234_567_890L), systemZone), "Long epoch milliseconds should be converted to ZonedDateTime then to LocalDateTime."), // 10. GregorianCalendar instance
        Arguments.of(new GregorianCalendar(2023, Calendar.MAY, 15, 10, 15, 30), LocalDateTime.ofInstant(Instant.ofEpochMilli(new GregorianCalendar(2023, Calendar.MAY, 15, 10, 15, 30).getTimeInMillis()), systemZone), "Calendar instance should be converted via Instant to ZonedDateTime then to LocalDateTime."));
    }

    private static Stream<Arguments> edgeCaseScenarios() {
        ZoneId systemZone = ZoneId.systemDefault();
        return Stream.of(// 1. Passing a LocalDateTime returns the same instance (fields unchanged)
        Arguments.of(LocalDateTime.of(2023, 5, 15, 10, 15, 30), LocalDateTime.of(2023, 5, 15, 10, 15, 30), "Passing a LocalDateTime should return the same value (system default zone does not alter the fields)."), // 2. java.sql.Date → midnight of that date
        Arguments.of(java.sql.Date.valueOf("2023-05-15"), LocalDateTime.of(2023, 5, 15, 0, 0), "java.sql.Date represents a date without time; conversion should yield midnight of that date."), // 3. String with explicit offset
        Arguments.of("2023-05-15T10:15:30+02:00", LocalDateTime.of(2023, 5, 15, 10, 15, 30), "String with an explicit offset is parsed, the offset is applied to obtain the correct instant, then the LocalDateTime part is returned."), // 4. ISO string with milliseconds
        Arguments.of("2023-05-15T10:15:30.123", LocalDateTime.of(2023, 5, 15, 10, 15, 30, 123_000_000), "ISO string with milliseconds should preserve the fractional second as nanoseconds."), // 5. Epoch millisecond value 0 → Unix epoch in system zone
        Arguments.of(0L, LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), systemZone), "Epoch millisecond value 0 corresponds to the Unix epoch; conversion should respect the system default zone."), // 6. Negative epoch milliseconds
        Arguments.of(-1_234_567_890L, LocalDateTime.ofInstant(Instant.ofEpochMilli(-1_234_567_890L), systemZone), "Negative epoch milliseconds represent a time before 1970‑01‑01; conversion must handle negative values correctly."), // 7. AtomicLong containing epoch milliseconds
        Arguments.of(new AtomicLong(1_234_567_890L), LocalDateTime.ofInstant(Instant.ofEpochMilli(1_234_567_890L), systemZone), "AtomicLong containing epoch milliseconds should be treated the same as a plain Long."));
    }

    private static Stream<Arguments> exceptionScenarios() {
        return Stream.of(// 1. Unsupported Boolean type
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Unsupported source type Boolean should cause IllegalArgumentException."), // 2. Invalid month (13) – parsing failure
        Arguments.of("2023-13-01T00:00:00", DateTimeParseException.class, "String with an impossible month (13) cannot be parsed and must raise DateTimeParseException."), // 3. Completely non‑date string
        Arguments.of("not-a-date", DateTimeParseException.class, "Completely non‑date string should trigger a parsing failure."), // 4. LocalTime (time‑only) – not convertible
        Arguments.of(LocalTime.of(10, 15, 30), IllegalArgumentException.class, "LocalTime (time‑only) is not convertible to LocalDateTime and should raise IllegalArgumentException."), // 5. YearMonth – missing day/time
        Arguments.of(YearMonth.of(2023, 5), IllegalArgumentException.class, "YearMonth lacks day and time information; conversion is unsupported and must throw IllegalArgumentException."), // 6. Arbitrary Map
        Arguments.of(new HashMap<String, Object>(), IllegalArgumentException.class, "Arbitrary Map objects are not valid date/time sources and should result in IllegalArgumentException."));
    }

    @Test
    void convertsZonedDateTimeToLocalDateTime() {
        // GIVEN a ZonedDateTime in UTC
        ZonedDateTime zoned = ZonedDateTime.of(2022, 5, 10, 15, 30, 45, 0, ZoneId.of("UTC"));
        // WHEN converting to LocalDateTime via DateConversions
        LocalDateTime result = DateConversions.toLocalDateTime(zoned, converter);
        // THEN the zone information is stripped and the date‑time components are preserved
        assertEquals(LocalDateTime.of(2022, 5, 10, 15, 30, 45), result);
    }

    @Test
    void convertsIsoStringToLocalDateTime() {
        // GIVEN an ISO‑8601 string that represents an instant in UTC
        String isoString = "2022-05-10T15:30:45Z";
        // WHEN converting the string (the Converter will first turn it into a ZonedDateTime)
        LocalDateTime result = DateConversions.toLocalDateTime(isoString, converter);
        // THEN the resulting LocalDateTime matches the instant expressed in the string
        assertEquals(LocalDateTime.of(2022, 5, 10, 15, 30, 45), result);
    }

    @Test
    void throwsExceptionForUnsupportedSource() {
        // GIVEN a source type that the Converter cannot turn into a ZonedDateTime
        Integer unsupported = 12345;
        // WHEN/THEN an IllegalArgumentException (or a subclass) is thrown
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toLocalDateTime(unsupported, converter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("conversionScenarios")
    void conversionScenarios(Object from, LocalDateTime expected, String description) {
        LocalDateTime actual = DateConversions.toLocalDateTime(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("edgeCaseScenarios")
    void edgeCaseScenarios(Object from, LocalDateTime expected, String description) {
        LocalDateTime actual = DateConversions.toLocalDateTime(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionScenarios")
    void exceptionScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> DateConversions.toLocalDateTime(from, converter), description);
    }
}
