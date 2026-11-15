package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toInstant_Test {

    /**
     * Shared Converter used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor
        // provides a sensible configuration for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    /**
     * Provides all test cases from the JSON description.
     *
     * The first element of each {@link Arguments} is the input object,
     * the second is the {@link Converter} (may be {@code null}),
     * the third is either an {@link Instant} expected result or a {@link Class}
     * representing the expected exception type.
     */
    private static Stream<Arguments> toInstantCases() {
        return Stream.of(// 1. java.util.Date
        Arguments.of(new Date(1615803630000L), new Converter(), Instant.parse("2021-03-15T10:20:30Z")), // 2. UTC Calendar
        Arguments.of(createUtcCalendar(1615803630000L), new Converter(), Instant.parse("2021-03-15T10:20:30Z")), // 3. Long epoch millis
        Arguments.of(1615803630000L, new Converter(), Instant.parse("2021-03-15T10:20:30Z")), // 4. ISO‑8601 UTC string
        Arguments.of("2022-12-31T23:59:59Z", new Converter(), Instant.parse("2022-12-31T23:59:59Z")), // 5. LocalDateTime interpreted as UTC (the spec expects UTC)
        Arguments.of(LocalDateTime.of(2020, 1, 1, 0, 0, 0), new Converter(), Instant.parse("2020-01-01T00:00:00Z")), // 6. ZonedDateTime with +02:00 offset
        Arguments.of(ZonedDateTime.of(2021, 5, 10, 12, 30, 0, 0, ZoneId.of("+02:00")), new Converter(), Instant.parse("2021-05-10T10:30:00Z")), // 7. OffsetDateTime with -05:00 offset
        Arguments.of(OffsetDateTime.of(2021, 5, 10, 12, 30, 0, 0, ZoneOffset.ofHours(-5)), new Converter(), Instant.parse("2021-05-10T17:30:00Z")), // 8. Direct Instant
        Arguments.of(Instant.parse("2023-07-01T12:00:00Z"), new Converter(), Instant.parse("2023-07-01T12:00:00Z")), // 9. Null input -> NPE
        Arguments.of(null, new Converter(), NullPointerException.class), // 10. Unsupported Boolean -> IAE
        Arguments.of(Boolean.TRUE, new Converter(), IllegalArgumentException.class), // 11. Empty string -> DateTimeParseException
        Arguments.of("", new Converter(), DateTimeParseException.class), // 12. Malformed string -> DateTimeParseException
        Arguments.of("not-a-date", new Converter(), DateTimeParseException.class), // 13. Integer (unsupported numeric) -> IAE
        Arguments.of(Integer.valueOf(12345), new Converter(), IllegalArgumentException.class), // 14. BigDecimal (unsupported) -> IAE
        Arguments.of(new java.math.BigDecimal("123.45"), new Converter(), IllegalArgumentException.class), // 15. LocalDate (no time) -> IAE
        Arguments.of(LocalDate.of(2021, 5, 10), new Converter(), IllegalArgumentException.class), // 16. java.sql.Date (no time) -> IAE
        Arguments.of(java.sql.Date.valueOf("2021-05-10"), new Converter(), IllegalArgumentException.class), // 17. LocalTime (no date) -> IAE
        Arguments.of(LocalTime.of(12, 30, 0), new Converter(), IllegalArgumentException.class), // 18. AtomicLong (unsupported) -> IAE
        Arguments.of(new AtomicLong(1615803630000L), new Converter(), IllegalArgumentException.class), // 19. Map (unsupported) -> IAE
        Arguments.of(new HashMap<>(), new Converter(), IllegalArgumentException.class), // 20. Null Converter -> NPE
        Arguments.of(new Date(1615803630000L), null, NullPointerException.class));
    }

    /**
     * Helper that creates a {@link Calendar} set to UTC with the supplied epoch millis.
     */
    private static Calendar createUtcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /**
     * Simple description helper for assertion messages.
     */
    private static String describe(Object obj) {
        return obj == null ? "null" : obj.getClass().getSimpleName() + ":" + obj.toString();
    }

    @Test
    void toInstant_givenInstant_returnsSameInstant() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN
        Instant result = LocalDateConversions.toInstant(now, converter);
        // THEN
        assertEquals(now, result, "Instant input should be returned unchanged");
    }

    @Test
    void toInstant_givenDate_returnsCorrespondingInstant() {
        // GIVEN
        // current time in milliseconds
        Date date = new Date();
        Instant expected = date.toInstant();
        // WHEN
        Instant result = LocalDateConversions.toInstant(date, converter);
        // THEN
        assertEquals(expected, result, "Date should be converted to its equivalent Instant");
    }

    @Test
    void toInstant_givenLongEpochMilli_returnsCorrespondingInstant() {
        // GIVEN
        long epochMilli = System.currentTimeMillis();
        Instant expected = Instant.ofEpochMilli(epochMilli);
        // WHEN
        Instant result = LocalDateConversions.toInstant(epochMilli, converter);
        // THEN
        assertEquals(expected, result, "Long epoch‑millis should be converted to the correct Instant");
    }

    @Test
    void toInstant_givenLocalDateTime_returnsInstantAtSystemDefaultZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MARCH, 15, 12, 30, 45);
        ZoneId systemZone = ZoneId.systemDefault();
        Instant expected = ldt.atZone(systemZone).toInstant();
        // WHEN
        Instant result = LocalDateConversions.toInstant(ldt, converter);
        // THEN
        assertEquals(expected, result, "LocalDateTime should be interpreted in the system default time‑zone");
    }

    @Test
    void toInstant_givenZonedDateTime_returnsInstantPreservingZoneOffset() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"));
        Instant expected = zdt.toInstant();
        // WHEN
        Instant result = LocalDateConversions.toInstant(zdt, converter);
        // THEN
        assertEquals(expected, result, "ZonedDateTime should be converted to its exact Instant representation");
    }

    @Test
    void toInstant_givenOffsetDateTime_returnsInstantPreservingOffset() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2021, 6, 10, 8, 15, 30, 0, ZoneOffset.ofHoursMinutes(5, 30));
        Instant expected = odt.toInstant();
        // WHEN
        Instant result = LocalDateConversions.toInstant(odt, converter);
        // THEN
        assertEquals(expected, result, "OffsetDateTime should be converted to its exact Instant representation");
    }

    @Test
    void toInstant_givenSqlTimestamp_returnsCorrespondingInstant() {
        // GIVEN
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Instant expected = ts.toInstant();
        // WHEN
        Instant result = LocalDateConversions.toInstant(ts, converter);
        // THEN
        assertEquals(expected, result, "java.sql.Timestamp should be converted to the matching Instant");
    }

    @ParameterizedTest(name = "{index}: from={0}, converter={1}")
    @MethodSource("toInstantCases")
    void toInstant_parameterised(Object from, Converter conv, Object expected) {
        if (expected instanceof Instant) {
            // happy path – expect a concrete Instant result
            Instant result = LocalDateConversions.toInstant(from, conv);
            assertEquals(expected, result, () -> "Conversion of " + describe(from) + " should yield " + expected);
        } else if (expected instanceof Class) {
            // error path – expect an exception of the given type
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> LocalDateConversions.toInstant(from, conv), () -> "Conversion of " + describe(from) + " should throw " + exClass.getSimpleName());
        } else {
            fail("Test data error – expected must be Instant or Exception class");
        }
    }
}
