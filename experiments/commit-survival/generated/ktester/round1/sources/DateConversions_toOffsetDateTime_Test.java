package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toOffsetDateTime_Test {

    @Mock
    private Converter // the Converter used by the method under test
    mockConverter;

    @Mock
    private ConverterOptions // options returned from the Converter
    mockOptions;

    /**
     * Common test data
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    private static final Instant SAMPLE_INSTANT = Instant.parse("2023-01-01T12:34:56Z");

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks and define default behaviour
        MockitoAnnotations.openMocks(this);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(TEST_ZONE);
        // Initialise mocks and define default behaviour (zone will be overridden per test)
        MockitoAnnotations.openMocks(this);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    /* -------------------------------------------------------------
     * Helper to configure the mock converter with a specific zone.
     * ------------------------------------------------------------- */
    private void configureConverterZone(ZoneId zone) {
        when(mockOptions.getZoneId()).thenReturn(zone);
    }

    private static Stream<Arguments> validConversionProvider() {
        // Helper constants
        ZoneId UTC = ZoneId.of("UTC");
        ZoneId NEW_YORK = ZoneId.of("America/New_York");
        ZoneId PARIS = ZoneId.of("Europe/Paris");
        ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
        // converter zone ignored for ZonedDateTime source
        ZoneId LONDON = ZoneId.of("UTC");
        ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
        ZoneId BERLIN = ZoneId.of("Europe/Berlin");
        // Base instant for many cases
        Instant instant = Instant.parse("2023-05-15T10:20:30Z");
        long epochMilli = instant.toEpochMilli();
        // Calendar instance in UTC set to the same instant
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.set(2023, Calendar.MAY, 15, 10, 20, 30);
        utcCal.set(Calendar.MILLISECOND, 0);
        return Stream.of(// java.util.Date -> UTC
        Arguments.of("Convert java.util.Date (UTC) to OffsetDateTime", Date.from(instant), UTC, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.UTC)), // java.sql.Timestamp -> New York (UTC‑4 in May)
        Arguments.of("Convert Timestamp to OffsetDateTime in New York", Timestamp.valueOf("2023-05-15 10:20:30.123"), NEW_YORK, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 123_000_000, ZoneOffset.ofHours(-4))), // Instant -> Paris (UTC+2 in May)
        Arguments.of("Convert Instant to OffsetDateTime in Paris", instant, PARIS, OffsetDateTime.of(2023, 5, 15, 12, 20, 30, 0, ZoneOffset.ofHours(2))), // LocalDateTime -> Tokyo (UTC+9)
        Arguments.of("Convert LocalDateTime to OffsetDateTime in Tokyo", LocalDateTime.of(2023, 5, 15, 10, 20, 30), TOKYO, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.ofHours(9))), // ZonedDateTime (London) -> converter zone UTC (ignored)
        Arguments.of("Convert ZonedDateTime (London) with UTC converter", ZonedDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneId.of("Europe/London")), LONDON, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.UTC)), // OffsetDateTime (UTC+2) -> converter zone UTC
        Arguments.of("Convert OffsetDateTime +02:00 to UTC", OffsetDateTime.parse("2023-05-15T10:20:30+02:00"), UTC, OffsetDateTime.of(2023, 5, 15, 8, 20, 30, 0, ZoneOffset.UTC)), // Long epoch‑millis -> UTC
        Arguments.of("Convert Long epoch‑millis to OffsetDateTime UTC", Long.valueOf(epochMilli), UTC, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.UTC)), // ISO‑8601 String -> UTC
        Arguments.of("Convert ISO‑8601 string to OffsetDateTime UTC", "2023-05-15T10:20:30Z", UTC, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.UTC)), // LocalDate -> UTC (midnight)
        Arguments.of("Convert LocalDate to OffsetDateTime UTC (midnight)", LocalDate.of(2023, 5, 15), UTC, OffsetDateTime.of(2023, 5, 15, 0, 0, 0, 0, ZoneOffset.UTC)), // LocalTime -> UTC (epoch date)
        Arguments.of("Convert LocalTime to OffsetDateTime UTC (epoch date)", LocalTime.of(10, 20, 30), UTC, OffsetDateTime.of(1970, 1, 1, 10, 20, 30, 0, ZoneOffset.UTC)), // Calendar (UTC) -> UTC
        Arguments.of("Convert Calendar (UTC) to OffsetDateTime UTC", utcCal, UTC, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.UTC)), // java.sql.Date -> New York (midnight)
        Arguments.of("Convert java.sql.Date to OffsetDateTime New York (midnight)", java.sql.Date.valueOf("2023-05-15"), NEW_YORK, OffsetDateTime.of(2023, 5, 15, 0, 0, 0, 0, ZoneOffset.ofHours(-4))), // BigDecimal epoch‑millis -> UTC
        Arguments.of("Convert BigDecimal epoch‑millis to OffsetDateTime UTC", BigDecimal.valueOf(epochMilli), UTC, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.UTC)), // BigInteger epoch‑millis -> UTC
        Arguments.of("Convert BigInteger epoch‑millis to OffsetDateTime UTC", BigInteger.valueOf(epochMilli), UTC, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.UTC)), // AtomicLong epoch‑millis -> UTC
        Arguments.of("Convert AtomicLong epoch‑millis to OffsetDateTime UTC", new AtomicLong(epochMilli), UTC, OffsetDateTime.of(2023, 5, 15, 10, 20, 30, 0, ZoneOffset.UTC)), // ZonedDateTime (Berlin) -> Kolkata (UTC+5:30)
        Arguments.of("Convert ZonedDateTime Berlin to OffsetDateTime Kolkata", ZonedDateTime.of(2023, 5, 15, 10, 20, 30, 0, BERLIN), KOLKATA, OffsetDateTime.of(2023, 5, 15, 13, 50, 30, 0, ZoneOffset.ofHoursMinutes(5, 30))), // OffsetDateTime -07:00 -> UTC
        Arguments.of("Convert OffsetDateTime -07:00 to UTC", OffsetDateTime.parse("2023-05-15T10:20:30-07:00"), UTC, OffsetDateTime.of(2023, 5, 15, 17, 20, 30, 0, ZoneOffset.UTC)));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        ZoneId UTC = ZoneId.of("UTC");
        // Helper converter with null zone (mocked)
        Converter nullZoneConverter = mock(Converter.class);
        ConverterOptions nullZoneOptions = mock(ConverterOptions.class);
        when(nullZoneConverter.getOptions()).thenReturn(nullZoneOptions);
        when(nullZoneOptions.getZoneId()).thenReturn(null);
        return Stream.of(// Non‑parseable string
        Arguments.of("Non‑parseable string should throw DateTimeParseException", "not-a-date", UTC, null, DateTimeParseException.class), // Null source – specification expects NPE
        Arguments.of("Null source should throw NullPointerException", null, UTC, null, NullPointerException.class), // Converter with null ZoneId
        Arguments.of("Converter with null ZoneId should throw NullPointerException", Instant.parse("2023-05-15T10:20:30Z"), null, nullZoneConverter, NullPointerException.class), // Null converter
        Arguments.of("Null converter should throw NullPointerException", Instant.parse("2023-05-15T10:20:30Z"), UTC, null, NullPointerException.class), // Unsupported Boolean source
        Arguments.of("Unsupported Boolean source should throw IllegalArgumentException", Boolean.TRUE, UTC, null, IllegalArgumentException.class), // Unsupported Double source
        Arguments.of("Unsupported Double source should throw IllegalArgumentException", Double.valueOf(1_234_567.89), UTC, null, IllegalArgumentException.class), // BigDecimal with fractional milliseconds
        Arguments.of("BigDecimal with fractional millis should throw IllegalArgumentException", BigDecimal.valueOf(1_684_132_830_000.123), UTC, null, IllegalArgumentException.class), // Arbitrary Object
        Arguments.of("Arbitrary Object source should throw IllegalArgumentException", new Object(), UTC, null, IllegalArgumentException.class));
    }

    /**
     * Verify that an {@link Instant} is correctly converted to {@link OffsetDateTime}
     * using the zone supplied by {@link ConverterOptions#getZoneId()}.
     */
    @Test
    void toOffsetDateTime_fromInstant_returnsCorrectOffsetDateTime() {
        // given
        // the "from" argument
        Object source = SAMPLE_INSTANT;
        // when
        OffsetDateTime result = DateConversions.toOffsetDateTime(source, mockConverter);
        // then
        OffsetDateTime expected = SAMPLE_INSTANT.atZone(TEST_ZONE).toOffsetDateTime();
        assertEquals(expected, result, "OffsetDateTime conversion from Instant did not match expected value");
    }

    /**
     * Verify that a {@link java.util.Date} is correctly converted to {@link OffsetDateTime}.
     * The internal conversion to {@link Instant} should handle the Date instance.
     */
    @Test
    void toOffsetDateTime_fromDate_returnsCorrectOffsetDateTime() {
        // given
        Date date = Date.from(SAMPLE_INSTANT);
        Object source = date;
        // when
        OffsetDateTime result = DateConversions.toOffsetDateTime(source, mockConverter);
        // then
        OffsetDateTime expected = SAMPLE_INSTANT.atZone(TEST_ZONE).toOffsetDateTime();
        assertEquals(expected, result, "OffsetDateTime conversion from Date did not match expected value");
    }

    /**
     * Verify that a {@code null} source returns {@code null} (the behaviour of the
     * underlying {@code toInstant} conversion for a null input).
     */
    @Test
    void toOffsetDateTime_nullInput_returnsNull() {
        // given
        Object source = null;
        // when
        OffsetDateTime result = DateConversions.toOffsetDateTime(source, mockConverter);
        // then
        assertNull(result, "Conversion of null should produce null");
    }

    /* -------------------------------------------------------------
     * Parameterized test for all *valid* conversion cases.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toOffsetDateTime_validConversions(String description, Object source, ZoneId zone, OffsetDateTime expected) {
        configureConverterZone(zone);
        OffsetDateTime result = DateConversions.toOffsetDateTime(source, mockConverter);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     * Parameterized test for *invalid* inputs – expected exception.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void toOffsetDateTime_invalidInputs(String description, Object source, ZoneId zone, Converter converter, Class<? extends Throwable> expectedException) {
        // If a specific converter instance is supplied (e.g., null), use it;
        // otherwise configure the default mock.
        Converter usedConverter = converter != null ? converter : mockConverter;
        if (usedConverter == mockConverter) {
            // configure zone only when mock is used
            configureConverterZone(zone);
        }
        assertThrows(expectedException, () -> DateConversions.toOffsetDateTime(source, usedConverter), description);
    }

    /* -------------------------------------------------------------
     * Additional explicit test for null converter (covers the case
     * where the mock is not used).
     * ------------------------------------------------------------- */
    @Test
    void toOffsetDateTime_nullConverter_throwsNPE() {
        Object source = Instant.parse("2023-05-15T10:20:30Z");
        assertThrows(NullPointerException.class, () -> DateConversions.toOffsetDateTime(source, null), "Passing a null Converter must raise NullPointerException");
    }
}
