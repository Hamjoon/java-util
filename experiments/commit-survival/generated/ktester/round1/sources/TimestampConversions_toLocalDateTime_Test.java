package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toLocalDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");

    /**
     * Minimal Converter implementation for tests.
     */
    private static class SimpleConverter implements Converter {

        private final ConverterOptions options;

        SimpleConverter(ZoneId zone) {
            this.options = new SimpleOptions(zone);
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    /**
     * Minimal ConverterOptions implementation for tests.
     */
    private static class SimpleOptions implements ConverterOptions {

        private final ZoneId zoneId;

        SimpleOptions(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZoneId() {
            return zoneId;
        }
    }

    /**
     * Converter that deliberately returns null from getOptions().
     */
    private static class ConverterWithNullOptions implements Converter {

        @Override
        public ConverterOptions getOptions() {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock converter to return a mock options object,
        // and configure the mock options to return the desired ZoneId.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(TEST_ZONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid interaction leakage.
        reset(mockConverter, mockOptions);
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(Arguments.of("UTC zone – result should equal instant components", Timestamp.from(Instant.parse("2023-03-15T12:30:45Z")), new SimpleConverter(ZoneId.of("UTC")), LocalDateTime.of(2023, 3, 15, 12, 30, 45)), Arguments.of("America/New_York zone – UTC‑4 offset on 2023‑03‑15", Timestamp.from(Instant.parse("2023-03-15T12:30:45Z")), new SimpleConverter(ZoneId.of("America/New_York")), LocalDateTime.of(2023, 3, 15, 8, 30, 45)));
    }

    @Test
    void toLocalDateTime_convertsTimestampUsingProvidedZone() {
        // GIVEN
        // UTC instant
        Instant instant = Instant.parse("2023-01-01T12:00:00Z");
        Timestamp timestamp = Timestamp.from(instant);
        // WHEN
        LocalDateTime result = TimestampConversions.toLocalDateTime(timestamp, mockConverter);
        // THEN
        // 12:00 UTC in America/New_York (UTC‑05:00) is 07:00 local time on the same day.
        LocalDateTime expected = LocalDateTime.of(2023, 1, 1, 7, 0);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_throwsClassCastExceptionWhenNotTimestamp() {
        // GIVEN
        Object notATimestamp = "2023-01-01T12:00:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimestampConversions.toLocalDateTime(notATimestamp, mockConverter));
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toLocalDateTime_successfulConversions(String description, Timestamp timestamp, Converter converter, LocalDateTime expected) {
        // WHEN
        LocalDateTime actual = TimestampConversions.toLocalDateTime(timestamp, converter);
        // THEN
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------------
    @Test
    void toLocalDateTime_nullFrom_throwsNullPointerException() {
        // GIVEN
        Timestamp nullTimestamp = null;
        Converter converter = new SimpleConverter(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toLocalDateTime(nullTimestamp, converter));
    }

    @Test
    void toLocalDateTime_nonTimestampFrom_throwsClassCastException() {
        // GIVEN
        // java.util.Date is not java.sql.Timestamp
        Object notTimestamp = new java.util.Date();
        Converter converter = new SimpleConverter(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimestampConversions.toLocalDateTime(notTimestamp, converter));
    }

    @Test
    void toLocalDateTime_converterWithNullZoneId_throwsNullPointerException() {
        // GIVEN
        Timestamp ts = Timestamp.from(Instant.now());
        // SimpleOptions constructed with null zoneId will cause atZone(null) -> NPE
        Converter converter = new SimpleConverter(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toLocalDateTime(ts, converter));
    }

    @Test
    void toLocalDateTime_nullConverter_throwsNullPointerException() {
        // GIVEN
        Timestamp ts = Timestamp.from(Instant.now());
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toLocalDateTime(ts, nullConverter));
    }

    @Test
    void toLocalDateTime_converterWithNullOptions_throwsNullPointerException() {
        // GIVEN
        Timestamp ts = Timestamp.from(Instant.now());
        Converter converter = new ConverterWithNullOptions();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toLocalDateTime(ts, converter));
    }
}
