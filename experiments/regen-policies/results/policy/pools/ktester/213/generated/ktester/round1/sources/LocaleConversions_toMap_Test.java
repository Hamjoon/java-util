package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class LocaleConversions_toMap_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter sharedConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter has a public constructor that requires a ConverterOptions instance.
        // Using the default options is sufficient for these tests.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up, but null‑ify the reference for completeness.
        sharedConverter = null;
    }

    /**
     * Supplies a range of Locale instances together with the string we expect
     *  {@link LocaleConversions#toString(Object, Converter)} to return when the
     *  mock Converter simply calls {@code toString()} on the source object.
     */
    private static Stream<Arguments> provideLocalesForSuccessfulConversion() {
        return Stream.of(Arguments.of(Locale.US, "en_US"), Arguments.of(Locale.ENGLISH, "en"), Arguments.of(Locale.GERMANY, "de_DE"), Arguments.of(new Locale("en", "US", "POSIX"), "en_US_POSIX"), // Locale.ROOT.toString() is the empty string
        Arguments.of(Locale.ROOT, ""));
    }

    @Test
    void toMap_returnsMapContainingLocaleKeyAndStringValue() {
        // GIVEN
        // any non‑null locale
        Locale locale = Locale.CANADA_FRENCH;
        // WHEN
        Map<?, ?> result = LocaleConversions.toMap(locale, sharedConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertTrue(result.containsKey(MapConversions.LOCALE), "Map must contain the key defined by MapConversions.LOCALE");
        Object value = result.get(MapConversions.LOCALE);
        assertNotNull(value, "Value for the LOCALE key must not be null");
        // The implementation of LocaleConversions.toString ultimately produces a String
        // representation of the locale (e.g., "fr_CA").  We assert that the value is a String
        // and that it matches the expected format.
        assertTrue(value instanceof String, "Value should be a String");
        // Locale's default string representation
        String expected = locale.toString();
        assertEquals(expected, value, "String representation of the locale must match");
    }

    @Test
    void toMap_withNullLocaleThrowsClassCastException() {
        // GIVEN
        Object nullLocale = null;
        // WHEN / THEN
        // The method casts the input to Locale without a null check,
        // so a NullPointerException (or ClassCastException) is expected.
        // In practice the cast succeeds (null) but the subsequent call to
        // LocaleConversions.toString(null, ...) will NPE, which we assert here.
        assertThrows(ClassCastException.class, () -> LocaleConversions.toMap(nullLocale, sharedConverter));
    }

    /**
     * Verify that {@code toMap} returns a {@link LinkedHashMap} containing a single entry
     * whose key is {@link MapConversions#LOCALE} and whose value is the string
     * representation produced by {@link LocaleConversions#toString(Object, Converter)}.
     *
     * The {@link Converter} is mocked to simply return {@code src.toString()}.
     */
    @ParameterizedTest(name = "[{index}] locale={0}, expectedString={1}")
    @MethodSource("provideLocalesForSuccessfulConversion")
    void toMap_successfulConversion(Locale locale, String expectedString) {
        // GIVEN – a mock Converter that delegates to Object.toString()
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(String.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            return src == null ? null : src.toString();
        });
        // WHEN
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        // THEN – basic map contract
        assertNotNull(result, "Resulting map must not be null");
        assertTrue(result instanceof LinkedHashMap, "Resulting map should be a LinkedHashMap to preserve insertion order");
        assertEquals(1, result.size(), "Map must contain exactly one entry");
        // THEN – key/value correctness
        assertTrue(result.containsKey(MapConversions.LOCALE), "Map must contain the key defined by MapConversions.LOCALE");
        Object value = result.get(MapConversions.LOCALE);
        assertEquals(expectedString, value, "The value stored for the LOCALE key must match the expected string representation");
    }

    @Test
    void toMap_nullLocaleProducesNullValueWhenConverterReturnsNull() {
        // GIVEN – a mock Converter that returns null for any input
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(isNull(), eq(String.class))).thenReturn(null);
        // WHEN
        Map<?, ?> result = LocaleConversions.toMap(null, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null even when the source locale is null");
        assertTrue(result.containsKey(MapConversions.LOCALE), "Map must contain the LOCALE key");
        assertNull(result.get(MapConversions.LOCALE), "Value for LOCALE key should be null when converter returns null");
    }

    @Test
    void toMap_nullLocaleAndConverterThrowsNullPointerException() {
        // GIVEN – a mock Converter that throws NPE when source is null
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(isNull(), eq(String.class))).thenThrow(new NullPointerException("src is null"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocaleConversions.toMap(null, mockConverter), "A NullPointerException from the converter should propagate");
    }

    @Test
    void toMap_nullConverterThrowsNullPointerException() {
        // GIVEN a valid locale but a null converter
        Locale locale = Locale.US;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocaleConversions.toMap(locale, null), "Passing a null Converter should result in a NullPointerException");
    }

    @Test
    void toMap_invalidFromTypeThrowsClassCastException() {
        // GIVEN – a non‑Locale object (String)
        Object notALocale = "not a locale";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocaleConversions.toMap(notALocale, sharedConverter), "Casting a non‑Locale object to Locale must throw ClassCastException");
    }

    @Test
    void toMap_converterThrowsRuntimeException() {
        // GIVEN – a mock Converter that throws RuntimeException
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(String.class))).thenThrow(new RuntimeException("converter error"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> LocaleConversions.toMap(Locale.US, mockConverter));
        assertEquals("converter error", ex.getMessage(), "The RuntimeException message should be propagated unchanged");
    }

    @Test
    void toMap_converterThrowsAssertionError() {
        // GIVEN – a mock Converter that throws AssertionError
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(String.class))).thenThrow(new AssertionError("assertion failed"));
        // WHEN / THEN
        AssertionError err = assertThrows(AssertionError.class, () -> LocaleConversions.toMap(Locale.US, mockConverter));
        assertEquals("assertion failed", err.getMessage(), "The AssertionError message should be propagated unchanged");
    }
}
