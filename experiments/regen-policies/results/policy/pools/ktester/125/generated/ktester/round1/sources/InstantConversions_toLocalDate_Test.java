package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLocalDate_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter has a public constructor that requires ConverterOptions.
        // Using default options is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    private static Stream<Arguments> zonedDateTimeProvider() {
        return Stream.of(Arguments.of("ZonedDateTime UTC – simple date extraction", ZonedDateTime.of(2023, 5, 10, 15, 30, 0, 0, ZoneOffset.UTC), null, LocalDate.of(2023, 5, 10)), Arguments.of("ZonedDateTime with non‑UTC zone – date after zone adjustment", ZonedDateTime.of(2023, 12, 31, 23, 0, 0, 0, ZoneId.of("America/New_York")), null, LocalDate.of(2023, 12, 31)));
    }

    private static Stream<Arguments> instantProvider() {
        return Stream.of(Arguments.of("Instant at midday UTC", Instant.parse("2023-05-10T15:30:00Z"), null, LocalDate.of(2023, 5, 10)), Arguments.of("Instant close to midnight UTC – no rollover", Instant.parse("2023-12-31T23:30:00Z"), null, LocalDate.of(2023, 12, 31)));
    }

    @Test
    void toLocalDate_fromInstant_returnsCorrectDate() {
        // GIVEN
        // 2023‑03‑15
        Instant instant = Instant.parse("2023-03-15T10:20:30Z");
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(instant, converter);
        // THEN
        assertEquals(LocalDate.of(2023, 3, 15), result);
    }

    @Test
    void toLocalDate_fromLongEpochMillis_returnsCorrectDate() {
        // GIVEN
        // 2023‑03‑15T00:00:00Z
        long epochMillis = 1678838400000L;
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(epochMillis, converter);
        // THEN
        assertEquals(LocalDate.of(2023, 3, 15), result);
    }

    @Test
    void toLocalDate_fromDate_returnsCorrectDate() {
        // GIVEN
        java.util.Date date = java.util.Date.from(Instant.parse("2022-12-01T05:00:00Z"));
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(date, converter);
        // THEN
        assertEquals(LocalDate.of(2022, 12, 1), result);
    }

    @Test
    void toLocalDate_fromString_returnsCorrectDate() {
        // GIVEN
        String isoString = "2021-07-04";
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(isoString, converter);
        // THEN
        assertEquals(LocalDate.of(2021, 7, 4), result);
    }

    @Test
    void toLocalDate_nullInput_returnsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(nullObj, converter);
        // THEN
        assertNull(result);
    }

    @Test
    void toLocalDate_fromZonedDateTime_returnsCorrectDate() {
        // GIVEN
        java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(2020, 5, 20, 13, 45, 0, 0, ZoneOffset.UTC);
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(zdt, converter);
        // THEN
        assertEquals(LocalDate.of(2020, 5, 20), result);
    }

    // ---------- ZonedDateTime ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("zonedDateTimeProvider")
    void toLocalDate_fromZonedDateTime(String description, ZonedDateTime source, Converter conv, LocalDate expected) {
        LocalDate result = InstantConversions.toLocalDate(source, conv);
        assertEquals(expected, result, description);
    }

    // ---------- Instant ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("instantProvider")
    void toLocalDate_fromInstant(String description, Instant source, Converter conv, LocalDate expected) {
        LocalDate result = InstantConversions.toLocalDate(source, conv);
        assertEquals(expected, result, description);
    }

    // ---------- java.util.Date ----------
    @Test
    void toLocalDate_fromUtilDate() {
        Date date = Date.from(Instant.parse("2023-05-10T00:00:00Z"));
        LocalDate result = InstantConversions.toLocalDate(date, null);
        assertEquals(LocalDate.of(2023, 5, 10), result);
    }

    // ---------- java.sql.Timestamp ----------
    @Test
    void toLocalDate_fromTimestamp() {
        Timestamp ts = Timestamp.valueOf("2023-05-10 12:34:56");
        LocalDate result = InstantConversions.toLocalDate(ts, null);
        assertEquals(LocalDate.of(2023, 5, 10), result);
    }

    // ---------- Calendar ----------
    @Test
    void toLocalDate_fromCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2023, Calendar.MAY, 10, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        LocalDate result = InstantConversions.toLocalDate(calendar, null);
        assertEquals(LocalDate.of(2023, 5, 10), result);
    }

    // ---------- Long epoch milliseconds ----------
    @Test
    void toLocalDate_fromLongEpochMillis() {
        Long epoch = Long.valueOf(Instant.parse("2023-05-10T00:00:00Z").toEpochMilli());
        LocalDate result = InstantConversions.toLocalDate(epoch, null);
        assertEquals(LocalDate.of(2023, 5, 10), result);
    }

    // ---------- ISO‑8601 String (parsable) ----------
    @Test
    void toLocalDate_fromIsoString() {
        String iso = "2023-05-10T15:30:00Z";
        LocalDate result = InstantConversions.toLocalDate(iso, null);
        assertEquals(LocalDate.of(2023, 5, 10), result);
    }

    // ---------- ISO‑8601 String (non‑parsable) ----------
    @Test
    void toLocalDate_fromNonParsableString_throwsDateTimeParseException() {
        String bad = "not-a-date";
        assertThrows(DateTimeParseException.class, () -> InstantConversions.toLocalDate(bad, null), "Expected DateTimeParseException for non‑parsable string");
    }

    // ---------- Invalid ISO‑8601 offset ----------
    @Test
    void toLocalDate_invalidOffsetString_throwsDateTimeException() {
        String badOffset = "2023-05-10T15:30:00+25:00";
        assertThrows(DateTimeException.class, () -> InstantConversions.toLocalDate(badOffset, null), "Expected DateTimeException for invalid zone offset");
    }

    // ---------- Unsupported type ----------
    @Test
    void toLocalDate_unsupportedObject_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDate(unsupported, null), "Unsupported source type must cause IllegalArgumentException");
    }

    // ---------- Unsupported numeric type ----------
    @Test
    void toLocalDate_unsupportedNumeric_throwsIllegalArgumentException() {
        Integer unsupported = Integer.valueOf(12345);
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDate(unsupported, null), "Unsupported numeric type must cause IllegalArgumentException");
    }

    // ---------- Unsupported collection type ----------
    @Test
    void toLocalDate_unsupportedCollection_throwsIllegalArgumentException() {
        List<String> list = Arrays.asList("2023-05-10");
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDate(list, null), "Unsupported collection type must cause IllegalArgumentException");
    }

    // ---------- Null input handling ----------
    @Test
    void toLocalDate_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> InstantConversions.toLocalDate(null, null), "Null source should result in NullPointerException");
    }

    // ---------- Custom Converter that throws exception ----------
    @Test
    void toLocalDate_converterThrowsException_propagates(@Mock Converter failingConverter) {
        // Arrange: make any conversion attempt throw IllegalStateException
        doThrow(new IllegalStateException("converter failure")).when(failingConverter).convert(any(), any());
        String iso = "2023-05-10T15:30:00Z";
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> InstantConversions.toLocalDate(iso, failingConverter));
        assertEquals("converter failure", ex.getMessage());
    }

    // ---------- Custom Converter that interprets plain date strings ----------
    @Test
    void toLocalDate_customConverterInterpretsPlainDateString() {
        // A minimal custom Converter that simply returns the input string unchanged.
        // The internal conversion logic of InstantConversions will then parse it.
        Converter custom = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                // Directly return the string; let InstantConversions handle parsing.
                @SuppressWarnings("unchecked")
                T result = (T) from;
                return result;
            }
        };
        String plainDate = "2023-05-10";
        LocalDate result = InstantConversions.toLocalDate(plainDate, custom);
        assertEquals(LocalDate.of(2023, 5, 10), result, "Custom converter should allow plain date strings to be parsed correctly");
    }
}
