package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.*;
import java.util.stream.Stream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toString_Test {

    @Mock
    private ConverterOptions // used only to construct a Converter
    mockOptions;

    private Converter converter;

    private static MockedStatic<DateConversions> dateConversionsMock;

    @BeforeAll
    static void setupBeforeAll() {
        // Mock the static DateConversions class once for all tests
        dateConversionsMock = Mockito.mockStatic(DateConversions.class);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the options to return an empty map for overrides – the Converter constructor expects it
        Mockito.when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock between tests to avoid cross‑test interference
        dateConversionsMock.reset();
    }

    private static Calendar calendarFromInstant(Instant instant, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(Date.from(instant));
        return cal;
    }

    private static Calendar calendarFromEpochMilli(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Stream<Arguments> validInputsProvider() {
        return Stream.of(// 2023‑03‑15T10:20:30.000Z → ISO‑8601 string
        Arguments.of(calendarFromInstant(Instant.parse("2023-03-15T10:20:30Z"), TimeZone.getTimeZone("UTC")), (Converter) (c, o) -> "2023-03-15T10:20:30Z", "2023-03-15T10:20:30Z"), // epoch zero → custom string
        Arguments.of(calendarFromEpochMilli(0L), (Converter) (c, o) -> "epoch-zero", "epoch-zero"));
    }

    @Test
    void toString_ShouldDelegateToDateConversions() {
        // GIVEN
        Calendar calendar = new GregorianCalendar();
        // a fixed epoch milli for reproducibility
        calendar.setTimeInMillis(1_632_000_000_000L);
        // WHEN
        String expected = DateConversions.toString(calendar.getTime(), converter);
        String actual = CalendarConversions.toString(calendar, converter);
        // THEN
        assertEquals(expected, actual, "CalendarConversions.toString should delegate to DateConversions.toString");
    }

    @Test
    void toString_WithNonCalendar_ShouldThrowClassCastException() {
        // GIVEN
        Object notACalendar = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toString(notACalendar, converter), "Passing a non‑Calendar object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] Valid input: expected={2}")
    @MethodSource("validInputsProvider")
    void toString_ValidInputs(Calendar calendar, Converter mockConverter, String expected) {
        // Mock DateConversions to simply delegate to the supplied mock converter
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            Converter conv = invocation.getArgument(1);
            // Use the mockConverter supplied by the test case
            return mockConverter.convert(conv, d);
        });
        String actual = CalendarConversions.toString(calendar, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toString_NullFrom_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toString(null, converter));
    }

    @Test
    void toString_NonCalendar_ThrowsClassCastException() {
        Object notACalendar = new Object();
        assertThrows(ClassCastException.class, () -> CalendarConversions.toString(notACalendar, converter));
    }

    @Test
    void toString_ConverterThrowsException_Propagates() {
        Calendar cal = calendarFromInstant(Instant.now(), TimeZone.getTimeZone("UTC"));
        // Mock DateConversions to invoke a converter that throws
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenAnswer(invocation -> {
            Converter conv = invocation.getArgument(1);
            // Simulate a converter that throws IllegalStateException
            throw new IllegalStateException("converter failure");
        });
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CalendarConversions.toString(cal, converter));
        assertEquals("converter failure", ex.getMessage());
    }

    @Test
    void toString_VerifyDatePassedToDateConversions() {
        Calendar cal = calendarFromInstant(Instant.parse("2022-12-31T23:59:59.123Z"), TimeZone.getTimeZone("UTC"));
        long expectedEpoch = cal.getTimeInMillis();
        // Capture the Date argument passed to DateConversions
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenAnswer(invocation -> {
            Date passed = invocation.getArgument(0);
            assertEquals(expectedEpoch, passed.getTime(), "The Date passed to DateConversions must match Calendar's instant");
            // Return the epoch as a string
            return String.valueOf(passed.getTime());
        });
        String result = CalendarConversions.toString(cal, converter);
        assertEquals(String.valueOf(expectedEpoch), result);
    }

    @Test
    void toString_DifferentTimeZones_UTCFormatting() {
        // 2023-06-01T12:00:00+02:00 (Europe/Paris) → should become 2023-06-01T10:00:00Z
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        cal.set(2023, Calendar.JUNE, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            // Format the Date in UTC ISO‑8601
            return Instant.ofEpochMilli(d.getTime()).toString();
        });
        String result = CalendarConversions.toString(cal, converter);
        assertEquals("2023-06-01T10:00:00Z", result);
    }

    @Test
    void toString_NegativeOffset_ReturnsEpochString() {
        // 2023-12-31T23:59:59-05:00 (America/New_York)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.set(2023, Calendar.DECEMBER, 31, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 0);
        long expectedEpoch = cal.getTimeInMillis();
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            return String.valueOf(d.getTime());
        });
        String result = CalendarConversions.toString(cal, converter);
        assertEquals(String.valueOf(expectedEpoch), result);
    }

    @Test
    void toString_MinLongValue() {
        Calendar cal = calendarFromEpochMilli(Long.MIN_VALUE);
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenAnswer(invocation -> String.valueOf(((Date) invocation.getArgument(0)).getTime()));
        String result = CalendarConversions.toString(cal, converter);
        assertEquals(String.valueOf(Long.MIN_VALUE), result);
    }

    @Test
    void toString_MaxLongValue() {
        Calendar cal = calendarFromEpochMilli(Long.MAX_VALUE);
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenAnswer(invocation -> String.valueOf(((Date) invocation.getArgument(0)).getTime()));
        String result = CalendarConversions.toString(cal, converter);
        assertEquals(String.valueOf(Long.MAX_VALUE), result);
    }

    @Test
    void toString_NullConverter_ThrowsNullPointerException() {
        Calendar cal = calendarFromInstant(Instant.now(), TimeZone.getTimeZone("UTC"));
        assertThrows(NullPointerException.class, () -> CalendarConversions.toString(cal, null));
    }

    @Test
    void toString_ConverterReturnsNull_PropagatesNull() {
        Calendar cal = calendarFromInstant(Instant.parse("2022-05-15T08:30:00Z"), TimeZone.getTimeZone("UTC"));
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenReturn(null);
        String result = CalendarConversions.toString(cal, converter);
        assertNull(result, "Method should return null when DateConversions returns null");
    }

    @Test
    void toString_MillisecondPrecisionLoss() {
        Calendar cal = calendarFromInstant(Instant.parse("2023-03-15T10:20:30.999Z"), TimeZone.getTimeZone("UTC"));
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            // Return seconds precision (epoch / 1000)
            return String.valueOf(d.getTime() / 1000);
        });
        String result = CalendarConversions.toString(cal, converter);
        assertEquals(String.valueOf(Instant.parse("2023-03-15T10:20:30.999Z").toEpochMilli() / 1000), result);
    }

    @Test
    void toString_CalendarGetTimeReturnsNull_ThrowsNPE() {
        // Custom Calendar that returns null from getTime()
        Calendar badCal = new Calendar() {

            @Override
            public Date getTime() {
                return null;
            }

            @Override
            public void setTime(Date date) {
            }

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public void roll(int field, boolean up) {
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

            @Override
            public Date getTimeZone() {
                return null;
            }

            @Override
            public void setTimeZone(TimeZone zone) {
            }

            @Override
            public TimeZone getTimeZone() {
                return TimeZone.getDefault();
            }

            @Override
            public boolean isSet(int field) {
                return false;
            }

            @Override
            public void clear() {
            }

            @Override
            public void clear(int field) {
            }

            @Override
            public void set(int year, int month, int date) {
            }

            @Override
            public void set(int year, int month, int date, int hourOfDay, int minute) {
            }

            @Override
            public void set(int year, int month, int date, int hourOfDay, int minute, int second) {
            }

            @Override
            public void setTimeInMillis(long millis) {
            }

            @Override
            public long getTimeInMillis() {
                return 0;
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
            public int get(int field) {
                return 0;
            }

            @Override
            public void set(int field, int value) {
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
        // DateConversions will receive a null Date and throw NPE
        dateConversionsMock.when(() -> DateConversions.toString(null, any(Converter.class))).// let it throw NPE
        thenCallRealMethod();
        assertThrows(NullPointerException.class, () -> CalendarConversions.toString(badCal, converter));
    }

    @Test
    void toString_CalendarGetTimeThrowsException_Propagates() {
        Calendar explodingCal = new Calendar() {

            @Override
            public Date getTime() {
                throw new IllegalStateException("boom");
            }

            // other abstract methods can be left empty / default
            @Override
            public void setTime(Date date) {
            }

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public void roll(int field, boolean up) {
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

            @Override
            public void setTimeZone(TimeZone zone) {
            }

            @Override
            public TimeZone getTimeZone() {
                return TimeZone.getDefault();
            }

            @Override
            public boolean isSet(int field) {
                return false;
            }

            @Override
            public void clear() {
            }

            @Override
            public void clear(int field) {
            }

            @Override
            public void set(int year, int month, int date) {
            }

            @Override
            public void set(int year, int month, int date, int hourOfDay, int minute) {
            }

            @Override
            public void set(int year, int month, int date, int hourOfDay, int minute, int second) {
            }

            @Override
            public void setTimeInMillis(long millis) {
            }

            @Override
            public long getTimeInMillis() {
                return 0;
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
            public int get(int field) {
                return 0;
            }

            @Override
            public void set(int field, int value) {
            }
        };
        assertThrows(IllegalStateException.class, () -> CalendarConversions.toString(explodingCal, converter));
    }

    @Test
    void toString_ConverterThrowsOnNegativeEpoch_Propagates() {
        Calendar cal = calendarFromInstant(Instant.parse("1960-01-01T00:00:00Z"), TimeZone.getTimeZone("UTC"));
        dateConversionsMock.when(() -> DateConversions.toString(any(Date.class), any(Converter.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            if (d.getTime() < 0) {
                throw new UnsupportedOperationException("negative epoch not supported");
            }
            return "ok";
        });
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> CalendarConversions.toString(cal, converter));
        assertEquals("negative epoch not supported", ex.getMessage());
    }
}
