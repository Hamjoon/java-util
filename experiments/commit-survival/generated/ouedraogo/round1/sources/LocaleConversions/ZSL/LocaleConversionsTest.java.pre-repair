package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.COUNTRY;
import static com.cedarsoftware.util.convert.MapConversions.LANGUAGE;
import static com.cedarsoftware.util.convert.MapConversions.SCRIPT;
import static com.cedarsoftware.util.convert.MapConversions.VARIANT;

/**
 * Unit tests for {@link LocaleConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>{@link LocaleConversions#toString(Object, Converter)} – conversion of a {@link Locale}
 *       to its IETF BCP‑47 language tag.</li>
 *   <li>{@link LocaleConversions#toMap(Object, Converter)} – conversion of a {@link Locale}
 *       to a {@link Map} containing the language, country, script and variant entries
 *       when they are present.</li>
 * </ul>
 *
 * No external mocking frameworks are used; only real objects from the public API are
 * instantiated as required by the assignment constraints.
 */
public class LocaleConversionsTest {

    // The Converter argument is not used by the methods under test, so we can safely pass null.
    private static final Converter DUMMY_CONVERTER = null;

    @Test
    public void testToString_SimpleLocale() {
        Locale locale = Locale.US; // language= "en", country= "US"
        String result = LocaleConversions.toString(locale, DUMMY_CONVERTER);
        // Locale.toLanguageTag() for en-US should be exactly "en-US"
        assertEquals("en-US", result);
    }

    @Test
    public void testToString_WithScriptAndVariant() {
        // Build a locale with language, script, region and variant
        Locale locale = new Locale.Builder()
                .setLanguage("sr")
                .setScript("Latn")
                .setRegion("RS")
                .setVariant("rozaj")
                .build(); // Expected tag: "sr-Latn-RS-rozaj"
        String tag = LocaleConversions.toString(locale, DUMMY_CONVERTER);
        assertEquals("sr-Latn-RS-rozaj", tag);
    }

    @Test
    public void testToMap_MinimalLocale() {
        Locale locale = new Locale("fr"); // only language, no country/script/variant
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        // Must contain language entry
        assertTrue(map.containsKey(LANGUAGE));
        assertEquals("fr", map.get(LANGUAGE));

        // No other entries should be present
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
        assertEquals(1, map.size());
    }

    @Test
    public void testToMap_FullLocale() {
        // Locale with language, country, script and variant
        Locale locale = new Locale.Builder()
                .setLanguage("zh")
                .setScript("Hans")
                .setRegion("CN")
                .setVariant("POSIX")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        // Verify each key/value pair
        assertEquals("zh", map.get(LANGUAGE));
        assertEquals("CN", map.get(COUNTRY));
        assertEquals("Hans", map.get(SCRIPT));
        assertEquals("POSIX", map.get(VARIANT));

        // Ensure no extra keys
        assertEquals(4, map.size());
    }

    @Test
    public void testToMap_PartialLocale_CountryOnly() {
        Locale locale = new Locale("de", "DE"); // language + country
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        assertEquals("de", map.get(LANGUAGE));
        assertEquals("DE", map.get(COUNTRY));

        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
        assertEquals(2, map.size());
    }

    @Test
    public void testToMap_PartialLocale_ScriptOnly() {
        // Locale with language and script but no country/variant
        Locale locale = new Locale.Builder()
                .setLanguage("az")
                .setScript("Latn")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        assertEquals("az", map.get(LANGUAGE));
        assertEquals("Latn", map.get(SCRIPT));

        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(VARIANT));
        assertEquals(2, map.size());
    }

    @Test
    public void testToMap_EmptyStringsAreIgnored() {
        // Construct a Locale where country, script and variant are empty strings.
        Locale locale = new Locale("es", "", "");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, DUMMY_CONVERTER);

        // Only language should be present; empty strings must be ignored by StringUtilities.hasContent
        assertEquals("es", map.get(LANGUAGE));
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
        assertEquals(1, map.size());
    }
}
