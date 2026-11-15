package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toTimestamp_Test {

    /**
     * Shared {@link Converter} used by all test methods.
     */
    private static Converter converter;

    /**
     * Shared default {@link Converter} used for the majority of tests.
     */
    private static Converter defaultConverter;

    @Mock
    Converter // injected by Mockito
    stringToInstantConverter;

    @Mock
    Converter nullReturningConverter;

    @Mock
    Converter exceptionThrowingConverter;

    @Mock
    Converter returnDateConverter;

    static class CustomTemporal {
        /* dummy placeholder */
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special overrides are required for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference for GC (good practice in large test suites).
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// java.sql.Timestamp → same instant
        Arguments.of(Timestamp.valueOf("2023-01-15 12:34:56.789"), Timestamp.valueOf("2023-01-15 12:34:56.789"), "Timestamp input should be returned unchanged (value‑wise)"), // java.util.Date → epoch millis
        Arguments.of(new Date(1673782496789L), Timestamp.from(Instant.ofEpochMilli(1673782496789L)), "Date should be converted via its epoch milli value"), // Instant → direct pass‑through
        Arguments.of(Instant.parse("2023-02-20T08:15:30.123Z"), Timestamp.from(Instant.parse("2023-02-20T08:15:30.123Z")), "Instant should be wrapped directly"), // LocalDateTime → system default zone
        Arguments.of(LocalDateTime.of(2023, 3, 10, 14, 22, 33, 456_000_000), Timestamp.from(LocalDateTime.of(2023, 3, 10, 14, 22, 33, 456_000_000).atZone(ZoneId.systemDefault()).toInstant()), "LocalDateTime interpreted in the system default zone"), // ZonedDateTime → same instant
        Arguments.of(ZonedDateTime.of(2023, 4, 5, 9, 0, 0, 0, ZoneId.of("Europe/Paris")), Timestamp.from(ZonedDateTime.of(2023, 4, 5, 9, 0, 0, 0, ZoneId.of("Europe/Paris")).toInstant()), "ZonedDateTime should keep its instant"), // OffsetDateTime → same instant
        Arguments.of(OffsetDateTime.of(2023, 5, 12, 16, 45, 0, 0, ZoneOffset.ofHours(-4)), Timestamp.from(OffsetDateTime.of(2023, 5, 12, 16, 45, 0, 0, ZoneOffset.ofHours(-4)).toInstant()), "OffsetDateTime should be converted via its offset"), // LocalDate → start of day in default zone
        Arguments.of(LocalDate.of(2023, 6, 1), Timestamp.from(LocalDate.of(2023, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), "LocalDate interpreted as start of day in default zone"), // Duration → epoch + duration
        Arguments.of(Duration.ofHours(5).plusMinutes(30), Timestamp.from(Instant.EPOCH.plus(Duration.ofHours(5).plusMinutes(30))), "Duration should be treated as offset from the epoch"));
    }

    @Test
    void toTimestamp_fromDate_returnsEquivalentTimestamp() {
        // GIVEN
        // 2022-01-01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        Date date = new Date(epochMilli);
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(date, converter);
        // THEN
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(epochMilli, ts.getTime(), "Timestamp derived from Date should have the same epoch milliseconds");
    }

    @Test
    void toTimestamp_fromInstant_returnsEquivalentTimestamp() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-15T12:34:56.789Z");
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(instant, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(instant.toEpochMilli(), ts.getTime(), "Timestamp derived from Instant should match the instant's epoch milliseconds");
        // Verify nanos component is preserved (Timestamp stores nanos separately)
        assertEquals(instant.getNano(), ts.getNanos(), "Nanos part of the Instant should be reflected in the Timestamp");
    }

    @Test
    void toTimestamp_fromLocalDateTime_assumesSystemDefaultZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 59, 59, 123_456_789);
        // The conversion uses the system default zone unless the Converter overrides it.
        ZoneId zone = ZoneId.systemDefault();
        long expectedEpochMilli = ldt.atZone(zone).toInstant().toEpochMilli();
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(ldt, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(expectedEpochMilli, ts.getTime(), "Timestamp derived from LocalDateTime should match epoch milliseconds in the system default zone");
        assertEquals(ldt.getNano(), ts.getNanos(), "Nanos part of the LocalDateTime should be preserved in the Timestamp");
    }

    @Test
    void toTimestamp_fromLongEpochMillis_returnsCorrespondingTimestamp() {
        // GIVEN
        long epochMilli = System.currentTimeMillis();
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(epochMilli, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(epochMilli, ts.getTime(), "Timestamp created from a long epoch millisecond value should match the value");
    }

    @Test
    void toTimestamp_fromTimestamp_returnsSameInstanceValue() {
        // GIVEN
        Timestamp original = new Timestamp(1_600_000_000_000L);
        original.setNanos(555_000_000);
        // WHEN
        Timestamp result = DoubleConversions.toTimestamp(original, converter);
        // THEN
        assertNotNull(result);
        // The conversion creates a new Timestamp instance, but the value must be equal.
        assertEquals(original.getTime(), result.getTime(), "Converted Timestamp should have the same epoch milliseconds");
        assertEquals(original.getNanos(), result.getNanos(), "Converted Timestamp should preserve the nanos component");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toTimestamp_validConversions(Object source, Timestamp expected, String description) {
        Timestamp result = DoubleConversions.toTimestamp(source, defaultConverter);
        assertNotNull(result, "Resulting Timestamp must not be null");
        assertEquals(expected.getTime(), result.getTime(), "Epoch milliseconds must match expected value");
        assertEquals(expected.getNanos(), result.getNanos(), "Nanosecond component must match expected value");
    }

    @Test
    void toTimestamp_fromCalendar_returnsCorrespondingInstant() {
        // GIVEN a UTC calendar set to a known epoch milli
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1673782496789L);
        // WHEN
        Timestamp ts = DoubleConversions.toTimestamp(cal, defaultConverter);
        // THEN
        assertEquals(1673782496789L, ts.getTime(), "Calendar epoch milli should be preserved");
        assertEquals(0, ts.getNanos(), "Calendar does not carry nanos – expect zero");
    }

    @Test
    void toTimestamp_fromSqlDate_usesMidnightInDefaultZone() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-01-15");
        // java.sql.Date represents midnight in the default time‑zone.
        long expectedEpochMilli = sqlDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Timestamp ts = DoubleConversions.toTimestamp(sqlDate, defaultConverter);
        assertEquals(expectedEpochMilli, ts.getTime(), "sql.Date should be interpreted as start‑of‑day in default zone");
    }

    @Test
    void toTimestamp_fromLocalTime_throwsIllegalArgumentException() {
        LocalTime lt = LocalTime.of(14, 30, 15, 123_000_000);
        assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toTimestamp(lt, defaultConverter), "LocalTime lacks date/zone information and should cause IAE");
    }

    @Test
    void toTimestamp_fromLongEpochMillis_returnsExactTimestamp() {
        long epoch = 123_456_789L;
        Timestamp ts = DoubleConversions.toTimestamp(epoch, defaultConverter);
        assertEquals(epoch, ts.getTime(), "Long epoch millis should be directly used");
        assertEquals(0, ts.getNanos(), "Long conversion does not provide nanos – expect zero");
    }

    @Test
    void toTimestamp_fromString_usingCustomConverter() {
        String source = "2023-03-01T12:00:00Z";
        Instant instant = Instant.parse(source);
        // Stub the converter to return the expected Instant
        when(stringToInstantConverter.convert(eq(source), eq(Instant.class))).thenReturn(instant);
        Timestamp ts = DoubleConversions.toTimestamp(source, stringToInstantConverter);
        assertEquals(instant.toEpochMilli(), ts.getTime(), "Timestamp should wrap the Instant produced by the custom converter");
        assertEquals(instant.getNano(), ts.getNanos(), "Nanos should be preserved from the Instant returned by the converter");
    }

    @Test
    void toTimestamp_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DoubleConversions.toTimestamp(null, defaultConverter), "Null source should result in NPE");
    }

    @Test
    void toTimestamp_unsupportedTypeWithoutConverter_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toTimestamp(Boolean.TRUE, defaultConverter), "Unsupported source type without a converter should cause IAE");
    }

    @Test
    void toTimestamp_converterReturnsNull_throwsNullPointerException() {
        CustomTemporal obj = new CustomTemporal();
        // Simulate a converter that returns null for the given object
        when(nullReturningConverter.convert(eq(obj), eq(Instant.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> DoubleConversions.toTimestamp(obj, nullReturningConverter), "Converter returning null should cause NPE when Timestamp.from(null) is invoked");
    }

    @Test
    void toTimestamp_converterThrowsException_propagates() {
        Object source = new Object();
        when(exceptionThrowingConverter.convert(eq(source), eq(Instant.class))).thenThrow(new IllegalStateException("converter failure"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> DoubleConversions.toTimestamp(source, exceptionThrowingConverter), "Exception from converter should propagate");
        assertEquals("converter failure", ex.getMessage());
    }

    @Test
    void toTimestamp_converterReturnsWrongType_throwsClassCastException() {
        Object source = new Object();
        Date wrong = new Date();
        // Return a Date where an Instant is expected
        when(returnDateConverter.convert(eq(source), eq(Instant.class))).thenReturn(wrong);
        assertThrows(ClassCastException.class, () -> DoubleConversions.toTimestamp(source, returnDateConverter), "Returning a non‑Instant should cause a ClassCastException");
    }

    @Test
    void toTimestamp_longMaxValue_throwsDateTimeException() {
        assertThrows(DateTimeException.class, () -> DoubleConversions.toTimestamp(Long.MAX_VALUE, defaultConverter), "Long.MAX_VALUE epoch millis exceeds Instant range and should throw DateTimeException");
    }

    @Test
    void toTimestamp_instantBeyondMax_throwsDateTimeException() {
        Instant beyond = Instant.MAX.plusSeconds(1);
        assertThrows(DateTimeException.class, () -> DoubleConversions.toTimestamp(beyond, defaultConverter), "Instant beyond MAX should cause DateTimeException");
    }

    @Test
    void toTimestamp_localDateTimeOutOfRange_throwsDateTimeException() {
        // Year.MAX_VALUE is far beyond the range supported by Instant
        LocalDateTime ldt = LocalDateTime.of(Year.MAX_VALUE, 1, 1, 0, 0);
        assertThrows(DateTimeException.class, () -> DoubleConversions.toTimestamp(ldt, defaultConverter), "LocalDateTime with year beyond Instant range should throw DateTimeException");
    }
}
