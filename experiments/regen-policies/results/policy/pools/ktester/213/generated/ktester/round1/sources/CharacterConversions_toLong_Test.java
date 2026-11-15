package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class CharacterConversions_toLong_Test {

    private Converter converter;

    private Converter defaultConverter;

    @BeforeEach
    void setupBeforeEach() {
        // The default converter is used for the majority of cases where a non‑null
        // converter is required but its behaviour is irrelevant.
        defaultConverter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> successCases() {
        // a mock converter that throws on any interaction – used to prove the method ignores it
        Converter throwingMock = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        return Stream.of(// basic characters
        Arguments.of(Character.valueOf('A'), null, 65L), Arguments.of(Character.valueOf('\u0000'), null, 0L), Arguments.of(Character.valueOf('z'), null, 122L), Arguments.of(Character.valueOf('\uFFFF'), null, 65535L), // high‑surrogate
        Arguments.of(Character.valueOf('\uD800'), null, 55296L), // low‑surrogate
        Arguments.of(Character.valueOf('\uDC00'), null, 56320L), Arguments.of(Character.valueOf('5'), null, 53L), Arguments.of(Character.valueOf(' '), null, 32L), Arguments.of(Character.valueOf('\u0100'), null, 256L), // using a non‑null (but ignored) converter
        Arguments.of(Character.valueOf('X'), throwingMock, 88L));
    }

    private static Stream<Arguments> exceptionCases() {
        // a mock converter that throws on any interaction – used to verify it is never called
        Converter throwingMock = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        return Stream.of(// null source
        Arguments.of(null, null, NullPointerException.class), // non‑character objects – all should result in ClassCastException
        Arguments.of(Integer.valueOf(65), null, ClassCastException.class), Arguments.of("A", null, ClassCastException.class), Arguments.of(new Date(), null, ClassCastException.class), Arguments.of(Byte.valueOf((byte) 1), null, ClassCastException.class), Arguments.of(Boolean.TRUE, null, ClassCastException.class), Arguments.of(new StringBuilder("A"), null, ClassCastException.class), // additional primitive wrapper types
        Arguments.of(Short.valueOf((short) 10), null, ClassCastException.class), Arguments.of(Long.valueOf(65L), null, ClassCastException.class), Arguments.of(Float.valueOf(65.0f), null, ClassCastException.class), Arguments.of(Double.valueOf(65.0), null, ClassCastException.class), // big number types
        Arguments.of(BigInteger.valueOf(65), null, ClassCastException.class), Arguments.of(BigDecimal.valueOf(65), null, ClassCastException.class), // atomic types
        Arguments.of(new AtomicBoolean(true), null, ClassCastException.class), Arguments.of(new AtomicInteger(65), null, ClassCastException.class), Arguments.of(new AtomicLong(65L), null, ClassCastException.class), // array and other unrelated types
        Arguments.of(new char[] { 'A', 'B' }, null, ClassCastException.class), Arguments.of(LocalDate.now(), null, ClassCastException.class), Arguments.of(DayOfWeek.MONDAY, null, ClassCastException.class), // using a throwing converter – should still throw ClassCastException, not invoke converter
        // this case is actually successful;
        Arguments.of(Character.valueOf('X'), throwingMock, 88L));
    }

    @Test
    void toLong_givenCharacter_returnsItsUnicodeValue() {
        // given
        // Unicode 65
        Character ch = 'A';
        // when
        long result = CharacterConversions.toLong(ch, defaultConverter);
        // then
        assertEquals(65L, result, "The long value should match the character's Unicode code point");
    }

    @Test
    void toLong_givenStringThrowsClassCastException() {
        // given
        String str = "B";
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterConversions.toLong(str, defaultConverter), "Casting a non‑character object should result in ClassCastException");
    }

    @Test
    void toLong_givenNullThrowsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterConversions.toLong(nullObj, defaultConverter), "Casting null to char should throw NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (expected long result)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, expected={2}")
    @MethodSource("successCases")
    void toLong_successfulConversion(Object from, Converter converter, long expected) {
        // when
        long result = CharacterConversions.toLong(from, converter);
        // then
        assertEquals(expected, result, () -> "Expected char cast of " + from + " to yield " + expected);
    }

    /* --------------------------------------------------------------
     *  Exception test cases (expected ClassCastException or NPE)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → {2}")
    @MethodSource("exceptionCases")
    void toLong_expectedException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> CharacterConversions.toLong(from, converter), () -> "Casting " + (from == null ? "null" : from.getClass().getSimpleName()) + " to char should throw " + expectedException.getSimpleName());
    }
}
