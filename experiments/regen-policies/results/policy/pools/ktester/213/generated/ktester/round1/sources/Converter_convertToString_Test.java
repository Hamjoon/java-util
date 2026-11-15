package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

@ExtendWith(MockitoExtension.class)
class Converter_convertToString_Test {

    /**
     * Simple custom class used to verify that a user‑defined {@code toString()}
     * implementation is honoured by the converter.
     */
    private static final class Foo {

        @Override
        public String toString() {
            return "FooBar";
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure deterministic Date.toString() output (UTC) for the tests that rely on it.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Provides successful conversion test data (source object → expected String).
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> successfulConversions() {
        return Stream.of(// String cases
        org.junit.jupiter.params.provider.Arguments.of("hello world", "hello world"), org.junit.jupiter.params.provider.Arguments.of("", ""), org.junit.jupiter.params.provider.Arguments.of(new StringBuilder("abc"), "abc"), // Primitive types
        org.junit.jupiter.params.provider.Arguments.of(42, "42"), org.junit.jupiter.params.provider.Arguments.of(3.1415, "3.1415"), org.junit.jupiter.params.provider.Arguments.of(true, "true"), org.junit.jupiter.params.provider.Arguments.of('Z', "Z"), // Wrapper types
        org.junit.jupiter.params.provider.Arguments.of(Integer.valueOf(7), "7"), org.junit.jupiter.params.provider.Arguments.of(Long.valueOf(1234567890123L), "1234567890123"), org.junit.jupiter.params.provider.Arguments.of(Double.valueOf(-0.5), "-0.5"), org.junit.jupiter.params.provider.Arguments.of(Boolean.FALSE, "false"), org.junit.jupiter.params.provider.Arguments.of(Character.valueOf('q'), "q"), // Big numbers
        org.junit.jupiter.params.provider.Arguments.of(new BigDecimal("12345.6789"), "12345.6789"), org.junit.jupiter.params.provider.Arguments.of(new BigInteger("98765432109876543210"), "98765432109876543210"), // Date/Time types
        org.junit.jupiter.params.provider.Arguments.of(new Date(0L), "Thu Jan 01 00:00:00 UTC 1970"), org.junit.jupiter.params.provider.Arguments.of(LocalDate.of(2020, 1, 15), "2020-01-15"), org.junit.jupiter.params.provider.Arguments.of(LocalDateTime.of(2020, 1, 15, 12, 30, 45), "2020-01-15T12:30:45"), org.junit.jupiter.params.provider.Arguments.of(Instant.ofEpochSecond(0L), "1970-01-01T00:00:00Z"), org.junit.jupiter.params.provider.Arguments.of(ZonedDateTime.parse("2020-01-15T12:30:45+01:00[Europe/Paris]"), "2020-01-15T12:30:45+01:00[Europe/Paris]"), org.junit.jupiter.params.provider.Arguments.of(Timestamp.valueOf("2021-06-30 14:22:33"), "2021-06-30 14:22:33.0"), // Atomic types
        org.junit.jupiter.params.provider.Arguments.of(new AtomicInteger(99), "99"), org.junit.jupiter.params.provider.Arguments.of(new AtomicLong(123456789L), "123456789"), org.junit.jupiter.params.provider.Arguments.of(new AtomicBoolean(false), "false"), // Buffer types
        org.junit.jupiter.params.provider.Arguments.of(ByteBuffer.wrap(new byte[] { 65, 66, 67 }), "ABC"), org.junit.jupiter.params.provider.Arguments.of(CharBuffer.wrap(new char[] { 'x', 'y', 'z' }), "xyz"), // Collection types
        org.junit.jupiter.params.provider.Arguments.of(List.of(1, 2, 3), "[1, 2, 3]"), // Set order is not guaranteed – we will validate using a regex in the test method.
        org.junit.jupiter.params.provider.Arguments.of(Set.of("a", "b"), "[a, b]"), org.junit.jupiter.params.provider.Arguments.of(Map.of("key", 10), "{key=10}"), // UUID and misc
        org.junit.jupiter.params.provider.Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "123e4567-e89b-12d3-a456-426614174000"), org.junit.jupiter.params.provider.Arguments.of(new AtomicReference<>("ref"), "ref"), // Custom object
        org.junit.jupiter.params.provider.Arguments.of(new Foo(), "FooBar"), // Unsupported / fallback (Thread) – falls back to Object.toString()
        org.junit.jupiter.params.provider.Arguments.of(new Thread(), "Thread[main,5,main]"));
    }

    /**
     * Provides test data for scenarios that must raise an exception.
     * The second element is the expected exception class.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> exceptionScenarios() {
        return Stream.of(// Generic Object without meaningful conversion – expecting IllegalArgumentException
        org.junit.jupiter.params.provider.Arguments.of(new Object(), IllegalArgumentException.class), // Primitive array (int[]) – expecting IllegalArgumentException as per spec
        org.junit.jupiter.params.provider.Arguments.of(new int[] { 1, 2, 3 }, IllegalArgumentException.class), // Byte array without explicit handling – expecting IllegalArgumentException
        org.junit.jupiter.params.provider.Arguments.of(new byte[0], IllegalArgumentException.class), // Null pointer scenario – the converter returns null, but we also test explicit NPE
        // (the spec mentions possible NPE; we assert that no NPE is thrown and null is returned)
        // This case is covered by the null test above, so we omit it here.
        // AtomicReference containing null – expecting NullPointerException
        org.junit.jupiter.params.provider.Arguments.of(new AtomicReference<>(null), NullPointerException.class));
    }

    @Test
    void convertNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        assertNull(result, "Converting a null source should produce a null String");
    }

    @Test
    void convertString_returnsSameInstance() {
        // GIVEN
        String source = "already a string";
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        assertSame(source, result, "Converting a String should return the original instance");
    }

    @Test
    void convertInteger_toString() {
        // GIVEN
        Integer source = 42;
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        assertEquals("42", result);
    }

    @Test
    void convertBoolean_toString() {
        // GIVEN
        Boolean sourceTrue = Boolean.TRUE;
        Boolean sourceFalse = Boolean.FALSE;
        // WHEN
        String resultTrue = Converter.convertToString(sourceTrue);
        String resultFalse = Converter.convertToString(sourceFalse);
        // THEN
        assertEquals("true", resultTrue);
        assertEquals("false", resultFalse);
    }

    @Test
    void convertLocalDate_toString() {
        // GIVEN
        LocalDate source = LocalDate.of(2020, 1, 2);
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        assertEquals("2020-01-02", result);
    }

    @Test
    void convertLocalDateTime_toString() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        assertEquals("2020-01-02T03:04:05", result);
    }

    @Test
    void convertUuid_toString() {
        // GIVEN
        UUID source = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        assertEquals(source.toString(), result);
    }

    @Test
    void convertByteBuffer_toString() {
        // GIVEN
        // "ABC"
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 65, 66, 67 });
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        assertEquals("ABC", result);
    }

    @Test
    void convertCharBuffer_toString() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("XYZ");
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        assertEquals("XYZ", result);
    }

    @Test
    void convertArrayOfPrimitives_toString() {
        // GIVEN
        int[] source = { 1, 2, 3 };
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        // The default conversion for primitive arrays is a comma‑separated list.
        assertEquals("[1, 2, 3]", result);
    }

    @ParameterizedTest(name = "{index} ⇒ source={0}, expected={1}")
    @MethodSource("successfulConversions")
    void convertToString_success(Object source, String expected) {
        // WHEN
        String result = Converter.convertToString(source);
        // THEN
        // Special handling for Set where order may vary.
        if (source instanceof Set) {
            // Remove brackets and split on comma to compare as unordered collections.
            // e.g., "a,b"
            String normalized = result.replaceAll("[\\[\\]\\s]", "");
            Set<String> actualSet = new HashSet<>(Arrays.asList(normalized.split(",")));
            Set<String> expectedSet = new HashSet<>(Arrays.asList("a", "b"));
            assertEquals(expectedSet, actualSet, "Set conversion should contain both elements irrespective of order");
        } else {
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "{index} ⇒ source={0} should throw {1}")
    @MethodSource("exceptionScenarios")
    void convertToString_exceptions(Object source, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> Converter.convertToString(source));
    }
}
