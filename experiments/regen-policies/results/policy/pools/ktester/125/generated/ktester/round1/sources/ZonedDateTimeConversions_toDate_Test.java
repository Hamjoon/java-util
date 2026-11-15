package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toDate_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    /**
     * A sample ZonedDateTime used in several tests.
     */
    private static ZonedDateTime sampleZoned;

    /**
     * The epoch‑millis representation of {@link #sampleZoned}.
     */
    private static long sampleMillis;

    /**
     * Simple holder for test arguments and expected outcomes.
     */
    private static class ArgumentsHolder {

        final Object input;

        // may be null
        final Converter converter;

        // null when an exception is expected
        final Date expected;

        final Class<? extends Throwable> expectedException;

        final String description;

        ArgumentsHolder(Object input, Converter converter, Date expected, Class<? extends Throwable> expectedException) {
            this(input, converter, expected, expectedException, (expectedException == null) ? "Successful conversion" : "Expected exception");
        }

        ArgumentsHolder(Object input, Converter converter, Date expected, Class<? extends Throwable> expectedException, String description) {
            this.input = input;
            this.converter = converter;
            this.expected = expected;
            this.expectedException = expectedException;
            this.description = description;
        }

        @Override
        public String toString() {
            return "input=" + input + ", expectedException=" + expectedException;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter has a public constructor that requires ConverterOptions.
        // Using the default options is sufficient for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
        // Prepare a deterministic ZonedDateTime (UTC) for reproducible results.
        sampleZoned = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        3, // year, month, day
        15, // hour, minute, second, nano
        12, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        123_000_000, ZoneOffset.UTC);
        sampleMillis = sampleZoned.toInstant().toEpochMilli();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /**
     * Provides arguments for the “basic numeric and temporal conversions” group.
     */
    static Stream<ArgumentsHolder> basicNumericAndTemporalProvider() {
        return Stream.of(// Long literal
        new ArgumentsHolder(1622548800000L, null, new Date(1622548800000L), null), // Integer (Number) – 12345
        new ArgumentsHolder(12345, null, new Date(12345L), null), // Numeric String
        new ArgumentsHolder("1622548800000", null, new Date(1622548800000L), null), // java.util.Date instance
        new ArgumentsHolder(new Date(1622548800000L), null, new Date(1622548800000L), null), // java.sql.Timestamp
        new ArgumentsHolder(Timestamp.valueOf("2021-06-01 12:00:00"), null, Date.from(Instant.parse("2021-06-01T12:00:00Z")), null), // Instant
        new ArgumentsHolder(Instant.ofEpochMilli(1622548800000L), null, new Date(1622548800000L), null), // LocalDateTime with UTC zone (converter is null but system default is UTC)
        new ArgumentsHolder(LocalDateTime.of(2021, 6, 1, 12, 0, 0), null, new Date(1622548800000L), IllegalArgumentException.class), // Calendar (UTC)
        new ArgumentsHolder(createUtcCalendar(1622548800000L), null, new Date(1622548800000L), null));
    }

    /**
     * Provides arguments for the “null and unsupported inputs” group.
     */
    static Stream<ArgumentsHolder> nullAndUnsupportedProvider() {
        return Stream.of(// null source – should throw NPE according to JSON spec
        new ArgumentsHolder(null, null, null, NullPointerException.class), // Boolean – unsupported
        new ArgumentsHolder(Boolean.TRUE, null, null, IllegalArgumentException.class));
    }

    /**
     * Provides arguments for the “numeric wrapper types” group.
     */
    static Stream<ArgumentsHolder> numericWrapperProvider() {
        return Stream.of(new ArgumentsHolder(new BigDecimal("1622548800000"), null, new Date(1622548800000L), null), new ArgumentsHolder(new BigInteger("1622548800000"), null, new Date(1622548800000L), null), new ArgumentsHolder(new AtomicLong(1622548800000L), null, new Date(1622548800000L), null));
    }

    /**
     * Provides arguments for the “temporal types other than those covered” group.
     */
    static Stream<ArgumentsHolder> otherTemporalProvider() {
        return Stream.of(// ZonedDateTime with Europe/Paris (UTC+2 in summer)
        new ArgumentsHolder(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("Europe/Paris")), null, new Date(1622548800000L), null), // OffsetDateTime with -05:00 offset
        new ArgumentsHolder(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-5)), null, new Date(1622566800000L), null), // LocalDate (needs zone – system default is UTC)
        new ArgumentsHolder(LocalDate.of(2021, 6, 1), null, new Date(1622505600000L), IllegalArgumentException.class), // LocalTime (needs zone – system default is UTC)
        new ArgumentsHolder(LocalTime.of(12, 30, 15), null, new Date(45015000L), IllegalArgumentException.class));
    }

    /**
     * Provides arguments for the “Map based conversion” group.
     */
    static Stream<ArgumentsHolder> mapBasedProvider() {
        // Map with DATE, TIME, ZONE keys
        CompactMap<String, Object> mapFull = new CompactMap<>();
        mapFull.put(MapConversions.DATE, LocalDate.of(2021, 6, 1));
        mapFull.put(MapConversions.TIME, LocalTime.of(12, 0, 0));
        mapFull.put(MapConversions.ZONE, ZoneId.of("UTC"));
        // Map with DATE (as java.util.Date) and TIME, no ZONE
        CompactMap<String, Object> mapNoZone = new CompactMap<>();
        mapNoZone.put(MapConversions.DATE, Date.from(Instant.ofEpochMilli(1622548800000L)));
        mapNoZone.put(MapConversions.TIME, LocalTime.of(12, 0, 0));
        return Stream.of(new ArgumentsHolder(mapFull, null, new Date(1622548800000L), null), new ArgumentsHolder(mapNoZone, null, new Date(1622548800000L), null));
    }

    /**
     * Provides arguments for the “string ISO instant parsing” group.
     */
    static Stream<ArgumentsHolder> isoStringProvider() {
        return Stream.of(// Valid ISO‑8601 instant
        new ArgumentsHolder("2021-06-01T12:00:00Z", null, new Date(1622548800000L), null), // Non‑ISO format – expect IllegalArgumentException
        new ArgumentsHolder("06/01/2021 12:00:00", null, null, IllegalArgumentException.class));
    }

    /**
     * Provides arguments for the “unsupported source types” group.
     */
    static Stream<ArgumentsHolder> unsupportedSourceProvider() {
        return Stream.of(new ArgumentsHolder(new Object(), null, null, IllegalArgumentException.class), new ArgumentsHolder(new int[] { 1, 2, 3 }, null, null, IllegalArgumentException.class), new ArgumentsHolder(Arrays.asList(1, 2, 3), null, null, IllegalArgumentException.class));
    }

    /**
     * Provides arguments for the “missing converter for zone‑dependent types” group.
     */
    static Stream<ArgumentsHolder> missingConverterProvider() {
        return Stream.of(new ArgumentsHolder(LocalDateTime.of(2021, 6, 1, 12, 0, 0), null, null, IllegalArgumentException.class), new ArgumentsHolder(LocalDate.of(2021, 6, 1), null, null, IllegalArgumentException.class), new ArgumentsHolder(LocalTime.of(12, 30, 0), null, null, IllegalArgumentException.class));
    }

    /**
     * Provides arguments for the “malformed Map inputs” group.
     */
    static Stream<ArgumentsHolder> malformedMapProvider() {
        CompactMap<String, Object> emptyMap = new CompactMap<>();
        CompactMap<String, Object> onlyTime = new CompactMap<>();
        onlyTime.put(MapConversions.TIME, LocalTime.of(12, 0, 0));
        CompactMap<String, Object> dateStringTime = new CompactMap<>();
        dateStringTime.put(MapConversions.DATE, "2021-06-01");
        dateStringTime.put(MapConversions.TIME, LocalTime.of(12, 0, 0));
        CompactMap<String, Object> dateTimeString = new CompactMap<>();
        dateTimeString.put(MapConversions.DATE, LocalDate.of(2021, 6, 1));
        dateTimeString.put(MapConversions.TIME, "12:00:00");
        return Stream.of(new ArgumentsHolder(emptyMap, null, null, IllegalArgumentException.class), new ArgumentsHolder(onlyTime, null, null, IllegalArgumentException.class), new ArgumentsHolder(dateStringTime, null, null, IllegalArgumentException.class), new ArgumentsHolder(dateTimeString, null, null, IllegalArgumentException.class));
    }

    /**
     * Provides arguments for the “invalid numeric strings” group.
     */
    static Stream<ArgumentsHolder> invalidNumericStringProvider() {
        return Stream.of(new ArgumentsHolder("notANumber", null, null, IllegalArgumentException.class), new ArgumentsHolder("12345.67", null, null, IllegalArgumentException.class));
    }

    /**
     * Creates a UTC Calendar set to the supplied epoch‑millis.
     */
    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Verify that a {@code null} source returns {@code null} (the contract of Converter)
     * and that {@code toDate} consequently returns {@code null}.
     */
    @Test
    void toDate_NullInput_ReturnsNull() {
        Date result = ZonedDateTimeConversions.toDate(null, converter);
        assertNull(result, "Converting null should produce a null Date");
    }

    /**
     * Convert a {@link ZonedDateTime} instance directly.
     * The resulting {@link Date} should represent the same instant.
     */
    @Test
    void toDate_FromZonedDateTime_ReturnsEquivalentDate() {
        Date date = ZonedDateTimeConversions.toDate(sampleZoned, converter);
        assertNotNull(date, "Resulting Date must not be null");
        assertEquals(sampleMillis, date.getTime(), "Date milliseconds must match the ZonedDateTime instant");
    }

    /**
     * Convert a {@link Map} that follows the conventions used by the library
     * (keys {@code ZONE} and {@code TIME} or {@code DATE}).
     * The map is interpreted as a ZonedDateTime and the resulting Date
     * should match the expected epoch milliseconds.
     */
    @Test
    void toDate_FromMap_ReturnsEquivalentDate() {
        Map<String, Object> map = new HashMap<>();
        // The library expects a key named "time" (or the static DATE constant) for epoch millis
        map.put(MapConversions.TIME, sampleMillis);
        // And a key named "zone" (or the static ZONE constant) for the zone ID
        map.put(MapConversions.ZONE, "UTC");
        Date date = ZonedDateTimeConversions.toDate(map, converter);
        assertNotNull(date);
        assertEquals(sampleMillis, date.getTime(), "Date created from map must represent the same instant as the original ZonedDateTime");
    }

    /**
     * Convert a {@link java.sql.Timestamp} – the Converter knows how to turn a
     * Timestamp into a {@link Date}. The resulting Date should have the same
     * millisecond value as the Timestamp.
     */
    @Test
    void toDate_FromTimestamp_ReturnsEquivalentDate() {
        Timestamp ts = new Timestamp(sampleMillis);
        // preserve nanosecond part
        ts.setNanos(sampleZoned.getNano());
        Date date = ZonedDateTimeConversions.toDate(ts, converter);
        assertNotNull(date);
        assertEquals(sampleMillis, date.getTime(), "Date created from Timestamp must have the same epoch milli");
    }

    /**
     * Convert a {@link java.util.Date} (already a Date). The method should simply
     * wrap the long value returned by {@code toLong}, resulting in an equal Date.
     */
    @Test
    void toDate_FromDate_ReturnsSameInstanceValue() {
        Date original = new Date(sampleMillis);
        Date result = ZonedDateTimeConversions.toDate(original, converter);
        assertNotNull(result);
        assertEquals(original.getTime(), result.getTime(), "Converting a Date should yield a Date with identical epoch millis");
    }

    /**
     * Convert a {@link Long} representing epoch milliseconds.
     * The conversion chain should produce a Date with the same instant.
     */
    @Test
    void toDate_FromLong_ReturnsEquivalentDate() {
        Long epoch = Long.valueOf(sampleMillis);
        Date date = ZonedDateTimeConversions.toDate(epoch, converter);
        assertNotNull(date);
        assertEquals(sampleMillis, date.getTime(), "Date created from Long epoch millis must match the original value");
    }

    /**
     * Ensure that an unsupported type (e.g., a plain String that cannot be parsed)
     * results in a {@code null} Date rather than throwing an exception.
     * The Converter returns {@code null} for object types when conversion fails.
     */
    @Test
    void toDate_FromUnsupportedType_ReturnsNull() {
        String bogus = "not-a-date";
        Date result = ZonedDateTimeConversions.toDate(bogus, converter);
        assertNull(result, "Conversion of an unsupported type should yield null");
    }

    @ParameterizedTest(name = "{index}: toDate({0})")
    @MethodSource("basicNumericAndTemporalProvider")
    void toDate_BasicNumericAndTemporal(ArgumentsHolder holder) {
        if (holder.expectedException != null) {
            assertThrows(holder.expectedException, () -> ZonedDateTimeConversions.toDate(holder.input, holder.converter), holder.description);
        } else {
            Date result = ZonedDateTimeConversions.toDate(holder.input, holder.converter);
            assertNotNull(result, holder.description);
            assertEquals(holder.expected.getTime(), result.getTime(), holder.description);
        }
    }

    @ParameterizedTest(name = "{index}: toDate({0})")
    @MethodSource("nullAndUnsupportedProvider")
    void toDate_NullAndUnsupported(ArgumentsHolder holder) {
        assertThrows(holder.expectedException, () -> ZonedDateTimeConversions.toDate(holder.input, holder.converter), holder.description);
    }

    @ParameterizedTest(name = "{index}: toDate({0})")
    @MethodSource("numericWrapperProvider")
    void toDate_NumericWrappers(ArgumentsHolder holder) {
        Date result = ZonedDateTimeConversions.toDate(holder.input, holder.converter);
        assertNotNull(result);
        assertEquals(holder.expected.getTime(), result.getTime());
    }

    @ParameterizedTest(name = "{index}: toDate({0})")
    @MethodSource("otherTemporalProvider")
    void toDate_OtherTemporalTypes(ArgumentsHolder holder) {
        if (holder.expectedException != null) {
            assertThrows(holder.expectedException, () -> ZonedDateTimeConversions.toDate(holder.input, holder.converter), holder.description);
        } else {
            Date result = ZonedDateTimeConversions.toDate(holder.input, holder.converter);
            assertNotNull(result);
            assertEquals(holder.expected.getTime(), result.getTime());
        }
    }

    @ParameterizedTest(name = "{index}: toDate(Map)")
    @MethodSource("mapBasedProvider")
    void toDate_MapBased(ArgumentsHolder holder) {
        Date result = ZonedDateTimeConversions.toDate(holder.input, holder.converter);
        assertNotNull(result);
        assertEquals(holder.expected.getTime(), result.getTime());
    }

    @ParameterizedTest(name = "{index}: toDate(String)")
    @MethodSource("isoStringProvider")
    void toDate_StringParsing(ArgumentsHolder holder) {
        if (holder.expectedException != null) {
            assertThrows(holder.expectedException, () -> ZonedDateTimeConversions.toDate(holder.input, holder.converter), holder.description);
        } else {
            Date result = ZonedDateTimeConversions.toDate(holder.input, holder.converter);
            assertNotNull(result);
            assertEquals(holder.expected.getTime(), result.getTime());
        }
    }

    @ParameterizedTest(name = "{index}: toDate(unsupported)")
    @MethodSource("unsupportedSourceProvider")
    void toDate_UnsupportedSources(ArgumentsHolder holder) {
        assertThrows(holder.expectedException, () -> ZonedDateTimeConversions.toDate(holder.input, holder.converter), holder.description);
    }

    @ParameterizedTest(name = "{index}: toDate(zone‑dependent without converter)")
    @MethodSource("missingConverterProvider")
    void toDate_MissingConverterForZoneDependent(ArgumentsHolder holder) {
        assertThrows(holder.expectedException, () -> ZonedDateTimeConversions.toDate(holder.input, holder.converter), holder.description);
    }

    @ParameterizedTest(name = "{index}: toDate(malformed map)")
    @MethodSource("malformedMapProvider")
    void toDate_MalformedMaps(ArgumentsHolder holder) {
        assertThrows(holder.expectedException, () -> ZonedDateTimeConversions.toDate(holder.input, holder.converter), holder.description);
    }

    @ParameterizedTest(name = "{index}: toDate(invalid numeric string)")
    @MethodSource("invalidNumericStringProvider")
    void toDate_InvalidNumericStrings(ArgumentsHolder holder) {
        assertThrows(holder.expectedException, () -> ZonedDateTimeConversions.toDate(holder.input, holder.converter), holder.description);
    }
}
