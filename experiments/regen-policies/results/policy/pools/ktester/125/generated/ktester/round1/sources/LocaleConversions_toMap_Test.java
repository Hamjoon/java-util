package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.cedarsoftware.util.convert.MapConversions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocaleConversions_toMap_Test {

    @Mock
    private Converter // not used by the method but required as a parameter
    mockConverter;

    /* -------------------------------------------------------------
     *  Helper – dummy Converter implementation for the few tests that
     *  explicitly need a non‑null instance.
     * ------------------------------------------------------------- */
    private static class DummyConverter implements Converter {
        // No behaviour required – LocaleConversions.toMap never uses the Converter.
    }

    private static Stream<Arguments> standardLocaleProvider() {
        return Stream.of(// Locale contains only language; country, script and variant are empty strings.
        Arguments.of(new Locale("en"), Map.of(LANGUAGE, "en"), "Locale contains only language; only LANGUAGE key should be present"), // Locale has language and non‑empty country; script and variant are empty.
        Arguments.of(new Locale("en", "US"), Map.of(LANGUAGE, "en", COUNTRY, "US"), "Locale with language and country; COUNTRY key should be present"), // Locale has language and script; country and variant are empty.
        Arguments.of(new Locale.Builder().setLanguage("en").setScript("Latn").build(), Map.of(LANGUAGE, "en", SCRIPT, "Latn"), "Locale with language and script; SCRIPT key should be present"));
    }

    private static Stream<Arguments> additionalLocaleProvider() {
        return Stream.of(// language + country + script + variant
        Arguments.of(new Locale.Builder().setLanguage("en").setRegion("US").setScript("Latn").setVariant("POSIX").build(), Map.of(LANGUAGE, "en", COUNTRY, "US", SCRIPT, "Latn", VARIANT, "POSIX"), "All four components populated"), // language + variant only
        Arguments.of(new Locale.Builder().setLanguage("en").setVariant("POSIX").build(), Map.of(LANGUAGE, "en", VARIANT, "POSIX"), "Only language and variant present"), // language + country + script
        Arguments.of(new Locale.Builder().setLanguage("en").setRegion("US").setScript("Latn").build(), Map.of(LANGUAGE, "en", COUNTRY, "US", SCRIPT, "Latn"), "Language, country and script present"), // language + country + variant
        Arguments.of(new Locale.Builder().setLanguage("en").setRegion("US").setVariant("POSIX").build(), Map.of(LANGUAGE, "en", COUNTRY, "US", VARIANT, "POSIX"), "Language, country and variant present"), // language + script + variant
        Arguments.of(new Locale.Builder().setLanguage("en").setScript("Latn").setVariant("POSIX").build(), Map.of(LANGUAGE, "en", SCRIPT, "Latn", VARIANT, "POSIX"), "Language, script and variant present"), // empty language, non‑empty country
        Arguments.of(new Locale("", "US"), Map.of(LANGUAGE, "", COUNTRY, "US"), "Empty language string with a country"), // empty language with script
        Arguments.of(new Locale.Builder().setLanguage("").setScript("Latn").build(), Map.of(LANGUAGE, "", SCRIPT, "Latn"), "Empty language and a script"), // all components empty strings
        Arguments.of(new Locale(""), Map.of(LANGUAGE, ""), "All components empty – only LANGUAGE key with empty value"), // country whitespace only, script present
        Arguments.of(new Locale.Builder().setLanguage("en").setRegion("   ").setScript("Latn").build(), Map.of(LANGUAGE, "en", SCRIPT, "Latn"), "Country whitespace only – should be omitted, script kept"), // non‑null Converter instance (method ignores it)
        Arguments.of(new Locale("en", "US"), Map.of(LANGUAGE, "en", COUNTRY, "US"), "Non‑null converter supplied – result identical"));
    }

    private static Stream<Arguments> errorScenarioProvider() {
        return Stream.of(// Not a Locale – causes ClassCastException
        Arguments.of("not a locale", ClassCastException.class, "Object that is not a Locale should trigger ClassCastException"), // Null 'from' argument – NullPointerException when dereferencing
        Arguments.of(null, NullPointerException.class, "Null 'from' argument should trigger NullPointerException"));
    }

    @Test
    void toMap_WithOnlyLanguage_ShouldContainOnlyLanguageKey() {
        // language only
        Locale locale = new Locale("en");
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("en", result.get(LANGUAGE));
        assertFalse(result.containsKey(COUNTRY));
        assertFalse(result.containsKey(SCRIPT));
        assertFalse(result.containsKey(VARIANT));
    }

    @Test
    void toMap_WithLanguageAndCountry_ShouldContainLanguageAndCountry() {
        // language + country
        Locale locale = new Locale("en", "US");
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("en", result.get(LANGUAGE));
        assertEquals("US", result.get(COUNTRY));
        assertFalse(result.containsKey(SCRIPT));
        assertFalse(result.containsKey(VARIANT));
    }

    @Test
    void toMap_WithLanguageCountryAndVariant_ShouldContainAllThree() {
        // language, country, variant
        Locale locale = new Locale("en", "US", "POSIX");
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("en", result.get(LANGUAGE));
        assertEquals("US", result.get(COUNTRY));
        assertEquals("POSIX", result.get(VARIANT));
        assertFalse(result.containsKey(SCRIPT));
    }

    @Test
    void toMap_WithScript_ShouldContainScriptKey() {
        Locale locale = new Locale.Builder().setLanguage("zh").setScript(// Simplified Chinese script
        "Hans").setRegion("CN").build();
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertNotNull(result);
        // language + country + script (region is mapped to country)
        assertEquals(3, result.size());
        assertEquals("zh", result.get(LANGUAGE));
        assertEquals("CN", result.get(COUNTRY));
        assertEquals("Hans", result.get(SCRIPT));
        assertFalse(result.containsKey(VARIANT));
    }

    @Test
    void toMap_WithAllFieldsPopulated_ShouldContainAllKeys() {
        Locale locale = new Locale.Builder().setLanguage("sr").setScript("Latn").setRegion("RS").setVariant("EURO").build();
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("sr", result.get(LANGUAGE));
        assertEquals("RS", result.get(COUNTRY));
        assertEquals("Latn", result.get(SCRIPT));
        assertEquals("EURO", result.get(VARIANT));
    }

    @Test
    void toMap_NullConverter_ShouldStillWork() {
        Locale locale = new Locale("fr");
        // Passing null for the converter – the method never uses it
        Map<?, ?> result = LocaleConversions.toMap(locale, null);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("fr", result.get(LANGUAGE));
    }

    /* -------------------------------------------------------------
     *  Group 1 – Standard Locale conversion
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("standardLocaleProvider")
    void toMap_StandardLocaleConversion(Locale locale, Map<String, Object> expected, String description) {
        // Act
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        // Assert – map must contain exactly the expected entries
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(expected.size(), result.size(), () -> "Map size differs. Expected: " + expected + ", actual: " + result);
        expected.forEach((k, v) -> assertEquals(v, result.get(k), () -> "Value for key '" + k + "' differs"));
        // ensure no unexpected keys are present
        assertTrue(result.keySet().containsAll(expected.keySet()), () -> "Result contains unexpected keys: " + result.keySet());
    }

    /* -------------------------------------------------------------
     *  Group 2 – Additional Locale combinations
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalLocaleProvider")
    void toMap_AdditionalLocaleCombinations(Locale locale, Map<String, Object> expected, String description) {
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertNotNull(result);
        assertEquals(expected.size(), result.size(), () -> "Unexpected map size for case: " + description);
        expected.forEach((k, v) -> assertEquals(v, result.get(k), () -> "Incorrect value for key '" + k + "' in case: " + description));
        assertTrue(result.keySet().containsAll(expected.keySet()), () -> "Result contains unexpected keys for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Group 3 – Edge‑case script/variant whitespace handling
     * ------------------------------------------------------------- */
    @Test
    void toMap_EdgeCaseScriptVariantWhitespace() {
        Locale locale = new Locale.Builder().setLanguage("en").setScript(// whitespace only – should be omitted
        "   ").setVariant(// surrounding whitespace – should be trimmed by hasContent
        "\tPOSIX\n").build();
        Map<?, ?> result = LocaleConversions.toMap(locale, mockConverter);
        assertNotNull(result);
        // Expected map: language + trimmed variant
        assertEquals(2, result.size());
        assertEquals("en", result.get(LANGUAGE));
        assertEquals("POSIX", result.get(VARIANT));
        assertFalse(result.containsKey(SCRIPT));
    }

    /* -------------------------------------------------------------
     *  Group 4 – Error scenarios
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("errorScenarioProvider")
    void toMap_ErrorScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        // Act & Assert
        assertThrows(expectedException, () -> LocaleConversions.toMap(from, mockConverter), () -> "Expected exception not thrown for case: " + description);
    }
}
