package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Date;
import java.sql.Time;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class Converter_convertToCalendar_Test {

    // Helper holder class for parameterized tests
    private static class UnsupportedCase {

        final String typeName;

        final Object value;

        UnsupportedCase(String typeName, Object value) {
            this.typeName = typeName;
            this.value = value;
        }

        @Override
        public String toString() {
            return typeName;
        }
    }

    // -------------------------------------------------------------------------
    private static Stream<UnsupportedCase> unsupportedSourceProvider() {
        return Stream.of(new UnsupportedCase("String", "2022-03-15T10:20:30Z"), new UnsupportedCase("Integer", 12345), new UnsupportedCase("Boolean", true), new UnsupportedCase("Double", 123.45), new UnsupportedCase("BigDecimal", new java.math.BigDecimal("123.45")), new UnsupportedCase("LocalDate", LocalDate.of(2022, 3, 15)), new UnsupportedCase("Instant", Instant.now()), new UnsupportedCase("java.sql.Time", Time.valueOf(LocalTime.of(12, 34, 56))), new UnsupportedCase("List", Arrays.asList(1, 2, 3)), new UnsupportedCase("AtomicInteger", new AtomicInteger(5)));
    }

    @Test
    @DisplayName("convertToCalendar should return null when input is null")
    void testConvertNull() {
        // given
        Object input = null;
        // when
        Calendar result = Converter.convertToCalendar(input);
        // then
        assertNull(result, "Conversion of null should yield null");
    }

    @Test
    @DisplayName("convertToCalendar should correctly convert java.util.Date")
    void testConvertFromDate() {
        // given
        Date date = new Date();
        // when
        Calendar calendar = Converter.convertToCalendar(date);
        // then
        assertNotNull(calendar, "Resulting Calendar must not be null");
        assertEquals(date.getTime(), calendar.getTimeInMillis(), "Calendar time must match the original Date time");
    }

    @Test
    @DisplayName("convertToCalendar should correctly convert an existing Calendar")
    void testConvertFromCalendar() {
        // given
        Calendar original = Calendar.getInstance();
        original.setTimeInMillis(System.currentTimeMillis());
        // when
        Calendar converted = Converter.convertToCalendar(original);
        // then
        assertNotNull(converted, "Resulting Calendar must not be null");
        assertEquals(original.getTimeInMillis(), converted.getTimeInMillis(), "Converted Calendar must preserve the same instant");
    }

    @Test
    @DisplayName("convertToCalendar should correctly convert java.time.LocalDateTime")
    void testConvertFromLocalDateTime() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 15, 30, 45);
        long expectedMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        // when
        Calendar calendar = Converter.convertToCalendar(ldt);
        // then
        assertNotNull(calendar, "Resulting Calendar must not be null");
        assertEquals(expectedMillis, calendar.getTimeInMillis(), "Calendar should represent the same instant as the LocalDateTime");
    }

    @Test
    @DisplayName("convertToCalendar should correctly convert java.sql.Timestamp")
    void testConvertFromTimestamp() {
        // given
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        // when
        Calendar calendar = Converter.convertToCalendar(timestamp);
        // then
        assertNotNull(calendar, "Resulting Calendar must not be null");
        assertEquals(timestamp.getTime(), calendar.getTimeInMillis(), "Calendar time must match the Timestamp time");
    }

    @Test
    @DisplayName("convertToCalendar should correctly convert a Long representing epoch milliseconds")
    void testConvertFromLong() {
        // given
        Long epochMillis = System.currentTimeMillis();
        // when
        Calendar calendar = Converter.convertToCalendar(epochMillis);
        // then
        assertNotNull(calendar, "Resulting Calendar must not be null");
        assertEquals(epochMillis.longValue(), calendar.getTimeInMillis(), "Calendar should represent the same epoch milliseconds as the Long input");
    }

    @Test
    @DisplayName("convertToCalendar should correctly convert an existing Calendar (identity)")
    void testConvertFromCalendarIdentity() {
        // given
        Calendar original = Calendar.getInstance();
        original.setTimeInMillis(System.currentTimeMillis());
        // when
        Calendar converted = Converter.convertToCalendar(original);
        // then
        assertNotNull(converted, "Resulting Calendar must not be null");
        // The specification (and existing test) expects the *same* instance to be returned
        assertSame(original, converted, "When source is already a Calendar, the same instance should be returned");
    }

    @Test
    @DisplayName("convertToCalendar should correctly convert java.sql.Date (date‑only) to midnight")
    void testConvertFromSqlDate() {
        // given: 2022‑03‑15
        java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2022, 3, 15));
        // when
        Calendar calendar = Converter.convertToCalendar(sqlDate);
        // then
        assertNotNull(calendar, "Resulting Calendar must not be null");
        // Expect midnight of that day in the default time‑zone
        ZonedDateTime expected = sqlDate.toLocalDate().atStartOfDay(ZoneId.systemDefault());
        assertEquals(expected.toInstant().toEpochMilli(), calendar.getTimeInMillis(), "Calendar should be set to midnight of the sql.Date in the system default zone");
    }

    @Test
    @DisplayName("convertToCalendar should correctly convert ZonedDateTime preserving the instant")
    void testConvertFromZonedDateTime() {
        // given: 2022‑03‑15T10:20:30+02:00[Europe/Paris] (which is 08:20:30Z)
        ZonedDateTime zdt = ZonedDateTime.of(2022, 3, 15, 10, 20, 30, 0, ZoneId.of("Europe/Paris"));
        long expectedMillis = zdt.toInstant().toEpochMilli();
        // when
        Calendar calendar = Converter.convertToCalendar(zdt);
        // then
        assertNotNull(calendar, "Resulting Calendar must not be null");
        assertEquals(expectedMillis, calendar.getTimeInMillis(), "Calendar should represent the same instant as the ZonedDateTime, regardless of original zone");
    }

    @Test
    @DisplayName("convertToCalendar should correctly convert negative epoch milliseconds")
    void testConvertFromNegativeLong() {
        // given: -1 ms (one millisecond before the Unix epoch)
        Long negative = -1L;
        // when
        Calendar calendar = Converter.convertToCalendar(negative);
        // then
        assertNotNull(calendar, "Resulting Calendar must not be null");
        assertEquals(negative.longValue(), calendar.getTimeInMillis(), "Calendar should represent the same negative epoch milliseconds");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported String source")
    void testConvertFromStringUnsupported() {
        // given
        String input = "2022-03-15T10:20:30Z";
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "A plain String is not a supported source type; should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported Integer source")
    void testConvertFromIntegerUnsupported() {
        // given
        Integer input = 12345;
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "Unsupported numeric type Integer should cause IllegalArgumentException");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported Boolean source")
    void testConvertFromBooleanUnsupported() {
        // given
        Boolean input = Boolean.TRUE;
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "Unsupported source type Boolean should trigger IllegalArgumentException");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported Double source")
    void testConvertFromDoubleUnsupported() {
        // given
        Double input = 123.45;
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "Unsupported numeric source type Double should trigger IllegalArgumentException");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported BigDecimal source")
    void testConvertFromBigDecimalUnsupported() {
        // given
        java.math.BigDecimal input = new java.math.BigDecimal("123.45");
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "Unsupported source type BigDecimal should trigger IllegalArgumentException");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported LocalDate source")
    void testConvertFromLocalDateUnsupported() {
        // given
        LocalDate input = LocalDate.of(2022, 3, 15);
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "LocalDate (date‑only) is not a supported source for Calendar conversion");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported Instant source")
    void testConvertFromInstantUnsupported() {
        // given
        Instant input = Instant.now();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "Instant is not listed among supported sources; conversion should fail");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported java.sql.Time source")
    void testConvertFromSqlTimeUnsupported() {
        // given
        Time input = Time.valueOf(LocalTime.of(12, 34, 56));
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "java.sql.Time (time‑only) cannot be converted to a Calendar");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported List source")
    void testConvertFromListUnsupported() {
        // given
        List<Integer> input = Arrays.asList(1, 2, 3);
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "A collection type (List) is not a supported source");
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for unsupported AtomicInteger source")
    void testConvertFromAtomicIntegerUnsupported() {
        // given
        AtomicInteger input = new AtomicInteger(5);
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(input), "Atomic wrapper types are not supported as source for Calendar conversion");
    }

    @Test
    @DisplayName("convertToCalendar should handle Long.MAX_VALUE within Calendar range or throw")
    void testConvertFromLongMaxValue() {
        // given
        Long max = Long.MAX_VALUE;
        // when / then
        try {
            Calendar calendar = Converter.convertToCalendar(max);
            assertNotNull(calendar, "Calendar should not be null when conversion succeeds");
            // Verify that the instant is exactly Long.MAX_VALUE if Calendar accepted it
            assertEquals(max.longValue(), calendar.getTimeInMillis(), "Calendar should represent the same epoch milliseconds as Long.MAX_VALUE");
        } catch (IllegalArgumentException | ArithmeticException e) {
            // Acceptable outcome if the value is out of Calendar's supported range
            assertTrue(e instanceof IllegalArgumentException, "Expected IllegalArgumentException for overflow, got: " + e);
        }
    }

    @Test
    @DisplayName("convertToCalendar should throw IllegalArgumentException for Long.MIN_VALUE (underflow)")
    void testConvertFromLongMinValue() {
        // given
        Long min = Long.MIN_VALUE;
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(min), "Long.MIN_VALUE is far before Calendar's minimum; should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: convertToCalendar should reject {0}")
    @MethodSource("unsupportedSourceProvider")
    @DisplayName("convertToCalendar should reject unsupported source types")
    void testUnsupportedSources(UnsupportedCase uc) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToCalendar(uc.value), "Source type " + uc.typeName + " should trigger IllegalArgumentException");
    }
}
