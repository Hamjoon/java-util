package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toInstant_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // standard Java time types we are exercising.
        converter = new Converter(new ConverterOptions());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release; the static converter will be GC‑ed after the suite.
        converter = null;
    }

    private static Stream<Arguments> happyPathProvider() {
        ???;
    }

    private static Calendar calendarUtc(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    @DisplayName("Convert ISO‑8601 string to Instant")
    void testStringIso8601() {
        String iso = "2023-08-15T12:34:56Z";
        Instant expected = Instant.parse(iso);
        Instant actual = StringConversions.toInstant(iso, converter);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Convert java.util.Date to Instant")
    void testDate() {
        // epoch start
        Date date = new Date(0L);
        Instant expected = Instant.ofEpochMilli(0L);
        Instant actual = StringConversions.toInstant(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Convert java.sql.Timestamp to Instant")
    void testTimestamp() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Timestamp ts = Timestamp.from(now);
        Instant actual = StringConversions.toInstant(ts, converter);
        assertEquals(now, actual);
    }

    @Test
    @DisplayName("Convert ZonedDateTime to Instant")
    void testZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneOffset.UTC);
        Instant expected = zdt.toInstant();
        Instant actual = StringConversions.toInstant(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Convert Instant (already) to Instant")
    void testInstantItself() {
        Instant now = Instant.now();
        Instant actual = StringConversions.toInstant(now, converter);
        assertEquals(now, actual);
    }

    @Test
    @DisplayName("Null source yields null")
    void testNullSource() {
        assertNull(StringConversions.toInstant(null, defaultConverter));
    }

    @Test
    @DisplayName("Unparsable string yields null")
    void testInvalidString() {
        String bad = "not-a-date-time";
        assertNull(StringConversions.toInstant(bad, converter));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("happyPathProvider")
    void testToInstant_HappyPath(String description, Object source, Instant expected) {
        Instant actual = StringConversions.toInstant(source, defaultConverter);
        assertEquals(expected, actual, description);
    }

    @Test
    @DisplayName("Unsupported type (Integer) yields null")
    void testUnsupportedTypeReturnsNull() {
        Integer src = 12345;
        assertNull(StringConversions.toInstant(src, defaultConverter));
    }

    @Test
    @DisplayName("MonthDay without year yields null")
    void testMonthDayWithoutYear() {
        MonthDay src = MonthDay.of(5, 10);
        assertNull(StringConversions.toInstant(src, defaultConverter));
    }

    @Test
    @DisplayName("Malformed simple string returns null")
    void testMalformedStringReturnsNull() {
        String bad = "not-a-date";
        assertNull(StringConversions.toInstant(bad, defaultConverter));
    }

    @Test
    @DisplayName("Malformed ISO string throws DateTimeParseException")
    void testMalformedIsoStringThrows() {
        String bad = "2023-02-30T25:61:61Z";
        assertThrows(DateTimeParseException.class, () -> StringConversions.toInstant(bad, defaultConverter));
    }

    @Test
    @DisplayName("Custom converter that supplies ZonedDateTime")
    void testCustomConverterSuppliesZonedDateTime() {
        Converter custom = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneOffset.UTC);
            }
        };
        Instant expected = Instant.parse("2023-05-10T12:30:45Z");
        Instant actual = StringConversions.toInstant("custom", custom);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Custom converter throws unchecked exception – should propagate")
    void testCustomConverterThrowsException() {
        Converter exploding = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new IllegalStateException("conversion failed");
            }
        };
        assertThrows(IllegalStateException.class, () -> StringConversions.toInstant("any", exploding));
    }

    @Test
    @DisplayName("Custom converter returns wrong type – ClassCastException expected")
    void testCustomConverterReturnsWrongType() {
        Converter badReturn = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return "not a ZonedDateTime";
            }
        };
        assertThrows(ClassCastException.class, () -> StringConversions.toInstant("any", badReturn));
    }

    @Test
    @DisplayName("Custom converter returns null for non‑null source – NullPointerException")
    void testCustomConverterReturnsNull() {
        Converter returnsNull = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> StringConversions.toInstant(new Object(), returnsNull));
    }
}
