package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

class InstantConversions_toDate_Test {

    private static Converter converter;

    private static Converter defaultConverter;

    private static class NumericCase {

        final Object input;

        final long expectedMillis;

        final String description;

        NumericCase(Object input, long expectedMillis, String description) {
            this.input = input;
            this.expectedMillis = expectedMillis;
            this.description = description;
        }
    }

    /**
     * Simple holder used for the custom‑converter test.
     */
    private static final class MyInstantHolder {

        private final Instant instant;

        MyInstantHolder(Instant instant) {
            this.instant = instant;
        }

        Instant getInstant() {
            return instant;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default options are sufficient for these tests
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<NumericCase> numericCases() {
        return Stream.of(new NumericCase(new Long(1_672_570_496_000L), 1_672_570_496_000L, "Long"), new NumericCase(BigDecimal.valueOf(1_672_570_496_000L), 1_672_570_496_000L, "BigDecimal (no fraction)"), new NumericCase(new BigDecimal("1_672_570_496_000.999"), 1_672_570_496_000L, "BigDecimal with fraction (truncated)"), new NumericCase(new BigInteger("1672570496000"), 1_672_570_496_000L, "BigInteger"), new NumericCase(new AtomicLong(1_672_570_496_000L), 1_672_570_496_000L, "AtomicLong"), new NumericCase(Integer.valueOf(1_672_570_496), 1_672_570_496L, "Integer"));
    }

    @Test
    void toDate_fromDate_returnsSameInstant() {
        Date now = new Date();
        Date result = InstantConversions.toDate(now, defaultConverter);
        assertEquals(now.getTime(), result.getTime(), "Converting a Date should preserve the exact epoch milliseconds");
    }

    @Test
    void toDate_fromLong_returnsDateWithSameMillis() {
        long millis = System.currentTimeMillis();
        Date result = InstantConversions.toDate(millis, defaultConverter);
        assertEquals(millis, result.getTime(), "Converting a Long should produce a Date with the same epoch milliseconds");
    }

    @Test
    void toDate_fromStringNumeric_returnsDateWithParsedMillis() {
        // arbitrary epoch ms
        long millis = 1_640_000_000_000L;
        String source = Long.toString(millis);
        Date result = InstantConversions.toDate(source, defaultConverter);
        assertEquals(millis, result.getTime(), "Converting a numeric String should be parsed to the corresponding epoch milliseconds");
    }

    @Test
    void toDate_fromCalendar_returnsDateWithCalendarTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_600_000_000_000L);
        Date result = InstantConversions.toDate(cal, defaultConverter);
        assertEquals(cal.getTimeInMillis(), result.getTime(), "Converting a Calendar should yield a Date with the same epoch milliseconds");
    }

    @Test
    void toDate_fromInstant_returnsDateWithInstantEpochMilli() {
        Instant instant = Instant.now();
        Date result = InstantConversions.toDate(instant, defaultConverter);
        assertEquals(instant.toEpochMilli(), result.getTime(), "Converting an Instant should produce a Date with the same epoch milliseconds");
    }

    @Test
    void toDate_fromLocalDateTime_returnsDateUsingSystemDefaultZone() {
        LocalDateTime ldt = LocalDateTime.now();
        ZoneId zone = ZoneId.systemDefault();
        long expected = ldt.atZone(zone).toInstant().toEpochMilli();
        Date result = InstantConversions.toDate(ldt, defaultConverter);
        assertEquals(expected, result.getTime(), "Converting a LocalDateTime should use the system default zone to compute epoch milliseconds");
    }

    @Test
    void toDate_fromNull_returnsEpochZeroDate() {
        Date result = InstantConversions.toDate(null, defaultConverter);
        assertEquals(0L, result.getTime(), "Converting null should follow Converter's null‑handling contract and return epoch 0 for Date");
    }

    @ParameterizedTest(name = "toDate from {2} should yield epoch millis {1}")
    @MethodSource("numericCases")
    void toDate_fromVariousNumericTypes(NumericCase tc) {
        Date result = InstantConversions.toDate(tc.input, defaultConverter);
        assertEquals(tc.expectedMillis, result.getTime(), "Numeric conversion failed for " + tc.description);
    }

    @Test
    void toDate_fromTimestamp_preservesNanos() {
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56.123");
        // Timestamp#getTime returns the milliseconds part only; the nanosecond part is stored separately.
        // InstantConversions.toLong should combine both, so we compute the expected epoch millis accordingly.
        // milliseconds part (the nanoseconds are ignored for Date)
        long expected = ts.getTime();
        Date result = InstantConversions.toDate(ts, defaultConverter);
        assertEquals(expected, result.getTime(), "Timestamp conversion should preserve the millisecond component");
    }

    @Test
    void toDate_fromInstantDirect() {
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        Date result = InstantConversions.toDate(instant, defaultConverter);
        assertEquals(instant.toEpochMilli(), result.getTime(), "Instant conversion should yield the same epoch millis");
    }

    @Test
    void toDate_fromZonedDateTimeUtc() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneId.of("UTC"));
        Date result = InstantConversions.toDate(zdt, defaultConverter);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "UTC ZonedDateTime should map directly to its instant");
    }

    @Test
    void toDate_fromOffsetDateTimeWithPositiveOffset() {
        OffsetDateTime odt = OffsetDateTime.parse("2023-01-01T12:34:56+02:00");
        Date result = InstantConversions.toDate(odt, defaultConverter);
        assertEquals(odt.toInstant().toEpochMilli(), result.getTime(), "OffsetDateTime should be converted to its UTC instant");
    }

    @Test
    void toDate_fromLocalDate_startOfDay() {
        LocalDate ld = LocalDate.of(2023, 1, 1);
        ZoneId zone = ZoneId.systemDefault();
        long expected = ld.atStartOfDay(zone).toInstant().toEpochMilli();
        Date result = InstantConversions.toDate(ld, defaultConverter);
        assertEquals(expected, result.getTime(), "LocalDate should be treated as start‑of‑day in the system default zone");
    }

    @Test
    void toDate_fromLocalTime_combinedWithEpochDate() {
        LocalTime lt = LocalTime.of(12, 34, 56);
        ZoneId zone = ZoneId.systemDefault();
        // 1970‑01‑01 is the epoch date
        long expected = lt.atDate(LocalDate.ofEpochDay(0)).atZone(zone).toInstant().toEpochMilli();
        Date result = InstantConversions.toDate(lt, defaultConverter);
        assertEquals(expected, result.getTime(), "LocalTime should be combined with 1970‑01‑01 in the default zone");
    }

    @Test
    void toDate_fromZonedDateTimeNonUtc() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneId.of("America/New_York"));
        Date result = InstantConversions.toDate(zdt, defaultConverter);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "ZonedDateTime with a non‑UTC zone must be converted via its instant");
    }

    @Test
    void toDate_fromSqlDate_midnight() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-01-01");
        // java.sql.Date represents midnight at the default zone
        ZoneId zone = ZoneId.systemDefault();
        long expected = sqlDate.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli();
        Date result = InstantConversions.toDate(sqlDate, defaultConverter);
        assertEquals(expected, result.getTime(), "java.sql.Date should preserve the midnight instant in the default zone");
    }

    @Test
    void toDate_fromCalendarWithLosAngelesZone() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        cal.set(2023, Calendar.JANUARY, 1, 12, 34, 56);
        cal.set(Calendar.MILLISECOND, 0);
        // Calendar already stores the instant in UTC
        long expected = cal.getTimeInMillis();
        Date result = InstantConversions.toDate(cal, defaultConverter);
        assertEquals(expected, result.getTime(), "Calendar with a non‑UTC zone should be converted using its underlying instant");
    }

    @Test
    void toDate_fromOffsetDateTimeBeforeEpoch() {
        OffsetDateTime odt = OffsetDateTime.parse("1969-12-31T23:59:59-00:00");
        Date result = InstantConversions.toDate(odt, defaultConverter);
        assertEquals(-1000L, result.getTime(), "OffsetDateTime before 1970‑01‑01 should yield a negative epoch value");
    }

    @Test
    void toDate_fromLongMaxValue() {
        long max = Long.MAX_VALUE;
        Date result = InstantConversions.toDate(max, defaultConverter);
        assertEquals(max, result.getTime(), "Date construction with Long.MAX_VALUE should succeed (far‑future date)");
    }

    @Test
    void toDate_withCustomConverterExtractsInstant() {
        // Arrange – a holder containing a known instant
        MyInstantHolder holder = new MyInstantHolder(Instant.parse("2023-01-01T12:34:56Z"));
        // Mock a Converter that knows how to turn MyInstantHolder into a Long epoch millis
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq(holder), eq(Long.class))).thenReturn(holder.getInstant().toEpochMilli());
        // Act
        Date result = InstantConversions.toDate(holder, mockConverter);
        // Assert
        assertEquals(holder.getInstant().toEpochMilli(), result.getTime(), "Custom converter should extract the instant and produce the correct Date");
    }

    @Test
    void toDate_withThrowingConverterPropagatesException() {
        // Arrange – a mock that throws IllegalStateException for any conversion request
        Converter throwingConverter = mock(Converter.class);
        when(throwingConverter.convert(any(), any())).thenThrow(new IllegalStateException("Conversion failed"));
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> InstantConversions.toDate("1672570496000", throwingConverter), "Exception from a custom converter must propagate");
        assertEquals("Conversion failed", ex.getMessage());
    }

    @Test
    void toDate_fromBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toDate(Boolean.TRUE, defaultConverter), "Boolean source should not be convertible to a Date");
    }

    @Test
    void toDate_fromArray_throwsIllegalArgumentException() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toDate(arr, defaultConverter), "Array source should not be convertible to a Date");
    }

    @Test
    void toDate_fromUuid_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toDate(java.util.UUID.randomUUID(), defaultConverter), "UUID source should not be convertible to a Date");
    }

    @Test
    void toDate_fromCustomObjectWithoutConverter_throwsIllegalArgumentException() {
        MyInstantHolder holder = new MyInstantHolder(Instant.now());
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toDate(holder, null), "Custom object without a converter must cause IllegalArgumentException");
    }

    @Test
    void toDate_fromBlankString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> InstantConversions.toDate("   ", defaultConverter), "Blank numeric string should cause NumberFormatException");
    }

    @Test
    void toDate_fromNaN_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toDate(Double.NaN, defaultConverter), "NaN is not a valid epoch‑millis value");
    }

    @Test
    void toDate_fromFloat_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toDate(Float.valueOf(123.45f), defaultConverter), "Floating‑point numbers other than Double should be rejected");
    }

    @Test
    void toDate_fromInvalidNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> InstantConversions.toDate("not a number", defaultConverter), "Non‑numeric string should cause NumberFormatException");
    }
}
