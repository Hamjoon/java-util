package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toString_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    // Minimal stub for ConverterOptions – only getZoneId() is required for the tests.
    private static class StubOptions implements ConverterOptions {

        private final ZoneId zoneId;

        StubOptions(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZoneId() {
            return zoneId;
        }
    }

    // Minimal stub for Converter – only getOptions() is required.
    private static class StubConverter implements Converter {

        private final ConverterOptions options;

        StubConverter(ConverterOptions options) {
            this.options = options;
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations
        mocks = MockitoAnnotations.openMocks(this);
        // Default behaviour: UTC zone
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources
        mocks.close();
    }

    private static Stream<Arguments> provideWholeSeconds() {
        return Stream.of(Arguments.of("UTC zone – whole seconds", Timestamp.from(Instant.parse("2023-01-01T12:00:00Z")), ZoneId.of("UTC"), "2023-01-01T12:00:00+00:00"), Arguments.of("America/New_York – whole seconds", Timestamp.from(Instant.parse("2023-01-01T12:00:00Z")), ZoneId.of("America/New_York"), "2023-01-01T07:00:00-05:00"));
    }

    private static Stream<Arguments> provideMilliseconds() {
        Timestamp ts1 = Timestamp.from(Instant.parse("2023-06-15T08:30:45Z"));
        // 250 ms
        ts1.setNanos(250_000_000);
        Timestamp ts2 = Timestamp.from(Instant.parse("2023-06-15T08:30:45Z"));
        // same instant, different zone
        ts2.setNanos(250_000_000);
        Timestamp ts3 = Timestamp.from(Instant.parse("2023-06-15T08:30:45Z"));
        // exactly 1 ms
        ts3.setNanos(1_000_000);
        return Stream.of(Arguments.of("UTC – 250 ms", ts1, ZoneId.of("UTC"), "2023-06-15T08:30:45.250+00:00"), Arguments.of("Europe/Paris – 250 ms (summer offset +02:00)", ts2, ZoneId.of("Europe/Paris"), "2023-06-15T10:30:45.250+02:00"), Arguments.of("UTC – exactly 1 ms", ts3, ZoneId.of("UTC"), "2023-06-15T08:30:45.001+00:00"));
    }

    private static Stream<Arguments> provideNanoseconds() {
        Timestamp ts1 = Timestamp.from(Instant.parse("2023-03-10T14:20:30Z"));
        ts1.setNanos(123_456_789);
        Timestamp ts2 = Timestamp.from(Instant.parse("2023-03-10T14:20:30Z"));
        ts2.setNanos(123_456_789);
        Timestamp ts3 = Timestamp.from(Instant.parse("2023-12-31T23:59:59Z"));
        ts3.setNanos(1);
        Timestamp ts4 = Timestamp.from(Instant.parse("2023-12-31T23:59:59Z"));
        ts4.setNanos(999_999_999);
        return Stream.of(Arguments.of("UTC – 123456789 ns", ts1, ZoneId.of("UTC"), "2023-03-10T14:20:30.123456789+00:00"), Arguments.of("Asia/Kolkata – 123456789 ns", ts2, ZoneId.of("Asia/Kolkata"), "2023-03-10T19:50:30.123456789+05:30"), Arguments.of("UTC – single nanosecond (leading zeros)", ts3, ZoneId.of("UTC"), "2023-12-31T23:59:59.000000001+00:00"), Arguments.of("UTC – max nanosecond value", ts4, ZoneId.of("UTC"), "2023-12-31T23:59:59.999999999+00:00"));
    }

    @Test
    void toString_WholeSeconds_NoNanos() {
        // GIVEN a Timestamp with zero nanoseconds (whole seconds)
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-02T03:04:05Z"));
        // explicit, though default is 0
        ts.setNanos(0);
        // WHEN converting to String
        String result = TimestampConversions.toString(ts, mockConverter);
        // THEN the result should match the pattern without fractional part
        assertEquals("2023-01-02T03:04:05+00:00", result);
    }

    @Test
    void toString_Milliseconds_ExactMillis() {
        // GIVEN a Timestamp with nanoseconds that are an exact multiple of 1_000_000
        // 123 milliseconds => 123_000_000 nanoseconds
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-02T03:04:05Z"));
        ts.setNanos(123_000_000);
        // WHEN converting to String
        String result = TimestampConversions.toString(ts, mockConverter);
        // THEN the result should contain three‑digit millisecond fraction
        assertEquals("2023-01-02T03:04:05.123+00:00", result);
    }

    @Test
    void toString_Nanoseconds_NonMultipleOfMillis() {
        // GIVEN a Timestamp with nanoseconds that are NOT a multiple of 1_000_000
        // 123,456,789 nanoseconds
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-02T03:04:05Z"));
        ts.setNanos(123_456_789);
        // WHEN converting to String
        String result = TimestampConversions.toString(ts, mockConverter);
        // THEN the result should contain nine‑digit nanosecond fraction
        assertEquals("2023-01-02T03:04:05.123456789+00:00", result);
    }

    @Test
    void toString_DifferentZone_OffsetApplied() {
        // GIVEN a Timestamp with zero nanos and a converter configured for a non‑UTC zone
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        Timestamp ts = Timestamp.from(Instant.parse("2023-06-15T12:30:00Z"));
        ts.setNanos(0);
        // WHEN converting to String
        String result = TimestampConversions.toString(ts, mockConverter);
        // THEN the offset should reflect the zone (EDT = -04:00 at this date)
        assertEquals("2023-06-15T08:30:00-04:00", result);
    }

    // ---------- Whole‑seconds group ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideWholeSeconds")
    void toString_wholeSeconds(String description, Timestamp ts, ZoneId zone, String expected) {
        // configure converter mock for the required zone
        when(mockOptions.getZoneId()).thenReturn(zone);
        String actual = TimestampConversions.toString(ts, mockConverter);
        assertEquals(expected, actual, description);
    }

    // ---------- Milliseconds group ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideMilliseconds")
    void toString_milliseconds(String description, Timestamp ts, ZoneId zone, String expected) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        String actual = TimestampConversions.toString(ts, mockConverter);
        assertEquals(expected, actual, description);
    }

    // ---------- Nanoseconds group ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNanoseconds")
    void toString_nanoseconds(String description, Timestamp ts, ZoneId zone, String expected) {
        when(mockOptions.getZoneId()).thenReturn(zone);
        String actual = TimestampConversions.toString(ts, mockConverter);
        assertEquals(expected, actual, description);
    }

    // ---------- Invalid input group ----------
    @Test
    void toString_invalidInput_notATimestamp() {
        // GIVEN a non‑Timestamp object
        Object notTimestamp = "2023-01-01";
        // WHEN converting – THEN ClassCastException is thrown
        assertThrows(ClassCastException.class, () -> TimestampConversions.toString(notTimestamp, mockConverter));
    }

    @Test
    void toString_invalidInput_nullFrom() {
        // GIVEN a null 'from' argument
        assertThrows(NullPointerException.class, () -> TimestampConversions.toString(null, mockConverter));
    }

    // ---------- Null converter group ----------
    @Test
    void toString_nullConverter() {
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-01T12:00:00Z"));
        assertThrows(NullPointerException.class, () -> TimestampConversions.toString(ts, null));
    }

    // ---------- Converter with null Options ----------
    @Test
    void toString_converterNullOptions() {
        // Mock a converter that returns null Options
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-01T12:00:00Z"));
        assertThrows(NullPointerException.class, () -> TimestampConversions.toString(ts, converterWithNullOptions));
    }

    // ---------- Converter Options with null ZoneId ----------
    @Test
    void toString_converterNullZoneId() {
        // Mock Options that returns null ZoneId
        ConverterOptions optionsWithNullZone = mock(ConverterOptions.class);
        when(optionsWithNullZone.getZoneId()).thenReturn(null);
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(optionsWithNullZone);
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-01T12:00:00Z"));
        assertThrows(NullPointerException.class, () -> TimestampConversions.toString(ts, converter));
    }
}
