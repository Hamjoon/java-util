package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.DATE;
import static com.cedarsoftware.util.convert.MapConversions.TIME;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toBigDecimal_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance – the default constructor provides a
        // sensible set of options for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that delegates to the public InstantConversions implementation so the expected
     *  value is produced by the same logic the class under test uses internally.
     */
    private static BigDecimal expectedFromInstant(Instant instant) {
        return InstantConversions.toBigDecimal(instant, new Converter(new ConverterOptions()));
    }

    /**
     * Supplies all valid inputs together with the expected {@link BigDecimal} result.
     */
    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// ---- Instant inputs -------------------------------------------------
        arguments(Instant.ofEpochSecond(10, 500_000_000L), new BigDecimal("10.5")), arguments(Instant.EPOCH, BigDecimal.ZERO), // ---- Date / Calendar ------------------------------------------------
        arguments(new Date(1_600_000_000_000L), BigDecimal.valueOf(1_600_000_000L)), arguments(createUtcCalendar(1_600_000_123_456L), BigDecimal.valueOf(1_600_000_123_456L)), // ---- Numeric inputs ------------------------------------------------
        arguments(Long.valueOf(1_234_567_890L), BigDecimal.valueOf(1_234_567_890L)), arguments(Double.valueOf(1_600_000_000_123.456), new BigDecimal("1600000000123.456")), // ---- String inputs --------------------------------------------------
        arguments("2023-01-01T12:34:56Z", BigDecimal.valueOf(1672576496L)), arguments("2023-01-01 12:34:56+02:00", BigDecimal.valueOf(1672569296L)), // ---- ZonedDateTime --------------------------------------------------
        arguments(ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneOffset.UTC), BigDecimal.valueOf(1672576496L)), arguments(ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 0, // +01:00
        ZoneId.of("Europe/Paris")), BigDecimal.valueOf(1672572896L)), // ---- OffsetDateTime -------------------------------------------------
        arguments(OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneOffset.ofHoursMinutes(2, 30)), BigDecimal.valueOf(1672568896L)), // ---- LocalDateTime (interpreted as UTC) -----------------------------
        arguments(LocalDateTime.of(2023, 1, 1, 0, 0, 0, 0), BigDecimal.valueOf(1672531200L)), // ---- Map input (DATE/TIME/ZONE) ------------------------------------
        arguments(Map.of(DATE, new Date(0L), TIME, new Date(0L), ZONE, ZoneId.of("UTC")), BigDecimal.ZERO), // ---- AtomicLong ----------------------------------------------------
        arguments(new AtomicLong(123_456_789L), BigDecimal.valueOf(123_456_789L)), // ---- BigInteger -----------------------------------------------------
        arguments(new BigInteger("9876543210"), BigDecimal.valueOf(9_876_543_210L)), // ---- java.sql.Timestamp ---------------------------------------------
        arguments(Timestamp.valueOf("2020-09-13 12:34:56.789"), BigDecimal.valueOf(1_600_000_000_789L)), // ---- java.sql.Date -------------------------------------------------
        arguments(java.sql.Date.valueOf("2020-09-13"), BigDecimal.valueOf(java.sql.Date.valueOf("2020-09-13").getTime())), // ---- Edge case – negative epoch (pre‑1970) -------------------------
        arguments(new Date(-1_234L), new BigDecimal("-1.234")), // ---- Edge case – Instant with fractional nanoseconds ----------------
        arguments(Instant.ofEpochSecond(10, 123_456_789L), new BigDecimal("10.123456789")), // ---- Edge case – Instant.MAX ----------------------------------------
        arguments(Instant.MAX, expectedFromInstant(Instant.MAX)), // ---- Edge case – Instant.MIN ----------------------------------------
        arguments(Instant.MIN, expectedFromInstant(Instant.MIN)));
    }

    /**
     * Helper that creates a {@link Calendar} instance fixed to UTC and set to the supplied epoch‑millis.
     */
    private static Calendar createUtcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /**
     * Supplies all invalid conversion scenarios.
     */
    static Stream<Arguments> invalidConversionProvider() {
        // Map with missing keys
        Map<Object, Object> mapMissingTime = Map.of(DATE, new Date(0L));
        // Map with wrong value types
        Map<Object, Object> mapBadDate = Map.of(DATE, "not a date", TIME, new Date(0L), ZONE, ZoneId.of("UTC"));
        Map<Object, Object> mapBadTime = Map.of(DATE, new Date(0L), TIME, "not a date", ZONE, ZoneId.of("UTC"));
        Map<Object, Object> mapBadZone = Map.of(DATE, new Date(0L), TIME, new Date(0L), ZONE, "UTC");
        // Map with null values for required keys
        Map<Object, Object> mapNullDate = Map.of(DATE, null, TIME, new Date(0L), ZONE, ZoneId.of("UTC"));
        Map<Object, Object> mapNullTime = Map.of(DATE, new Date(0L), TIME, null, ZONE, ZoneId.of("UTC"));
        Map<Object, Object> mapNullZone = Map.of(DATE, new Date(0L), TIME, new Date(0L), ZONE, null);
        return Stream.of(// null source
        arguments(null, new Converter(new ConverterOptions()), NullPointerException.class), // unsupported type (plain Object)
        arguments(new Object(), new Converter(new ConverterOptions()), IllegalArgumentException.class), // null converter
        arguments(Instant.EPOCH, null, NullPointerException.class), // map missing required keys
        arguments(mapMissingTime, new Converter(new ConverterOptions()), IllegalArgumentException.class), // map with incorrect value types
        arguments(mapBadDate, new Converter(new ConverterOptions()), IllegalArgumentException.class), arguments(mapBadTime, new Converter(new ConverterOptions()), IllegalArgumentException.class), arguments(mapBadZone, new Converter(new ConverterOptions()), IllegalArgumentException.class), // malformed date‑time strings
        arguments("2023-13-01T99:99:99Z", new Converter(new ConverterOptions()), DateTimeParseException.class), arguments("not-a-date", new Converter(new ConverterOptions()), DateTimeParseException.class), // unsupported numeric type (Float)
        arguments(Float.valueOf(123.45f), new Converter(new ConverterOptions()), IllegalArgumentException.class), // map with null values for required entries
        arguments(mapNullDate, new Converter(new ConverterOptions()), IllegalArgumentException.class), arguments(mapNullTime, new Converter(new ConverterOptions()), IllegalArgumentException.class), arguments(mapNullZone, new Converter(new ConverterOptions()), IllegalArgumentException.class));
    }

    @Test
    void toBigDecimal_fromZonedDateTime_returnsExpectedValue() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 12, 34, 56, 123_456_789, ZoneId.of("America/New_York"));
        // WHEN
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(zdt, converter);
        // THEN
        BigDecimal expected = expectedFromInstant(zdt.toInstant());
        assertEquals(expected, actual, "BigDecimal conversion from ZonedDateTime is incorrect");
    }

    @Test
    void toBigDecimal_fromOffsetDateTime_returnsExpectedValue() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2022, 11, 3, 8, 15, 30, 0, ZoneOffset.ofHours(-5));
        // WHEN
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(odt, converter);
        // THEN
        BigDecimal expected = expectedFromInstant(odt.toInstant());
        assertEquals(expected, actual);
    }

    @Test
    void toBigDecimal_fromInstant_returnsExpectedValue() {
        // GIVEN
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        // WHEN
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(instant, converter);
        // THEN
        BigDecimal expected = expectedFromInstant(instant);
        assertEquals(expected, actual);
    }

    @Test
    void toBigDecimal_fromDate_returnsExpectedValue() {
        // GIVEN
        // current moment
        Date date = new Date();
        // WHEN
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(date, converter);
        // THEN
        BigDecimal expected = expectedFromInstant(date.toInstant());
        assertEquals(expected, actual);
    }

    @Test
    void toBigDecimal_fromCalendar_returnsExpectedValue() {
        // GIVEN
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        // WHEN
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(cal, converter);
        // THEN
        BigDecimal expected = expectedFromInstant(cal.toInstant());
        assertEquals(expected, actual);
    }

    @Test
    void toBigDecimal_fromLongEpochMilli_returnsExpectedValue() {
        // GIVEN
        long epochMilli = System.currentTimeMillis();
        // WHEN
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(epochMilli, converter);
        // THEN
        BigDecimal expected = expectedFromInstant(Instant.ofEpochMilli(epochMilli));
        assertEquals(expected, actual);
    }

    @Test
    void toBigDecimal_nullInput_returnsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(input, converter);
        // THEN
        assertNull(actual, "Converting null should yield null");
    }

    /**
     * Parameterized test that iterates over all *valid* conversion scenarios.
     * The expected {@link BigDecimal} is either computed directly or derived from an {@link Instant}
     * using the {@code expectedFromInstant} helper.
     */
    @ParameterizedTest(name = "[{index}] toBigDecimal({0}) = {1}")
    @MethodSource("validConversionProvider")
    void testValidConversions(Object from, BigDecimal expected) {
        BigDecimal actual = ZonedDateTimeConversions.toBigDecimal(from, converter);
        assertEquals(expected, actual, () -> "Unexpected conversion result for input: " + Objects.toString(from));
    }

    /**
     * Parameterized test for inputs that must result in an exception.
     * The expected exception type is asserted via {@link Assertions#assertThrows}.
     */
    @ParameterizedTest(name = "[{index}] toBigDecimal({0}) throws {2}")
    @MethodSource("invalidConversionProvider")
    void testInvalidConversions(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toBigDecimal(from, conv), () -> "Expected exception " + expectedException.getSimpleName() + " for input: " + from);
    }
}
