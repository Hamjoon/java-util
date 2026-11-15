package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

class Converter_convertToBoolean_Test {

    static Stream<Arguments> directBooleanValues() {
        return Stream.of(Arguments.of(Boolean.TRUE, true), Arguments.of(Boolean.FALSE, false));
    }

    static Stream<Arguments> stringRepresentations() {
        return Stream.of(Arguments.of("true", true), Arguments.of("TRUE", true), Arguments.of("false", false), Arguments.of("FALSE", false), Arguments.of("yes", true), Arguments.of("no", false), Arguments.of("1", true), Arguments.of("0", false), // unrecognised string – specification says default to false
        Arguments.of("random", false));
    }

    static Stream<Arguments> numericValues() {
        return Stream.of(Arguments.of(1, true), Arguments.of(0, false), Arguments.of(-1, true), Arguments.of(2L, true), Arguments.of(0L, false), Arguments.of(0.0, false), Arguments.of(3.14, true));
    }

    static Stream<Arguments> atomicWrapperValues() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), true), Arguments.of(new AtomicBoolean(false), false), Arguments.of(new AtomicInteger(0), false), Arguments.of(new AtomicInteger(5), true), Arguments.of(new AtomicLong(0L), false), Arguments.of(new AtomicLong(10L), true));
    }

    static Stream<Arguments> unsupportedTypesExpectException() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(new Date()), Arguments.of(DayOfWeek.MONDAY), Arguments.of(UUID.randomUUID()), Arguments.of(LocalDate.now()), Arguments.of(LocalDateTime.now()), Arguments.of(Instant.now()), Arguments.of(ZonedDateTime.now()), Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Float.NaN), Arguments.of(Float.NEGATIVE_INFINITY), Arguments.of(new AtomicReference<>(Boolean.TRUE)), Arguments.of(Optional.of(true)));
    }

    static Stream<Arguments> collectionBooleanValues() {
        return Stream.of(Arguments.of(Collections.emptyList(), false), Arguments.of(Arrays.asList(1, 2, 3), true), Arguments.of(new int[] {}, false), // non‑empty primitive array is true
        Arguments.of(new int[] { 0 }, true), Arguments.of(new String[] {}, false), Arguments.of(new String[] { "a", "b" }, true), Arguments.of(Collections.emptySet(), false), Arguments.of(new HashSet<>(Arrays.asList(1, 2)), true), Arguments.of(Collections.emptyMap(), false), Arguments.of(Collections.singletonMap("k", "v"), true));
    }

    static Stream<Arguments> characterValues() {
        return Stream.of(Arguments.of('t', true), Arguments.of('T', true), Arguments.of('f', false), Arguments.of('F', false), Arguments.of('1', true), Arguments.of('0', false), Arguments.of('y', true), Arguments.of('N', false), Arguments.of('x', false));
    }

    static Stream<Arguments> numericWrapperValues() {
        return Stream.of(Arguments.of(Byte.valueOf((byte) 0), false), Arguments.of(Byte.valueOf((byte) 5), true), Arguments.of(Short.valueOf((short) 0), false), Arguments.of(Short.valueOf((short) -3), true), Arguments.of(Integer.valueOf(0), false), Arguments.of(Integer.valueOf(42), true), Arguments.of(Long.valueOf(0L), false), Arguments.of(Long.valueOf(-100L), true), Arguments.of(Float.valueOf(0.0f), false), Arguments.of(Float.valueOf(2.5f), true), Arguments.of(Double.valueOf(0.0d), false), Arguments.of(Double.valueOf(-0.001d), true));
    }

    static Stream<Arguments> bigNumberValues() {
        return Stream.of(Arguments.of(BigInteger.ZERO, false), Arguments.of(BigInteger.valueOf(123456789L), true), Arguments.of(BigDecimal.ZERO, false), Arguments.of(new BigDecimal("0.000"), false), Arguments.of(new BigDecimal("3.1415"), true));
    }

    static Stream<Arguments> stringEdgeCases() {
        return Stream.of(Arguments.of("", false), Arguments.of("   ", false), Arguments.of(" true ", true), Arguments.of("FALSE ", false), Arguments.of("null", false), Arguments.of("+1", true), Arguments.of("-0", false), Arguments.of("on", true), Arguments.of("off", false));
    }

    static Stream<Arguments> bufferValues() {
        // ByteBuffer needs flip() after putting the byte
        ByteBuffer trueByte = ByteBuffer.allocate(1).put((byte) 1);
        trueByte.flip();
        ByteBuffer falseByte = ByteBuffer.allocate(1).put((byte) 0);
        falseByte.flip();
        return Stream.of(Arguments.of(CharBuffer.wrap("true"), true), Arguments.of(CharBuffer.wrap("FALSE"), false), Arguments.of(trueByte, true), Arguments.of(falseByte, false));
    }

    static Stream<Arguments> charSequenceValues() {
        return Stream.of(Arguments.of(new StringBuilder("yes"), true), Arguments.of(new StringBuilder("no"), false));
    }

    static Stream<Arguments> customToStringObjects() {
        Object trueObj = new Object() {

            @Override
            public String toString() {
                return "true";
            }
        };
        Object falseObj = new Object() {

            @Override
            public String toString() {
                return "random";
            }
        };
        return Stream.of(Arguments.of(trueObj, true), Arguments.of(falseObj, false));
    }

    static Stream<Arguments> specialNumericValues() {
        return Stream.of(Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Float.NaN), Arguments.of(Float.NEGATIVE_INFINITY));
    }

    @Test
    void shouldReturnTrueWhenInputIsBooleanTrue() {
        Boolean source = Boolean.TRUE;
        Boolean result = Converter.convertToBoolean(source);
        assertTrue(result, "Converter should return true for Boolean.TRUE");
    }

    @Test
    void shouldReturnFalseWhenInputIsBooleanFalse() {
        Boolean source = Boolean.FALSE;
        Boolean result = Converter.convertToBoolean(source);
        assertFalse(result, "Converter should return false for Boolean.FALSE");
    }

    @Test
    void shouldConvertStringTrueToBooleanTrue() {
        String source = "true";
        Boolean result = Converter.convertToBoolean(source);
        assertTrue(result, "String \"true\" should be converted to Boolean.TRUE");
    }

    @Test
    void shouldConvertStringFalseToBooleanFalse() {
        String source = "false";
        Boolean result = Converter.convertToBoolean(source);
        assertFalse(result, "String \"false\" should be converted to Boolean.FALSE");
    }

    @Test
    void shouldConvertNumericOneToBooleanTrue() {
        Integer source = 1;
        Boolean result = Converter.convertToBoolean(source);
        assertTrue(result, "Numeric 1 should be converted to Boolean.TRUE");
    }

    @Test
    void shouldConvertNumericZeroToBooleanFalse() {
        Integer source = 0;
        Boolean result = Converter.convertToBoolean(source);
        assertFalse(result, "Numeric 0 should be converted to Boolean.FALSE");
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        Boolean result = Converter.convertToBoolean(null);
        assertNull(result, "Converter should return null when the source is null");
    }

    @Test
    void shouldReturnNullForUnsupportedType() {
        // not a known convertible type
        Object source = new Object();
        Boolean result = Converter.convertToBoolean(source);
        assertNull(result, "Converter should return null for an unsupported source type");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("directBooleanValues")
    void shouldConvertDirectBooleanValues(Object source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("stringRepresentations")
    void shouldConvertStringRepresentations(String source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("numericValues")
    void shouldConvertNumericValues(Object source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("atomicWrapperValues")
    void shouldConvertAtomicWrapperTypes(Object source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → IllegalArgumentException")
    @MethodSource("unsupportedTypesExpectException")
    void shouldThrowForUnsupportedTypes(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBoolean(source), "Conversion of " + source.getClass().getSimpleName() + " should throw IllegalArgumentException");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("collectionBooleanValues")
    void shouldConvertCollectionsAndMaps(Object source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] '{0}' → {1}")
    @MethodSource("characterValues")
    void shouldConvertCharacterValues(Character source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("numericWrapperValues")
    void shouldConvertNumericWrapperTypes(Object source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("bigNumberValues")
    void shouldConvertBigNumberTypes(Object source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("stringEdgeCases")
    void shouldHandleStringEdgeCases(String source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("bufferValues")
    void shouldConvertBufferTypes(Object source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("charSequenceValues")
    void shouldConvertCharSequenceImplementations(CharSequence source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] customObject → {1}")
    @MethodSource("customToStringObjects")
    void shouldConvertCustomObjectToString(Object source, Boolean expected) {
        Boolean result = Converter.convertToBoolean(source);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0} → IllegalArgumentException")
    @MethodSource("specialNumericValues")
    void shouldThrowForSpecialNumericValues(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBoolean(source), "Special numeric value " + source + " must cause IllegalArgumentException");
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldThrowForAtomicReference() {
        AtomicReference<Boolean> ref = new AtomicReference<>(true);
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBoolean(ref), "AtomicReference is not a supported source for Boolean conversion");
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldThrowForOptional() {
        Optional<Boolean> opt = Optional.of(true);
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBoolean(opt), "Optional is not a supported source for Boolean conversion");
    }
}
