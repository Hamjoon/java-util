package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLocalDateTime_Test {

    private Converter converter;

    /**
     * A converter that simply delegates to the default implementation – used to verify that a non‑null converter is ignored.
     */
    static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    /**
     * A converter that always throws a ConversionException – used to test exception propagation.
     */
    static class ThrowingConverter extends Converter {

        ThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw new ConversionException("Forced conversion failure");
        }
    }

    /**
     * A converter that returns null for any conversion – used to provoke a NullPointerException later in the chain.
     */
    static class NullReturningConverter extends Converter {

        NullReturningConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter is fully functional without custom overrides
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        converter = null;
    }

    static Stream<Arguments> validConversionProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        return Stream.of(// ZonedDateTime – same zone
        arguments("Valid Conversions - ZonedDateTime", ZonedDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneId.of("Europe/Paris")), null, LocalDateTime.of(2023, 5, 10, 14, 30, 0)), // OffsetDateTime – positive offset
        arguments("Valid Conversions - OffsetDateTime", OffsetDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneOffset.ofHours(2)), null, LocalDateTime.ofInstant(OffsetDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneOffset.ofHours(2)).toInstant(), systemZone)), // LocalDateTime – identity
        arguments("Valid Conversions - LocalDateTime", LocalDateTime.of(2023, 5, 10, 14, 30, 0), null, LocalDateTime.of(2023, 5, 10, 14, 30, 0)), // java.util.Date
        arguments("Valid Conversions - java.util.Date", Date.from(Instant.parse("2023-05-10T12:30:00Z")), null, LocalDateTime.ofInstant(Instant.parse("2023-05-10T12:30:00Z"), systemZone)), // java.sql.Timestamp
        arguments("Valid Conversions - java.sql.Timestamp", Timestamp.from(Instant.parse("2023-05-10T12:30:00Z")), null, LocalDateTime.ofInstant(Instant.parse("2023-05-10T12:30:00Z"), systemZone)), // Calendar (UTC)
        arguments("Valid Conversions - Calendar", GregorianCalendar.from(ZonedDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneId.of("UTC"))), null, LocalDateTime.of(2023, 5, 10, 14, 30, 0)), // Instant
        arguments("Valid Conversions - Instant", Instant.parse("2023-05-10T12:30:00Z"), null, LocalDateTime.ofInstant(Instant.parse("2023-05-10T12:30:00Z"), systemZone)), // LocalDate
        arguments("Valid Conversions - LocalDate", LocalDate.of(2023, 5, 10), null, LocalDateTime.of(2023, 5, 10, 0, 0, 0)), // Year
        arguments("Valid Conversions - Year", Year.of(2023), null, LocalDateTime.of(2023, 1, 1, 0, 0, 0)), // Instant – epoch boundary
        arguments("Valid Conversions - Instant (Epoch Boundary)", Instant.EPOCH, null, LocalDateTime.ofInstant(Instant.EPOCH, systemZone)), // ZonedDateTime in UTC – different system zone handling
        arguments("Valid Conversions - ZonedDateTime Different Zone", ZonedDateTime.of(2023, 5, 10, 22, 30, 0, 0, ZoneId.of("UTC")), null, LocalDateTime.ofInstant(ZonedDateTime.of(2023, 5, 10, 22, 30, 0, 0, ZoneId.of("UTC")).toInstant(), systemZone)), // OffsetDateTime – negative offset
        arguments("Valid Conversions - OffsetDateTime Negative Offset", OffsetDateTime.of(2023, 5, 10, 4, 15, 0, 0, ZoneOffset.ofHours(-5)), null, LocalDateTime.ofInstant(OffsetDateTime.of(2023, 5, 10, 4, 15, 0, 0, ZoneOffset.ofHours(-5)).toInstant(), systemZone)), // Non‑null converter ignored (use DummyConverter)
        arguments("Valid Conversions - Non‑null Converter Ignored", Instant.parse("2023-05-10T12:30:00Z"), new DummyConverter(), LocalDateTime.ofInstant(Instant.parse("2023-05-10T12:30:00Z"), systemZone)));
    }

    @Test
    void toLocalDateTime_givenNull_returnsNull() {
        assertNull(NumberConversions.toLocalDateTime(null, converter));
    }

    @Test
    void toLocalDateTime_givenLocalDateTime_returnsSameInstanceValue() {
        LocalDateTime source = LocalDateTime.of(2022, Month.MARCH, 15, 10, 30, 45);
        LocalDateTime result = NumberConversions.toLocalDateTime(source, converter);
        assertEquals(source, result);
    }

    @Test
    void toLocalDateTime_givenDate_convertsCorrectly() {
        // 2021‑06‑01T00:00:00Z
        long epochMilli = 1622505600000L;
        Date date = new Date(epochMilli);
        LocalDateTime expected = Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenTimestamp_convertsCorrectly() {
        // 2020‑12‑31T23:59:59.123Z
        long epochMilli = 1609459199123L;
        java.sql.Timestamp ts = new java.sql.Timestamp(epochMilli);
        // ensure nanos are present
        ts.setNanos(123_000_000);
        LocalDateTime expected = Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenLocalDate_convertsToStartOfDay() {
        LocalDate date = LocalDate.of(2023, Month.JANUARY, 5);
        LocalDateTime expected = date.atStartOfDay();
        LocalDateTime result = NumberConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenZonedDateTime_preservesLocalDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2024, 2, 29, 14, 45, 30, 0, ZoneId.of("America/New_York"));
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(zdt, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenOffsetDateTime_preservesLocalDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2025, 7, 20, 9, 15, 0, 0, ZoneOffset.ofHours(-5));
        // Conversion goes through ZonedDateTime, which keeps the same instant.
        LocalDateTime expected = odt.toZonedDateTime().toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(odt, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenInstant_convertsUsingSystemDefaultZone() {
        Instant instant = Instant.parse("2022-08-15T12:34:56Z");
        LocalDateTime expected = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_givenCalendar_convertsCorrectly() {
        Calendar cal = Calendar.getInstance();
        // 2022‑06‑01T00:00:00Z
        cal.setTimeInMillis(1654041600000L);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        LocalDateTime expected = Instant.ofEpochMilli(1654041600000L).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime result = NumberConversions.toLocalDateTime(cal, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void validConversions(String description, Object from, Converter suppliedConverter, LocalDateTime expected) {
        LocalDateTime result = NumberConversions.toLocalDateTime(from, suppliedConverter);
        assertEquals(expected, result, description);
    }

    @Test
    void invalidConversion_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toLocalDateTime(null, null));
    }

    @Test
    void invalidConversion_unsupportedString_throwsConversionException() {
        assertThrows(ConversionException.class, () -> NumberConversions.toLocalDateTime("2023-05-10T12:30:00", null));
    }

    @Test
    void invalidConversion_converterThrowsException_throwsConversionException() {
        Converter throwing = new ThrowingConverter();
        assertThrows(ConversionException.class, () -> NumberConversions.toLocalDateTime("2023-05-10T12:30:00", throwing));
    }

    @Test
    void invalidConversion_converterReturnsNull_throwsNullPointerException() {
        Converter nullReturning = new NullReturningConverter();
        // The source type is supported (ZonedDateTime) but the custom converter returns null,
        // causing a NPE when .toLocalDateTime() is invoked on the null result.
        assertThrows(NullPointerException.class, () -> NumberConversions.toLocalDateTime(ZonedDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneId.of("UTC")), nullReturning));
    }

    @Test
    void invalidConversion_unsupportedTemporalType_YearMonth_throwsConversionException() {
        assertThrows(ConversionException.class, () -> NumberConversions.toLocalDateTime(YearMonth.of(2023, 5), null));
    }

    @Test
    void invalidConversion_arbitraryObject_throwsConversionException() {
        assertThrows(ConversionException.class, () -> NumberConversions.toLocalDateTime(new Object(), null));
    }
}
