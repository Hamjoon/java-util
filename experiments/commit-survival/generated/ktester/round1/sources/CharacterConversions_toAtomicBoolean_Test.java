package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toAtomicBoolean_Test {

    /**
     * Mocked {@link Converter} used for all test cases.
     */
    @Mock
    private Converter mockConverter;

    /* -------------------------------------------------------------
     *  Helper class used for the "null converter usage" scenario.
     * ------------------------------------------------------------- */
    private static final class CustomConvertible {

        private final String value;

        CustomConvertible(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "CustomConvertible{" + value + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates and injects the mock for us via the @Mock annotation.
        // Ensure the mock starts with a clean state before each test.
        reset(mockConverter);
    }

    /**
     * Helper that stubs the {@code Converter} to return the supplied Boolean
     * regardless of the input value.  The real {@code CharacterConversions.toBoolean}
     * implementation ultimately calls {@code Converter.convert(value, Boolean.class)}.
     *
     * @param result the Boolean value the mock should return
     */
    private void stubConverterToReturn(Boolean result) {
        // The exact signature of Converter.convert is not required for the test;
        // we use Mockito's any() matchers to cover any possible overload.
        when(mockConverter.convert(any(), any())).thenReturn(result);
    }

    /**
     * Stubs the mock to throw the supplied RuntimeException when invoked.
     */
    private void stubConverterToThrow(RuntimeException ex) {
        when(mockConverter.convert(any(), any())).thenThrow(ex);
    }

    static Stream<Arguments> nullInputProvider() {
        // both arguments null → false
        return Stream.of(Arguments.of(null, null, false, "Both source and converter are null"));
    }

    static Stream<Arguments> booleanInputProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, null, true, "Direct Boolean true"), Arguments.of(Boolean.FALSE, null, false, "Direct Boolean false"));
    }

    static Stream<Arguments> stringInputProvider() {
        return Stream.of(Arguments.of("true", null, true, "String \"true\" (case‑insensitive)"), Arguments.of("TRUE", null, true, "Upper‑case string \"TRUE\""), Arguments.of("false", null, false, "String \"false\""), Arguments.of("yes", null, true, "Non‑standard truthy string \"yes\""), Arguments.of("0", null, false, "String representing zero"), Arguments.of("", null, false, "Empty string"), Arguments.of("   ", null, false, "Whitespace‑only string"), Arguments.of("  true  ", null, true, "String with surrounding spaces spelling true"), Arguments.of("false ", null, false, "String with trailing space spelling false"));
    }

    static Stream<Arguments> numericInputProvider() {
        return Stream.of(Arguments.of(0, null, false, "Integer zero"), Arguments.of(1, null, true, "Integer one"), Arguments.of(-5, null, true, "Negative integer"), Arguments.of(0.0, null, false, "Double zero"), Arguments.of(3.14, null, true, "Double non‑zero"));
    }

    static Stream<Arguments> stringNumericProvider() {
        return Stream.of(Arguments.of("1", null, true, "String numeric \"1\""), Arguments.of("-0", null, false, "String \"-0\" representing zero"), Arguments.of("0.0", null, false, "String \"0.0\" representing floating‑point zero"));
    }

    static Stream<Arguments> nonStringObjectsProvider() {
        return Stream.of(Arguments.of(new Object(), null, false, "Arbitrary Object"), Arguments.of(UUID.randomUUID(), null, false, "UUID instance"), Arguments.of(new Date(0L), null, false, "Date instance"));
    }

    static Stream<Arguments> atomicTypesProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), null, true, "AtomicBoolean true"), Arguments.of(new AtomicInteger(0), null, false, "AtomicInteger zero"), Arguments.of(new AtomicLong(123L), null, true, "AtomicLong non‑zero"));
    }

    static Stream<Arguments> converterFallbackProvider() {
        return Stream.of(Arguments.of("custom", true, "Mock converter returning true"), Arguments.of("custom", false, "Mock converter returning false"));
    }

    static Stream<Arguments> converterExceptionProvider() {
        return Stream.of(Arguments.of("any", new RuntimeException("converter failure")));
    }

    static Stream<Arguments> invalidNumericStringProvider() {
        return Stream.of(Arguments.of("123abc"));
    }

    static Stream<Arguments> unsupportedTypeProvider() {
        return Stream.of(Arguments.of(new AtomicReference<>("test")));
    }

    static Stream<Arguments> nullConverterUsageProvider() {
        // Simulate a source that requires a Converter (e.g., a custom convertible type).
        // We don't have the actual class, so we use a placeholder that the library would
        // attempt to convert via the supplied Converter.
        return Stream.of(Arguments.of(new CustomConvertible("value")));
    }

    @Test
    void toAtomicBoolean_givenBooleanTrue_returnsAtomicTrue() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        stubConverterToReturn(Boolean.TRUE);
        // WHEN
        AtomicBoolean atomic = CharacterConversions.toAtomicBoolean(source, mockConverter);
        // THEN
        assertNotNull(atomic, "Resulting AtomicBoolean should not be null");
        assertTrue(atomic.get(), "AtomicBoolean should contain true");
        // Verify that the converter was asked to convert the original value.
        verify(mockConverter, atLeastOnce()).convert(eq(source), any());
    }

    @Test
    void toAtomicBoolean_givenStringFalse_returnsAtomicFalse() {
        // GIVEN
        String source = "false";
        stubConverterToReturn(Boolean.FALSE);
        // WHEN
        AtomicBoolean atomic = CharacterConversions.toAtomicBoolean(source, mockConverter);
        // THEN
        assertNotNull(atomic, "Resulting AtomicBoolean should not be null");
        assertFalse(atomic.get(), "AtomicBoolean should contain false");
        verify(mockConverter, atLeastOnce()).convert(eq(source), any());
    }

    @Test
    void toAtomicBoolean_givenNumericOne_returnsAtomicTrue() {
        // GIVEN
        Integer source = 1;
        stubConverterToReturn(Boolean.TRUE);
        // WHEN
        AtomicBoolean atomic = CharacterConversions.toAtomicBoolean(source, mockConverter);
        // THEN
        assertNotNull(atomic);
        assertTrue(atomic.get());
        verify(mockConverter, atLeastOnce()).convert(eq(source), any());
    }

    @Test
    void toAtomicBoolean_givenNull_returnsAtomicFalse() {
        // GIVEN
        Object source = null;
        // The library’s contract states that a null conversion to Boolean yields null,
        // which is interpreted as false by the AtomicBoolean constructor.
        stubConverterToReturn(null);
        // WHEN
        AtomicBoolean atomic = CharacterConversions.toAtomicBoolean(source, mockConverter);
        // THEN
        assertNotNull(atomic);
        assertFalse(atomic.get(), "Null input should result in false");
        verify(mockConverter, atLeastOnce()).convert(isNull(), any());
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullInputProvider")
    void toAtomicBoolean_nullInput(Object from, Converter converter, boolean expected, String description) {
        // WHEN
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(from, converter);
        // THEN
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("booleanInputProvider")
    void toAtomicBoolean_booleanInput(Object from, Converter converter, boolean expected, String description) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(from, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("stringInputProvider")
    void toAtomicBoolean_stringInput(Object from, Converter converter, boolean expected, String description) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(from, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("numericInputProvider")
    void toAtomicBoolean_numericInput(Object from, Converter converter, boolean expected, String description) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(from, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("stringNumericProvider")
    void toAtomicBoolean_stringNumericInput(Object from, Converter converter, boolean expected, String description) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(from, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nonStringObjectsProvider")
    void toAtomicBoolean_nonStringObjects(Object from, Converter converter, boolean expected, String description) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(from, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("atomicTypesProvider")
    void toAtomicBoolean_atomicWrapperTypes(Object from, Converter converter, boolean expected, String description) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(from, converter);
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("converterFallbackProvider")
    void toAtomicBoolean_converterFallback(String source, boolean converterResult, String description) {
        // Arrange: stub the mock to return the desired Boolean
        stubConverterToReturn(converterResult);
        // WHEN
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, mockConverter);
        // THEN
        assertNotNull(result, description);
        assertEquals(converterResult, result.get(), description);
        verify(mockConverter, atLeastOnce()).convert(eq(source), any());
    }

    @ParameterizedTest(name = "Converter throws RuntimeException")
    @MethodSource("converterExceptionProvider")
    void toAtomicBoolean_converterThrowsException(String source, RuntimeException ex) {
        stubConverterToThrow(ex);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> CharacterConversions.toAtomicBoolean(source, mockConverter), "Exception should propagate from the converter");
        assertEquals(ex.getMessage(), thrown.getMessage());
    }

    @ParameterizedTest(name = "Invalid numeric string causes NumberFormatException")
    @MethodSource("invalidNumericStringProvider")
    void toAtomicBoolean_invalidNumericString(String source) {
        // The implementation is expected to attempt numeric parsing and fail.
        assertThrows(NumberFormatException.class, () -> CharacterConversions.toAtomicBoolean(source, null), "Parsing an invalid numeric string should throw NumberFormatException");
    }

    @ParameterizedTest(name = "Unsupported type causes IllegalArgumentException")
    @MethodSource("unsupportedTypeProvider")
    void toAtomicBoolean_unsupportedType(Object source) {
        assertThrows(IllegalArgumentException.class, () -> CharacterConversions.toAtomicBoolean(source, null), "Unsupported source types should cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "Null converter usage leads to NullPointerException")
    @MethodSource("nullConverterUsageProvider")
    void toAtomicBoolean_nullConverterUsage(Object source) {
        // The source requires a Converter, but we pass null → NPE expected.
        assertThrows(NullPointerException.class, () -> CharacterConversions.toAtomicBoolean(source, null), "Missing converter for convertible source should throw NullPointerException");
    }
}
