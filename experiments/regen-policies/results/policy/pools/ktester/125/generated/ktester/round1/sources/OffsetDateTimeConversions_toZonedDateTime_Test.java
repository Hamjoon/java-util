package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toZonedDateTime_Test {

    /**
     * Shared {@link Converter} used by all test methods.
     */
    private static Converter converter;

    /**
     * The {@link ZoneId} that the {@link Converter} will apply.
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");

    /**
     * Shared {@link Converter} used by the simple tests.
     */
    private static Converter sharedConverter;

    /**
     * Fixed zone used by the shared converter.
     */
    private static final ZoneId SHARED_ZONE = ZoneId.of("America/New_York");

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with a ConverterOptions that specifies the test zone.
        ConverterOptions options = new ConverterOptions();
        // The real API provides a setter for the zone; if the name differs, adjust accordingly.
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /**
     * Creates a mock {@link Converter} whose {@link ConverterOptions#getZoneId()} returns the supplied zone.
     */
    private static Converter mockConverterWithZone(ZoneId zone) {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(zone);
        return converter;
    }

    /**
     * Creates a mock {@link Converter} whose {@link Converter#getOptions()} returns {@code null}.
     */
    private static Converter mockConverterWithNullOptions() {
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(null);
        return converter;
    }

    /**
     * Creates a mock {@link Converter} whose {@link ConverterOptions#getZoneId()} returns {@code null}.
     */
    private static Converter mockConverterWithNullZoneId() {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(null);
        return converter;
    }

    /**
     * Creates a mock {@link Converter} whose {@link Converter#getOptions()} throws the supplied exception.
     */
    private static Converter mockConverterThrowingOnGetOptions(RuntimeException ex) {
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenThrow(ex);
        return converter;
    }

    /**
     * Creates a mock {@link Converter} whose {@link ConverterOptions#getZoneId()} throws the supplied exception.
     */
    private static Converter mockConverterThrowingOnGetZoneId(RuntimeException ex) {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenThrow(ex);
        return converter;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Group: Valid conversions
        Arguments.of("Convert an OffsetDateTime with +02:00 offset to UTC zone; instant adjusted correctly.", OffsetDateTime.parse("2023-03-25T10:15:30+02:00"), ZoneId.of("UTC"), ZonedDateTime.parse("2023-03-25T08:15:30Z[UTC]")), Arguments.of("Convert an OffsetDateTime at -05:00 to the same America/New_York zone; local time unchanged.", OffsetDateTime.parse("2022-12-31T23:00:00-05:00"), ZoneId.of("America/New_York"), ZonedDateTime.parse("2023-01-01T00:00:00-05:00[America/New_York]")), // Group: Valid conversions - various offsets and zones
        Arguments.of("Convert +05:30 offset to Asia/Tokyo (+09:00); instant shifted forward 3.5h.", OffsetDateTime.parse("2023-06-15T12:00:00+05:30"), ZoneId.of("Asia/Tokyo"), ZonedDateTime.parse("2023-06-15T15:30:00+09:00[Asia/Tokyo]")), Arguments.of("Convert -04:00 offset to Europe/London (UTC at that date); instant adjusted +4h.", OffsetDateTime.parse("2023-12-01T08:45:00-04:00"), ZoneId.of("Europe/London"), ZonedDateTime.parse("2023-12-01T13:45:00Z[Europe/London]")), // Group: Valid conversions - DST transition
        Arguments.of("Just before end of European Summer Time; target zone switches to +01:00.", OffsetDateTime.parse("2023-10-29T01:30:00+02:00"), ZoneId.of("Europe/Paris"), ZonedDateTime.parse("2023-10-29T00:30:00+01:00[Europe/Paris]")), Arguments.of("Just before start of European Summer Time; target zone jumps from +00 to +01.", OffsetDateTime.parse("2023-03-26T01:30:00+00:00"), ZoneId.of("Europe/Paris"), ZonedDateTime.parse("2023-03-26T02:30:00+01:00[Europe/Paris]")), // Group: Valid conversions - extreme offsets
        Arguments.of("Maximum positive offset (+14:00) to UTC; instant moves back 14h crossing previous day.", OffsetDateTime.parse("2023-01-01T10:00:00+14:00"), ZoneId.of("UTC"), ZonedDateTime.parse("2022-12-31T20:00:00Z[UTC]")), Arguments.of("Maximum negative offset (-12:00) to Pacific/Honolulu (-10:00); instant shifts forward 2h.", OffsetDateTime.parse("2023-01-01T10:00:00-12:00"), ZoneId.of("Pacific/Honolulu"), ZonedDateTime.parse("2023-01-02T12:00:00-10:00[Pacific/Honolulu]")), // Group: Valid conversions - fractional minute offset
        Arguments.of("Offset +05:45 (e.g., Nepal) to UTC; instant moved back 5h45m.", OffsetDateTime.parse("2023-04-01T12:00:00+05:45"), ZoneId.of("UTC"), ZonedDateTime.parse("2023-04-01T06:15:00Z[UTC]")), Arguments.of("Offset -03:30 to America/New_York (DST -04:00); instant adjusted accordingly.", OffsetDateTime.parse("2023-07-15T23:15:00-03:30"), ZoneId.of("America/New_York"), ZonedDateTime.parse("2023-07-16T01:45:00-04:00[America/New_York]")), // Group: Valid conversions - same instant across zones
        Arguments.of("UTC OffsetDateTime to Pacific Standard Time; local time 8h earlier previous day.", OffsetDateTime.parse("2023-03-01T00:00:00Z"), ZoneId.of("America/Los_Angeles"), ZonedDateTime.parse("2023-02-28T16:00:00-08:00[America/Los_Angeles]")));
    }

    @Test
    void toZonedDateTime_convertsOffsetDateTimeUsingConverterZone() {
        // GIVEN: an OffsetDateTime in UTC.
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 10, 12, 30, 15, 0, java.time.ZoneOffset.UTC);
        // WHEN: the conversion method is invoked.
        ZonedDateTime result = OffsetDateTimeConversions.toZonedDateTime(source, converter);
        // THEN: the instant must be identical and the zone must be the one from the converter.
        ZonedDateTime expected = source.toInstant().atZone(TEST_ZONE);
        assertEquals(expected, result, "The ZonedDateTime should have the same instant as the source and the zone from ConverterOptions");
    }

    @Test
    void toZonedDateTime_nullSource_throwsNullPointerException() {
        // GIVEN: a null source object.
        Object source = null;
        // WHEN / THEN: a NullPointerException is expected because the method casts directly.
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(source, converter), "Calling toZonedDateTime with a null source should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toZonedDateTime_validConversions(String description, OffsetDateTime from, ZoneId targetZone, ZonedDateTime expected) {
        Converter converter = mockConverterWithZone(targetZone);
        ZonedDateTime actual = OffsetDateTimeConversions.toZonedDateTime(from, converter);
        assertEquals(expected, actual, description);
    }

    @Test
    void toZonedDateTime_nullConverter_throwsNullPointerException() {
        OffsetDateTime source = OffsetDateTime.now();
        Converter nullConverter = null;
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(source, nullConverter), "Passing a null Converter should result in NullPointerException");
    }

    @Test
    void toZonedDateTime_converterWithNullOptions_throwsNullPointerException() {
        OffsetDateTime source = OffsetDateTime.now();
        Converter converter = mockConverterWithNullOptions();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(source, converter), "Converter with null options should cause NPE when retrieving the ZoneId");
    }

    @Test
    void toZonedDateTime_converterWithNullZoneId_throwsNullPointerException() {
        OffsetDateTime source = OffsetDateTime.now();
        Converter converter = mockConverterWithNullZoneId();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(source, converter), "Converter whose options return a null ZoneId should cause NPE during atZone(null)");
    }

    @Test
    void toZonedDateTime_wrongSourceType_throwsClassCastException() {
        // not an OffsetDateTime
        Object source = "2023-01-01";
        Converter converter = mockConverterWithZone(ZoneId.of("UTC"));
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toZonedDateTime(source, converter), "Passing a non‑OffsetDateTime source should result in ClassCastException");
    }

    @Test
    void toZonedDateTime_converterGetOptionsThrows_propagatesException() {
        OffsetDateTime source = OffsetDateTime.parse("2023-03-25T10:15:30+02:00");
        Converter converter = mockConverterThrowingOnGetOptions(new IllegalStateException("options failure"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> OffsetDateTimeConversions.toZonedDateTime(source, converter), "Exception from Converter#getOptions() should be propagated");
        assertEquals("options failure", ex.getMessage());
    }

    @Test
    void toZonedDateTime_converterGetZoneIdThrows_propagatesException() {
        OffsetDateTime source = OffsetDateTime.parse("2023-03-25T10:15:30+02:00");
        Converter converter = mockConverterThrowingOnGetZoneId(new RuntimeException("zoneId failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> OffsetDateTimeConversions.toZonedDateTime(source, converter), "Exception from ConverterOptions#getZoneId() should be propagated");
        assertEquals("zoneId failure", ex.getMessage());
    }
}
