package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toBigDecimal_Test {

    /**
     * Shared {@link Converter} instance used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
        // The default options are sufficient for the conversion tests.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that mirrors the internal algorithm to obtain the expected result.
     */
    private static BigDecimal expectedFromInstant(Instant instant) {
        return InstantConversions.toBigDecimal(instant, converter);
    }

    private static Stream<Arguments> provideVariousValidInputs() {
        Instant instant = Instant.parse("2021-06-01T12:34:56.789Z");
        BigDecimal bdInstant = new BigDecimal("1622553296.789");
        BigDecimal bdLocalDate = new BigDecimal("1622505600");
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(instant.toEpochMilli());
        return Stream.of(// Direct Instant
        Arguments.of(instant, new Expected(bdInstant), "Direct Instant input should be converted to a BigDecimal representing epoch seconds with nanosecond precision."), // java.util.Date
        Arguments.of(Date.from(instant), new Expected(bdInstant), "java.util.Date should be converted to the same epoch‑second BigDecimal as the equivalent Instant."), // java.sql.Timestamp
        Arguments.of(Timestamp.from(instant), new Expected(bdInstant), "java.sql.Timestamp (which carries nanoseconds) should yield the identical BigDecimal value."), // LocalDateTime (interpreted as UTC)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 34, 56, 789_000_000), new Expected(bdInstant), "LocalDateTime interpreted as UTC should be converted to the same epoch‑second BigDecimal."), // LocalDate (midnight UTC)
        Arguments.of(LocalDate.of(2021, 6, 1), new Expected(bdLocalDate), "LocalDate should be treated as midnight UTC of that date, yielding the epoch‑second BigDecimal for 2021‑06‑01T00:00:00Z."), // Calendar set to the same instant
        Arguments.of(utcCal, new Expected(bdInstant), "A GregorianCalendar set to the same instant should produce the identical BigDecimal."), // Long epoch‑millis
        Arguments.of(Long.valueOf(1622553296789L), new Expected(bdInstant), "Long representing epoch milliseconds should be converted to seconds with fractional part."), // ISO‑8601 String
        Arguments.of("2021-06-01T12:34:56.789Z", new Expected(bdInstant), "ISO‑8601 string representation of an instant should be parsed and converted to the same BigDecimal."), // Null source
        Arguments.of(null, new Expected(null), "A null source object should result in a null return value (no conversion performed)."), // Unsupported Boolean – should raise IllegalArgumentException
        Arguments.of(Boolean.TRUE, new Expected(IllegalArgumentException.class), "Unsupported source type (Boolean) must cause an IllegalArgumentException."), // LocalTime without date – should raise IllegalArgumentException
        Arguments.of(LocalTime.of(12, 34, 56), new Expected(IllegalArgumentException.class), "LocalTime alone cannot be mapped to an instant without a date component; conversion should fail with IllegalArgumentException."));
    }

    private static Stream<Arguments> provideUnsupportedTypes() {
        return Stream.of(Arguments.of(Integer.valueOf(42), "Integer type is not supported and should trigger IllegalArgumentException."), Arguments.of(Double.valueOf(3.14), "Double type is not supported and should trigger IllegalArgumentException."), Arguments.of(new Object(), "Arbitrary Object instances are unsupported and should cause IllegalArgumentException."), Arguments.of(Map.of("key", "value"), "Map instances are not convertible to an Instant and must raise IllegalArgumentException."));
    }

    private static Stream<Arguments> provideInvalidStrings() {
        return Stream.of(Arguments.of("2021-13-01T00:00:00Z", "String with an invalid month (13) cannot be parsed and should throw DateTimeParseException."), Arguments.of("not-a-date", "Non‑date textual input should be rejected with a DateTimeParseException."), Arguments.of("", "Empty string cannot be parsed as an instant and must raise DateTimeParseException."));
    }

    @Test
    void toBigDecimal_givenInstant_returnsSameBigDecimal() {
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        BigDecimal expected = expectedFromInstant(instant);
        BigDecimal actual = LocalDateConversions.toBigDecimal(instant, converter);
        assertEquals(expected, actual, "BigDecimal conversion for Instant should be delegated unchanged");
    }

    @Test
    void toBigDecimal_givenDate_returnsSameBigDecimal() {
        Instant instant = Instant.parse("2022-06-15T08:20:30Z");
        Date date = Date.from(instant);
        BigDecimal expected = expectedFromInstant(instant);
        BigDecimal actual = LocalDateConversions.toBigDecimal(date, converter);
        assertEquals(expected, actual, "Date should be converted via its underlying Instant");
    }

    @Test
    void toBigDecimal_givenTimestamp_returnsSameBigDecimal() {
        Instant instant = Instant.parse("2021-12-31T23:59:59Z");
        Timestamp ts = Timestamp.from(instant);
        BigDecimal expected = expectedFromInstant(instant);
        BigDecimal actual = LocalDateConversions.toBigDecimal(ts, converter);
        assertEquals(expected, actual, "Timestamp conversion should match Instant conversion");
    }

    @Test
    void toBigDecimal_givenLocalDateTime_returnsSameBigDecimal() {
        // LocalDateTime has no zone; the conversion in LocalDateConversions uses UTC.
        LocalDateTime ldt = LocalDateTime.of(2020, 2, 29, 14, 15, 16);
        Instant instant = ldt.toInstant(ZoneOffset.UTC);
        BigDecimal expected = expectedFromInstant(instant);
        BigDecimal actual = LocalDateConversions.toBigDecimal(ldt, converter);
        assertEquals(expected, actual, "LocalDateTime should be interpreted as UTC before conversion");
    }

    @Test
    void toBigDecimal_givenNull_returnsNull() {
        // According to Converter contract, a null source yields null for object return types.
        assertNull(LocalDateConversions.toBigDecimal(null, converter), "Converting a null source should return null");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideVariousValidInputs")
    void testToBigDecimalVariousInputs(Object from, Expected expected, String description) {
        if (expected.value() instanceof Class<?>) {
            // Expected an exception.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected.value();
            assertThrows(exClass, () -> LocalDateConversions.toBigDecimal(from, converter), description);
        } else if (expected.value() == null) {
            // Expected a null result.
            assertNull(LocalDateConversions.toBigDecimal(from, converter), description);
        } else {
            // Expected a concrete BigDecimal.
            BigDecimal actual = LocalDateConversions.toBigDecimal(from, converter);
            assertEquals(expected.value(), actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideUnsupportedTypes")
    void testToBigDecimalUnsupportedTypes(Object from, String description) {
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toBigDecimal(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideInvalidStrings")
    void testToBigDecimalInvalidString(Object from, String description) {
        assertThrows(DateTimeParseException.class, () -> LocalDateConversions.toBigDecimal(from, converter), description);
    }
}
