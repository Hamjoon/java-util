package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toBigInteger_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used by a few legacy tests.
     */
    private static Converter sharedConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The shared converter uses the default options – sufficient for the
        // simple date‑time conversions performed in the original tests.
        ConverterOptions options = new ConverterOptions();
        sharedConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the
        // test class tidy.
        converter = null;
    }

    /**
     * Builds the expected {@link BigInteger} from an {@link Instant}.
     */
    private static BigInteger expectedFromInstant(Instant instant) {
        long seconds = instant.getEpochSecond();
        int nanos = instant.getNano();
        return BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nanos));
    }

    /**
     * Creates a {@link Calendar} set to the supplied epoch‑millis in UTC.
     */
    private static Calendar utcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Stream<Arguments> validConversionProvider() {
        // default UTC
        SimpleConverter defaultConverter = new SimpleConverter();
        // explicit UTC
        SimpleConverter utcConverter = new SimpleConverter(ZoneOffset.UTC);
        // -05:00
        SimpleConverter minusFive = new SimpleConverter(ZoneOffset.ofHours(-5));
        return Stream.of(// Direct Instant with nanos
        Arguments.of("Instant with nanos", Instant.ofEpochSecond(1622547805L, 123_456_789L), defaultConverter, expectedFromInstant(Instant.ofEpochSecond(1622547805L, 123_456_789L))), // java.util.Date (millis)
        Arguments.of("java.util.Date (millis)", new Date(1622547805123L), defaultConverter, expectedFromInstant(Instant.ofEpochMilli(1622547805123L))), // java.sql.Timestamp (full precision)
        Arguments.of("java.sql.Timestamp with nanos", Timestamp.valueOf("2021-06-01 12:30:05.123456789"), defaultConverter, expectedFromInstant(Instant.ofEpochSecond(1622551805L, 123_456_789L))), // Long interpreted as epoch‑milliseconds
        Arguments.of("Long epoch‑millis", Long.valueOf(1622547805123L), defaultConverter, expectedFromInstant(Instant.ofEpochMilli(1622547805123L))), // ISO‑8601 UTC string
        Arguments.of("ISO‑8601 UTC string", "2021-06-01T12:30:05.123456789Z", defaultConverter, expectedFromInstant(Instant.parse("2021-06-01T12:30:05.123456789Z"))), // OffsetDateTime with +02:00
        Arguments.of("OffsetDateTime +02:00", OffsetDateTime.parse("2021-06-01T12:30:05.123456789+02:00"), defaultConverter, expectedFromInstant(Instant.parse("2021-06-01T10:30:05.123456789Z"))), // ZonedDateTime with +02:00[Europe/Paris]
        Arguments.of("ZonedDateTime +02:00[Europe/Paris]", ZonedDateTime.parse("2021-06-01T12:30:05.123456789+02:00[Europe/Paris]"), defaultConverter, expectedFromInstant(Instant.parse("2021-06-01T10:30:05.123456789Z"))), // LocalDateTime interpreted as UTC (default converter)
        Arguments.of("LocalDateTime interpreted as UTC", LocalDateTime.of(2021, 6, 1, 12, 30, 5, 123_456_789), utcConverter, expectedFromInstant(Instant.ofEpochSecond(1622551805L, 123_456_789L))), // Instant.EPOCH -> zero
        Arguments.of("Instant.EPOCH", Instant.EPOCH, defaultConverter, BigInteger.ZERO), // Negative epoch seconds
        Arguments.of("Negative epoch seconds", Instant.ofEpochSecond(-1L, 0), defaultConverter, BigInteger.valueOf(-1L).multiply(BigInteger.valueOf(1_000_000_000L))), // Calendar (UTC) set to same epoch‑millis as Date test
        Arguments.of("Calendar (UTC) same epoch‑millis", utcCalendar(1622547805123L), defaultConverter, expectedFromInstant(Instant.ofEpochMilli(1622547805123L))), // java.sql.Date (millis)
        Arguments.of("java.sql.Date (millis)", new java.sql.Date(1622547805123L), defaultConverter, expectedFromInstant(Instant.ofEpochMilli(1622547805123L))), // String with single fractional digit
        Arguments.of("String with single fractional digit", "2021-06-01T12:30:05.1Z", defaultConverter, expectedFromInstant(Instant.parse("2021-06-01T12:30:05.100Z"))), // LocalDateTime with non‑UTC default offset (-05:00)
        Arguments.of("LocalDateTime with -05:00 offset", LocalDateTime.of(2021, 6, 1, 12, 30, 5, 123_456_789), minusFive, expectedFromInstant(Instant.ofEpochSecond(1622569805L, 123_456_789L))), // OffsetDateTime with -04:00
        Arguments.of("OffsetDateTime -04:00", OffsetDateTime.parse("2021-06-01T12:30:05.123456789-04:00"), defaultConverter, expectedFromInstant(Instant.ofEpochSecond(1622566205L, 123_456_789L))), // Short interpreted as epoch‑milliseconds
        Arguments.of("Short epoch‑millis", Short.valueOf((short) 12345), defaultConverter, expectedFromInstant(Instant.ofEpochMilli(12345L))), // LocalDate interpreted as start‑of‑day UTC
        Arguments.of("LocalDate start‑of‑day UTC", LocalDate.of(2021, 6, 1), utcConverter, expectedFromInstant(Instant.ofEpochSecond(1622505600L))), // ZonedDateTime on DST transition (Brazil DST example)
        Arguments.of("ZonedDateTime DST transition", ZonedDateTime.parse("2021-12-31T23:00:00-01:00[America/Sao_Paulo]"), defaultConverter, expectedFromInstant(Instant.ofEpochSecond(1640991600L))));
    }

    private static Stream<Arguments> exceptionProvider() {
        SimpleConverter defaultConverter = new SimpleConverter();
        return Stream.of(// Null source
        Arguments.of("Null source object", null, defaultConverter, NullPointerException.class), // Invalid string format
        Arguments.of("Invalid date‑time string", "not-a-date", defaultConverter, ConversionException.class), // Unsupported source type (plain Object)
        Arguments.of("Unsupported source type (Object)", new Object(), defaultConverter, ConversionException.class), // Null converter
        Arguments.of("Null converter", Instant.now(), null, NullPointerException.class), // OffsetTime (no date)
        Arguments.of("OffsetTime without date", OffsetTime.of(12, 30, 5, 123_456_789, ZoneOffset.ofHours(2)), defaultConverter, ConversionException.class), // LocalTime (no date)
        Arguments.of("LocalTime without date", LocalTime.of(12, 30, 5, 123_456_789), defaultConverter, ConversionException.class), // BigInteger source (unsupported)
        Arguments.of("BigInteger source", BigInteger.valueOf(1622547805123000000L), defaultConverter, ConversionException.class), // Instant beyond supported range (overflow)
        Arguments.of("Instant overflow (too large)", Instant.ofEpochSecond(Long.MAX_VALUE / 1_000_000_000, 0), defaultConverter, ConversionException.class), // Both source and converter null
        Arguments.of("Both source and converter null", null, null, NullPointerException.class), // Double (floating‑point) source
        Arguments.of("Double source (unsupported numeric type)", Double.valueOf(1622547805.123), defaultConverter, ConversionException.class), // AtomicLong source (unsupported)
        Arguments.of("AtomicLong source (unsupported)", new AtomicLong(1622547805123L), defaultConverter, ConversionException.class), // Date overflow (epoch‑millis > Instant.MAX)
        Arguments.of("Date overflow (Long.MAX_VALUE)", new Date(Long.MAX_VALUE), defaultConverter, ConversionException.class), // Instant underflow (below minimum)
        Arguments.of("Instant underflow (too small)", Instant.ofEpochSecond(Long.MIN_VALUE / 1_000_000_000 - 1, 0), defaultConverter, ConversionException.class), // Plain numeric string (epoch‑millis) – not ISO‑8601
        Arguments.of("Plain numeric string (epoch‑millis) not ISO‑8601", "1622547805123", defaultConverter, ConversionException.class), // LocalDateTime with null default ZoneOffset in Converter
        Arguments.of("LocalDateTime with null default ZoneOffset", LocalDateTime.of(2021, 6, 1, 12, 30, 5, 123_456_789), new SimpleConverter(null), NullPointerException.class));
    }

    @Test
    void toBigInteger_fromOffsetDateTime_returnsSameAsInstantConversion() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 10, 12, 30, 45, 123_456_789, ZoneOffset.UTC);
        Instant expectedInstant = odt.toInstant();
        // WHEN
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(odt, converter);
        BigInteger expected = InstantConversions.toBigInteger(expectedInstant, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from OffsetDateTime should delegate to Instant conversion and produce the same result.");
    }

    @Test
    void toBigInteger_fromInstant_returnsSameAsInstantConversion() {
        // GIVEN
        Instant instant = Instant.now();
        // WHEN
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(instant, converter);
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        // THEN
        assertEquals(expected, actual, "Conversion from Instant should be identical to InstantConversions.toBigInteger.");
    }

    @Test
    void toBigInteger_fromNull_returnsNull() {
        // GIVEN
        Object from = null;
        // WHEN
        BigInteger result = OffsetDateTimeConversions.toBigInteger(from, converter);
        // THEN
        assertNull(result, "Converting a null source should return null.");
    }

    @Test
    void toBigInteger_fromStringRepresentingEpochSecond_isConvertedCorrectly() {
        // GIVEN
        // The Converter can turn a numeric String into an Instant (seconds since epoch)
        // by first converting it to a Long then to an Instant.
        // 2021‑06‑01T00:00:00Z
        String epochSeconds = "1622505600";
        Instant expectedInstant = Instant.ofEpochSecond(Long.parseLong(epochSeconds));
        // WHEN
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(epochSeconds, converter);
        BigInteger expected = InstantConversions.toBigInteger(expectedInstant, converter);
        // THEN
        assertEquals(expected, actual, "String representing epoch seconds should be converted via the Converter to the correct BigInteger.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toBigInteger_validConversions(String description, Object from, Converter converter, BigInteger expected) {
        // WHEN
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(from, converter);
        // THEN
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toBigInteger_expectedExceptions(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toBigInteger(from, converter), description);
    }
}
