package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toCalendar_Test {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a Converter with a known time‑zone (UTC) so that the conversion result is predictable.
        ConverterOptions options = new ConverterOptions();
        // The API of ConverterOptions is not shown in the prompt, but it is known to have a
        // setter for the time‑zone (most Cedar Software versions expose it).  If the method
        // name differs, adjust accordingly.
        options.setTimeZone(UTC);
        converter = new Converter(options);
    }

    /**
     * Helper that creates a Mockito mock of {@link Converter} returning the supplied {@link TimeZone}.
     */
    private static Converter mockConverter(TimeZone tz) {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getTimeZone()).thenReturn(tz);
        return converter;
    }

    private static Stream<Arguments> provideSuccessCases() {
        return Stream.of(// 1. ZonedDateTime (UTC) -> same instant, UTC zone
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), UTC, 1684139400000L, UTC, "ZonedDateTime → Calendar (same instant, UTC)"), // 2. LocalDateTime with America/New_York zone
        Arguments.of(LocalDateTime.of(2023, 12, 31, 23, 59, 59), TimeZone.getTimeZone("America/New_York"), 1704085199000L, TimeZone.getTimeZone("America/New_York"), "LocalDateTime interpreted in New York zone"), // 3. java.util.Date (instant preserved) – Asia/Tokyo zone
        Arguments.of(Date.from(Instant.parse("2022-01-01T12:00:00Z")), TimeZone.getTimeZone("Asia/Tokyo"), 1641038400000L, TimeZone.getTimeZone("Asia/Tokyo"), "java.util.Date → Calendar (instant preserved)"), // 4. java.sql.Timestamp interpreted in Europe/Paris zone (CEST)
        Arguments.of(Timestamp.valueOf("2021-07-04 15:20:30"), TimeZone.getTimeZone("Europe/Paris"), 1625404830000L, TimeZone.getTimeZone("Europe/Paris"), "Timestamp interpreted as local date‑time in Paris"), // 5. BigDecimal representing epoch millis
        Arguments.of(BigDecimal.valueOf(1672531199000L), UTC, 1672531199000L, UTC, "BigDecimal epoch millis"), // 8. LocalDate → start‑of‑day UTC
        Arguments.of(LocalDate.of(2022, 2, 28), UTC, 1646006400000L, UTC, "LocalDate start‑of‑day UTC"), // 9. Instant (preserved) – America/Los_Angeles zone
        Arguments.of(Instant.parse("2020-06-01T12:34:56Z"), TimeZone.getTimeZone("America/Los_Angeles"), 1591023296000L, TimeZone.getTimeZone("America/Los_Angeles"), "Instant preserved, zone changed to Los_Angeles"), // 10. OffsetDateTime with -05:00 offset, New York zone (DST gap)
        Arguments.of(OffsetDateTime.of(2021, 3, 14, 2, 30, 0, 0, ZoneOffset.ofHours(-5)), TimeZone.getTimeZone("America/New_York"), // as described in the spec
        1615701000000L, TimeZone.getTimeZone("America/New_York"), "OffsetDateTime in DST spring‑forward gap (NY)"), // 11. java.sql.Date interpreted in London (BST)
        Arguments.of(java.sql.Date.valueOf("2022-10-15"), TimeZone.getTimeZone("Europe/London"), 1665792000000L, TimeZone.getTimeZone("Europe/London"), "java.sql.Date interpreted as midnight BST"), // 12. Long epoch millis
        Arguments.of(Long.valueOf(1609459200000L), UTC, 1609459200000L, UTC, "Long epoch millis"), // 13. Existing Calendar (Asia/Kolkata) → Calendar with UTC zone, same instant
        Arguments.of(createCalendarWithZone("2022-01-01T00:00:00+05:30", TimeZone.getTimeZone("Asia/Kolkata")), UTC, 1640905800000L, UTC, "Calendar conversion – instant preserved, zone switched to UTC"), // 14. BigInteger epoch millis
        Arguments.of(BigInteger.valueOf(1672531199000L), UTC, 1672531199000L, UTC, "BigInteger epoch millis"), // 15. AtomicLong epoch millis
        Arguments.of(new AtomicLong(1672531199000L), UTC, 1672531199000L, UTC, "AtomicLong epoch millis"), // 16. Double epoch millis (truncated)
        Arguments.of(Double.valueOf(1.632531199e12), UTC, 1632531199000L, UTC, "Double epoch millis (truncation)"), // 17. ISO‑8601 string with offset, converted to UTC
        Arguments.of("2023-04-05T14:20:00+02:00", UTC, 1680697200000L, UTC, "String with offset → UTC instant"), // 18. ZonedDateTime (Paris) → Calendar in New York zone
        Arguments.of(ZonedDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneId.of("Europe/Paris")), TimeZone.getTimeZone("America/New_York"), 1672507200000L, TimeZone.getTimeZone("America/New_York"), "ZonedDateTime from Paris, target zone New York"), // 19. ZonedDateTime in DST gap (Europe/Paris)
        Arguments.of(ZonedDateTime.of(2022, 3, 27, 2, 30, 0, 0, ZoneId.of("Europe/Paris")), TimeZone.getTimeZone("Europe/Paris"), 1648343400000L, TimeZone.getTimeZone("Europe/Paris"), "ZonedDateTime in Paris DST spring‑forward gap"));
    }

    /**
     * Utility to create a Calendar from an ISO‑8601 string and a specific zone.
     */
    private static Calendar createCalendarWithZone(String iso, TimeZone zone) {
        Instant instant = Instant.parse(iso);
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(instant.toEpochMilli());
        return cal;
    }

    private static Stream<Arguments> provideExceptionCases() {
        // Converter with null options
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // Converter with options that return null timezone
        Converter converterWithNullTZ = mock(Converter.class);
        ConverterOptions optsNullTZ = mock(ConverterOptions.class);
        when(converterWithNullTZ.getOptions()).thenReturn(optsNullTZ);
        when(optsNullTZ.getTimeZone()).thenReturn(null);
        return Stream.of(// 6. Null source → NPE from toZonedDateTime
        Arguments.of(null, mockConverter(UTC), NullPointerException.class, "Null source should cause NPE"), // 7. Invalid string → ConversionException (runtime)
        Arguments.of("invalid date string", mockConverter(UTC), RuntimeException.class, "Unsupported string format should raise exception"), // 21. Null converter argument
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), null, NullPointerException.class, "Null converter should cause NPE"), // 22. Converter with null options
        Arguments.of(Instant.now(), converterWithNullOptions, NullPointerException.class, "Converter returning null options should cause NPE"), // 23. Converter whose options have null timezone
        Arguments.of(Instant.now(), converterWithNullTZ, NullPointerException.class, "Converter with null timezone should cause NPE from Calendar.getInstance(null)"), // 24. Unsupported Boolean source
        Arguments.of(Boolean.TRUE, mockConverter(UTC), RuntimeException.class, "Unsupported Boolean source should raise ConversionException"), // 25. Unsupported UUID source
        Arguments.of(UUID.randomUUID(), mockConverter(UTC), RuntimeException.class, "Unsupported UUID source should raise ConversionException"));
    }

    /**
     * Verify that a {@link ZonedDateTime} is converted to a {@link Calendar}
     * with the same instant and the converter's time‑zone.
     */
    @Test
    void toCalendar_fromZonedDateTime_matchesInstantAndTimeZone() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, // explicit UTC to match the options
        ZoneOffset.UTC);
        // WHEN
        Calendar result = LocalDateConversions.toCalendar(source, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(source.toInstant().toEpochMilli(), result.getTimeInMillis(), "Calendar should represent the same instant as the source ZonedDateTime");
        assertEquals(UTC, result.getTimeZone(), "Calendar's TimeZone must be the one supplied via ConverterOptions");
    }

    /**
     * Verify that a {@link LocalDateTime} is converted to a {@link Calendar}
     * using the converter's time‑zone (UTC in this test).
     */
    @Test
    void toCalendar_fromLocalDateTime_usesConverterTimeZone() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        // Expected ZonedDateTime is the LocalDateTime interpreted in the converter's zone (UTC)
        ZonedDateTime expectedZdt = source.atZone(UTC.toZoneId());
        // WHEN
        Calendar result = LocalDateConversions.toCalendar(source, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(expectedZdt.toInstant().toEpochMilli(), result.getTimeInMillis(), "Calendar should represent the same instant as the LocalDateTime interpreted in the converter's zone");
        assertEquals(UTC, result.getTimeZone(), "Calendar's TimeZone must be the one supplied via ConverterOptions");
    }

    /**
     * Verify that a {@link java.util.Date} is converted correctly.
     */
    @Test
    void toCalendar_fromDate_preservesInstant() {
        // GIVEN
        // a fixed epoch milli value
        java.util.Date source = new java.util.Date(1_680_000_000_000L);
        // WHEN
        Calendar result = LocalDateConversions.toCalendar(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTimeInMillis(), "Calendar should have the same epoch milli as the source Date");
        assertEquals(UTC, result.getTimeZone(), "Calendar's TimeZone must be the one supplied via ConverterOptions");
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideSuccessCases")
    void toCalendar_success(Object from, TimeZone converterZone, Long expectedMillis, TimeZone expectedZone, String description) {
        Converter converter = mockConverter(converterZone);
        Calendar result = LocalDateConversions.toCalendar(from, converter);
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(expectedMillis.longValue(), result.getTimeInMillis(), "Unexpected epoch milli for case: " + description);
        assertEquals(expectedZone, result.getTimeZone(), "Unexpected TimeZone for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideExceptionCases")
    void toCalendar_exceptions(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> LocalDateConversions.toCalendar(from, converter), "Expected exception for case: " + description);
    }

    @Test
    void toCalendar_converterWithNullTimeZone_usesSystemDefault() {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        // explicit null
        when(options.getTimeZone()).thenReturn(null);
        // WHEN
        Calendar result = LocalDateConversions.toCalendar(now, converter);
        // THEN
        assertNotNull(result);
        assertEquals(now.toInstant().toEpochMilli(), result.getTimeInMillis(), "Instant must be preserved");
        assertEquals(TimeZone.getDefault(), result.getTimeZone(), "TimeZone should fall back to the JVM default when converter's zone is null");
    }
}
