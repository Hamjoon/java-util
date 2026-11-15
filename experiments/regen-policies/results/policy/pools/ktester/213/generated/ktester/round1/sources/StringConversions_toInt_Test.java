package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toInt_Test {

    /**
     * A real {@link Converter} instance – the conversion logic inside {@code toInt} does not depend on it.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options (no overrides needed for these tests)
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference
        converter = null;
    }

    private static Stream<Arguments> emptyAndSimpleConversions() {
        return Stream.of(// Empty string should be treated as zero according to the early empty‑check.
        Arguments.of("", null, 0), // A well‑formed integer string is parsed directly by Integer.valueOf.
        Arguments.of("123", null, 123));
    }

    private static Stream<Arguments> boundaryValues() {
        return Stream.of(// Integer.MAX_VALUE
        Arguments.of(String.valueOf(Integer.MAX_VALUE), Integer.MAX_VALUE), // Integer.MIN_VALUE
        Arguments.of(String.valueOf(Integer.MIN_VALUE), Integer.MIN_VALUE), // Leading plus sign is accepted by Integer.valueOf
        Arguments.of("+42", 42), // Leading zeros are ignored
        Arguments.of("00123", 123));
    }

    private static Stream<Arguments> outOfRangeValues() {
        return Stream.of(// Just above Integer.MAX_VALUE
        Arguments.of(String.valueOf((long) Integer.MAX_VALUE + 1L)), // Just below Integer.MIN_VALUE
        Arguments.of(String.valueOf((long) Integer.MIN_VALUE - 1L)));
    }

    private static Stream<Arguments> invalidFormats() {
        return Stream.of(// Decimal number
        Arguments.of("12.34"), // Alphanumeric
        Arguments.of("abc123"), // Leading whitespace
        Arguments.of("   56"), // Lone sign character
        Arguments.of("-"));
    }

    private static Stream<Arguments> nonStringInputs() {
        return Stream.of(// Integer instance (not a String)
        Arguments.of(123, Integer.class), // StringBuilder (a CharSequence but not a String)
        Arguments.of(new StringBuilder("456"), StringBuilder.class));
    }

    @Test
    void toInt_EmptyString_ReturnsZero() {
        // given
        String input = "";
        // when
        Integer result = StringConversions.toInt(input, converter);
        // then
        assertEquals(0, result);
    }

    @Test
    void toInt_ValidIntegerString_ReturnsParsedValue() {
        // given
        String input = "12345";
        // when
        Integer result = StringConversions.toInt(input, converter);
        // then
        assertEquals(12345, result);
    }

    @Test
    void toInt_NumberJustAboveIntMax_ThrowsIllegalArgumentException() {
        // given
        // "2147483648"
        String input = String.valueOf((long) Integer.MAX_VALUE + 1L);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(input, converter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @Test
    void toInt_NumberJustBelowIntMin_ThrowsIllegalArgumentException() {
        // given
        // "-2147483649"
        String input = String.valueOf((long) Integer.MIN_VALUE - 1L);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(input, converter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @Test
    void toInt_NonNumericString_ThrowsIllegalArgumentException() {
        // given
        String input = "abcXYZ";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(input, converter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("emptyAndSimpleConversions")
    void toInt_EmptyAndSimpleConversions(Object from, Converter conv, Integer expected) {
        Integer result = StringConversions.toInt(from, conv);
        assertEquals(expected, result);
    }

    @Test
    void toInt_NullInput_ThrowsNullPointerException() {
        // The method casts the argument to String, so a null reference triggers NPE.
        assertThrows(NullPointerException.class, () -> StringConversions.toInt(null, null));
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → expected={1}")
    @MethodSource("boundaryValues")
    void toInt_BoundaryValues(String from, Integer expected) {
        Integer result = StringConversions.toInt(from, null);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → IllegalArgumentException")
    @MethodSource("outOfRangeValues")
    void toInt_OutOfRangeValues(String from) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(from, null));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → IllegalArgumentException")
    @MethodSource("invalidFormats")
    void toInt_InvalidFormats(String from) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(from, null));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @Test
    void toInt_NonNullConverter_Ignored() {
        // The converter argument is not used; any non‑null object should behave the same.
        String input = "99";
        // passing a plain Object
        Integer result = StringConversions.toInt(input, new Object());
        assertEquals(99, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} (type={1}) → ClassCastException")
    @MethodSource("nonStringInputs")
    void toInt_NonStringInput(Object from, Class<?> type) {
        assertThrows(ClassCastException.class, () -> StringConversions.toInt(from, null));
    }
}
