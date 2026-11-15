package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocale_Test {

    @Mock
    Converter // the method does not use this, but we provide it
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that the converter is not used by the method under test
        verifyNoInteractions(mockConverter);
    }

    /**
     * Provides test cases for valid, case‑sensitive, extended, private‑use, long tags, etc.
     */
    private static Stream<LocaleTestCase> validLocaleProvider() {
        return Stream.of(new LocaleTestCase("EN-us", Locale.forLanguageTag("EN-us"), "Case‑insensitive language, case‑sensitive region"), new LocaleTestCase("eN-uS", Locale.forLanguageTag("eN-uS"), "Mixed‑case tag should be forwarded unchanged"), new LocaleTestCase("zh-Hant-TW", Locale.forLanguageTag("zh-Hant-TW"), "Complex tag with script and region"), new LocaleTestCase("sr-Cyrl-BA", Locale.forLanguageTag("sr-Cyrl-BA"), "Tag with language, script, and region"), new LocaleTestCase("en-US-x-private", Locale.forLanguageTag("en-US-x-private"), "Private‑use subtag should be accepted"), new LocaleTestCase("de-DE-u-co-phonebk", Locale.forLanguageTag("de-DE-u-co-phonebk"), "Unicode extension should be forwarded"), new LocaleTestCase("a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z", Locale.forLanguageTag("a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z"), "Very long syntactically valid BCP‑47 tag"));
    }

    /**
     * Test cases where whitespace-only strings should be treated as empty.
     */
    private static Stream<LocaleTestCase> whitespaceProvider() {
        return Stream.of(new LocaleTestCase("   ", null, "String of spaces should be empty"), new LocaleTestCase("\t\n", null, "String with tab and newline should be empty"));
    }

    /**
     * Test cases where the input is not a String and a ClassCastException is expected.
     */
    private static Stream<Object> nonStringProvider() {
        return Stream.of(// Integer
        12345, new // Anonymous object with custom toString
        Object() {

            @Override
            public String toString() {
                return "en-CA";
            }
        });
    }

    @Test
    void toLocale_NullInput_ReturnsNull() {
        Object from = null;
        Locale result = StringConversions.toLocale(from, mockConverter);
        assertNull(result, "Locale should be null when input is null");
    }

    @Test
    void toLocale_EmptyString_ReturnsNull() {
        Object from = "";
        Locale result = StringConversions.toLocale(from, mockConverter);
        assertNull(result, "Locale should be null when input string is empty");
    }

    @Test
    void toLocale_ValidLanguageTag_ReturnsCorrectLocale() {
        Object from = "en-US";
        Locale result = StringConversions.toLocale(from, mockConverter);
        assertNotNull(result, "Locale should not be null for a valid language tag");
        assertEquals("en", result.getLanguage(), "Language part should be 'en'");
        assertEquals("US", result.getCountry(), "Country part should be 'US'");
    }

    @Test
    void toLocale_ValidSimpleLanguage_ReturnsCorrectLocale() {
        Object from = "fr";
        Locale result = StringConversions.toLocale(from, mockConverter);
        assertNotNull(result, "Locale should not be null for a valid simple language tag");
        assertEquals("fr", result.getLanguage(), "Language part should be 'fr'");
        // Country will be empty for a simple language tag
        assertEquals("", result.getCountry(), "Country part should be empty for simple language tag");
    }

    @Test
    void toLocale_InvalidTag_ReturnsLocaleWithRawTag() {
        // Locale.forLanguageTag tolerates unknown tags and returns a Locale with the raw tag as language
        Object from = "invalid-tag-123";
        Locale result = StringConversions.toLocale(from, mockConverter);
        assertNotNull(result, "Locale should not be null even for an unrecognised tag");
        assertEquals("invalid-tag-123", result.toLanguageTag(), "Locale's language tag should match the original input");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validLocaleProvider")
    void toLocale_ValidAndExtendedTags_ReturnsExpectedLocale(LocaleTestCase tc) {
        Locale result = StringConversions.toLocale(tc.from(), mockConverter);
        assertNotNull(result, tc.description());
        // Locale equality works via language, script, country, extensions etc.
        assertEquals(tc.expected(), result, tc.description());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("whitespaceProvider")
    void toLocale_WhitespaceStrings_ReturnNull(LocaleTestCase tc) {
        Locale result = StringConversions.toLocale(tc.from(), mockConverter);
        assertNull(result, tc.description());
    }

    @ParameterizedTest(name = "[{index}] Non‑String input should throw ClassCastException")
    @MethodSource("nonStringProvider")
    void toLocale_NonStringInput_ThrowsClassCastException(Object nonString) {
        assertThrows(ClassCastException.class, () -> StringConversions.toLocale(nonString, mockConverter), "Casting non‑String should propagate ClassCastException");
    }

    /**
     * Test that passing a null converter does not affect the result.
     */
    @Test
    void toLocale_NullConverterArgument_Ignored() {
        Object from = "en-GB";
        Locale result = StringConversions.toLocale(from, null);
        assertNotNull(result);
        assertEquals(Locale.forLanguageTag("en-GB"), result);
    }

    /**
     * Test that an unknown but syntactically valid tag is forwarded unchanged.
     */
    @Test
    void toLocale_UnknownTag_ReturnsLocaleWithRawTag() {
        Object from = "invalid_tag";
        Locale result = StringConversions.toLocale(from, mockConverter);
        assertNotNull(result);
        assertEquals(Locale.forLanguageTag("invalid_tag"), result);
    }
}
