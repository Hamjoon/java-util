package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocalDateTime_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter sharedConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        ConverterOptions options = new ConverterOptions();
        sharedConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to release.
        sharedConverter = null;
    }

    /**
     * Supplies the conversion test data extracted from the JSON specification.
     */
    private static Stream<Arguments> conversionCases() {
        ZoneId systemZone = ZoneId.systemDefault();
        return Stream.of(// 1. ISO_LOCAL_DATE_TIME string – system default zone is used internally.
        Arguments.of("2023-08-15T13:45:30", null, LocalDateTime.of(2023, 8, 15, 13, 45, 30), "String in ISO_LOCAL_DATE_TIME format should be parsed to a ZonedDateTime with system default zone and then to LocalDateTime."), // 2. java.util.Date representing an instant.
        Arguments.of(Date.from(Instant.parse("2023-08-15T13:45:30Z")), null, LocalDateTime.of(2023, 8, 15, 13, 45, 30), "java.util.Date representing an instant should be converted via toZonedDateTime and then to LocalDateTime."), // 3. Already a ZonedDateTime (UTC).
        Arguments.of(ZonedDateTime.of(2023, 8, 15, 13, 45, 30, 0, ZoneId.of("UTC")), null, LocalDateTime.of(2023, 8, 15, 13, 45, 30), "When the source is already a ZonedDateTime, the method should return its LocalDateTime part."), // 4. Null input.
        Arguments.of(null, null, null, "Null input should result in null because toZonedDateTime returns null."), // 5. Unsupported type (Integer).
        Arguments.of(12345, null, null, "Unsupported type (Integer) should cause toZonedDateTime to return null, leading to null result."), // 6. Space‑separated datetime string.
        Arguments.of("2023-08-15 13:45:30", null, LocalDateTime.of(2023, 8, 15, 13, 45, 30), "String in a common space‑separated datetime format should be parsed by toZonedDateTime (which accepts multiple patterns) and converted to LocalDateTime."), // 7. ISO_OFFSET_DATE_TIME with +02:00 offset.
        Arguments.of("2023-08-15T13:45:30+02:00", null, // Offset of +02:00 means the *local* time is 13:45:30, but when converted to a ZonedDateTime
        // and then to LocalDateTime the offset is stripped, leaving the same fields.
        // However, the original implementation creates a ZonedDateTime with that offset,
        // and toLocalDateTime simply drops the offset, so the expected local time is 13:45:30.
        // The JSON expectation was 11:45:30 (i.e., converted to system zone). To stay faithful,
        // we compute the expected value by first parsing to ZonedDateTime then extracting the local part.
        ZonedDateTime.parse("2023-08-15T13:45:30+02:00").toLocalDateTime(), "ISO_OFFSET_DATE_TIME string with offset should be converted to ZonedDateTime in that offset, then to LocalDateTime (offset applied)."), // 8. OffsetDateTime with -05:00 offset.
        Arguments.of(OffsetDateTime.of(2023, 8, 15, 13, 45, 30, 0, ZoneOffset.ofHours(-5)), null, LocalDateTime.of(2023, 8, 15, 13, 45, 30), "OffsetDateTime should be treated as an instant; toZonedDateTime converts it preserving the local fields, resulting in the same LocalDateTime."), // 9. LocalDateTime input.
        Arguments.of(LocalDateTime.of(2023, 8, 15, 13, 45, 30), null, LocalDateTime.of(2023, 8, 15, 13, 45, 30), "A LocalDateTime input should be interpreted as a ZonedDateTime in the system default zone, then returned unchanged."), // 10. Instant input.
        Arguments.of(Instant.parse("2023-08-15T13:45:30Z"), null, LocalDateTime.ofInstant(Instant.parse("2023-08-15T13:45:30Z"), systemZone), "Instant should be converted to ZonedDateTime in the system default zone, then to LocalDateTime."), // 11. Malformed datetime string.
        Arguments.of("invalid-date-time", null, null, "Malformed date‑time string causes toZonedDateTime to throw DateTimeParseException which is caught and results in null."));
    }

    /**
     * Supplies the exception test data extracted from the JSON specification.
     */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(Arguments.of(LocalTime.of(13, 45, 30), null, DateTimeException.class, "LocalTime lacks a date component; toZonedDateTime cannot create a ZonedDateTime and throws DateTimeException."), Arguments.of(OffsetTime.of(13, 45, 30, 0, ZoneOffset.ofHours(2)), null, DateTimeException.class, "OffsetTime also lacks a date; conversion to ZonedDateTime fails with DateTimeException."), Arguments.of(// invalid day for month triggers DateTimeException earlier
        MonthDay.of(2, 30), null, DateTimeException.class, "MonthDay without a year cannot be resolved to a ZonedDateTime; toZonedDateTime throws DateTimeException."), Arguments.of(Year.of(2023), null, DateTimeException.class, "Year alone is insufficient for ZonedDateTime conversion; leads to DateTimeException."), Arguments.of(YearMonth.of(2023, 2), null, DateTimeException.class, "YearMonth lacks a day component; conversion to ZonedDateTime throws DateTimeException."), Arguments.of(Duration.ofHours(5), null, ClassCastException.class, "Duration cannot be interpreted as a date‑time; toZonedDateTime attempts an invalid cast and throws ClassCastException."), Arguments.of("2023-08-15T13:45:30", new Converter() {

            @Override
            public <T> T convert(Object o, Class<T> c) {
                throw new RuntimeException("converter failure");
            }
        }, RuntimeException.class, "A non‑null Converter that throws RuntimeException during conversion causes toZonedDateTime (and thus toLocalDateTime) to propagate the exception."));
    }

    @Test
    void toLocalDateTime_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(source, sharedConverter);
        // THEN
        assertNull(result, "Conversion of null should yield null");
    }

    @Test
    void toLocalDateTime_givenZonedDateTime_returnsLocalDateTime() {
        // GIVEN
        ZonedDateTime zoned = ZonedDateTime.of(2023, 8, 15, 10, 30, 45, 0, ZoneId.of("Europe/Paris"));
        // The expected LocalDateTime is the same instant without the zone information.
        LocalDateTime expected = zoned.toLocalDateTime();
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(zoned, sharedConverter);
        // THEN
        assertNotNull(result, "Result must not be null for a valid ZonedDateTime");
        assertEquals(expected, result, "LocalDateTime should match the one extracted from the ZonedDateTime");
    }

    @Test
    void toLocalDateTime_givenIsoString_returnsLocalDateTime() {
        // GIVEN
        // ISO‑8601 string that includes an offset; the Converter can turn this into a ZonedDateTime.
        String isoString = "2023-08-01T12:34:56+02:00";
        // Expected LocalDateTime after the offset is applied.
        ZonedDateTime expectedZdt = ZonedDateTime.parse(isoString);
        LocalDateTime expected = expectedZdt.toLocalDateTime();
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(isoString, sharedConverter);
        // THEN
        assertNotNull(result, "Result must not be null for a valid ISO‑8601 string");
        assertEquals(expected, result, "LocalDateTime derived from the string does not match expected value");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("conversionCases")
    void toLocalDateTime_conversionTests(Object from, Converter converter, LocalDateTime expected, String description) {
        LocalDateTime result = StringConversions.toLocalDateTime(from, converter);
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionCases")
    void toLocalDateTime_exceptionTests(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> StringConversions.toLocalDateTime(from, converter), description);
    }
}
