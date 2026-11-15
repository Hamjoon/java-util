package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.nio.file.Path;
import java.nio.file.Paths;

class Converter_convert2String_Test {

    /**
     * Simple POJO that overrides {@code toString()}
     */
    private static final class Foo {

        @Override
        public String toString() {
            return "Foo";
        }
    }

    /**
     * POJO whose {@code toString()} deliberately throws a {@link RuntimeException}
     */
    private static final class ThrowingToString {

        @Override
        public String toString() {
            throw new RuntimeException("toString failure");
        }
    }

    /**
     * Marker class used to register a broken conversion function
     */
    private static final class BrokenConversion {
        // no content needed
    }

    private static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(123), "123"), Arguments.of(Long.valueOf(1234567890123L), "1234567890123"), Arguments.of(Double.valueOf(12.34), "12.34"), Arguments.of(Float.valueOf(5.6f), "5.6"), Arguments.of(Boolean.TRUE, "true"), Arguments.of(Boolean.FALSE, "false"), Arguments.of(Character.valueOf('A'), "A"));
    }

    private static Stream<Arguments> charSequenceProvider() {
        return Stream.of(Arguments.of("hello world", "hello world"), Arguments.of(CharBuffer.wrap("buffer content"), "buffer content"), Arguments.of(CharBuffer.allocate(0), ""));
    }

    private static Stream<Arguments> byteBufferProvider() {
        return Stream.of(Arguments.of(ByteBuffer.wrap(new byte[] { 65, 66, 67 }), "ABC"), Arguments.of(ByteBuffer.allocate(0), ""));
    }

    private static Stream<Arguments> dateTimeProvider() {
        return Stream.of(Arguments.of(new Date(0L), "Thu Jan 01 00:00:00 GMT 1970"), Arguments.of(LocalDate.of(2020, 1, 1), "2020-01-01"), Arguments.of(LocalDateTime.of(2020, 1, 1, 12, 30, 45), "2020-01-01T12:30:45"), Arguments.of(ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 12, 30), ZoneId.of("UTC")), "2020-01-01T12:30Z[UTC]"));
    }

    private static Stream<Arguments> simpleObjectProvider() {
        return Stream.of(Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "123e4567-e89b-12d3-a456-426614174000"), Arguments.of(new BigDecimal("12345.6789"), "12345.6789"), Arguments.of(new BigInteger("9876543210"), "9876543210"));
    }

    private static Stream<Arguments> collectionArrayProvider() {
        return Stream.of(Arguments.of(List.of(1, 2, 3), "[1, 2, 3]"), Arguments.of(new LinkedHashSet<>(List.of("a", "b")), "[a, b]"), Arguments.of(new int[] { 1, 2, 3 }, "[1, 2, 3]"), Arguments.of(new String[] { "x", "y" }, "[x, y]"));
    }

    private static Stream<Arguments> emptyWhitespaceProvider() {
        return Stream.of(Arguments.of("", ""), Arguments.of("   ", "   "));
    }

    @Test
    void convert2String_returnsEmptyStringWhenInputIsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        String result = Converter.convert2String(input);
        // THEN
        assertEquals("", result, "convert2String(null) should return an empty string");
    }

    @Test
    void convert2String_convertsVariousObjectsToString() {
        // GIVEN
        Integer intInput = 42;
        Double doubleInput = 3.14;
        // epoch
        java.util.Date dateInput = new java.util.Date(0L);
        java.util.UUID uuidInput = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        // WHEN / THEN
        assertEquals("42", Converter.convert2String(intInput), "Integer should be converted to its string representation");
        assertEquals("3.14", Converter.convert2String(doubleInput), "Double should be converted to its string representation");
        // Date conversion uses the default format defined by the library; we only assert it is non‑empty
        assertFalse(Converter.convert2String(dateInput).isEmpty(), "Date should be converted to a non‑empty string");
        assertEquals(uuidInput.toString(), Converter.convert2String(uuidInput), "UUID should be converted to its standard string form");
    }

    // ---------- convert2String - primitive wrappers and primitives ----------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("primitiveWrapperProvider")
    void convert2String_primitiveWrappers(Object input, String expected) {
        assertEquals(expected, Converter.convert2String(input));
    }

    // ---------- convert2String - String and CharSequence types ----------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("charSequenceProvider")
    void convert2String_charSequences(Object input, String expected) {
        assertEquals(expected, Converter.convert2String(input));
    }

    // ---------- convert2String - ByteBuffer handling ----------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("byteBufferProvider")
    void convert2String_byteBuffers(Object input, String expected) {
        assertEquals(expected, Converter.convert2String(input));
    }

    // ---------- convert2String - Date and time types ----------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("dateTimeProvider")
    void convert2String_dateAndTime(Object input, String expected) {
        String actual = Converter.convert2String(input);
        // For java.util.Date we cannot rely on the JVM default timezone, so we only check the
        // constant part of the string that is guaranteed by Date.toString().
        if (input instanceof Date) {
            assertTrue(actual.startsWith("Thu Jan 01"), "Date conversion should start with expected epoch representation");
        } else {
            assertEquals(expected, actual);
        }
    }

    // ---------- convert2String - UUID and other simple objects ----------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("simpleObjectProvider")
    void convert2String_simpleObjects(Object input, String expected) {
        assertEquals(expected, Converter.convert2String(input));
    }

    // ---------- convert2String - Collections and arrays ----------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("collectionArrayProvider")
    void convert2String_collectionsAndArrays(Object input, String expected) {
        assertEquals(expected, Converter.convert2String(input));
    }

    // ---------- convert2String - Custom object with overridden toString ----------
    @Test
    void convert2String_customObjectWithOverriddenToString() {
        // defined as inner class below
        Object foo = new Foo();
        assertEquals("Foo", Converter.convert2String(foo));
    }

    // ---------- convert2String - Empty and whitespace strings ----------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("emptyWhitespaceProvider")
    void convert2String_emptyAndWhitespaceStrings(String input, String expected) {
        assertEquals(expected, Converter.convert2String(input));
    }

    // ---------- convert2String - unsupported source type throws IllegalArgumentException ----------
    @Test
    void convert2String_unsupportedSourceThrows() {
        Path path = Paths.get("/tmp/file.txt");
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2String(path));
    }

    // ---------- convert2String - primitive array of unsupported component type throws IllegalArgumentException ----------
    @Test
    void convert2String_unsupportedPrimitiveArrayThrows() {
        boolean[] boolArray = new boolean[] { true, false };
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2String(boolArray));
    }

    // ---------- convert2String - custom object with toString that throws RuntimeException ----------
    @Test
    void convert2String_toStringThrowsRuntimeException() {
        // defined as inner class below
        Object throwing = new ThrowingToString();
        assertThrows(RuntimeException.class, () -> Converter.convert2String(throwing));
    }

    // ---------- convert2String - conversion function throws unchecked exception ----------
    @Test
    void convert2String_customConversionThrowsRuntimeException() {
        // Register a broken conversion for the test class
        Converter.addConversion(BrokenConversion.class, String.class, (from, conv) -> {
            throw new RuntimeException("broken conversion");
        });
        assertThrows(RuntimeException.class, () -> Converter.convert2String(new BrokenConversion()));
    }
}
