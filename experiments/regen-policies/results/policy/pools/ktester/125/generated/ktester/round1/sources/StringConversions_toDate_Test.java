package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class StringConversions_toDate_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Dummy class that has no date semantics. Used to verify that the
     * conversion routine rejects unknown types when no {@link Converter}
     * is supplied.
     */
    private static final class CustomDate {
        // empty on purpose
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to release, but nullify the reference for completeness.
        converter = null;
    }

    private static Stream<Arguments> isoStringProvider() {
        return Stream.of(Arguments.of("2023-03-15T10:20:30Z", Date.from(Instant.parse("2023-03-15T10:20:30Z"))));
    }

    @Test
    void toDate_givenNull_returnsNull() {
        Date result = StringConversions.toDate(null, converter);
        assertNull(result, "Converting a null source should yield null");
    }

    @Test
    void toDate_givenDate_returnsSameInstant() {
        // arbitrary epoch millis
        Date source = new Date(1_600_000_000_000L);
        Date result = StringConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "When the source is already a Date, the same instant should be returned");
    }

    @Test
    void toDate_givenZonedDateTime_convertsCorrectly() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 4, 5, 12, 30, 45, 0, ZoneOffset.UTC);
        Date expected = Date.from(zdt.toInstant());
        Date result = StringConversions.toDate(zdt, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "ZonedDateTime should be converted to a Date with the same instant");
    }

    @Test
    void toDate_givenIsoString_convertsCorrectly() {
        // ISO‑8601 string that the Converter can parse into a ZonedDateTime
        String iso = "2023-04-05T12:30:45Z";
        Instant instant = Instant.parse(iso);
        Date expected = Date.from(instant);
        Date result = StringConversions.toDate(iso, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "ISO‑8601 string should be parsed and converted to the correct Date");
    }

    @Test
    void toDate_givenTimestamp_convertsCorrectly() {
        Timestamp ts = Timestamp.valueOf("2023-04-05 12:30:45.123456789");
        Date expected = new Date(ts.getTime());
        Date result = StringConversions.toDate(ts, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "Timestamp should be converted to a Date preserving the epoch millis");
    }

    @Test
    void toDate_givenUnsupportedType_returnsNull() {
        // An Integer is not directly convertible to a ZonedDateTime, so the method should return null.
        Integer unsupported = 42;
        Date result = StringConversions.toDate(unsupported, converter);
        assertNull(result, "Unsupported source type should result in null");
    }

    /* ---------- Null ZonedDateTime handling (mocked static) ---------- */
    @Test
    void toDate_whenInternalToZonedDateTimeReturnsNull_thenResultIsNull() {
        // Mock the static method toZonedDateTime to return null regardless of arguments.
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(any(), any())).thenReturn(null);
            Date result = StringConversions.toDate("anyString", converter);
            assertNull(result, "If toZonedDateTime returns null, toDate must return null");
        }
    }

    /* -------------------- ISO‑8601 String conversion -------------------- */
    @ParameterizedTest(name = "[{index}] isoString={0}")
    @MethodSource("isoStringProvider")
    void toDate_isoStringConversion(String isoString, Date expected) {
        Date result = StringConversions.toDate(isoString, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "ISO‑8601 string should be parsed to the expected instant");
    }

    /* -------------------- LocalDate conversion (midnight UTC) -------------------- */
    @Test
    void toDate_localDateMidnightUtc() {
        LocalDate ld = LocalDate.of(2022, 5, 10);
        ZonedDateTime zdt = ld.atStartOfDay(ZoneOffset.UTC);
        Date expected = Date.from(zdt.toInstant());
        Date result = StringConversions.toDate(ld, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "LocalDate should be interpreted as midnight UTC");
    }

    /* -------------------- Long epoch‑millis conversion -------------------- */
    @Test
    void toDate_longEpochMillis() {
        // 2021‑05‑31T00:00:00Z
        long epochMillis = 1_622_505_600_000L;
        Date expected = new Date(epochMillis);
        Date result = StringConversions.toDate(epochMillis, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "Long epoch‑millis must be converted to a Date with the same instant");
    }

    /* -------------------- Calendar instance conversion -------------------- */
    @Test
    void toDate_calendarConversion() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        Calendar cal = Calendar.getInstance(tz);
        // arbitrary instant
        cal.setTimeInMillis(1_600_000_000_000L);
        Date expected = new Date(cal.getTimeInMillis());
        Date result = StringConversions.toDate(cal, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "Calendar should be converted to a Date reflecting its absolute instant");
    }

    /* -------------------- ZonedDateTime with non‑UTC offset -------------------- */
    @Test
    void toDate_zonedDateTimeWithNonUtcOffset() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"));
        Date expected = Date.from(zdt.toInstant());
        Date result = StringConversions.toDate(zdt, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "ZonedDateTime with a specific zone offset should retain the same instant");
    }

    /* -------------------- Instant instance conversion -------------------- */
    @Test
    void toDate_instantConversion() {
        Instant instant = Instant.parse("2021-08-01T12:34:56.789Z");
        Date expected = Date.from(instant);
        Date result = StringConversions.toDate(instant, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "Instant should be transformed directly into a Date with identical epoch millis");
    }

    /* -------------------- Null source object (explicit) -------------------- */
    @Test
    void toDate_nullSourceObject() {
        Date result = StringConversions.toDate(null, null);
        assertNull(result, "Null source should lead to null result");
    }

    /* -------------------- Converter throws runtime exception -------------------- */
    @Test
    void toDate_converterThrowsRuntimeException_propagates() {
        // Mock the static method toZonedDateTime to throw a RuntimeException.
        try (MockedStatic<StringConversions> mocked = Mockito.mockStatic(StringConversions.class)) {
            mocked.when(() -> StringConversions.toZonedDateTime(any(), any())).thenThrow(new RuntimeException("Conversion failure"));
            RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toDate("2022-01-01", converter));
            assertEquals("Conversion failure", ex.getMessage(), "The original RuntimeException must propagate unchanged");
        }
    }

    /* -------------------- Invalid string format leads to parse exception -------------------- */
    @Test
    void toDate_invalidStringFormat_throwsDateTimeParseException() {
        String bad = "not-a-date";
        assertThrows(DateTimeParseException.class, () -> StringConversions.toDate(bad, converter), "An unparsable string should cause DateTimeParseException");
    }

    /* -------------------- Unsupported source type (Boolean) -------------------- */
    @Test
    void toDate_unsupportedBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toDate(Boolean.TRUE, converter), "Boolean has no date semantics and should trigger IllegalArgumentException");
    }

    /* -------------------- Empty string input -------------------- */
    @Test
    void toDate_emptyString_throwsDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> StringConversions.toDate("", converter), "Empty string cannot be parsed as a date");
    }

    /* -------------------- Invalid month value in ISO‑8601 string -------------------- */
    @Test
    void toDate_invalidMonthInIsoString_throwsDateTimeParseException() {
        String badMonth = "2021-13-01T00:00:00Z";
        assertThrows(DateTimeParseException.class, () -> StringConversions.toDate(badMonth, converter), "Month value 13 is out of range and must cause DateTimeParseException");
    }

    /* -------------------- Integer epoch‑millis conversion (non‑Long) -------------------- */
    @Test
    void toDate_integerEpochMillis_throwsIllegalArgumentException() {
        // seconds, not millis
        int epochSeconds = 1_622_505_600;
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toDate(epochSeconds, converter), "Integer (instead of Long) should trigger IllegalArgumentException");
    }

    /* -------------------- Null converter with non‑convertible custom object -------------------- */
    @Test
    void toDate_customObjectWithoutConverter_throwsIllegalArgumentException() {
        Object custom = new CustomDate();
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toDate(custom, null), "A custom, non‑convertible object without a Converter must cause IllegalArgumentException");
    }
}
