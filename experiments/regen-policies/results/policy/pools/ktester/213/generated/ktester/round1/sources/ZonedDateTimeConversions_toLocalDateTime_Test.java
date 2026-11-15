package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.*;
import java.util.stream.Stream;

class ZonedDateTimeConversions_toLocalDateTime_Test {

    // the Converter used in the tests
    private Converter converter;

    // options that hold the target ZoneId
    private ConverterOptions options;

    @BeforeEach
    void setupBeforeEach() {
        // Create fresh options and a Converter for each test so that state does not leak.
        options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; just null out references to help GC.
        converter = null;
        options = null;
    }

    /**
     * Supplies the arguments for the parameterised test.
     *
     * Each {@code Arguments} entry contains:
     * <ul>
     *   <li>the {@code from} object (usually a {@link ZonedDateTime} or {@code null})</li>
     *   <li>a {@link Converter} instance (or {@code null} when the case expects a NPE)</li>
     *   <li>a textual description (used as the display name of the test)</li>
     *   <li>the expected outcome – either a {@link LocalDateTime} or an exception class</li>
     * </ul>
     */
    private static Stream<Arguments> provideTestCases() {
        // Helper to build a Converter with a given ZoneId (or null options)
        java.util.function.Function<ZoneId, Converter> convFactory = zoneId -> {
            if (zoneId == null) {
                // options will be null – we need a Converter whose getOptions() returns null
                ConverterOptions nullOpts = null;
                return new Converter(nullOpts);
            }
            ConverterOptions opts = new ConverterOptions();
            opts.setZoneId(zoneId);
            return new Converter(opts);
        };
        return Stream.of(// 1. Normal conversion: +02:00 -> UTC (shift back 2h)
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneId.of("+02:00")), convFactory.apply(ZoneId.of("UTC")), "Normal conversion: source +02:00, target UTC", LocalDateTime.of(2023, 1, 1, 8, 0)), // 2. Same zone, no change
        Arguments.of(ZonedDateTime.of(2022, 12, 31, 23, 30, 0, 0, ZoneId.of("UTC")), convFactory.apply(ZoneId.of("UTC")), "Converter zone matches source zone; unchanged", LocalDateTime.of(2022, 12, 31, 23, 30)), // 3. Null source -> NPE
        Arguments.of(null, convFactory.apply(ZoneId.of("UTC")), "Null `from` leads to NullPointerException", NullPointerException.class), // 4. Wrong type (String) -> ClassCastException
        Arguments.of("2023-01-01T10:00Z", convFactory.apply(ZoneId.of("UTC")), "`from` is not a ZonedDateTime; expect ClassCastException", ClassCastException.class), // 5. Converter with null ZoneId -> NPE from withZoneSameInstant(null)
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Paris")), convFactory.apply(null), "Converter provides null ZoneId; expect NullPointerException", NullPointerException.class), // 6. Null converter -> NPE
        Arguments.of(ZonedDateTime.now(), null, "Converter itself is null; expect NullPointerException", NullPointerException.class), // 7. Cross‑midnight conversion: 2023‑01‑01 00:30 +02:00 -> UTC = previous day 22:30
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 0, 30, 0, 0, ZoneId.of("+02:00")), convFactory.apply(ZoneId.of("UTC")), "Cross‑midnight conversion, back two hours", LocalDateTime.of(2022, 12, 31, 22, 30)), // 8. DST forward shift (Europe/Paris 2023‑03‑26 01:30 CET -> UTC)
        Arguments.of(ZonedDateTime.of(2023, 3, 26, 1, 30, 0, 0, ZoneId.of("Europe/Paris")), convFactory.apply(ZoneId.of("UTC")), "DST forward shift: subtract one hour", LocalDateTime.of(2023, 3, 26, 0, 30)), // 9. Same UTC offset but different ZoneId (fixed +02:00 vs Etc/GMT-2)
        Arguments.of(ZonedDateTime.of(2023, 6, 15, 12, 0, 0, 0, ZoneId.of("+02:00")), convFactory.apply(ZoneId.of("Etc/GMT-2")), "Same offset, different ZoneId; no shift", LocalDateTime.of(2023, 6, 15, 12, 0)), // 10. Leap‑day handling
        Arguments.of(ZonedDateTime.of(2020, 2, 29, 12, 0, 0, 0, ZoneId.of("+05:00")), convFactory.apply(ZoneId.of("UTC")), "Leap‑day conversion, subtract five hours", LocalDateTime.of(2020, 2, 29, 7, 0)), // 11. Converter zone with minute offset (+05:30)
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneId.of("+02:00")), convFactory.apply(ZoneOffset.ofHoursMinutes(5, 30)), "Converter zone +05:30 adds 3h30m", LocalDateTime.of(2023, 1, 1, 13, 30)), // 12. Cross‑continent conversion New York -> Tokyo
        Arguments.of(ZonedDateTime.of(2023, 8, 20, 15, 45, 0, 0, ZoneId.of("America/New_York")), convFactory.apply(ZoneId.of("Asia/Tokyo")), "New York to Tokyo moves forward 13h, next day", LocalDateTime.of(2023, 8, 21, 4, 45)), // 13. Immutability check (same zone)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 9, 15, 0, 0, ZoneId.of("Europe/London")), convFactory.apply(ZoneId.of("Europe/London")), "Conversion to same zone returns identical LocalDateTime", LocalDateTime.of(2023, 5, 10, 9, 15)), // 14. Converter options null -> NPE
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneId.of("UTC")), // options == null
        new Converter(null), "Converter's options object is null; expect NullPointerException", NullPointerException.class));
    }

    /**
     * Verify that when the source ZonedDateTime is in UTC and the Converter is configured
     * for a different zone, the returned LocalDateTime reflects the same instant in the
     * target zone.
     */
    @Test
    void toLocalDateTime_convertsInstantToTargetZone() {
        // Arrange
        // UTC‑5 on 2022‑01‑01 (no DST)
        ZoneId targetZone = ZoneId.of("America/New_York");
        options.setZoneId(targetZone);
        ZonedDateTime source = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, // 2022‑01‑01T12:00Z
        ZoneId.of("UTC"));
        // Act
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // Assert
        // The same instant in New York is 5 hours earlier: 2022‑01‑01T07:00
        LocalDateTime expected = LocalDateTime.of(2022, 1, 1, 7, 0);
        assertEquals(expected, result, "LocalDateTime should represent the same instant in the target zone");
    }

    /**
     * Verify that when the source ZonedDateTime already uses the Converter's target zone,
     * the method simply returns the source's LocalDateTime.
     */
    @Test
    void toLocalDateTime_sameZoneReturnsOriginalLocalDateTime() {
        // Arrange
        ZoneId targetZone = ZoneId.of("Europe/Paris");
        options.setZoneId(targetZone);
        ZonedDateTime source = ZonedDateTime.of(2023, 6, 15, 14, 30, 0, 0, // already in target zone
        targetZone);
        // Act
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(source, converter);
        // Assert
        // should be unchanged
        LocalDateTime expected = source.toLocalDateTime();
        assertEquals(expected, result, "When source zone equals target zone, the LocalDateTime should be unchanged");
    }

    /**
     * Verify that a null source results in a {@code NullPointerException}
     * (the implementation casts the argument to {@link ZonedDateTime} without a null check).
     */
    @Test
    void toLocalDateTime_nullSourceThrowsException() {
        // Arrange
        options.setZoneId(ZoneId.systemDefault());
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalDateTime(null, converter), "Passing null as the source should throw NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideTestCases")
    void toLocalDateTime_parameterised(Object from, Converter conv, String description, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> ZonedDateTimeConversions.toLocalDateTime(from, conv), description);
        } else {
            // Expected a LocalDateTime result
            LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(from, conv);
            assertEquals(expected, result, description);
        }
    }
}
