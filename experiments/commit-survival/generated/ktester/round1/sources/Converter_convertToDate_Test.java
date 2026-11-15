package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToDate_Test {

    private static Stream<Arguments> dateConversionProvider() {
        // java.util.Date epoch
        Date utilDateEpoch = new Date(0L);
        // java.sql.Date epoch (subclass of java.util.Date)
        java.sql.Date sqlDateEpoch = new java.sql.Date(0L);
        // java.sql.Timestamp epoch
        Timestamp tsEpoch = new Timestamp(0L);
        // Calendar UTC at epoch
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(0L);
        // Long values
        Long zeroLong = 0L;
        // one day before epoch
        Long negativeLong = -86_400_000L;
        return Stream.of(Arguments.of("java.util.Date epoch (0L)", utilDateEpoch, "1970-01-01T00:00:00Z"), Arguments.of("java.sql.Date epoch (0L)", sqlDateEpoch, "1970-01-01T00:00:00Z"), Arguments.of("java.sql.Timestamp epoch (0L)", tsEpoch, "1970-01-01T00:00:00Z"), Arguments.of("Calendar UTC epoch", utcCal, "1970-01-01T00:00:00Z"), Arguments.of("Long 0L (epoch milliseconds)", zeroLong, "1970-01-01T00:00:00Z"), Arguments.of("Long -86400000L (one day before epoch)", negativeLong, "1969-12-31T00:00:00Z"));
    }

    private static Stream<Arguments> stringConversionProvider() {
        return Stream.of(// ISO‑8601 UTC string
        Arguments.of("ISO‑8601 UTC string", "1970-01-01T00:00:00Z", "1970-01-01T00:00:00Z"), // Date‑only string (interpreted as midnight UTC)
        Arguments.of("Date‑only string", "2023-05-01", "2023-05-01T00:00:00Z"), // Local date‑time without zone (treated as UTC)
        Arguments.of("Local date‑time without zone", "2023-05-01T12:34:56", "2023-05-01T12:34:56Z"), // Unsupported format should raise IllegalArgumentException
        Arguments.of("Unsupported format string", "05/01/2023 12:34", IllegalArgumentException.class));
    }

    private static Stream<Arguments> nullAndUnsupportedProvider() {
        return Stream.of(Arguments.of("Null input", null, null), Arguments.of("Unsupported object type", new Object(), IllegalArgumentException.class), Arguments.of("Unsupported Double type", 3.14159, IllegalArgumentException.class));
    }

    private static Stream<Arguments> unsupportedNumericProvider() {
        return Stream.of(Arguments.of("Integer instance", Integer.valueOf(0)), Arguments.of("Short instance", Short.valueOf((short) 0)), Arguments.of("Byte instance", Byte.valueOf((byte) 0)), Arguments.of("BigInteger instance", new BigInteger("0")), Arguments.of("BigDecimal instance", new BigDecimal("0")));
    }

    @Test
    void convertFromDate_returnsSameInstance() {
        Date now = new Date();
        Date result = Converter.convertToDate(now);
        assertNotNull(result);
        assertEquals(now, result);
    }

    @Test
    void convertFromNull_returnsNull() {
        assertNull(Converter.convertToDate(null));
    }

    @Test
    void convertFromTimestamp_convertsToDate() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Date result = Converter.convertToDate(ts);
        assertNotNull(result);
        assertEquals(new Date(ts.getTime()), result);
    }

    @Test
    void convertFromLocalDateTime_convertsToDate() {
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 15, 12, 30, 45);
        Date expected = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        Date result = Converter.convertToDate(ldt);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void convertFromLocalDate_convertsToDateAtStartOfDay() {
        LocalDate ld = LocalDate.of(2023, 5, 15);
        Date expected = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date result = Converter.convertToDate(ld);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void convertFromCalendar_convertsToDate() {
        Calendar cal = new GregorianCalendar(2023, Calendar.MAY, 15, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);
        Date expected = cal.getTime();
        Date result = Converter.convertToDate(cal);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void convertFromLongMilliseconds_convertsToDate() {
        long millis = System.currentTimeMillis();
        Date expected = new Date(millis);
        Date result = Converter.convertToDate(millis);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void convertFromString_parsesSupportedFormats() {
        // The library supports ISO‑8601 strings for dates/times.
        String iso = "2023-05-15T12:30:45Z";
        Date expected = Date.from(java.time.Instant.parse(iso));
        Date result = Converter.convertToDate(iso);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateConversionProvider")
    void dateConversions(String description, Object input, String expectedIso) {
        Date result = Converter.convertToDate(input);
        assertNotNull(result, "Result should not be null for " + description);
        Instant expectedInstant = Instant.parse(expectedIso);
        assertEquals(Date.from(expectedInstant), result, "Converted Date does not match expected for " + description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringConversionProvider")
    void stringConversions(String description, String input, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected exception case
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convertToDate(input), "Expected exception for input: " + description);
        } else {
            // Expected ISO‑8601 instant string
            Date result = Converter.convertToDate(input);
            assertNotNull(result, "Result should not be null for " + description);
            Instant expectedInstant = Instant.parse((String) expected);
            assertEquals(Date.from(expectedInstant), result, "Converted Date does not match expected for " + description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndUnsupportedProvider")
    void nullAndUnsupported(String description, Object input, Object expected) {
        if (expected == null) {
            assertNull(Converter.convertToDate(input), "Expected null result for " + description);
        } else if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convertToDate(input), "Expected exception for " + description);
        } else {
            fail("Invalid test definition for " + description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedNumericProvider")
    void unsupportedNumericTypes(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToDate(input), "Converting unsupported numeric type should throw IllegalArgumentException for " + description);
    }
}
