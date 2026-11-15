package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toZonedDateTime_Test {

    // shared across all tests
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options.
        // The default options use the JVM's default ZoneId, which is fine for the assertions below.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    @Test
    void toZonedDateTime_fromInstant_returnsSameInstantAtDefaultZone() {
        // GIVEN
        Instant instant = Instant.parse("2022-03-04T12:34:56Z");
        ZonedDateTime expected = instant.atZone(ZoneId.systemDefault());
        // WHEN
        ZonedDateTime actual = BigIntegerConversions.toZonedDateTime(instant, converter);
        // THEN
        assertEquals(expected, actual, "ZonedDateTime derived from an Instant should match the expected value");
    }

    @Test
    void toZonedDateTime_fromLongEpochSeconds_returnsCorrectZonedDateTime() {
        // GIVEN
        // 2022-01-01T00:00:00Z
        long epochSeconds = 1_640_995_200L;
        Instant instant = Instant.ofEpochSecond(epochSeconds);
        ZonedDateTime expected = instant.atZone(ZoneId.systemDefault());
        // WHEN
        ZonedDateTime actual = BigIntegerConversions.toZonedDateTime(epochSeconds, converter);
        // THEN
        assertEquals(expected, actual, "ZonedDateTime derived from a Long epoch‑seconds value should be correct");
    }

    @Test
    void toZonedDateTime_fromBigIntegerEpochSeconds_returnsCorrectZonedDateTime() {
        // GIVEN
        // 2022-01-01T00:00:00Z
        BigInteger epochSeconds = new BigInteger("1640995200");
        Instant instant = Instant.ofEpochSecond(epochSeconds.longValueExact());
        ZonedDateTime expected = instant.atZone(ZoneId.systemDefault());
        // WHEN
        ZonedDateTime actual = BigIntegerConversions.toZonedDateTime(epochSeconds, converter);
        // THEN
        assertEquals(expected, actual, "ZonedDateTime derived from a BigInteger epoch‑seconds value should be correct");
    }

    @Test
    void testToZonedDateTime_NullConverter() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toZonedDateTime(now, null), "A null Converter should cause a NullPointerException when the method tries to call converter.getOptions()");
    }

    @Test
    void testToZonedDateTime_NullOptions(@Mock Converter converterWithNullOptions) {
        // GIVEN
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        Instant now = Instant.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toZonedDateTime(now, converterWithNullOptions), "If Converter.getOptions() returns null, a NullPointerException is expected when accessing getZoneId()");
    }

    @Test
    void testToZonedDateTime_NullZoneId(@Mock Converter converterWithNullZoneId, @Mock ConverterOptions optionsWithNullZone) {
        // GIVEN
        when(optionsWithNullZone.getZoneId()).thenReturn(null);
        when(converterWithNullZoneId.getOptions()).thenReturn(optionsWithNullZone);
        Instant now = Instant.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toZonedDateTime(now, converterWithNullZoneId), "A null ZoneId from ConverterOptions should cause a NullPointerException when ZonedDateTime.atZone(null) is invoked");
    }

    @Test
    void testToZonedDateTime_NullFrom() {
        // GIVEN
        // using the valid static converter created in @BeforeAll
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toZonedDateTime(null, converter), "Passing a null source object should result in a NullPointerException (or be propagated from toInstant)");
    }

    @Test
    void testToZonedDateTime_UnsupportedFromType() {
        // GIVEN
        // not convertible to Instant
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toZonedDateTime(unsupported, converter), "An unsupported source type should trigger IllegalArgumentException from the underlying toInstant conversion");
    }
}
