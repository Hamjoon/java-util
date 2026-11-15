package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toDate_Test {

    /**
     * shared Converter instance used by all tests
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; default options are fine for these tests
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> numericInputsProvider() {
        return Stream.of(Arguments.of(123456789L, 123456789L, "Long value should be used directly as epoch milliseconds."), Arguments.of(42, 42L, "Integer value should be coerced to long."), Arguments.of("9876543210", 9876543210L, "Numeric String should be parsed to long."));
    }

    private static Stream<Arguments> javaDateTimeObjectsProvider() {
        // Helper for default zone calculations
        ZoneId defaultZone = ZoneId.systemDefault();
        Date date = new Date(1609459200000L);
        Instant instant = Instant.ofEpochMilli(1622505600000L);
        LocalDateTime ldt = LocalDateTime.of(2022, 5, 1, 12, 30, 0);
        long ldtMillis = ldt.atZone(defaultZone).toInstant().toEpochMilli();
        OffsetDateTime odt = OffsetDateTime.parse("2023-03-15T10:15:30+02:00");
        long odtMillis = odt.toInstant().toEpochMilli();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1633046400000L);
        long calendarMillis = calendar.getTimeInMillis();
        return Stream.of(Arguments.of(date, 1609459200000L, "java.util.Date should be converted using its internal epoch milliseconds."), Arguments.of(instant, 1622505600000L, "java.time.Instant should be converted to its epoch milliseconds."), Arguments.of(ldt, ldtMillis, "LocalDateTime should be interpreted in the system default zone."), Arguments.of(odt, odtMillis, "OffsetDateTime should be normalised to UTC before conversion."), Arguments.of(calendar, calendarMillis, "Calendar should provide its timeInMillis directly."));
    }

    private static Stream<Arguments> unsupportedOrInvalidInputsProvider() {
        return Stream.of(Arguments.of(new Object(), IllegalArgumentException.class, "Unsupported object type should trigger IllegalArgumentException."), Arguments.of("not-a-number", NumberFormatException.class, "Non‑numeric String should cause NumberFormatException."));
    }

    private static Stream<Arguments> boundaryNumericValuesProvider() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE, "Long.MAX_VALUE should be used directly as epoch milliseconds."), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE, "Long.MIN_VALUE should be used directly as epoch milliseconds."));
    }

    private static Stream<Arguments> stringDateRepresentationsProvider() {
        ZoneId defaultZone = ZoneId.systemDefault();
        long isoInstant = Instant.parse("2023-01-01T00:00:00Z").toEpochMilli();
        LocalDate dateOnly = LocalDate.parse("2023-01-01");
        long dateOnlyMillis = dateOnly.atStartOfDay(defaultZone).toInstant().toEpochMilli();
        return Stream.of(Arguments.of("2023-01-01T00:00:00Z", isoInstant, "ISO‑8601 UTC date‑time string should be parsed as an instant."), Arguments.of("2023-01-01", dateOnlyMillis, "Date‑only string should be interpreted as midnight in the system default zone."));
    }

    private static Stream<Arguments> temporalNonUtcOffsetsProvider() {
        OffsetDateTime odt = OffsetDateTime.parse("2023-12-31T23:00:00-05:00");
        // 2024-01-01T04:00:00Z
        long odtMillis = odt.toInstant().toEpochMilli();
        ZonedDateTime zdt = ZonedDateTime.parse("2022-06-15T15:45:30+09:00[Asia/Tokyo]");
        // 2022-06-15T06:45:30Z
        long zdtMillis = zdt.toInstant().toEpochMilli();
        return Stream.of(Arguments.of(odt, odtMillis, "OffsetDateTime with negative offset should be normalised to UTC."), Arguments.of(zdt, zdtMillis, "ZonedDateTime with a specific zone should be converted to the same instant in UTC."));
    }

    private static Stream<Arguments> atomicAndNumericWrappersProvider() {
        return Stream.of(Arguments.of(new AtomicLong(9876543210L), 9876543210L, "AtomicLong should be unwrapped to its long value."), Arguments.of(new BigInteger("1234567890123456789"), 1234567890123456789L, "BigInteger should be converted to long when within range."));
    }

    private static Stream<Arguments> invalidTemporalStringsProvider() {
        return Stream.of(Arguments.of("2023-02-30T10:00:00", DateTimeParseException.class, "Impossible date should cause DateTimeParseException."), Arguments.of("not-a-date-time", DateTimeParseException.class, "Completely invalid string should cause DateTimeParseException."));
    }

    private static Stream<Arguments> overflowNumericValuesProvider() {
        return Stream.of(Arguments.of(new BigDecimal("9223372036854775808"), ArithmeticException.class, "BigDecimal exceeding Long.MAX_VALUE should trigger ArithmeticException."), Arguments.of(new BigDecimal("-9223372036854775809"), ArithmeticException.class, "BigDecimal less than Long.MIN_VALUE should trigger ArithmeticException."));
    }

    private static Stream<Arguments> unsupportedBooleanInputProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Boolean.TRUE is not a supported source type."), Arguments.of(false, IllegalArgumentException.class, "Primitive boolean (autoboxed) is not supported."));
    }

    private static Stream<Arguments> temporalTypesWithoutDateComponentProvider() {
        return Stream.of(Arguments.of(LocalTime.of(15, 45, 30), IllegalArgumentException.class, "LocalTime lacks a date component, conversion should fail."), Arguments.of(OffsetTime.parse("10:15:30+02:00"), IllegalArgumentException.class, "OffsetTime lacks a date component, conversion should fail."));
    }

    private static Stream<Arguments> nullConverterWithMapInputProvider() {
        Map<String, Object> map = Collections.singletonMap("epoch", 1609459200000L);
        // CompactMap is part of the library; we create one via its API if available.
        CompactMap<String, Object> compactMap = new CompactMap<>();
        compactMap.put("epoch", 1609459200000L);
        return Stream.of(Arguments.of(map, NullPointerException.class, "Map conversion requires a non‑null Converter; passing null should NPE."), Arguments.of(compactMap, NullPointerException.class, "CompactMap conversion also requires a non‑null Converter; passing null should NPE."));
    }

    @Test
    void toDate_fromDate_returnsSameInstant() {
        Date now = new Date();
        Date result = OffsetDateTimeConversions.toDate(now, converter);
        assertNotNull(result);
        assertEquals(now.getTime(), result.getTime(), "Converting a java.util.Date should preserve the exact epoch millisecond value.");
    }

    @Test
    void toDate_fromOffsetDateTime_returnsCorrectInstant() {
        OffsetDateTime odt = OffsetDateTime.now();
        long expectedMillis = odt.toInstant().toEpochMilli();
        Date result = OffsetDateTimeConversions.toDate(odt, converter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "Converting an OffsetDateTime should yield a Date with the same epoch milliseconds.");
    }

    @Test
    void toDate_fromStringContainingLong_returnsParsedInstant() {
        long nowMillis = System.currentTimeMillis();
        String source = Long.toString(nowMillis);
        Date result = OffsetDateTimeConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(nowMillis, result.getTime(), "A numeric String should be parsed to its long value and converted to a Date.");
    }

    @Test
    void toDate_fromMapWithTimeKey_returnsDateFromMap() {
        long nowMillis = System.currentTimeMillis();
        Map<String, Object> map = Map.of("time", nowMillis);
        Date result = OffsetDateTimeConversions.toDate(map, converter);
        assertNotNull(result);
        assertEquals(nowMillis, result.getTime(), "A Map containing a \"time\" key should be interpreted as epoch milliseconds.");
    }

    @Test
    void toDate_fromNull_returnsEpochZeroDate() {
        // According to Converter's null‑handling contract, a null source converts to 0 for numeric primitives.
        Date result = OffsetDateTimeConversions.toDate(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Converting a null source should result in a Date representing the epoch (0 ms).");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericInputsProvider")
    void toDate_numericInputs(Object from, long expectedMillis, String description) {
        Date result = OffsetDateTimeConversions.toDate(from, null);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("javaDateTimeObjectsProvider")
    void toDate_javaDateTimeObjects(Object from, long expectedMillis, String description) {
        Date result = OffsetDateTimeConversions.toDate(from, null);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
    }

    // --------------------------------------------------------------------- //
    @Test
    void toDate_sqlTimestamp_returnsCorrectInstant() {
        Timestamp ts = new Timestamp(1640995200000L);
        Date result = OffsetDateTimeConversions.toDate(ts, null);
        assertNotNull(result);
        assertEquals(1640995200000L, result.getTime(), "java.sql.Timestamp should be converted using its epoch milliseconds.");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedOrInvalidInputsProvider")
    void toDate_unsupportedOrInvalidInputs(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toDate(from, null), description);
    }

    // --------------------------------------------------------------------- //
    @Test
    void toDate_nullFromAndNullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDate(null, null), "Both arguments null should result in NullPointerException.");
    }

    @Test
    void toDate_nullFromWithNonNullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDate(null, new Converter(new ConverterOptions())), "Null source with a non‑null Converter should still throw NullPointerException.");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("boundaryNumericValuesProvider")
    void toDate_boundaryNumericValues(Object from, long expectedMillis, String description) {
        Date result = OffsetDateTimeConversions.toDate(from, null);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("stringDateRepresentationsProvider")
    void toDate_stringDateRepresentations(String from, long expectedMillis, String description) {
        Date result = OffsetDateTimeConversions.toDate(from, null);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("temporalNonUtcOffsetsProvider")
    void toDate_temporalNonUtcOffsets(Object from, long expectedMillis, String description) {
        Date result = OffsetDateTimeConversions.toDate(from, null);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
    }

    // --------------------------------------------------------------------- //
    @Test
    void toDate_legacySqlDate_returnsCorrectInstant() {
        java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2021, 12, 31));
        ZoneId defaultZone = ZoneId.systemDefault();
        long expected = sqlDate.toLocalDate().atStartOfDay(defaultZone).toInstant().toEpochMilli();
        Date result = OffsetDateTimeConversions.toDate(sqlDate, null);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "java.sql.Date should be treated as start‑of‑day in the default zone.");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("atomicAndNumericWrappersProvider")
    void toDate_atomicAndNumericWrappers(Object from, long expectedMillis, String description) {
        Date result = OffsetDateTimeConversions.toDate(from, null);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidTemporalStringsProvider")
    void toDate_invalidTemporalStrings(String from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toDate(from, null), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("overflowNumericValuesProvider")
    void toDate_overflowNumericValues(BigDecimal from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toDate(from, null), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedBooleanInputProvider")
    void toDate_unsupportedBooleanInput(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toDate(from, null), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("temporalTypesWithoutDateComponentProvider")
    void toDate_temporalTypesWithoutDateComponent(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toDate(from, null), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullConverterWithMapInputProvider")
    void toDate_nullConverterWithMapInput(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toDate(from, null), description);
    }
}
