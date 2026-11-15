package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toShort_Test {

    /**
     * The Converter is required by the signature but not used by the method under test.
     *  We provide a mock instance so the test does not depend on any real configuration.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us via the @Mock annotation.
        // Ensure the mock is clean before each test.
        reset(mockConverter);
    }

    private static Stream<Arguments> edgeBoundaryProvider() {
        return Stream.of(// Short.MIN_VALUE
        Arguments.of("-32768", (short) -32768), // Short.MAX_VALUE
        Arguments.of("32767", (short) 32767));
    }

    private static Stream<Arguments> basicSuccessProvider() {
        return Stream.of(// empty string -> zero
        Arguments.of("", (short) 0), // simple positive
        Arguments.of("123", (short) 123), // simple negative
        Arguments.of("-321", (short) -321), // whitespace trimmed via fallback
        Arguments.of(" 45 ", (short) 45));
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(// outside short range
        Arguments.of("40000"), // non‑numeric
        Arguments.of("abc"), // whitespace only
        Arguments.of("   "), // decimal number
        Arguments.of("12.34"));
    }

    @Test
    void toShort_EmptyString_ReturnsZero() {
        // given
        String input = "";
        // when
        Short result = StringConversions.toShort(input, mockConverter);
        // then
        assertEquals((short) 0, result);
    }

    @Test
    void toShort_ValidShortString_ReturnsParsedValue() {
        // given
        String input = "12345";
        // when
        Short result = StringConversions.toShort(input, mockConverter);
        // then
        assertEquals((short) 12345, result);
    }

    @Test
    void toShort_NegativeShortString_ReturnsParsedValue() {
        // given
        String input = "-321";
        // when
        Short result = StringConversions.toShort(input, mockConverter);
        // then
        assertEquals((short) -321, result);
    }

    @Test
    void toShort_ValueJustAboveShortMax_ThrowsIllegalArgumentException() {
        // given
        // Short.MAX_VALUE + 1
        String input = "32768";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(input, mockConverter));
        assertTrue(ex.getMessage().contains("not parseable as a short value"));
    }

    @Test
    void toShort_NonNumericString_ThrowsIllegalArgumentException() {
        // given
        String input = "abc123";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(input, mockConverter));
        assertTrue(ex.getMessage().contains("not parseable as a short value"));
    }

    @Test
    void toShort_ValueJustBelowShortMin_ThrowsIllegalArgumentException() {
        // given
        // Short.MIN_VALUE - 1
        String input = "-32769";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(input, mockConverter));
        assertTrue(ex.getMessage().contains("not parseable as a short value"));
    }

    @Test
    void toShort_DecimalString_ThrowsIllegalArgumentException() {
        // given
        String input = "12.34";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(input, mockConverter));
        assertTrue(ex.getMessage().contains("not parseable as a short value"));
    }

    @Test
    void toShort_WhitespaceOnlyString_ThrowsIllegalArgumentException() {
        // given
        String input = "   ";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(input, mockConverter));
        assertTrue(ex.getMessage().contains("not parseable as a short value"));
    }

    @Test
    void toShort_StringWithLeadingAndTrailingSpaces_FallsBackToLongAndSucceeds() {
        // given
        String input = " 45 ";
        // when
        Short result = StringConversions.toShort(input, mockConverter);
        // then
        assertEquals((short) 45, result);
    }

    @Test
    void toShort_NullInput_ThrowsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toShort(input, mockConverter));
    }

    @Test
    void toShort_NonStringInput_ThrowsClassCastException() {
        // given
        // Integer, not a String
        Object input = 123;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toShort(input, mockConverter));
    }

    @Test
    void toShort_LongOverflowString_PropagatesNumberFormatException() {
        // given
        // Long.MAX_VALUE + 1
        String input = "9223372036854775808";
        // when / then
        NumberFormatException ex = assertThrows(NumberFormatException.class, () -> StringConversions.toShort(input, mockConverter));
        // The method does not wrap this exception, so we simply verify the type.
        assertNotNull(ex);
    }

    @ParameterizedTest(name = "toShort(''{0}'') should return {1}")
    @MethodSource("edgeBoundaryProvider")
    void toShort_EdgeBoundaries_ReturnParsedValue(String input, short expected) {
        Short result = StringConversions.toShort(input, mockConverter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "toShort(''{0}'') => {1}")
    @MethodSource("basicSuccessProvider")
    void toShort_BasicSuccessCases(String input, short expected) {
        Short result = StringConversions.toShort(input, mockConverter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "toShort(''{0}'') throws IllegalArgumentException")
    @MethodSource("illegalArgumentProvider")
    void toShort_IllegalArgumentScenarios(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(input, mockConverter));
        assertTrue(ex.getMessage().contains("not parseable as a short value"));
    }
}
