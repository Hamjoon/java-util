package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toOffsetDateTime_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter sharedConverter;

    /**
     * Fresh Converter for each test (if needed).
     */
    private Converter converter;

    /**
     * A mock Converter that throws a RuntimeException when used.
     */
    @Mock
    private Converter throwingConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // New instance for each test to avoid cross‑test state.
        converter = new Converter(new ConverterOptions());
        // Configure the mock to throw a RuntimeException on any conversion attempt.
        Mockito.doThrow(new RuntimeException("Converter failure")).when(throwingConverter).convert(Mockito.any(), Mockito.any());
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release, but clear references for GC.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        sharedConverter = null;
    }

    /**
     * Provides a set of valid source objects together with the expected ISO‑8601
     * representation of the resulting {@link OffsetDateTime}.
     *
     * The {@code converter} argument is always {@code null} in the JSON spec,
     * meaning the shared or instance converter should be used – the test picks
     * the instance {@code converter}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideValidConversions() {
        return Stream.of(arguments(ZonedDateTime.parse("2023-03-25T12:34:56+02:00[Europe/Paris]"), "2023-03-25T12:34:56+02:00", "Directly passing a ZonedDateTime should return the same instant with the same offset."), arguments(OffsetDateTime.parse("2023-03-25T12:34:56+02:00"), "2023-03-25T12:34:56+02:00", "Passing an OffsetDateTime should be converted via toZonedDateTime (as an instant) and retain the original offset."), arguments(LocalDateTime.of(2023, 3, 25, 12, 34, 56), "2023-03-25T12:34:56Z", "LocalDateTime without zone defaults to UTC (Z) offset after conversion."), arguments(Date.from(Instant.parse("2023-03-25T10:34:56Z")), "2023-03-25T10:34:56Z", "java.util.Date is interpreted as an instant in UTC."), arguments(Timestamp.valueOf("2023-03-25 12:34:56.123456789"), "2023-03-25T12:34:56.123456789Z", "Timestamp includes nanoseconds; conversion should preserve them and use UTC offset."), arguments(Instant.parse("2023-03-25T12:34:56Z"), "2023-03-25T12:34:56Z", "Instant converts directly to OffsetDateTime with UTC offset."), arguments(Year.of(2023), "2023-01-01T00:00Z", "Year is interpreted as start of year at midnight UTC."), arguments(LocalDate.of(2023, 3, 25), "2023-03-25T00:00Z", "LocalDate is interpreted as start of day at UTC."));
    }

    /**
     * Provides inputs that are expected to cause an exception.
     *
     * The third element is the exact exception class that should be thrown.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideExceptionCases() {
        return Stream.of(arguments(null, NullPointerException.class, "Null source object should result in NPE as toZonedDateTime cannot handle null."), arguments(new Object(), IllegalArgumentException.class, "Unsupported source type (Object) should cause IllegalArgumentException."), arguments("not-a-date", DateTimeParseException.class, "String that cannot be parsed as a date‑time should result in DateTimeParseException."), arguments("2023-13-01T00:00:00Z", DateTimeParseException.class, "String with an invalid month (13) should cause DateTimeParseException during parsing."), arguments(YearMonth.of(2023, 5), IllegalArgumentException.class, "YearMonth is not a supported conversion target for OffsetDateTime, leading to IllegalArgumentException."), arguments(BigInteger.valueOf(12345), IllegalArgumentException.class, "BigInteger is not convertible to OffsetDateTime; method should throw IllegalArgumentException."));
    }

    @Test
    void toOffsetDateTime_fromZonedDateTime_returnsEquivalentOffsetDateTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000, // CET/CEST (+01/+02)
        ZoneId.of("Europe/Paris"));
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(zdt, converter);
        // THEN
        OffsetDateTime expected = zdt.toOffsetDateTime();
        assertEquals(expected, result, "OffsetDateTime should match the conversion from ZonedDateTime");
        assertEquals(zdt.getOffset(), result.getOffset(), "Offsets must be identical");
    }

    @Test
    void toOffsetDateTime_fromInstant_returnsOffsetDateTimeAtSystemDefaultZone() {
        // GIVEN
        Instant instant = Instant.parse("2022-12-01T10:15:30.500Z");
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(instant, converter);
        // THEN
        // NumberConversions delegates to toZonedDateTime which uses the system default zone.
        ZoneId systemZone = ZoneId.systemDefault();
        OffsetDateTime expected = instant.atZone(systemZone).toOffsetDateTime();
        assertEquals(expected, result, "OffsetDateTime should represent the instant in the system default zone");
    }

    @Test
    void toOffsetDateTime_fromLocalDateTime_usesSystemDefaultOffset() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, 3, 10, 9, 0, 0);
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(ldt, converter);
        // THEN
        ZoneId systemZone = ZoneId.systemDefault();
        OffsetDateTime expected = ldt.atZone(systemZone).toOffsetDateTime();
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_fromDate_returnsOffsetDateTimeAtSystemDefaultZone() {
        // GIVEN
        Date date = Date.from(Instant.parse("2020-07-04T12:00:00Z"));
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(date, converter);
        // THEN
        ZoneId systemZone = ZoneId.systemDefault();
        OffsetDateTime expected = date.toInstant().atZone(systemZone).toOffsetDateTime();
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_nullInput_returnsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(nullObj, converter);
        // THEN
        assertNull(result, "Converting a null source should yield null");
    }

    @Test
    void toOffsetDateTime_usingSharedConverter_behavesSameAsInstanceConverter() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.now();
        // WHEN
        OffsetDateTime fromShared = NumberConversions.toOffsetDateTime(zdt, sharedConverter);
        OffsetDateTime fromInstance = NumberConversions.toOffsetDateTime(zdt, converter);
        // THEN
        assertEquals(fromShared, fromInstance, "Both shared and per‑test converters should produce identical OffsetDateTime results.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideValidConversions")
    void toOffsetDateTime_validInputs_returnExpected(Object from, String expectedIso, String description) {
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(from, converter);
        // THEN
        assertNotNull(result, "Result must not be null for valid input");
        assertEquals(expectedIso, result.toString(), () -> "Expected ISO‑8601 string '" + expectedIso + "' but got '" + result + "'");
    }

    /**
     * Calendar conversion – the expected value is the current UTC time at the moment
     * of execution, rounded to seconds.  Because of execution latency we allow a
     * small delta (2 seconds) when comparing.
     */
    @Test
    void toOffsetDateTime_fromCalendarUtc_returnsCurrentUtcRoundedToSeconds() {
        // GIVEN a Calendar in UTC
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(cal, converter);
        // THEN
        OffsetDateTime nowUtc = Instant.now().truncatedTo(ChronoUnit.SECONDS).atZone(ZoneOffset.UTC).toOffsetDateTime();
        long secondsDiff = Math.abs(Duration.between(result, nowUtc).getSeconds());
        assertTrue(secondsDiff < 2, "Result should be within 2 seconds of the current UTC time; diff was " + secondsDiff + " seconds");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideExceptionCases")
    void toOffsetDateTime_invalidInputs_throwExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> NumberConversions.toOffsetDateTime(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }

    @Test
    void toOffsetDateTime_converterThrowsRuntimeException_propagates() {
        // GIVEN a valid source but a Converter that throws RuntimeException
        ZonedDateTime zdt = ZonedDateTime.parse("2023-03-25T12:34:56+02:00[Europe/Paris]");
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> NumberConversions.toOffsetDateTime(zdt, throwingConverter), "A RuntimeException thrown by the Converter must be propagated unchanged.");
    }
}
