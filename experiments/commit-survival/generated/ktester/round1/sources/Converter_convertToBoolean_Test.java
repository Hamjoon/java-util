package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.stream.Stream;
import java.util.concurrent.atomic.*;

class Converter_convertToBoolean_Test {

    static Stream<Arguments> stringTrueProvider() {
        return Stream.of(Arguments.of("true"), Arguments.of("TRUE"), Arguments.of("yes"), Arguments.of("1"));
    }

    static Stream<Arguments> stringFalseProvider() {
        return Stream.of(Arguments.of("false"), Arguments.of("FALSE"), Arguments.of("no"), Arguments.of("0"));
    }

    static Stream<Arguments> stringIllegalProvider() {
        return Stream.of(Arguments.of("maybe"), Arguments.of(""));
    }

    static Stream<Arguments> numericIntegerProvider() {
        return Stream.of(Arguments.of(0, Boolean.FALSE), Arguments.of(1, Boolean.TRUE), Arguments.of(-5, Boolean.TRUE), Arguments.of(1234567890123456789L, Boolean.TRUE));
    }

    static Stream<Arguments> numericFloatingProvider() {
        return Stream.of(Arguments.of(0.0, Boolean.FALSE), Arguments.of(-0.0, Boolean.FALSE), Arguments.of(0.0001, Boolean.TRUE), Arguments.of(-3.14, Boolean.TRUE));
    }

    static Stream<Arguments> atomicProvider() {
        return Stream.of(Arguments.of(AtomicBoolean.TRUE, Boolean.TRUE), Arguments.of(AtomicBoolean.FALSE, Boolean.FALSE), Arguments.of(new AtomicInteger(0), Boolean.FALSE), Arguments.of(new AtomicLong(42L), Boolean.TRUE));
    }

    static Stream<Arguments> dateProvider() {
        return Stream.of(Arguments.of(new Date(), Boolean.TRUE), Arguments.of(null, null));
    }

    static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(new java.util.ArrayList<>()));
    }

    static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(0), Boolean.FALSE), Arguments.of(Integer.valueOf(42), Boolean.TRUE), Arguments.of(Long.valueOf(0L), Boolean.FALSE), Arguments.of(Long.valueOf(-123L), Boolean.TRUE), Arguments.of(Short.valueOf((short) 0), Boolean.FALSE), Arguments.of(Short.valueOf((short) 7), Boolean.TRUE), Arguments.of(Byte.valueOf((byte) 0), Boolean.FALSE), Arguments.of(Byte.valueOf((byte) 5), Boolean.TRUE), Arguments.of(Float.valueOf(0.0f), Boolean.FALSE), Arguments.of(Float.valueOf(-0.0f), Boolean.FALSE), Arguments.of(Float.valueOf(2.5f), Boolean.TRUE), Arguments.of(Double.valueOf(0.0d), Boolean.FALSE), Arguments.of(Double.valueOf(-0.0d), Boolean.FALSE), Arguments.of(Double.valueOf(-7.89d), Boolean.TRUE));
    }

    static Stream<Arguments> bigNumberProvider() {
        return Stream.of(Arguments.of(new BigDecimal("0"), Boolean.FALSE), Arguments.of(new BigDecimal("0.000"), Boolean.FALSE), Arguments.of(new BigDecimal("123.45"), Boolean.TRUE), Arguments.of(new BigDecimal("-0.01"), Boolean.TRUE), Arguments.of(new BigInteger("0"), Boolean.FALSE), Arguments.of(new BigInteger("98765432109876543210"), Boolean.TRUE), Arguments.of(new BigInteger("-42"), Boolean.TRUE));
    }

    static Stream<Arguments> characterProvider() {
        return Stream.of(Arguments.of('t', Boolean.TRUE), Arguments.of('T', Boolean.TRUE), Arguments.of('y', Boolean.TRUE), Arguments.of('Y', Boolean.TRUE), Arguments.of('1', Boolean.TRUE), Arguments.of('0', Boolean.FALSE), Arguments.of('f', Boolean.FALSE), Arguments.of('F', Boolean.FALSE), Arguments.of('n', Boolean.FALSE), Arguments.of('N', Boolean.FALSE));
    }

    static Stream<Arguments> stringWhitespaceProvider() {
        return Stream.of(Arguments.of(" true ", Boolean.TRUE), Arguments.of("\tFALSE\n", Boolean.FALSE), Arguments.of("+1", Boolean.TRUE), Arguments.of("-0", Boolean.FALSE), Arguments.of("-5", Boolean.TRUE));
    }

    static Stream<Arguments> stringSynonymProvider() {
        return Stream.of(Arguments.of("on", Boolean.TRUE), Arguments.of("OFF", Boolean.FALSE), Arguments.of("y", Boolean.TRUE), Arguments.of("N", Boolean.FALSE));
    }

    @Test
    void convertToBoolean_givenNull_returnsNull() {
        // given
        Object input = null;
        // when
        Boolean result = Converter.convertToBoolean(input);
        // then
        assertNull(result, "Converting null should return null");
    }

    @Test
    void convertToBoolean_givenBooleanTrue_returnsTrue() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        Boolean result = Converter.convertToBoolean(input);
        // then
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void convertToBoolean_givenBooleanFalse_returnsFalse() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        Boolean result = Converter.convertToBoolean(input);
        // then
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void convertToBoolean_givenStringTrue_returnsTrue() {
        // given
        String input = "true";
        // when
        Boolean result = Converter.convertToBoolean(input);
        // then
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void convertToBoolean_givenStringFalse_returnsFalse() {
        // given
        String input = "false";
        // when
        Boolean result = Converter.convertToBoolean(input);
        // then
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void convertToBoolean_givenIntegerOne_returnsTrue() {
        // given
        Integer input = 1;
        // when
        Boolean result = Converter.convertToBoolean(input);
        // then
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void convertToBoolean_givenIntegerZero_returnsFalse() {
        // given
        Integer input = 0;
        // when
        Boolean result = Converter.convertToBoolean(input);
        // then
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void convertToBoolean_givenUnsupportedObject_returnsNull() {
        // given
        // No conversion rule for plain Object to Boolean
        Object input = new Object();
        // when
        Boolean result = Converter.convertToBoolean(input);
        // then
        assertNull(result, "Unsupported conversion should result in null");
    }

    @ParameterizedTest(name = "[{index}] String ''{0}'' → true")
    @MethodSource("stringTrueProvider")
    void convertToBoolean_stringTrueValues(String input) {
        assertEquals(Boolean.TRUE, Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] String ''{0}'' → false")
    @MethodSource("stringFalseProvider")
    void convertToBoolean_stringFalseValues(String input) {
        assertEquals(Boolean.FALSE, Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] String ''{0}'' → IllegalArgumentException")
    @MethodSource("stringIllegalProvider")
    void convertToBoolean_stringIllegalValues(String input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] Integer {0} → {1}")
    @MethodSource("numericIntegerProvider")
    void convertToBoolean_numericIntegers(Number input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] Float/Double {0} → {1}")
    @MethodSource("numericFloatingProvider")
    void convertToBoolean_floatingPoint(Number input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("atomicProvider")
    void convertToBoolean_atomicTypes(Object input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] Date instance → {0}")
    @MethodSource("dateProvider")
    void convertToBoolean_dateTypes(Object input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] Unsupported {0} → IllegalArgumentException")
    @MethodSource("unsupportedProvider")
    void convertToBoolean_unsupportedTypes(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] Wrapper {0} → {1}")
    @MethodSource("numericWrapperProvider")
    void convertToBoolean_numericWrappers(Object input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("bigNumberProvider")
    void convertToBoolean_bigNumbers(Object input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input));
    }

    @ParameterizedTest(name = "[{index}] Char ''{0}'' → {1}")
    @MethodSource("characterProvider")
    void convertToBoolean_characterInput(Character input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input));
    }

    @Test
    void convertToBoolean_characterUnsupported_throws() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBoolean('x'));
    }

    @ParameterizedTest(name = "[{index}] String ''{0}'' → {1}")
    @MethodSource("stringWhitespaceProvider")
    void convertToBoolean_whitespaceAndSign(String input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input));
    }

    @Test
    void convertToBoolean_whitespaceOnly_throws() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBoolean("   "));
    }

    @ParameterizedTest(name = "[{index}] Synonym ''{0}'' → {1}")
    @MethodSource("stringSynonymProvider")
    void convertToBoolean_synonyms(String input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input));
    }

    @Test
    void convertToBoolean_enumUnsupported_throws() {
        ???;
    }
}
