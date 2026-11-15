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
class TimestampConversions_toLocalDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /* ---------------------------------------------------------------------- */
    private static class ConverterStub implements Converter {

        private final ConverterOptions options;

        private ConverterStub(ConverterOptions options) {
            this.options = options;
        }

        static ConverterStub withZone(String zoneId) {
            ZoneId zid = zoneId == null ? null : ZoneId.of(zoneId);
            ConverterOptions opts = mock(ConverterOptions.class);
            when(opts.getZoneId()).thenReturn(zid);
            return new ConverterStub(opts);
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
        // The remaining methods of Converter are irrelevant for these tests.
        // They can stay unimplemented (default methods) or throw UnsupportedOperationException.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the mocks for each test.
        // MockitoExtension already creates the mocks, we only need to define their behaviour here.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test interference.
        clearInvocations(mockConverter, mockOptions);
    }

    /* ---------------------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Valid Timestamp conversion – UTC zone
        Arguments.of(Timestamp.valueOf("2023-03-15 10:30:00"), ConverterStub.withZone("UTC"), LocalDateTime.of(2023, 3, 15, 10, 30, 0)), // Timestamp conversion – non-UTC zone with date shift
        Arguments.of(Timestamp.valueOf("2023-03-15 00:30:00"), ConverterStub.withZone("America/New_York"), LocalDateTime.of(2023, 3, 14, 20, 30, 0)), // Timestamp conversion – nanosecond precision
        Arguments.of(Timestamp.valueOf("2023-03-15 10:30:00.123456789"), ConverterStub.withZone("UTC"), LocalDateTime.of(2023, 3, 15, 10, 30, 0, 123_456_789)), // Epoch timestamp with Asia/Tokyo zone
        Arguments.of(Timestamp.valueOf("1970-01-01 00:00:00"), ConverterStub.withZone("Asia/Tokyo"), LocalDateTime.of(1970, 1, 1, 9, 0, 0)), // Far future timestamp with UTC zone
        Arguments.of(Timestamp.valueOf("9999-12-31 23:59:59"), ConverterStub.withZone("UTC"), LocalDateTime.of(9999, 12, 31, 23, 59, 59)));
    }

    @Test
    void toLocalDateTime_usesConverterZoneId_Utc() {
        // GIVEN
        ZoneId utc = ZoneId.of("UTC");
        when(mockOptions.getZoneId()).thenReturn(utc);
        Timestamp ts = Timestamp.valueOf("2023-08-15 12:34:56.123456789");
        LocalDateTime expected = ts.toInstant().atZone(utc).toLocalDateTime();
        // WHEN
        LocalDateTime result = TimestampConversions.toLocalDateTime(ts, mockConverter);
        // THEN
        assertEquals(expected, result, "The converted LocalDateTime should match the expected value in UTC");
    }

    @Test
    void toLocalDateTime_usesConverterZoneId_NewYork() {
        // GIVEN
        ZoneId newYork = ZoneId.of("America/New_York");
        when(mockOptions.getZoneId()).thenReturn(newYork);
        Timestamp ts = Timestamp.valueOf("2023-08-15 12:34:56.123456789");
        LocalDateTime expected = ts.toInstant().atZone(newYork).toLocalDateTime();
        // WHEN
        LocalDateTime result = TimestampConversions.toLocalDateTime(ts, mockConverter);
        // THEN
        assertEquals(expected, result, "The converted LocalDateTime should respect the America/New_York zone supplied by ConverterOptions");
    }

    @Test
    void toLocalDateTime_nullTimestamp_throwsNullPointerException() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toLocalDateTime(null, mockConverter), "Passing a null Timestamp should result in a NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {2} using zone {1}")
    @MethodSource("validConversionProvider")
    void toLocalDateTime_validConversions(Timestamp from, Converter converter, LocalDateTime expected) {
        // WHEN
        LocalDateTime result = TimestampConversions.toLocalDateTime(from, converter);
        // THEN
        assertEquals(expected, result, () -> String.format("Conversion of %s with zone %s should yield %s", from, ((ConverterStub) converter).getOptions().getZoneId(), expected));
    }

    @Test
    void toLocalDateTime_invalidSourceType_throwsClassCastException() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        Object notATimestamp = new java.util.Date();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimestampConversions.toLocalDateTime(notATimestamp, mockConverter), "Providing a non‑Timestamp source should throw ClassCastException");
    }

    @Test
    void toLocalDateTime_converterWithNullZoneId_throwsNullPointerException() {
        // GIVEN
        Converter converterWithNullZone = ConverterStub.withZone(null);
        Timestamp ts = Timestamp.valueOf("2023-03-15 10:30:00");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toLocalDateTime(ts, converterWithNullZone), "A null ZoneId in ConverterOptions should cause NPE when atZone(null) is invoked");
    }

    @Test
    void toLocalDateTime_nullConverterArgument_throwsNullPointerException() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:00:00");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toLocalDateTime(ts, null), "Passing a null Converter should cause NPE when getOptions() is called");
    }
}
