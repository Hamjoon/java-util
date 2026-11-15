package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toCalendar_Test {

    /**
     * A real Converter instance used for the tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default options are sufficient
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the test tidy
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        // 2021‑06‑01T12:00:00Z
        long epoch = 1_622_548_800_000L;
        return Stream.of(// plain Long
        Arguments.of("Convert a plain Long representing epoch milliseconds", epoch, epoch), // java.util.Date
        Arguments.of("Convert a java.util.Date object", new Date(epoch), epoch), // java.time.Instant
        Arguments.of("Convert an Instant", Instant.ofEpochMilli(epoch), epoch), // java.time.LocalDateTime (system default zone)
        Arguments.of("Convert a LocalDateTime (system default zone)", LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()), epoch), // java.time.ZonedDateTime
        Arguments.of("Convert a ZonedDateTime", ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()), epoch), // java.math.BigInteger
        Arguments.of("Convert a BigInteger", new BigInteger("1622548800000"), epoch), // java.math.BigDecimal
        Arguments.of("Convert a BigDecimal", new BigDecimal("1622548800000"), epoch));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of("Boolean value", true), Arguments.of("Arbitrary Object", new Object()), Arguments.of("Year instance", Year.of(2021)));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("Non‑numeric string", "not-a-number"), Arguments.of("Mixed alphanumeric string", "1234abc"));
    }

    private static Stream<Arguments> overflowProvider() {
        return Stream.of(Arguments.of("BigDecimal exceeding Long.MAX_VALUE", // Long.MAX_VALUE + 1
        new BigDecimal("9223372036854775808")), Arguments.of("BigInteger less than Long.MIN_VALUE", // Long.MIN_VALUE - 1
        new BigInteger("-9223372036854775809")));
    }

    @Test
    void toCalendar_givenLong_returnsCalendarWithSameInstant() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMillis = 1_640_995_200_000L;
        // WHEN
        Calendar cal = NumberConversions.toCalendar(epochMillis, converter);
        // THEN
        assertNotNull(cal, "Calendar should not be null");
        assertEquals(epochMillis, cal.getTimeInMillis(), "Calendar should contain the same epoch milliseconds as the source long");
    }

    @Test
    void toCalendar_givenStringNumber_returnsCalendarWithParsedInstant() {
        // GIVEN
        // same instant as above, expressed as a String
        String epochString = "1640995200000";
        // WHEN
        Calendar cal = NumberConversions.toCalendar(epochString, converter);
        // THEN
        assertNotNull(cal);
        assertEquals(Long.parseLong(epochString), cal.getTimeInMillis(), "String that represents a number should be converted to the same epoch millis");
    }

    @Test
    void toCalendar_givenNull_returnsCalendarAtEpochZero() {
        // GIVEN
        Object nullValue = null;
        // WHEN
        Calendar cal = NumberConversions.toCalendar(nullValue, converter);
        // THEN
        assertNotNull(cal);
        assertEquals(0L, cal.getTimeInMillis(), "Null input should result in a Calendar at epoch (0 milliseconds)");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toCalendar_validConversions(String description, Object from, long expectedMillis) {
        Calendar cal = NumberConversions.toCalendar(from, converter);
        assertNotNull(cal, "Calendar should not be null");
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar epoch millis should match expected value");
    }

    @Test
    void toCalendar_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toCalendar(null, converter), "Null source should cause NPE");
    }

    @Test
    void toCalendar_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toCalendar(1622548800000L, null), "Null converter should cause NPE");
    }

    @Test
    void toCalendar_nullFromAndConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toCalendar(null, null), "Both arguments null should cause NPE");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedSourceProvider")
    void toCalendar_unsupportedSource_throwsIllegalArgumentException(String description, Object from) {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCalendar(from, converter), description + " should trigger IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidStringProvider")
    void toCalendar_invalidString_throwsNumberFormatException(String description, String value) {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toCalendar(value, converter), description + " should trigger NumberFormatException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("overflowProvider")
    void toCalendar_numericOverflow_throwsArithmeticException(String description, Object from) {
        assertThrows(ArithmeticException.class, () -> NumberConversions.toCalendar(from, converter), description + " should trigger ArithmeticException");
    }
}
