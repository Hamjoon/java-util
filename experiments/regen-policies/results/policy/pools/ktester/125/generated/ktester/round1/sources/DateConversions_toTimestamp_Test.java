package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toTimestamp_Test {

    private static Converter converter;

    /**
     * Default converter – used for the majority of the test cases.
     */
    private static Converter defaultConverter;

    /**
     * Parses an ISO‑8601 string into epoch milliseconds.
     */
    private static final class IsoStringConverter extends Converter {

        IsoStringConverter() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object from, Class<T> toClass) {
            if (toClass == Long.class && from instanceof String) {
                long epoch = Instant.parse((String) from).toEpochMilli();
                return (T) Long.valueOf(epoch);
            }
            return super.convert(from, toClass);
        }
    }

    /**
     * Parses a numeric string that represents **seconds** since epoch.
     */
    private static final class SecondsStringConverter extends Converter {

        SecondsStringConverter() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object from, Class<T> toClass) {
            if (toClass == Long.class && from instanceof String) {
                long seconds = Long.parseLong((String) from);
                return (T) Long.valueOf(seconds * 1_000L);
            }
            return super.convert(from, toClass);
        }
    }

    /**
     * Parses a custom pattern (e.g. "yyyy‑MM‑dd HH:mm:ss") into epoch milliseconds.
     */
    private static final class CustomPatternConverter extends Converter {

        private final DateTimeFormatter formatter;

        CustomPatternConverter(String pattern) {
            super(new ConverterOptions());
            this.formatter = DateTimeFormatter.ofPattern(pattern);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object from, Class<T> toClass) {
            if (toClass == Long.class && from instanceof String) {
                LocalDateTime ldt = LocalDateTime.parse((String) from, formatter);
                long epoch = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return (T) Long.valueOf(epoch);
            }
            return super.convert(from, toClass);
        }
    }

    /**
     * Extracts a long value from a user‑defined object.
     */
    private static final class CustomObjectConverter extends Converter {

        CustomObjectConverter() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object from, Class<T> toClass) {
            if (toClass == Long.class && from instanceof MyCustom) {
                return (T) Long.valueOf(((MyCustom) from).value);
            }
            return super.convert(from, toClass);
        }
    }

    /**
     * Converter that always throws an unchecked exception – used to verify propagation.
     */
    private static final class ThrowingConverter extends Converter {

        ThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw new IllegalStateException("forced failure");
        }
    }

    /**
     * Simple POJO used by {@link CustomObjectConverter}.
     */
    private static final class MyCustom {

        final long value;

        MyCustom(long value) {
            this.value = value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a non‑null options instance; the default options are sufficient for our tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        converter = null;
    }

    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// null input – the library treats null as epoch 0 (no exception)
        Arguments.of(null, defaultConverter, "1970-01-01T00:00:00Z", "null → epoch"), // plain Long (epoch millis)
        Arguments.of(1622548800000L, defaultConverter, "2021-06-01T12:00:00Z", "Long → exact instant"), // java.util.Date
        Arguments.of(new Date(1622548800000L), defaultConverter, "2021-06-01T12:00:00Z", "Date → exact instant"), // java.sql.Timestamp
        Arguments.of(new Timestamp(1622548800000L), defaultConverter, "2021-06-01T12:00:00Z", "Timestamp → exact instant"), // numeric String (epoch millis)
        Arguments.of("1622548800000", defaultConverter, "2021-06-01T12:00:00Z", "String (millis) → instant"), // ISO‑8601 string parsed by a custom converter
        Arguments.of("2021-06-01T12:00:00Z", new IsoStringConverter(), "2021-06-01T12:00:00Z", "ISO‑8601 string via IsoStringConverter"), // User defined object via a custom converter
        Arguments.of(new MyCustom(1622548800000L), new CustomObjectConverter(), "2021-06-01T12:00:00Z", "MyCustom via CustomObjectConverter"), // BigDecimal
        Arguments.of(new BigDecimal("1622548800000"), defaultConverter, "2021-06-01T12:00:00Z", "BigDecimal → instant"), // BigInteger
        Arguments.of(BigInteger.valueOf(1622548800000L), defaultConverter, "2021-06-01T12:00:00Z", "BigInteger → instant"), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1622548800000L), defaultConverter, "2021-06-01T12:00:00Z", "Instant → instant"), // LocalDateTime (system default zone)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 0, 0), defaultConverter, "2021-06-01T12:00:00Z", "LocalDateTime → instant (system zone)"), // ZonedDateTime at UTC
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC), defaultConverter, "2021-06-01T12:00:00Z", "ZonedDateTime UTC → instant"), // OffsetDateTime (+02:00) → UTC instant
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(2)), defaultConverter, "2021-06-01T10:00:00Z", "OffsetDateTime +02 → instant"), // Calendar in UTC
        Arguments.of(utcCalendar(1622548800000L), defaultConverter, "2021-06-01T12:00:00Z", "Calendar UTC → instant"), // Seconds‑based string via custom converter
        Arguments.of("1622548800", new SecondsStringConverter(), "2021-06-01T12:00:00Z", "Seconds string via SecondsStringConverter"), // Custom pattern string via custom converter
        Arguments.of("2021-06-01 12:00:00", new CustomPatternConverter("yyyy-MM-dd HH:mm:ss"), "2021-06-01T12:00:00Z", "Pattern string via CustomPatternConverter"), // LocalDate (interpreted as start of day)
        Arguments.of(LocalDate.of(2021, 6, 1), defaultConverter, "2021-06-01T00:00:00Z", "LocalDate → start‑of‑day instant"));
    }

    private static Stream<Arguments> exceptionConversions() {
        return Stream.of(// Null converter argument
        Arguments.of(1622548800000L, null, NullPointerException.class, "null converter → NPE"), // NaN double
        Arguments.of(Double.NaN, defaultConverter, IllegalArgumentException.class, "Double.NaN → IAE"), // Positive infinity double
        Arguments.of(Double.POSITIVE_INFINITY, defaultConverter, IllegalArgumentException.class, "Double.POSITIVE_INFINITY → IAE"), // BigDecimal with fractional part
        Arguments.of(new BigDecimal("123.45"), defaultConverter, ArithmeticException.class, "BigDecimal with fraction → ArithmeticException"), // BigInteger overflow
        Arguments.of(new BigInteger("9223372036854775808"), defaultConverter, ArithmeticException.class, "BigInteger > Long.MAX_VALUE → ArithmeticException"), // Instant far beyond long‑millis range
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE), defaultConverter, ArithmeticException.class, "Instant overflow → ArithmeticException"), // LocalTime (no date)
        Arguments.of(LocalTime.of(12, 30), defaultConverter, ClassCastException.class, "LocalTime → ClassCastException"), // Arbitrary object without a custom converter
        Arguments.of(new Object(), defaultConverter, ClassCastException.class, "Object → ClassCastException"), // Primitive array
        Arguments.of(new int[] { 1, 2, 3 }, defaultConverter, ClassCastException.class, "int[] → ClassCastException"), // Non‑numeric string
        Arguments.of("not-a-number", defaultConverter, IllegalArgumentException.class, "Invalid numeric string → IAE"), // Empty string
        Arguments.of("", defaultConverter, IllegalArgumentException.class, "Empty string → IAE"), // Boolean
        Arguments.of(Boolean.TRUE, defaultConverter, ClassCastException.class, "Boolean → ClassCastException"), // Converter that throws a runtime exception
        Arguments.of(1622548800000L, new ThrowingConverter(), IllegalStateException.class, "ThrowingConverter → IllegalStateException"));
    }

    /**
     * Creates a {@link Calendar} instance fixed to UTC with the supplied epoch millis.
     */
    private static Calendar utcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    @Test
    void toTimestamp_givenNull_returnsEpochTimestamp() {
        Timestamp ts = DateConversions.toTimestamp(null, converter);
        assertNotNull(ts, "Timestamp should never be null");
        assertEquals(0L, ts.getTime(), "Null input should be converted to epoch (0 ms)");
    }

    @Test
    void toTimestamp_givenLong_returnsCorrespondingTimestamp() {
        // some arbitrary epoch ms
        Long millis = 1_620_000_000_000L;
        Timestamp ts = DateConversions.toTimestamp(millis, converter);
        assertEquals(millis, ts.getTime(), "Long milliseconds should be preserved in Timestamp");
    }

    @Test
    void toTimestamp_givenDate_returnsCorrespondingTimestamp() {
        // date with known ms
        Date date = new Date(1_620_000_000_123L);
        Timestamp ts = DateConversions.toTimestamp(date, converter);
        assertEquals(date.getTime(), ts.getTime(), "Timestamp should reflect the same epoch milliseconds as the source Date");
    }

    @Test
    void toTimestamp_givenSqlTimestamp_returnsSameTimestamp() {
        Timestamp original = new Timestamp(1_620_000_000_456L);
        Timestamp ts = DateConversions.toTimestamp(original, converter);
        // The conversion creates a new Timestamp, but the underlying epoch ms must match.
        assertEquals(original.getTime(), ts.getTime(), "Converting a java.sql.Timestamp should preserve its epoch milliseconds");
    }

    @Test
    void toTimestamp_givenLocalDateTimeAtEpoch_returnsEpochTimestamp() {
        // LocalDateTime of 1970‑01‑01T00:00:00 corresponds to epoch 0 in the default system zone.
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        Timestamp ts = DateConversions.toTimestamp(ldt, converter);
        long expected = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, ts.getTime(), "LocalDateTime should be converted to the correct epoch milliseconds");
    }

    @Test
    void toTimestamp_givenUnsupportedType_throwsException() {
        // An object that the Converter cannot turn into a long should cause an IllegalArgumentException.
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toTimestamp(unsupported, converter), "Converting an unsupported type should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulConversions")
    void toTimestamp_successful(Object from, Converter converter, String expectedIso, String description) {
        // Act
        Timestamp ts = DateConversions.toTimestamp(from, converter);
        // Assert
        assertNotNull(ts, description + " – result must not be null");
        long expectedEpoch = Instant.parse(expectedIso).toEpochMilli();
        assertEquals(expectedEpoch, ts.getTime(), description + " – epoch milliseconds must match expected value");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionConversions")
    void toTimestamp_exceptions(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // Act & Assert
        assertThrows(expectedException, () -> DateConversions.toTimestamp(from, converter), description + " – expected exception not thrown");
    }
}
