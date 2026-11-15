package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toBigDecimal_Test {

    /**
     * The divisor used by {@link CalendarConversions#toBigDecimal(Object, Converter)}.
     *  It is defined in {@code BigDecimalConversions.GRAND}.  We obtain it via reflection
     *  so the test does not need to know the exact numeric value.
     */
    private static BigDecimal GRAND;

    /**
     * Small functional helper to allow method‑chaining on a {@link Calendar}
     * when constructing arguments for {@code @MethodSource}.
     */
    private interface CalendarConsumer {

        void accept(Calendar c);
    }

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Retrieve the public static final field BigDecimalConversions.GRAND via reflection.
        Class<?> clazz = Class.forName("com.cedarsoftware.util.convert.BigDecimalConversions");
        Field f = clazz.getDeclaredField("GRAND");
        f.setAccessible(true);
        GRAND = (BigDecimal) f.get(null);
        assertNotNull(GRAND, "BigDecimalConversions.GRAND must be non‑null for the test");
    }

    private static Stream<Arguments> validConversions() {
        return Stream.of(// 2023‑01‑01T00:00:00Z, epoch 1672531200000 ms, GRAND assumed 1000 → 1672531200
        Arguments.of("UTC GregorianCalendar 2023‑01‑01", createGregorianCalendar("UTC", 1672531200000L), new BigDecimal("1672531200")), // Same epoch but with +02:00 zone – zone must be ignored
        Arguments.of("UTC+02 GregorianCalendar same epoch", createGregorianCalendar("UTC+02:00", 1672531200000L), new BigDecimal("1672531200")), // Epoch start
        Arguments.of("Epoch start (0 ms)", createGregorianCalendar("UTC", 0L), BigDecimal.ZERO), // One day before epoch (negative)
        Arguments.of("One day before epoch", createGregorianCalendar("UTC", -86400000L), new BigDecimal("-86400")), // Non‑null Converter (should be ignored)
        Arguments.of("Non‑null dummy Converter", createGregorianCalendar("UTC", 123456789L), new BigDecimal("123456.789")));
    }

    private static Stream<Arguments> errorHandling() {
        return Stream.of(Arguments.of("Null Calendar should NPE", null, NullPointerException.class), Arguments.of("Non‑Calendar object should CCE", "not a calendar", ClassCastException.class));
    }

    private static Stream<Arguments> boundaryValues() {
        return Stream.of(Arguments.of("Long.MAX_VALUE epoch", createGregorianCalendar("UTC", Long.MAX_VALUE), // Long.MAX_VALUE / 1000
        new BigDecimal("9223372036854775.807")), Arguments.of("Long.MIN_VALUE epoch", createGregorianCalendar("UTC", Long.MIN_VALUE), // Long.MIN_VALUE / 1000
        new BigDecimal("-9223372036854775.808")), Arguments.of("One millisecond after epoch", createGregorianCalendar("UTC", 1L), new BigDecimal("0.001")), Arguments.of("One millisecond before epoch", createGregorianCalendar("UTC", -1L), new BigDecimal("-0.001")));
    }

    private static Stream<Arguments> nonStandardCalendars() {
        return Stream.of(// Calendar.getInstance with GMT+05:30 but epoch set explicitly
        Arguments.of("Calendar.getInstance(GMT+05:30) with epoch 1609459200000", Calendar.getInstance(TimeZone.getTimeZone("GMT+05:30")).apply(c -> c.setTimeInMillis(1609459200000L)), new BigDecimal("1609459200")), // Default GregorianCalendar with epoch zero
        Arguments.of("Default GregorianCalendar with epoch zero", new GregorianCalendar() {

            {
                setTimeInMillis(0L);
            }
        }, BigDecimal.ZERO));
    }

    private static Stream<Arguments> grandEdgeCases() {
        Calendar cal = createGregorianCalendar("UTC", 1000L);
        return Stream.of(Arguments.of("GRAND = 0 → ArithmeticException", cal, ArithmeticException.class, BigDecimal.ZERO), Arguments.of("GRAND = null → NullPointerException", cal, NullPointerException.class, null));
    }

    /**
     * Creates a {@link GregorianCalendar} with the supplied time‑zone ID and epoch milliseconds.
     *
     * @param tzId        the ID understood by {@link TimeZone#getTimeZone(String)}
     * @param epochMillis epoch milliseconds to set
     * @return a fully initialised {@link GregorianCalendar}
     */
    private static GregorianCalendar createGregorianCalendar(String tzId, long epochMillis) {
        GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone(tzId));
        gc.setTimeInMillis(epochMillis);
        return gc;
    }

    private static Calendar apply(Calendar calendar, CalendarConsumer consumer) {
        consumer.accept(calendar);
        return calendar;
    }

    @Test
    void testToBigDecimal_WithValidCalendar_ReturnsExpectedValue() {
        // Arrange – a known instant (2020‑01‑01T00:00:00Z)
        // 2020‑01‑01 00:00:00 UTC in milliseconds
        long epochMillis = 1577836800000L;
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(epochMillis);
        // Act
        BigDecimal result = CalendarConversions.toBigDecimal(cal, null);
        // Expected value using the same formula as the implementation
        BigDecimal expected = new BigDecimal(epochMillis).divide(GRAND);
        // Assert
        assertEquals(0, expected.compareTo(result), () -> "Result should be epochMillis / GRAND; expected " + expected + " but got " + result);
    }

    @Test
    void testToBigDecimal_WithNullFrom_ThrowsNullPointerException() {
        // Arrange
        Calendar from = null;
        // Act & Assert
        assertThrows(NullPointerException.class, () -> CalendarConversions.toBigDecimal(from, null), "Calling toBigDecimal with a null Calendar should throw NPE");
    }

    @Test
    void testToBigDecimal_WithNonCalendarObject_ThrowsClassCastException() {
        // Arrange
        Object notACalendar = "I am not a Calendar";
        // Act & Assert
        assertThrows(ClassCastException.class, () -> CalendarConversions.toBigDecimal(notACalendar, null), "Calling toBigDecimal with a non‑Calendar object should throw ClassCastException");
    }

    // ---------- Valid conversions ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversions")
    void testValidConversions(String description, Calendar calendar, BigDecimal expected) {
        BigDecimal result = CalendarConversions.toBigDecimal(calendar, null);
        assertEquals(0, expected.compareTo(result), () -> description + " – expected " + expected + " but got " + result);
    }

    // ---------- Error handling ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorHandling")
    void testErrorHandling(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toBigDecimal(from, null), description);
    }

    // ---------- Boundary values ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryValues")
    void testBoundaryValues(String description, Calendar calendar, BigDecimal expected) {
        BigDecimal result = CalendarConversions.toBigDecimal(calendar, null);
        assertEquals(0, expected.compareTo(result), () -> description + " – expected " + expected + " but got " + result);
    }

    // ---------- Converter side‑effects ----------
    @Test
    void testConverterSideEffects_Ignored() {
        Calendar calendar = createGregorianCalendar("UTC", 5000L);
        Converter badConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Should not be called");
            }
        };
        BigDecimal result = CalendarConversions.toBigDecimal(calendar, badConverter);
        assertEquals(0, new BigDecimal("5").compareTo(result), "Converter must be ignored – result should be 5 (5000 ms / 1000)");
    }

    // ---------- Non‑standard Calendar instances ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonStandardCalendars")
    void testNonStandardCalendars(String description, Calendar calendar, BigDecimal expected) {
        BigDecimal result = CalendarConversions.toBigDecimal(calendar, null);
        assertEquals(0, expected.compareTo(result), () -> description + " – expected " + expected + " but got " + result);
    }

    // ---------- Null getTime() handling ----------
    @Test
    void testNullGetTimeHandling() {
        Calendar badCalendar = new Calendar() {

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public Date getTime() {
                return null;
            }

            @Override
            public void setTime(Date date) {
            }

            @Override
            public void set(int field, int value) {
            }

            @Override
            public int get(int field) {
                return 0;
            }

            @Override
            public void clear() {
            }

            @Override
            public void clear(int field) {
            }

            @Override
            public boolean isSet(int field) {
                return false;
            }

            @Override
            public void setTimeZone(TimeZone zone) {
            }

            @Override
            public TimeZone getTimeZone() {
                return TimeZone.getTimeZone("UTC");
            }

            @Override
            public void setLenient(boolean lenient) {
            }

            @Override
            public boolean isLenient() {
                return true;
            }

            @Override
            public void setFirstDayOfWeek(int value) {
            }

            @Override
            public int getFirstDayOfWeek() {
                return 1;
            }

            @Override
            public void setMinimalDaysInFirstWeek(int value) {
            }

            @Override
            public int getMinimalDaysInFirstWeek() {
                return 1;
            }

            @Override
            public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
            }

            @Override
            public int getWeekYear() {
                return 0;
            }

            @Override
            public int getWeeksInWeekYear() {
                return 0;
            }

            @Override
            public void roll(int field, boolean up) {
            }

            @Override
            public void roll(int field, int amount) {
            }

            @Override
            public int getMinimum(int field) {
                return 0;
            }

            @Override
            public int getMaximum(int field) {
                return 0;
            }

            @Override
            public int getGreatestMinimum(int field) {
                return 0;
            }

            @Override
            public int getLeastMaximum(int field) {
                return 0;
            }

            @Override
            public int getActualMaximum(int field) {
                return 0;
            }

            @Override
            public int getActualMinimum(int field) {
                return 0;
            }
        };
        assertThrows(NullPointerException.class, () -> CalendarConversions.toBigDecimal(badCalendar, null), "Calendar with getTime() returning null must cause NPE");
    }

    // ---------- GRAND divisor edge cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("grandEdgeCases")
    void testGrandDivisorEdgeCases(String description, Calendar calendar, Class<? extends Throwable> expectedException, BigDecimal newGrandValue) throws Exception {
        // Save original GRAND
        Field f = Class.forName("com.cedarsoftware.util.convert.BigDecimalConversions").getDeclaredField("GRAND");
        f.setAccessible(true);
        Object original = f.get(null);
        try {
            // Set the edge‑case value (could be null or zero)
            f.set(null, newGrandValue);
            assertThrows(expectedException, () -> CalendarConversions.toBigDecimal(calendar, null), description);
        } finally {
            // Restore original value to avoid side‑effects on other tests
            f.set(null, original);
        }
    }
}
