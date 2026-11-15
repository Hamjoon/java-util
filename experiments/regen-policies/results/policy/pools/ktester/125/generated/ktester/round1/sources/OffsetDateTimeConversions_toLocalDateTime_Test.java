package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLocalDateTime_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor in the library.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        ???;
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of(null, defaultConverter, NullPointerException.class, "Null source should cause NullPointerException"), Arguments.of(LocalDateTime.of(2023, 8, 15, 12, 34, 56), null, NullPointerException.class, "Null converter should cause NullPointerException"), Arguments.of(Boolean.TRUE, defaultConverter, RuntimeException.class, "Unsupported Boolean should raise a conversion exception"), Arguments.of(Float.valueOf(1.23f), defaultConverter, RuntimeException.class, "Unsupported Float should raise a conversion exception"), Arguments.of(OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2)), defaultConverter, RuntimeException.class, "OffsetTime cannot be converted to ZonedDateTime"), Arguments.of("2023-08-15T12:34:56+25:00", defaultConverter, RuntimeException.class, "String with invalid offset should raise a conversion exception"), Arguments.of("", defaultConverter, RuntimeException.class, "Empty string cannot be parsed and should raise a conversion exception"), Arguments.of("not-a-date", defaultConverter, RuntimeException.class, "Non‑parseable string should raise a conversion exception"), Arguments.of(Collections.singletonMap("key", "value"), defaultConverter, RuntimeException.class, "Generic Map cannot be interpreted as a date‑time"), Arguments.of(ZoneId.of("Europe/Paris"), defaultConverter, RuntimeException.class, "ZoneId alone cannot be converted to ZonedDateTime"), Arguments.of(new BigInteger("9223372036854775808"), defaultConverter, RuntimeException.class, "BigInteger overflow should raise a conversion exception"), Arguments.of(Double.NaN, defaultConverter, RuntimeException.class, "Double.NaN is not a valid epoch‑millis representation"), Arguments.of("any-value", new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        }, NullPointerException.class, "Custom converter returning null leads to NPE"), Arguments.of("trigger", new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalStateException("converter failure");
            }
        }, IllegalStateException.class, "Exception from custom converter should propagate"));
    }

    @Test
    void shouldConvertFromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 1, 12, 34, 56, 0, ZoneOffset.ofHours(2));
        LocalDateTime expected = odt.toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(odt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertFromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 1, 12, 34, 56, 0, ZoneId.of("Europe/Paris"));
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertFromString() {
        // ISO‑8601 format recognised by the built‑in converter
        String iso = "2023-05-01T12:34:56";
        LocalDateTime expected = LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(iso, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertFromDate() {
        // java.util.Date represents an instant in UTC.
        Date date = Date.from(Instant.parse("2023-05-01T12:34:56Z"));
        // The conversion uses the system default zone.
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = date.toInstant().atZone(systemZone).toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertFromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.from(Instant.parse("2023-05-01T12:34:56Z")));
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = cal.toInstant().atZone(systemZone).toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertFromInstant() {
        Instant instant = Instant.parse("2023-05-01T12:34:56Z");
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = instant.atZone(systemZone).toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertNull(OffsetDateTimeConversions.toLocalDateTime(null, converter));
    }

    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("successfulConversionProvider")
    void shouldConvertSuccessfully(Object from, Converter converter, String expectedIso, String description) {
        LocalDateTime expected = LocalDateTime.parse(expectedIso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("exceptionProvider")
    void shouldThrowExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toLocalDateTime(from, converter), description);
    }
}
