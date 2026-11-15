package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLocalDateTime_Test {

    /**
     * Converter used for all tests – created once for the whole test suite.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance.  We mock it because the tests do not depend on any overrides.
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        // The internal map of user‑defined overrides must be non‑null.
        when(mockOptions.getConverterOverrides()).thenReturn(new HashMap<>());
        converter = new Converter(mockOptions);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// ZonedDateTime – offset removed
        Arguments.of(ZonedDateTime.parse("2023-05-01T10:15:30+02:00[Europe/Paris]"), "2023-05-01T08:15:30"), // LocalDateTime – identity
        Arguments.of(LocalDateTime.of(2023, 5, 1, 10, 15, 30), "2023-05-01T10:15:30"), // java.util.Date – epoch millis
        Arguments.of(new Date(1682916930000L), "2023-04-30T22:15:30"), // Calendar – UTC
        Arguments.of(createUtcCalendar(1682916930000L), "2023-04-30T22:15:30"), // Long epoch milliseconds
        Arguments.of(1682916930000L, "2023-04-30T22:15:30"), // ISO‑8601 string without zone
        Arguments.of("2023-05-01T10:15:30", "2023-05-01T10:15:30"), // Timestamp with nanoseconds – full precision preserved
        Arguments.of(Timestamp.valueOf("2023-05-01 10:15:30.123456789"), "2023-05-01T10:15:30.123456789"), // Instant (UTC) – zone dropped
        Arguments.of(Instant.parse("2023-05-01T10:15:30Z"), "2023-05-01T10:15:30"), // OffsetDateTime +02:00 – offset removed
        Arguments.of(OffsetDateTime.parse("2023-05-01T10:15:30+02:00"), "2023-05-01T08:15:30"), // LocalDate – midnight
        Arguments.of(LocalDate.of(2023, 5, 1), "2023-05-01T00:00:00"), // Year – first day of year at midnight
        Arguments.of(Year.of(2023), "2023-01-01T00:00:00"), // Negative epoch millis (one day before epoch)
        Arguments.of(-86400000L, "1969-12-31T00:00:00"), // ISO‑8601 string with UTC designator – zone stripped
        Arguments.of("2023-05-01T10:15:30Z", "2023-05-01T10:15:30"), // Explicit null source – should return null
        Arguments.of(null, (String) null));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Unsupported type: Boolean
        Arguments.of(Boolean.TRUE, ConversionException.class), // Unsupported type: generic Object
        Arguments.of(new Object(), ConversionException.class), // Empty string
        Arguments.of("", ConversionException.class), // Invalid month in string
        Arguments.of("2023-13-01T10:15:30", ConversionException.class), // Invalid hour in string
        Arguments.of("2023-05-01T25:00:00", ConversionException.class), // Floating‑point number (epoch millis not allowed)
        Arguments.of(123.456, ConversionException.class), // BigDecimal not supported
        Arguments.of(new BigDecimal("123456789"), ConversionException.class), // LocalTime – lacks date component
        Arguments.of(LocalTime.of(10, 15, 30), ConversionException.class), // java.sql.Date – date only
        Arguments.of(java.sql.Date.valueOf("2023-05-01"), ConversionException.class), // ZoneId – not a temporal value
        Arguments.of(ZoneId.of("Europe/Paris"), ConversionException.class));
    }

    /**
     * Creates a {@link Calendar} instance set to UTC with the supplied epoch milliseconds.
     */
    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void toLocalDateTime_givenLocalDateTime_returnsSameInstance() {
        // strip nanos for deterministic equality
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime result = NumberConversions.toLocalDateTime(now, converter);
        assertEquals(now, result);
    }

    @Test
    void toLocalDateTime_givenZonedDateTime_returnsLocalDateTimePart() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneId.of("Europe/Paris"));
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(zdt, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenInstant_convertsUsingSystemDefaultZone() {
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(instant, systemZone);
        LocalDateTime result = NumberConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenUtilDate_convertsUsingInstantAndSystemZone() {
        Date date = Date.from(Instant.parse("2022-12-31T23:59:59Z"));
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(date.toInstant(), systemZone);
        LocalDateTime result = NumberConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenSqlTimestamp_convertsUsingInstantAndSystemZone() {
        Timestamp ts = Timestamp.valueOf("2021-07-15 08:15:30");
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = ts.toInstant().atZone(systemZone).toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenCalendar_convertsUsingInstantAndSystemZone() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2020, Calendar.FEBRUARY, 20, 10, 20, 30);
        cal.set(Calendar.MILLISECOND, 0);
        Instant instant = cal.toInstant();
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(instant, systemZone);
        LocalDateTime result = NumberConversions.toLocalDateTime(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenNull_returnsNull() {
        assertNull(NumberConversions.toLocalDateTime(null, converter));
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validConversionProvider")
    void testValidConversions(Object from, String expectedIso) {
        LocalDateTime result = NumberConversions.toLocalDateTime(from, converter);
        if (expectedIso == null) {
            assertNull(result, "Result should be null for null source");
        } else {
            LocalDateTime expected = LocalDateTime.parse(expectedIso);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] from={0} should throw {1}")
    @MethodSource("invalidConversionProvider")
    void testInvalidConversions(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toLocalDateTime(from, converter), "Expected conversion to fail for input: " + from);
    }

    @Test
    void testConverterInducedException_isPropagated() {
        // Create a mock Converter that throws a RuntimeException when any conversion is attempted.
        Converter failingConverter = mock(Converter.class);
        when(failingConverter.convert(any(), any())).thenThrow(new RuntimeException("converter failure"));
        // The source string is valid; the failure should come from the converter itself.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> NumberConversions.toLocalDateTime("2023-05-01T10:15:30", failingConverter));
        assertEquals("converter failure", ex.getMessage());
    }
}
