package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toSqlDate_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance – the default constructor provides sensible defaults
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> validConversionProvider() {
        // a converter that parses an ISO‑8601 string to epoch millis
        Converter isoConverter = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                if (toClass == Long.class && from instanceof String) {
                    return toClass.cast(Instant.parse((String) from).toEpochMilli());
                }
                return super.convert(from, toClass);
            }
        };
        return Stream.of(// Long epoch millis
        Arguments.of(1622548800000L, null, 1622548800000L, "Convert a Long representing epoch milliseconds to java.util.Date"), // numeric String
        Arguments.of("1622548800000", null, 1622548800000L, "Convert a numeric String representing epoch milliseconds to java.util.Date"), // existing java.util.Date
        Arguments.of(new Date(1622548800000L), null, 1622548800000L, "Convert an existing java.util.Date instance (same instant)"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2021-06-01 00:00:00.000"), null, 1622548800000L, "Convert a java.sql.Timestamp to java.util.Date preserving the same instant"), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1622548800000L), null, 1622548800000L, "Convert a java.time.Instant to java.util.Date"), // LocalDateTime interpreted as UTC (as per JSON spec)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 0, 0, 0), null, LocalDateTime.of(2021, 6, 1, 0, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), "Convert a LocalDateTime (interpreted as UTC) to java.util.Date"), // ZonedDateTime in UTC
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC), null, 1622548800000L, "Convert a ZonedDateTime in UTC to java.util.Date"), // OffsetDateTime in UTC
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC), null, 1622548800000L, "Convert an OffsetDateTime in UTC to java.util.Date"), // AtomicLong holding epoch millis
        Arguments.of(new AtomicLong(1622548800000L), null, 1622548800000L, "Convert an AtomicLong holding epoch millis to java.util.Date"), // Custom converter parsing ISO‑8601 string
        Arguments.of("2021-06-01T00:00:00Z", isoConverter, 1622548800000L, "Use a custom Converter that parses ISO‑8601 strings to epoch millis"));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of(null, null, NullPointerException.class, "Passing null as the source object should result in a NullPointerException"), Arguments.of(true, null, IllegalArgumentException.class, "Passing an unsupported type (Boolean) should cause an IllegalArgumentException"), Arguments.of("not-a-number", null, NumberFormatException.class, "Passing a non‑numeric String should trigger a NumberFormatException"));
    }

    private static Stream<Arguments> boundaryProvider() {
        // ISO‑8601 custom converter (same as above) – reused for the two offset string cases
        Converter isoConverter = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                if (toClass == Long.class && from instanceof String) {
                    return toClass.cast(Instant.parse((String) from).toEpochMilli());
                }
                return super.convert(from, toClass);
            }
        };
        return Stream.of(// Epoch start LocalDateTime interpreted as UTC → epoch 0
        Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 0), null, 0L, "Convert the epoch start LocalDateTime (interpreted as UTC) to java.util.Date"), // One millisecond before epoch (negative epoch)
        Arguments.of(LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_000_000), null, -1L, "Convert a LocalDateTime one millisecond before the epoch to java.util.Date"), // ZonedDateTime in America/New_York (UTC‑4 at the given date)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneId.of("America/New_York")), null, 1622552400000L, "Convert a ZonedDateTime in a non‑UTC zone (America/New_York, UTC‑4) to java.util.Date"), // OffsetDateTime with -05:00 offset
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5)), null, 1622552400000L, "Convert an OffsetDateTime with a -05:00 offset to java.util.Date"), // Timestamp with milliseconds
        Arguments.of(Timestamp.valueOf("2021-06-01 00:00:00.123"), null, 1622548800123L, "Convert a Timestamp that includes milliseconds; ensures sub‑second precision is retained"), // Instant just before epoch (‑1 ns rounded to –1 ms)
        Arguments.of(Instant.ofEpochSecond(-1, 999_999_999), null, -1L, "Convert an Instant just 1 ns before epoch (rounded to the nearest millisecond)"), // Date at Long.MAX_VALUE – we only assert that the conversion does not overflow
        Arguments.of(new Date(Long.MAX_VALUE), null, Long.MAX_VALUE, "Convert a Date at the maximum long value; checks handling of extreme future timestamps"), // Date at Long.MIN_VALUE
        Arguments.of(new Date(Long.MIN_VALUE), null, Long.MIN_VALUE, "Convert a Date at the minimum long value; verifies behavior with extreme past timestamps"), // ISO‑8601 string with +02:00 offset using custom converter
        Arguments.of("2021-06-01T00:00:00+02:00", isoConverter, 1622541600000L, "Custom converter parses ISO‑8601 with positive offset; ensures offset is applied before Date creation"), // ISO‑8601 string with -02:00 offset using custom converter
        Arguments.of("2021-06-01T00:00:00-02:00", isoConverter, 1622556000000L, "Custom converter parses ISO‑8601 with negative offset; validates correct conversion to UTC epoch millis"));
    }

    private static Stream<Arguments> converterExceptionProvider() {
        // Converter that deliberately throws a RuntimeException
        Converter throwingConverter = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                throw new RuntimeException("forced failure");
            }
        };
        // ISO‑8601 converter that attempts to parse an invalid date string
        Converter isoConverter = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                if (toClass == Long.class && from instanceof String) {
                    // This will throw DateTimeParseException for the malformed string
                    return toClass.cast(Instant.parse((String) from).toEpochMilli());
                }
                return super.convert(from, toClass);
            }
        };
        return Stream.of(Arguments.of("12345", throwingConverter, RuntimeException.class, "Custom Converter throws a RuntimeException during conversion, which should propagate"), Arguments.of("2021-13-01T00:00:00Z", isoConverter, DateTimeParseException.class, "Custom ISO‑8601 converter attempts to parse an invalid date string and throws DateTimeParseException"));
    }

    private static Stream<Arguments> numericOverflowProvider() {
        return Stream.of(// BigDecimal exceeding Long.MAX_VALUE
        Arguments.of(new BigDecimal("9223372036854775808"), null, ArithmeticException.class, "BigDecimal value exceeds Long.MAX_VALUE; toLong should use longValueExact and throw ArithmeticException"), // String larger than Long.MAX_VALUE
        Arguments.of("9223372036854775808", null, NumberFormatException.class, "String representing a number larger than Long.MAX_VALUE should cause NumberFormatException"), // Instant that overflows when converted to milliseconds
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1), null, ArithmeticException.class, "Instant epoch seconds multiplied to milliseconds overflows long range, leading to ArithmeticException"));
    }

    private static Stream<Arguments> invalidNumericProvider() {
        return Stream.of(Arguments.of(Double.NaN, null, IllegalArgumentException.class, "NaN cannot be converted to a long epoch millisecond value"), Arguments.of(Double.POSITIVE_INFINITY, null, IllegalArgumentException.class, "Infinite double value is invalid for epoch conversion"), Arguments.of(Double.NEGATIVE_INFINITY, null, IllegalArgumentException.class, "Negative infinite double value is invalid for epoch conversion"));
    }

    @Test
    void toSqlDate_fromUtilDate_returnsSameInstant() {
        // GIVEN
        Date now = new Date();
        // WHEN
        Date result = InstantConversions.toSqlDate(now, converter);
        // THEN
        assertNotNull(result);
        assertEquals(now.getTime(), result.getTime(), "Resulting Date should have the same epoch milliseconds as the source Date");
    }

    @Test
    void toSqlDate_fromInstant_returnsEquivalentDate() {
        // GIVEN
        Instant instant = Instant.now();
        // WHEN
        Date result = InstantConversions.toSqlDate(instant, converter);
        // THEN
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.getTime(), "Resulting Date should represent the same instant as the source Instant");
    }

    @Test
    void toSqlDate_fromLong_returnsDateWithThatEpochMillis() {
        // GIVEN
        long epochMillis = System.currentTimeMillis();
        // WHEN
        Date result = InstantConversions.toSqlDate(epochMillis, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMillis, result.getTime(), "Resulting Date should have the epoch milliseconds supplied as a Long");
    }

    @Test
    void toSqlDate_fromTimestamp_returnsDateWithSameInstant() {
        // GIVEN
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        // WHEN
        Date result = InstantConversions.toSqlDate(ts, converter);
        // THEN
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime(), "Resulting Date should have the same epoch milliseconds as the source Timestamp");
    }

    @Test
    void toSqlDate_fromLocalDateTime_assumesSystemDefaultZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        long expected = ldt.toInstant(ZoneOffset.systemDefault().getRules().getOffset(ldt)).toEpochMilli();
        // WHEN
        Date result = InstantConversions.toSqlDate(ldt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "Resulting Date should represent the same instant as the source LocalDateTime using the system default zone");
    }

    @Test
    void toSqlDate_fromZonedDateTime_returnsExactInstant() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.now();
        // WHEN
        Date result = InstantConversions.toSqlDate(zdt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(zdt.toInstant().toEpochMilli(), result.getTime(), "Resulting Date should have the same epoch milliseconds as the source ZonedDateTime");
    }

    @Test
    void toSqlDate_fromOffsetDateTime_returnsExactInstant() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.now();
        // WHEN
        Date result = InstantConversions.toSqlDate(odt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(odt.toInstant().toEpochMilli(), result.getTime(), "Resulting Date should have the same epoch milliseconds as the source OffsetDateTime");
    }

    @Test
    void toSqlDate_fromLocalDate_returnsStartOfDayInSystemZone() {
        // GIVEN
        LocalDate ld = LocalDate.now();
        long expected = ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        // WHEN
        Date result = InstantConversions.toSqlDate(ld, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "Resulting Date should represent the start of the given LocalDate in the system default zone");
    }

    @Test
    void toSqlDate_fromLocalTime_throwsException() {
        // GIVEN
        LocalTime lt = LocalTime.now();
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> InstantConversions.toSqlDate(lt, converter), "Converting a LocalTime (which lacks a date) should result in a conversion failure");
    }

    /* -------------------------------------------------------------
     *  Valid conversions – parameterised test
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toSqlDate_validConversions(Object from, Converter conv, long expectedEpochMillis, String description) {
        Date result = InstantConversions.toSqlDate(from, conv);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Incorrect epoch millis for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Invalid inputs – parameterised test
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidInputProvider")
    void toSqlDate_invalidInputs(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> InstantConversions.toSqlDate(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Boundary and Time‑Zone Edge Cases – parameterised test
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("boundaryProvider")
    void toSqlDate_boundaryCases(Object from, Converter conv, long expectedEpochMillis, String description) {
        Date result = InstantConversions.toSqlDate(from, conv);
        assertNotNull(result);
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Boundary case failed: " + description);
    }

    /* -------------------------------------------------------------
     *  Converter exception scenarios – parameterised test
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("converterExceptionProvider")
    void toSqlDate_converterExceptions(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> InstantConversions.toSqlDate(from, conv), () -> "Expected exception for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Numeric overflow scenarios – parameterised test
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericOverflowProvider")
    void toSqlDate_numericOverflow(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> InstantConversions.toSqlDate(from, conv), () -> "Expected overflow exception for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Invalid numeric values – parameterised test
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidNumericProvider")
    void toSqlDate_invalidNumericValues(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> InstantConversions.toSqlDate(from, conv), () -> "Expected IllegalArgumentException for case: " + description);
    }
}
