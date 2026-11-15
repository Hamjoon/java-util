package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

class DateConversions_toLocalDateTime_Test {

    private static Converter converter;

    /**
     * Minimal stub for a {@code Converter} that uses a supplied {@link DateTimeFormatter}
     * to parse {@link String} values into temporal objects.  The real library provides
     * a similar implementation; this stub is only required for the custom‑format test.
     */
    class DateTimeFormatterConverter extends Converter {

        private final DateTimeFormatter formatter;

        DateTimeFormatterConverter(DateTimeFormatter formatter) {
            super(new ConverterOptions());
            this.formatter = formatter;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object from, Class<T> toClass) {
            if (from instanceof String && (toClass == LocalDateTime.class || toClass == ZonedDateTime.class)) {
                // Parse as a LocalDateTime first, then let the underlying conversion handle the rest.
                LocalDateTime ldt = LocalDateTime.parse((String) from, formatter);
                return (T) ldt;
            }
            return super.convert(from, toClass);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using defaults is sufficient for these tests
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to release
        converter = null;
    }

    /* -------------------------------------------------------------
     *  Group 1 – Direct temporal types (LocalDateTime, ZonedDateTime,
     *            OffsetDateTime)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> directTemporalProvider() {
        return Stream.of(// already a LocalDateTime – should be returned unchanged (or equal)
        Arguments.of(LocalDateTime.of(2023, 5, 17, 14, 30, 45), // behaves as a no‑op for this type
        new Converter(new ConverterOptions()), LocalDateTime.of(2023, 5, 17, 14, 30, 45)), // ZonedDateTime – zone information is discarded
        Arguments.of(ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneId.of("Europe/Paris")), new Converter(new ConverterOptions()), LocalDateTime.of(2023, 5, 17, 14, 30, 45)), // OffsetDateTime – offset is ignored
        Arguments.of(OffsetDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneOffset.ofHours(2)), new Converter(new ConverterOptions()), LocalDateTime.of(2023, 5, 17, 14, 30, 45)));
    }

    /* -------------------------------------------------------------
     *  Group 2 – Legacy date/time classes (Date, Calendar, Instant)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> legacyProvider() {
        // Helper calendar instance – note that Calendar months are zero‑based
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2023, Calendar.MAY, 17, 14, 30, 45);
        calendar.set(Calendar.MILLISECOND, 0);
        return Stream.of(// java.util.Date (or java.sql.Timestamp) → system default zone
        Arguments.of(Date.from(Instant.parse("2023-05-17T14:30:45Z")), new Converter(new ConverterOptions()), LocalDateTime.ofInstant(Instant.parse("2023-05-17T14:30:45Z"), ZoneId.systemDefault())), // java.util.Calendar → based on its instant value
        Arguments.of(calendar, new Converter(new ConverterOptions()), LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault())), // Instant → interpreted in the system default zone
        Arguments.of(Instant.parse("2023-05-17T14:30:45Z"), new Converter(new ConverterOptions()), LocalDateTime.ofInstant(Instant.parse("2023-05-17T14:30:45Z"), ZoneId.systemDefault())));
    }

    /* -------------------------------------------------------------
     *  Group 3 – String representations (ISO‑8601 and custom format)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> stringProvider() {
        // ISO‑8601 string – the default Converter can parse it directly.
        String iso = "2023-05-17T14:30:45";
        // Custom formatted string – we need a Converter that knows the pattern.
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String custom = "2023/05/17 14:30:45";
        Converter customConverter = new DateTimeFormatterConverter(customFormatter);
        return Stream.of(// ISO‑8601
        Arguments.of(iso, new Converter(new ConverterOptions()), LocalDateTime.parse(iso)), // Custom format
        Arguments.of(custom, customConverter, LocalDateTime.of(2023, 5, 17, 14, 30, 45)));
    }

    /* -------------------------------------------------------------
     *  Group 4 – Error handling (nulls, unsupported types, malformed strings)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> errorProvider() {
        return Stream.of(// null source object → NullPointerException (as per library contract)
        Arguments.of(null, new Converter(new ConverterOptions()), NullPointerException.class), // unsupported source type (Integer) → IllegalArgumentException
        Arguments.of(12345, new Converter(new ConverterOptions()), IllegalArgumentException.class), // malformed date‑time string → DateTimeParseException
        Arguments.of("not-a-date", new Converter(new ConverterOptions()), java.time.format.DateTimeParseException.class), // null converter argument → NullPointerException before conversion logic
        Arguments.of(LocalDate.of(2023, 5, 17), null, NullPointerException.class));
    }

    @Test
    void toLocalDateTime_givenLocalDateTime_returnsSameInstance() {
        // truncate nanos for deterministic comparison
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime result = DateConversions.toLocalDateTime(now, converter);
        assertNotNull(result);
        assertEquals(now, result);
    }

    @Test
    void toLocalDateTime_givenZonedDateTime_returnsLocalDateTimePart() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 14, 30, 45, 0, ZoneId.of("Europe/Paris"));
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime result = DateConversions.toLocalDateTime(zdt, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenDate_returnsEquivalentLocalDateTime() {
        // java.util.Date represents an instant in UTC
        Date date = Date.from(Instant.parse("2022-12-01T08:15:30.000Z"));
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(date.toInstant(), zone);
        LocalDateTime result = DateConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenString_returnsParsedLocalDateTime() {
        // The built‑in Converter supports ISO‑8601 strings for date‑time conversion
        String iso = "2021-07-20T16:45:12";
        LocalDateTime expected = LocalDateTime.parse(iso);
        LocalDateTime result = DateConversions.toLocalDateTime(iso, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenNull_returnsNull() {
        // Converter returns null for object target types when source is null
        LocalDateTime result = DateConversions.toLocalDateTime(null, converter);
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] direct temporal conversion")
    @MethodSource("directTemporalProvider")
    void toLocalDateTime_directTemporalTypes(Object from, Converter conv, LocalDateTime expected) {
        LocalDateTime result = DateConversions.toLocalDateTime(from, conv);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] legacy conversion")
    @MethodSource("legacyProvider")
    void toLocalDateTime_legacyDateTimeClasses(Object from, Converter conv, LocalDateTime expected) {
        LocalDateTime result = DateConversions.toLocalDateTime(from, conv);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] string conversion")
    @MethodSource("stringProvider")
    void toLocalDateTime_stringRepresentations(Object from, Converter conv, LocalDateTime expected) {
        LocalDateTime result = DateConversions.toLocalDateTime(from, conv);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] error handling")
    @MethodSource("errorProvider")
    void toLocalDateTime_errorHandling(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        // The method under test is static; we simply assert that the expected exception is thrown.
        assertThrows(expectedException, () -> DateConversions.toLocalDateTime(from, conv));
    }
}
