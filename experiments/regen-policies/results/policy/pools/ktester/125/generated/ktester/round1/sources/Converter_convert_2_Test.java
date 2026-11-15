package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert_2_Test {

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// reference type → null
        Arguments.of(null, String.class, null), // primitive numeric → 0
        Arguments.of(null, int.class, 0), // primitive boolean → false
        Arguments.of(null, boolean.class, false), // primitive char → '\u0000'
        Arguments.of(null, char.class, '\u0000'));
    }

    static Stream<Arguments> identityConversionProvider() {
        return Stream.of(Arguments.of("sample", String.class, "sample"), Arguments.of(123, Integer.class, 123));
    }

    static Stream<Arguments> stringToPrimitiveProvider() {
        return Stream.of(Arguments.of("true", Boolean.class, true), Arguments.of("42", int.class, 42), Arguments.of("3.14", Double.class, 3.14));
    }

    static Stream<Arguments> numberToStringProvider() {
        return Stream.of(Arguments.of(987L, Long.class, "987"), Arguments.of(new BigDecimal("12345.67"), BigDecimal.class, "12345.67"));
    }

    static Stream<Arguments> atomicConversionProvider() {
        return Stream.of(Arguments.of("false", AtomicBoolean.class, new AtomicBoolean(false)), Arguments.of("123", AtomicInteger.class, new AtomicInteger(123)));
    }

    static Stream<Arguments> primitiveToWrapperProvider() {
        return Stream.of(Arguments.of(42, Integer.class, 42), Arguments.of(true, Boolean.class, true));
    }

    static Stream<Arguments> wrapperToPrimitiveProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(7), int.class, 7), Arguments.of(Boolean.FALSE, boolean.class, false));
    }

    static Stream<Arguments> numericToBooleanProvider() {
        return Stream.of(Arguments.of(0, false), Arguments.of(5, true), Arguments.of(-1, true));
    }

    @Test
    void shouldConvertStringToLong() {
        // given
        String source = "12345";
        // when
        Long result = Converter.convert(source, Long.class);
        // then
        assertNotNull(result);
        assertEquals(12345L, result);
    }

    @Test
    void shouldConvertStringToDateUsingDefaultPattern() {
        // given
        // The default conversion for a String → Date expects the ISO‑8601 format.
        String source = "2023-03-15T12:30:45Z";
        // when
        Date result = Converter.convert(source, Date.class);
        // then
        assertNotNull(result);
        // Convert the expected instant to a Date for comparison.
        Instant expectedInstant = Instant.parse(source);
        assertEquals(Date.from(expectedInstant), result);
    }

    @Test
    void shouldConvertLongToLocalDateTime() {
        // given
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        // when
        LocalDateTime result = Converter.convert(epochMilli, LocalDateTime.class);
        // then
        assertNotNull(result);
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault()), result);
    }

    @Test
    void shouldConvertMapToUuid() {
        // given
        UUID expected = UUID.randomUUID();
        Map<String, Object> map = new HashMap<>();
        map.put("mostSigBits", expected.getMostSignificantBits());
        map.put("leastSigBits", expected.getLeastSignificantBits());
        // when
        UUID result = Converter.convert(map, UUID.class);
        // then
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void shouldReturnNullWhenSourceIsNullForObjectTarget() {
        // given
        Object source = null;
        // when
        String result = Converter.convert(source, String.class);
        // then
        assertNull(result);
    }

    @Test
    void shouldReturnZeroWhenSourceIsNullForNumericPrimitive() {
        // given
        Object source = null;
        // when
        int result = Converter.convert(source, int.class);
        // then
        assertEquals(0, result);
    }

    @Test
    void shouldReturnFalseWhenSourceIsNullForBooleanPrimitive() {
        // given
        Object source = null;
        // when
        boolean result = Converter.convert(source, boolean.class);
        // then
        assertFalse(result);
    }

    @Test
    void shouldConvertCollectionToSet() {
        // given
        List<String> list = List.of("a", "b", "c");
        // when
        Set<String> result = Converter.convert(list, Set.class);
        // then
        assertNotNull(result);
        assertEquals(Set.of("a", "b", "c"), result);
    }

    @Test
    void shouldConvertStringToEnum() {
        // given
        String source = "MONDAY";
        // when
        Day result = Converter.convert(source, Day.class);
        // then
        assertNotNull(result);
        assertEquals(Day.MONDAY, result);
    }

    @ParameterizedTest(name = "[{index}] null → {1}")
    @MethodSource("nullHandlingProvider")
    void nullHandling(Object source, Class<?> targetType, Object expected) {
        // When source is null, the converter should return the default for the target.
        Object result = Converter.convert(source, targetType);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] identity conversion of {0}")
    @MethodSource("identityConversionProvider")
    void identityConversion(Object source, Class<?> targetType, Object expected) {
        Object result = Converter.convert(source, targetType);
        // For identity conversion the same instance (or equal primitive) must be returned.
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("stringToPrimitiveProvider")
    void stringToPrimitive(String source, Class<?> targetType, Object expected) {
        Object result = Converter.convert(source, targetType);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {0} ({1}) → String")
    @MethodSource("numberToStringProvider")
    void numberToString(Object source, Class<?> sourceType, String expected) {
        String result = Converter.convert(source, String.class);
        assertEquals(expected, result);
    }

    @Test
    void dateToInstantConversion() {
        Date source = new Date(0L);
        Instant result = Converter.convert(source, Instant.class);
        assertEquals(Instant.EPOCH, result);
    }

    @Test
    void localDateToDateConversion() {
        LocalDate source = LocalDate.of(2023, 5, 17);
        Date result = Converter.convert(source, Date.class);
        // Expected at start of day in the system default zone
        ZonedDateTime zdt = source.atStartOfDay(ZoneId.systemDefault());
        assertEquals(Date.from(zdt.toInstant()), result);
    }

    @Test
    void isoStringToLocalDateTimeConversion() {
        String source = "2023-08-01T12:34:56";
        LocalDateTime result = Converter.convert(source, LocalDateTime.class);
        assertEquals(LocalDateTime.parse(source), result);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("atomicConversionProvider")
    void atomicConversion(String source, Class<?> targetType, Object expected) {
        Object result = Converter.convert(source, targetType);
        assertEquals(expected, result);
    }

    @Test
    void unsupportedConversionThrowsException() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, ZonedDateTime.class), "Conversion from generic Object to ZonedDateTime must fail");
    }

    @Test
    void stringToDayOfWeekEnum() {
        String source = "MONDAY";
        DayOfWeek result = Converter.convert(source, DayOfWeek.class);
        assertEquals(DayOfWeek.MONDAY, result);
    }

    @Test
    void stringToUuid() {
        String source = "123e4567-e89b-12d3-a456-426614174000";
        UUID expected = UUID.fromString(source);
        UUID result = Converter.convert(source, UUID.class);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] primitive {0} → {1}")
    @MethodSource("primitiveToWrapperProvider")
    void primitiveToWrapper(Object primitive, Class<?> wrapperType, Object expected) {
        Object result = Converter.convert(primitive, wrapperType);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] wrapper {0} → {1}")
    @MethodSource("wrapperToPrimitiveProvider")
    void wrapperToPrimitive(Object wrapper, Class<?> primitiveType, Object expected) {
        Object result = Converter.convert(wrapper, primitiveType);
        assertEquals(expected, result);
    }

    @Test
    void numericWideningByteToInteger() {
        byte source = 12;
        Integer result = Converter.convert(source, Integer.class);
        assertEquals(12, result);
    }

    @Test
    void numericWideningShortToLong() {
        short source = 30000;
        Long result = Converter.convert(source, Long.class);
        assertEquals(30000L, result);
    }

    @Test
    void numericNarrowingLongToInt() {
        Long source = 123456789L;
        int result = Converter.convert(source, int.class);
        assertEquals(123456789, result);
    }

    @Test
    void numericNarrowingDoubleToInteger() {
        Double source = 123.987;
        Integer result = Converter.convert(source, Integer.class);
        assertEquals(123, result);
    }

    @ParameterizedTest(name = "[{index}] {0} → Boolean")
    @MethodSource("numericToBooleanProvider")
    void numericToBoolean(Number source, Boolean expected) {
        Boolean result = Converter.convert(source, Boolean.class);
        assertEquals(expected, result);
    }

    @Test
    void stringToCharacter() {
        String source = "A";
        Character result = Converter.convert(source, Character.class);
        assertEquals('A', result);
    }

    @Test
    void charToString() {
        char source = 'Z';
        String result = Converter.convert(source, String.class);
        assertEquals("Z", result);
    }

    @Test
    void instantToDateConversion() {
        Instant source = Instant.now();
        Date result = Converter.convert(source, Date.class);
        assertEquals(Date.from(source), result);
    }

    @Test
    void calendarToZonedDateTimeConversion() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2022‑01‑01T00:00:00Z
        cal.setTimeInMillis(1_640_995_200_000L);
        ZonedDateTime result = Converter.convert(cal, ZonedDateTime.class);
        assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(cal.getTimeInMillis()), ZoneId.of("UTC")), result);
    }

    @Test
    void timestampToLocalDateTimeConversion() {
        java.sql.Timestamp source = java.sql.Timestamp.valueOf("2022-01-01 12:34:56");
        LocalDateTime result = Converter.convert(source, LocalDateTime.class);
        assertEquals(LocalDateTime.of(2022, 1, 1, 12, 34, 56), result);
    }

    @Test
    void zonedDateTimeToInstantConversion() {
        ZonedDateTime source = ZonedDateTime.of(LocalDateTime.of(2021, 12, 31, 23, 0, 0), ZoneId.of("UTC"));
        Instant result = Converter.convert(source, Instant.class);
        assertEquals(Instant.parse("2021-12-31T23:00:00Z"), result);
    }

    @Test
    void byteArrayToByteBuffer() {
        byte[] source = new byte[] { 1, 2, 3 };
        ByteBuffer result = Converter.convert(source, ByteBuffer.class);
        assertArrayEquals(source, result.array());
    }

    @Test
    void byteBufferToByteArray() {
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 10, 20 });
        byte[] result = Converter.convert(source, byte[].class);
        assertArrayEquals(new byte[] { 10, 20 }, result);
    }

    @Test
    void charBufferToString() {
        CharBuffer source = CharBuffer.wrap("hello");
        String result = Converter.convert(source, String.class);
        assertEquals("hello", result);
    }

    @Test
    void stringToCharBuffer() {
        String source = "world";
        CharBuffer result = Converter.convert(source, CharBuffer.class);
        assertEquals(source, result.toString());
    }

    @Test
    void stringToBigDecimal() {
        String source = "12345.6789";
        BigDecimal result = Converter.convert(source, BigDecimal.class);
        assertEquals(new BigDecimal("12345.6789"), result);
    }

    @Test
    void bigDecimalToBigInteger() {
        BigDecimal source = new BigDecimal("9876.54321");
        BigInteger result = Converter.convert(source, BigInteger.class);
        assertEquals(new BigInteger("9876"), result);
    }

    @Test
    void longToBigDecimal() {
        Long source = 123L;
        BigDecimal result = Converter.convert(source, BigDecimal.class);
        assertEquals(new BigDecimal("123"), result);
    }

    @Test
    void invalidUuidStringThrowsException() {
        String source = "not-a-uuid";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, UUID.class), "Malformed UUID strings must raise an exception");
    }

    @Test
    void enumConversionFromOrdinalThrowsException() {
        // ordinal for DayOfWeek.WEDNESDAY
        Integer source = 2;
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, DayOfWeek.class), "Direct ordinal → enum conversion is not supported");
    }

    @Test
    void nonNumericStringToIntThrowsException() {
        String source = "abc";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, int.class), "Non‑numeric string cannot be parsed to int");
    }

    @Test
    void emptyStringToBooleanThrowsException() {
        String source = "";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, Boolean.class), "Empty string is not a valid Boolean representation");
    }

    @Test
    void impossibleDateStringThrowsException() {
        String source = "2023-02-30";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, LocalDate.class));
    }

    @Test
    void invalidMonthStringThrowsException() {
        String source = "2023-13-01T10:00:00";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, LocalDateTime.class));
    }

    @Test
    void nonIsoInstantStringThrowsException() {
        String source = "not-an-instant";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, Instant.class));
    }

    @Test
    void lowerCaseEnumStringThrowsException() {
        String source = "monday";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, DayOfWeek.class));
    }

    @Test
    void unknownEnumConstantThrowsException() {
        String source = "FRIDAYX";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, DayOfWeek.class));
    }

    @Test
    void charToIntegerUnsupported() {
        char source = 'A';
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, Integer.class));
    }

    @Test
    void booleanToIntegerUnsupported() {
        Boolean source = true;
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, Integer.class));
    }

    @Test
    void byteArrayToCharBufferUnsupported() {
        byte[] source = new byte[] { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, CharBuffer.class));
    }

    @Test
    void charBufferToByteArrayUnsupported() {
        CharBuffer source = CharBuffer.wrap("hello");
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, byte[].class));
    }

    @Test
    void malformedBigDecimalStringThrowsException() {
        String source = "12.34.56";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, BigDecimal.class));
    }

    @Test
    void nonNumericStringToBigIntegerThrowsException() {
        String source = "abc123";
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, BigInteger.class));
    }

    @Test
    void listToIntUnsupported() {
        List<Integer> source = List.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, int.class));
    }

    @Test
    void mapToBooleanUnsupported() {
        Map<String, Boolean> source = Collections.singletonMap("true", true);
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, Boolean.class));
    }

    @Test
    void mapToBooleanDirectUnsupported() {
        Map<String, Object> source = Collections.singletonMap("true", true);
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, Boolean.class));
    }
}
