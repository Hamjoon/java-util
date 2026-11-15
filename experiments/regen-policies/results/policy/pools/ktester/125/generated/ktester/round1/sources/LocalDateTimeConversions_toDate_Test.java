package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toDate_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance – use the default options.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Helper to create a mock {@link Converter} that returns a fixed value from its {@code convert} method.
     */
    private static Converter mockConverterReturning(Object returnValue) {
        Converter mock = mock(Converter.class);
        // The internal toLong method eventually calls converter.convert(..., Long.class)
        // We therefore stub any conversion to Long.class to return the supplied value.
        when(mock.convert(any(), eq(Long.class))).thenReturn(returnValue);
        return mock;
    }

    /**
     * Helper to create a mock {@link Converter} that throws a supplied exception when asked to convert.
     */
    private static Converter mockConverterThrowing(RuntimeException ex) {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Long.class))).thenThrow(ex);
        return mock;
    }

    /**
     * Provides arguments for the "various source types" group.
     */
    private static Stream<Arguments> sourceTypesProvider() {
        // 2021‑06‑01T12:00:00Z
        long epoch = 1_622_548_800_000L;
        // Custom converter that returns the epoch millis regardless of input.
        Converter mockReturningEpoch = mockConverterReturning(epoch);
        return Stream.of(// Long literal (epoch millis)
        Arguments.of(epoch, null, new Date(epoch), "Long value is interpreted as epoch milliseconds and returned unchanged."), // Integer literal (treated as Number)
        Arguments.of(1_622_548_800, null, new Date(1_622_548_800L), "Integer (or any Number) is converted via Number.longValue() and used as epoch milliseconds."), // String containing a numeric value
        Arguments.of("1622548800000", null, new Date(epoch), "String containing a numeric value is parsed to long and converted to Date."), // Existing Date instance
        Arguments.of(new Date(epoch), null, new Date(epoch), "When the source is already a java.util.Date, the same instant is returned."), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2021-06-01 12:00:00"), null, new Date(epoch), "java.sql.Timestamp is converted to its underlying epoch milliseconds."), // LocalDateTime (interpreted as UTC)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 0), null, new Date(epoch), "LocalDateTime is interpreted as UTC and transformed to epoch milliseconds."), // Instant
        Arguments.of(Instant.ofEpochMilli(epoch), null, new Date(epoch), "Instant is directly converted to its epoch millisecond value."), // Null source – expect NPE
        Arguments.of(null, null, NullPointerException.class, "Passing a null source should result in a NullPointerException."), // Unsupported source type – expect IllegalArgumentException
        Arguments.of(new Object(), null, IllegalArgumentException.class, "Unsupported source type triggers an IllegalArgumentException."), // Custom converter returning a fixed long
        Arguments.of("any", mockReturningEpoch, new Date(epoch), "When a custom Converter is supplied, its conversion result (fixed long) is used to build the Date, regardless of the original value."));
    }

    /**
     * Provides arguments for the "additional numeric types" group.
     */
    private static Stream<Arguments> additionalNumericProvider() {
        // 2021‑06‑01T12:00:00Z
        long epoch = 1_622_548_800_000L;
        return Stream.of(Arguments.of(new BigDecimal("1622548800000"), new Date(epoch), "BigDecimal containing a valid epoch‑millis value is converted to a Date."), Arguments.of(new BigInteger("1622548800000"), new Date(epoch), "BigInteger containing a valid epoch‑millis value is converted to a Date."), Arguments.of(new AtomicLong(epoch), new Date(epoch), "AtomicLong holding epoch‑millis is converted to a Date."));
    }

    /**
     * Provides arguments for the "exception scenarios" group.
     */
    private static Stream<Arguments> exceptionScenariosProvider() {
        long epoch = 1_622_548_800_000L;
        // Mock converters for special behaviours
        Converter mockReturningNull = mockConverterReturning(null);
        Converter mockReturningString = mockConverterReturning("not a number");
        Converter mockThrowingRuntime = mockConverterThrowing(new RuntimeException("boom"));
        return Stream.of(// Non‑numeric String -> NumberFormatException
        Arguments.of("notANumber", null, NumberFormatException.class, "A non‑numeric String cannot be parsed to a long, causing NumberFormatException."), // Boolean -> IllegalArgumentException
        Arguments.of(true, null, IllegalArgumentException.class, "Boolean is not a supported source type for toDate, so toLong throws IllegalArgumentException."), // List -> IllegalArgumentException
        Arguments.of(Arrays.asList(1, 2, 3), null, IllegalArgumentException.class, "A List is an unsupported source type and triggers IllegalArgumentException."), // Custom converter returns null -> NullPointerException
        Arguments.of("any", mockReturningNull, NullPointerException.class, "Custom Converter returns null; toLong attempts to unbox a null Long, resulting in NullPointerException."), // Custom converter returns String -> ClassCastException
        Arguments.of("any", mockReturningString, ClassCastException.class, "Custom Converter returns a String instead of a Number/Long; the cast inside toLong fails with ClassCastException."), // Custom converter throws RuntimeException -> RuntimeException
        Arguments.of("any", mockThrowingRuntime, RuntimeException.class, "Converter throws a RuntimeException; the exception propagates out of toDate."), // LocalDate (no time) -> IllegalArgumentException
        Arguments.of(LocalDate.of(2021, 6, 1), null, IllegalArgumentException.class, "LocalDate lacks time‑of‑day information; toLong cannot derive epoch milliseconds and throws IllegalArgumentException."));
    }

    @Test
    void toDate_fromDate_returnsSameInstant() {
        Date source = new Date(123_456_789L);
        Date result = LocalDateTimeConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "Date converted from a Date should preserve the epoch millisecond value.");
    }

    @Test
    void toDate_fromLong_returnsCorrespondingDate() {
        // 2021‑01‑01T00:00:00Z
        Long epochMillis = 1_609_459_200_000L;
        Date result = LocalDateTimeConversions.toDate(epochMillis, converter);
        assertNotNull(result);
        assertEquals(epochMillis.longValue(), result.getTime(), "Date converted from a Long should represent the same instant.");
    }

    @Test
    void toDate_fromTimestamp_returnsCorrespondingDate() {
        Timestamp ts = new Timestamp(1_609_459_200_000L);
        Date result = LocalDateTimeConversions.toDate(ts, converter);
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime(), "Date converted from a Timestamp should preserve the epoch millisecond value.");
    }

    @Test
    void toDate_fromCalendar_returnsCorrespondingDate() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(1_609_459_200_000L);
        Date result = LocalDateTimeConversions.toDate(cal, converter);
        assertNotNull(result);
        assertEquals(cal.getTimeInMillis(), result.getTime(), "Date converted from a Calendar should preserve the epoch millisecond value.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("sourceTypesProvider")
    void toDate_variousSourceTypes(Object from, Converter customConverter, Object expected, String description) {
        Converter usedConverter = (customConverter != null) ? customConverter : converter;
        if (expected instanceof Class) {
            // Expected exception case
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> LocalDateTimeConversions.toDate(from, usedConverter), description);
        } else {
            // Expected normal Date result
            Date result = LocalDateTimeConversions.toDate(from, usedConverter);
            assertNotNull(result, description);
            Date expectedDate = (Date) expected;
            assertEquals(expectedDate.getTime(), result.getTime(), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalNumericProvider")
    void toDate_additionalNumericTypes(Object from, Date expected, String description) {
        Date result = LocalDateTimeConversions.toDate(from, converter);
        assertNotNull(result, description);
        assertEquals(expected.getTime(), result.getTime(), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionScenariosProvider")
    void toDate_exceptionScenarios(Object from, Converter customConverter, Class<? extends Throwable> expectedException, String description) {
        Converter usedConverter = (customConverter != null) ? customConverter : converter;
        assertThrows(expectedException, () -> LocalDateTimeConversions.toDate(from, usedConverter), description);
    }
}
