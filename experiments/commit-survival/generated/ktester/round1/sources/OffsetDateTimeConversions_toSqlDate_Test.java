package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toSqlDate_Test {

    /**
     * Converter used for all tests – created once for the whole test suite.
     */
    private static Converter converter;

    /**
     * Converter used for the “normal” conversion tests – created once for the whole suite.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    /**
     * Helper that creates a mock Converter which, when asked to convert the given
     *  {@code source} to {@code Long.class}, either returns {@code result} or throws
     *  the supplied {@code exception}.
     */
    private static Converter mockConverterReturning(Object source, Object result, RuntimeException exception) {
        Converter mock = mock(Converter.class);
        if (exception != null) {
            when(mock.convert(eq(source), eq(Long.class))).thenThrow(exception);
        } else {
            when(mock.convert(eq(source), eq(Long.class))).thenReturn(result);
        }
        return mock;
    }

    private static Stream<Arguments> numericInputProvider() {
        return Stream.of(Arguments.of(123456789L, 123456789L), Arguments.of(42, 42L), // double is truncated – the mock returns the truncated long value
        Arguments.of(3.1415926535, 3L));
    }

    private static Stream<Arguments> utilDateProvider() {
        return Stream.of(Arguments.of(new Date(0L), 0L), Arguments.of(new Date(Long.MAX_VALUE), Long.MAX_VALUE));
    }

    private static Stream<Arguments> timestampProvider() {
        return Stream.of(Arguments.of(Timestamp.valueOf("1970-01-01 00:00:00.123"), 123L), Arguments.of(Timestamp.valueOf("2023-03-15 12:34:56.789"), 1678889696789L));
    }

    private static Stream<Arguments> instantProvider() {
        return Stream.of(Arguments.of(Instant.ofEpochMilli(0L), 0L), Arguments.of(Instant.parse("2000-01-01T00:00:00Z"), 946684800000L));
    }

    private static Stream<Arguments> localDateTimeProvider() {
        return Stream.of(Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 0, 123_000_000), 123L), Arguments.of(LocalDateTime.of(2023, 3, 15, 12, 34, 56, 789_000_000), 1678889696789L));
    }

    private static Stream<Arguments> offsetDateTimeProvider() {
        return Stream.of(Arguments.of(OffsetDateTime.parse("1970-01-01T00:00:00.123Z"), 123L), Arguments.of(OffsetDateTime.parse("2023-03-15T12:34:56.789+02:00"), 1678882496789L));
    }

    private static Stream<Arguments> zonedDateTimeProvider() {
        return Stream.of(Arguments.of(ZonedDateTime.parse("1970-01-01T00:00:00.123Z"), 123L), Arguments.of(ZonedDateTime.of(2023, 3, 15, 12, 34, 56, 789_000_000, ZoneId.of("America/New_York")), 1678914896789L));
    }

    private static Stream<Arguments> calendarProvider() {
        Calendar utcZero = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcZero.setTimeInMillis(0L);
        Calendar defaultCal = Calendar.getInstance();
        defaultCal.setTimeInMillis(1678889696789L);
        return Stream.of(Arguments.of(utcZero, 0L), Arguments.of(defaultCal, 1678889696789L));
    }

    private static Stream<Arguments> atomicLongProvider() {
        return Stream.of(Arguments.of(new AtomicLong(0L), 0L), Arguments.of(new AtomicLong(1678889696789L), 1678889696789L));
    }

    private static Stream<Arguments> bigDecimalProvider() {
        return Stream.of(Arguments.of(new BigDecimal("12345"), 12345L));
    }

    private static Stream<Arguments> bigIntegerProvider() {
        return Stream.of(Arguments.of(new BigInteger("9876543210"), 9876543210L));
    }

    private static Stream<Arguments> numericStringProvider() {
        return Stream.of(Arguments.of("0", 0L), Arguments.of("1678889696789", 1678889696789L));
    }

    private static Stream<Arguments> boundaryLongProvider() {
        return Stream.of(Arguments.of(Long.MIN_VALUE), Arguments.of(Long.MAX_VALUE));
    }

    @Test
    void toSqlDate_fromOffsetDateTime_returnsEquivalentDate() {
        // GIVEN
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2023-08-15T12:34:56+02:00");
        // WHEN
        Date actual = OffsetDateTimeConversions.toSqlDate(offsetDateTime, realConverter);
        // THEN
        Date expected = Date.from(offsetDateTime.toInstant());
        assertEquals(expected.getTime(), actual.getTime(), "SQL Date should represent the same instant as the supplied OffsetDateTime");
    }

    @Test
    void toSqlDate_fromZonedDateTime_returnsEquivalentDate() {
        // GIVEN
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2021-12-01T08:15:30-05:00[America/New_York]");
        // WHEN
        Date actual = OffsetDateTimeConversions.toSqlDate(zonedDateTime, realConverter);
        // THEN
        Date expected = Date.from(zonedDateTime.toInstant());
        assertEquals(expected.getTime(), actual.getTime(), "SQL Date should represent the same instant as the supplied ZonedDateTime");
    }

    @Test
    void toSqlDate_fromUtilDate_returnsSameDateInstanceValue() {
        // GIVEN
        Date now = new Date();
        // WHEN
        Date actual = OffsetDateTimeConversions.toSqlDate(now, converter);
        // THEN
        // The conversion creates a new Date instance but the epoch milliseconds must be identical.
        assertEquals(now.getTime(), actual.getTime(), "SQL Date created from java.util.Date should have identical epoch milliseconds");
    }

    @Test
    void toSqlDate_fromLong_returnsDateWithSameEpochMilli() {
        // GIVEN
        long epochMilli = System.currentTimeMillis();
        // WHEN
        Date actual = OffsetDateTimeConversions.toSqlDate(epochMilli, realConverter);
        // THEN
        assertEquals(epochMilli, actual.getTime(), "SQL Date created from a long epoch milli should match the supplied value");
    }

    @Test
    void toSqlDate_fromNull_returnsNullDate() {
        // GIVEN
        Object nullValue = null;
        // WHEN
        Date actual = OffsetDateTimeConversions.toSqlDate(nullValue, converter);
        // THEN
        // The implementation delegates to toLong(null,…), which returns 0, so a Date(0) is produced.
        // This behaviour is verified rather than expecting a null return.
        assertNotNull(actual, "Conversion of null should not produce a null Date");
        assertEquals(0L, actual.getTime(), "Conversion of null should result in a Date representing the epoch start (0 ms)");
    }

    @Test
    void toSqlDate_nullSource_withConverterThrowingNPE_propagatesException() {
        // GIVEN
        Converter mock = mock(Converter.class);
        when(mock.convert(isNull(), eq(Long.class))).thenThrow(new NullPointerException("source is null"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toSqlDate(null, mock), "Passing a null source object should result in a NullPointerException from the converter");
    }

    @Test
    void toSqlDate_unsupportedObject_withConverterThrowingIAE_propagatesException() {
        // GIVEN
        Object unsupported = new Object();
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(unsupported), eq(Long.class))).thenThrow(new IllegalArgumentException("unsupported type"));
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toSqlDate(unsupported, mock), "When the source object type is not supported by the converter, an IllegalArgumentException should be thrown.");
    }

    @Test
    void toSqlDate_nullConverter_throwsNPE() {
        // GIVEN
        Long value = 123L;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toSqlDate(value, null), "Passing a null Converter should cause a NullPointerException.");
    }

    @ParameterizedTest(name = "{index}: from={0} → expected epoch={1}")
    @MethodSource("numericInputProvider")
    void toSqlDate_numericInputs(Object from, long expectedEpoch) {
        // GIVEN
        Converter mock = mockConverterReturning(from, expectedEpoch, null);
        // WHEN
        Date actual = OffsetDateTimeConversions.toSqlDate(from, mock);
        // THEN
        assertEquals(expectedEpoch, actual.getTime(), "Numeric input should be interpreted as epoch milliseconds.");
    }

    @ParameterizedTest(name = "{index}: Date({0}) → expected epoch={1}")
    @MethodSource("utilDateProvider")
    void toSqlDate_utilDateInputs(Date source, long expectedEpoch) {
        // GIVEN – a mock that simply returns source.getTime()
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(source), eq(Long.class))).thenReturn(source.getTime());
        // WHEN
        Date actual = OffsetDateTimeConversions.toSqlDate(source, mock);
        // THEN
        assertEquals(expectedEpoch, actual.getTime(), "java.util.Date should be converted preserving its epoch milliseconds.");
    }

    @ParameterizedTest(name = "{index}: Timestamp({0}) → expected epoch={1}")
    @MethodSource("timestampProvider")
    void toSqlDate_timestampInputs(Timestamp ts, long expectedEpoch) {
        // GIVEN – mock returns ts.getTime()
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(ts), eq(Long.class))).thenReturn(ts.getTime());
        // WHEN
        Date actual = OffsetDateTimeConversions.toSqlDate(ts, mock);
        // THEN
        assertEquals(expectedEpoch, actual.getTime(), "Timestamp should be converted to a Date with the same epoch milliseconds.");
    }

    @ParameterizedTest(name = "{index}: Instant({0}) → expected epoch={1}")
    @MethodSource("instantProvider")
    void toSqlDate_instantInputs(Instant instant, long expectedEpoch) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(instant), eq(Long.class))).thenReturn(instant.toEpochMilli());
        Date actual = OffsetDateTimeConversions.toSqlDate(instant, mock);
        assertEquals(expectedEpoch, actual.getTime(), "Instant should be converted to Date preserving epoch milliseconds.");
    }

    @ParameterizedTest(name = "{index}: LocalDateTime({0}) → expected epoch={1}")
    @MethodSource("localDateTimeProvider")
    void toSqlDate_localDateTimeInputs(LocalDateTime ldt, long expectedEpoch) {
        // The real Converter knows how to handle LocalDateTime → epoch conversion,
        // but we force the same behaviour with a mock for determinism.
        Converter mock = mock(Converter.class);
        // Simulate the conversion that OffsetDateTimeConversions.toLong would perform:
        // interpret the LDT in the system default zone.
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        when(mock.convert(eq(ldt), eq(Long.class))).thenReturn(zdt.toInstant().toEpochMilli());
        Date actual = OffsetDateTimeConversions.toSqlDate(ldt, mock);
        assertEquals(expectedEpoch, actual.getTime(), "LocalDateTime should be interpreted in the system default zone and converted to epoch millis.");
    }

    @ParameterizedTest(name = "{index}: OffsetDateTime({0}) → expected epoch={1}")
    @MethodSource("offsetDateTimeProvider")
    void toSqlDate_offsetDateTimeInputs(OffsetDateTime odt, long expectedEpoch) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(odt), eq(Long.class))).thenReturn(odt.toInstant().toEpochMilli());
        Date actual = OffsetDateTimeConversions.toSqlDate(odt, mock);
        assertEquals(expectedEpoch, actual.getTime(), "OffsetDateTime should be converted to epoch milliseconds (UTC).");
    }

    @ParameterizedTest(name = "{index}: ZonedDateTime({0}) → expected epoch={1}")
    @MethodSource("zonedDateTimeProvider")
    void toSqlDate_zonedDateTimeInputs(ZonedDateTime zdt, long expectedEpoch) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(zdt), eq(Long.class))).thenReturn(zdt.toInstant().toEpochMilli());
        Date actual = OffsetDateTimeConversions.toSqlDate(zdt, mock);
        assertEquals(expectedEpoch, actual.getTime(), "ZonedDateTime should be converted to epoch milliseconds (UTC).");
    }

    @ParameterizedTest(name = "{index}: Calendar({0}) → expected epoch={1}")
    @MethodSource("calendarProvider")
    void toSqlDate_calendarInputs(Calendar cal, long expectedEpoch) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(cal), eq(Long.class))).thenReturn(cal.getTimeInMillis());
        Date actual = OffsetDateTimeConversions.toSqlDate(cal, mock);
        assertEquals(expectedEpoch, actual.getTime(), "Calendar should be converted preserving its epoch milliseconds.");
    }

    @ParameterizedTest(name = "{index}: AtomicLong({0}) → expected epoch={1}")
    @MethodSource("atomicLongProvider")
    void toSqlDate_atomicLongInputs(AtomicLong al, long expectedEpoch) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(al), eq(Long.class))).thenReturn(al.get());
        Date actual = OffsetDateTimeConversions.toSqlDate(al, mock);
        assertEquals(expectedEpoch, actual.getTime(), "AtomicLong should be converted to a Date with the same epoch milliseconds.");
    }

    @ParameterizedTest(name = "{index}: BigDecimal({0}) → expected epoch={1}")
    @MethodSource("bigDecimalProvider")
    void toSqlDate_bigDecimalInput(BigDecimal bd, long expectedEpoch) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(bd), eq(Long.class))).thenReturn(bd.longValue());
        Date actual = OffsetDateTimeConversions.toSqlDate(bd, mock);
        assertEquals(expectedEpoch, actual.getTime(), "BigDecimal should be converted via longValue().");
    }

    @ParameterizedTest(name = "{index}: BigInteger({0}) → expected epoch={1}")
    @MethodSource("bigIntegerProvider")
    void toSqlDate_bigIntegerInput(BigInteger bi, long expectedEpoch) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(bi), eq(Long.class))).thenReturn(bi.longValue());
        Date actual = OffsetDateTimeConversions.toSqlDate(bi, mock);
        assertEquals(expectedEpoch, actual.getTime(), "BigInteger should be converted via longValue().");
    }

    @ParameterizedTest(name = "{index}: String(\"{0}\") → expected epoch={1}")
    @MethodSource("numericStringProvider")
    void toSqlDate_numericStringInputs(String str, long expectedEpoch) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(str), eq(Long.class))).thenReturn(Long.parseLong(str));
        Date actual = OffsetDateTimeConversions.toSqlDate(str, mock);
        assertEquals(expectedEpoch, actual.getTime(), "String containing a numeric epoch value should be parsed and converted.");
    }

    @ParameterizedTest(name = "{index}: Long boundary {0} → expected epoch={0}")
    @MethodSource("boundaryLongProvider")
    void toSqlDate_boundaryLongValues(Long value) {
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(value), eq(Long.class))).thenReturn(value);
        Date actual = OffsetDateTimeConversions.toSqlDate(value, mock);
        assertEquals(value.longValue(), actual.getTime(), "Boundary long values should be wrapped in a Date unchanged.");
    }

    @Test
    void toSqlDate_nonNumericString_throwsNumberFormatException() {
        // GIVEN
        String bad = "not-a-number";
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(bad), eq(Long.class))).thenThrow(new NumberFormatException("For input string: \"not-a-number\""));
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toSqlDate(bad, mock), "A non‑numeric String should trigger NumberFormatException.");
    }

    @Test
    void toSqlDate_booleanInput_throwsIllegalArgumentException() {
        // GIVEN
        Boolean bool = Boolean.TRUE;
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(bool), eq(Long.class))).thenThrow(new IllegalArgumentException("Boolean not supported"));
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toSqlDate(bool, mock), "Boolean values are not supported for date conversion.");
    }

    @Test
    void toSqlDate_doubleNaN_throwsIllegalArgumentException() {
        // GIVEN
        Double nan = Double.NaN;
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(nan), eq(Long.class))).thenThrow(new IllegalArgumentException("NaN not a valid epoch"));
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toSqlDate(nan, mock), "NaN is not a valid epoch millisecond value.");
    }

    @Test
    void toSqlDate_converterReturnsUnsupportedType_throwsClassCastException() {
        // GIVEN
        Object source = new Object();
        // not a Number / String
        Object unsupportedResult = new Object();
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(source), eq(Long.class))).thenReturn(unsupportedResult);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toSqlDate(source, mock), "If the Converter returns an object that cannot be cast to a numeric type, toLong should throw ClassCastException.");
    }
}
