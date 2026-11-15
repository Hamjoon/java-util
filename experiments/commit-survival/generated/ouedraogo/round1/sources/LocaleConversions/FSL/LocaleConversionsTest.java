package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import static com.cedarsoftware.util.convert.MapConversions.COUNTRY;
import static com.cedarsoftware.util.convert.MapConversions.LANGUAGE;
import static com.cedarsoftware.util.convert.MapConversions.SCRIPT;
import static com.cedarsoftware.util.convert.MapConversions.VARIANT;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link LocaleConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of {@link Locale} objects to their BCP‑47 language tag representation
 *       via {@link LocaleConversions#toString(Object, Converter)}.</li>
 *   <li>Conversion of {@link Locale} objects to a {@link Map} representation via
 *       {@link LocaleConversions#toMap(Object, Converter)}. The map must contain the
 *       mandatory {@code LANGUAGE} entry and optional {@code COUNTRY}, {@code SCRIPT},
 *       and {@code VARIANT} entries only when the corresponding {@link Locale} field
 *       has content (i.e. is not {@code null} nor empty).</li>
 *   <li>Correct handling of locales that contain a script, a variant, both, or none.</li>
 * </ul>
 *
 * The {@link Converter} instance required by the static methods is created with a
 * {@code null} {@link ConverterOptions} because the conversion logic does not
 * depend on any configuration.
 */
public class LocaleConversionsTest {

    /** Helper that creates a {@link Converter} with default (null) options. */
    private static Converter defaultConverter() {
        return new Converter(null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     Tests for LocaleConversions.toString                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_SimpleLanguage() {
        Locale locale = Locale.ENGLISH; // language = "en"
        String expected = locale.toLanguageTag(); // "en"
        assertEquals(expected, LocaleConversions.toString(locale, defaultConverter()));
    }

    @Test
    public void testToString_LanguageAndCountry() {
        Locale locale = Locale.US; // language = "en", country = "US"
        String expected = locale.toLanguageTag(); // "en-US"
        assertEquals(expected, LocaleConversions.toString(locale, defaultConverter()));
    }

    @Test
    public void testToString_LanguageScriptCountry() {
        // Serbian written in Latin script for Serbia
        Locale locale = new Locale.Builder()
                .setLanguage("sr")
                .setScript("Latn")
                .setRegion("RS")
                .build();
        String expected = locale.toLanguageTag(); // "sr-Latn-RS"
        assertEquals(expected, LocaleConversions.toString(locale, defaultConverter()));
    }

    @Test
    public void testToString_WithVariant() {
        // English (United States) with POSIX variant
        Locale locale = new Locale("en", "US", "POSIX");
        String expected = locale.toLanguageTag(); // "en-US-u-va-posix"
        assertEquals(expected, LocaleConversions.toString(locale, defaultConverter()));
    }

    @Test
    public void testToString_RootLocale() {
        Locale locale = Locale.ROOT; // language = "", country = "", variant = ""
        String expected = locale.toLanguageTag(); // "" (empty string)
        assertEquals(expected, LocaleConversions.toString(locale, defaultConverter()));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Tests for LocaleConversions.toMap                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMap_OnlyLanguage() {
        Locale locale = Locale.FRENCH; // language = "fr"
        Map<?, ?> map = LocaleConversions.toMap(locale, defaultConverter());

        assertTrue("Result should be a CompactLinkedMap", map instanceof CompactLinkedMap);
        assertEquals("Map size should be 1 (only language)", 1, map.size());
        assertEquals("Language entry must match", "fr", map.get(LANGUAGE));
        assertNull("Country entry must be absent", map.get(COUNTRY));
        assertNull("Script entry must be absent", map.get(SCRIPT));
        assertNull("Variant entry must be absent", map.get(VARIANT));
    }

    @Test
    public void testToMap_LanguageAndCountry() {
        Locale locale = Locale.CANADA_FRENCH; // language = "fr", country = "CA"
        Map<?, ?> map = LocaleConversions.toMap(locale, defaultConverter());

        assertTrue(map instanceof CompactLinkedMap);
        assertEquals(2, map.size());
        assertEquals("fr", map.get(LANGUAGE));
        assertEquals("CA", map.get(COUNTRY));
        assertNull(map.get(SCRIPT));
        assertNull(map.get(VARIANT));
    }

    @Test
    public void testToMap_AllFieldsPresent() {
        // Locale with language, script, country, and variant
        Locale locale = new Locale.Builder()
                .setLanguage("zh")
                .setScript("Hans")
                .setRegion("CN")
                .setVariant("POSIX")
                .build();

        Map<?, ?> map = LocaleConversions.toMap(locale, defaultConverter());

        assertTrue(map instanceof CompactLinkedMap);
        assertEquals(4, map.size());
        assertEquals("zh", map.get(LANGUAGE));
        assertEquals("CN", map.get(COUNTRY));
        assertEquals("Hans", map.get(SCRIPT));
        assertEquals("POSIX", map.get(VARIANT));
    }

    @Test
    public void testToMap_EmptyOptionalFieldsAreOmitted() {
        // Locale with language only; other fields are empty strings
        Locale locale = new Locale.Builder()
                .setLanguage("de")
                .setRegion("")   // explicitly empty
                .setScript("")   // explicitly empty
                .setVariant("")  // explicitly empty
                .build();

        Map<?, ?> map = LocaleConversions.toMap(locale, defaultConverter());

        assertTrue(map instanceof CompactLinkedMap);
        assertEquals(1, map.size());
        assertEquals("de", map.get(LANGUAGE));
        assertNull(map.get(COUNTRY));
        assertNull(map.get(SCRIPT));
        assertNull(map.get(VARIANT));
    }

    @Test
    public void testToMap_NullCountryButNonEmptyScriptAndVariant() {
        // Locale with language, script, variant but no country
        Locale locale = new Locale.Builder()
                .setLanguage("sl")
                .setScript("Latn")
                .setVariant("1994")
                .build();

        Map<?, ?> map = LocaleConversions.toMap(locale, defaultConverter());

        assertTrue(map instanceof CompactLinkedMap);
        // Expected entries: LANGUAGE, SCRIPT, VARIANT (no COUNTRY)
        assertEquals(3, map.size());
        assertEquals("sl", map.get(LANGUAGE));
        assertEquals("Latn", map.get(SCRIPT));
        assertEquals("1994", map.get(VARIANT));
        assertNull(map.get(COUNTRY));
    }

    @Test
    public void testToMap_ConsistencyBetweenToStringAndToMap() {
        // Verify that the language tag produced by toString matches the components
        // stored in the map for a complex locale.
        Locale locale = new Locale.Builder()
                .setLanguage("es")
                .setScript("Latn")
                .setRegion("MX")
                .setVariant("POSIX")
                .build();

        String tag = LocaleConversions.toString(locale, defaultConverter());
        Map<?, ?> map = LocaleConversions.toMap(locale, defaultConverter());

        // The tag must contain language, script, region and variant information.
        assertTrue("Tag should start with language", tag.startsWith("es"));
        assertTrue("Tag should contain script", tag.contains("Latn"));
        assertTrue("Tag should contain country", tag.contains("MX"));
        assertTrue("Tag should contain variant", tag.contains("POSIX"));

        // Map entries must correspond to the locale fields.
        assertEquals("es", map.get(LANGUAGE));
        assertEquals("MX", map.get(COUNTRY));
        assertEquals("Latn", map.get(SCRIPT));
        assertEquals("POSIX", map.get(VARIANT));
    }
}
