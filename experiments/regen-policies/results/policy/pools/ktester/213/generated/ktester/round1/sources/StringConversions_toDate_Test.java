package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;

class StringConversions_toDate_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter has a public constructor that accepts ConverterOptions
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Release reference for GC
        converter = null;
    }

    @Test
    void testToDate_NullInput_ReturnsNull() {
        Date result = StringConversions.toDate(null, converter);
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void testToDate_WithDateObject_ReturnsSameInstant() {
        Date now = new Date();
        Date result = StringConversions.toDate(now, converter);
        assertNotNull(result, "Result should not be null for a Date input");
        assertEquals(now.getTime(), result.getTime(), "Converting a Date should preserve the original instant");
    }

    @Test
    void testToDate_WithIsoString_ReturnsParsedDate() {
        // ISO‑8601 instant string
        String iso = "2023-04-15T12:30:45Z";
        Date result = StringConversions.toDate(iso, converter);
        assertNotNull(result, "Result should not be null for a valid ISO‑8601 string");
        Instant expectedInstant = Instant.parse(iso);
        assertEquals(expectedInstant.toEpochMilli(), result.getTime(), "The parsed Date should match the instant represented by the string");
    }

    @Test
    void testToDate_WithLocalDate_ReturnsStartOfDayInSystemZone() {
        LocalDate localDate = LocalDate.of(2022, 12, 31);
        Date result = StringConversions.toDate(localDate, converter);
        assertNotNull(result, "Result should not be null for a LocalDate input");
        // The conversion should treat the LocalDate as start of day in the system default zone
        ZonedDateTime zdt = localDate.atStartOfDay(ZoneId.systemDefault());
        long expectedMillis = zdt.toInstant().toEpochMilli();
        assertEquals(expectedMillis, result.getTime(), "The Date should represent the start of the given LocalDate in the system default zone");
    }

    @Test
    void testToDate_WithZonedDateTime_ReturnsEquivalentDate() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 5, 20, 14, 45, 30, 0, ZoneId.of("America/New_York"));
        Date result = StringConversions.toDate(zdt, converter);
        assertNotNull(result, "Result should not be null for a ZonedDateTime input");
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "The Date should represent the same instant as the source ZonedDateTime");
    }

    @Test
    void testToDate_NullInput_NullConverter_ReturnsNull() {
        // Both source and converter are null – method should simply return null
        Date result = StringConversions.toDate(null, null);
        assertNull(result, "Null source with null converter must return null");
    }

    @Test
    void testToDate_UnsupportedObject_ReturnsNull() {
        // An object that cannot be interpreted as a date/time should yield null
        Date result = StringConversions.toDate(new Object(), null);
        assertNull(result, "Unsupported source type should result in null");
    }

    @Test
    void testToDate_FromDate_WithNullConverter_ReturnsSameInstant() {
        // 2022-12-31T23:59:59Z
        Date src = new Date(1672531199000L);
        Date result = StringConversions.toDate(src, null);
        assertNotNull(result);
        assertEquals(src.getTime(), result.getTime(), "Date conversion must preserve the original epoch milliseconds even with null converter");
    }

    @Test
    void testToDate_FromTimestamp_WithNullConverter() {
        // 2021-01-01T00:00:00.123Z
        Timestamp ts = new Timestamp(1609459200123L);
        Date result = StringConversions.toDate(ts, null);
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime(), "Timestamp should be converted to Date preserving milliseconds");
    }

    @Test
    void testToDate_FromInstant_WithNullConverter() {
        Instant instant = Instant.parse("2023-03-15T10:20:30.400Z");
        Date result = StringConversions.toDate(instant, null);
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.getTime(), "Instant must be converted to Date with identical epoch milliseconds");
    }

    @Test
    void testToDate_FromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.parse("2021-12-31T23:00:00-05:00");
        Date result = StringConversions.toDate(odt, null);
        assertNotNull(result);
        // Expected instant is 2022-01-01T04:00:00Z
        Instant expected = odt.toInstant();
        assertEquals(expected.toEpochMilli(), result.getTime(), "OffsetDateTime should be converted using its instant (offset applied)");
    }

    @Test
    void testToDate_FromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2020, 2, 29, 6, 15, 30);
        Date result = StringConversions.toDate(ldt, null);
        assertNotNull(result);
        // Interpreted in the system default zone
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "LocalDateTime should be treated as system default zone before conversion");
    }

    @Test
    void testToDate_FromCalendar_Utc() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2024, Calendar.JANUARY, 2, 9, 45, 0);
        cal.set(Calendar.MILLISECOND, 250);
        Date result = StringConversions.toDate(cal, null);
        assertNotNull(result);
        Instant expected = cal.toInstant();
        assertEquals(expected.toEpochMilli(), result.getTime(), "Calendar set to UTC must be converted to the equivalent Date");
    }

    @Test
    void testToDate_FromLocalDate() {
        LocalDate ld = LocalDate.of(2023, 7, 4);
        Date result = StringConversions.toDate(ld, null);
        assertNotNull(result);
        ZonedDateTime zdt = ld.atStartOfDay(ZoneId.systemDefault());
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "LocalDate should be interpreted as start‑of‑day in system default zone");
    }

    @Test
    void testToDate_UnsupportedString_ReturnsNull() {
        // invalid month
        String bad = "2021-13-01";
        Date result = StringConversions.toDate(bad, null);
        assertNull(result, "Unparseable string should result in null");
    }

    @Test
    void testToDate_WithNonNullConverter_NoEffect() {
        // A mock converter that simply returns null for any conversion – it should not be used
        Converter mockConverter = mock(Converter.class);
        // Ensure that convert is never called for an Instant source
        when(mockConverter.convert(any(), any())).thenReturn(null);
        Instant instant = Instant.parse("1970-01-01T00:00:00Z");
        Date result = StringConversions.toDate(instant, mockConverter);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Instant conversion must ignore the supplied converter");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void testToDate_Propagation_DateTimeParseException() {
        String malformed = "2021-02-30T25:61:00Z";
        // The conversion should throw DateTimeParseException which propagates out of toDate
        assertThrows(DateTimeParseException.class, () -> StringConversions.toDate(malformed, null), "Malformed ISO‑8601 string must cause DateTimeParseException");
    }

    @Test
    void testToDate_Propagation_IllegalArgumentException() {
        Integer unsupported = 12345;
        // toZonedDateTime is expected to throw IllegalArgumentException for unsupported types
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toDate(unsupported, null), "Unsupported source type should cause IllegalArgumentException");
    }

    @Test
    void testToDate_Propagation_RuntimeExceptionFromCustomConverter() {
        // Custom converter that always throws RuntimeException
        Converter explodingConverter = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("converter failure");
            }
        };
        String iso = "2022-01-01T00:00:00Z";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toDate(iso, explodingConverter), "RuntimeException from custom converter must propagate");
        assertEquals("converter failure", ex.getMessage());
    }

    @Test
    void testToDate_Propagation_NullPointerExceptionFromConverterWhenFromNull() {
        // Converter that dereferences the source when it is null
        Converter badConverter = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                if (src == null) {
                    throw new NullPointerException("source is null");
                }
                return null;
            }
        };
        NullPointerException npe = assertThrows(NullPointerException.class, () -> StringConversions.toDate(null, badConverter), "Converter that throws NPE for null source must let the exception bubble up");
        assertEquals("source is null", npe.getMessage());
    }
}
