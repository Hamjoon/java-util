package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_isConversionSupportedFor_2_Test {

    private static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// Both null
        Arguments.of(null, null, false), // Source null, target concrete
        Arguments.of(null, String.class, false), // Source concrete, target null
        Arguments.of(Integer.class, null, false));
    }

    private static Stream<Arguments> identityConversionProvider() {
        return Stream.of(Arguments.of(String.class, String.class, true), Arguments.of(int.class, int.class, true), Arguments.of(AtomicBoolean.class, AtomicBoolean.class, true));
    }

    private static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of(int.class, Integer.class, true), Arguments.of(Integer.class, int.class, true), Arguments.of(boolean.class, Boolean.class, true), Arguments.of(char.class, Character.class, true));
    }

    private static Stream<Arguments> numericStringProvider() {
        return Stream.of(Arguments.of(String.class, Integer.class, true), Arguments.of(String.class, Double.class, true), Arguments.of(Integer.class, String.class, true), Arguments.of(BigDecimal.class, String.class, true));
    }

    private static Stream<Arguments> booleanStringProvider() {
        return Stream.of(Arguments.of(String.class, Boolean.class, true), Arguments.of(Boolean.class, String.class, true));
    }

    private static Stream<Arguments> dateTimestampProvider() {
        return Stream.of(Arguments.of(Date.class, Timestamp.class, true), Arguments.of(Timestamp.class, Date.class, true), Arguments.of(LocalDate.class, Date.class, false), Arguments.of(LocalDateTime.class, ZonedDateTime.class, true));
    }

    private static Stream<Arguments> atomicNumberProvider() {
        return Stream.of(Arguments.of(AtomicInteger.class, Integer.class, true), Arguments.of(Long.class, AtomicLong.class, true));
    }

    private static Stream<Arguments> assignableConversionProvider() {
        return Stream.of(// Timestamp extends Date – should be supported
        Arguments.of(Timestamp.class, Date.class, true), // Concrete List implementation -> interface
        Arguments.of(ArrayList.class, List.class, true), // Interface -> concrete (no direct conversion)
        Arguments.of(List.class, ArrayList.class, false));
    }

    private static Stream<Arguments> unsupportedConversionProvider() {
        return Stream.of(Arguments.of(String.class, Thread.class, false), Arguments.of(Object.class, Integer.class, false), Arguments.of(Map.class, Set.class, false), Arguments.of(ZonedDateTime.class, LocalDate.class, false));
    }

    private static Stream<Arguments> collectionConversionProvider() {
        return Stream.of(Arguments.of(ArrayList.class, Collection.class, true), Arguments.of(HashSet.class, Set.class, true));
    }

    @Test
    void shouldReturnTrueForSupportedPrimitiveToWrapperConversion() {
        // given
        Class<?> source = int.class;
        Class<?> target = Integer.class;
        // when
        boolean result = Converter.isConversionSupportedFor(source, target);
        // then
        assertTrue(result, "Conversion from int to Integer should be supported");
    }

    @Test
    void shouldReturnTrueForSupportedStringToNumberConversion() {
        // given
        Class<?> source = String.class;
        Class<?> target = Long.class;
        // when
        boolean result = Converter.isConversionSupportedFor(source, target);
        // then
        assertTrue(result, "Conversion from String to Long should be supported");
    }

    @Test
    void shouldReturnTrueForSupportedDateToInstantConversion() {
        // given
        Class<?> source = Date.class;
        Class<?> target = Instant.class;
        // when
        boolean result = Converter.isConversionSupportedFor(source, target);
        // then
        assertTrue(result, "Conversion from Date to Instant should be supported");
    }

    @Test
    void shouldReturnFalseForUnsupportedConversion() {
        // given
        // unlikely to be convertible
        Class<?> source = Thread.class;
        Class<?> target = UUID.class;
        // when
        boolean result = Converter.isConversionSupportedFor(source, target);
        // then
        assertFalse(result, "Conversion from Thread to UUID should not be supported");
    }

    @Test
    void shouldReturnTrueForSupportedCollectionToListConversion() {
        // given
        Class<?> source = java.util.Set.class;
        Class<?> target = java.util.List.class;
        // when
        boolean result = Converter.isConversionSupportedFor(source, target);
        // then
        assertTrue(result, "Conversion from Set to List should be supported");
    }

    @Test
    void shouldReturnTrueForSupportedAtomicBooleanToBooleanConversion() {
        // given
        Class<?> source = AtomicBoolean.class;
        Class<?> target = Boolean.class;
        // when
        boolean result = Converter.isConversionSupportedFor(source, target);
        // then
        assertTrue(result, "Conversion from AtomicBoolean to Boolean should be supported");
    }

    @Test
    void shouldReturnFalseWhenSourceIsNull() {
        // given
        Class<?> source = null;
        Class<?> target = String.class;
        // when / then
        assertThrows(NullPointerException.class, () -> Converter.isConversionSupportedFor(source, target), "Passing a null source class should throw NPE");
    }

    @Test
    void shouldReturnFalseWhenTargetIsNull() {
        // given
        Class<?> source = String.class;
        Class<?> target = null;
        // when / then
        assertThrows(NullPointerException.class, () -> Converter.isConversionSupportedFor(source, target), "Passing a null target class should throw NPE");
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("nullHandlingProvider")
    void testNullHandling(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Null handling: source=%s, target=%s", source, target));
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("identityConversionProvider")
    void testIdentityConversions(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Identity conversion: %s -> %s", source, target));
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("primitiveWrapperProvider")
    void testPrimitiveWrapperConversions(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Primitive ↔ wrapper conversion: %s -> %s", source, target));
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("numericStringProvider")
    void testNumericStringConversions(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Numeric/String conversion: %s -> %s", source, target));
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("booleanStringProvider")
    void testBooleanStringConversions(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Boolean/String conversion: %s -> %s", source, target));
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("dateTimestampProvider")
    void testDateTimestampConversions(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Date/Timestamp conversion: %s -> %s", source, target));
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("atomicNumberProvider")
    void testAtomicNumberConversions(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Atomic number conversion: %s -> %s", source, target));
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("assignableConversionProvider")
    void testAssignableConversions(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Assignable conversion: %s -> %s", source, target));
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("unsupportedConversionProvider")
    void testUnsupportedConversions(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Unsupported conversion: %s -> %s", source, target));
    }

    @ParameterizedTest(name = "[{index}] source={0}, target={1} => expected={2}")
    @MethodSource("collectionConversionProvider")
    void testSupportedCollectionConversions(Class<?> source, Class<?> target, boolean expected) {
        boolean result = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, result, () -> String.format("Collection conversion: %s -> %s", source, target));
    }
}
