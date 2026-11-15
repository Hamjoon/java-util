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
class TimestampConversions_toOffsetDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
        // Default behaviour: UTC zone – individual tests can override.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Reset interactions and close the mock context.
        reset(mockConverter, mockOptions);
        mocks.close();
    }

    /* -------------------------------------------------------------
     *  Parameterized test for successful conversions (valid `from`).
     * ------------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// UTC zone (default mock configuration)
        Arguments.of(Timestamp.valueOf("2022-01-01 12:00:00.0"), ZoneId.of("UTC"), "2022-01-01T12:00:00Z"), // Non‑UTC zone
        Arguments.of(Timestamp.valueOf("2022-06-15 08:15:30.0"), ZoneId.of("America/New_York"), // The expected string is the ISO‑8601 representation after applying the zone.
        // 2022‑06‑15 08:15:30 in New York is 2022‑06‑15T12:15:30Z (offset -04:00)
        "2022-06-15T12:15:30-04:00"));
    }

    @Test
    void toOffsetDateTime_usesConverterZone_Utc() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2023-01-02 15:30:45.123456789");
        // The mock is already configured to return UTC.
        OffsetDateTime expected = ts.toInstant().atZone(ZoneId.of("UTC")).toOffsetDateTime();
        // WHEN
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, mockConverter);
        // THEN
        assertEquals(expected, actual, "OffsetDateTime should be calculated using UTC zone from ConverterOptions");
    }

    @Test
    void toOffsetDateTime_usesConverterZone_NonUtc() {
        // GIVEN
        ZoneId zone = ZoneId.of("America/New_York");
        when(mockOptions.getZoneId()).thenReturn(zone);
        Timestamp ts = Timestamp.valueOf("2023-06-15 08:15:30.000000000");
        OffsetDateTime expected = ts.toInstant().atZone(zone).toOffsetDateTime();
        // WHEN
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, mockConverter);
        // THEN
        assertEquals(expected, actual, "OffsetDateTime should be calculated using the zone supplied by ConverterOptions");
    }

    @Test
    void toOffsetDateTime_nullTimestamp_returnsNull() {
        // GIVEN
        Timestamp ts = null;
        // WHEN
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, mockConverter);
        // THEN
        assertNull(actual, "Passing a null Timestamp should result in a null OffsetDateTime");
    }

    @ParameterizedTest(name = "[{index}] Timestamp {0} with zone {1}")
    @MethodSource("validConversionProvider")
    void toOffsetDateTime_successfulConversion(Timestamp ts, ZoneId zone, String expectedIso) {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(zone);
        OffsetDateTime expected = ts.toInstant().atZone(zone).toOffsetDateTime();
        // WHEN
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, mockConverter);
        // THEN
        assertEquals(expected, actual, () -> "Converted OffsetDateTime should match expected value for zone " + zone);
        assertEquals(expectedIso, actual.toString(), "ISO‑8601 string representation should match the expected format");
    }

    /* -------------------------------------------------------------
     *  Null `from` argument – should throw NullPointerException.
     * ------------------------------------------------------------- */
    @Test
    void toOffsetDateTime_nullFrom_throwsNullPointerException() {
        // GIVEN
        Timestamp nullTs = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toOffsetDateTime(nullTs, mockConverter), "Passing a null Timestamp should result in NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Invalid `from` type – should throw ClassCastException.
     * ------------------------------------------------------------- */
    @Test
    void toOffsetDateTime_invalidFromType_throwsClassCastException() {
        // GIVEN
        String notATimestamp = "2022-01-01T12:00:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimestampConversions.toOffsetDateTime(notATimestamp, mockConverter), "Passing a non‑Timestamp object should raise ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Null `converter` argument – should throw NullPointerException.
     * ------------------------------------------------------------- */
    @Test
    void toOffsetDateTime_nullConverter_throwsNullPointerException() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2022-01-01 12:00:00.0");
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toOffsetDateTime(ts, nullConverter), "Passing a null Converter should result in NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Null `converter.getOptions()` – should throw NullPointerException.
     * ------------------------------------------------------------- */
    @Test
    void toOffsetDateTime_nullConverterOptions_throwsNullPointerException() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2022-01-01 12:00:00.0");
        // simulate missing options
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toOffsetDateTime(ts, mockConverter), "When Converter#getOptions() returns null, a NullPointerException is expected");
    }

    /* -------------------------------------------------------------
     *  Null `converter.getOptions().getZoneId()` – should throw NullPointerException.
     * ------------------------------------------------------------- */
    @Test
    void toOffsetDateTime_nullZoneId_throwsNullPointerException() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2022-01-01 12:00:00.0");
        // simulate missing zone
        when(mockOptions.getZoneId()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toOffsetDateTime(ts, mockConverter), "When ConverterOptions#getZoneId() returns null, a NullPointerException is expected");
    }
}
