package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.DATE;
import static com.cedarsoftware.util.convert.MapConversions.TIME;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toDate_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // Using a real Converter ensures the internal conversion logic (e.g. toLong)
        // works as intended for the various source types.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    private static Stream<Arguments> validDirectDateTimeProvider() {
        ZoneId utc = ZoneId.of("UTC");
        return Stream.of(// java.util.Date
        Arguments.of(new Date(1672531200000L), null, 1672531200000L), // java.util.Calendar (UTC)
        Arguments.of(createUtcCalendar(1672531200000L), null, 1672531200000L), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1672531200000L), null, 1672531200000L), // ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, utc), null, 1672531200000L), // OffsetDateTime (UTC)
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), null, 1672531200000L), // LocalDateTime interpreted in converter's default zone (UTC)
        Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0, 0), new SimpleConverter(utc), 1672531200000L), // LocalDate interpreted as start‑of‑day in converter's default zone (UTC)
        Arguments.of(LocalDate.of(2023, 1, 1), new SimpleConverter(utc), 1672531200000L), // LocalTime combined with supplied date via converter
        Arguments.of(LocalTime.of(0, 0, 0), new SimpleConverter(utc, LocalDate.of(2023, 1, 1)), 1672531200000L));
    }

    private static Stream<Arguments> numericEpochProvider() {
        return Stream.of(Arguments.of(Long.valueOf(0L), null, 0L), // interpreted as ms
        Arguments.of(Integer.valueOf(1672531200), null, 1_672_531_200L), Arguments.of(new AtomicLong(1672531200000L), null, 1672531200000L), Arguments.of(new BigDecimal("1672531200000"), null, 1672531200000L), Arguments.of(new BigInteger("1672531200000"), null, 1672531200000L));
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(// ISO‑8601 UTC
        Arguments.of("2023-01-01T00:00:00Z", null, 1672531200000L), // Custom format without zone, interpreted using converter's default zone (UTC)
        Arguments.of("2023-01-01 00:00:00", new SimpleConverter(ZoneId.of("UTC")), 1672531200000L), // Numeric string interpreted as epoch millis
        Arguments.of("1672531200000", null, 1672531200000L));
    }

    private static Stream<Arguments> mapRepresentationProvider() {
        ZoneId utc = ZoneId.of("UTC");
        return Stream.of(// Map with a DATE entry only
        Arguments.of(new CompactLinkedMap<String, Object>() {

            {
                put(DATE, new Date(1672531200000L));
            }
        }, null, 1672531200000L), // Map with DATE, TIME, ZONE combined
        Arguments.of(new CompactLinkedMap<String, Object>() {

            {
                put(TIME, LocalTime.of(12, 30, 15));
                put(DATE, LocalDate.of(2023, 1, 1));
                put(ZONE, utc);
            }
        }, null, 1672571415000L), // Map with TIME + ZONE, date supplied by converter
        Arguments.of(new CompactLinkedMap<String, Object>() {

            {
                put(TIME, LocalTime.of(0, 0, 0));
                put(ZONE, utc);
            }
        }, new SimpleConverter(utc, LocalDate.of(2023, 1, 1)), 1672531200000L));
    }

    private static Stream<Arguments> sqlTypesProvider() {
        return Stream.of(Arguments.of(Timestamp.from(Instant.ofEpochMilli(1672531200000L)), null, 1672531200000L), Arguments.of(java.sql.Date.valueOf("2023-01-01"), null, 1672531200000L));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of(new Object(), null), Arguments.of(Boolean.TRUE, null));
    }

    private static Stream<String> invalidStringProvider() {
        return Stream.of("2023/01/01 00:00:00", "Jan 01, 2023");
    }

    /**
     * Creates a {@link Calendar} set to UTC with the supplied epoch milliseconds.
     */
    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    @Test
    void toDate_givenDate_returnsSameInstant() {
        // GIVEN
        Date now = new Date();
        // WHEN
        Date result = ZonedDateTimeConversions.toDate(now, converter);
        // THEN
        assertNotNull(result);
        assertEquals(now.getTime(), result.getTime(), "toDate should return a Date with the same epoch millisecond as the input Date");
    }

    @Test
    void toDate_givenLongEpochMillis_returnsCorrespondingDate() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMillis = 1_640_995_200_000L;
        // boxed Long
        Long source = epochMillis;
        // WHEN
        Date result = ZonedDateTimeConversions.toDate(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMillis, result.getTime(), "toDate should convert a Long epoch millisecond value to a Date with the same instant");
    }

    @Test
    void toDate_givenZonedDateTime_returnsEquivalentDate() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 15, 12, 30, 45, 123_000_000, ZoneOffset.UTC);
        long expectedMillis = zdt.toInstant().toEpochMilli();
        // WHEN
        Date result = ZonedDateTimeConversions.toDate(zdt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "toDate should correctly convert a ZonedDateTime to a Date representing the same instant");
    }

    @Test
    void toDate_givenNull_returnsNullDate() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = ZonedDateTimeConversions.toDate(source, converter);
        // THEN
        assertNull(result, "toDate should return null when the source object is null");
    }

    @ParameterizedTest(name = "[{index}] from={0} converter={1}")
    @MethodSource("validDirectDateTimeProvider")
    void toDate_validDirectDateTimeTypes(Object from, Converter conv, long expectedEpoch) {
        Date result = ZonedDateTimeConversions.toDate(from, conv);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), () -> "Unexpected epoch for input: " + from);
    }

    @Test
    void toDate_nullSourceAndNullConverter_returnsNull() {
        assertNull(ZonedDateTimeConversions.toDate(null, null), "When both source and converter are null the result must be null");
    }

    @Test
    void toDate_emptyString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toDate("", null), "Empty string should trigger IllegalArgumentException");
    }

    @ParameterizedTest
    @MethodSource("numericEpochProvider")
    void toDate_numericEpochValues(Object from, Converter conv, long expectedEpoch) {
        Date result = ZonedDateTimeConversions.toDate(from, conv);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), () -> "Numeric epoch conversion failed for " + from);
    }

    @ParameterizedTest
    @MethodSource("stringParsingProvider")
    void toDate_stringParsing(String from, Converter conv, long expectedEpoch) {
        Date result = ZonedDateTimeConversions.toDate(from, conv);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), () -> "String parsing failed for \"" + from + "\"");
    }

    @ParameterizedTest
    @MethodSource("mapRepresentationProvider")
    void toDate_mapRepresentations(Map<String, Object> from, Converter conv, long expectedEpoch) {
        Date result = ZonedDateTimeConversions.toDate(from, conv);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), () -> "Map conversion produced wrong epoch for map: " + from);
    }

    @ParameterizedTest
    @MethodSource("sqlTypesProvider")
    void toDate_sqlTypes(Object from, Converter conv, long expectedEpoch) {
        Date result = ZonedDateTimeConversions.toDate(from, conv);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), () -> "SQL type conversion failed for " + from);
    }

    @ParameterizedTest
    @MethodSource("invalidInputProvider")
    void toDate_invalidInputs(Object from, Converter conv) {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toDate(from, conv), () -> "Expected IllegalArgumentException for input: " + from);
    }

    @Test
    void toDate_longMaxValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toDate(Long.MAX_VALUE, null), "Long.MAX_VALUE exceeds Date range and should cause IllegalArgumentException");
    }

    @Test
    void toDate_longMinValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toDate(Long.MIN_VALUE, null), "Long.MIN_VALUE exceeds Date range and should cause IllegalArgumentException");
    }

    @ParameterizedTest
    @MethodSource("invalidStringProvider")
    void toDate_invalidStringFormats(String input) {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toDate(input, null), () -> "Invalid date string \"" + input + "\" should trigger IllegalArgumentException");
    }
}
