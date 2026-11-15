package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Locale;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocale_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is not used by the method under test, but we provide a real instance
        // to satisfy the method signature.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> provideValidTagCases() {
        return Stream.of(// Simple language only
        Arguments.of("en", Locale.forLanguageTag("en")), // Language‑country
        Arguments.of("en-US", Locale.forLanguageTag("en-US")), // Malformed tag – Locale.forLanguageTag never returns null
        Arguments.of("!!!", Locale.forLanguageTag("!!!")), // Whitespace only – not considered empty by StringUtilities.isEmpty
        Arguments.of("   ", Locale.forLanguageTag("   ")), // Tag with surrounding whitespace
        Arguments.of(" en-GB ", Locale.forLanguageTag(" en-GB ")), // Mixed‑case tag
        Arguments.of("EN-us", Locale.forLanguageTag("EN-us")), // Complex BCP‑47 tag with script sub‑tag
        Arguments.of("zh-Hans-CN", Locale.forLanguageTag("zh-Hans-CN")), // Standard language‑country tag
        Arguments.of("fr-CA", Locale.forLanguageTag("fr-CA")), // Tag containing a variant
        Arguments.of("en-GB-oed", Locale.forLanguageTag("en-GB-oed")));
    }

    private static Stream<Arguments> provideNonStringCases() {
        return Stream.of(// StringBuilder
        Arguments.of(new StringBuilder("en")), // Integer
        Arguments.of(123), // Plain Object
        Arguments.of(new Object()), // Boolean
        Arguments.of(true), // Double
        Arguments.of(3.14), // StringBuffer
        Arguments.of(new StringBuffer("en")), // char array
        Arguments.of(new char[] { 'e', 'n' }));
    }

    @Test
    void toLocale_withNullInput_returnsNull() {
        // Given
        Object source = null;
        // When
        Locale result = StringConversions.toLocale(source, converter);
        // Then
        assertNull(result, "Null input should result in null Locale");
    }

    @Test
    void toLocale_withEmptyString_returnsNull() {
        // Given
        Object source = "";
        // When
        Locale result = StringConversions.toLocale(source, converter);
        // Then
        assertNull(result, "Empty string should result in null Locale");
    }

    @Test
    void toLocale_withValidLanguageTag_returnsCorrespondingLocale() {
        // Given
        Object source = "en-US";
        // When
        Locale result = StringConversions.toLocale(source, converter);
        // Then
        assertNotNull(result, "Resulting Locale should not be null for a valid tag");
        assertEquals("en", result.getLanguage(), "Language should be 'en'");
        assertEquals("US", result.getCountry(), "Country should be 'US'");
    }

    @Test
    void toLocale_withLanguageOnlyTag_returnsLocaleWithOnlyLanguage() {
        // Given
        Object source = "fr";
        // When
        Locale result = StringConversions.toLocale(source, converter);
        // Then
        assertNotNull(result, "Resulting Locale should not be null for a language‑only tag");
        assertEquals("fr", result.getLanguage(), "Language should be 'fr'");
        assertEquals("", result.getCountry(), "Country should be empty");
    }

    @Test
    void toLocale_withMalformedTag_returnsLocaleNonNull() {
        // Given
        Object source = "!!!";
        // When
        Locale result = StringConversions.toLocale(source, converter);
        // Then
        // Locale.forLanguageTag never returns null; it creates a Locale with the raw tag as language.
        assertNotNull(result, "Locale should never be null even for malformed tags");
        assertEquals("!!!", result.getLanguage(), "Language should match the malformed tag");
    }

    @ParameterizedTest(name = "[{index}] tag=''{0}''")
    @MethodSource("provideValidTagCases")
    void toLocale_withVariousTags_returnsLocaleFromLanguageTag(String tag, Locale expected) {
        // Given
        Object source = tag;
        // When
        Locale result = StringConversions.toLocale(source, converter);
        // Then
        assertNotNull(result, "Resulting Locale should never be null for any tag");
        assertEquals(expected, result, () -> "Locale should be exactly the one produced by Locale.forLanguageTag(\"" + tag + "\")");
    }

    @ParameterizedTest(name = "[{index}] non‑String input=''{0}''")
    @MethodSource("provideNonStringCases")
    void toLocale_withNonStringInput_throwsClassCastException(Object nonString) {
        // When / Then
        assertThrows(ClassCastException.class, () -> StringConversions.toLocale(nonString, converter), () -> "Passing a non‑String object should result in ClassCastException");
    }
}
