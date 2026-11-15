package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toAtomicBoolean_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * The real Converter used for most test scenarios.
     */
    private static Converter realConverter;

    /**
     * Mocked Converter for the “Custom Converter usage” group.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; the default constructor provides sensible defaults.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the test tidy.
        converter = null;
    }

    private static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of(null, false, "When the source object is null the conversion should yield false."));
    }

    private static Stream<Arguments> booleanInputProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, true, "A Boolean true should be passed through unchanged."), Arguments.of(Boolean.FALSE, false, "A Boolean false should be passed through unchanged."));
    }

    private static Stream<Arguments> stringCaseInsensitiveProvider() {
        return Stream.of(Arguments.of("true", true, "String \"true\" (any case) converts to true."), Arguments.of("TRUE", true, "Upper‑case string \"TRUE\" converts to true."), Arguments.of("false", false, "String \"false\" (any case) converts to false."), Arguments.of("FaLsE", false, "Mixed‑case string \"FaLsE\" converts to false."), Arguments.of("yes", false, "Non‑boolean literal strings should default to false."));
    }

    private static Stream<Arguments> numericZeroVsNonZeroProvider() {
        return Stream.of(Arguments.of(1, true, "Integer non‑zero value converts to true."), Arguments.of(-5, true, "Negative integer non‑zero value converts to true."), Arguments.of(0, false, "Integer zero converts to false."), Arguments.of(0.0d, false, "Floating‑point zero converts to false."), Arguments.of(0.0001d, true, "Floating‑point non‑zero converts to true."), Arguments.of(0.0e0, false, "Scientific notation zero also converts to false."));
    }

    private static Stream<Arguments> atomicBooleanInputProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), true, "An AtomicBoolean holding true should be converted to true."), Arguments.of(new AtomicBoolean(false), false, "An AtomicBoolean holding false should be converted to false."));
    }

    private static Stream<Arguments> whitespaceStringProvider() {
        return Stream.of(Arguments.of("  true  ", true, "String containing \"true\" with leading/trailing spaces should be trimmed and evaluated as true."), Arguments.of("\tFALSE\n", false, "String containing \"false\" surrounded by whitespace characters should be evaluated as false."));
    }

    private static Stream<Arguments> emptyAndBlankStringProvider() {
        return Stream.of(Arguments.of("", false, "An empty string does not represent a boolean value and should be converted to false."), Arguments.of("   ", false, "A string consisting only of whitespace should be treated as false."));
    }

    private static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(Arguments.of(new BigDecimal("0"), false, "BigDecimal zero should be interpreted as false."), Arguments.of(new BigDecimal("0.000"), false, "BigDecimal representing zero with fractional part should still be false."), Arguments.of(new BigDecimal("123.45"), true, "Any non‑zero BigDecimal should be true."), Arguments.of(BigInteger.ZERO, false, "BigInteger zero converts to false."), Arguments.of(BigInteger.ONE, true, "BigInteger non‑zero converts to true."), Arguments.of(Long.valueOf(0L), false, "Long zero should be false."), Arguments.of(Long.valueOf(42L), true, "Long non‑zero should be true."), Arguments.of(Short.valueOf((short) 0), false, "Short zero converts to false."), Arguments.of(Short.valueOf((short) -2), true, "Negative short non‑zero converts to true."), Arguments.of(Byte.valueOf((byte) 0), false, "Byte zero converts to false."), Arguments.of(Byte.valueOf((byte) 5), true, "Byte non‑zero converts to true."));
    }

    private static Stream<Arguments> customObjectWithoutConverterProvider() {
        return Stream.of(Arguments.of(new Object(), false, "When no Converter is supplied, an unknown object type defaults to false."));
    }

    private static Stream<Arguments> enumInputProvider() {
        return Stream.of(Arguments.of(SampleEnum.TRUE, false, "Enum values are not recognized as booleans by default and should result in false."));
    }

    private static Stream<Arguments> stringNumericLiteralsProvider() {
        return Stream.of(Arguments.of("0", false, "String \"0\" is not the literal \"true\" and therefore converts to false."), Arguments.of("1", false, "String \"1\" does not map to true in the default conversion logic."), Arguments.of("-1", false, "Negative numeric strings are also treated as false."), Arguments.of("0.0", false, "Floating‑point zero expressed as a string should be false."), Arguments.of("0.1", false, "Non‑zero numeric strings are not interpreted as true by default."));
    }

    private static Stream<Arguments> stringNonBooleanLiteralsProvider() {
        return Stream.of(Arguments.of("no", false, "\"no\" is not a recognized boolean literal and should be false."), Arguments.of("on", false, "\"on\" is not treated as true by the default conversion."), Arguments.of("off", false, "\"off\" should also resolve to false."));
    }

    @Test
    void toAtomicBoolean_fromBooleanTrue() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicBoolean result = StringConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true when source Boolean is true");
    }

    @Test
    void toAtomicBoolean_fromBooleanFalse() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicBoolean result = StringConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source Boolean is false");
    }

    @Test
    void toAtomicBoolean_fromStringTrue() {
        // given
        String source = "true";
        // when
        AtomicBoolean result = StringConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true for string \"true\"");
    }

    @Test
    void toAtomicBoolean_fromStringFalse() {
        // given
        String source = "false";
        // when
        AtomicBoolean result = StringConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false for string \"false\"");
    }

    @Test
    void toAtomicBoolean_fromStringMixedCase() {
        // given
        String source = "TrUe";
        // when
        AtomicBoolean result = StringConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean conversion should be case‑insensitive for \"TrUe\"");
    }

    @Test
    void toAtomicBoolean_fromNull() {
        // given
        Object source = null;
        // when
        AtomicBoolean result = StringConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source is null");
    }

    @Test
    void toAtomicBoolean_fromNumericOne() {
        // given
        // many converters treat non‑zero numbers as true
        Integer source = 1;
        // when
        AtomicBoolean result = StringConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        // The exact behavior depends on Converter implementation; we assert true for typical semantics.
        assertTrue(result.get(), "AtomicBoolean should be true for numeric value 1");
    }

    @Test
    void toAtomicBoolean_fromNumericZero() {
        // given
        Integer source = 0;
        // when
        AtomicBoolean result = StringConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false for numeric value 0");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nullInputProvider")
    void toAtomicBoolean_nullInput(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("booleanInputProvider")
    void toAtomicBoolean_booleanInput(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("stringCaseInsensitiveProvider")
    void toAtomicBoolean_stringCaseInsensitive(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("numericZeroVsNonZeroProvider")
    void toAtomicBoolean_numericZeroVsNonZero(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("atomicBooleanInputProvider")
    void toAtomicBoolean_atomicBooleanInput(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("whitespaceStringProvider")
    void toAtomicBoolean_stringWithWhitespace(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("emptyAndBlankStringProvider")
    void toAtomicBoolean_emptyAndBlankStrings(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("numericWrapperProvider")
    void toAtomicBoolean_numericWrapperTypes(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("customObjectWithoutConverterProvider")
    void toAtomicBoolean_customObjectWithoutConverter(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("enumInputProvider")
    void toAtomicBoolean_enumInput(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("stringNumericLiteralsProvider")
    void toAtomicBoolean_stringNumericLiterals(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("stringNonBooleanLiteralsProvider")
    void toAtomicBoolean_stringNonBooleanLiterals(Object from, boolean expected, String _desc) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicBoolean_customConverterReturnsTrue() {
        // Arrange – the mock converter is instructed to return Boolean.TRUE for the specific input.
        when(mockConverter.convert(eq("customObject"), eq(Boolean.class))).thenReturn(Boolean.TRUE);
        // Act
        AtomicBoolean result = StringConversions.toAtomicBoolean("customObject", mockConverter);
        // Assert
        assertNotNull(result);
        assertTrue(result.get(), "Mock converter returning true should yield AtomicBoolean(true)");
        verify(mockConverter, times(1)).convert(eq("customObject"), eq(Boolean.class));
    }

    @Test
    void toAtomicBoolean_customConverterReturnsFalse() {
        // Arrange – the mock converter is instructed to return Boolean.FALSE for the specific input.
        when(mockConverter.convert(eq("customObject"), eq(Boolean.class))).thenReturn(Boolean.FALSE);
        // Act
        AtomicBoolean result = StringConversions.toAtomicBoolean("customObject", mockConverter);
        // Assert
        assertNotNull(result);
        assertFalse(result.get(), "Mock converter returning false should yield AtomicBoolean(false)");
        verify(mockConverter, times(1)).convert(eq("customObject"), eq(Boolean.class));
    }
}
