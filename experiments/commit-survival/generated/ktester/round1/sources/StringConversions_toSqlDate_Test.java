package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toSqlDate_Test {

    /**
     * Shared {@link Converter} used by all test methods.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} used for the majority of test cases (default behaviour).
     */
    private static Converter defaultConverter;

    /**
     * Simple holder for the parameterised test arguments.
     */
    private static final class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final Long expectedEpochMilli;

        ArgumentsHolder(Object from, Converter converter, Long expectedEpochMilli) {
            this.from = from;
            this.converter = converter;
            this.expectedEpochMilli = expectedEpochMilli;
        }

        @Override
        public String toString() {
            return String.valueOf(from);
        }
    }

    /**
     * Dummy domain class used for custom conversion tests.
     */
    private static final class Foo {
    }

    /**
     * Custom {@link Converter} that knows how to turn a {@link Foo}
     * into {@link Instant#ofEpochMilli(long)} with a fixed epoch of 9999.
     */
    private static final class FooConverter extends Converter {

        FooConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object source, Class<T> target) {
            if (source instanceof Foo && target == Instant.class) {
                @SuppressWarnings("unchecked")
                T result = (T) Instant.ofEpochMilli(9999L);
                return result;
            }
            return super.convert(source, target);
        }
    }

    /**
     * Converter that deliberately throws a {@link RuntimeException}
     * when asked to convert a {@link Foo}.
     */
    private static final class ThrowingConverter extends Converter {

        ThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object source, Class<T> target) {
            if (source instanceof Foo) {
                throw new RuntimeException("forced failure");
            }
            return super.convert(source, target);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter with default options – this mimics the library's
        // typical usage and allows the internal conversion logic to run un‑mocked.
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<ArgumentsHolder> normalConversionProvider() {
        return Stream.of(// null source – should yield null (converter may be null as well)
        new ArgumentsHolder(null, null, null), // java.util.Date(0)
        new ArgumentsHolder(new Date(0L), null, 0L), // Instant.ofEpochMilli(123456789)
        new ArgumentsHolder(Instant.ofEpochMilli(123456789L), null, 123456789L), // ISO‑8601 UTC string
        new ArgumentsHolder("2023-01-01T12:34:56Z", null, 1672576496000L), // java.sql.Timestamp
        new ArgumentsHolder(Timestamp.valueOf("2022-12-31 23:59:59"), null, 1672531199000L), // Long epoch milliseconds
        new ArgumentsHolder(Long.valueOf(1609459200000L), null, 1609459200000L), // Calendar set to 2021‑06‑15 08:30:00 UTC
        new ArgumentsHolder(createUtcCalendar(2021, 6, 15, 8, 30, 0), null, Instant.ofEpochSecond(1623745800L).toEpochMilli()), // Date‑only string
        new ArgumentsHolder("2023-02-28", null, 1675209600000L), // LocalDate
        new ArgumentsHolder(LocalDate.of(2022, 12, 31), null, 1672444800000L), // ZonedDateTime (America/New_York)
        new ArgumentsHolder(ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York")), null, 1672652799000L), // OffsetDateTime
        new ArgumentsHolder(OffsetDateTime.parse("2023-03-01T12:00:00+02:00"), null, 1677664800000L), // java.sql.Date
        new ArgumentsHolder(java.sql.Date.valueOf("2022-01-01"), null, 1640995200000L), // Unsupported object – should return null
        new ArgumentsHolder(new Object(), null, null), // Malformed string – should return null
        new ArgumentsHolder("not-a-date", null, null), // Custom Foo with custom converter returning epoch 9999
        new ArgumentsHolder(new Foo(), new FooConverter(), 9999L));
    }

    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        return cal;
    }

    @Test
    void toSqlDate_returnsNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = StringConversions.toSqlDate(source, converter);
        // THEN
        assertNull(result, "Expected null when the source object is null");
    }

    @Test
    void toSqlDate_convertsDateUnchanged() {
        // GIVEN
        Date now = new Date();
        // WHEN
        Date result = StringConversions.toSqlDate(now, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(now.getTime(), result.getTime(), "The epoch millisecond value should be identical to the original Date");
    }

    @Test
    void toSqlDate_convertsInstantCorrectly() {
        // GIVEN
        Instant instant = Instant.now();
        // WHEN
        Date result = StringConversions.toSqlDate(instant, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(instant.toEpochMilli(), result.getTime(), "The Date should represent the same instant (epoch milliseconds)");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("normalConversionProvider")
    void toSqlDate_variousInputs(Object from, Converter converter, Long expectedEpochMilli) {
        Converter used = (converter != null) ? converter : defaultConverter;
        Date result = StringConversions.toSqlDate(from, used);
        if (expectedEpochMilli == null) {
            assertNull(result, "Expected null for input: " + from);
        } else {
            assertNotNull(result, "Result should not be null for input: " + from);
            assertEquals(expectedEpochMilli.longValue(), result.getTime(), "Incorrect epoch milliseconds for input: " + from);
        }
    }

    @Test
    void toSqlDate_invalidIsoDateThrowsDateTimeParseException() {
        String badDate = "2023-02-30";
        assertThrows(DateTimeParseException.class, () -> StringConversions.toSqlDate(badDate, defaultConverter), "Parsing an invalid ISO‑8601 date should throw DateTimeParseException");
    }

    @Test
    void toSqlDate_malformedNumberStringThrowsNumberFormatException() {
        String badNumber = "123abc";
        assertThrows(NumberFormatException.class, () -> StringConversions.toSqlDate(badNumber, defaultConverter), "Parsing a non‑numeric string as a number should throw NumberFormatException");
    }

    @Test
    void toSqlDate_unsupportedObjectWithoutConverterThrowsIllegalArgumentException() {
        Object unsupported = new Object();
        // Pass a null converter to force the internal logic to hit the illegal‑argument path.
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toSqlDate(unsupported, null), "Unsupported type with null converter should throw IllegalArgumentException");
    }

    @Test
    void toSqlDate_converterThrowsRuntimeExceptionPropagates() {
        Foo badFoo = new Foo();
        Converter throwing = new ThrowingConverter();
        assertThrows(RuntimeException.class, () -> StringConversions.toSqlDate(badFoo, throwing), "RuntimeException from custom converter should propagate");
    }

    @Test
    void toSqlDate_bothParametersNullReturnsNull() {
        // Both arguments null – should simply return null, no NPE.
        Date result = StringConversions.toSqlDate(null, null);
        assertNull(result);
    }
}
