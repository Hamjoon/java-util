package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toDouble_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter CONVERTER;

    /**
     * Expected value for the {@link #KNOWN_INSTANT} when converted to {@code double}.
     */
    private static double EXPECTED_DOUBLE;

    /**
     * A fixed instant used throughout the test suite.
     */
    private static final Instant KNOWN_INSTANT = Instant.ofEpochSecond(1_000_000_000L, 500_000_000L);

    /**
     * Shared {@link Converter} instance used for the positive test cases.
     */
    private static final Converter SHARED_CONVERTER = new SimpleConverter();

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter with default options – no mocking required.
        CONVERTER = new Converter(new ConverterOptions());
        // Compute the expected double once, using the same logic that the production code uses.
        // InstantConversions.toDouble simply returns seconds + nanos/1_000_000_000.
        EXPECTED_DOUBLE = KNOWN_INSTANT.getEpochSecond() + KNOWN_INSTANT.getNano() / 1_000_000_000d;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long‑running test suites.
        CONVERTER = null;
    }

    /**
     * Supplies all valid conversion scenarios.
     */
    private static Stream<Arguments> validInputProvider() {
        // Instant
        Arguments instant = Arguments.of(Instant.ofEpochSecond(1622548800L), SHARED_CONVERTER, 1622548800.0);
        // java.util.Date (milliseconds since epoch)
        Arguments date = Arguments.of(// 2020‑01‑01T00:00:00Z
        new Date(1577836800000L), SHARED_CONVERTER, 1577836800.0);
        // java.sql.Timestamp
        Arguments timestamp = Arguments.of(Timestamp.from(Instant.ofEpochSecond(1577836800L)), SHARED_CONVERTER, 1577836800.0);
        // LocalDateTime interpreted as UTC
        Arguments localDateTime = Arguments.of(LocalDateTime.of(2020, 1, 1, 0, 0, 0), SHARED_CONVERTER, 1577836800.0);
        // LocalDate (start of day UTC)
        Arguments localDate = Arguments.of(LocalDate.of(2020, 1, 1), SHARED_CONVERTER, 1577836800.0);
        // LocalTime (seconds since midnight of the epoch date)
        Arguments localTime = Arguments.of(LocalTime.of(12, 34, 56), SHARED_CONVERTER, 45296.0);
        // ZonedDateTime with negative offset
        Arguments zonedDateTime = Arguments.of(ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5)), SHARED_CONVERTER, 1577858400.0);
        // OffsetDateTime
        Arguments offsetDateTime = Arguments.of(OffsetDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(2)), SHARED_CONVERTER, 1577887200.0);
        // YearMonth (first day of month at midnight UTC)
        Arguments yearMonth = Arguments.of(YearMonth.of(2020, 1), SHARED_CONVERTER, 1577836800.0);
        // Year (January 1st at midnight UTC)
        Arguments year = Arguments.of(Year.of(2020), SHARED_CONVERTER, 1577836800.0);
        // Calendar (UTC)
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(1577836800000L);
        Arguments calendar = Arguments.of(utcCal, SHARED_CONVERTER, 1577836800.0);
        // BigDecimal numeric input
        Arguments bigDecimal = Arguments.of(new BigDecimal("1577836800"), SHARED_CONVERTER, 1577836800.0);
        // Long numeric input
        Arguments longNumber = Arguments.of(Long.valueOf(1577836800L), SHARED_CONVERTER, 1577836800.0);
        // Double numeric input
        Arguments doubleNumber = Arguments.of(Double.valueOf(1577836800.0), SHARED_CONVERTER, 1577836800.0);
        // String numeric input
        Arguments stringNumber = Arguments.of("1577836800", SHARED_CONVERTER, 1577836800.0);
        // String ISO‑8601 datetime input
        Arguments isoString = Arguments.of("2020-01-01T00:00:00Z", SHARED_CONVERTER, 1577836800.0);
        return Stream.of(instant, date, timestamp, localDateTime, localDate, localTime, zonedDateTime, offsetDateTime, yearMonth, year, calendar, bigDecimal, longNumber, doubleNumber, stringNumber, isoString);
    }

    /**
     * Supplies all scenarios that must result in an exception.
     */
    private static Stream<Arguments> exceptionInputProvider() {
        // Null source object
        Arguments nullSource = Arguments.of(null, new SimpleConverter(), NullPointerException.class);
        // Null converter
        Arguments nullConverter = Arguments.of(Instant.ofEpochSecond(1622548800L), null, NullPointerException.class);
        // Unsupported type (Boolean)
        Arguments unsupported = Arguments.of(Boolean.TRUE, new SimpleConverter(), ConversionException.class);
        // Invalid string values
        Arguments notADate = Arguments.of("not-a-date", new SimpleConverter(), ConversionException.class);
        Arguments emptyString = Arguments.of("", new SimpleConverter(), ConversionException.class);
        Arguments blankString = Arguments.of("   ", new SimpleConverter(), ConversionException.class);
        Arguments partiallyNumeric = Arguments.of("123abc", new SimpleConverter(), ConversionException.class);
        return Stream.of(nullSource, nullConverter, unsupported, notADate, emptyString, blankString, partiallyNumeric);
    }

    /**
     * Verify that a {@link Date} representing a known instant is converted to the correct double value.
     */
    @Test
    void toDouble_fromDate_returnsExpectedDouble() {
        // GIVEN
        Date date = Date.from(KNOWN_INSTANT);
        // WHEN
        double actual = LocalDateTimeConversions.toDouble(date, CONVERTER);
        // THEN
        assertEquals(EXPECTED_DOUBLE, actual, 1e-12, "Conversion from Date to double did not produce the expected epoch‑seconds value");
    }

    /**
     * Verify that a {@link java.time.Instant} source is handled directly.
     */
    @Test
    void toDouble_fromInstant_returnsExpectedDouble() {
        // GIVEN
        Instant instant = KNOWN_INSTANT;
        // WHEN
        double actual = LocalDateTimeConversions.toDouble(instant, CONVERTER);
        // THEN
        assertEquals(EXPECTED_DOUBLE, actual, 1e-12, "Conversion from Instant to double did not produce the expected epoch‑seconds value");
    }

    /**
     * Verify that a {@code null} source yields the default numeric value (0.0) as defined by {@link Converter}.
     */
    @Test
    void toDouble_fromNull_returnsZero() {
        // GIVEN
        Object source = null;
        // WHEN
        double actual = LocalDateTimeConversions.toDouble(source, CONVERTER);
        // THEN
        assertEquals(0.0d, actual, "Conversion from null should return 0.0 according to Converter's null‑handling contract");
    }

    /**
     * Parameterized test that verifies successful conversion of various
     * supported source types to the expected {@code double} value.
     */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("validInputProvider")
    void toDouble_validInputs(Object from, Converter converter, double expected) {
        double actual = LocalDateTimeConversions.toDouble(from, converter);
        assertEquals(expected, actual, 1e-12, () -> "Conversion of " + from.getClass().getSimpleName() + " did not produce the expected double");
    }

    /**
     * Parameterized test that verifies the method throws the expected exception
     * for invalid or unsupported inputs.
     */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("exceptionInputProvider")
    void toDouble_invalidInputs(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toDouble(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
