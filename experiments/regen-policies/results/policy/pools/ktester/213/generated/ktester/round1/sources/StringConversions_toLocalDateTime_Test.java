package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import java.sql.Timestamp;

class StringConversions_toLocalDateTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a Converter with default options for each test
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        converter = null;
    }

    @Test
    void toLocalDateTime_NullInput_ReturnsNull() {
        // GIVEN
        Object from = null;
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(from, converter);
        // THEN
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void toLocalDateTime_ZonedDateTimeInput_ReturnsLocalDateTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2022, 5, 10, 12, 30, 45, 0, ZoneId.of("UTC"));
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(zdt, converter);
        // THEN
        assertNotNull(result, "Result should not be null for a valid ZonedDateTime");
        assertEquals(zdt.toLocalDateTime(), result, "The returned LocalDateTime must match the ZonedDateTime's local component");
    }

    @Test
    void toLocalDateTime_UnconvertibleObject_ReturnsNull() {
        // GIVEN
        // Converter cannot turn a plain Object into a ZonedDateTime
        Object from = new Object();
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(from, converter);
        // THEN
        assertNull(result, "Conversion should return null when the source cannot be turned into a ZonedDateTime");
    }

    @Test
    void toLocalDateTime_StringIsoZonedDateTime_ReturnsLocalDateTime() {
        // GIVEN
        // ISO‑8601 string that includes zone information
        String isoString = "2023-01-02T10:15:30+01:00[Europe/Paris]";
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(isoString, converter);
        // THEN
        // The expected LocalDateTime is the same instant expressed without the zone offset
        ZonedDateTime expectedZdt = ZonedDateTime.parse(isoString);
        assertNotNull(result, "Result should not be null for a parsable ISO‑8601 string");
        assertEquals(expectedZdt.toLocalDateTime(), result, "The LocalDateTime derived from the string should match the ZonedDateTime's local part");
    }

    @Test
    void toLocalDateTime_StringWithoutZone_ReturnsLocalDateTime() {
        // GIVEN
        String noZone = "2023-08-15T13:45:30";
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(noZone, converter);
        // THEN
        LocalDateTime expected = LocalDateTime.of(2023, 8, 15, 13, 45, 30);
        assertNotNull(result, "Result should not be null for a valid local date‑time string");
        assertEquals(expected, result, "The returned LocalDateTime must equal the parsed components of the input string");
    }

    @Test
    void toLocalDateTime_DateEpochMillis_ReturnsLocalDateTime() {
        // GIVEN
        // 2023‑08‑15 13:45:30 UTC expressed as epoch milliseconds
        Date date = new Date(1692107130000L);
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(date, converter);
        // THEN
        // The expected value is the instant represented in the JVM's default zone
        LocalDateTime expected = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        assertNotNull(result, "Result should not be null for a valid java.util.Date");
        assertEquals(expected, result, "LocalDateTime should represent the same instant as the Date, interpreted in the default zone");
    }

    @Test
    void toLocalDateTime_Timestamp_ReturnsLocalDateTime() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2023-08-15 13:45:30");
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(ts, converter);
        // THEN
        LocalDateTime expected = LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
        assertNotNull(result, "Result should not be null for a valid Timestamp");
        assertEquals(expected, result, "Timestamp should be converted to the same LocalDateTime as its underlying instant");
    }

    @Test
    void toLocalDateTime_InstantEpoch_ReturnsLocalDateTime() {
        // GIVEN
        Instant epoch = Instant.ofEpochSecond(0);
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(epoch, converter);
        // THEN
        LocalDateTime expected = LocalDateTime.ofInstant(epoch, ZoneId.systemDefault());
        assertNotNull(result, "Result should not be null for Instant.EPOCH");
        assertEquals(expected, result, "Instant at epoch should be represented as the LocalDateTime of the default zone at that instant");
    }

    @Test
    void toLocalDateTime_OffsetDateTime_ReturnsLocalDateTime() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.parse("2023-08-15T13:45:30+02:00");
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(odt, converter);
        // THEN
        LocalDateTime expected = odt.toLocalDateTime();
        assertNotNull(result, "Result should not be null for a valid OffsetDateTime");
        assertEquals(expected, result, "OffsetDateTime should lose its offset when converted to LocalDateTime");
    }

    @Test
    void toLocalDateTime_UnsupportedInteger_ReturnsNull() {
        // GIVEN
        Integer unsupported = 42;
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(unsupported, converter);
        // THEN
        assertNull(result, "Conversion of an unsupported type (Integer) should return null");
    }

    @Test
    void toLocalDateTime_InvalidString_ReturnsNull() {
        // GIVEN
        String bad = "not-a-date";
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(bad, converter);
        // THEN
        assertNull(result, "A malformed date‑time string should result in null");
    }

    @Test
    void toLocalDateTime_NullConverter_IgnoresAndParses() {
        // GIVEN
        String isoString = "2023-08-15T13:45:30+02:00[Europe/Paris]";
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(isoString, null);
        // THEN
        ZonedDateTime expectedZdt = ZonedDateTime.parse(isoString);
        assertNotNull(result, "Result should not be null when converter is null but parsing succeeds");
        assertEquals(expectedZdt.toLocalDateTime(), result, "Parsing should succeed regardless of a null Converter argument");
    }

    @Test
    void toLocalDateTime_EmptyString_ReturnsNull() {
        // GIVEN
        String empty = "";
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(empty, converter);
        // THEN
        assertNull(result, "An empty string cannot be parsed and should yield null");
    }

    @Test
    void toLocalDateTime_WhitespaceString_ReturnsNull() {
        // GIVEN
        String whitespace = "   ";
        // WHEN
        LocalDateTime result = StringConversions.toLocalDateTime(whitespace, converter);
        // THEN
        assertNull(result, "A whitespace‑only string should be considered invalid and return null");
    }
}
