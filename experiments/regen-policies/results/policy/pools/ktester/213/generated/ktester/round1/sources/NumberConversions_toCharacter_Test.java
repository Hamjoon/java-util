package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class NumberConversions_toCharacter_Test {

    /**
     * Converter used for all tests – it is stateless, so a single instance is enough.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default options are sufficient for our tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to release, but clear the reference for completeness.
        converter = null;
    }

    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Integer values
        // ASCII 'A'
        Arguments.of(65, 'A'), // lower bound
        Arguments.of(0, '\u0000'), // ASCII 'a'
        Arguments.of(97, 'a'), // String that can be parsed
        // "66" → 'B'
        Arguments.of("66", 'B'), // Maximum char value
        Arguments.of((int) Character.MAX_VALUE, Character.MAX_VALUE), // Atomic numeric types
        Arguments.of(new AtomicInteger(65), 'A'), Arguments.of(new AtomicLong(97L), 'a'));
    }

    static Stream<Arguments> outOfRangeProvider() {
        return Stream.of(// negative
        Arguments.of(-1), // just above max
        Arguments.of((long) Character.MAX_VALUE + 1), // far beyond max
        Arguments.of(123_456_789), // string > max
        Arguments.of("70000"), // BigInteger and BigDecimal beyond range
        Arguments.of(new BigInteger("100000")), Arguments.of(new BigDecimal("50000.0")), // Date/Calendar/ZonedDateTime that exceed the range
        Arguments.of(new Date(100_000L)), Arguments.of(createCalendar(100_000L)), Arguments.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(65_536), ZoneOffset.UTC)), // Unsupported object types
        Arguments.of(new Object()), Arguments.of(new int[] { 1, 2, 3 }));
    }

    private static Calendar createCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, "Boolean cannot be converted"), Arguments.of(new BigInteger("-1"), "Negative BigInteger"), Arguments.of(new BigDecimal("65.5"), "BigDecimal with fractional part"), Arguments.of("not a number", "Non‑numeric string"), Arguments.of("", "Empty string"), Arguments.of("12a34", "String with non‑numeric characters"), Arguments.of("66.5", "Decimal string not allowed"));
    }

    static Stream<Arguments> floatingPointSpecialProvider() {
        return Stream.of(Arguments.of(Float.NaN), Arguments.of(Float.POSITIVE_INFINITY), Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY));
    }

    @Test
    void toCharacter_fromIntegerWithinRange_returnsCorrectChar() {
        // given
        // ASCII 'A'
        Integer source = 65;
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('A', result);
    }

    @Test
    void toCharacter_fromLongZero_returnsNullChar() {
        // given
        // char value '\u0000'
        Long source = 0L;
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('\u0000', result);
    }

    @Test
    void toCharacter_fromMaxCharValue_returnsMaxChar() {
        // given
        // 65535
        Integer source = (int) Character.MAX_VALUE;
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals(Character.MAX_VALUE, result);
    }

    @Test
    void toCharacter_fromNegativeNumber_throwsIllegalArgumentException() {
        // given
        Integer source = -1;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCharacter(source, converter));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    void toCharacter_fromNumberGreaterThanMaxChar_throwsIllegalArgumentException() {
        // given
        // 65536
        Long source = (long) Character.MAX_VALUE + 1L;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCharacter(source, converter));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    void toCharacter_fromStringRepresentingNumber_returnsChar() {
        // given
        // ASCII 'a'
        String source = "97";
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('a', result);
    }

    @Test
    void toCharacter_fromCharObject_returnsSameChar() {
        // given
        Character source = 'Z';
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('Z', result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected=''{1}''")
    @MethodSource("validConversionProvider")
    void toCharacter_validConversions(Object from, char expected) {
        char result = NumberConversions.toCharacter(from, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → IllegalArgumentException")
    @MethodSource("outOfRangeProvider")
    void toCharacter_outOfRange(Object from) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCharacter(from, converter));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @ParameterizedTest(name = "[{index}] from={0} (type={1}) → IllegalArgumentException")
    @MethodSource("unsupportedProvider")
    void toCharacter_unsupportedTypes(Object from, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCharacter(from, converter));
        assertTrue(ex.getMessage().contains("out of range") || ex.getMessage().contains("cannot"));
    }

    @Test
    void toCharacter_nullInput_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCharacter(null, converter));
        // The exact message is not mandated; we only verify that an IAE is thrown.
        assertNotNull(ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] from={0} → IllegalArgumentException")
    @MethodSource("floatingPointSpecialProvider")
    void toCharacter_floatingPointSpecialValues(Object from) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCharacter(from, converter));
        assertTrue(ex.getMessage().contains("out of range") || ex.getMessage().contains("cannot"));
    }

    @Test
    void toCharacter_customConverterForcesLongValue() {
        // Create a mock Converter – its internal behavior is irrelevant because we will
        // mock NumberConversions.toLong to return a forced value.
        Converter mockConverter = mock(Converter.class);
        // Mock the static method NumberConversions.toLong to return 66 regardless of input.
        try (MockedStatic<NumberConversions> mocked = mockStatic(NumberConversions.class)) {
            mocked.when(() -> NumberConversions.toLong(any(), eq(mockConverter))).thenReturn(66L);
            // The actual 'from' value does not matter; we use a dummy string.
            char result = NumberConversions.toCharacter("any value", mockConverter);
            assertEquals('B', result);
            // Verify that our stub was indeed used.
            mocked.verify(() -> NumberConversions.toLong(any(), eq(mockConverter)), times(1));
        }
    }
}
