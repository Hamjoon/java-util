/*****************************************************************************************
 * JUnit 4 test suite for {@code com.cedarsoftware.util.convert.LocaleConversions}.
 *
 * The tests exercise both conversion directions:
 *   1. {@code LocaleConversions.toString(Object, Converter)} – verifies that the
 *      resulting BCP‑47 language tag matches the {@link Locale}'s own {@code toLanguageTag()}
 *      output.
 *   2. {@code LocaleConversions.toMap(Object, Converter)} – verifies that a map is
 *      correctly populated with the expected keys (LANGUAGE, COUNTRY, SCRIPT, VARIANT)
 *      and that keys are omitted when the corresponding {@link Locale} component is
 *      empty.
 *
 * The test class resides in the same package as {@code LocaleConversions} so it can
 * access the package‑private static methods directly.
 *
 * No external mocking frameworks are used; real {@link Locale} instances are created
 * via the public constructors and {@link Locale.Builder}.  The {@code Converter}
 * argument is not required for the current implementation, therefore {@code null}
 * is passed safely.
 *
 * Author: ChatGPT (OpenAI)
 * Date:   2026‑11‑14
 *****************************************************************************************/

package com.cedarsoftware.util.convert;

import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import static com.cedarsoftware.util.convert.MapConversions.COUNTRY;
import static com.cedarsoftware.util.convert.MapConversions.LANGUAGE;
import static com.cedarsoftware.util.convert.MapConversions.SCRIPT;
import static com.cedarsoftware.util.convert.MapConversions.VARIANT;

/**
 * Tests for {@link LocaleConversions}.
 */
public class LocaleConversionsTest {

    /* -------------------------------------------------------------------------
     * Helper – a dummy Converter instance.
     *
     * The current implementation of LocaleConversions does not use the
     * Converter argument, but the method signatures require it.  Passing
     * {@code null} is safe because the code never dereferences the parameter.
     * ---------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = null;

    /* -------------------------------------------------------------------------
     * toString() tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToStringSimpleLocale() {
        Locale locale = new Locale("en");
        String expected = locale.toLanguageTag(); // "en"
        String actual = LocaleConversions.toString(locale, DUMMY_CONVERTER);
        assertEquals("toString should return Locale's language tag", expected, actual);
    }

    @Test
    public void testToStringLocaleWithCountry() {
        Locale locale = new Locale("fr", "CA"); // French (Canada)
        String expected = locale.toLanguageTag(); // "fr-CA"
        String actual = LocaleConversions.toString(locale, DUMMY_CONVERTER);
        assertEquals("toString should include country when present", expected, actual);
    }

    @Test
    public void testToStringLocaleWithScriptAndCountry() {
        // "en-Latn-US" – English, Latin script, United States
        Locale locale = Locale.forLanguageTag("en-Latn-US");
        String expected = locale.toLanguageTag(); // "en-Latn-US"
        String actual = LocaleConversions.toString(locale, DUMMY_CONVERTER);
        assertEquals("toString should include script and country", expected, actual);
    }

    @Test
    public void testToStringLocaleWithVariant() {
        Locale locale = new Locale("de", "DE", "POSIX"); // German (Germany, POSIX variant)
        String expected = locale.toLanguageTag(); // "de-DE-u-va-posix"
        String actual = LocaleConversions.toString(locale, DUMMY_CONVERTER);
        assertEquals("toString should include variant when present", expected, actual);
    }

    /* -------------------------------------------------------------------------
     * toMap() tests – verify that each component is correctly placed into the map
     * and that missing components are omitted.
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMapLanguageOnly() {
        Locale locale = new Locale("es"); // Spanish, no country/script/variant
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertEquals("LANGUAGE entry should match locale language", "es", map.get(LANGUAGE));
        assertFalse("COUNTRY key must be absent", map.containsKey(COUNTRY));
        assertFalse("SCRIPT key must be absent", map.containsKey(SCRIPT));
        assertFalse("VARIANT key must be absent", map.containsKey(VARIANT));
    }

    @Test
    public void testToMapLanguageAndCountry() {
        Locale locale = new Locale("it", "IT"); // Italian (Italy)
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        assertEquals("Map should contain two entries", 2, map.size());
        assertEquals("LANGUAGE entry", "it", map.get(LANGUAGE));
        assertEquals("COUNTRY entry", "IT", map.get(COUNTRY));
        assertFalse("SCRIPT key must be absent", map.containsKey(SCRIPT));
        assertFalse("VARIANT key must be absent", map.containsKey(VARIANT));
    }

    @Test
    public void testToMapLanguageAndScript() {
        // Locale with script but no country: "sr-Cyrl"
        Locale locale = Locale.forLanguageTag("sr-Cyrl");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        assertEquals("Map should contain two entries", 2, map.size());
        assertEquals("LANGUAGE entry", "sr", map.get(LANGUAGE));
        assertEquals("SCRIPT entry", "Cyrl", map.get(SCRIPT));
        assertFalse("COUNTRY key must be absent", map.containsKey(COUNTRY));
        assertFalse("VARIANT key must be absent", map.containsKey(VARIANT));
    }

    @Test
    public void testToMapLanguageAndVariant() {
        Locale locale = new Locale("ja", "", "JP"); // Japanese with variant "JP"
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        assertEquals("Map should contain two entries", 2, map.size());
        assertEquals("LANGUAGE entry", "ja", map.get(LANGUAGE));
        assertEquals("VARIANT entry", "JP", map.get(VARIANT));
        assertFalse("COUNTRY key must be absent", map.containsKey(COUNTRY));
        assertFalse("SCRIPT key must be absent", map.containsKey(SCRIPT));
    }

    @Test
    public void testToMapAllComponents() {
        // Build a Locale with language, script, country and variant
        Locale locale = new Locale.Builder()
                .setLanguage("en")
                .setScript("Latn")
                .setRegion("GB")
                .setVariant("POSIX")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        assertEquals("Map should contain four entries", 4, map.size());
        assertEquals("LANGUAGE entry", "en", map.get(LANGUAGE));
        assertEquals("SCRIPT entry", "Latn", map.get(SCRIPT));
        assertEquals("COUNTRY entry", "GB", map.get(COUNTRY));
        assertEquals("VARIANT entry", "POSIX", map.get(VARIANT));
    }

    @Test
    public void testToMapOmitsEmptyComponents() {
        // Locale with empty country and script, but with language and variant
        Locale locale = new Locale.Builder()
                .setLanguage("pt")
                .setVariant("POSIX")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        assertEquals("Map should contain two entries (language + variant)", 2, map.size());
        assertEquals("LANGUAGE entry", "pt", map.get(LANGUAGE));
        assertEquals("VARIANT entry", "POSIX", map.get(VARIANT));
        assertFalse("COUNTRY key must be absent", map.containsKey(COUNTRY));
        assertFalse("SCRIPT key must be absent", map.containsKey(SCRIPT));
    }
}
