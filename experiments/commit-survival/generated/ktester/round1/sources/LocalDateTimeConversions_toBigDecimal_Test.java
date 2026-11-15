package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toBigDecimal_Test {

    /**
     * The Converter instance used for the tests.
     */
    private static Converter converter;

    /**
     * The default {@link Converter} used for most tests.
     */
    private static Converter defaultConverter;

    /**
     * A {@link Converter} that interprets a numeric string as seconds since the epoch.
     */
    private static Converter stringToInstantConverter;

    /**
     * A {@link Converter} that always throws a {@link ConversionException}.
     */
    private static Converter throwingConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The library provides a public no‑arg Constructor for ConverterOptions.
        ConverterOptions options = new ConverterOptions();
        defaultConverter = new Converter(options);
        // -----------------------------------------------------------------
        // Mock a Converter that converts a numeric String to an Instant.
        // The conversion logic used by LocalDateTimeConversions.toInstant is
        // assumed to call converter.convert(source, Instant.class). We mock
        // that interaction accordingly.
        // -----------------------------------------------------------------
        stringToInstantConverter = mock(Converter.class);
        when(stringToInstantConverter.convert(eq("20.75"), eq(Instant.class))).thenReturn(Instant.ofEpochSecond(20, 750_000_000L));
        // -----------------------------------------------------------------
        // Mock a Converter that always throws ConversionException.
        // -----------------------------------------------------------------
        throwingConverter = mock(Converter.class);
        when(throwingConverter.convert(any(), any())).thenThrow(new ConversionException("forced failure"));
    }

    @AfterAll
    static void teardownAfterAll() {
        defaultConverter = null;
        stringToInstantConverter = null;
        throwingConverter = null;
    }

    private static Stream<Arguments> validStandardTypesProvider() {
        return Stream.of(Arguments.of("Instant → same epoch seconds", Instant.ofEpochSecond(10, 0), BigDecimal.valueOf(10)), Arguments.of("java.util.Date → epoch seconds", Date.from(Instant.ofEpochSecond(10)), BigDecimal.valueOf(10)), Arguments.of("Calendar (UTC) → epoch seconds", createUtcCalendar(Instant.ofEpochSecond(10).toEpochMilli()), BigDecimal.valueOf(10)), Arguments.of("Long (epoch‑millis) → seconds with fraction", Long.valueOf(15_000L), BigDecimal.valueOf(15)), Arguments.of("LocalDateTime (UTC) → epoch seconds", LocalDateTime.of(2023, 1, 1, 0, 0), BigDecimal.valueOf(1672531200L)), Arguments.of("OffsetDateTime (+02:00) → epoch seconds", OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(2)), BigDecimal.valueOf(1672502400L)), Arguments.of("ZonedDateTime (Europe/Paris) → epoch seconds", ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Paris")), BigDecimal.valueOf(1672502400L)), Arguments.of("Timestamp with nanos → fractional seconds", Timestamp.valueOf("2023-01-01 00:00:00.123"), new BigDecimal("1672531200.123")));
    }

    private static Calendar createUtcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Stream<Arguments> validStringHandlingProvider() {
        return Stream.of(Arguments.of("String \"20.75\" interpreted as seconds via custom converter", "20.75", stringToInstantConverter, new BigDecimal("20.75")));
    }

    private static Stream<Arguments> validNumericTypesProvider() {
        return Stream.of(Arguments.of("Long zero → 0", Long.valueOf(0L), BigDecimal.valueOf(0)), Arguments.of("AtomicLong 2500ms → 2.5 seconds", new AtomicLong(2_500L), new BigDecimal("2.5")), Arguments.of("BigInteger 7500ms → 7.5 seconds", new BigInteger("7500"), new BigDecimal("7.5")), Arguments.of("Double 12.34 → 12.34 seconds", Double.valueOf(12.34), new BigDecimal("12.34")), Arguments.of("BigDecimal 56.78 → unchanged", new BigDecimal("56.78"), new BigDecimal("56.78")));
    }

    private static Stream<Arguments> validDateOnlyProvider() {
        return Stream.of(Arguments.of("LocalDate 2023‑01‑02 → start‑of‑day UTC", LocalDate.of(2023, 1, 2), BigDecimal.valueOf(1672617600L)), Arguments.of("Date -1000ms → -1 second", new Date(-1_000L), BigDecimal.valueOf(-1)));
    }

    private static Stream<Arguments> validZoneAwareProvider() {
        return Stream.of(Arguments.of("ZonedDateTime America/New_York (winter) → epoch seconds", ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), BigDecimal.valueOf(1672534800L)), Arguments.of("OffsetDateTime -03:00 → same epoch seconds", OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-3)), BigDecimal.valueOf(1672534800L)));
    }

    /**
     * Verify that a {@code LocalDateTime} representing the epoch (1970‑01‑01T00:00:00)
     * is converted to {@link BigDecimal#ZERO}.
     */
    @Test
    void toBigDecimal_givenEpochLocalDateTime_returnsZero() {
        // GIVEN
        LocalDateTime epoch = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        // WHEN
        BigDecimal result = LocalDateTimeConversions.toBigDecimal(epoch, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(BigDecimal.ZERO, result, "Epoch should be converted to BigDecimal zero");
    }

    /**
     * Verify that a {@code LocalDateTime} with a known instant is converted to the
     * corresponding {@link BigDecimal} representation of the epoch‑millisecond value.
     */
    @Test
    void toBigDecimal_givenSpecificLocalDateTime_returnsExpectedValue() {
        // GIVEN
        // 2023‑03‑15T12:30:45.123Z  → epoch milli = 1678884645123
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 12, 30, 45, 123_000_000);
        long expectedMillis = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        BigDecimal expected = new BigDecimal(expectedMillis);
        // WHEN
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(ldt, converter);
        // THEN
        assertNotNull(actual, "Result should not be null");
        assertEquals(expected, actual, "The BigDecimal representation should match the epoch‑millisecond value");
    }

    /**
     * Verify that passing {@code null} returns {@code null} (the conversion utility
     * follows the convention of returning {@code null} for null inputs when the
     * target type is an object).
     */
    @Test
    void toBigDecimal_givenNull_returnsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        BigDecimal result = LocalDateTimeConversions.toBigDecimal(input, converter);
        // THEN
        assertNull(result, "Converting null should yield null");
    }

    /* --------------------------------------------------------------
     * Parameterized tests for valid conversions – standard types
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validStandardTypesProvider")
    void toBigDecimal_validStandardTypes(String description, Object input, BigDecimal expected) {
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(input, defaultConverter);
        assertNotNull(actual, description);
        assertEquals(expected, actual, description);
    }

    /* --------------------------------------------------------------
     * Parameterized tests for valid conversions – custom string handling
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validStringHandlingProvider")
    void toBigDecimal_customStringHandling(String description, Object input, Converter converter, BigDecimal expected) {
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(input, converter);
        assertNotNull(actual, description);
        assertEquals(expected, actual, description);
    }

    /* --------------------------------------------------------------
     * Parameterized tests for valid conversions – additional numeric types
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validNumericTypesProvider")
    void toBigDecimal_validNumericTypes(String description, Object input, BigDecimal expected) {
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(input, defaultConverter);
        assertNotNull(actual, description);
        assertEquals(expected, actual, description);
    }

    /* --------------------------------------------------------------
     * Parameterized tests for valid conversions – date‑only types
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validDateOnlyProvider")
    void toBigDecimal_validDateOnly(String description, Object input, BigDecimal expected) {
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(input, defaultConverter);
        assertNotNull(actual, description);
        assertEquals(expected, actual, description);
    }

    /* --------------------------------------------------------------
     * Parameterized tests for valid conversions – time‑only types
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_validTimeOnly() {
        LocalTime time = LocalTime.of(12, 34, 56);
        // 12*3600 + 34*60 + 56 = 45296 seconds after midnight on 1970‑01‑01
        BigDecimal expected = BigDecimal.valueOf(45_296);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(time, defaultConverter);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------
     * Parameterized tests for valid conversions – zone‑aware non‑standard offsets
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validZoneAwareProvider")
    void toBigDecimal_validZoneAware(String description, Object input, BigDecimal expected) {
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(input, defaultConverter);
        assertNotNull(actual, description);
        assertEquals(expected, actual, description);
    }

    /* --------------------------------------------------------------
     * Tests for invalid / edge cases – expecting exceptions
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toBigDecimal(null, defaultConverter));
    }

    @Test
    void toBigDecimal_unsupportedObject_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal(new Object(), defaultConverter));
    }

    @Test
    void toBigDecimal_emptyString_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal("", defaultConverter));
    }

    @Test
    void toBigDecimal_nonNumericString_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal("not-a-date", defaultConverter));
    }

    @Test
    void toBigDecimal_longOverflow_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal(Long.MAX_VALUE, defaultConverter));
    }

    @Test
    void toBigDecimal_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toBigDecimal(Instant.now(), null));
    }

    @Test
    void toBigDecimal_offsetTimeUnsupported_throwsConversionException() {
        OffsetTime offsetTime = OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC);
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal(offsetTime, defaultConverter));
    }

    @Test
    void toBigDecimal_doubleNaN_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal(Double.NaN, defaultConverter));
    }

    @Test
    void toBigDecimal_doublePositiveInfinity_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal(Double.POSITIVE_INFINITY, defaultConverter));
    }

    @Test
    void toBigDecimal_doubleNegativeInfinity_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal(Double.NEGATIVE_INFINITY, defaultConverter));
    }

    @Test
    void toBigDecimal_customConverterThrows_throwsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal("trigger", throwingConverter));
    }
}
