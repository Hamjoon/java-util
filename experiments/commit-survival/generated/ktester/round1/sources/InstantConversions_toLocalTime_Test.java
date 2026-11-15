package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLocalTime_Test {

    /**
     * Original default time‑zone – restored after all tests have run.
     */
    private static TimeZone originalDefaultZone;

    /**
     * The {@link Converter} used by the tests.
     */
    private Converter converter;

    /* -------------------------------------------------------------
     *  Helper holder class for parameterised arguments
     * ------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Object from;

        // not used – kept for future extensibility
        final Converter converter;

        final LocalTime expected;

        final String displayName;

        ArgumentsHolder(Object from, Converter converter, LocalTime expected, String displayName) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original default zone and force UTC for deterministic behaviour.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a public no‑arg constructor in the library.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for the current tests.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone so we do not affect other test suites.
        TimeZone.setDefault(originalDefaultZone);
    }

    /**
     * Helper that creates the expected {@link LocalTime} from an {@link Instant} using UTC.
     */
    private static LocalTime expectedLocalTimeFromInstant(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalTime();
    }

    private static Stream<ArgumentsHolder> normalConversionProvider() {
        return Stream.of(// LocalTime – should be returned unchanged.
        new ArgumentsHolder(LocalTime.of(13, 45, 30), null, LocalTime.of(13, 45, 30), "LocalTime instance should be unchanged"), // LocalDateTime – time part unchanged.
        new ArgumentsHolder(LocalDateTime.of(2023, 5, 20, 9, 15, 0), null, LocalTime.of(9, 15, 0), "LocalDateTime time component should be extracted"), // ZonedDateTime – local time of its zone.
        new ArgumentsHolder(ZonedDateTime.of(2023, 5, 20, 22, 30, 0, 0, ZoneId.of("America/New_York")), null, LocalTime.of(22, 30, 0), "ZonedDateTime local time should be returned"), // OffsetDateTime – local time part.
        new ArgumentsHolder(OffsetDateTime.of(2023, 5, 20, 5, 0, 0, 0, ZoneOffset.ofHours(-5)), null, LocalTime.of(5, 0, 0), "OffsetDateTime local time should be returned"), // java.util.Date – UTC instant.
        new ArgumentsHolder(Date.from(Instant.parse("2023-05-20T12:34:56Z")), null, LocalTime.of(12, 34, 56), "Date should be interpreted as UTC instant"), // java.sql.Timestamp – time component.
        new ArgumentsHolder(Timestamp.valueOf("2023-05-20 23:59:59"), null, LocalTime.of(23, 59, 59), "Timestamp time component should be extracted"), // Instant – UTC.
        new ArgumentsHolder(Instant.parse("2023-05-20T06:30:00Z"), null, LocalTime.of(6, 30, 0), "Instant should be interpreted as UTC"), // Calendar (UTC) – hour/minute/second fields.
        new ArgumentsHolder(createUtcCalendar(2023, 5, 20, 14, 22, 33), null, LocalTime.of(14, 22, 33), "Calendar fields should be used"));
    }

    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        return cal;
    }

    @Test
    void toLocalTime_fromInstant() {
        Instant instant = Instant.ofEpochMilli(123_456_789L);
        LocalTime expected = expectedLocalTimeFromInstant(instant);
        LocalTime actual = InstantConversions.toLocalTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromDate() {
        Date date = new Date(123_456_789L);
        LocalTime expected = expectedLocalTimeFromInstant(date.toInstant());
        LocalTime actual = InstantConversions.toLocalTime(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromTimestamp() {
        Timestamp ts = new Timestamp(123_456_789L);
        LocalTime expected = expectedLocalTimeFromInstant(ts.toInstant());
        LocalTime actual = InstantConversions.toLocalTime(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(123_456_789L), ZoneOffset.UTC);
        LocalTime expected = ldt.toLocalTime();
        LocalTime actual = InstantConversions.toLocalTime(ldt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(123_456_789L), ZoneOffset.UTC);
        LocalTime expected = zdt.toLocalTime();
        LocalTime actual = InstantConversions.toLocalTime(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(123_456_789L), ZoneOffset.UTC);
        LocalTime expected = odt.toLocalTime();
        LocalTime actual = InstantConversions.toLocalTime(odt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(123_456_789L);
        LocalTime expected = expectedLocalTimeFromInstant(cal.toInstant());
        LocalTime actual = InstantConversions.toLocalTime(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromLongEpochMillis() {
        Long epochMillis = 123_456_789L;
        LocalTime expected = expectedLocalTimeFromInstant(Instant.ofEpochMilli(epochMillis));
        LocalTime actual = InstantConversions.toLocalTime(epochMillis, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromStringRepresentingInstant() {
        // The Converter can parse ISO‑8601 strings to an Instant.
        // corresponds to 123_456 ms
        String iso = "1970-01-01T00:02:03.456Z";
        Instant instant = Instant.parse(iso);
        LocalTime expected = expectedLocalTimeFromInstant(instant);
        LocalTime actual = InstantConversions.toLocalTime(iso, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("normalConversionProvider")
    void toLocalTime_normalConversions(ArgumentsHolder holder) {
        Object from = holder.from;
        // converter is always the real one; the JSON specifies null for converter,
        // meaning the default instance created in @BeforeEach should be used.
        LocalTime actual = InstantConversions.toLocalTime(from, converter);
        assertEquals(holder.expected, actual);
    }

    @Test
    void toLocalTime_nullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> InstantConversions.toLocalTime(null, converter), "Passing null as the source should cause NullPointerException");
    }

    @Test
    void toLocalTime_unsupportedTypeThrowsException() {
        String invalid = "invalid type";
        // the exact type may be ConversionException or ClassCastException
        assertThrows(// the exact type may be ConversionException or ClassCastException
        RuntimeException.class, () -> InstantConversions.toLocalTime(invalid, converter), "Unsupported source type should trigger a conversion failure");
    }

    @Test
    void toLocalTime_fromLocalDateThrowsException() {
        LocalDate ld = LocalDate.of(2023, 5, 20);
        // could be DateTimeException or ConversionException
        assertThrows(// could be DateTimeException or ConversionException
        RuntimeException.class, () -> InstantConversions.toLocalTime(ld, converter), "LocalDate lacks a time component and should cause a conversion exception");
    }

    @Test
    void toLocalTime_fromOffsetTimeThrowsException() {
        OffsetTime ot = OffsetTime.of(13, 45, 30, 0, ZoneOffset.ofHours(2));
        assertThrows(RuntimeException.class, () -> InstantConversions.toLocalTime(ot, converter), "OffsetTime lacks a date part and should cause a conversion exception");
    }

    @Test
    void toLocalTime_fromSqlDateThrowsException() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-05-20");
        assertThrows(RuntimeException.class, () -> InstantConversions.toLocalTime(sqlDate, converter), "java.sql.Date represents only a date and should cause a conversion exception");
    }

    @Test
    void toLocalTime_converterThrowsConversionException() {
        // Custom Converter that always throws ConversionException
        Converter badConverter = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                throw new ConversionException("forced failure");
            }
        };
        Instant instant = Instant.now();
        assertThrows(ConversionException.class, () -> InstantConversions.toLocalTime(instant, badConverter), "A converter that throws should propagate the ConversionException");
    }

    @Test
    void toLocalTime_invalidTimeZoneInCalendarThrowsIllegalArgumentException() {
        // GMT+25 is not a valid offset; Calendar constructor will succeed but
        // later conversion to ZonedDateTime should fail.
        TimeZone badTz = TimeZone.getTimeZone("GMT+25");
        Calendar cal = Calendar.getInstance(badTz);
        cal.setTimeInMillis(0L);
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalTime(cal, converter), "Invalid time‑zone offset should result in IllegalArgumentException");
    }
}
