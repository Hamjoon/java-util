package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toTimestamp_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used for the “default” conversion tests.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides
        // a sensible set of options for the built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test suites.
        converter = null;
    }

    /**
     * Creates a {@link Timestamp} from an {@link Instant} (truncates to millis).
     */
    private static Timestamp tsFromInstant(Instant i) {
        return Timestamp.from(i);
    }

    /**
     * Returns a {@link Timestamp} representing the given epoch‑millis.
     */
    private static Timestamp tsFromEpochMilli(long epochMilli) {
        return new Timestamp(epochMilli);
    }

    private static Stream<Arguments> standardConversionsProvider() {
        return Stream.of(// java.util.Date
        Arguments.of(new Date(1672572896000L), null, tsFromEpochMilli(1672572896000L), "Date → Timestamp"), // java.sql.Timestamp (should be returned unchanged)
        Arguments.of(Timestamp.valueOf("2023-01-01 12:34:56.789"), null, Timestamp.valueOf("2023-01-01 12:34:56.789"), "Timestamp → same Timestamp"), // java.time.Instant
        Arguments.of(Instant.parse("2023-01-01T12:34:56Z"), null, tsFromInstant(Instant.parse("2023-01-01T12:34:56Z")), "Instant → Timestamp"), // java.time.LocalDateTime (system default zone)
        Arguments.of(LocalDateTime.of(2023, 1, 1, 12, 34, 56), null, tsFromInstant(ZonedDateTime.of(LocalDateTime.of(2023, 1, 1, 12, 34, 56), ZoneId.systemDefault()).toInstant()), "LocalDateTime → Timestamp (system default zone)"), // java.time.ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneId.of("UTC")), null, tsFromInstant(Instant.parse("2023-01-01T12:34:56Z")), "ZonedDateTime UTC → Timestamp"), // java.time.OffsetDateTime (UTC)
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneOffset.UTC), null, tsFromInstant(Instant.parse("2023-01-01T12:34:56Z")), "OffsetDateTime UTC → Timestamp"), // Long epoch‑millis
        Arguments.of(Long.valueOf(1672572896000L), null, tsFromEpochMilli(1672572896000L), "Long epoch‑millis → Timestamp"), // ISO‑8601 String
        Arguments.of("2023-01-01T12:34:56Z", null, tsFromInstant(Instant.parse("2023-01-01T12:34:56Z")), "ISO‑8601 String → Timestamp"));
    }

    private static Converter customStringConverter() {
        return new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                if (src instanceof String && ((String) src).startsWith("custom-format:")) {
                    String body = ((String) src).substring("custom-format:".length());
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                    LocalDateTime ldt = LocalDateTime.parse(body, fmt);
                    // Return an Instant (the library will accept this)
                    return ldt.atZone(ZoneId.systemDefault()).toInstant();
                }
                return null;
            }
        };
    }

    @Test
    void toTimestamp_fromDate_returnsEquivalentTimestamp() {
        // GIVEN
        // current instant in milliseconds
        Date now = new Date();
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(now, converter);
        // THEN
        assertNotNull(ts, "Timestamp should not be null for a non‑null Date input");
        assertEquals(now.getTime(), ts.getTime(), "Timestamp milliseconds must match the original Date milliseconds");
    }

    @Test
    void toTimestamp_fromInstant_returnsEquivalentTimestamp() {
        // GIVEN
        Instant instant = Instant.now();
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(instant, converter);
        // THEN
        assertNotNull(ts, "Timestamp should not be null for a non‑null Instant input");
        assertEquals(instant.toEpochMilli(), ts.getTime(), "Timestamp milliseconds must match the original Instant milliseconds");
    }

    @Test
    void toTimestamp_fromNull_returnsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(nullObj, converter);
        // THEN
        assertNull(ts, "Timestamp should be null when the source object is null");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("standardConversionsProvider")
    void toTimestamp_standardConversions(Object from, Converter converter, Timestamp expected, String description) {
        Timestamp actual = DoubleConversions.toTimestamp(from, converter);
        assertNotNull(actual, description);
        assertEquals(expected, actual, description);
    }

    @Test
    void toTimestamp_customConverterParsesNonStandardString() {
        // GIVEN
        String source = "custom-format:2023/01/01 12:34:56";
        Converter custom = customStringConverter();
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(source, custom);
        // THEN
        Instant expectedInstant = LocalDateTime.of(2023, 1, 1, 12, 34, 56).atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(Timestamp.from(expectedInstant), ts);
    }

    @Test
    void toTimestamp_nullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> DoubleConversions.toTimestamp(null, null));
    }

    @Test
    void toTimestamp_unsupportedTypeThrowsConversionException() {
        assertThrows(ConversionException.class, () -> DoubleConversions.toTimestamp(Boolean.TRUE, null));
    }

    @Test
    void toTimestamp_malformedStringThrowsDateTimeParseExceptionWrapped() {
        // The library wraps parsing errors in ConversionException; we assert the cause.
        ConversionException ex = assertThrows(ConversionException.class, () -> DoubleConversions.toTimestamp("not-a-date", null));
        assertTrue(ex.getCause() instanceof DateTimeParseException, "Cause should be DateTimeParseException");
    }

    @Test
    void toTimestamp_dateAtEpochStart() {
        Date epoch = new Date(0L);
        Timestamp ts = DoubleConversions.toTimestamp(epoch, null);
        assertEquals(tsFromEpochMilli(0L), ts);
    }

    @Test
    void toTimestamp_dateOneDayBeforeEpoch() {
        Date dayBefore = new Date(-86_400_000L);
        Timestamp ts = DoubleConversions.toTimestamp(dayBefore, null);
        assertEquals(tsFromEpochMilli(-86_400_000L), ts);
    }

    @Test
    void toTimestamp_longMaxValueThrowsConversionException() {
        assertThrows(ConversionException.class, () -> DoubleConversions.toTimestamp(Long.MAX_VALUE, null));
    }

    @Test
    void toTimestamp_instantWithNanosecondFractionTruncates() {
        Instant instant = Instant.ofEpochSecond(0, 999_999_999);
        Timestamp ts = DoubleConversions.toTimestamp(instant, null);
        // 999_999_999 ns = 999 ms (truncated)
        Timestamp expected = tsFromEpochMilli(0L + 999);
        assertEquals(expected, ts);
    }

    @Test
    void toTimestamp_sqlDateBecomesMidnightTimestamp() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-01-01");
        Timestamp ts = DoubleConversions.toTimestamp(sqlDate, null);
        // midnight in the default zone
        Instant expected = sqlDate.toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    void toTimestamp_sqlTimeBecomesEpochDateWithTime() {
        java.sql.Time sqlTime = java.sql.Time.valueOf("12:34:56");
        Timestamp ts = DoubleConversions.toTimestamp(sqlTime, null);
        // The library treats java.sql.Time as the time on 1970‑01‑01
        Instant expected = sqlTime.toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    void toTimestamp_utcCalendar() {
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.set(2023, Calendar.JANUARY, 1, 12, 34, 56);
        utcCal.set(Calendar.MILLISECOND, 0);
        Timestamp ts = DoubleConversions.toTimestamp(utcCal, null);
        Instant expected = utcCal.toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    void toTimestamp_newYorkCalendarConvertedToUTC() {
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCal.set(2023, Calendar.JANUARY, 1, 12, 34, 56);
        nyCal.set(Calendar.MILLISECOND, 0);
        Timestamp ts = DoubleConversions.toTimestamp(nyCal, null);
        // New York (UTC‑5) → UTC should be +5 hours
        Instant expected = nyCal.toInstant();
        assertEquals(Timestamp.from(expected), ts);
        // sanity check of the instant value
        assertEquals(Instant.parse("2023-01-01T17:34:56Z"), expected);
    }

    @Test
    void toTimestamp_localDateAtStartOfDay() {
        LocalDate ld = LocalDate.of(2023, 1, 1);
        Timestamp ts = DoubleConversions.toTimestamp(ld, null);
        Instant expected = ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    void toTimestamp_localTimeOnEpochDate() {
        LocalTime lt = LocalTime.of(12, 34, 56);
        Timestamp ts = DoubleConversions.toTimestamp(lt, null);
        // Combine with epoch date (1970‑01‑01) in the default zone
        Instant expected = LocalDate.ofEpochDay(0).atTime(lt).atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    void toTimestamp_customConverterReturnsNullFallsBackToBuiltIn() {
        // Converter that always returns null – the library should try its own conversion.
        Converter nullReturning = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        };
        String iso = "2023-01-01T12:34:56Z";
        Timestamp ts = DoubleConversions.toTimestamp(iso, nullReturning);
        assertEquals(Timestamp.from(Instant.parse(iso)), ts);
    }

    @Test
    void toTimestamp_customConverterReturnsNullForUnsupportedType() {
        Converter nullReturning = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        };
        assertThrows(ConversionException.class, () -> DoubleConversions.toTimestamp(Boolean.TRUE, nullReturning));
    }

    @Test
    void toTimestamp_customConverterThrowsRuntimeExceptionPropagates() {
        Converter exploding = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("converter failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> DoubleConversions.toTimestamp("any", exploding));
        assertEquals("converter failure", ex.getMessage());
    }

    @Test
    void toTimestamp_dateWithMinLongThrowsConversionException() {
        Date minDate = new Date(Long.MIN_VALUE);
        assertThrows(ConversionException.class, () -> DoubleConversions.toTimestamp(minDate, null));
    }

    @Test
    void toTimestamp_localDateTimeFarFutureThrowsDateTimeException() {
        LocalDateTime ldt = LocalDateTime.of(1_000_000_000, 1, 1, 0, 0, 0);
        assertThrows(DateTimeException.class, () -> DoubleConversions.toTimestamp(ldt, null));
    }

    @Test
    void toTimestamp_localDateFarFutureThrowsDateTimeException() {
        LocalDate ld = LocalDate.of(1_000_000_000, 1, 1);
        assertThrows(DateTimeException.class, () -> DoubleConversions.toTimestamp(ld, null));
    }

    @Test
    void toTimestamp_converterReturnsStringInsteadOfInstant() {
        Converter bad = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return "not-an-instant";
            }
        };
        assertThrows(ConversionException.class, () -> DoubleConversions.toTimestamp("2023-01-01T12:34:56Z", bad));
    }

    @Test
    void toTimestamp_converterReturnsIntegerInsteadOfInstant() {
        Converter bad = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return Integer.valueOf(42);
            }
        };
        assertThrows(ConversionException.class, () -> DoubleConversions.toTimestamp("2023-01-01T12:34:56Z", bad));
    }

    @Test
    void toTimestamp_unsupportedDurationThrowsConversionException() {
        Duration dur = Duration.ofHours(5);
        assertThrows(ConversionException.class, () -> DoubleConversions.toTimestamp(dur, null));
    }

    @Test
    void toTimestamp_unsupportedBigDecimalThrowsConversionException() {
        BigDecimal bd = new BigDecimal("1234567890");
        assertThrows(ConversionException.class, () -> DoubleConversions.toTimestamp(bd, null));
    }
}
