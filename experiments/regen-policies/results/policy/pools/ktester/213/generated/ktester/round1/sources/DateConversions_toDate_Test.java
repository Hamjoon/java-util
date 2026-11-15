package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class DateConversions_toDate_Test {

    /**
     * Converter used for all tests – it is fully functional, no mocking required.
     */
    private static Converter converter;

    /**
     * Returns a fixed long value irrespective of the input.
     */
    private static class MockConverterReturning implements Converter {

        private final Long value;

        MockConverterReturning(Long value) {
            this.value = value;
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            // The real Converter has many overloads – we only need the one used by DateConversions.
            //noinspection unchecked
            return (T) value;
        }
        // All other methods of Converter are irrelevant for these tests; they can be left empty.
        // The class implements the interface solely to satisfy the method signature.
    }

    /**
     * Throws the supplied runtime exception when invoked.
     */
    private static class MockConverterThrowing implements Converter {

        private final RuntimeException ex;

        MockConverterThrowing(RuntimeException ex) {
            this.ex = ex;
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw ex;
        }
    }

    /**
     * Simple holder to pass heterogeneous test data via {@code MethodSource}.
     */
    private static class ArgumentsHolder {

        final Object input;

        final Date expected;

        final String description;

        ArgumentsHolder(Object input, Date expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default options are sufficient for our tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Numeric and temporal valid conversions.
     */
    static Stream<ArgumentsHolder> validNumericTemporalProvider() {
        return Stream.of(// Long epoch millis
        new ArgumentsHolder(1622548800000L, new Date(1622548800000L), "Long value is passed directly; toLong should return the same long and Date constructor creates the correct instant."), // Integer (Number) – interpreted as milliseconds
        new ArgumentsHolder(1622548800, new Date(1622548800L), "Integer is a Number; toLong should use Number.longValue() without any scaling."), // ISO‑8601 string
        new ArgumentsHolder("2021-06-01T12:00:00Z", Date.from(Instant.parse("2021-06-01T12:00:00Z")), "ISO‑8601 string is parsed by toLong (via Instant.parse) and converted to epoch milliseconds."), // java.util.Date
        new ArgumentsHolder(new Date(1622548800000L), new Date(1622548800000L), "java.util.Date is recognized; toLong extracts its getTime() value."), // java.sql.Timestamp
        new ArgumentsHolder(Timestamp.valueOf("2021-06-01 12:00:00"), Date.from(Instant.parse("2021-06-01T12:00:00Z")), "Timestamp extends java.util.Date; conversion should behave the same as Date."), // java.time.Instant
        new ArgumentsHolder(Instant.ofEpochMilli(1622548800000L), Date.from(Instant.ofEpochMilli(1622548800000L)), "Instant is handled; toLong returns instant.toEpochMilli()."), // Calendar (UTC)
        new ArgumentsHolder(createUtcCalendar(1622548800000L), Date.from(Instant.ofEpochMilli(1622548800000L)), "Calendar is recognized; conversion uses calendar.getTimeInMillis()."));
    }

    /**
     * Strings with fractional seconds.
     */
    static Stream<ArgumentsHolder> fractionalStringProvider() {
        return Stream.of(new ArgumentsHolder("2021-06-01T12:00:00.123Z", Date.from(Instant.parse("2021-06-01T12:00:00.123Z")), "ISO‑8601 string that includes milliseconds; toLong should parse via Instant.parse and preserve the fraction."));
    }

    /**
     * Unsupported string format – expects parsing exception.
     */
    static Stream<Object> unsupportedStringProvider() {
        return Stream.of("06/01/2021 12:00:00", "", "   ");
    }

    /**
     * LocalDateTime conversion – default zone is used.
     */
    static Stream<ArgumentsHolder> localDateTimeProvider() {
        // Use the system default zone offset at the test execution moment.
        ZoneId defaultZone = ZoneId.systemDefault();
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 12, 0, 0);
        long expected = ldt.atZone(defaultZone).toInstant().toEpochMilli();
        return Stream.of(new ArgumentsHolder(ldt, new Date(expected), "LocalDateTime interpreted in the JVM default time‑zone."));
    }

    /**
     * ZonedDateTime conversion.
     */
    static Stream<ArgumentsHolder> zonedDateTimeProvider() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
        return Stream.of(new ArgumentsHolder(zdt, Date.from(zdt.toInstant()), "ZonedDateTime contains zone information; conversion should use its instant value."));
    }

    /**
     * OffsetDateTime conversion.
     */
    static Stream<ArgumentsHolder> offsetDateTimeProvider() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 6, 1, 7, 0, 0, 0, ZoneOffset.ofHours(-5));
        return Stream.of(new ArgumentsHolder(odt, Date.from(odt.toInstant()), "OffsetDateTime should be converted via its instant, normalising the offset."));
    }

    /**
     * YearMonth conversion – first day of month at UTC midnight.
     */
    static Stream<ArgumentsHolder> yearMonthProvider() {
        YearMonth ym = YearMonth.of(2021, 6);
        Instant instant = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return Stream.of(new ArgumentsHolder(ym, Date.from(instant), "YearMonth is interpreted as the first day of the month at midnight UTC."));
    }

    /**
     * Year conversion – Jan 1st UTC midnight.
     */
    static Stream<ArgumentsHolder> yearProvider() {
        Year y = Year.of(2021);
        Instant instant = y.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return Stream.of(new ArgumentsHolder(y, Date.from(instant), "Year is interpreted as Jan 1st of that year at midnight UTC."));
    }

    /**
     * MonthDay conversion – uses current year.
     */
    static Stream<ArgumentsHolder> monthDayProvider() {
        MonthDay md = MonthDay.of(6, 1);
        int currentYear = Year.now(ZoneOffset.UTC).getValue();
        Instant instant = md.atYear(currentYear).atStartOfDay(ZoneOffset.UTC).toInstant();
        return Stream.of(new ArgumentsHolder(md, Date.from(instant), "MonthDay lacks a year; conversion should use the current year in UTC."));
    }

    /**
     * BigDecimal conversion (integral).
     */
    static Stream<ArgumentsHolder> bigDecimalProvider() {
        BigDecimal bd = new BigDecimal("1622548800123");
        return Stream.of(new ArgumentsHolder(bd, new Date(1622548800123L), "BigDecimal should be converted via its long value to epoch milliseconds."));
    }

    /**
     * BigInteger conversion.
     */
    static Stream<ArgumentsHolder> bigIntegerProvider() {
        BigInteger bi = new BigInteger("1622548800123");
        return Stream.of(new ArgumentsHolder(bi, new Date(1622548800123L), "BigInteger should be converted to long and then to Date."));
    }

    /**
     * AtomicLong conversion.
     */
    static Stream<ArgumentsHolder> atomicLongProvider() {
        AtomicLong al = new AtomicLong(1622548800000L);
        return Stream.of(new ArgumentsHolder(al, new Date(1622548800000L), "AtomicLong should be read via get() and used as epoch milliseconds."));
    }

    /**
     * Map with "epoch" key.
     */
    static Stream<ArgumentsHolder> mapEpochProvider() {
        Map<String, Object> map = Collections.singletonMap("epoch", 1622548800000L);
        return Stream.of(new ArgumentsHolder(map, new Date(1622548800000L), "Map containing an \"epoch\" entry should be extracted."));
    }

    /**
     * Boundary values – Long.MAX_VALUE and Long.MIN_VALUE.
     */
    static Stream<ArgumentsHolder> boundaryProvider() {
        return Stream.of(new ArgumentsHolder(Long.MAX_VALUE, new Date(Long.MAX_VALUE), "Maximum long epoch value."), new ArgumentsHolder(Long.MIN_VALUE, new Date(Long.MIN_VALUE), "Minimum long epoch value."));
    }

    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    @Test
    void toDate_fromLong_returnsCorrectDate() {
        // GIVEN
        // some arbitrary epoch millis
        Long epochMillis = 1_617_123_456_789L;
        // WHEN
        Date result = DateConversions.toDate(epochMillis, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMillis.longValue(), result.getTime(), "Date should contain the exact epoch milliseconds supplied");
    }

    @Test
    void toDate_fromStringNumeric_returnsCorrectDate() {
        // GIVEN
        // seconds since epoch, not millis – Converter treats as long (millis)
        String numericString = "1623456789";
        // WHEN
        Date result = DateConversions.toDate(numericString, converter);
        // THEN
        assertNotNull(result);
        // The string is parsed as a long value (1623456789L) which is interpreted as milliseconds.
        assertEquals(1_623_456_789L, result.getTime(), "String containing a numeric value should be parsed to a long and used as epoch millis");
    }

    @Test
    void toDate_fromDate_returnsSameInstant() {
        // GIVEN
        Date original = new Date(1_600_000_000_000L);
        // WHEN
        Date result = DateConversions.toDate(original, converter);
        // THEN
        assertNotNull(result);
        assertEquals(original.getTime(), result.getTime(), "Converting a Date should preserve the underlying epoch millisecond value");
        // The returned instance should be a new object (not the same reference)
        assertNotSame(original, result, "Result should be a new Date instance, not the original reference");
    }

    @Test
    void toDate_fromInstant_returnsCorrectDate() {
        // GIVEN
        Instant instant = Instant.ofEpochMilli(1_500_000_000_000L);
        // WHEN
        Date result = DateConversions.toDate(instant, converter);
        // THEN
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.getTime(), "Instant should be converted to a Date with the same epoch millisecond value");
    }

    @Test
    void toDate_fromNull_returnsEpochZeroDate() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = DateConversions.toDate(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Null source should be converted to a Date representing epoch start (0 ms)");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validNumericTemporalProvider")
    void toDate_validNumericTemporal(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertNotNull(result);
        assertEquals(holder.expected.getTime(), result.getTime(), "Converted Date should match the expected epoch milliseconds.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("fractionalStringProvider")
    void toDate_fractionalString(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertNotNull(result);
        assertEquals(holder.expected.getTime(), result.getTime(), "Fractional seconds should be preserved.");
    }

    @ParameterizedTest(name = "{index}: unsupported string \"{0}\"")
    @MethodSource("unsupportedStringProvider")
    void toDate_unsupportedStringThrows(String badString) {
        assertThrows(DateTimeException.class, () -> DateConversions.toDate(badString, converter), "Parsing an unsupported or empty string must throw a DateTimeException.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("localDateTimeProvider")
    void toDate_localDateTime(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "LocalDateTime should be interpreted in the default zone.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("zonedDateTimeProvider")
    void toDate_zonedDateTime(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "ZonedDateTime conversion must use its instant.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("offsetDateTimeProvider")
    void toDate_offsetDateTime(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "OffsetDateTime conversion must normalise to UTC instant.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("yearMonthProvider")
    void toDate_yearMonth(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "YearMonth should become first day of month at UTC midnight.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("yearProvider")
    void toDate_year(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "Year should become Jan 1st UTC midnight.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("monthDayProvider")
    void toDate_monthDay(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "MonthDay should use the current year in UTC.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("bigDecimalProvider")
    void toDate_bigDecimal(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "BigDecimal integral value should be converted to Date.");
    }

    @Test
    void toDate_bigDecimalFractionalThrows() {
        BigDecimal bd = new BigDecimal("1622548800.5");
        assertThrows(ArithmeticException.class, () -> DateConversions.toDate(bd, converter), "BigDecimal with fractional part must raise ArithmeticException.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("bigIntegerProvider")
    void toDate_bigInteger(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "BigInteger should be converted to Date.");
    }

    @Test
    void toDate_bigIntegerOutOfRangeThrows() {
        // Long.MAX_VALUE + 1
        BigInteger bi = new BigInteger("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> DateConversions.toDate(bi, converter), "BigInteger out of long range must throw ArithmeticException.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("atomicLongProvider")
    void toDate_atomicLong(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "AtomicLong should be converted to Date.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("mapEpochProvider")
    void toDate_mapWithEpoch(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "Map with \"epoch\" key should be converted correctly.");
    }

    @Test
    void toDate_mapWithoutEpochThrows() {
        Map<String, Object> map = Collections.singletonMap("foo", 123L);
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(map, converter), "Map lacking \"epoch\" entry must cause IllegalArgumentException.");
    }

    @Test
    void toDate_mapWithNonNumericEpochThrows() {
        Map<String, Object> map = Collections.singletonMap("epoch", "notANumber");
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(map, converter), "Non‑numeric \"epoch\" value must cause IllegalArgumentException.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("boundaryProvider")
    void toDate_boundaryValues(ArgumentsHolder holder) {
        Date result = DateConversions.toDate(holder.input, converter);
        assertEquals(holder.expected.getTime(), result.getTime(), "Boundary long values should be accepted.");
    }

    @Test
    void toDate_nullInputReturnsEpochZero() {
        Date result = DateConversions.toDate(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Null source should be converted to epoch start (0 ms).");
    }

    @Test
    void toDate_customConverterOverrides() {
        Converter mock = new MockConverterReturning(1622548800000L);
        Date result = DateConversions.toDate("ignored", mock);
        assertEquals(1622548800000L, result.getTime(), "Custom converter should dictate the returned epoch milliseconds.");
    }

    @Test
    void toDate_converterThrowsRuntimeException() {
        RuntimeException ex = new RuntimeException("conversion failed");
        Converter mock = new MockConverterThrowing(ex);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> DateConversions.toDate("any", mock), "Exception from custom converter must propagate.");
        assertSame(ex, thrown);
    }

    @Test
    void toDate_converterReturnsNullThrowsNPE() {
        Converter mock = new MockConverterReturning(null);
        assertThrows(NullPointerException.class, () -> DateConversions.toDate("any", mock), "Converter returning null should cause NPE when Date constructor receives null.");
    }

    @Test
    void toDate_unsupportedObjectThrows() {
        Object obj = new Object();
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(obj, converter), "Unsupported source type must cause IllegalArgumentException.");
    }

    @Test
    void toDate_booleanInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(Boolean.TRUE, converter), "Boolean input must be rejected.");
    }

    @Test
    void toDate_numberNaNThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(Double.NaN, converter), "NaN is not a valid epoch millisecond.");
    }

    @Test
    void toDate_numberInfinityThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(Double.POSITIVE_INFINITY, converter), "Infinity is not a valid epoch millisecond.");
    }
}
