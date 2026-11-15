package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toDate_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    // returns 946684800000L
    @Mock
    Converter customConverter;

    // returns a String instead of a Number
    @Mock
    Converter customConverterReturningString;

    // throws RuntimeException
    @Mock
    Converter exceptionThrowingConverter;

    // returns null
    @Mock
    Converter nullReturningConverter;

    /**
     * Simple {@link Number} implementation used to verify numeric handling.
     */
    private static final class CustomNumber extends Number {

        private final long value;

        CustomNumber(long v) {
            this.value = v;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public byte byteValue() {
            return (byte) value;
        }

        @Override
        public short shortValue() {
            return (short) value;
        }
    }

    /**
     * Marker class – not recognised by the conversion logic.
     */
    private static final class UnsupportedType {
    }

    /**
     * Marker class used together with a custom {@link Converter}.
     */
    private static final class CustomObject {
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special overrides are required for the tests.
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // configure the mock converters once per test
        when(customConverter.convert(any(), eq(Long.class))).thenReturn(946684800000L);
        when(customConverterReturningString.convert(any(), eq(Long.class))).thenReturn("not-a-number");
        when(exceptionThrowingConverter.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("forced exception"));
        when(nullReturningConverter.convert(any(), eq(Long.class))).thenReturn(null);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test suites.
        converter = null;
    }

    /**
     * Provides all *valid* conversion inputs that should yield a {@link Date}.
     */
    private static Stream<Arguments> validConversionProvider() {
        ZoneId system = ZoneId.systemDefault();
        return Stream.of(// Long
        Arguments.of(123456789L, converter, Date.from(Instant.ofEpochMilli(123456789L))), // Integer
        Arguments.of(Integer.valueOf(987654321), converter, Date.from(Instant.ofEpochMilli(987654321L))), // java.util.Date (already a Date)
        Arguments.of(new Date(1609459200000L), converter, new Date(1609459200000L)), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2022-03-15 12:30:45.123"), converter, Date.from(Instant.parse("2022-03-15T12:30:45.123Z"))), // java.time.Instant
        Arguments.of(Instant.parse("2020-06-01T08:15:30.000Z"), converter, Date.from(Instant.parse("2020-06-01T08:15:30.000Z"))), // LocalDateTime – interpreted in the JVM default zone
        Arguments.of(LocalDateTime.of(2021, 12, 31, 23, 59, 59), converter, Date.from(ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, system).toInstant())), // ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2022, 5, 10, 14, 20, 0, 0, ZoneId.of("UTC")), converter, Date.from(Instant.parse("2022-05-10T14:20:00Z"))), // ISO‑8601 instant string
        Arguments.of("2023-07-04T09:30:00Z", converter, Date.from(Instant.parse("2023-07-04T09:30:00Z"))), // Calendar built from epoch millis
        Arguments.of(new Calendar.Builder().setInstant(1650000000000L).build(), converter, Date.from(Instant.ofEpochMilli(1650000000000L))), // CustomNumber (subclass of Number)
        Arguments.of(new CustomNumber(172800000L), converter, Date.from(Instant.ofEpochMilli(172800000L))), // CustomObject with a mock converter that returns a known epoch millis
        Arguments.of(new CustomObject(), customConverter, Date.from(Instant.ofEpochMilli(946684800000L))));
    }

    private static Stream<Arguments> edgeConversionProvider() {
        ZoneId system = ZoneId.systemDefault();
        return Stream.of(// epoch zero
        Arguments.of(0L, converter, Date.from(Instant.ofEpochMilli(0L))), // negative epoch (one day before 1970)
        Arguments.of(-86400000L, converter, Date.from(Instant.ofEpochMilli(-86400000L))), // java.sql.Date (subclass of java.util.Date)
        Arguments.of(new java.sql.Date(1609459200000L), converter, new Date(1609459200000L)), // OffsetDateTime with +02:00 offset
        Arguments.of(OffsetDateTime.parse("2022-12-31T23:00:00+02:00"), converter, Date.from(Instant.parse("2022-12-31T21:00:00Z"))), // Year – start of year in default zone
        Arguments.of(Year.of(2025), converter, Date.from(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, system).toInstant())), // YearMonth – first day of month in default zone
        Arguments.of(YearMonth.of(2023, 3), converter, Date.from(ZonedDateTime.of(2023, 3, 1, 0, 0, 0, 0, system).toInstant())), // AtomicLong
        Arguments.of(new AtomicLong(123456789L), converter, Date.from(Instant.ofEpochMilli(123456789L))), // BigDecimal
        Arguments.of(new BigDecimal("987654321"), converter, Date.from(Instant.ofEpochMilli(987654321L))), // BigInteger
        Arguments.of(new BigInteger("1234567890123"), converter, Date.from(Instant.ofEpochMilli(1234567890123L))), // LocalDate (leap day) – midnight in default zone
        Arguments.of(LocalDate.of(2020, 2, 29), converter, Date.from(ZonedDateTime.of(2020, 2, 29, 0, 0, 0, 0, system).toInstant())), // ISO‑8601 string with +02:00 offset
        Arguments.of("2023-07-04T09:30:00+02:00", converter, Date.from(Instant.parse("2023-07-04T07:30:00Z"))), // Date‑only ISO string – midnight in default zone
        Arguments.of("2023-07-04", converter, Date.from(ZonedDateTime.of(2023, 7, 4, 0, 0, 0, 0, system).toInstant())));
    }

    /**
     * Test cases that must result in an exception being thrown.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Unsupported type – should raise IllegalArgumentException
        Arguments.of(new UnsupportedType(), converter, IllegalArgumentException.class), // Null source – internal toLong throws NPE
        Arguments.of(null, converter, NullPointerException.class), // LocalTime – no date component
        Arguments.of(LocalTime.of(15, 30, 45), converter, IllegalArgumentException.class), // MonthDay – missing year
        Arguments.of(MonthDay.of(12, 25), converter, IllegalArgumentException.class), // CustomObject with a converter that returns a String (not a Number)
        Arguments.of(new CustomObject(), customConverterReturningString, IllegalArgumentException.class), // Malformed ISO string
        Arguments.of("invalid-date-string", converter, DateTimeParseException.class), // BigDecimal overflow (value > Long.MAX_VALUE)
        Arguments.of(new BigDecimal("9223372036854775808"), converter, ArithmeticException.class), // Converter that deliberately throws RuntimeException
        Arguments.of("12345", exceptionThrowingConverter, RuntimeException.class), // Converter that returns null – leads to NPE when unboxing
        Arguments.of("67890", nullReturningConverter, NullPointerException.class), // Boolean – unsupported type
        Arguments.of(Boolean.TRUE, converter, IllegalArgumentException.class), // Instant beyond Long.MAX_VALUE milliseconds
        Arguments.of(Instant.ofEpochMilli(Long.MAX_VALUE).plusSeconds(1), converter, ArithmeticException.class), // Year with negative value – invalid year
        Arguments.of(Year.of(-5), converter, DateTimeException.class));
    }

    @Test
    void toDate_fromZonedDateTime_returnsEquivalentDate() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 12, 30, 0, 0, ZoneId.of("UTC"));
        // WHEN
        Date result = ZonedDateTimeConversions.toDate(zdt, converter);
        // THEN
        assertEquals(Date.from(zdt.toInstant()), result, "Date produced from ZonedDateTime should match the instant of the source");
    }

    @Test
    void toDate_fromIsoString_returnsEquivalentDate() {
        // GIVEN
        // ISO‑8601 instant in UTC
        String iso = "2023-05-10T12:30:00Z";
        Instant expectedInstant = Instant.parse(iso);
        // WHEN
        Date result = ZonedDateTimeConversions.toDate(iso, converter);
        // THEN
        assertEquals(Date.from(expectedInstant), result, "Date produced from ISO‑8601 string should represent the same instant");
    }

    @Test
    void toDate_fromMapWithTimeKey_returnsEquivalentDate() {
        // GIVEN
        // corresponds to 2023‑05‑10T12:30:00Z
        long epochMillis = 1_683_714_600_000L;
        Map<String, Object> map = Map.of("time", epochMillis);
        // WHEN
        Date result = ZonedDateTimeConversions.toDate(map, converter);
        // THEN
        assertEquals(new Date(epochMillis), result, "Date produced from map containing a 'time' key should match the supplied epoch millis");
    }

    @Test
    void toDate_fromNull_returnsEpochZeroDate() {
        // GIVEN
        Object from = null;
        long epochZero = 0L;
        // WHEN
        Date result = ZonedDateTimeConversions.toDate(from, converter);
        // THEN
        assertEquals(new Date(epochZero), result, "Null source should be converted to a Date representing epoch 0");
    }

    @ParameterizedTest(name = "[{index}] toDate({0}) → expected {2}")
    @MethodSource("validConversionProvider")
    void toDate_validConversions(Object from, Converter conv, Date expected) {
        Date result = ZonedDateTimeConversions.toDate(from, conv);
        assertEquals(expected, result, () -> "Conversion of " + from.getClass().getSimpleName() + " should produce the correct Date");
    }

    @ParameterizedTest(name = "[{index}] edge conversion {0}")
    @MethodSource("edgeConversionProvider")
    void toDate_edgeConversions(Object from, Converter conv, Date expected) {
        Date result = ZonedDateTimeConversions.toDate(from, conv);
        assertEquals(expected, result, () -> "Edge conversion of " + from.getClass().getSimpleName() + " should match expected Date");
    }

    @ParameterizedTest(name = "[{index}] exception case {0}")
    @MethodSource("exceptionProvider")
    void toDate_expectedExceptions(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toDate(from, conv), () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should throw " + expectedException.getSimpleName());
    }
}
