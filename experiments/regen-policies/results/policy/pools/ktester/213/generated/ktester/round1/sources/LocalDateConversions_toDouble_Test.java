package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toDouble_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance; using the default options is sufficient
        converter = new Converter(new ConverterOptions());
    }

    @Test
    void toDoubleFromInstant() {
        // given
        // 1973-11-29T21:33:09Z
        Instant instant = Instant.ofEpochSecond(123_456_789L);
        // when
        double result = LocalDateConversions.toDouble(instant, converter);
        // then
        assertEquals(123_456_789.0, result);
    }

    @Test
    void toDoubleFromDate() {
        // given
        // 2021‑10‑01T00:00:00Z = 1633072800000 ms since epoch
        Date date = new Date(1_633_072_800_000L);
        // when
        double result = LocalDateConversions.toDouble(date, converter);
        // then
        assertEquals(1_633_072_800.0, result);
    }

    @Test
    void toDoubleFromMapWithTimeKey() {
        // given
        // Map representation that the Converter understands for Instant/Date types
        // same instant as above
        Map<String, Object> map = Map.of("time", 1_633_072_800_000L);
        // when
        double result = LocalDateConversions.toDouble(map, converter);
        // then
        assertEquals(1_633_072_800.0, result);
    }

    @Test
    void toDoubleFromNullReturnsZero() {
        // given
        Object from = null;
        // when
        double result = LocalDateConversions.toDouble(from, converter);
        // then
        assertEquals(0.0, result);
    }

    @Test
    void toDoubleFromInstant_Input() {
        // Instant input – epoch seconds should be returned unchanged.
        Instant instant = Instant.ofEpochSecond(1_609_459_200L);
        double result = LocalDateConversions.toDouble(instant, null);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromDate_Input() {
        // java.util.Date input – converted via Date.toInstant().
        Date date = new Date(1_609_459_200_000L);
        double result = LocalDateConversions.toDouble(date, null);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromCalendar_Input() {
        // java.util.Calendar input – converted via Calendar.toInstant().
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1_609_459_200_000L);
        double result = LocalDateConversions.toDouble(calendar, null);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromLongNumber_Input() {
        // Long representing epoch milliseconds.
        Long millis = Long.valueOf(1_609_459_200_000L);
        double result = LocalDateConversions.toDouble(millis, null);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromDoubleNumber_Input() {
        // Double representing epoch seconds (including fractional part).
        Double seconds = Double.valueOf(1_609_459_200.123);
        double result = LocalDateConversions.toDouble(seconds, null);
        assertEquals(1_609_459_200.123, result);
    }

    @Test
    void toDoubleFromStringIso_Input() {
        // ISO‑8601 string parsed by a SimpleStringConverter.
        String iso = "2021-01-01T00:00:00Z";
        // assumes UTC parsing
        Converter isoConverter = new SimpleStringConverter();
        double result = LocalDateConversions.toDouble(iso, isoConverter);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromLocalDateTime_WithZoneOffset() {
        // LocalDateTime combined with a supplied ZoneOffset (UTC).
        LocalDateTime ldt = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        Converter ldtConverter = new LocalDateTimeToInstantConverter(ZoneOffset.UTC);
        double result = LocalDateConversions.toDouble(ldt, ldtConverter);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleUnsupportedType_throwsClassCastException() {
        // An object type that cannot be converted to an Instant.
        Object unsupported = new Object();
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toDouble(unsupported, null));
    }

    @Test
    void toDoubleNullSource_throwsNullPointerException() {
        // Null source should cause a NullPointerException because toInstant cannot handle null.
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toDouble(null, null));
    }

    @Test
    void toDoubleFromTimestamp_Input() {
        // java.sql.Timestamp (subclass of java.util.Date).
        Timestamp ts = new Timestamp(1_609_459_200_000L);
        double result = LocalDateConversions.toDouble(ts, null);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromLocalDate_WithConverter() {
        // LocalDate interpreted as midnight UTC by a custom converter.
        LocalDate ld = LocalDate.of(2021, 1, 1);
        Converter ldConverter = new LocalDateToInstantConverter(ZoneOffset.UTC);
        double result = LocalDateConversions.toDouble(ld, ldConverter);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromLocalTime_WithConverter() {
        // LocalTime combined with epoch date (1970‑01‑01) by a custom converter.
        LocalTime lt = LocalTime.of(0, 0, 0);
        Converter ltConverter = new LocalTimeToInstantConverter(ZoneOffset.UTC);
        double result = LocalDateConversions.toDouble(lt, ltConverter);
        assertEquals(0.0, result);
    }

    @Test
    void toDoubleFromYearMonth_WithConverter() {
        // YearMonth converted to first day of month at UTC midnight.
        YearMonth ym = YearMonth.of(2021, 1);
        Converter ymConverter = new YearMonthToInstantConverter(ZoneOffset.UTC);
        double result = LocalDateConversions.toDouble(ym, ymConverter);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromYear_WithConverter() {
        // Year interpreted as Jan 1st midnight UTC.
        Year year = Year.of(2021);
        Converter yearConverter = new YearToInstantConverter(ZoneOffset.UTC);
        double result = LocalDateConversions.toDouble(year, yearConverter);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromMonthDay_WithConverter() {
        // MonthDay supplied with year 1970 and UTC midnight.
        MonthDay md = MonthDay.of(1, 1);
        Converter mdConverter = new MonthDayToInstantConverter(Year.of(1970), ZoneOffset.UTC);
        double result = LocalDateConversions.toDouble(md, mdConverter);
        assertEquals(0.0, result);
    }

    @Test
    void toDoubleFromZonedDateTime_Input() {
        // ZonedDateTime already represents an instant.
        ZonedDateTime zdt = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        double result = LocalDateConversions.toDouble(zdt, null);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromOffsetDateTime_Input() {
        // OffsetDateTime directly convertible to an Instant.
        OffsetDateTime odt = OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        double result = LocalDateConversions.toDouble(odt, null);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromBigDecimal_Input() {
        // BigDecimal representing epoch seconds (including fraction).
        BigDecimal bd = new BigDecimal("1609459200.123");
        double result = LocalDateConversions.toDouble(bd, null);
        assertEquals(1_609_459_200.123, result);
    }

    @Test
    void toDoubleFromBigInteger_Input() {
        // BigInteger interpreted as epoch milliseconds.
        BigInteger bi = new BigInteger("1609459200000");
        double result = LocalDateConversions.toDouble(bi, null);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromAtomicLong_Input() {
        // AtomicLong interpreted as epoch milliseconds.
        AtomicLong al = new AtomicLong(1_609_459_200_000L);
        double result = LocalDateConversions.toDouble(al, null);
        assertEquals(1_609_459_200.0, result);
    }

    @Test
    void toDoubleFromNegativeInstant_Input() {
        // Negative epoch Instant (pre‑1970).
        Instant instant = Instant.ofEpochSecond(-1L);
        double result = LocalDateConversions.toDouble(instant, null);
        assertEquals(-1.0, result);
    }

    @Test
    void toDoubleFromStringCustomFormat_WithConverter() {
        // Non‑ISO string parsed by a SimpleDateFormatStringConverter.
        String custom = "01/01/2021 00:00:00";
        Converter fmtConverter = new SimpleDateFormatStringConverter("MM/dd/yyyy HH:mm:ss", ZoneOffset.UTC);
        double result = LocalDateConversions.toDouble(custom, fmtConverter);
        assertEquals(1_609_459_200.0, result);
    }
}
