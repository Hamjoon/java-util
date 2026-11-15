package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toInstant_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Preserve the original default time‑zone so we can restore it after the suite runs.
     */
    private static TimeZone originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve original zone and force UTC for deterministic results
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Initialise a Converter with default options (used for the simple cases)
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time‑zone
        TimeZone.setDefault(originalDefaultZone);
    }

    /**
     * Data provider for the “Valid Conversions” group.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// java.util.Date → Instant
        Arguments.of(// 2023‑01‑01T12:00:00Z
        new Date(1_672_574_400_000L), converter, Instant.parse("2023-01-01T12:00:00Z"), "Convert a java.util.Date (epoch millis 1672574400000) to Instant"), // Instant (converter null) → Instant
        Arguments.of(Instant.parse("2023-03-15T08:30:45Z"), null, Instant.parse("2023-03-15T08:30:45Z"), "Instant input should be returned unchanged"), // LocalDateTime interpreted as UTC → Instant
        Arguments.of(LocalDateTime.of(2022, 12, 31, 23, 59, 59), null, Instant.parse("2022-12-31T23:59:59Z"), "LocalDateTime interpreted as UTC"), // LocalDate → midnight UTC → Instant
        Arguments.of(LocalDate.of(2021, 5, 20), null, Instant.parse("2021-05-20T00:00:00Z"), "LocalDate (midnight UTC) conversion"), // ZonedDateTime with offset -05:00 → UTC Instant
        Arguments.of(ZonedDateTime.of(2020, 2, 29, 10, 15, 30, 0, ZoneOffset.ofHours(-5)), null, Instant.parse("2020-02-29T15:15:30Z"), "ZonedDateTime with -05:00 offset"), // Long epoch millis → Instant
        Arguments.of(// 2021‑01‑01T00:00:00Z
        Long.valueOf(1_609_459_200_000L), null, Instant.parse("2021-01-01T00:00:00Z"), "Long epoch millis conversion"), // String parsed by custom converter → Instant
        Arguments.of("2022-07-04T14:20:00Z", new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> to) {
                // Simple parser for the test – ignore 'to' parameter
                return (T) Instant.parse((String) src);
            }
        }, Instant.parse("2022-07-04T14:20:00Z"), "String parsed via custom Converter"));
    }

    /**
     * Data provider for the “Exception Scenarios” group.
     */
    private static Stream<Arguments> exceptionScenarioProvider() {
        return Stream.of(// both arguments null → NPE
        Arguments.of(null, null, NullPointerException.class, "Both arguments null should throw NPE"), // Unsupported Boolean type → IAE
        Arguments.of(Boolean.TRUE, null, IllegalArgumentException.class, "Unsupported source type Boolean"), // Malformed ISO‑8601 string → DateTimeParseException
        Arguments.of("2022-13-40T99:99:99Z", null, java.time.format.DateTimeParseException.class, "Malformed ISO‑8601 string"), // Arbitrary Object → IAE
        Arguments.of(new Object(), null, IllegalArgumentException.class, "Arbitrary object with no conversion rule"), // Long.MAX_VALUE epoch millis → DateTimeException (overflow)
        Arguments.of(Long.MAX_VALUE, null, java.time.DateTimeException.class, "Epoch‑millis exceeds Instant range"), // BigInteger > Long.MAX_VALUE → DateTimeException
        Arguments.of(new BigInteger("9223372036854775808"), null, java.time.DateTimeException.class, "BigInteger epoch value larger than Long.MAX_VALUE"), // LocalTime (no date) → IAE
        Arguments.of(LocalTime.of(12, 34, 56), null, IllegalArgumentException.class, "LocalTime lacks date component"), // Custom converter throws RuntimeException
        Arguments.of("2022-07-04T14:20:00Z", new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> to) {
                throw new RuntimeException("converter failure");
            }
        }, RuntimeException.class, "Custom converter throws RuntimeException"));
    }

    @Test
    void toInstant_givenInstant_returnsSameInstant() {
        Instant now = Instant.now();
        Instant result = LocalDateConversions.toInstant(now, converter);
        assertEquals(now, result, "Instant input should be returned unchanged");
    }

    @Test
    void toInstant_givenDate_returnsInstantAtSameEpochMilli() {
        // 2020‑09‑13T12:26:40Z
        Date date = new Date(1_600_000_000_000L);
        Instant expected = date.toInstant();
        Instant result = LocalDateConversions.toInstant(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void toInstant_givenCalendar_returnsInstantAtSameEpochMilli() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_600_000_000_000L);
        Instant expected = Instant.ofEpochMilli(cal.getTimeInMillis());
        Instant result = LocalDateConversions.toInstant(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void toInstant_givenLongEpochMilli_returnsCorrespondingInstant() {
        Long epochMilli = 1_600_000_000_000L;
        Instant expected = Instant.ofEpochMilli(epochMilli);
        Instant result = LocalDateConversions.toInstant(epochMilli, converter);
        assertEquals(expected, result);
    }

    @Test
    void toInstant_givenLocalDateTime_assumesSystemDefaultZone() {
        LocalDateTime ldt = LocalDateTime.of(2020, Month.JANUARY, 2, 3, 4, 5);
        // The conversion uses the default zone (UTC in our setup)
        Instant expected = ldt.atZone(ZoneId.systemDefault()).toInstant();
        Instant result = LocalDateConversions.toInstant(ldt, converter);
        assertEquals(expected, result);
    }

    @Test
    void toInstant_givenZonedDateTime_returnsSameInstant() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 5, 15, 10, 30, 45, 0, ZoneId.of("America/New_York"));
        Instant expected = zdt.toInstant();
        Instant result = LocalDateConversions.toInstant(zdt, converter);
        assertEquals(expected, result);
    }

    @Test
    void toInstant_givenOffsetDateTime_returnsSameInstant() {
        OffsetDateTime odt = OffsetDateTime.of(2022, 8, 20, 14, 22, 33, 0, ZoneOffset.ofHours(-5));
        Instant expected = odt.toInstant();
        Instant result = LocalDateConversions.toInstant(odt, converter);
        assertEquals(expected, result);
    }

    @Test
    void toInstant_givenNull_returnsNull() {
        assertNull(LocalDateConversions.toInstant(null, converter), "Converting a null source should yield null");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toInstant_validConversions(Object from, Converter customConverter, Instant expected, String description) {
        // Use the custom converter if supplied, otherwise fall back to the shared one
        Converter usedConverter = (customConverter != null) ? customConverter : converter;
        Instant result = LocalDateConversions.toInstant(from, usedConverter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionScenarioProvider")
    void toInstant_exceptionScenarios(Object from, Converter customConverter, Class<? extends Throwable> expectedException, String description) {
        Converter usedConverter = (customConverter != null) ? customConverter : converter;
        // The method is expected to throw the supplied exception type
        assertThrows(expectedException, () -> LocalDateConversions.toInstant(from, usedConverter), description);
    }
}
