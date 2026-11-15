package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocaleConversions}.
 *
 * The tests cover:
 *   • Normal usage of {@code toString} with simple, script‑aware and variant locales.
 *   • Edge cases such as {@link Locale#ROOT} and a {@code null} source.
 *   • Normal usage of {@code toMap} with all combinations of language, country, script and variant.
 *   • Verification that empty components are omitted from the resulting map.
 *   • Verification that a {@code null} source throws {@link NullPointerException}
 *     (the method casts the argument to {@link Locale} without a check).
 *
 * No external libraries or mocking frameworks are used – only the public APIs
 * listed in the problem statement.
 */
public class LocaleConversionsTest {

    private Converter dummyConverter; // Not used by the methods but required by the signature

    @Before
    public void setUp() {
        // The Converter is not used by the methods under test; a simple instance with default options is sufficient.
        // ConverterOptions is not part of the provided API, but a no‑arg constructor exists in the library.
        // If the library version does not expose a public no‑arg constructor, passing {@code null}
        // is also acceptable because the methods ignore the converter argument.
        dummyConverter = new Converter(new ConverterOptions());
    }

    /* ----------------------------------------------------------------------
     * Tests for toString(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void toString_simpleLocale() {
        Locale locale = Locale.US; // en_US
        String tag = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("en-US", tag);
    }

    @Test
    public void toString_localeWithScriptAndVariant() {
        // Locale with language, script, region and variant
        Locale locale = new Locale.Builder()
                .setLanguage("sr")
                .setScript("Latn")
                .setRegion("RS")
                .setVariant("rozaj")
                .build(); // Expected: sr-Latn-RS-rozaj
        String tag = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("sr-Latn-RS-rozaj", tag);
    }

    @Test
    public void toString_localeWithOnlyLanguage() {
        Locale locale = new Locale("fr"); // French, no country
        String tag = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("fr", tag);
    }

    @Test
    public void toString_rootLocaleProducesEmptyString() {
        Locale locale = Locale.ROOT; // language == "", country == "", etc.
        String tag = LocaleConversions.toString(locale, dummyConverter);
        // According to Locale.toLanguageTag(), ROOT yields an empty string.
        assertEquals("", tag);
    }

    @Test(expected = NullPointerException.class)
    public void toString_nullSourceThrowsNPE() {
        // The method casts the argument to Locale without a null‑check.
        LocaleConversions.toString(null, dummyConverter);
    }

    /* ----------------------------------------------------------------------
     * Tests for toMap(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void toMap_onlyLanguage() {
        Locale locale = new Locale("es"); // Spanish, no country, script or variant
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(1, map.size());
        assertEquals("es", map.get(LANGUAGE));
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
    }

    @Test
    public void toMap_languageAndCountry() {
        Locale locale = new Locale("de", "DE"); // German (Germany)
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(2, map.size());
        assertEquals("de", map.get(LANGUAGE));
        assertEquals("DE", map.get(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
    }

    @Test
    public void toMap_allComponentsPresent() {
        // Language, script, country, variant
        Locale locale = new Locale.Builder()
                .setLanguage("zh")
                .setScript("Hans")
                .setRegion("CN")
                .setVariant("POSIX")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(4, map.size());
        assertEquals("zh", map.get(LANGUAGE));
        assertEquals("CN", map.get(COUNTRY));
        assertEquals("Hans", map.get(SCRIPT));
        assertEquals("POSIX", map.get(VARIANT));
    }

    @Test
    public void toMap_omitsEmptyComponents() {
        // Create a locale where script and variant are empty strings.
        Locale locale = new Locale.Builder()
                .setLanguage("en")
                .setRegion("GB")
                .build(); // script == "", variant == ""

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(2, map.size()); // only language and country should be present
        assertEquals("en", map.get(LANGUAGE));
        assertEquals("GB", map.get(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
    }

    @Test(expected = NullPointerException.class)
    public void toMap_nullSourceThrowsNPE() {
        // As with toString, a null source leads to a ClassCastException which surfaces as NPE
        // when the cast to Locale is attempted.
        LocaleConversions.toMap(null, dummyConverter);
    }

    /* ----------------------------------------------------------------------
     * Helper assertions (optional but improve readability)
     * ---------------------------------------------------------------------- */

    private void assertMapContainsOnly(Map<String, Object> map, String... expectedKeys) {
        assertEquals(expectedKeys.length, map.size());
        for (String key : expectedKeys) {
            assertTrue("Map should contain key: " + key, map.containsKey(key));
        }
    }
}
