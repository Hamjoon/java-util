package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.time.format.DateTimeParseException;
import java.util.Date;
import static org.mockito.ArgumentMatchers.any;

class DoubleConversions_toOffsetDateTime_Test {

    /**
     * Converter used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – the zone will be the system default unless the
        // implementation supplies its own default (commonly UTC).  The tests compute
        // the expected value using the same options, so the exact default does not
        // matter.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    @DisplayName("Convert Instant to OffsetDateTime")
    void testInstantConversion() {
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        OffsetDateTime expected = instant.atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
        OffsetDateTime actual = DoubleConversions.toOffsetDateTime(instant, converter);
        assertEquals(expected, actual, "OffsetDateTime produced from an Instant should match the expected value");
    }

    @Test
    @DisplayName("Convert LocalDateTime to OffsetDateTime")
    void testLocalDateTimeConversion() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.FEBRUARY, 15, 9, 30, 45);
        // The conversion path is: LocalDateTime -> Instant (via the converter's zone)
        // -> OffsetDateTime.
        OffsetDateTime expected = ldt.atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
        OffsetDateTime actual = DoubleConversions.toOffsetDateTime(ldt, converter);
        assertEquals(expected, actual, "OffsetDateTime produced from a LocalDateTime should match the expected value");
    }

    @Test
    @DisplayName("Convert ZonedDateTime to OffsetDateTime")
    void testZonedDateTimeConversion() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Paris"));
        // The method first converts the source to an Instant, then applies the
        // converter's zone.  Therefore the expected result uses the converter's zone,
        // not the original zone of the ZonedDateTime.
        OffsetDateTime expected = zdt.toInstant().atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
        OffsetDateTime actual = DoubleConversions.toOffsetDateTime(zdt, converter);
        assertEquals(expected, actual, "OffsetDateTime produced from a ZonedDateTime should be based on the converter's zone");
    }

    @Test
    @DisplayName("Convert java.util.Date to OffsetDateTime")
    void testDateConversion() {
        Date date = Date.from(Instant.parse("2021-07-04T10:20:30Z"));
        OffsetDateTime expected = date.toInstant().atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
        OffsetDateTime actual = DoubleConversions.toOffsetDateTime(date, converter);
        assertEquals(expected, actual, "OffsetDateTime produced from a java.util.Date should match the expected value");
    }

    @Test
    @DisplayName("Convert java.sql.Timestamp to OffsetDateTime")
    void testTimestampConversion() {
        Timestamp ts = Timestamp.valueOf("2020-05-01 08:15:00");
        OffsetDateTime expected = ts.toInstant().atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
        OffsetDateTime actual = DoubleConversions.toOffsetDateTime(ts, converter);
        assertEquals(expected, actual, "OffsetDateTime produced from a java.sql.Timestamp should match the expected value");
    }

    @Test
    @DisplayName("Null source object should throw NullPointerException")
    void test_toOffsetDateTime_nullFrom() {
        // Arrange: a valid converter is already available from @BeforeEach.
        Object from = null;
        // Act & Assert
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(from, converter), "Passing null as the source object should cause a NullPointerException");
    }

    @Test
    @DisplayName("Null Converter should throw NullPointerException")
    void test_toOffsetDateTime_nullConverter() {
        // Arrange: a valid source object (Instant works for any non‑null case)
        Instant from = Instant.now();
        Converter nullConverter = null;
        // Act & Assert
        assertThrows(NullPointerException.class, () -> DoubleConversions.toOffsetDateTime(from, nullConverter), "Passing null as the Converter should cause a NullPointerException");
    }

    @Test
    @DisplayName("Unsupported source type should result in ConversionException")
    void test_toOffsetDateTime_invalidFromType() {
        // Arrange: a source object that DoubleConversions cannot turn into an Instant.
        Object unsupported = new Object();
        // Mock the static method DoubleConversions.toInstant to throw ConversionException.
        try (MockedStatic<DoubleConversions> mocked = Mockito.mockStatic(DoubleConversions.class)) {
            mocked.when(() -> DoubleConversions.toInstant(any(), any())).thenThrow(new ConversionException("Cannot convert object of type Object to Instant"));
            // Act & Assert
            ConversionException ex = assertThrows(ConversionException.class, () -> DoubleConversions.toOffsetDateTime(unsupported, converter), "Providing an unsupported object should raise ConversionException");
            assertTrue(ex.getMessage().contains("Cannot convert"));
        }
    }

    @Test
    @DisplayName("Invalid ZoneId in Converter options should throw DateTimeException")
    void test_toOffsetDateTime_invalidZoneId() {
        // Arrange: a valid source Instant.
        Instant from = Instant.now();
        // Create a mock Converter whose getOptions().getZoneId() throws DateTimeException.
        ConverterOptions badOptions = Mockito.mock(ConverterOptions.class);
        Mockito.when(badOptions.getZoneId()).thenThrow(DateTimeException.class);
        Converter badConverter = Mockito.mock(Converter.class);
        Mockito.when(badConverter.getOptions()).thenReturn(badOptions);
        // Act & Assert
        assertThrows(DateTimeException.class, () -> DoubleConversions.toOffsetDateTime(from, badConverter), "An invalid ZoneId in the Converter should cause DateTimeException when atZone is invoked");
    }
}
