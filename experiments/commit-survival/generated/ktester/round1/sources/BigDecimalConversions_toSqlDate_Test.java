package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toSqlDate_Test {

    @Mock
    private Converter // a dummy converter – not used by the paths under test
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Force a known default zone for deterministic results (UTC).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Verify that an {@link Instant} is correctly converted to a {@link java.sql.Date}
     * (which extends {@link java.util.Date}) by preserving the epoch‑millisecond value.
     */
    @Test
    void toSqlDate_givenInstant_returnsDateWithSameEpochMilli() {
        // GIVEN
        Instant instant = Instant.parse("2023-01-01T12:34:56.789Z");
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(instant, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(instant.toEpochMilli(), result.getTime(), "The Date should represent the same instant in milliseconds");
    }

    /**
     * Verify that a {@link java.util.Date} instance is returned unchanged (apart from the
     * type conversion to {@code java.sql.Date}) when supplied as the source value.
     */
    @Test
    void toSqlDate_givenUtilDate_returnsDateWithSameEpochMilli() {
        // GIVEN
        // arbitrary timestamp
        long epochMilli = 1_617_123_456_789L;
        Date sourceDate = new Date(epochMilli);
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(sourceDate, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(epochMilli, result.getTime(), "The Date should preserve the original epoch‑millisecond value");
    }

    @Test
    void toSqlDate_givenTimestamp_returnsDateWithSameInstant() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2021-06-01 12:34:56.789");
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(ts, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(ts.toInstant().toEpochMilli(), result.getTime(), "Timestamp should be converted via its instant");
    }

    @Test
    void toSqlDate_givenLocalDateTime_returnsDateWithSystemZoneInstant() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 12, 34, 56, 123_000_000);
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(ldt, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "LocalDateTime should be interpreted in the system default zone");
    }

    @Test
    void toSqlDate_givenLocalDate_returnsDateAtStartOfDay() {
        // GIVEN
        LocalDate ld = LocalDate.of(2021, 6, 1);
        ZonedDateTime zdt = ld.atStartOfDay(ZoneId.systemDefault());
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(ld, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "LocalDate should be converted to midnight of the system default zone");
    }

    @Test
    void toSqlDate_givenLocalTime_combinesWithCurrentDate() {
        // GIVEN
        LocalTime lt = LocalTime.of(12, 34, 56, 123_000_000);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ZonedDateTime zdt = today.atTime(lt).atZone(ZoneId.systemDefault());
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(lt, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "LocalTime should be combined with today's date in the system default zone");
    }

    @Test
    void toSqlDate_givenZonedDateTime_returnsDateWithSameInstant() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2021, 6, 1, 12, 34, 56, 123_000_000, ZoneId.of("UTC"));
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(zdt, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "ZonedDateTime should be converted via its instant");
    }

    @Test
    void toSqlDate_givenOffsetDateTime_returnsDateWithSameInstant() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2021, 6, 1, 12, 34, 56, 123_000_000, ZoneOffset.ofHours(-5));
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(odt, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(odt.toInstant().toEpochMilli(), result.getTime(), "OffsetDateTime should be converted via its instant");
    }

    @Test
    void toSqlDate_givenCalendar_returnsDateWithSameEpochMilli() {
        // GIVEN
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_622_548_800_123L);
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(cal, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(1_622_548_800_123L, result.getTime(), "Calendar's timeInMillis should be used for the Date");
    }

    @Test
    void toSqlDate_givenLongEpochMilli_returnsDateWithSameValue() {
        // GIVEN
        long epochMilli = 1_622_548_800_123L;
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(epochMilli, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMilli, result.getTime(), "Long representing epoch milliseconds should be used directly");
    }

    @Test
    void toSqlDate_givenIsoString_returnsParsedInstantDate() {
        // GIVEN
        String iso = "2021-06-01T12:34:56.123Z";
        Instant instant = Instant.parse(iso);
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(iso, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.getTime(), "ISO‑8601 string should be parsed to an Instant");
    }

    @Test
    void toSqlDate_givenNullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toSqlDate(source, mockConverter), "Null source should result in NullPointerException");
    }

    @Test
    void toSqlDate_givenNullConverter_throwsNullPointerException() {
        // GIVEN
        Date source = new Date(1622548800000L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toSqlDate(source, null), "Null converter should result in NullPointerException");
    }

    @Test
    void toSqlDate_givenUnsupportedBoolean_throwsIllegalArgumentException() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toSqlDate(source, mockConverter), "Unsupported type Boolean should raise IllegalArgumentException");
    }

    @Test
    void toSqlDate_givenUnsupportedObject_throwsIllegalArgumentException() {
        // GIVEN
        Object source = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toSqlDate(source, mockConverter), "Arbitrary Object should raise IllegalArgumentException");
    }

    @Test
    void toSqlDate_givenMalformedIsoString_throwsDateTimeParseException() {
        // GIVEN
        String badIso = "2021-13-01T99:99:99Z";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> BigDecimalConversions.toSqlDate(badIso, mockConverter), "Malformed ISO‑8601 string should cause DateTimeParseException");
    }

    @Test
    void toSqlDate_givenEmptyString_throwsDateTimeParseException() {
        // GIVEN
        String empty = "";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> BigDecimalConversions.toSqlDate(empty, mockConverter), "Empty string should cause DateTimeParseException");
    }

    @Test
    void toSqlDate_givenUnsupportedNumericType_throwsIllegalArgumentException() {
        // GIVEN
        Integer source = 12345;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toSqlDate(source, mockConverter), "Non‑Long numeric types should raise IllegalArgumentException");
    }
}
