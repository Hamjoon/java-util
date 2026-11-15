package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toCalendar_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter has a no‑arg constructor via ConverterOptions
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        converter = null;
    }

    // ---------- Valid conversions ----------
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Long literal
        Arguments.of(1622548800000L, 1622548800000L, "Direct Long epoch milliseconds"), // Integer literal (auto‑boxed to Integer)
        Arguments.of(1622548800, 1622548800L, "Integer treated as long"), // Numeric string
        Arguments.of("1622548800000", 1622548800000L, "String containing numeric epoch millis"), // java.util.Date
        Arguments.of(new Date(1622548800000L), 1622548800000L, "java.util.Date"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2021-06-01 00:00:00.000"), 1622505600000L, "java.sql.Timestamp (UTC midnight)"), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1622548800000L), 1622548800000L, "Instant"), // LocalDateTime (system default zone)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 0, 0), LocalDateTime.of(2021, 6, 1, 0, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), "LocalDateTime interpreted in system default zone"), // ZonedDateTime UTC
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneId.of("UTC")), 1622505600000L, "ZonedDateTime UTC"), // Existing Calendar
        Arguments.of(new Calendar.Builder().setInstant(1622548800000L).build(), 1622548800000L, "Existing Calendar copy"), // AtomicLong
        Arguments.of(new AtomicLong(1622548800000L), 1622548800000L, "AtomicLong"), // BigDecimal whole number
        Arguments.of(new BigDecimal("1622548800000"), 1622548800000L, "BigDecimal whole"), // BigInteger
        Arguments.of(new BigInteger("1622548800000"), 1622548800000L, "BigInteger"), // LocalDate (system default zone start of day)
        Arguments.of(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), "LocalDate start‑of‑day"), // OffsetDateTime UTC
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC), 1622505600000L, "OffsetDateTime UTC"), // java.sql.Date (midnight in system default zone)
        Arguments.of(java.sql.Date.valueOf("2021-06-01"), java.sql.Date.valueOf("2021-06-01").toInstant().toEpochMilli(), "java.sql.Date"), // Negative epoch millis
        Arguments.of(-1L, -1L, "Negative epoch millis"), // Double whole number
        Arguments.of(1622548800000.0, 1622548800000L, "Double whole number"), // String with whitespace
        Arguments.of(" 1622548800000 ", 1622548800000L, "String with surrounding whitespace"));
    }

    // ---------- Exception scenarios ----------
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source
        Arguments.of(null, NullPointerException.class, "null source"), // Null converter
        Arguments.of(1622548800000L, NullPointerException.class, "null converter", (Converter) null), // Non‑numeric string
        Arguments.of("notANumber", NumberFormatException.class, "non‑numeric string"), // Partially numeric string
        Arguments.of("123abc", NumberFormatException.class, "string with non‑digit chars"), // Unsupported Boolean
        Arguments.of(true, IllegalArgumentException.class, "Boolean source"), // Unsupported Collection
        Arguments.of(new ArrayList<>(), IllegalArgumentException.class, "Collection source"), // BigDecimal with fraction
        Arguments.of(new BigDecimal("1622548800.5"), ArithmeticException.class, "BigDecimal with fractional part"), // Double with fraction
        Arguments.of(1622548800.75, IllegalArgumentException.class, "Double with non‑integral value"), // LocalTime (no date)
        Arguments.of(LocalTime.of(12, 30, 45), IllegalArgumentException.class, "LocalTime without date"));
    }

    @Test
    void toCalendar_fromLong_returnsCalendarWithSameTime() {
        // given
        long epochMillis = System.currentTimeMillis();
        // when
        Calendar cal = DateConversions.toCalendar(epochMillis, converter);
        // then
        assertNotNull(cal);
        assertEquals(epochMillis, cal.getTimeInMillis());
    }

    @Test
    void toCalendar_fromDate_returnsCalendarWithSameTime() {
        // given
        Date date = new Date();
        // when
        Calendar cal = DateConversions.toCalendar(date, converter);
        // then
        assertNotNull(cal);
        assertEquals(date.getTime(), cal.getTimeInMillis());
    }

    @Test
    void toCalendar_fromCalendar_returnsCopy() {
        // given
        Calendar original = Calendar.getInstance();
        original.setTimeInMillis(123456789L);
        // when
        Calendar result = DateConversions.toCalendar(original, converter);
        // then
        assertNotNull(result);
        assertEquals(original.getTimeInMillis(), result.getTimeInMillis());
        // Ensure it is a different instance (copy, not the same reference)
        assertNotSame(original, result);
    }

    @Test
    void toCalendar_fromZonedDateTime_returnsCalendarWithSameInstant() {
        // given
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        // when
        Calendar cal = DateConversions.toCalendar(zdt, converter);
        // then
        assertNotNull(cal);
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    void toCalendar_fromStringNumber_returnsCalendar() {
        // given
        // 2021‑01‑01T00:00:00Z
        String millisString = String.valueOf(1609459200000L);
        // when
        Calendar cal = DateConversions.toCalendar(millisString, converter);
        // then
        assertNotNull(cal);
        assertEquals(1609459200000L, cal.getTimeInMillis());
    }

    @Test
    void toCalendar_nullInputReturnsNull() {
        // given
        Object input = null;
        // when
        Calendar cal = DateConversions.toCalendar(input, converter);
        // then
        assertNull(cal);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toCalendar_validInputs(Object from, long expectedMillis, String description) {
        Calendar cal = DateConversions.toCalendar(from, converter);
        assertNotNull(cal, "Resulting Calendar should not be null for " + description);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Incorrect epoch millis for " + description);
    }

    @ParameterizedTest(name = "[{index}] Expect {1} for {2}")
    @MethodSource("exceptionProvider")
    void toCalendar_invalidInputs(Object from, Class<? extends Throwable> expectedException, String description) {
        // When converter is null we need a special overload; otherwise reuse the field.
        Converter usedConverter = (expectedException == NullPointerException.class && from != null) ? null : converter;
        assertThrows(expectedException, () -> DateConversions.toCalendar(from, usedConverter), "Expected " + expectedException.getSimpleName() + " for " + description);
    }
}
