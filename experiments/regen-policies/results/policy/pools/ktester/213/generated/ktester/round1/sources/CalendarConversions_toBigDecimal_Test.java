package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toBigDecimal_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter does not need any special options for this test
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields and time‑zone.
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are zero‑based, hence {@code month - 1}
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validCalendarProvider() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        // 1970‑01‑01T00:00:00Z  → epochMillis = 0 → 0 / GRAND = 0
        Calendar epochZero = createCalendar(1970, 1, 1, 0, 0, 0, 0, utc);
        BigDecimal expectedZero = new BigDecimal(0).divide(BigDecimalConversions.GRAND);
        // 2020‑09‑13T12:26:40Z → epochMillis = 1_600_000_000_000 → seconds = 1_600_000_000
        Calendar positive = createCalendar(2020, 9, 13, 12, 26, 40, 0, utc);
        long positiveMillis = positive.getTimeInMillis();
        BigDecimal expectedPositive = new BigDecimal(positiveMillis).divide(BigDecimalConversions.GRAND);
        // 1969‑12‑31T00:00:00Z → epochMillis = -86_400_000 → seconds = -86_400
        Calendar negative = createCalendar(1969, 12, 31, 0, 0, 0, 0, utc);
        long negativeMillis = negative.getTimeInMillis();
        BigDecimal expectedNegative = new BigDecimal(negativeMillis).divide(BigDecimalConversions.GRAND);
        return Stream.of(arguments(epochZero, expectedZero, "Epoch start (1970‑01‑01T00:00:00Z) should yield 0"), arguments(positive, expectedPositive, "Typical positive epoch (2020‑09‑13T12:26:40Z)"), arguments(negative, expectedNegative, "Negative epoch (one day before Unix epoch)"));
    }

    @Test
    void testEpochZeroReturnsZero() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, TimeZone.getTimeZone("UTC"));
        BigDecimal result = CalendarConversions.toBigDecimal(cal, converter);
        assertEquals(BigDecimal.ZERO, result, "Epoch 0 should produce BigDecimal 0");
    }

    @Test
    void testWholeSecondEpoch() {
        // 2000‑01‑01T00:00:00Z = 946684800000 ms since epoch
        Calendar cal = createCalendar(2000, 1, 1, 0, 0, 0, 0, TimeZone.getTimeZone("UTC"));
        // should be 946684800000
        long epochMillis = cal.getTimeInMillis();
        BigDecimal expected = new BigDecimal(epochMillis).divide(BigDecimal.valueOf(1000));
        BigDecimal result = CalendarConversions.toBigDecimal(cal, converter);
        assertEquals(expected, result, "Conversion of a whole‑second epoch should match expected seconds as BigDecimal");
    }

    @Test
    void testWithMillisecondsComponent() {
        // 2023‑03‑15T12:34:56.789Z
        Calendar cal = createCalendar(2023, 3, 15, 12, 34, 56, 789, TimeZone.getTimeZone("UTC"));
        // includes the 789 ms
        long epochMillis = cal.getTimeInMillis();
        BigDecimal expected = new BigDecimal(epochMillis).divide(BigDecimal.valueOf(1000));
        BigDecimal result = CalendarConversions.toBigDecimal(cal, converter);
        assertEquals(expected, result, "Conversion should correctly include the millisecond component in the division");
    }

    /**
     * Parameterized test that verifies conversion of various {@link Calendar}
     * instances to the expected {@link BigDecimal}.
     *
     * The expected value is computed using the same formula as the implementation:
     * {@code new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND)}.
     */
    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("validCalendarProvider")
    void testValidCalendars(Object from, BigDecimal expected, String description) {
        // The method under test does not use the Converter, so passing the real instance is fine.
        BigDecimal result = CalendarConversions.toBigDecimal(from, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void testNullFromThrowsNPE() {
        // Passing null for the 'from' argument should cause a NullPointerException
        assertThrows(NullPointerException.class, () -> CalendarConversions.toBigDecimal(null, converter), "Null 'from' should raise NullPointerException");
    }

    @Test
    void testNonCalendarFromThrowsClassCastException() {
        // Passing an object that is not a Calendar should result in ClassCastException
        String notACalendar = "I am not a Calendar";
        assertThrows(ClassCastException.class, () -> CalendarConversions.toBigDecimal(notACalendar, converter), "Non‑Calendar 'from' should raise ClassCastException");
    }

    @Test
    void testCalendarWithNullDateThrowsNPE() {
        // Create a mock Calendar that returns null from getTime()
        Calendar mockCalendar = Mockito.mock(Calendar.class);
        Mockito.when(mockCalendar.getTime()).thenReturn(null);
        // The method will attempt to call .getTime().getTime() → NPE
        assertThrows(NullPointerException.class, () -> CalendarConversions.toBigDecimal(mockCalendar, converter), "Calendar whose getTime() returns null should raise NullPointerException");
    }
}
