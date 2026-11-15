package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toOffsetDateTime_Test {

    private static ConverterOptions options;

    private Converter converter;

    /**
     * Custom converter that returns a pre‑constructed {@link ZonedDateTime}
     * for a specific source type.
     */
    private static final class StubConverter extends Converter {

        private final Object stubResult;

        private final RuntimeException stubException;

        StubConverter(Object stubResult) {
            super(new ConverterOptions());
            this.stubResult = stubResult;
            this.stubException = null;
        }

        StubConverter(RuntimeException stubException) {
            super(new ConverterOptions());
            this.stubResult = null;
            this.stubException = stubException;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object source, Class<T> target) {
            if (stubException != null) {
                throw stubException;
            }
            return (T) stubResult;
        }
    }

    /**
     * Simple custom date class used only for custom‑converter tests.
     */
    private static final class MyCustomDate {

        final int year, month, day, hour, minute, second;

        MyCustomDate(int year, int month, int day, int hour, int minute, int second) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }
    }

    /**
     * Placeholder for the library's {@code ConversionException}. If the real
     * class is unavailable on the compilation classpath, replace it with a more
     * generic {@link RuntimeException} in the assertions above.
     */
    @SuppressWarnings("serial")
    private static class ConversionException extends RuntimeException {

        public ConversionException(String message) {
            super(message);
        }

        public ConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // enforce UTC for deterministic behaviour of LocalDateTime/LocalDate/LocalTime conversions
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        options = new ConverterOptions();
    }

    @BeforeEach
    void setupBeforeEach() {
        // a fresh Converter instance for each test to avoid cross‑test state
        converter = new Converter(options);
    }

    /**
     * Provides a stream of arguments for {@link #toOffsetDateTime_validInputs(Object, String)}.
     *
     * <p>All dates/times represent the same instant (2023‑01‑01T12:30:45Z) unless
     * otherwise noted.
     */
    private static Stream<Arguments> validInputProvider() {
        // common instant used throughout many test cases
        Instant instant = Instant.parse("2023-01-01T12:30:45Z");
        long epochSeconds = instant.getEpochSecond();
        return Stream.of(// OffsetDateTime – should be returned unchanged
        arguments(OffsetDateTime.of(2023, 1, 1, 12, 30, 45, 0, ZoneOffset.UTC), "2023-01-01T12:30:45Z"), // ZonedDateTime – conversion preserves instant & offset
        arguments(ZonedDateTime.ofInstant(instant, ZoneId.of("UTC")), "2023-01-01T12:30:45Z"), // LocalDateTime – interpreted in system default zone (UTC)
        arguments(LocalDateTime.of(2023, 1, 1, 12, 30, 45, 0), "2023-01-01T12:30:45Z"), // java.util.Date – epoch‑millis → OffsetDateTime
        arguments(Date.from(instant), "2023-01-01T12:30:45Z"), // java.util.Calendar – same as Date
        arguments(createUtcCalendar(instant.toEpochMilli()), "2023-01-01T12:30:45Z"), // Instant – direct mapping
        arguments(instant, "2023-01-01T12:30:45Z"), // Long epoch seconds
        arguments(epochSeconds, "2023-01-01T12:30:45Z"), // String ISO‑8601 with explicit offset
        arguments("2023-01-01T12:30:45+02:00", "2023-01-01T10:30:45Z"), // String ISO‑8601 without offset (treated as UTC)
        arguments("2023-01-01T12:30:45", "2023-01-01T12:30:45Z"), // String ISO‑8601 with zone ID (same as ZonedDateTime)
        arguments("2023-08-15T12:34:56+02:00[Europe/Paris]", "2023-08-15T10:34:56Z"), // BigInteger epoch seconds
        arguments(BigInteger.valueOf(epochSeconds), "2023-01-01T12:30:45Z"), // BigDecimal epoch seconds
        arguments(new BigDecimal(Long.toString(epochSeconds)), "2023-01-01T12:30:45Z"), // LocalDate – start‑of‑day UTC
        arguments(LocalDate.of(2023, 1, 1), "2023-01-01T00:00:00Z"), // LocalTime – epoch date (1970‑01‑01) at given time UTC
        arguments(LocalTime.of(12, 30, 45), "1970-01-01T12:30:45Z"), // java.sql.Timestamp – instant conversion
        arguments(Timestamp.from(instant), "2023-01-01T12:30:45Z"), // java.sql.Date – midnight UTC of the date
        arguments(java.sql.Date.valueOf("2023-01-01"), "2023-01-01T00:00:00Z"));
    }

    /**
     * Helper that creates a {@link Calendar} set to UTC with the supplied epoch‑millis.
     */
    private static Calendar createUtcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /**
     * Supplies inputs that are not supported or are out of range.
     */
    private static Stream<Arguments> exceptionInputProvider() {
        return Stream.of(// Invalid ISO‑8601 string
        arguments("not-a-date", ConversionException.class), // Numeric string without date format
        arguments("1234567890", ConversionException.class), // Long.MAX_VALUE – out of OffsetDateTime range
        arguments(Long.MAX_VALUE, ConversionException.class), // BigInteger overflow (Long.MAX_VALUE + 1)
        arguments(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), ConversionException.class), // BigDecimal overflow
        arguments(new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE), ConversionException.class), // Negative epoch seconds (pre‑1970) – this is actually valid, but the JSON expects a valid result.
        // We therefore test a value far beyond the lower bound to trigger an exception.
        // this is a valid case; we will not assert exception
        arguments(-315619200L, OffsetDateTime.class), // Unsupported primitive types
        arguments(true, ConversionException.class), arguments(123.45d, ConversionException.class), arguments(12.34f, ConversionException.class), arguments((short) 5, ConversionException.class), arguments((byte) 7, ConversionException.class), arguments(42, ConversionException.class), // Unsupported complex types
        arguments(Duration.ofHours(5), ConversionException.class), arguments(ZoneId.of("UTC"), ConversionException.class), arguments(ZoneOffset.of("+02:00"), ConversionException.class), arguments(OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC), ConversionException.class), arguments(new Object[0], ConversionException.class), // Custom converter returns wrong type (String) – leads to ClassCastException
        arguments(new MyCustomDate(2023, 1, 1, 12, 30, 45), ClassCastException.class));
    }

    @Test
    void toOffsetDateTime_givenZonedDateTime_returnsMatchingOffsetDateTime() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.parse("2023-08-15T12:34:56+02:00[Europe/Paris]");
        OffsetDateTime expected = source.toOffsetDateTime();
        // WHEN
        OffsetDateTime actual = BigIntegerConversions.toOffsetDateTime(source, converter);
        // THEN
        assertEquals(expected, actual, "OffsetDateTime should match the source ZonedDateTime's offset");
    }

    @Test
    void toOffsetDateTime_givenIsoString_returnsMatchingOffsetDateTime() {
        // GIVEN
        String isoString = "2023-08-15T12:34:56+02:00[Europe/Paris]";
        ZonedDateTime parsed = ZonedDateTime.parse(isoString);
        OffsetDateTime expected = parsed.toOffsetDateTime();
        // WHEN
        OffsetDateTime actual = BigIntegerConversions.toOffsetDateTime(isoString, converter);
        // THEN
        assertEquals(expected, actual, "Converter should parse ISO string to the correct OffsetDateTime");
    }

    @Test
    void toOffsetDateTime_givenNull_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toOffsetDateTime(nullSource, converter), "Passing null should result in NullPointerException because toZonedDateTime returns null");
    }

    /**
     * Parameterised test that verifies successful conversion of a wide range of
     * supported source objects to the expected {@link OffsetDateTime}.
     *
     * The expected value is expressed as an ISO‑8601 string which is parsed
     * into an {@code OffsetDateTime} for comparison.
     */
    @ParameterizedTest(name = "{index}: from={0} → expected={1}")
    @MethodSource("validInputProvider")
    void toOffsetDateTime_validInputs(Object from, String expectedIso) {
        // GIVEN – the expected OffsetDateTime parsed from the ISO string
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        // WHEN
        OffsetDateTime actual = BigIntegerConversions.toOffsetDateTime(from, converter);
        // THEN
        assertEquals(expected, actual, () -> "Conversion of " + from.getClass().getSimpleName() + " should match expected OffsetDateTime");
    }

    @Test
    void toOffsetDateTime_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNull(result, "A null source should result in a null OffsetDateTime");
    }

    @Test
    void toOffsetDateTime_customConverterProvidesResult() {
        // GIVEN – a custom source object and a stub converter that returns a known ZonedDateTime
        MyCustomDate src = new MyCustomDate(2023, 1, 1, 12, 30, 45);
        ZonedDateTime zoned = ZonedDateTime.of(2023, 1, 1, 12, 30, 45, 0, ZoneId.of("UTC"));
        Converter custom = new StubConverter(zoned);
        // WHEN
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(src, custom);
        // THEN
        assertEquals(zoned.toOffsetDateTime(), result, "Custom converter should supply the ZonedDateTime that is then transformed to OffsetDateTime");
    }

    @Test
    void toOffsetDateTime_customConverterReturnsNull() {
        // GIVEN – a custom source and a converter that returns null
        MyCustomDate src = new MyCustomDate(2023, 1, 1, 12, 30, 45);
        Converter custom = new StubConverter((Object) null);
        // WHEN
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(src, custom);
        // THEN
        assertNull(result, "If the custom converter returns null the conversion result should be null");
    }

    @Test
    void toOffsetDateTime_customConverterThrowsException() {
        // GIVEN – a custom source and a converter that throws IllegalArgumentException
        MyCustomDate src = new MyCustomDate(2023, 1, 1, 12, 30, 45);
        Converter custom = new StubConverter(new IllegalArgumentException("bad value"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toOffsetDateTime(src, custom), "Exception from custom converter should propagate unchanged");
        assertEquals("bad value", ex.getMessage());
    }

    /**
     * Parameterised test for inputs that should result in a {@link ConversionException}
     * (or a generic {@link RuntimeException} if the specific type is unavailable).
     */
    @ParameterizedTest(name = "{index}: from={0} → throws {1}")
    @MethodSource("exceptionInputProvider")
    void toOffsetDateTime_invalidInputs(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> BigIntegerConversions.toOffsetDateTime(from, converter), "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should throw " + expectedException.getSimpleName());
    }

    /**
     * Special test for the “wrong type” custom‑converter scenario where the stub
     * returns a {@link String} instead of a {@link ZonedDateTime}.
     */
    @Test
    void toOffsetDateTime_customConverterReturnsWrongType() {
        // GIVEN – a stub converter that returns a plain String
        MyCustomDate src = new MyCustomDate(2023, 1, 1, 12, 30, 45);
        Converter custom = new StubConverter("not-a-date");
        // WHEN / THEN – a ClassCastException (or ConversionException) is expected
        assertThrows(ClassCastException.class, () -> BigIntegerConversions.toOffsetDateTime(src, custom), "Returning a non‑ZonedDateTime from the custom converter should cause a ClassCastException");
    }
}
