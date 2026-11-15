package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class Converter_convertToBoolean_Test {

    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// ----- Boolean literals (already covered but kept for completeness) -----
        Arguments.of(Boolean.TRUE, true), Arguments.of(Boolean.FALSE, false), // ----- AtomicBoolean -----
        Arguments.of(new AtomicBoolean(true), true), Arguments.of(new AtomicBoolean(false), false), // ----- String variations (case, whitespace, synonyms) -----
        Arguments.of("true", true), Arguments.of("TRUE", true), Arguments.of("false", false), Arguments.of("FALSE", false), Arguments.of("yes", true), Arguments.of("YES", true), Arguments.of("no", false), Arguments.of("NO", false), Arguments.of("1", true), Arguments.of("0", false), Arguments.of("on", true), Arguments.of("ON", true), Arguments.of("off", false), Arguments.of("OFF", false), // leading/trailing whitespace
        Arguments.of("  true  ", true), // mixed whitespace
        Arguments.of("\tfalse\n", false), // single‑character true
        Arguments.of("t", true), Arguments.of("T", true), // single‑character false
        Arguments.of("f", false), Arguments.of("F", false), // ----- Numeric types -----
        // int zero
        Arguments.of(0, false), // int non‑zero
        Arguments.of(42, true), // long zero
        Arguments.of(0L, false), // long non‑zero (negative)
        Arguments.of(-5L, true), // double zero
        Arguments.of(0.0, false), // double non‑zero (negative)
        Arguments.of(-0.1, true), Arguments.of(Byte.valueOf((byte) 0), false), Arguments.of(Byte.valueOf((byte) 2), true), // ----- BigInteger -----
        Arguments.of(BigInteger.ZERO, false), Arguments.of(new BigInteger("12345678901234567890"), true), // ----- Map with "value" key (already covered, but again for completeness) -----
        Arguments.of(Map.of("value", "true"), true));
    }

    private static Stream<Arguments> invalidConversions() {
        return Stream.of(// ----- Arbitrary object with no conversion defined -----
        Arguments.of(new Object()), // ----- Unrecognised String values -----
        Arguments.of("notABoolean"), Arguments.of(""), Arguments.of("maybe"), // whitespace only
        Arguments.of("   "), // literal "null"
        Arguments.of("null"), // ----- Unsupported types -----
        Arguments.of(new Date()), Arguments.of(Calendar.getInstance()), Arguments.of(UUID.randomUUID()), // primitive array
        Arguments.of(new int[] { 1, 2, 3 }), // empty List (any Collection)
        Arguments.of(new ArrayList<>()), // empty Map
        Arguments.of(new HashMap<>()), // AtomicInteger not supported for Boolean
        Arguments.of(new AtomicInteger(7)), // Character not recognised
        Arguments.of('z'), Arguments.of('?'));
    }

    @Test
    void convertNullReturnsNull() {
        // given
        Object input = null;
        // when
        Boolean result = Converter.convertToBoolean(input);
        // then
        assertNull(result, "Converting null should return null for Boolean target type");
    }

    @Test
    void convertBooleanIsIdentity() {
        // given
        Boolean trueValue = Boolean.TRUE;
        Boolean falseValue = Boolean.FALSE;
        // when
        Boolean resultTrue = Converter.convertToBoolean(trueValue);
        Boolean resultFalse = Converter.convertToBoolean(falseValue);
        // then
        assertEquals(trueValue, resultTrue, "Boolean TRUE should be returned unchanged");
        assertEquals(falseValue, resultFalse, "Boolean FALSE should be returned unchanged");
    }

    @Test
    void convertStringValues() {
        // given
        String trueStr = "true";
        String falseStr = "false";
        String yesStr = "yes";
        String noStr = "no";
        String oneStr = "1";
        String zeroStr = "0";
        // when
        Boolean trueResult = Converter.convertToBoolean(trueStr);
        Boolean falseResult = Converter.convertToBoolean(falseStr);
        Boolean yesResult = Converter.convertToBoolean(yesStr);
        Boolean noResult = Converter.convertToBoolean(noStr);
        Boolean oneResult = Converter.convertToBoolean(oneStr);
        Boolean zeroResult = Converter.convertToBoolean(zeroStr);
        // then
        assertTrue(trueResult, "String \"true\" should convert to Boolean.TRUE");
        assertFalse(falseResult, "String \"false\" should convert to Boolean.FALSE");
        assertTrue(yesResult, "String \"yes\" should convert to Boolean.TRUE");
        assertFalse(noResult, "String \"no\" should convert to Boolean.FALSE");
        assertTrue(oneResult, "String \"1\" should convert to Boolean.TRUE");
        assertFalse(zeroResult, "String \"0\" should convert to Boolean.FALSE");
    }

    @Test
    void convertNumericValues() {
        // given
        Integer one = 1;
        Integer zero = 0;
        Long nonZeroLong = 123L;
        Double zeroDouble = 0.0;
        Double nonZeroDouble = -45.6;
        // when
        Boolean resultOne = Converter.convertToBoolean(one);
        Boolean resultZero = Converter.convertToBoolean(zero);
        Boolean resultLong = Converter.convertToBoolean(nonZeroLong);
        Boolean resultZeroDouble = Converter.convertToBoolean(zeroDouble);
        Boolean resultNonZeroDouble = Converter.convertToBoolean(nonZeroDouble);
        // then
        assertTrue(resultOne, "Integer 1 should convert to Boolean.TRUE");
        assertFalse(resultZero, "Integer 0 should convert to Boolean.FALSE");
        assertTrue(resultLong, "Non‑zero Long should convert to Boolean.TRUE");
        assertFalse(resultZeroDouble, "Double 0.0 should convert to Boolean.FALSE");
        assertTrue(resultNonZeroDouble, "Non‑zero Double should convert to Boolean.TRUE");
    }

    @Test
    void convertMapWithValueKey() {
        // given
        var map = java.util.Map.of("value", "true");
        // when
        Boolean result = Converter.convertToBoolean(map);
        // then
        assertTrue(result, "Map with key \"value\" and value \"true\" should convert to Boolean.TRUE");
    }

    @ParameterizedTest(name = "{index}: convertToBoolean({0}) => {1}")
    @MethodSource("successfulConversions")
    void convertValidInputs(Object input, Boolean expected) {
        assertEquals(expected, Converter.convertToBoolean(input), () -> "Expected conversion of " + input + " to be " + expected);
    }

    @ParameterizedTest(name = "{index}: convertToBoolean({0}) throws IllegalArgumentException")
    @MethodSource("invalidConversions")
    void convertInvalidInputs(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBoolean(input), () -> "Expected IllegalArgumentException for input: " + input);
    }
}
