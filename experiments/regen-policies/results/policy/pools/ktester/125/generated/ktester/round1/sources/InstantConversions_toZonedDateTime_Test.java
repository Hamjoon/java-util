package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toZonedDateTime_Test {

    private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Build a Converter whose options contain the test ZoneId.
        ConverterOptions options = new ConverterOptions();
        // configure the zone used by conversions
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    /**
     * Creates a mock {@link Converter} whose {@code getOptions().getZoneId()} returns the supplied {@code zoneId}.
     */
    private static Converter mockConverterWithZone(ZoneId zoneId) {
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getZoneId()).thenReturn(zoneId);
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(options);
        return converter;
    }

    /**
     * Creates a mock {@link Converter} whose {@code getOptions()} returns {@code null}.
     */
    private static Converter mockConverterNullOptions() {
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(null);
        return converter;
    }

    /**
     * Creates a mock {@link Converter} whose {@code getOptions().getZoneId()} returns {@code null}.
     */
    private static Converter mockConverterNullZone() {
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getZoneId()).thenReturn(null);
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(options);
        return converter;
    }

    /**
     * Creates a mock {@link Converter} whose {@code getOptions().getZoneId()} throws the supplied exception.
     */
    private static Converter mockConverterThrowingOnZoneId(RuntimeException ex) {
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getZoneId()).thenThrow(ex);
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(options);
        return converter;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// UTC zone
        Arguments.of(Instant.ofEpochSecond(0), mockConverterWithZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.of("UTC")), "UTC zone – epoch instant"), // America/New_York zone
        Arguments.of(Instant.parse("2023-03-15T12:30:45.123Z"), mockConverterWithZone(ZoneId.of("America/New_York")), ZonedDateTime.ofInstant(Instant.parse("2023-03-15T12:30:45.123Z"), ZoneId.of("America/New_York")), "America/New_York zone – typical instant"), // Asia/Kolkata (+05:30)
        Arguments.of(// 2020‑09‑13T12:26:40.500Z
        Instant.ofEpochSecond(1_600_000_000L, 500_000_000L), mockConverterWithZone(ZoneId.of("Asia/Kolkata")), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_600_000_000L, 500_000_000L), ZoneId.of("Asia/Kolkata")), "Asia/Kolkata zone – nanosecond precision"), // Negative epoch (pre‑1970)
        Arguments.of(Instant.ofEpochSecond(-1), mockConverterWithZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.ofEpochSecond(-1), ZoneId.of("UTC")), "UTC zone – negative epoch"), // Instant with nanosecond precision (UTC)
        Arguments.of(Instant.ofEpochSecond(1_622_547_800L, 123_456_789L), mockConverterWithZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_622_547_800L, 123_456_789L), ZoneId.of("UTC")), "UTC zone – sub‑second precision"), // Fixed offset zone (+02:00)
        Arguments.of(Instant.parse("2022-01-01T00:00:00Z"), mockConverterWithZone(ZoneOffset.ofHours(2)), ZonedDateTime.ofInstant(Instant.parse("2022-01-01T00:00:00Z"), ZoneOffset.ofHours(2)), "ZoneOffset +02:00"), // DST transition instant (America/New_York)
        Arguments.of(Instant.parse("2021-03-14T07:00:00Z"), mockConverterWithZone(ZoneId.of("America/New_York")), ZonedDateTime.ofInstant(Instant.parse("2021-03-14T07:00:00Z"), ZoneId.of("America/New_York")), "DST start instant – New York"), // Instant.MAX (UTC)
        Arguments.of(Instant.MAX, mockConverterWithZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.MAX, ZoneId.of("UTC")), "UTC zone – Instant.MAX"), // Instant.MIN (UTC)
        Arguments.of(Instant.MIN, mockConverterWithZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.MIN, ZoneId.of("UTC")), "UTC zone – Instant.MIN"));
    }

    @Test
    void testToZonedDateTime_InstantConverted() {
        // GIVEN
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        // WHEN
        ZonedDateTime result = InstantConversions.toZonedDateTime(instant, converter);
        // THEN
        ZonedDateTime expected = instant.atZone(TEST_ZONE);
        assertEquals(expected, result, "The ZonedDateTime should be the Instant at the configured zone");
    }

    @Test
    void testToZonedDateTime_NullInstantThrowsNPE() {
        // GIVEN
        Instant nullInstant = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toZonedDateTime(nullInstant, converter), "Passing a null Instant should result in a NullPointerException");
    }

    @Test
    void testToZonedDateTime_WrongSourceTypeThrowsClassCastException() {
        // GIVEN
        String notAnInstant = "2023-01-01T12:34:56Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> InstantConversions.toZonedDateTime(notAnInstant, converter), "Passing a non‑Instant source should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void testToZonedDateTime_ValidConversions(Instant from, Converter converter, ZonedDateTime expected, String description) {
        // WHEN
        ZonedDateTime result = InstantConversions.toZonedDateTime(from, converter);
        // THEN
        assertEquals(expected, result, () -> "Conversion failed for case: " + description);
    }

    @Test
    void testToZonedDateTime_NullConverterThrowsNPE() {
        Instant instant = Instant.now();
        assertThrows(NullPointerException.class, () -> InstantConversions.toZonedDateTime(instant, null), "Passing a null Converter should throw NullPointerException");
    }

    @Test
    void testToZonedDateTime_IncompatibleSourceTypeThrowsClassCastException() {
        Converter converter = mockConverterWithZone(ZoneId.of("UTC"));
        String notAnInstant = "2023-01-01T00:00:00Z";
        assertThrows(ClassCastException.class, () -> InstantConversions.toZonedDateTime(notAnInstant, converter), "Providing a non‑Instant source should cause ClassCastException");
    }

    @Test
    void testToZonedDateTime_ConverterReturnsNullZoneIdThrowsNPE() {
        Converter converter = mockConverterNullZone();
        Instant instant = Instant.now();
        assertThrows(NullPointerException.class, () -> InstantConversions.toZonedDateTime(instant, converter), "Converter returning null ZoneId should cause NullPointerException");
    }

    @Test
    void testToZonedDateTime_ConverterOptionsNullThrowsNPE() {
        Converter converter = mockConverterNullOptions();
        Instant instant = Instant.now();
        assertThrows(NullPointerException.class, () -> InstantConversions.toZonedDateTime(instant, converter), "Converter with null options should cause NullPointerException");
    }

    @Test
    void testToZonedDateTime_ConverterZoneIdThrowsRuntimeException() {
        RuntimeException ex = new RuntimeException("zoneId failure");
        Converter converter = mockConverterThrowingOnZoneId(ex);
        Instant instant = Instant.now();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> InstantConversions.toZonedDateTime(instant, converter), "Exception from getZoneId() should be propagated");
        assertSame(ex, thrown, "Propagated exception should be the original one");
    }

    @Test
    void testToZonedDateTime_SourceInstantRemainsUnchanged() {
        Instant source = Instant.parse("2023-05-01T12:00:00Z");
        Converter converter = mockConverterWithZone(ZoneId.of("UTC"));
        // Capture the original reference/value
        Instant before = source;
        // Perform conversion
        InstantConversions.toZonedDateTime(source, converter);
        // Verify that the original Instant is still equal to the captured value
        assertEquals(before, source, "The source Instant must remain unchanged after conversion");
    }
}
