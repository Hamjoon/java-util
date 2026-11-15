package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toBigDecimal_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a Converter with default options – this is sufficient for the built‑in conversions
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that computes the expected value by delegating to the same conversion chain
     * that {@code ZonedDateTimeConversions.toBigDecimal} uses internally.
     */
    private static BigDecimal expected(Object source, Converter conv) {
        Instant instant = ZonedDateTimeConversions.toInstant(source, conv);
        return InstantConversions.toBigDecimal(instant, conv);
    }

    /**
     * Provides the valid conversion test data extracted from the JSON specification.
     *
     * <p>Each {@link Arguments} contains:
     * <ul>
     *   <li>The source object (Date, Instant, ZonedDateTime, OffsetDateTime, Timestamp, Calendar, Long)</li>
     *   <li>The {@link Converter} instance to use (a real instance – equivalent to {@code MockConverter.INSTANCE})</li>
     * </ul>
     */
    private static Stream<Arguments> validConversionProvider() {
        // behaves like MockConverter.INSTANCE
        Converter mockConverter = new Converter(new ConverterOptions());
        // Date instance
        Date date = new Date(1_600_000_000_000L);
        // Instant instance
        Instant instant = Instant.parse("2023-01-01T12:34:56.789Z");
        // ZonedDateTime instance (UTC)
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_000_000, ZoneId.of("UTC"));
        // OffsetDateTime instance (parsed)
        OffsetDateTime odt = OffsetDateTime.parse("2022-12-31T23:59:59.999+02:00");
        // Timestamp instance
        Timestamp ts = new Timestamp(1_700_000_000_000L);
        // Calendar instance (UTC)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_800_000_000_000L);
        // Long epoch‑millis
        Long epochLong = 1_900_000_000_000L;
        return Stream.of(Arguments.of(date, mockConverter), Arguments.of(instant, mockConverter), Arguments.of(zdt, mockConverter), Arguments.of(odt, mockConverter), Arguments.of(ts, mockConverter), Arguments.of(cal, mockConverter), Arguments.of(epochLong, mockConverter));
    }

    /**
     * Provides the invalid conversion test data extracted from the JSON specification.
     *
     * <p>Each {@link Arguments} contains:
     * <ul>
     *   <li>The source object (may be {@code null})</li>
     *   <li>The {@link Converter} instance (may be {@code null})</li>
     *   <li>The expected exception class</li>
     * </ul>
     */
    private static Stream<Arguments> invalidConversionProvider() {
        Converter mockConverter = new Converter(new ConverterOptions());
        // Calendar for the case where a Calendar is built and then used
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0L);
        return Stream.of(// null source → NullPointerException (toInstant inspects the value)
        Arguments.of(null, mockConverter, NullPointerException.class), // Unsupported type Boolean → ConversionException
        Arguments.of(Boolean.TRUE, mockConverter, ConversionException.class), // Unparseable String → ConversionException
        Arguments.of("not-a-date", mockConverter, ConversionException.class), // Null converter → NullPointerException before any conversion logic
        Arguments.of(new Date(0L), null, NullPointerException.class));
    }

    @Test
    void toBigDecimal_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 3, 15, 12, 30, 45, 123_456_789, ZoneId.of("UTC"));
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(zdt, converter);
        BigDecimal exp = expected(zdt, converter);
        assertEquals(exp, actual, "Conversion from ZonedDateTime should match the delegated Instant conversion");
    }

    @Test
    void toBigDecimal_fromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 999_000_000, ZoneOffset.ofHours(-5));
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(odt, converter);
        BigDecimal exp = expected(odt, converter);
        assertEquals(exp, actual);
    }

    @Test
    void toBigDecimal_fromInstant() {
        Instant instant = Instant.now();
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(instant, converter);
        BigDecimal exp = expected(instant, converter);
        assertEquals(exp, actual);
    }

    @Test
    void toBigDecimal_fromDate() {
        // a fixed epoch‑millis value
        Date date = new Date(1_600_000_000_000L);
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(date, converter);
        BigDecimal exp = expected(date, converter);
        assertEquals(exp, actual);
    }

    @Test
    void toBigDecimal_fromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_600_000_123_456L);
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(cal, converter);
        BigDecimal exp = expected(cal, converter);
        assertEquals(exp, actual);
    }

    @Test
    void toBigDecimal_fromLongEpochMilli() {
        Long epochMilli = 1_600_000_123_456L;
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(epochMilli, converter);
        BigDecimal exp = expected(epochMilli, converter);
        assertEquals(exp, actual);
    }

    @Test
    void toBigDecimal_fromStringRepresentingInstant() {
        String iso = "2023-04-01T10:15:30.250Z";
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(iso, converter);
        BigDecimal exp = expected(iso, converter);
        assertEquals(exp, actual);
    }

    @Test
    void toBigDecimal_nullInputReturnsNull() {
        assertNull(ZonedDateTimeConversions.toBigDecimal(null, converter), "When the source value is null the conversion should return null");
    }

    @ParameterizedTest(name = "[{index}] valid conversion from {0}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_validConversions(Object source, Converter conv) {
        // Act
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(source, conv);
        // Expected value is computed via the same conversion chain
        BigDecimal expected = expected(source, conv);
        // Assert
        assertEquals(expected, actual, () -> "Conversion result for source [" + source + "] should match expected BigDecimal");
    }

    @ParameterizedTest(name = "[{index}] invalid conversion from {0} with converter {1}")
    @MethodSource("invalidConversionProvider")
    void toBigDecimal_invalidConversions(Object source, Converter conv, Class<? extends Throwable> expectedException) {
        // Act & Assert
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toBigDecimal(source, conv), () -> "Expected " + expectedException.getSimpleName() + " for source [" + source + "]");
    }
}
