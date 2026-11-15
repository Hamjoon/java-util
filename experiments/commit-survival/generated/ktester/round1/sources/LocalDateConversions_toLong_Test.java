package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toLong_Test {

    /**
     * The Converter instance used for all tests.
     */
    private Converter converter;

    /**
     * Mocked ConverterOptions – we only need it to satisfy the Converter constructor.
     */
    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the options so that getConverterOverrides() returns an empty map.
        // This prevents the real Converter from trying to load any external overrides.
        org.mockito.Mockito.when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        // Create the Converter under test.
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test at the moment.
        converter = null;
    }

    private static Stream<Arguments> provideDateCases() {
        return Stream.of(Arguments.of(new Date(0L), 0L), Arguments.of(new Date(1_622_548_800_000L), 1_622_548_800_000L));
    }

    private static Stream<Arguments> provideInstantCases() {
        return Stream.of(Arguments.of(Instant.ofEpochMilli(0L), 0L), Arguments.of(Instant.ofEpochMilli(1_622_548_800_000L), 1_622_548_800_000L), Arguments.of(Instant.ofEpochMilli(-123_456_789L), -123_456_789L));
    }

    private static Stream<Arguments> provideLocalDateTimeCases() {
        return Stream.of(Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 0), 0L), Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 0, 0), 1_622_548_800_000L), Arguments.of(LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999_000_000), -1L));
    }

    private static Stream<Arguments> provideLocalDateCases() {
        return Stream.of(Arguments.of(LocalDate.of(1970, 1, 1), 0L), Arguments.of(LocalDate.of(2021, 6, 1), 1_622_505_600_000L));
    }

    private static Stream<Arguments> provideZonedDateTimeCases() {
        return Stream.of(Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC), 1_622_548_800_000L), Arguments.of(ZonedDateTime.of(2021, 6, 1, 7, 0, 0, 0, ZoneOffset.ofHours(-5)), 1_622_548_800_000L));
    }

    private static Stream<Arguments> provideOffsetDateTimeCases() {
        return Stream.of(Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC), 1_622_548_800_000L), Arguments.of(OffsetDateTime.of(2021, 6, 1, 7, 0, 0, 0, ZoneOffset.ofHours(-5)), 1_622_548_800_000L));
    }

    private static Stream<Arguments> provideCalendarCases() {
        Calendar utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(0L);
        Calendar nyCal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        nyCal.setTimeInMillis(1_622_548_800_000L);
        return Stream.of(Arguments.of(utcCal, 0L), Arguments.of(nyCal, 1_622_548_800_000L));
    }

    private static Stream<Arguments> provideTimestampCases() {
        return Stream.of(Arguments.of(Timestamp.from(Instant.ofEpochMilli(0L)), 0L), Arguments.of(Timestamp.from(Instant.ofEpochMilli(1_622_548_800_123L)), 1_622_548_800_123L));
    }

    private static Stream<Arguments> provideSqlDateCases() {
        return Stream.of(Arguments.of(java.sql.Date.valueOf(LocalDate.of(1970, 1, 1)), 0L), Arguments.of(java.sql.Date.valueOf(LocalDate.of(2021, 6, 1)), 1_622_505_600_000L));
    }

    private static Stream<Arguments> provideNumericWrapperCases() {
        return Stream.of(Arguments.of(Long.valueOf(1_622_548_800_000L), 1_622_548_800_000L), Arguments.of(Integer.valueOf(1_622_548_800), 1_622_548_800L), Arguments.of(Double.valueOf(1_622_548_800_000.0), 1_622_548_800_000L));
    }

    private static Stream<Arguments> provideBigNumberCases() {
        return Stream.of(Arguments.of(new BigDecimal("1622548800000"), 1_622_548_800_000L), Arguments.of(new BigInteger("1622548800000"), 1_622_548_800_000L));
    }

    private static Stream<Arguments> provideAtomicLongCases() {
        return Stream.of(Arguments.of(new AtomicLong(1_622_548_800_000L), 1_622_548_800_000L));
    }

    private static Stream<Arguments> provideStringNumberCases() {
        return Stream.of(Arguments.of("1622548800000", 1_622_548_800_000L), Arguments.of("-123456789", -123_456_789L));
    }

    private static Stream<Arguments> provideNullOrUnsupportedCases() {
        return Stream.of(Arguments.of(null, NullPointerException.class), Arguments.of(Boolean.TRUE, IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideMalformedStringCases() {
        return Stream.of(Arguments.of("12345abc", NumberFormatException.class), Arguments.of("   ", NumberFormatException.class), Arguments.of("notANumber", NumberFormatException.class));
    }

    @Test
    void toLong_givenInstant_returnsEpochMilli() {
        // GIVEN
        // a fixed point in time
        Instant instant = Instant.ofEpochMilli(1_642_000_000_000L);
        // WHEN
        long result = LocalDateConversions.toLong(instant, converter);
        // THEN
        assertEquals(instant.toEpochMilli(), result, "toLong should return the same epoch milli as the source Instant");
    }

    @Test
    void toLong_givenDate_returnsTime() {
        // GIVEN
        // a fixed java.util.Date
        Date date = new Date(1_642_000_123_456L);
        // WHEN
        long result = LocalDateConversions.toLong(date, converter);
        // THEN
        assertEquals(date.getTime(), result, "toLong should return the same millisecond value as the source Date");
    }

    @Test
    void toLong_givenLongNumber_returnsSameLong() {
        // GIVEN
        Long source = 1_234_567_890L;
        // WHEN
        long result = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(source.longValue(), result, "toLong should return the long value of a Number instance");
    }

    @Test
    void toLong_givenIntegerNumber_returnsLongValue() {
        // GIVEN
        Integer source = 42;
        // WHEN
        long result = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(source.longValue(), result, "toLong should correctly widen an Integer to long");
    }

    @Test
    void toLong_givenStringNumber_returnsParsedLong() {
        // GIVEN
        String source = "987654321";
        // WHEN
        long result = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(Long.parseLong(source), result, "toLong should parse a numeric String to its long representation");
    }

    @Test
    void toLong_givenNull_returnsZero() {
        // GIVEN
        Object source = null;
        // WHEN
        long result = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(0L, result, "toLong should return 0 for a null source (the Converter treats null as zero for primitives)");
    }

    @Test
    void toLong_givenUnsupportedType_throwsException() {
        // GIVEN
        // not convertible by default
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toLong(unsupported, converter), "toLong should throw when the source type cannot be converted to an Instant");
    }

    @ParameterizedTest(name = "[{index}] Date -> {1}")
    @MethodSource("provideDateCases")
    void toLong_fromDate(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] Instant -> {1}")
    @MethodSource("provideInstantCases")
    void toLong_fromInstant(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] LocalDateTime -> {1}")
    @MethodSource("provideLocalDateTimeCases")
    void toLong_fromLocalDateTime(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] LocalDate -> {1}")
    @MethodSource("provideLocalDateCases")
    void toLong_fromLocalDate(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] ZonedDateTime -> {1}")
    @MethodSource("provideZonedDateTimeCases")
    void toLong_fromZonedDateTime(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] OffsetDateTime -> {1}")
    @MethodSource("provideOffsetDateTimeCases")
    void toLong_fromOffsetDateTime(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] Calendar -> {1}")
    @MethodSource("provideCalendarCases")
    void toLong_fromCalendar(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] Timestamp -> {1}")
    @MethodSource("provideTimestampCases")
    void toLong_fromTimestamp(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] java.sql.Date -> {1}")
    @MethodSource("provideSqlDateCases")
    void toLong_fromSqlDate(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] Numeric wrapper -> {1}")
    @MethodSource("provideNumericWrapperCases")
    void toLong_fromNumericWrapper(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] BigDecimal/BigInteger -> {1}")
    @MethodSource("provideBigNumberCases")
    void toLong_fromBigNumber(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] AtomicLong -> {1}")
    @MethodSource("provideAtomicLongCases")
    void toLong_fromAtomicLong(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] String numeric -> {1}")
    @MethodSource("provideStringNumberCases")
    void toLong_fromStringNumber(Object from, long expected) {
        assertEquals(expected, LocalDateConversions.toLong(from, converter));
    }

    @ParameterizedTest(name = "[{index}] Null/unsupported -> {1}")
    @MethodSource("provideNullOrUnsupportedCases")
    void toLong_nullOrUnsupported(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateConversions.toLong(from, converter));
    }

    @Test
    void toLong_nullConverter_throwsNPE() {
        Date date = new Date(0L);
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toLong(date, null));
    }

    @ParameterizedTest(name = "[{index}] Malformed String -> {1}")
    @MethodSource("provideMalformedStringCases")
    void toLong_malformedString(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateConversions.toLong(from, converter));
    }

    @Test
    void toLong_unsupportedCollection_throwsIllegalArgumentException() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toLong(list, converter));
    }

    @Test
    void toLong_bigDecimalWithFraction_throwsArithmeticException() {
        BigDecimal fractional = new BigDecimal("1622548800.5");
        assertThrows(ArithmeticException.class, () -> LocalDateConversions.toLong(fractional, converter));
    }
}
