package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toLocalDateTime_Test {

    // default system‑zone converter
    private Converter converter;

    // converter forced to UTC
    private Converter utcConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter that uses the JVM default zone (same as the existing test class).
        this.converter = new Converter(new DefaultConverterOptions());
        // Converter that forces UTC – useful to verify that the zone supplied in
        // ConverterOptions is honoured by TimestampConversions#toLocalDateTime.
        ConverterOptions utcOptions = new DefaultConverterOptions();
        utcOptions.setZoneId(ZoneOffset.UTC);
        this.utcConverter = new Converter(utcOptions);
    }

    /**
     * Helper that creates a {@link Timestamp} from a date string and time components.
     */
    private Timestamp createTimestamp(String dateStr, int hour, int minute, int second, int nanos, ZoneId zone) {
        return Timestamp.from(LocalDate.parse(dateStr).atTime(hour, minute, second, nanos).atZone(zone).toInstant());
    }

    /**
     * Helper that creates a {@link Timestamp} from an ISO‑8601 instant string.
     */
    private static Timestamp timestampFromInstant(String isoInstant) {
        return Timestamp.from(Instant.parse(isoInstant));
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// UTC conversion – result should match the timestamp fields
        Arguments.of("UTC conversion – timestamp fields unchanged", timestampFromInstant("2023-03-15T10:20:30.500Z"), ZoneOffset.UTC, LocalDateTime.of(2023, 3, 15, 10, 20, 30, 500_000_000)), // New York (EDT) conversion – shift by -4 hours on 2023‑03‑15
        Arguments.of("New York conversion – UTC‑4 offset applied", timestampFromInstant("2023-03-15T10:20:30.500Z"), ZoneId.of("America/New_York"), LocalDateTime.of(2023, 3, 15, 6, 20, 30, 500_000_000)), // Europe/Paris (UTC+1 winter) conversion of epoch
        Arguments.of("Paris conversion – epoch shifted to 01:00", timestampFromInstant("1970-01-01T00:00:00.000Z"), ZoneId.of("Europe/Paris"), LocalDateTime.of(1970, 1, 1, 1, 0, 0, 0)), // 2038‑01‑19 03:14:07 UTC (max 32‑bit signed Unix time)
        Arguments.of("Boundary 2038‑01‑19 UTC", timestampFromInstant("2038-01-19T03:14:07.000Z"), ZoneOffset.UTC, LocalDateTime.of(2038, 1, 19, 3, 14, 7, 0)), // One millisecond before epoch
        Arguments.of("Just before epoch UTC", timestampFromInstant("1969-12-31T23:59:59.999Z"), ZoneOffset.UTC, LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_000_000)), // Spring‑forward DST gap in New York (2023‑03‑12 07:30Z => 03:30‑04)
        Arguments.of("DST spring‑forward New York", timestampFromInstant("2023-03-12T07:30:00.000Z"), ZoneId.of("America/New_York"), LocalDateTime.of(2023, 3, 12, 3, 30, 0, 0)), // Fall‑back DST transition in New York (2023‑11‑05 05:30Z => 01:30‑05)
        Arguments.of("DST fall‑back New York", timestampFromInstant("2023-11-05T05:30:00.000Z"), ZoneId.of("America/New_York"), LocalDateTime.of(2023, 11, 5, 1, 30, 0, 0)), // Fixed offset +05:30 (Asia/Kolkata) conversion
        Arguments.of("Fixed offset +05:30", timestampFromInstant("2023-06-01T12:00:00.000Z"), ZoneOffset.ofHoursMinutes(5, 30), LocalDateTime.of(2023, 6, 1, 17, 30, 0, 0)), // Custom options – Europe/Moscow (UTC+3) conversion
        Arguments.of("Custom options – Europe/Moscow", timestampFromInstant("2022-12-31T23:00:00.000Z"), ZoneId.of("Europe/Moscow"), LocalDateTime.of(2023, 1, 1, 2, 0, 0, 0)), // Null ZoneId – expecting NullPointerException later (handled in exception tests)
        // We still provide a case here to illustrate the setup; the actual assertion
        // is performed in the exception‑scenario test group.
        Arguments.of("Null ZoneId – will trigger NPE", timestampFromInstant("2023-03-15T10:20:30.500Z"), null, null));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        // Mock converter with null options
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // Mock converter with options returning null ZoneId
        ConverterOptions optsNullZone = mock(ConverterOptions.class);
        when(optsNullZone.getZoneId()).thenReturn(null);
        Converter converterWithNullZone = mock(Converter.class);
        when(converterWithNullZone.getOptions()).thenReturn(optsNullZone);
        // Mock a normal converter (UTC) for overflow tests
        Converter utcConverter = new Converter(new DefaultConverterOptions(ZoneOffset.UTC));
        return Stream.of(// Null source object
        Arguments.of("Null source object → NPE", null, utcConverter, NullPointerException.class), // Wrong source type (java.util.Date)
        Arguments.of("Wrong source type (Date) → ClassCastException", new java.util.Date(), utcConverter, ClassCastException.class), // Converter with null ZoneId (atZone(null) throws NPE)
        Arguments.of("Converter with null ZoneId → NullPointerException", timestampFromInstant("2023-03-15T10:20:30.500Z"), converterWithNullZone, NullPointerException.class), // Null converter argument
        Arguments.of("Null converter → NullPointerException", timestampFromInstant("2023-03-15T10:20:30.500Z"), null, NullPointerException.class), // Converter with null options
        Arguments.of("Converter with null options → NullPointerException", timestampFromInstant("2023-03-15T10:20:30.500Z"), converterWithNullOptions, NullPointerException.class), // Timestamp with Long.MAX_VALUE (overflow)
        Arguments.of("Timestamp(Long.MAX_VALUE) → DateTimeException", new Timestamp(Long.MAX_VALUE), utcConverter, DateTimeException.class), // Timestamp with Long.MIN_VALUE (underflow)
        Arguments.of("Timestamp(Long.MIN_VALUE) → DateTimeException", new Timestamp(Long.MIN_VALUE), utcConverter, DateTimeException.class));
    }

    @Test
    void testToLocalDateTimeUsesConverterZone() {
        // System default zone test – the expected LocalDateTime is built with the same zone.
        ZoneId sysZone = ZoneId.systemDefault();
        Timestamp ts = createTimestamp("2023-06-15", 15, 30, 45, 123_456_789, sysZone);
        LocalDateTime expected = LocalDateTime.of(2023, 6, 15, 15, 30, 45, 123_456_789);
        LocalDateTime result = TimestampConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, result, "Conversion should respect the Converter's default (system) zone");
    }

    @Test
    void testToLocalDateTimeWithUtcConverter() {
        // UTC converter – the timestamp is created in UTC, conversion must yield the same instant
        // expressed as a LocalDateTime in UTC.
        Timestamp ts = createTimestamp("1970-01-01", 0, 0, 0, 0, ZoneOffset.UTC);
        LocalDateTime expected = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0);
        LocalDateTime result = TimestampConversions.toLocalDateTime(ts, utcConverter);
        assertEquals(expected, result, "Conversion should respect the UTC zone configured in the Converter");
    }

    @Test
    void testVariousDatesAndTimes() {
        ZoneId zone = ZoneId.systemDefault();
        // A collection of representative timestamps.
        Object[][] data = { { "1888-01-02", 12, 30, 45, 123_456_789 }, { "1969-12-31", 23, 59, 59, 999_999_999 }, { "1970-01-01", 0, 0, 1, 1 }, { "2023-06-15", 15, 30, 0, 500_000_000 } };
        for (Object[] row : data) {
            String date = (String) row[0];
            int h = (int) row[1];
            int m = (int) row[2];
            int s = (int) row[3];
            int n = (int) row[4];
            Timestamp ts = createTimestamp(date, h, m, s, n, zone);
            LocalDateTime expected = LocalDateTime.of(Integer.parseInt(date.substring(0, 4)), Integer.parseInt(date.substring(5, 7)), Integer.parseInt(date.substring(8, 10)), h, m, s, n);
            LocalDateTime actual = TimestampConversions.toLocalDateTime(ts, converter);
            assertEquals(expected, actual, () -> "Failed conversion for timestamp: " + ts);
        }
    }

    @Test
    void testClassCastExceptionWhenSourceIsNotTimestamp() {
        // The method casts the source to Timestamp without checks – a non‑Timestamp should
        // trigger a ClassCastException.
        Object notATimestamp = "2023-01-01";
        assertThrows(ClassCastException.class, () -> TimestampConversions.toLocalDateTime(notATimestamp, new Converter(new DefaultConverterOptions())), "Expected ClassCastException when source object is not a Timestamp");
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (parameterised)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void testSuccessfulConversions(String description, Timestamp timestamp, ZoneId zoneId, LocalDateTime expected) {
        // Build a real Converter with the required zone (or mock if zoneId is null)
        Converter converter;
        if (zoneId == null) {
            // null zone – we need a mock that returns null from getZoneId()
            ConverterOptions options = mock(ConverterOptions.class);
            when(options.getZoneId()).thenReturn(null);
            converter = mock(Converter.class);
            when(converter.getOptions()).thenReturn(options);
        } else {
            ConverterOptions options = new DefaultConverterOptions();
            options.setZoneId(zoneId);
            converter = new Converter(options);
        }
        LocalDateTime result = TimestampConversions.toLocalDateTime(timestamp, converter);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Exception scenario test cases (parameterised)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionScenarios")
    void testExceptionScenarios(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> TimestampConversions.toLocalDateTime(from, converter), description);
    }
}
