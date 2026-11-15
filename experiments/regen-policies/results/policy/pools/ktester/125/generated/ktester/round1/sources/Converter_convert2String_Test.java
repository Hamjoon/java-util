package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

class Converter_convert2String_Test {

    /**
     * Simple POJO whose {@code toString()} returns a predefined value.
     */
    private static final class TestObject {

        private final String toStringResult;

        TestObject(String toStringResult) {
            this.toStringResult = toStringResult;
        }

        @Override
        public String toString() {
            return toStringResult;
        }
    }

    /**
     * Object that deliberately throws an exception from {@code toString()}.
     * Used to verify that such exceptions propagate out of {@code convert2String}.
     */
    private static final class BrokenToString {

        @Override
        public String toString() {
            throw new RuntimeException("Broken toString");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Force UTC so that Date.toString() yields a deterministic result
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Provides a stream of arguments: the source object and the expected
     * string result from {@link Converter#convert2String(Object)}.
     */
    private static Stream<Arguments> provideSuccessfulConversions() {
        // Custom object whose toString() returns a specific value
        TestObject customObj = new TestObject("TestObject[ID=99]");
        // Date instance – we rely on UTC timezone set in @BeforeEach
        // 2021‑07‑01 00:00:00 UTC
        Date utcDate = new Date(1625097600000L);
        String utcDateString = utcDate.toString();
        return Stream.of(// Primitive wrapper types
        Arguments.of(Integer.valueOf(12345), "12345"), Arguments.of(Long.valueOf(9223372036854775807L), "9223372036854775807"), Arguments.of(Boolean.TRUE, "true"), Arguments.of(Double.NaN, "NaN"), Arguments.of(Float.POSITIVE_INFINITY, "Infinity"), Arguments.of(Short.valueOf((short) -123), "-123"), Arguments.of(Byte.valueOf((byte) 0), "0"), Arguments.of(Character.valueOf('Z'), "Z"), // CharBuffer and StringBuilder (both rely on toString())
        Arguments.of(CharBuffer.wrap("charbuf"), "charbuf"), Arguments.of(new StringBuilder("builder"), "builder"), // BigDecimal / BigInteger
        Arguments.of(new BigDecimal("12345.6789"), "12345.6789"), Arguments.of(new BigInteger("9876543210"), "9876543210"), // UUID
        Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "123e4567-e89b-12d3-a456-426614174000"), // Java Time API
        Arguments.of(LocalDate.parse("2021-12-31"), "2021-12-31"), Arguments.of(LocalDateTime.parse("2021-12-31T23:59:59"), "2021-12-31T23:59:59"), Arguments.of(ZonedDateTime.parse("2021-12-31T23:59:59Z"), "2021-12-31T23:59:59Z"), Arguments.of(Instant.parse("2021-12-31T23:59:59Z"), "2021-12-31T23:59:59Z"), // Atomic types
        Arguments.of(new AtomicInteger(42), "42"), // Empty collection
        Arguments.of(Collections.emptyList(), "[]"), // java.util.Date (fallback to toString())
        Arguments.of(utcDate, utcDateString), // Custom object with meaningful toString()
        Arguments.of(customObj, "TestObject[ID=99]"));
    }

    /**
     * Provides inputs that are expected to cause the converter to throw
     * an exception because no conversion to {@code String} is registered.
     */
    private static Stream<Arguments> provideExceptionConversions() {
        // InputStream – unsupported conversion
        ByteArrayInputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        // Object whose toString() throws a RuntimeException
        BrokenToString broken = new BrokenToString();
        // AtomicReference holding a null reference – may cause NPE inside the converter
        AtomicReference<Object> nullRef = new AtomicReference<>(null);
        return Stream.of(Arguments.of(emptyStream, IllegalArgumentException.class), Arguments.of(broken, RuntimeException.class), Arguments.of(nullRef, NullPointerException.class));
    }

    @Test
    void convert2String_returnsEmptyString_whenInputIsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        String result = Converter.convert2String(input);
        // THEN
        assertEquals("", result, "Null input should be converted to an empty string");
    }

    @Test
    void convert2String_returnsSameString_whenInputIsString() {
        // GIVEN
        String input = "Hello, world!";
        // WHEN
        String result = Converter.convert2String(input);
        // THEN
        assertEquals(input, result, "A String input should be returned unchanged");
    }

    @Test
    void convert2String_convertsIntegerToString() {
        // GIVEN
        Integer input = 12345;
        // WHEN
        String result = Converter.convert2String(input);
        // THEN
        assertEquals("12345", result, "Integer should be converted to its decimal string representation");
    }

    @Test
    void convert2String_convertsLongToString() {
        // GIVEN
        Long input = 9876543210L;
        // WHEN
        String result = Converter.convert2String(input);
        // THEN
        assertEquals("9876543210", result, "Long should be converted to its decimal string representation");
    }

    @Test
    void convert2String_convertsBooleanToString() {
        // GIVEN
        Boolean input = Boolean.TRUE;
        // WHEN
        String result = Converter.convert2String(input);
        // THEN
        assertEquals("true", result, "Boolean should be converted to its textual representation");
    }

    @Test
    void convert2String_matchesConvertToStringResult_forVariousInputs() {
        // GIVEN
        Object[] inputs = { "sample text", 42, 3.14, true, java.util.UUID.randomUUID() };
        // WHEN / THEN
        for (Object input : inputs) {
            String viaConvert2String = Converter.convert2String(input);
            String viaConvertToString = Converter.convertToString(input);
            assertEquals(viaConvertToString, viaConvert2String, "convert2String should behave identically to convertToString for input: " + input);
        }
    }

    @ParameterizedTest(name = "convert2String({0}) → \"{1}\"")
    @MethodSource("provideSuccessfulConversions")
    void convert2String_successfulConversions(Object input, String expected) {
        // WHEN
        String result = Converter.convert2String(input);
        // THEN
        assertEquals(expected, result, () -> "Expected conversion of " + input.getClass().getSimpleName() + " to produce \"" + expected + "\"");
    }

    @ParameterizedTest(name = "convert2String({0}) throws {1}")
    @MethodSource("provideExceptionConversions")
    void convert2String_exceptionalCases(Object input, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> Converter.convert2String(input), () -> "Expected " + expectedException.getSimpleName() + " when converting " + (input == null ? "null" : input.getClass().getSimpleName()));
    }
}
