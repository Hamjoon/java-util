package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toOffsetDateTime_Test {

    /**
     * Shared Converter used by all tests.
     */
    private static Converter converter;

    /**
     * Preserve the original default time‑zone so we can restore it after the suite.
     */
    private static TimeZone originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter – the implementation under test uses it for
        // delegating to other conversion utilities.
        converter = new Converter(new ConverterOptions());
        // Force a deterministic zone for all tests (UTC). This makes the expected
        // OffsetDateTime values independent of the machine on which the tests run.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone to avoid side‑effects for other test suites.
        TimeZone.setDefault(originalDefaultZone);
        converter = null;
    }

    private static Stream<Arguments> provideConversionBranchCases() {
        // 1. ZonedDateTime – should keep the same instant & offset.
        ZonedDateTime zdt = ZonedDateTime.parse("2023-05-01T12:34:56+02:00[Europe/Paris]");
        OffsetDateTime expectedFromZdt = OffsetDateTime.ofInstant(zdt.toInstant(), zdt.getOffset());
        // 2. Instant – interpreted as UTC.
        Instant instant = Instant.parse("2023-05-01T10:34:56Z");
        OffsetDateTime expectedFromInstant = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        // 3. java.util.Date – uses system default zone (UTC in our test setup).
        Date date = Date.from(Instant.parse("2023-05-01T10:34:56Z"));
        OffsetDateTime expectedFromDate = OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        // 4. java.sql.Timestamp – treated as a local date‑time in the default zone.
        Timestamp ts = Timestamp.valueOf("2023-05-01 12:34:56");
        LocalDateTime ldtFromTs = ts.toLocalDateTime();
        OffsetDateTime expectedFromTimestamp = OffsetDateTime.ofLocal(ldtFromTs, ZoneId.systemDefault(), null);
        // 5. LocalDateTime – same handling as Timestamp.
        LocalDateTime ldt = LocalDateTime.parse("2023-05-01T12:34:56");
        OffsetDateTime expectedFromLocalDateTime = OffsetDateTime.ofLocal(ldt, ZoneId.systemDefault(), null);
        // 6. OffsetDateTime – should be returned unchanged.
        OffsetDateTime odt = OffsetDateTime.parse("2023-05-01T12:34:56+03:00");
        OffsetDateTime expectedFromOffsetDateTime = odt;
        // 7. ISO‑8601 string – parsed to a ZonedDateTime then to OffsetDateTime.
        String isoString = "2023-05-01T12:34:56+02:00";
        OffsetDateTime expectedFromString = OffsetDateTime.parse(isoString);
        return Stream.of(arguments(zdt, expectedFromZdt, "ZonedDateTime source"), arguments(instant, expectedFromInstant, "Instant source"), arguments(date, expectedFromDate, "java.util.Date source"), arguments(ts, expectedFromTimestamp, "java.sql.Timestamp source"), arguments(ldt, expectedFromLocalDateTime, "LocalDateTime source"), arguments(odt, expectedFromOffsetDateTime, "OffsetDateTime source"), arguments(isoString, expectedFromString, "ISO‑8601 String source"));
    }

    private static Stream<Arguments> provideUnsupportedSourceCases() {
        return Stream.of(arguments(Boolean.TRUE, "Boolean source should be unsupported"), arguments(BigDecimal.valueOf(12345), "BigDecimal source should be unsupported"));
    }

    private static Stream<Arguments> provideMalformedStringCases() {
        return Stream.of(arguments("2023-13-40T25:61:61+99:99", "Completely malformed ISO‑8601 string"), arguments("not-a-date-time", "Non‑date string"));
    }

    /**
     * Verify that converting an {@link OffsetDateTime} returns an equal value.
     */
    @Test
    void shouldConvertOffsetDateTimeDirectly() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000, ZoneOffset.ofHours(-4));
        // WHEN
        OffsetDateTime result = BigDecimalConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(source, result, "Converted OffsetDateTime should be equal to the source");
    }

    /**
     * Verify that a {@link ZonedDateTime} is converted to an {@link OffsetDateTime}
     * with the same instant and offset.
     */
    @Test
    void shouldConvertZonedDateTimeToOffsetDateTime() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, // UTC+1 (or +2 with DST)
        ZoneId.of("Europe/Paris"));
        // WHEN
        OffsetDateTime result = BigDecimalConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        // The instant must be identical.
        assertEquals(source.toInstant(), result.toInstant(), "Instant of the converted OffsetDateTime must match the source");
        // The offset must match the source's offset at that instant.
        assertEquals(source.getOffset(), result.getOffset(), "Offset of the converted OffsetDateTime must match the source");
    }

    /**
     * Verify that a {@link Date} (which represents an instant in UTC) is converted
     * to an {@link OffsetDateTime} that represents the same instant in the system default zone.
     */
    @Test
    void shouldConvertDateToOffsetDateTime() {
        // GIVEN
        // 2022-01-01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        Date source = new Date(epochMilli);
        Instant expectedInstant = Instant.ofEpochMilli(epochMilli);
        // WHEN
        OffsetDateTime result = BigDecimalConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedInstant, result.toInstant(), "Converted OffsetDateTime must represent the same instant as the source Date");
    }

    /**
     * Verify that passing {@code null} as the source throws a {@link NullPointerException}.
     * The implementation delegates to {@code toZonedDateTime}, which returns {@code null}
     * for a {@code null} source, leading to an NPE when calling {@code toOffsetDateTime()}.
     */
    @Test
    void shouldThrowWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toOffsetDateTime(source, converter), "Converting a null source should throw NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideConversionBranchCases")
    void shouldConvertVariousSourcesToOffsetDateTime(Object from, OffsetDateTime expected) {
        // WHEN
        OffsetDateTime result = BigDecimalConversions.toOffsetDateTime(from, null);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result, "Converted OffsetDateTime does not match expected value");
    }

    @Test
    void shouldConvertGregorianCalendarToOffsetDateTime() {
        // GIVEN – a GregorianCalendar representing the same instant as the ZonedDateTime above.
        ZonedDateTime zdt = ZonedDateTime.parse("2023-05-01T12:34:56+02:00[Europe/Paris]");
        GregorianCalendar calendar = GregorianCalendar.from(zdt);
        // Expected OffsetDateTime: instant from calendar, zone = system default (UTC in tests).
        Instant instant = Instant.ofEpochMilli(calendar.getTimeInMillis());
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
        // WHEN
        OffsetDateTime result = BigDecimalConversions.toOffsetDateTime(calendar, null);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result, "GregorianCalendar conversion produced unexpected OffsetDateTime");
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("provideUnsupportedSourceCases")
    void shouldThrowIllegalArgumentExceptionForUnsupportedSource(Object from, String description) {
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toOffsetDateTime(from, null), description);
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("provideMalformedStringCases")
    void shouldThrowDateTimeParseExceptionForInvalidString(String input, String description) {
        assertThrows(DateTimeParseException.class, () -> BigDecimalConversions.toOffsetDateTime(input, null), description);
    }

    @Test
    void shouldPropagateRuntimeExceptionFromConverter() {
        // GIVEN – a Converter mock that throws on any method call.
        Converter throwingConverter = org.mockito.Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("forced failure");
        });
        Instant now = Instant.now();
        // WHEN / THEN – the exception must bubble up unchanged.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BigDecimalConversions.toOffsetDateTime(now, throwingConverter), "Exception from Converter should be propagated");
        assertEquals("forced failure", ex.getMessage());
    }
}
