package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toLocalDate_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a ConverterOptions instance; the default constructor is sufficient.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// already a LocalDate
        Arguments.of(LocalDate.of(2023, 5, 10), null, LocalDate.of(2023, 5, 10)), // LocalDateTime → ZonedDateTime (system default) → LocalDate
        Arguments.of(LocalDateTime.of(2023, 5, 10, 15, 30, 45), null, LocalDate.of(2023, 5, 10)), // ZonedDateTime → LocalDate
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.systemDefault()), null, LocalDate.of(2023, 5, 10)), // OffsetDateTime → ZonedDateTime → LocalDate
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC), null, LocalDate.of(2023, 5, 10)), // Instant → system default zone → LocalDate
        Arguments.of(Instant.ofEpochSecond(1683696000L), null, LocalDate.of(2023, 5, 10)), // java.util.Date (epoch ms) → LocalDate
        Arguments.of(new Date(1683696000000L), null, LocalDate.of(2023, 5, 10)), // java.sql.Timestamp → LocalDate
        Arguments.of(new Timestamp(1683696000000L), null, LocalDate.of(2023, 5, 10)), // Calendar instance with known epoch ms
        Arguments.of(createCalendar(1683696000000L), null, LocalDate.of(2023, 5, 10)), // Long epoch millis
        Arguments.of(Long.valueOf(1683696000000L), null, LocalDate.of(2023, 5, 10)), // String ISO‑8601 date
        Arguments.of("2023-05-10", null, LocalDate.of(2023, 5, 10)), // BigInteger epoch millis (numeric group)
        Arguments.of(BigInteger.valueOf(1683696000000L), null, LocalDate.of(2023, 5, 10)), // BigDecimal epoch millis
        Arguments.of(new BigDecimal("1683696000000"), null, LocalDate.of(2023, 5, 10)), // java.sql.Date directly represents a date
        Arguments.of(java.sql.Date.valueOf("2023-05-10"), null, LocalDate.of(2023, 5, 10)));
    }

    private static Calendar createCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// null source
        Arguments.of(null, NullPointerException.class, "null source should NPE"), // invalid month
        Arguments.of("2023-13-01", DateTimeParseException.class, "invalid month"), // non‑leap year Feb 29
        Arguments.of("2023-02-29", DateTimeParseException.class, "invalid leap day"), // empty string
        Arguments.of("", DateTimeParseException.class, "empty string"), // non‑date string
        Arguments.of("not-a-date", DateTimeParseException.class, "garbage string"), // LocalTime (no date)
        Arguments.of(LocalTime.of(15, 30, 45), IllegalArgumentException.class, "LocalTime unsupported"), // Duration
        Arguments.of(Duration.ofHours(5), IllegalArgumentException.class, "Duration unsupported"), // UUID
        Arguments.of(UUID.randomUUID(), IllegalArgumentException.class, "UUID unsupported"), // Integer
        Arguments.of(Integer.valueOf(20230510), IllegalArgumentException.class, "Integer unsupported"));
    }

    private static Stream<Arguments> unsupportedTemporalProvider() {
        return Stream.of(Arguments.of(OffsetTime.of(15, 30, 45, 0, ZoneOffset.UTC), IllegalArgumentException.class), Arguments.of(YearMonth.of(2023, 5), DateTimeException.class), Arguments.of(MonthDay.of(5, 10), DateTimeException.class));
    }

    @Test
    void toLocalDate_fromZeroEpochMillis_returnsEpochDate() {
        // GIVEN
        // 1970‑01‑01T00:00:00Z
        BigInteger epochMillis = BigInteger.ZERO;
        // WHEN
        LocalDate result = BigIntegerConversions.toLocalDate(epochMillis, converter);
        // THEN
        assertEquals(LocalDate.of(1970, 1, 1), result, "BigInteger 0 should convert to the epoch LocalDate (1970‑01‑01)");
    }

    @Test
    void toLocalDate_fromOneDayEpochMillis_returnsNextDay() {
        // GIVEN
        // 1 day in ms
        BigInteger oneDayMillis = BigInteger.valueOf(86_400_000L);
        // WHEN
        LocalDate result = BigIntegerConversions.toLocalDate(oneDayMillis, converter);
        // THEN
        assertEquals(LocalDate.of(1970, 1, 2), result, "BigInteger representing one day in millis should convert to 1970‑01‑02");
    }

    @Test
    void toLocalDate_fromNegativeEpochMillis_returnsPreviousDate() {
        // GIVEN
        // -1 day in milliseconds
        BigInteger negativeOneDayMillis = BigInteger.valueOf(-86_400_000L);
        // WHEN
        LocalDate result = BigIntegerConversions.toLocalDate(negativeOneDayMillis, converter);
        // THEN
        assertEquals(LocalDate.of(1969, 12, 31), result, "Negative epoch millis should correctly map to the prior calendar date");
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {2}")
    @MethodSource("validConversionProvider")
    void toLocalDate_validConversions(Object from, Converter ignored, LocalDate expected) {
        // WHEN
        LocalDate result = BigIntegerConversions.toLocalDate(from, converter);
        // THEN
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from {0} throws {1}")
    @MethodSource("invalidConversionProvider")
    void toLocalDate_invalidInputs(Object from, Class<? extends Throwable> expectedException, String _desc) {
        // WHEN / THEN
        assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(from, converter));
    }

    @Test
    void toLocalDate_longZeroEpochMillis_boundary() {
        // GIVEN
        Long zero = Long.valueOf(0L);
        // WHEN
        LocalDate result = BigIntegerConversions.toLocalDate(zero, converter);
        // THEN
        assertEquals(LocalDate.of(1970, 1, 1), result);
    }

    @Test
    void toLocalDate_longMaxValue_overflow() {
        // GIVEN
        Long max = Long.valueOf(Long.MAX_VALUE);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toLocalDate(max, converter), "Long.MAX_VALUE epoch millis should overflow and throw DateTimeException");
    }

    @ParameterizedTest(name = "[{index}] {0} throws {1}")
    @MethodSource("unsupportedTemporalProvider")
    void toLocalDate_unsupportedTemporalTypes(Object from, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> BigIntegerConversions.toLocalDate(from, converter));
    }

    @Test
    void toLocalDate_customConverter_translatesString() {
        // Arrange a mock Converter that knows how to turn "custom-date" into a ZonedDateTime
        Converter mockConverter = mock(Converter.class);
        ZonedDateTime mockedZdt = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        when(mockConverter.convert(eq("custom-date"), eq(ZonedDateTime.class))).thenReturn(mockedZdt);
        // WHEN
        LocalDate result = BigIntegerConversions.toLocalDate("custom-date", mockConverter);
        // THEN
        assertEquals(LocalDate.of(2000, 1, 1), result, "Custom converter should map \"custom-date\" to 2000‑01‑01");
        // Verify that the mock was indeed called
        verify(mockConverter, times(1)).convert(eq("custom-date"), eq(ZonedDateTime.class));
    }
}
