package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.Timestamp;

@ExtendWith(MockitoExtension.class)
class Converter_convertToString_Test {

    /**
     * Provides a stream of arguments for the basic conversion tests.
     * Each element is an {@code Object[]} where:
     * <ul>
     *   <li>[0] – the input value</li>
     *   <li>[1] – the expected string result (null allowed)</li>
     *   <li>[2] – description for readability</li>
     * </ul>
     */
    private static List<Object[]> basicConversionProvider() {
        return List.of(new Object[] { "simple string", "simple string", "String should return itself" }, new Object[] { null, null, "Null input should return null" }, new Object[] { 123, "123", "Integer conversion" }, new Object[] { true, "true", "Boolean conversion" }, new Object[] { new BigDecimal("12.34"), "12.34", "BigDecimal conversion" }, new Object[] { LocalDate.of(2023, 5, 15), "2023-05-15", "LocalDate ISO‑8601" }, new Object[] { LocalDateTime.of(2023, 5, 15, 13, 45, 30), "2023-05-15T13:45:30", "LocalDateTime ISO‑8601" }, new Object[] { Date.from(Instant.parse("2023-05-15T13:45:30Z")), Date.from(Instant.parse("2023-05-15T13:45:30Z")).toString(), "java.util.Date default toString" }, new Object[] { new Calendar.Builder().setDate(2023, Calendar.MAY, 15).setTimeOfDay(13, 45, 30, 0).build(), // The library converts Calendar to its Date representation, which then uses Date.toString()
        new Calendar.Builder().setDate(2023, Calendar.MAY, 15).setTimeOfDay(13, 45, 30, 0).build().getTime().toString(), "Calendar conversion" }, new Object[] { new AtomicInteger(42), "42", "AtomicInteger conversion" }, new Object[] { new AtomicBoolean(true), "true", "AtomicBoolean conversion" }, new Object[] { new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        }, "custom", "Object with overridden toString" }, new Object[] { Character.valueOf('A'), "A", "Character conversion" }, new Object[] { new BigInteger("12345678901234567890"), "12345678901234567890", "BigInteger conversion" }, new Object[] { Timestamp.valueOf("2023-05-15 13:45:30.123"), "2023-05-15 13:45:30.123", "SQL Timestamp conversion" }, new Object[] { ZonedDateTime.parse("2023-05-15T13:45:30+02:00[Europe/Paris]"), "2023-05-15T13:45:30+02:00[Europe/Paris]", "ZonedDateTime conversion" });
    }

    @Test
    void convertNullReturnsNull() {
        // given
        Object input = null;
        // when
        String result = Converter.convertToString(input);
        // then
        assertNull(result, "Converting null should return null");
    }

    @Test
    void convertStringReturnsSameInstance() {
        // given
        String input = "Hello, World!";
        // when
        String result = Converter.convertToString(input);
        // then
        assertSame(input, result, "Converting a String should return the same instance");
    }

    @Test
    void convertIntegerToString() {
        // given
        Integer input = 42;
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals("42", result);
    }

    @Test
    void convertLongToString() {
        // given
        Long input = 123456789L;
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals("123456789", result);
    }

    @Test
    void convertDoubleToString() {
        // given
        Double input = 3.14159;
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals("3.14159", result);
    }

    @Test
    void convertBooleanToString() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals("true", result);
    }

    @Test
    void convertBigDecimalToString() {
        // given
        BigDecimal input = new BigDecimal("12345.6789");
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals("12345.6789", result);
    }

    @Test
    void convertBigIntegerToString() {
        // given
        BigInteger input = new BigInteger("9876543210");
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals("9876543210", result);
    }

    @Test
    void convertDateToString() {
        // given
        // Epoch
        Date input = new Date(0L);
        // when
        String result = Converter.convertToString(input);
        // then
        // The default conversion for Date uses Date.toString()
        assertEquals(input.toString(), result);
    }

    @Test
    void convertLocalDateToString() {
        // given
        LocalDate input = LocalDate.of(2022, Month.MARCH, 15);
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals(input.toString(), result);
    }

    @Test
    void convertLocalDateTimeToString() {
        // given
        LocalDateTime input = LocalDateTime.of(2022, Month.MARCH, 15, 10, 30, 45);
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals(input.toString(), result);
    }

    @Test
    void convertZonedDateTimeToString() {
        // given
        ZonedDateTime input = ZonedDateTime.of(2022, 3, 15, 10, 30, 45, 0, ZoneId.of("UTC"));
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals(input.toString(), result);
    }

    @Test
    void convertCalendarToString() {
        // given
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // Epoch
        cal.setTimeInMillis(0L);
        // when
        String result = Converter.convertToString(cal);
        // then
        // Calendar conversion ends up delegating to Date conversion
        assertEquals(cal.getTime().toString(), result);
    }

    @Test
    void convertCharToString() {
        // given
        Character input = 'X';
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals("X", result);
    }

    @Test
    void convertByteArrayToString() {
        // given
        byte[] input = "test".getBytes();
        // when
        String result = Converter.convertToString(input);
        // then
        // The library converts a byte[] to a String using the platform default charset.
        assertEquals(new String(input), result);
    }

    @Test
    void convertObjectArrayToString() {
        // given
        Object[] input = new Object[] { 1, "two", true };
        // when
        String result = Converter.convertToString(input);
        // then
        // Expected format is the array's toString representation: "[1, two, true]"
        assertEquals(Arrays.toString(input), result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicConversionProvider")
    void convertToString_basicCases(Object input, String expected, String description) {
        // when
        String result = Converter.convertToString(input);
        // then
        assertEquals(expected, result, description);
    }

    @Test
    void convertFileUnsupportedThrows() {
        // given
        java.io.File file = new java.io.File("/tmp/nonexistent.txt");
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToString(file), "Converting a File should throw IllegalArgumentException because no conversion is registered");
    }

    @Test
    void convertObjectToStringThrowsRuntimeException() {
        // given an object whose toString throws
        Object badToString = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("toString failure");
            }
        };
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> Converter.convertToString(badToString), "Exception from toString should be propagated");
        assertEquals("toString failure", ex.getMessage());
    }

    @Test
    void convertPrimitiveIntArrayUnsupportedThrows() {
        // given
        int[] primitiveArray = new int[] { 1, 2, 3 };
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToString(primitiveArray), "Primitive int[] is not supported for direct conversion to String");
    }

    @Test
    void convertMapUnsupportedThrows() {
        // given
        Map<String, String> map = new HashMap<>();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToString(map), "Map without a specific conversion rule should trigger IllegalArgumentException");
    }
}
