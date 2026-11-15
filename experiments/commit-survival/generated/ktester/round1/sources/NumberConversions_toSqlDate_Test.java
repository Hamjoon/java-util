package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toSqlDate_Test {

    private Converter converter;

    private Converter defaultConverter;

    /**
     * Simple placeholder used for custom‑converter tests.
     */
    static class CustomObject {
        // No fields or methods – just a distinct type.
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Force the JVM default time‑zone to UTC so that date‑time calculations are deterministic.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance – the default constructor supplies sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        // Helper objects reused across several cases
        Calendar utcCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // 2021‑06‑01T00:00:00Z
        utcCalendar.setTimeInMillis(1_622_505_600_000L);
        return Stream.of(// 1. Long value
        Arguments.of("Long value", 1_622_548_800_000L, new Converter(new ConverterOptions()), 1_622_548_800_000L), // 2. Integer value
        Arguments.of("Integer value", 12_345, new Converter(new ConverterOptions()), 12_345L), // 3. Numeric String
        Arguments.of("Numeric String", "1609459200000", new Converter(new ConverterOptions()), 1_609_459_200_000L), // 4. java.util.Date
        Arguments.of("java.util.Date", new Date(1_000L), new Converter(new ConverterOptions()), 1_000L), // 5. java.sql.Timestamp
        Arguments.of("java.sql.Timestamp", new Timestamp(1_600_000_123_456L), new Converter(new ConverterOptions()), 1_600_000_123_456L), // 6. Instant
        Arguments.of("Instant", Instant.ofEpochMilli(5_555_555L), new Converter(new ConverterOptions()), 5_555_555L), // 7. LocalDate (UTC start of day)
        Arguments.of("LocalDate", LocalDate.of(2020, 1, 2), new Converter(new ConverterOptions()), 1_577_923_200_000L), // 8. LocalDateTime (UTC)
        Arguments.of("LocalDateTime", LocalDateTime.of(2020, 1, 2, 3, 4, 5), new Converter(new ConverterOptions()), 1_577_934_245_000L), // 9. ZonedDateTime (UTC)
        Arguments.of("ZonedDateTime", ZonedDateTime.of(LocalDateTime.of(2020, 1, 2, 3, 4, 5), ZoneId.of("UTC")), new Converter(new ConverterOptions()), 1_577_934_245_000L), // 10. OffsetDateTime (UTC)
        Arguments.of("OffsetDateTime", OffsetDateTime.of(LocalDateTime.of(2020, 1, 2, 3, 4, 5), ZoneOffset.UTC), new Converter(new ConverterOptions()), 1_577_934_245_000L), // 11. Year
        Arguments.of("Year", Year.of(2021), new Converter(new ConverterOptions()), 1_609_459_200_000L), // 12. Duration
        Arguments.of("Duration", Duration.ofMillis(12_345L), new Converter(new ConverterOptions()), 12_345L), // 13. Long.MAX_VALUE
        Arguments.of("Long.MAX_VALUE", Long.MAX_VALUE, new Converter(new ConverterOptions()), Long.MAX_VALUE), // 14. Long.MIN_VALUE
        Arguments.of("Long.MIN_VALUE", Long.MIN_VALUE, new Converter(new ConverterOptions()), Long.MIN_VALUE), // 15. Negative long
        Arguments.of("Negative long", -12_345L, new Converter(new ConverterOptions()), -12_345L), // 16. Zero
        Arguments.of("Zero epoch", 0L, new Converter(new ConverterOptions()), 0L), // 17. BigDecimal (fraction truncated)
        Arguments.of("BigDecimal", new BigDecimal("12345.67"), new Converter(new ConverterOptions()), 12_345L), // 18. BigInteger
        Arguments.of("BigInteger", new BigInteger("9876543210"), new Converter(new ConverterOptions()), 9_876_543_210L), // 19. Double (fraction truncated)
        Arguments.of("Double", 12_345.999d, new Converter(new ConverterOptions()), 12_345L), // 20. Float (fraction truncated)
        Arguments.of("Float", 12_345.5f, new Converter(new ConverterOptions()), 12_345L), // 21. Short
        Arguments.of("Short", (short) 123, new Converter(new ConverterOptions()), 123L), // 22. Byte
        Arguments.of("Byte", (byte) 12, new Converter(new ConverterOptions()), 12L), // 23. AtomicInteger
        Arguments.of("AtomicInteger", new AtomicInteger(777), new Converter(new ConverterOptions()), 777L), // 24. AtomicLong
        Arguments.of("AtomicLong", new AtomicLong(8_888L), new Converter(new ConverterOptions()), 8_888L), // 25. Calendar (UTC)
        Arguments.of("Calendar (UTC)", utcCalendar, new Converter(new ConverterOptions()), 1_622_505_600_000L), // 26. Custom object with converter returning a long
        Arguments.of("Custom object → long via converter", new CustomObject(), new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                return 5_555L;
            }
        }, 5_555L), // 27. Custom object with converter returning a numeric String
        Arguments.of("Custom object → numeric String via converter", new CustomObject(), new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                return "9_999";
            }
        }, 9_999L));
    }

    private static Stream<Arguments> provideFailureConversions() {
        return Stream.of(// 1. Null source object – should throw NPE according to the JSON spec
        Arguments.of("Null source", null, new Converter(new ConverterOptions()), NullPointerException.class), // 2. Empty String – NumberFormatException
        Arguments.of("Empty String", "", new Converter(new ConverterOptions()), NumberFormatException.class), // 3. Null converter – NPE
        Arguments.of("Null converter", 12_345L, null, NullPointerException.class), // 4. Non‑numeric String – NumberFormatException
        Arguments.of("Non‑numeric String", "abc", new Converter(new ConverterOptions()), NumberFormatException.class), // 5. Boolean true – IllegalArgumentException
        Arguments.of("Boolean true", true, new Converter(new ConverterOptions()), IllegalArgumentException.class), // 6. BigDecimal overflow – ArithmeticException
        Arguments.of("BigDecimal overflow", new BigDecimal("9223372036854775808"), new Converter(new ConverterOptions()), ArithmeticException.class), // 7. Double.NaN – IllegalArgumentException
        Arguments.of("Double.NaN", Double.NaN, new Converter(new ConverterOptions()), IllegalArgumentException.class), // 8. LocalTime – IllegalArgumentException
        Arguments.of("LocalTime", LocalTime.of(12, 34, 56), new Converter(new ConverterOptions()), IllegalArgumentException.class), // 9. Custom converter returns null – NPE
        Arguments.of("Custom converter returns null", new CustomObject(), new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        }, NullPointerException.class), // 10. Converter returns Boolean – ClassCastException
        Arguments.of("Converter returns Boolean", new Date(123_456L), new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                return Boolean.TRUE;
            }
        }, ClassCastException.class));
    }

    @Test
    void toSqlDate_fromLong() {
        // 2020‑09‑13T12:26:40Z
        Long epoch = 1_600_000_000_000L;
        Date expected = new Date(epoch);
        Date actual = NumberConversions.toSqlDate(epoch, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Date created from a Long should match the supplied epoch milliseconds");
    }

    @Test
    void toSqlDate_fromInteger() {
        // truncates to seconds, but NumberConversions.toLong will handle it
        Integer epoch = (int) (1_600_000_000L);
        Date expected = new Date(epoch.longValue());
        Date actual = NumberConversions.toSqlDate(epoch, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Date created from an Integer should match the supplied value as milliseconds");
    }

    @Test
    void toSqlDate_fromString() {
        // same instant as above
        String epochStr = "1600000000000";
        Date expected = new Date(Long.parseLong(epochStr));
        Date actual = NumberConversions.toSqlDate(epochStr, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Date created from a numeric String should be parsed to the correct epoch millis");
    }

    @Test
    void toSqlDate_fromUtilDate() {
        Date now = new Date();
        Date actual = NumberConversions.toSqlDate(now, converter);
        assertEquals(now.getTime(), actual.getTime(), "Passing a java.util.Date should return a Date with the same epoch millis");
    }

    @Test
    void toSqlDate_fromSqlTimestamp() {
        Timestamp ts = new Timestamp(1_600_000_123_456L);
        Date expected = new Date(ts.getTime());
        Date actual = NumberConversions.toSqlDate(ts, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Timestamp should be converted to a Date with identical epoch millis");
    }

    @Test
    void toSqlDate_fromInstant() {
        Instant instant = Instant.ofEpochMilli(1_600_000_123_456L);
        Date expected = new Date(instant.toEpochMilli());
        Date actual = NumberConversions.toSqlDate(instant, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Instant should be converted to a Date with the same epoch millis");
    }

    @Test
    void toSqlDate_fromLocalDate() {
        LocalDate localDate = LocalDate.of(2020, Month.SEPTEMBER, 13);
        // LocalDate is interpreted as start of day in the JVM default zone
        ZonedDateTime zdt = localDate.atStartOfDay(ZoneId.systemDefault());
        Date expected = new Date(zdt.toInstant().toEpochMilli());
        Date actual = NumberConversions.toSqlDate(localDate, converter);
        assertEquals(expected.getTime(), actual.getTime(), "LocalDate should be converted to a Date representing start‑of‑day in the default zone");
    }

    @Test
    void toSqlDate_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2020, 9, 13, 12, 30, 45);
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        Date expected = new Date(zdt.toInstant().toEpochMilli());
        Date actual = NumberConversions.toSqlDate(ldt, converter);
        assertEquals(expected.getTime(), actual.getTime(), "LocalDateTime should be converted to a Date using the default zone");
    }

    @Test
    void toSqlDate_fromCalendar() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_600_000_123_456L);
        Date expected = new Date(cal.getTimeInMillis());
        Date actual = NumberConversions.toSqlDate(cal, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Calendar should be converted to a Date with the same epoch millis");
    }

    @Test
    void toSqlDate_fromNull() {
        // According to NumberConversions implementation, null is treated as 0L
        Date expected = new Date(0L);
        Date actual = NumberConversions.toSqlDate(null, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Null input should result in a Date representing epoch 0");
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (parameterised)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSuccessfulConversions")
    void toSqlDate_success(String description, Object from, Converter converter, long expectedMillis) {
        Date result = NumberConversions.toSqlDate(from, converter);
        assertEquals(expectedMillis, result.getTime(), () -> "Failed case: " + description);
    }

    /* --------------------------------------------------------------
     *  Failure conversion test cases (parameterised)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideFailureConversions")
    void toSqlDate_failure(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toSqlDate(from, converter), () -> "Expected exception for case: " + description);
    }
}
