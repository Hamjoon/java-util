package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

class DoubleConversions_toLocalDateTime_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    static final class MyTemporalObject {
    }

    static final class TestConverter extends Converter {

        TestConverter() {
            super(new ConverterOptions());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object source, Class<T> targetClass) {
            if (source instanceof MyTemporalObject && targetClass == ZonedDateTime.class) {
                ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.of(2022, 12, 31, 23, 59, 59), ZoneOffset.UTC);
                return (T) zdt;
            }
            return super.convert(source, targetClass);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special overrides are required for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test suites.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        ???;
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing a null source object should result in NullPointerException."), Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Unsupported type Boolean must cause IllegalArgumentException."), Arguments.of(Duration.ofHours(5), IllegalArgumentException.class, "Duration cannot be converted to LocalDateTime; should throw IllegalArgumentException."), Arguments.of(LocalTime.of(15, 30, 45), IllegalArgumentException.class, "LocalTime lacks a date component; conversion should raise IllegalArgumentException."), Arguments.of("2022-01-01T12:34:56+02:00", DateTimeParseException.class, "String with offset cannot be parsed by LocalDateTime.parse; expect DateTimeParseException."), Arguments.of("", DateTimeParseException.class, "Empty string is not a valid ISO‑8601 date‑time; expect DateTimeParseException."));
    }

    @Test
    void shouldConvertFromZonedDateTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 123_456_789, ZoneId.of("America/New_York"));
        // WHEN
        LocalDateTime result = DoubleConversions.toLocalDateTime(zdt, converter);
        // THEN
        assertEquals(zdt.toLocalDateTime(), result, "Conversion from ZonedDateTime should drop the zone and keep the local date‑time");
    }

    @Test
    void shouldConvertFromLocalDateTime() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2022, 12, 31, 23, 59, 59, 999_000_000);
        // WHEN
        LocalDateTime result = DoubleConversions.toLocalDateTime(ldt, converter);
        // THEN
        assertEquals(ldt, result, "Conversion from LocalDateTime should return the same instance values");
    }

    @Test
    void shouldConvertFromInstant() {
        // GIVEN
        Instant instant = Instant.parse("2021-08-15T10:15:30.123456789Z");
        // WHEN
        LocalDateTime result = DoubleConversions.toLocalDateTime(instant, converter);
        // THEN
        // The expected LocalDateTime is the instant interpreted in the system default zone.
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        assertEquals(expected, result, "Conversion from Instant should produce a LocalDateTime in the system default zone");
    }

    @Test
    void shouldConvertFromDate() {
        // GIVEN
        Date date = Date.from(Instant.parse("2020-01-01T12:00:00Z"));
        // WHEN
        LocalDateTime result = DoubleConversions.toLocalDateTime(date, converter);
        // THEN
        LocalDateTime expected = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        assertEquals(expected, result, "Conversion from java.util.Date should respect the system default zone");
    }

    @Test
    void shouldConvertFromCalendar() {
        // GIVEN
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2019, Calendar.JUNE, 30, 8, 45, 15);
        cal.set(Calendar.MILLISECOND, 250);
        // WHEN
        LocalDateTime result = DoubleConversions.toLocalDateTime(cal, converter);
        // THEN
        Instant instant = cal.toInstant();
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        assertEquals(expected, result, "Conversion from Calendar should produce a LocalDateTime in the system default zone");
    }

    @Test
    void shouldConvertFromTimestamp() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2018-03-25 17:20:30.123456789");
        // WHEN
        LocalDateTime result = DoubleConversions.toLocalDateTime(ts, converter);
        // THEN
        // Timestamp is a subclass of java.util.Date, so the same logic applies.
        LocalDateTime expected = LocalDateTime.ofInstant(ts.toInstant(), ZoneId.systemDefault());
        assertEquals(expected, result, "Conversion from Timestamp should behave like conversion from Date");
    }

    @Test
    void shouldThrowWhenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DoubleConversions.toLocalDateTime(nullSource, converter), "Passing a null source should result in a NullPointerException");
    }

    @Test
    void shouldThrowWhenConverterIsNull() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DoubleConversions.toLocalDateTime(ldt, null), "Passing a null Converter should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void shouldConvertValid(Object from, LocalDateTime expected, String description) {
        LocalDateTime result = DoubleConversions.toLocalDateTime(from, null);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidConversionProvider")
    void shouldThrowForInvalid(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> DoubleConversions.toLocalDateTime(from, null), description);
    }

    @Test
    void shouldDelegateToCustomConverter() {
        MyTemporalObject custom = new MyTemporalObject();
        Converter customConverter = new TestConverter();
        LocalDateTime result = DoubleConversions.toLocalDateTime(custom, customConverter);
        LocalDateTime expected = LocalDateTime.of(2022, 12, 31, 23, 59, 59);
        assertEquals(expected, result, "When a non‑null Converter is supplied, the method should delegate to it; " + "the custom object is converted to the expected LocalDateTime.");
    }

    @Test
    void shouldThrowWhenConverterIsNullAndSourceIsNonNull() {
        LocalDateTime ldt = LocalDateTime.now();
        assertThrows(NullPointerException.class, () -> DoubleConversions.toLocalDateTime(ldt, null), "Passing a null Converter should result in NullPointerException.");
    }
}
