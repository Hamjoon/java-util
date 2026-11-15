package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Date;
import java.util.Map;
import org.mockito.Mock;
import java.time.format.DateTimeParseException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toZonedDateTime_Test {

    /**
     * Shared Converter used by all test methods.
     */
    private static Converter converter;

    /**
     * Fixed zone used for deterministic assertions.
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    @Nested
    @DisplayName("Error handling / edge cases")
    class EdgeCases {

        @Test
        @DisplayName("null source object should throw IllegalArgumentException")
        void nullSourceThrows() {
            Converter conv = converterWithZone("UTC");
            assertThrows(IllegalArgumentException.class, () -> NumberConversions.toZonedDateTime(null, conv), "Passing null as the source should result in IllegalArgumentException");
        }

        @Test
        @DisplayName("null converter argument should throw NullPointerException")
        void nullConverterThrows() {
            Instant now = Instant.now();
            assertThrows(NullPointerException.class, () -> NumberConversions.toZonedDateTime(now, null), "Null converter should cause NPE when its options are accessed");
        }

        @Test
        @DisplayName("non‑parseable String should cause ConversionException")
        void unparsableStringThrows() {
            Converter conv = converterWithZone("UTC");
            // The library wraps parsing problems in its own ConversionException.
            assertThrows(ConversionException.class, () -> NumberConversions.toZonedDateTime("not-a-date", conv), "A String that cannot be parsed must raise ConversionException");
        }

        @Test
        @DisplayName("unsupported object type should cause ConversionException")
        void unsupportedObjectThrows() {
            Converter conv = converterWithZone("UTC");
            assertThrows(ConversionException.class, () -> NumberConversions.toZonedDateTime(new Object(), conv), "An unsupported source type must raise ConversionException");
        }
    }

    @Nested
    @DisplayName("Zone handling errors")
    class ZoneErrors {

        @Test
        @DisplayName("Converter with null ZoneId leads to NullPointerException")
        void nullZoneIdThrows() {
            Converter conv = converterWithNullZone();
            Instant now = Instant.now();
            assertThrows(NullPointerException.class, () -> NumberConversions.toZonedDateTime(now, conv), "Calling atZone(null) should throw NPE");
        }

        @Test
        @DisplayName("Converter with invalid ZoneId leads to DateTimeException")
        void invalidZoneIdThrows() {
            Converter conv = converterWithInvalidZone();
            Instant now = Instant.now();
            assertThrows(DateTimeException.class, () -> NumberConversions.toZonedDateTime(now, conv), "Resolving an invalid ZoneId must throw DateTimeException");
        }
    }

    @Nested
    @DisplayName("Numeric overflow errors")
    class NumericOverflow {

        @Test
        @DisplayName("Long.MAX_VALUE epoch millis causes DateTimeException")
        void longMaxValueThrows() {
            Converter conv = converterWithZone("UTC");
            assertThrows(DateTimeException.class, () -> NumberConversions.toZonedDateTime(Long.MAX_VALUE, conv), "Epoch millis beyond Instant range must throw DateTimeException");
        }

        @Test
        @DisplayName("Long.MIN_VALUE epoch millis causes DateTimeException")
        void longMinValueThrows() {
            Converter conv = converterWithZone("UTC");
            assertThrows(DateTimeException.class, () -> NumberConversions.toZonedDateTime(Long.MIN_VALUE, conv), "Epoch millis before Instant range must throw DateTimeException");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Configure ConverterOptions to use a known ZoneId (UTC)
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Help GC – not strictly required but keeps the template tidy
        converter = null;
    }

    /**
     * Helper that verifies the returned ZonedDateTime matches the expected instant
     *  and the test zone (UTC).
     */
    private static void assertZonedDateTimeMatchesInstant(Instant expectedInstant, ZonedDateTime actual) {
        assertNotNull(actual, "Resulting ZonedDateTime must not be null");
        assertEquals(TEST_ZONE, actual.getZone(), "ZonedDateTime must use the Converter's zone (UTC)");
        assertEquals(expectedInstant, actual.toInstant(), "ZonedDateTime instant must match the source instant");
    }

    /**
     * Helper stub that creates a {@link Converter} with a deterministic {@link ZoneId}.
     */
    private static Converter converterWithZone(String zoneId) {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of(zoneId));
        return new Converter(options);
    }

    /**
     * Helper stub that creates a {@link Converter} whose {@link ConverterOptions#getZoneId()}
     *  returns {@code null}.
     */
    private static Converter converterWithNullZone() {
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getZoneId()).thenReturn(null);
        return new Converter(options);
    }

    /**
     * Helper stub that creates a {@link Converter} whose {@link ConverterOptions#getZoneId()}
     *  throws a {@link DateTimeException} when accessed (simulating an invalid zone).
     */
    private static Converter converterWithInvalidZone() {
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getZoneId()).thenThrow(DateTimeException.class);
        return new Converter(options);
    }

    @Test
    void toZonedDateTime_fromInstant() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        ZonedDateTime result = NumberConversions.toZonedDateTime(now, converter);
        assertZonedDateTimeMatchesInstant(now, result);
    }

    @Test
    void toZonedDateTime_fromDate() {
        long epochMilli = System.currentTimeMillis();
        Date date = new Date(epochMilli);
        Instant expected = Instant.ofEpochMilli(epochMilli);
        ZonedDateTime result = NumberConversions.toZonedDateTime(date, converter);
        assertZonedDateTimeMatchesInstant(expected, result);
    }

    @Test
    void toZonedDateTime_fromLongEpochMillis() {
        // a fixed point in time
        long epochMilli = 1_600_000_000_000L;
        Instant expected = Instant.ofEpochMilli(epochMilli);
        ZonedDateTime result = NumberConversions.toZonedDateTime(epochMilli, converter);
        assertZonedDateTimeMatchesInstant(expected, result);
    }

    @Test
    void toZonedDateTime_fromZonedDateTime() {
        // Source ZonedDateTime has a different zone; the result must be re‑zoned to UTC
        ZoneId sourceZone = ZoneId.of("America/New_York");
        ZonedDateTime source = ZonedDateTime.of(2023, 3, 15, 12, 30, 45, 0, sourceZone);
        Instant expectedInstant = source.toInstant();
        ZonedDateTime result = NumberConversions.toZonedDateTime(source, converter);
        assertZonedDateTimeMatchesInstant(expectedInstant, result);
        assertEquals(TEST_ZONE, result.getZone(), "Result zone must be the Converter's zone, not the source zone");
    }

    @Test
    void toZonedDateTime_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1_600_000_000_000L);
        Instant expected = Instant.ofEpochMilli(cal.getTimeInMillis());
        ZonedDateTime result = NumberConversions.toZonedDateTime(cal, converter);
        assertZonedDateTimeMatchesInstant(expected, result);
    }

    @Test
    void toZonedDateTime_fromMapWithTimeKey() {
        // The Converter treats a map with a "time" entry as a millisecond epoch value
        long epochMilli = 1_600_000_000_000L;
        Map<String, Object> map = Map.of("time", epochMilli);
        Instant expected = Instant.ofEpochMilli(epochMilli);
        ZonedDateTime result = NumberConversions.toZonedDateTime(map, converter);
        assertZonedDateTimeMatchesInstant(expected, result);
    }

    @Test
    void toZonedDateTime_nullInputReturnsNull() {
        assertNull(NumberConversions.toZonedDateTime(null, converter), "Converting a null source should return null");
    }
}
