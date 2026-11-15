package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toDouble_Test {

    private Converter converter;

    /**
     * Some time‑zones used for the tests.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5/‑4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    private static final TimeZone GMT_PLUS_5_30 = TimeZone.getTimeZone("GMT+05:30");

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is not used by the implementation of toDouble,
        // but the method signature requires it.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields.
     */
    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are zero‑based
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    private static Stream<Arguments> normalConversions() {
        Calendar epoch = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epoch.clear();
        epoch.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        epoch.set(Calendar.MILLISECOND, 0);
        Calendar typical = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        typical.clear();
        typical.set(2023, Calendar.JANUARY, 1, 12, 34, 56);
        typical.set(Calendar.MILLISECOND, 0);
        Calendar preEpoch = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        preEpoch.clear();
        preEpoch.set(1960, Calendar.JANUARY, 1, 0, 0, 0);
        preEpoch.set(Calendar.MILLISECOND, 0);
        return Stream.of(Arguments.of("Epoch start calendar should be converted to 0.0 seconds.", epoch, epoch.getTimeInMillis() / 1000.0), Arguments.of("Typical modern date; verify correct conversion to seconds as double.", typical, typical.getTimeInMillis() / 1000.0), Arguments.of("Date before Unix epoch; conversion should yield a negative double value.", preEpoch, preEpoch.getTimeInMillis() / 1000.0));
    }

    private static Stream<Arguments> edgeCaseConversions() {
        // Far‑future date: 3000‑01‑01T00:00:00Z
        Calendar farFuture = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        farFuture.clear();
        farFuture.set(3000, Calendar.JANUARY, 1, 0, 0, 0);
        farFuture.set(Calendar.MILLISECOND, 0);
        // Calendar with timezone offset +05:30 set to 2022‑03‑14T09:15:30+05:30
        Calendar offsetCal = new GregorianCalendar(TimeZone.getTimeZone("GMT+05:30"));
        offsetCal.clear();
        offsetCal.set(2022, Calendar.MARCH, 14, 9, 15, 30);
        offsetCal.set(Calendar.MILLISECOND, 0);
        return Stream.of(Arguments.of("Far‑future date to ensure the method handles large epoch values without overflow.", farFuture, farFuture.getTimeInMillis() / 1000.0), Arguments.of("Calendar with non‑UTC timezone; conversion should use the absolute instant (epoch millis) independent of the zone.", offsetCal, offsetCal.getTimeInMillis() / 1000.0));
    }

    private static Stream<Arguments> errorHandlingCases() {
        return Stream.of(Arguments.of("Passing a null `from` argument should result in a NullPointerException at the cast.", null, NullPointerException.class), Arguments.of("Passing an object that is not a Calendar should cause a ClassCastException.", "not a calendar", ClassCastException.class));
    }

    private static Stream<Arguments> customCalendarCases() {
        // Calendar where getTime() returns null
        Calendar nullTimeCal = new GregorianCalendar() {

            @Override
            public Date getTime() {
                return null;
            }
        };
        nullTimeCal.clear();
        nullTimeCal.set(2021, Calendar.JUNE, 15, 12, 0, 0);
        nullTimeCal.set(Calendar.MILLISECOND, 0);
        // Calendar where getTime() throws IllegalStateException
        Calendar throwingCal = new GregorianCalendar() {

            @Override
            public Date getTime() {
                throw new IllegalStateException("forced exception");
            }
        };
        throwingCal.clear();
        throwingCal.set(2021, Calendar.JUNE, 15, 12, 0, 0);
        throwingCal.set(Calendar.MILLISECOND, 0);
        return Stream.of(Arguments.of("Calendar.getTime() returns null, causing a NullPointerException when the method attempts to call .getTime() on the null Date.", nullTimeCal, NullPointerException.class), Arguments.of("Calendar.getTime() deliberately throws an IllegalStateException; the method should propagate this exception unchanged.", throwingCal, IllegalStateException.class));
    }

    @Test
    void testEpochZero() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, UTC);
        double expected = 0.0;
        double actual = CalendarConversions.toDouble(cal, converter);
        assertEquals(expected, actual, 1e-12, "Epoch start should be 0.0 seconds");
    }

    @Test
    void testTypicalInstant() {
        Calendar cal = createCalendar(2023, 6, 15, 12, 34, 56, 789, UTC);
        double expected = cal.getTimeInMillis() / 1000.0;
        double actual = CalendarConversions.toDouble(cal, converter);
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    void testDifferentTimeZonesProduceSameInstantWhenFieldsAdjusted() {
        // Same absolute instant (2023‑06‑15T03:00:00.000Z) expressed in three zones
        Calendar utcCal = createCalendar(2023, 6, 15, 3, 0, 0, 0, UTC);
        // UTC+9
        Calendar tokyoCal = createCalendar(2023, 6, 15, 12, 0, 0, 0, TOKYO);
        // UTC+2 (summer)
        Calendar parisCal = createCalendar(2023, 6, 15, 4, 0, 0, 0, PARIS);
        double utc = CalendarConversions.toDouble(utcCal, converter);
        double tokyo = CalendarConversions.toDouble(tokyoCal, converter);
        double paris = CalendarConversions.toDouble(parisCal, converter);
        assertEquals(utc, tokyo, 1e-12, "Tokyo calendar should represent same epoch");
        assertEquals(utc, paris, 1e-12, "Paris calendar should represent same epoch");
    }

    @Test
    void testMillisecondPrecision() {
        Calendar cal = createCalendar(2000, 2, 29, 23, 59, 59, 999, NEW_YORK);
        double expected = cal.getTimeInMillis() / 1000.0;
        double actual = CalendarConversions.toDouble(cal, converter);
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    void testNegativeEpoch() {
        // Date before 1970‑01‑01 UTC
        Calendar cal = createCalendar(1965, 12, 31, 23, 59, 59, 0, UTC);
        double expected = cal.getTimeInMillis() / 1000.0;
        double actual = CalendarConversions.toDouble(cal, converter);
        assertEquals(expected, actual, 1e-12);
    }

    /**
     * Normal conversions (epoch start, typical date, pre‑epoch date).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalConversions")
    void testNormalConversions(String description, Calendar calendar, double expected) {
        double actual = CalendarConversions.toDouble(calendar, converter);
        assertEquals(expected, actual, 1e-12, description);
    }

    /**
     * Edge cases (far‑future date, non‑UTC timezone).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCaseConversions")
    void testEdgeCases(String description, Calendar calendar, double expected) {
        double actual = CalendarConversions.toDouble(calendar, converter);
        assertEquals(expected, actual, 1e-12, description);
    }

    /**
     * Error handling – null argument and wrong type.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("errorHandlingCases")
    void testErrorHandling(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toDouble(from, converter), description);
    }

    /**
     * Custom Calendar subclasses that misbehave in getTime().
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("customCalendarCases")
    void testCustomCalendarEdgeCases(String description, Calendar calendar, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toDouble(calendar, converter), description);
    }
}
