package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class Converter_convertToLong_Test {

    private static Stream<Arguments> additionalNumericProvider() {
        return Stream.of(arguments("Byte primitive value", (byte) 127, 127L), arguments("Short primitive negative value", (short) -32768, -32768L), arguments("Character 'A' → Unicode code point", new Character('A'), 65L), arguments("Wrapper Byte -42", new Byte((byte) -42), -42L), arguments("Wrapper Short 12345", new Short((short) 12345), 12345L), arguments("Double 12345.987 → truncated", new Double(12345.987), 12345L), arguments("Float -9876.543f → truncated", new Float(-9876.543f), -9876L));
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(arguments("Trimmed whitespace", "   42   ", 42L), arguments("Explicit plus sign", "+123", 123L), arguments("Binary literal 0b1010", "0b1010", 10L), arguments("Zero string", "0", 0L), arguments("Empty string → NFE", "", NumberFormatException.class), arguments("Non‑numeric string → NFE", "notANumber", NumberFormatException.class));
    }

    private static Stream<Arguments> boundaryOverflowProvider() {
        return Stream.of(arguments("Long.MAX_VALUE unchanged", Long.MAX_VALUE, 9223372036854775807L), arguments("Long.MIN_VALUE unchanged", Long.MIN_VALUE, -9223372036854775808L), arguments("BigInteger overflow → NFE", new BigInteger("9223372036854775808"), NumberFormatException.class), arguments("BigDecimal underflow → NFE", new BigDecimal("-9223372036854775809"), NumberFormatException.class), arguments("Double.NaN → NFE", Double.NaN, NumberFormatException.class), arguments("Double.POSITIVE_INFINITY → NFE", Double.POSITIVE_INFINITY, NumberFormatException.class));
    }

    private static Stream<Arguments> temporalNonUtcProvider() {
        // 2021‑01‑01T00:00:00Z
        long epoch = 1609459200000L;
        return Stream.of(arguments("ZonedDateTime America/New_York → UTC epoch", ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.of("America/New_York")), epoch), arguments("Calendar Asia/Tokyo → same epoch", createCalendar(TimeZone.getTimeZone("Asia/Tokyo"), epoch), epoch), arguments("LocalDate interpreted as start‑of‑day UTC", LocalDate.of(2021, 1, 1), epoch));
    }

    private static Calendar createCalendar(TimeZone tz, long millis) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(millis);
        return cal;
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(arguments("UUID unsupported", UUID.randomUUID(), UnsupportedOperationException.class), arguments("Arbitrary Object unsupported", new Object(), UnsupportedOperationException.class), arguments("CharBuffer toString() fails → NFE", CharBuffer.allocate(10).put("123").flip(), NumberFormatException.class), arguments("ByteBuffer unsupported", ByteBuffer.wrap(new byte[] { 0, 0, 0, 1 }), UnsupportedOperationException.class));
    }

    private static Stream<Arguments> stringOverflowProvider() {
        return Stream.of(arguments("Decimal string > Long.MAX_VALUE", "9223372036854775808", NumberFormatException.class), arguments("Decimal string < Long.MIN_VALUE", "-9223372036854775809", NumberFormatException.class), arguments("Invalid hex literal", "0xGHI", NumberFormatException.class), arguments("Invalid binary literal", "0b2", NumberFormatException.class), arguments("Scientific notation not supported", "1e3", NumberFormatException.class), arguments("Underscore in numeric literal", "1_000", NumberFormatException.class));
    }

    private static Stream<Arguments> floatingPointProvider() {
        return Stream.of(arguments("Double.NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY, NumberFormatException.class), arguments("Float.NaN", Float.NaN, NumberFormatException.class), arguments("Float.POSITIVE_INFINITY", Float.POSITIVE_INFINITY, NumberFormatException.class), arguments("Float.NEGATIVE_INFINITY", Float.NEGATIVE_INFINITY, NumberFormatException.class));
    }

    private static Stream<Arguments> unsupportedContainersProvider() {
        return Stream.of(arguments("Primitive int array", new int[] { 1, 2, 3 }, UnsupportedOperationException.class), arguments("List of integers", List.of(1, 2, 3), UnsupportedOperationException.class), arguments("Map with a single entry", Map.of("a", 1), UnsupportedOperationException.class));
    }

    private static Stream<Arguments> temporalOverflowProvider() {
        // Helper constants – we deliberately exceed the range of epoch milliseconds that fit in a long
        // seconds, will overflow when converted to millis
        long beyondMax = Long.MAX_VALUE / 1000 + 1;
        long beyondMin = Long.MIN_VALUE / 1000 - 1;
        return Stream.of(arguments("Instant beyond Long.MAX_VALUE millis", Instant.ofEpochSecond(beyondMax), NumberFormatException.class), arguments("Instant below Long.MIN_VALUE millis", Instant.ofEpochSecond(beyondMin), NumberFormatException.class));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void convert_null_returnsNull() {
        assertNull(Converter.convertToLong(null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void convert_longInstance_returnsSameValue() {
        Long source = 123L;
        Long result = Converter.convertToLong(source);
        assertEquals(source, result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void convert_integer_returnsLong() {
        Integer source = 42;
        Long result = Converter.convertToLong(source);
        assertEquals(42L, result);
    }

    @Test
    void convert_short_returnsLong() {
        Short source = (short) 7;
        Long result = Converter.convertToLong(source);
        assertEquals(7L, result);
    }

    @Test
    void convert_byte_returnsLong() {
        Byte source = (byte) 5;
        Long result = Converter.convertToLong(source);
        assertEquals(5L, result);
    }

    @Test
    void convert_bigDecimal_returnsLong() {
        BigDecimal source = new BigDecimal("12345.67");
        Long result = Converter.convertToLong(source);
        // fractional part is truncated
        assertEquals(12345L, result);
    }

    @Test
    void convert_bigInteger_returnsLong() {
        BigInteger source = new BigInteger("9876543210");
        Long result = Converter.convertToLong(source);
        assertEquals(9876543210L, result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void convert_numericString_returnsLong() {
        String source = "555";
        Long result = Converter.convertToLong(source);
        assertEquals(555L, result);
    }

    @Test
    void convert_invalidString_throwsNumberFormatException() {
        String source = "not-a-number";
        assertThrows(NumberFormatException.class, () -> Converter.convertToLong(source));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void convert_trueBoolean_returnsOne() {
        Boolean source = Boolean.TRUE;
        Long result = Converter.convertToLong(source);
        assertEquals(1L, result);
    }

    @Test
    void convert_falseBoolean_returnsZero() {
        Boolean source = Boolean.FALSE;
        Long result = Converter.convertToLong(source);
        assertEquals(0L, result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void convert_date_returnsEpochMillis() {
        // a fixed instant
        Date source = new Date(1_600_000_000_000L);
        Long result = Converter.convertToLong(source);
        assertEquals(1_600_000_000_000L, result);
    }

    @Test
    void convert_calendar_returnsEpochMillis() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_610_000_000_000L);
        Long result = Converter.convertToLong(cal);
        assertEquals(1_610_000_000_000L, result);
    }

    @Test
    void convert_instant_returnsEpochMillis() {
        Instant instant = Instant.ofEpochMilli(1_620_000_000_000L);
        Long result = Converter.convertToLong(instant);
        assertEquals(1_620_000_000_000L, result);
    }

    @Test
    void convert_localDateTime_returnsEpochMillis() {
        // 2021‑05‑01T12:34:56Z
        LocalDateTime ldt = LocalDateTime.of(2021, 5, 1, 12, 34, 56);
        long expected = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        Long result = Converter.convertToLong(ldt);
        assertEquals(expected, result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void convert_atomicLong_returnsValue() {
        AtomicLong source = new AtomicLong(777L);
        Long result = Converter.convertToLong(source);
        assertEquals(777L, result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void convert_bigDecimalOverflow_clampsToLongMax() {
        BigDecimal source = new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE);
        Long result = Converter.convertToLong(source);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    void convert_bigDecimalUnderflow_clampsToLongMin() {
        BigDecimal source = new BigDecimal(Long.MIN_VALUE).subtract(BigDecimal.ONE);
        Long result = Converter.convertToLong(source);
        assertEquals(Long.MIN_VALUE, result);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalNumericProvider")
    void convert_additionalNumericTypes(String description, Object input, Long expected) {
        Long result = Converter.convertToLong(input);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringParsingProvider")
    void convert_stringParsingVariations(String description, String input, Object expected) {
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expected;
            assertThrows(ex, () -> Converter.convertToLong(input), description);
        } else {
            Long result = Converter.convertToLong(input);
            assertEquals(expected, result, description);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryOverflowProvider")
    void convert_boundaryAndOverflow(String description, Object input, Object expected) {
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expected;
            assertThrows(ex, () -> Converter.convertToLong(input), description);
        } else {
            Long result = Converter.convertToLong(input);
            assertEquals(expected, result, description);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("temporalNonUtcProvider")
    void convert_temporalNonUtc(String description, Object input, Long expected) {
        Long result = Converter.convertToLong(input);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedTypesProvider")
    void convert_unsupportedTypes(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToLong(input), description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringOverflowProvider")
    void convert_stringOverflowAndFormat(String description, String input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToLong(input), description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("floatingPointProvider")
    void convert_floatingPointEdgeCases(String description, Number input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToLong(input), description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedContainersProvider")
    void convert_unsupportedContainers(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToLong(input), description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("temporalOverflowProvider")
    void convert_temporalOverflow(String description, Instant input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToLong(input), description);
    }
}
