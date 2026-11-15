package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toZonedDateTime_Test {

    @Mock
    private Converter // the Converter dependency
    converter;

    @Mock
    private ConverterOptions // the options returned by the Converter
    options;

    /**
     * Minimal concrete implementation of {@link Converter} used for the
     * “real‑object” test cases.  Only {@link #getOptions()} is needed.
     */
    private static class TestConverter implements Converter {

        private final ConverterOptions options;

        TestConverter(ZoneId zoneId) {
            this.options = new ConverterOptions() {

                @Override
                public ZoneId getZoneId() {
                    return zoneId;
                }
            };
        }

        TestConverter(ZoneId zoneId, boolean nullOptions) {
            this.options = nullOptions ? null : new ConverterOptions() {

                @Override
                public ZoneId getZoneId() {
                    return zoneId;
                }
            };
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: converter -> options -> zoneId
        when(converter.getOptions()).thenReturn(options);
        // Default deterministic ZoneId – individual tests may override this
        when(options.getZoneId()).thenReturn(ZoneId.of("Europe/Paris"));
    }

    /**
     * Valid conversions – normal zones
     */
    static Stream<Arguments> validConversions() {
        return Stream.of(// Convert a valid OffsetDateTime with a UTC ZoneId
        Arguments.of(OffsetDateTime.parse("2023-03-25T10:15:30+01:00"), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.parse("2023-03-25T09:15:30Z"), ZoneId.of("UTC"))), // Convert a valid OffsetDateTime with a non‑UTC ZoneId (New York)
        Arguments.of(OffsetDateTime.parse("2022-12-31T23:00:00-05:00"), ZoneId.of("America/New_York"), ZonedDateTime.ofInstant(Instant.parse("2023-01-01T04:00:00Z"), ZoneId.of("America/New_York"))));
    }

    /**
     * Edge‑zone and offset scenarios
     */
    static Stream<Arguments> edgeZoneConversions() {
        return Stream.of(// Max positive offset (+14:00) to a zone with same offset (Kiritimati)
        Arguments.of(OffsetDateTime.parse("2023-06-30T23:59:59+14:00"), ZoneId.of("Pacific/Kiritimati"), ZonedDateTime.ofInstant(Instant.parse("2023-06-30T09:59:59Z"), ZoneId.of("Pacific/Kiritimati"))), // Max negative offset (-12:00) to a zone with same offset (Etc/GMT+12)
        Arguments.of(OffsetDateTime.parse("2023-06-30T00:00:00-12:00"), ZoneId.of("Etc/GMT+12"), ZonedDateTime.ofInstant(Instant.parse("2023-06-30T12:00:00Z"), ZoneId.of("Etc/GMT+12"))), // Leap‑day conversion
        Arguments.of(OffsetDateTime.parse("2020-02-29T12:00:00+00:00"), ZoneId.of("Europe/Paris"), ZonedDateTime.ofInstant(Instant.parse("2020-02-29T12:00:00Z"), ZoneId.of("Europe/Paris"))), // Epoch instant conversion
        Arguments.of(OffsetDateTime.parse("1970-01-01T00:00:00Z"), ZoneId.of("America/Los_Angeles"), ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("America/Los_Angeles"))));
    }

    @Test
    void toZonedDateTime_ShouldApplyConverterZoneId() {
        // GIVEN: an OffsetDateTime in UTC
        OffsetDateTime offsetDateTime = OffsetDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        12, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        123_456_789, ZoneOffset.UTC);
        // WHEN: converting using the focal method
        ZonedDateTime result = OffsetDateTimeConversions.toZonedDateTime(offsetDateTime, converter);
        // THEN: the instant must be preserved
        assertEquals(offsetDateTime.toInstant(), result.toInstant(), "The instant of the resulting ZonedDateTime must match the source OffsetDateTime");
        // AND: the zone must be the one supplied by ConverterOptions
        assertEquals(ZoneId.of("Europe/Paris"), result.getZone(), "The zone of the resulting ZonedDateTime must be taken from ConverterOptions");
    }

    @Test
    void toZonedDateTime_NullInput_ShouldThrowNullPointerException() {
        // GIVEN: a null source object
        Object nullSource = null;
        // WHEN / THEN: a NullPointerException is expected (the method casts to OffsetDateTime)
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(nullSource, converter), "Calling toZonedDateTime with a null source should throw NPE");
    }

    @Test
    void toZonedDateTime_InvalidType_ShouldThrowClassCastException() {
        // GIVEN: an object that is NOT an OffsetDateTime
        Object notAnOffsetDateTime = "I am not an OffsetDateTime";
        // WHEN / THEN: a ClassCastException is expected because of the cast inside the method
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toZonedDateTime(notAnOffsetDateTime, converter), "Calling toZonedDateTime with an incompatible type should throw CCE");
    }

    @ParameterizedTest(name = "[{index}] from={0} zone={1}")
    @MethodSource("validConversions")
    void toZonedDateTime_ValidConversions(OffsetDateTime from, ZoneId targetZone, ZonedDateTime expected) {
        // Arrange – use a real TestConverter so that getOptions() returns a real object
        Converter realConverter = new TestConverter(targetZone);
        // Act
        ZonedDateTime result = OffsetDateTimeConversions.toZonedDateTime(from, realConverter);
        // Assert – the instant must be identical and the zone must be the target zone
        assertEquals(expected.toInstant(), result.toInstant(), "Instant should be preserved after conversion");
        assertEquals(expected.getZone(), result.getZone(), "Resulting zone must match the ConverterOptions zone");
    }

    @ParameterizedTest(name = "[{index}] edge case from={0} zone={1}")
    @MethodSource("edgeZoneConversions")
    void toZonedDateTime_EdgeZones(OffsetDateTime from, ZoneId targetZone, ZonedDateTime expected) {
        Converter realConverter = new TestConverter(targetZone);
        ZonedDateTime result = OffsetDateTimeConversions.toZonedDateTime(from, realConverter);
        assertEquals(expected.toInstant(), result.toInstant(), "Instant must be preserved for edge‑zone conversion");
        assertEquals(expected.getZone(), result.getZone(), "Zone must be the one supplied by the ConverterOptions");
    }

    /**
     * Invalid inputs – null source or wrong type
     */
    @Test
    void toZonedDateTime_NullSource_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(null, converter), "Passing null as the source should throw NullPointerException");
    }

    @Test
    void toZonedDateTime_WrongType_ThrowsClassCastException() {
        Object notOffsetDateTime = "2023-01-01";
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toZonedDateTime(notOffsetDateTime, converter), "Passing a non‑OffsetDateTime object should throw ClassCastException");
    }

    /**
     * Invalid inputs – problems with the Converter hierarchy
     */
    @Test
    void toZonedDateTime_NullConverter_ThrowsNPE() {
        OffsetDateTime src = OffsetDateTime.now();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(src, null), "A null Converter should cause a NullPointerException when accessed");
    }

    @Test
    void toZonedDateTime_NullOptions_ThrowsNPE() {
        // Converter returns null Options
        when(converter.getOptions()).thenReturn(null);
        OffsetDateTime src = OffsetDateTime.now();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(src, converter), "Null Options from the Converter should cause NPE");
    }

    @Test
    void toZonedDateTime_NullZoneId_ThrowsNPE() {
        // Options returns null ZoneId
        when(options.getZoneId()).thenReturn(null);
        OffsetDateTime src = OffsetDateTime.now();
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(src, converter), "Null ZoneId from Options should cause NPE while building ZonedDateTime");
    }

    /**
     * Invalid inputs – Converter methods themselves throw exceptions
     */
    @Test
    void toZonedDateTime_ConverterGetOptionsThrows_PropagatesException() {
        Converter badConverter = new Converter() {

            @Override
            public ConverterOptions getOptions() {
                throw new NullPointerException("options NPE");
            }
        };
        OffsetDateTime src = OffsetDateTime.now();
        NullPointerException ex = assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(src, badConverter));
        assertEquals("options NPE", ex.getMessage());
    }

    @Test
    void toZonedDateTime_OptionsGetZoneIdThrows_PropagatesException() {
        ConverterOptions badOptions = new ConverterOptions() {

            @Override
            public ZoneId getZoneId() {
                throw new IllegalStateException("zoneId error");
            }
        };
        Converter badConverter = new Converter() {

            @Override
            public ConverterOptions getOptions() {
                return badOptions;
            }
        };
        OffsetDateTime src = OffsetDateTime.now();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> OffsetDateTimeConversions.toZonedDateTime(src, badConverter));
        assertEquals("zoneId error", ex.getMessage());
    }
}
