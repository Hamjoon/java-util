package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocale_Test {

    /**
     * The Converter instance required by the method signature.
     *  It is not used inside {@code toLocale}, so a mock is sufficient.
     */
    @Mock
    private Converter mockConverter;

    @Test
    void toLocale_EmptyString_ReturnsNull() {
        // GIVEN
        String empty = "";
        // WHEN
        Locale result = StringConversions.toLocale(empty, mockConverter);
        // THEN
        assertNull(result, "An empty string should result in a null Locale");
    }

    @Test
    void toLocale_NullOrWhitespaceString_ReturnsNull() {
        // GIVEN
        String whitespace = "   ";
        // WHEN
        Locale result = StringConversions.toLocale(whitespace, mockConverter);
        // THEN
        // StringUtilities.isEmpty treats a string consisting only of whitespace as empty.
        assertNull(result, "A whitespace‑only string should result in a null Locale");
    }

    @Test
    void toLocale_ValidLanguageTag_ReturnsCorrespondingLocale() {
        // GIVEN
        String tag = "en-US";
        // WHEN
        Locale result = StringConversions.toLocale(tag, mockConverter);
        // THEN
        assertNotNull(result, "Locale should not be null for a valid language tag");
        assertEquals("en", result.getLanguage(), "Language part should be 'en'");
        assertEquals("US", result.getCountry(), "Country part should be 'US'");
    }

    @Test
    void toLocale_LanguageOnlyTag_ReturnsLocaleWithOnlyLanguage() {
        // GIVEN
        String tag = "fr";
        // WHEN
        Locale result = StringConversions.toLocale(tag, mockConverter);
        // THEN
        assertNotNull(result, "Locale should not be null for a language‑only tag");
        assertEquals("fr", result.getLanguage(), "Language should be 'fr'");
        assertEquals("", result.getCountry(), "Country should be empty for a language‑only tag");
    }

    @Test
    void toLocale_InvalidTag_ReturnsLocaleWithOriginalTag() {
        // GIVEN
        String tag = "invalid-tag-123";
        // WHEN
        Locale result = StringConversions.toLocale(tag, mockConverter);
        // THEN
        // Locale.forLanguageTag returns a Locale whose language is the whole tag when it cannot be parsed.
        assertNotNull(result);
        assertEquals(tag, result.toLanguageTag(), "Locale's language tag should equal the original string when the tag cannot be parsed");
    }

    @Test
    void toLocale_NullInput_ReturnsNull() {
        // GIVEN
        Object nullInput = null;
        // WHEN
        // passing null converter explicitly
        Locale result = StringConversions.toLocale(nullInput, null);
        // THEN
        assertNull(result, "A null input should be treated as empty and return null");
    }

    @ParameterizedTest(name = "[{index}] tag=''{0}'' → expected Locale={1}")
    @CsvSource({ // language‑only tag
    "'en',      ENGLISH", // language‑region tag
    "'en-US',   US", // French‑Canada tag
    "'fr-CA',   CANADA_FRENCH" })
    void toLocale_ValidLanguageTags_Parameterized(String tag, Locale expected) {
        // WHEN
        // converter not needed
        Locale result = StringConversions.toLocale(tag, null);
        // THEN
        assertNotNull(result, "Resulting Locale must not be null for tag: " + tag);
        assertEquals(expected, result, "Locale derived from tag '" + tag + "' does not match expected");
    }

    @Test
    void toLocale_NonStringInput_ThrowsClassCastException_Integer() {
        // GIVEN
        Integer nonString = 123;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toLocale(nonString, mockConverter), "Passing an Integer should cause a ClassCastException");
    }

    @Test
    void toLocale_NonStringInput_ThrowsClassCastException_Boolean() {
        // GIVEN
        Boolean nonString = Boolean.TRUE;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toLocale(nonString, mockConverter), "Passing a Boolean should cause a ClassCastException");
    }
}
