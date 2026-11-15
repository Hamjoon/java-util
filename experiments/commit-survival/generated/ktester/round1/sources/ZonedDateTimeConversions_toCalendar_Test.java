package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toCalendar_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
        // Default behaviour: the converter returns the mocked options.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito open mocks to avoid memory leaks.
        mocks.close();
    }

    /**
     * Supplies the success test cases defined in the JSON input.
     */
    private static Stream<Arguments> successCases() {
        return Stream.of(// Case 1 – both source and target zone are UTC
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789, ZoneId.of("UTC")), TimeZone.getTimeZone("UTC"), "Normal conversion when source ZonedDateTime and target zone are both UTC"), // Case 2 – source zone differs from target zone (NY -> LA)
        Arguments.of(ZonedDateTime.of(2023, 12, 25, 9, 0, 0, 0, ZoneId.of("America/New_York")), TimeZone.getTimeZone("America/Los_Angeles"), "Conversion where source ZonedDateTime has a different zone than the target zone supplied by the Converter"));
    }

    @Test
    void toCalendar_returnsCalendarWithSameInstantAndConfiguredTimeZone() {
        // GIVEN
        ZoneId zoneId = ZoneId.of("America/New_York");
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000, zoneId);
        TimeZone expectedTimeZone = TimeZone.getTimeZone(zoneId);
        when(mockOptions.getTimeZone()).thenReturn(expectedTimeZone);
        // WHEN
        Calendar result = ZonedDateTimeConversions.toCalendar(zdt, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(expectedTimeZone, result.getTimeZone(), "Calendar should use the TimeZone supplied by ConverterOptions");
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTimeInMillis(), "Calendar epoch milliseconds should match the ZonedDateTime instant");
    }

    @Test
    void toCalendar_throwsClassCastExceptionWhenInputIsNotZonedDateTime() {
        // GIVEN
        Object notZdt = java.time.LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toCalendar(notZdt, mockConverter), "Providing a non‑ZonedDateTime object should result in ClassCastException");
    }

    @Test
    void toCalendar_throwsNullPointerExceptionWhenInputIsNull() {
        // GIVEN
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(null, mockConverter), "Providing null as the source should result in NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Parameterised success scenarios (group: toCalendar_Success)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("successCases")
    void toCalendar_successfulConversion(Object from, TimeZone targetTimeZone, String description) {
        // GIVEN
        when(mockOptions.getTimeZone()).thenReturn(targetTimeZone);
        // WHEN
        Calendar result = ZonedDateTimeConversions.toCalendar(from, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(targetTimeZone, result.getTimeZone(), "Calendar must use the TimeZone supplied by ConverterOptions");
        // Verify the instant is preserved
        ZonedDateTime zdt = (ZonedDateTime) from;
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTimeInMillis(), "Calendar epoch milliseconds should match the ZonedDateTime instant");
    }

    /* -------------------------------------------------------------
     *  Null converter argument (group: toCalendar_NullConverter)
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_throwsNullPointerExceptionWhenConverterIsNull() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(zdt, null), "Passing null as the Converter should cause NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Null TimeZone in ConverterOptions (group: toCalendar_NullTimeZoneInOptions)
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_throwsNullPointerExceptionWhenTimeZoneInOptionsIsNull() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("UTC"));
        // simulate missing TZ
        when(mockOptions.getTimeZone()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toCalendar(zdt, mockConverter), "When ConverterOptions returns null TimeZone, Calendar.getInstance(null) throws NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Pre‑epoch conversion (group: toCalendar_PreEpoch)
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_handlesPreEpochZonedDateTime() {
        // GIVEN
        ZonedDateTime preEpoch = ZonedDateTime.of(1965, 7, 20, 10, 15, 30, 0, ZoneId.of("UTC"));
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(mockOptions.getTimeZone()).thenReturn(utc);
        // WHEN
        Calendar result = ZonedDateTimeConversions.toCalendar(preEpoch, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(utc, result.getTimeZone(), "Calendar must use UTC");
        assertEquals(preEpoch.toInstant().toEpochMilli(), result.getTimeInMillis(), "Epoch milliseconds should match the pre‑epoch ZonedDateTime");
        // Verify that the value is indeed negative
        assertTrue(result.getTimeInMillis() < 0, "Pre‑epoch epoch millis should be negative");
    }

    /* -------------------------------------------------------------
     *  Time‑zone shift verification (group: toCalendar_TimeZoneShiftVerification)
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_verifiesFieldsAfterTimeZoneShift() {
        // GIVEN
        // 2023‑03‑28 12:00 in Europe/Paris (+01:00)
        ZonedDateTime source = ZonedDateTime.of(2023, 3, 28, 12, 0, 0, 0, ZoneId.of("Europe/Paris"));
        // Target zone America/New_York (‑04:00 at that date)
        TimeZone targetTZ = TimeZone.getTimeZone("America/New_York");
        when(mockOptions.getTimeZone()).thenReturn(targetTZ);
        // WHEN
        Calendar cal = ZonedDateTimeConversions.toCalendar(source, mockConverter);
        // THEN
        assertNotNull(cal, "Resulting Calendar should not be null");
        assertEquals(targetTZ, cal.getTimeZone(), "Calendar must use the target TimeZone");
        // The instant must be preserved
        assertEquals(source.toInstant().toEpochMilli(), cal.getTimeInMillis(), "Instant must be unchanged");
        // Verify local fields reflect New York time (06:00)
        assertEquals(2023, cal.get(Calendar.YEAR), "Year field");
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH), "Month field (zero‑based, March = 2)");
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH), "Day of month field");
        assertEquals(6, cal.get(Calendar.HOUR_OF_DAY), "Hour of day field after zone shift");
        assertEquals(0, cal.get(Calendar.MINUTE), "Minute field");
        assertEquals(0, cal.get(Calendar.SECOND), "Second field");
    }
}
