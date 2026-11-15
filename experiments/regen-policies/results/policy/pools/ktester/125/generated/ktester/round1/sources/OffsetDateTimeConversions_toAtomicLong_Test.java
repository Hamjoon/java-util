package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toAtomicLong_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance; using the default constructor is sufficient for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> provideNumberInputs() {
        return Stream.of(// Long
        Arguments.of(123L, 123L), // Integer
        Arguments.of(456, 456L), Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    private static Stream<Arguments> provideStringNumericInputs() {
        return Stream.of(Arguments.of("789", 789L), Arguments.of("-42", -42L), // from original simple test
        Arguments.of("123", 123L));
    }

    private static Stream<Arguments> provideStringNonNumericInputs() {
        return Stream.of(Arguments.of("abc", NumberFormatException.class), Arguments.of("", NumberFormatException.class));
    }

    private static Stream<Arguments> provideBooleanInputs() {
        return Stream.of(Arguments.of(Boolean.TRUE, 1L), Arguments.of(Boolean.FALSE, 0L));
    }

    private static Stream<Arguments> provideBigNumberInputs() {
        return Stream.of(// truncation
        Arguments.of(new BigDecimal("12345.67"), 12345L), Arguments.of(new BigInteger("9876543210"), 9876543210L));
    }

    private static Stream<Arguments> provideBigNumberOverflowInputs() {
        return Stream.of(// > Long.MAX_VALUE
        Arguments.of(new BigInteger("9223372036854775808"), ArithmeticException.class), // > Long.MAX_VALUE
        Arguments.of(new BigDecimal("9223372036854775808"), ArithmeticException.class));
    }

    private static Stream<Arguments> provideTemporalMillisInputs() {
        // 2021‑01‑01T00:00:00Z
        long epoch = 1609459200000L;
        Date date = new Date(epoch);
        Instant instant = Instant.ofEpochMilli(epoch);
        Timestamp timestamp = new Timestamp(epoch);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(epoch);
        return Stream.of(Arguments.of(date, epoch), Arguments.of(instant, epoch), Arguments.of(timestamp, epoch), Arguments.of(calendar, epoch));
    }

    private static Stream<Arguments> provideInvalidDoubleInputs() {
        return Stream.of(Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Double.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("toAtomicLong returns 0 when source is null")
    void testNullInput() {
        // given
        Object from = null;
        // when
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should never be null");
        assertEquals(0L, result.get(), "Null input should be converted to 0");
    }

    @Test
    @DisplayName("toAtomicLong correctly converts a Long value")
    void testLongInput() {
        // given
        Long from = 12345L;
        // when
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // then
        assertEquals(12345L, result.get(), "Long value should be preserved");
    }

    @Test
    @DisplayName("toAtomicLong correctly converts a String representing a number")
    void testStringInput() {
        // given
        String from = "67890";
        // when
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // then
        assertEquals(67890L, result.get(), "String numeric value should be parsed to long");
    }

    @Test
    @DisplayName("toAtomicLong correctly converts an Integer value")
    void testIntegerInput() {
        // given
        Integer from = 42;
        // when
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // then
        assertEquals(42L, result.get(), "Integer should be widened to long");
    }

    @Test
    @DisplayName("toAtomicLong correctly converts an OffsetDateTime to epoch milliseconds")
    void testOffsetDateTimeInput() {
        // given: a known instant (2023‑01‑01T00:00:00Z)
        OffsetDateTime from = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        long expectedMillis = from.toInstant().toEpochMilli();
        // when
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // then
        assertEquals(expectedMillis, result.get(), "OffsetDateTime should be converted to epoch milliseconds");
    }

    @Test
    @DisplayName("toAtomicLong throws NullPointerException when converter is null")
    void testNullConverter() {
        // given
        Object from = 123L;
        // when / then
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toAtomicLong(from, null), "Passing a null Converter should result in NullPointerException");
    }

    @ParameterizedTest(name = "{index} => from={0}, expected={1}")
    @MethodSource("provideNumberInputs")
    @DisplayName("Number inputs are correctly converted to AtomicLong")
    void testNumberInputs(Object from, long expected) {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        assertEquals(expected, result.get(), () -> "Expected AtomicLong value " + expected + " for input " + from);
    }

    @ParameterizedTest(name = "{index} => from=\"{0}\", expected={1}")
    @MethodSource("provideStringNumericInputs")
    @DisplayName("Numeric strings are parsed to AtomicLong")
    void testStringNumericInputs(String from, long expected) {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        assertEquals(expected, result.get(), () -> "String \"" + from + "\" should be parsed to " + expected);
    }

    @ParameterizedTest(name = "{index} => from=\"{0}\" throws {1}")
    @MethodSource("provideStringNonNumericInputs")
    @DisplayName("Non‑numeric strings cause NumberFormatException")
    void testStringNonNumericInputs(String from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toAtomicLong(from, converter), () -> "String \"" + from + "\" should cause " + expectedException.getSimpleName());
    }

    @ParameterizedTest(name = "{index} => from={0}, expected={1}")
    @MethodSource("provideBooleanInputs")
    @DisplayName("Boolean values are converted to 1 (true) or 0 (false)")
    void testBooleanInputs(Boolean from, long expected) {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        assertEquals(expected, result.get(), () -> "Boolean " + from + " should be converted to " + expected);
    }

    @ParameterizedTest(name = "{index} => from={0}, expected={1}")
    @MethodSource("provideBigNumberInputs")
    @DisplayName("BigDecimal and BigInteger are converted to AtomicLong (truncating if needed)")
    void testBigNumberInputs(Object from, long expected) {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        assertEquals(expected, result.get(), () -> "Big number " + from + " should become " + expected);
    }

    @ParameterizedTest(name = "{index} => from={0} throws {1}")
    @MethodSource("provideBigNumberOverflowInputs")
    @DisplayName("Overflowing BigDecimal/BigInteger cause ArithmeticException")
    void testBigNumberOverflow(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toAtomicLong(from, converter), () -> "Overflowing value " + from + " should throw " + expectedException.getSimpleName());
    }

    @ParameterizedTest(name = "{index} => from={0}, expected={1}")
    @MethodSource("provideTemporalMillisInputs")
    @DisplayName("Temporal objects are converted to epoch‑millisecond AtomicLong")
    void testTemporalMillisInputs(Object from, long expected) {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        assertEquals(expected, result.get(), () -> "Temporal " + from + " should become " + expected);
    }

    @Test
    @DisplayName("LocalDateTime epoch start converts to 0")
    void testLocalDateTimeEpochStart() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0);
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(ldt, converter);
        assertEquals(0L, result.get(), "Epoch start LocalDateTime should be 0 milliseconds");
    }

    @Test
    @DisplayName("OffsetDateTime with UTC offset converts to its epoch milliseconds")
    void testOffsetDateTimeConversion() {
        long expected = 123456789L;
        OffsetDateTime odt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(expected), ZoneOffset.UTC);
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(odt, converter);
        assertEquals(expected, result.get(), "OffsetDateTime should be converted to epoch milliseconds");
    }

    @Test
    @DisplayName("ZonedDateTime in UTC converts to its epoch milliseconds")
    void testZonedDateTimeConversion() {
        long expected = 987654321L;
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(expected), ZoneId.of("UTC"));
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(zdt, converter);
        assertEquals(expected, result.get(), "ZonedDateTime should be converted to epoch milliseconds");
    }

    @Test
    @DisplayName("Unsupported object type throws IllegalArgumentException")
    void testUnsupportedObject() {
        Object from = new Object();
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toAtomicLong(from, converter), "Unsupported source type should trigger IllegalArgumentException");
    }

    @Test
    @DisplayName("OffsetTime is not supported and throws IllegalArgumentException")
    void testUnsupportedOffsetTime() {
        OffsetTime from = OffsetTime.now();
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toAtomicLong(from, converter), "OffsetTime is not a supported source type");
    }

    @ParameterizedTest(name = "{index} => from={0} throws IllegalArgumentException")
    @MethodSource("provideInvalidDoubleInputs")
    @DisplayName("NaN and Infinity double values are illegal for long conversion")
    void testInvalidDoubleInputs(Double from) {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toAtomicLong(from, converter), () -> "Double value " + from + " should cause IllegalArgumentException");
    }
}
