package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDate_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * A regular {@link Converter} used for the majority of tests.
     */
    private static Converter defaultConverter;

    /**
     * Simple stub that always returns the supplied long value 5555L.
     */
    private static class StubConverterReturns5555 extends Converter {

        StubConverterReturns5555() {
            super(new ConverterOptions());
        }

        @Override
        public Object convert(Object from, Class<?> toClass) {
            return 5555L;
        }
    }

    /**
     * Stub that returns {@code null}.
     */
    private static class StubConverterReturnsNull extends Converter {

        StubConverterReturnsNull() {
            super(new ConverterOptions());
        }

        @Override
        public Object convert(Object from, Class<?> toClass) {
            return null;
        }
    }

    /**
     * Stub that throws {@link IllegalStateException}.
     */
    private static class StubConverterThrowsIllegalState extends Converter {

        StubConverterThrowsIllegalState() {
            super(new ConverterOptions());
        }

        @Override
        public Object convert(Object from, Class<?> toClass) {
            throw new IllegalStateException("forced");
        }
    }

    /**
     * Stub that returns a non‑numeric object (e.g., a String).
     */
    private static class StubConverterReturnsNonNumeric extends Converter {

        StubConverterReturnsNonNumeric() {
            super(new ConverterOptions());
        }

        @Override
        public Object convert(Object from, Class<?> toClass) {
            // not a Number
            return new Object();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release reference for GC – not strictly necessary but keeps the pattern consistent.
        converter = null;
    }

    @Test
    void toDate_fromLong_returnsSameInstant() {
        // given
        // an arbitrary epoch‑millis value
        Long source = 1_642_000_000_000L;
        // when
        Date result = NumberConversions.toDate(source, converter);
        // then
        assertNotNull(result);
        assertEquals(source.longValue(), result.getTime(), "Date created from a Long should have the same epoch‑millis value.");
    }

    @Test
    void toDate_fromDate_returnsEqualDate() {
        // given
        Date source = new Date(1_642_000_123_456L);
        // when
        Date result = NumberConversions.toDate(source, converter);
        // then
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "Converting a Date should preserve the original epoch‑millis.");
    }

    @Test
    void toDate_fromTimestamp_returnsEqualDate() {
        // given
        Timestamp source = new Timestamp(1_642_000_987_654L);
        // when
        Date result = NumberConversions.toDate(source, converter);
        // then
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "Converting a Timestamp should preserve the original epoch‑millis.");
    }

    @Test
    void toDate_fromInstant_returnsEqualDate() {
        // given
        Instant source = Instant.ofEpochMilli(1_642_001_111_222L);
        // when
        Date result = NumberConversions.toDate(source, converter);
        // then
        assertNotNull(result);
        assertEquals(source.toEpochMilli(), result.getTime(), "Converting an Instant should produce a Date with the same epoch‑millis.");
    }

    @Test
    void toDate_fromNull_returnsEpochZeroDate() {
        // given
        Object source = null;
        // when
        Date result = NumberConversions.toDate(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "A null source should be converted to a Date representing epoch start (0 ms).");
    }

    @Test
    void toDate_fromStringNumber_returnsParsedDate() {
        // given
        // numeric string representing epoch‑millis
        String source = "1642002000000";
        // when
        Date result = NumberConversions.toDate(source, converter);
        // then
        assertNotNull(result);
        assertEquals(1_642_002_000_000L, result.getTime(), "A numeric String should be parsed to its long value and converted to Date.");
    }

    @Test
    void toDate_fromLocalDate_returnsStartOfDayUtc() {
        LocalDate source = LocalDate.of(2020, 1, 1);
        long expected = source.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "LocalDate conversion should treat the date as start of day at UTC.");
    }

    @Test
    void toDate_fromLocalDateTime_usesSystemDefaultZone() {
        LocalDateTime source = LocalDateTime.of(2020, 1, 1, 12, 30, 45);
        ZoneId zone = ZoneId.systemDefault();
        long expected = source.atZone(zone).toInstant().toEpochMilli();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "LocalDateTime should be converted using the system default zone.");
    }

    @Test
    void toDate_fromZonedDateTime_respectsItsOffset() {
        ZonedDateTime source = ZonedDateTime.parse("2020-01-01T12:30:45+02:00[Europe/Paris]");
        long expected = source.toInstant().toEpochMilli();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "ZonedDateTime conversion should respect its zone offset.");
    }

    @Test
    void toDate_fromBigDecimal_truncatesFraction() {
        BigDecimal source = new BigDecimal("12345.67");
        // truncates
        long expected = source.longValue();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "BigDecimal should be converted to long via truncation of the fractional part.");
    }

    @Test
    void toDate_fromNegativeBigDecimal_truncatesFraction() {
        BigDecimal source = new BigDecimal("-12345.67");
        // truncates toward zero
        long expected = source.longValue();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "Negative BigDecimal should be truncated and converted to long.");
    }

    @Test
    void toDate_fromBigInteger() {
        BigInteger source = new BigInteger("9876543210");
        long expected = source.longValue();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "BigInteger conversion to long and then to Date.");
    }

    @Test
    void toDate_fromAtomicLong() {
        AtomicLong source = new AtomicLong(7_777_777L);
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(7_777_777L, result.getTime(), "AtomicLong should be unwrapped to its long value.");
    }

    @Test
    void toDate_fromAtomicInteger() {
        AtomicInteger source = new AtomicInteger(12_345);
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(12_345L, result.getTime(), "AtomicInteger should be unwrapped to its int value and then to long.");
    }

    @Test
    void toDate_fromAtomicBoolean_isConvertedViaBoolean() {
        // Boolean conversion is not defined for toLong; this test ensures it throws.
        AtomicBoolean source = new AtomicBoolean(true);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDate(source, defaultConverter), "AtomicBoolean is not a supported type for toLong.");
    }

    @Test
    void toDate_fromInteger() {
        Integer source = Integer.valueOf(42);
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(42L, result.getTime(), "Integer should be unboxed and converted to a Date via toLong.");
    }

    @Test
    void toDate_fromCalendarUtc() {
        Calendar source = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        source.setTimeInMillis(1_642_123_456_789L);
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(1_642_123_456_789L, result.getTime(), "Calendar instance should be converted to its underlying millisecond value.");
    }

    @Test
    void toDate_fromLocalTime_combinesWithToday() {
        LocalTime source = LocalTime.of(15, 45, 30);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        long expected = LocalDateTime.of(today, source).atZone(zone).toInstant().toEpochMilli();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "LocalTime is combined with the current date to compute epoch milliseconds.");
    }

    @Test
    void toDate_fromOffsetDateTime_usesOffset() {
        OffsetDateTime source = OffsetDateTime.parse("2020-01-01T12:00:00+01:00");
        long expected = source.toInstant().toEpochMilli();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "OffsetDateTime conversion should use its offset to compute epoch milliseconds.");
    }

    @Test
    void toDate_fromOffsetTime_combinesWithToday() {
        OffsetTime source = OffsetTime.parse("12:00:00+01:00");
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        long expected = source.atDate(today).toInstant().toEpochMilli();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "OffsetTime should be combined with today’s date to compute epoch milliseconds.");
    }

    @Test
    void toDate_fromYear_startOfYearUtc() {
        Year source = Year.of(1999);
        long expected = source.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "Year conversion should map to start of that year in UTC.");
    }

    @Test
    void toDate_fromLongMaxValue() {
        Long source = Long.MAX_VALUE;
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(Long.MAX_VALUE, result.getTime(), "Maximum long value should be accepted and wrapped in a Date.");
    }

    @Test
    void toDate_fromLongMinValue() {
        Long source = Long.MIN_VALUE;
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(Long.MIN_VALUE, result.getTime(), "Minimum long value should be accepted and wrapped in a Date.");
    }

    @Test
    void toDate_fromSqlDate() {
        java.sql.Date source = java.sql.Date.valueOf("2022-03-15");
        // midnight UTC
        long expected = source.getTime();
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(expected, result.getTime(), "java.sql.Date extends java.util.Date; conversion should yield the same epoch milliseconds.");
    }

    @Test
    void toDate_fromStringWithWhitespace() {
        String source = "   987654321   ";
        Date result = NumberConversions.toDate(source, defaultConverter);
        assertEquals(987_654_321L, result.getTime(), "String with surrounding whitespace should be trimmed and parsed.");
    }

    @Test
    void toDate_nullSourceAndNullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toDate(null, null), "Passing null for both parameters should cause NPE.");
    }

    @Test
    void toDate_nonNumericString_throwsNumberFormatException() {
        String source = "notANumber";
        assertThrows(NumberFormatException.class, () -> NumberConversions.toDate(source, defaultConverter), "Non‑numeric string cannot be parsed to long.");
    }

    @Test
    void toDate_emptyString_throwsNumberFormatException() {
        String source = "";
        assertThrows(NumberFormatException.class, () -> NumberConversions.toDate(source, defaultConverter), "Empty string is not a valid numeric representation.");
    }

    @Test
    void toDate_booleanValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDate(Boolean.TRUE, defaultConverter), "Boolean values are not supported by toLong.");
    }

    @Test
    void toDate_arbitraryObjectWithoutConverter_throwsIllegalArgumentException() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDate(source, defaultConverter), "Arbitrary object without a custom Converter should trigger IllegalArgumentException.");
    }

    @Test
    void toDate_instantBeyondLongRange_throwsDateTimeExceptionOrArithmeticException() {
        // far beyond Long.MAX_VALUE
        Instant source = Instant.MAX;
        assertThrows(RuntimeException.class, () -> NumberConversions.toDate(source, defaultConverter), "Conversion of an out‑of‑range Instant should fail.");
    }

    @Test
    void toDate_customConverterReturns5555() {
        // any object
        Object source = new Object();
        Converter stub = new StubConverterReturns5555();
        Date result = NumberConversions.toDate(source, stub);
        assertEquals(5_555L, result.getTime(), "Custom Converter returning 5555 should be wrapped in a Date.");
    }

    @Test
    void toDate_customConverterReturnsNull_throwsNPE() {
        Object source = null;
        Converter stub = new StubConverterReturnsNull();
        assertThrows(NullPointerException.class, () -> NumberConversions.toDate(source, stub), "Converter returning null should cause NPE when unboxed to primitive long.");
    }

    @Test
    void toDate_customConverterThrowsIllegalStateException_propagates() {
        Object source = "anyString";
        Converter stub = new StubConverterThrowsIllegalState();
        assertThrows(IllegalStateException.class, () -> NumberConversions.toDate(source, stub), "Exception thrown by custom Converter should propagate unchanged.");
    }

    @Test
    void toDate_customConverterReturnsNonNumeric_throwsClassCastException() {
        Object source = "123";
        Converter stub = new StubConverterReturnsNonNumeric();
        assertThrows(ClassCastException.class, () -> NumberConversions.toDate(source, stub), "Converter returning a non‑numeric object should cause ClassCastException.");
    }
}
