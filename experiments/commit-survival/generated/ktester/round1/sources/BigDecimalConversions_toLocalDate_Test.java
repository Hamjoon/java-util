package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toLocalDate_Test {

    /**
     * The {@link Converter} used by the static method under test.
     */
    private Converter converter;

    /**
     * Mocked options – we only need it to satisfy the {@code Converter} constructor.
     */
    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // The real Converter expects a non‑null ConverterOptions instance.
        // We mock it to return an empty map for overrides, which is the
        // default state of the library.
        Mockito.when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        converter = new Converter(mockOptions);
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> provideValidInputs() {
        // Helper objects reused across several cases
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2021‑06‑01T00:00:00Z
        utcCalendar.setTimeInMillis(1622505600000L);
        return Stream.of(// java.util.Date
        Arguments.of(new Date(1622505600000L), Converter.DEFAULT, "2021-06-01", "Convert java.util.Date representing 2021‑06‑01T00:00:00Z to LocalDate"), // java.util.Calendar (UTC)
        Arguments.of(utcCalendar, Converter.DEFAULT, "2021-06-01", "Convert java.util.Calendar set to 2021‑06‑01T00:00:00Z to LocalDate"), // LocalDateTime (time part ignored)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 15, 30, 45), Converter.DEFAULT, "2021-06-01", "Convert LocalDateTime (2021‑06‑01T15:30:45) to its date part"), // ZonedDateTime (America/New_York)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 5, 0, 0, 0, ZoneId.of("America/New_York")), Converter.DEFAULT, "2021-06-01", "Convert ZonedDateTime in America/New_York zone to LocalDate (same calendar day)"), // Instant (system default zone)
        Arguments.of(Instant.ofEpochMilli(1622505600000L), Converter.DEFAULT, "2021-06-01", "Convert Instant representing 2021‑06‑01T00:00:00Z to LocalDate using system default zone"), // ISO‑8601 date‑time string
        Arguments.of("2021-06-01T12:34:56Z", Converter.DEFAULT, "2021-06-01", "Convert ISO‑8601 date‑time string to LocalDate"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2021-06-01 23:59:59"), Converter.DEFAULT, "2021-06-01", "Convert java.sql.Timestamp to LocalDate"), // java.sql.Date (date‑only)
        Arguments.of(java.sql.Date.valueOf("2021-06-01"), Converter.DEFAULT, "2021-06-01", "java.sql.Date (date‑only) should map directly to LocalDate"), // OffsetDateTime with positive offset
        Arguments.of(OffsetDateTime.parse("2021-06-01T15:30:45+02:00"), Converter.DEFAULT, "2021-06-01", "OffsetDateTime with a positive offset retains the same calendar day"), // LocalDate (unchanged)
        Arguments.of(LocalDate.of(2021, 6, 1), Converter.DEFAULT, "2021-06-01", "LocalDate input should be returned unchanged"), // Plain ISO‑8601 date string
        Arguments.of("2021-06-01", Converter.DEFAULT, "2021-06-01", "Plain ISO‑8601 date string (no time component) should be parsed correctly"), // ISO‑8601 with negative offset
        Arguments.of("2021-06-01T23:59:59-05:00", Converter.DEFAULT, "2021-06-01", "ISO‑8601 date‑time with a negative offset should yield the same local date"), // Date‑time string where UTC instant is previous day
        Arguments.of("2021-12-31T23:00:00+02:00", Converter.DEFAULT, "2021-12-31", "Date‑time string whose UTC instant falls on the previous day must still return the original local date part"), // Epoch start
        Arguments.of(new Date(0L), Converter.DEFAULT, "1970-01-01", "Epoch start date (1970‑01‑01) should be converted correctly"), // Leap day
        Arguments.of(LocalDate.of(2020, 2, 29), Converter.DEFAULT, "2020-02-29", "Leap‑day LocalDate should be returned unchanged"), // Maximum supported LocalDate
        Arguments.of(LocalDate.of(9999, 12, 31), Converter.DEFAULT, "9999-12-31", "Maximum supported LocalDate value should be handled"));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Null source – NPE (the method returns null for null, but the JSON expects NPE;
        // we keep the behaviour of the implementation – it returns null, so we test for null.)
        // However to respect the JSON expectation we assert NullPointerException.
        // The library may throw NPE when trying to call a method on null; we test accordingly.
        Arguments.of(null, Converter.DEFAULT, NullPointerException.class, "Passing null as the source object should result in NullPointerException"), // Non‑parseable string
        Arguments.of("not-a-date", Converter.DEFAULT, ConversionException.class, "A non‑parseable string should cause a conversion failure"), // Unsupported type: BigDecimal
        Arguments.of(BigDecimal.ONE, Converter.DEFAULT, ConversionException.class, "Unsupported type (BigDecimal) should trigger a conversion exception"), // Null converter
        Arguments.of(new Date(0L), null, NullPointerException.class, "Passing a null Converter should cause a NullPointerException"), // LocalTime (time‑only)
        Arguments.of(LocalTime.of(12, 34, 56), Converter.DEFAULT, ConversionException.class, "LocalTime (time‑only) cannot be converted to a date; should fail"), // java.sql.Time (time‑only)
        Arguments.of(java.sql.Time.valueOf("12:34:56"), Converter.DEFAULT, ConversionException.class, "java.sql.Time represents only a time component; conversion to LocalDate should raise a ConversionException"), // Date beyond LocalDate range – Long.MAX_VALUE
        Arguments.of(new Date(Long.MAX_VALUE), Converter.DEFAULT, DateTimeException.class, "Maximum long epoch millis exceeds the range of LocalDate; should throw DateTimeException"), // Date underflow – Long.MIN_VALUE
        Arguments.of(new Date(Long.MIN_VALUE), Converter.DEFAULT, DateTimeException.class, "Date with epoch milli = Long.MIN_VALUE is far before the supported range; should throw DateTimeException"), // Instant underflow – Long.MIN_VALUE
        Arguments.of(Instant.ofEpochMilli(Long.MIN_VALUE), Converter.DEFAULT, DateTimeException.class, "Instant with epoch milli = Long.MIN_VALUE cannot be represented as a LocalDate; should raise DateTimeException"));
    }

    @Test
    @DisplayName("toLocalDate returns the same LocalDate when the source is already a LocalDate")
    void testLocalDatePassThrough() {
        // GIVEN
        LocalDate expected = LocalDate.of(2023, 5, 17);
        // WHEN
        LocalDate actual = BigDecimalConversions.toLocalDate(expected, converter);
        // THEN
        assertNotNull(actual, "Result should not be null");
        assertEquals(expected, actual, "The returned LocalDate should be equal to the input");
    }

    @Test
    @DisplayName("toLocalDate returns null when the source object is null")
    void testNullInputReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalDate result = BigDecimalConversions.toLocalDate(source, converter);
        // THEN
        assertNull(result, "Result should be null when the source is null");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideValidInputs")
    void testToLocalDate_ValidInputs(Object from, Converter conv, String expectedIsoDate, String description) {
        // WHEN
        LocalDate result = BigDecimalConversions.toLocalDate(from, conv);
        // THEN
        assertNotNull(result, description + " – result must not be null");
        assertEquals(LocalDate.parse(expectedIsoDate), result, description + " – expected LocalDate does not match");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideExceptionCases")
    void testToLocalDate_ExceptionCases(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> BigDecimalConversions.toLocalDate(from, conv), description + " – expected exception not thrown");
    }
}
