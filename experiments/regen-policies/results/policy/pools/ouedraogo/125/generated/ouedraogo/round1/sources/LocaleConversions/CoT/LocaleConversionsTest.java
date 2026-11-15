package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@link LocaleConversions}.
 *
 * The test class is placed in the same package as {@code LocaleConversions}
 * so it can access the package‑private static methods {@code toString} and
 * {@code toMap}.
 *
 * The tests are divided into three sections:
 *   1. Basic functionality – verifies that the conversion produces the
 *      expected language tag or map for normal {@link Locale} objects.
 *   2. Edge‑case handling – checks behaviour when the source object is
 *      {@code null} or not a {@code Locale}, and when optional Locale
 *      components (country, script, variant) are empty.
 *   3. Comprehensive field coverage – builds locales that contain each
 *      possible attribute and asserts that the resulting map contains the
 *      correct keys and values.
 *
 * The {@code Converter} argument is not used by the current implementation,
 * therefore a {@code null} reference is sufficient for all tests.
 */
public class LocaleConversionsTest {

    /** A dummy Converter instance – not required for current logic but kept
     *  for signature compatibility. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // Since the implementation of LocaleConversions never touches the
        // Converter, we can safely pass null for the options as well.
        dummyConverter = new Converter(null);
    }

    /* --------------------------------------------------------------
     *  1.  Tests for {@code toString(Object, Converter)}
     * -------------------------------------------------------------- */

    @Test
    public void testToStringSimpleLocale() {
        Locale locale = Locale.US;                     // en-US
        String tag = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("en-US", tag);
    }

    @Test
    public void testToStringWithScriptAndRegion() {
        // Locale for Traditional Chinese in Taiwan (language + script + region)
        Locale locale = Locale.forLanguageTag("zh-Hant-TW");
        String tag = LocaleConversions.toString(locale, dummyConverter);
        assertEquals("zh-Hant-TW", tag);
    }

    @Test
    public void testToStringWithVariantOnly() {
        // Locale with language and variant (no country)
        Locale locale = new Locale("de", "", "POSIX");
        String tag = LocaleConversions.toString(locale, dummyConverter);
        // The variant appears after the language tag, separated by a dash
        assertEquals("de-POSIX", tag);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullLocale() {
        LocaleConversions.toString(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringInvalidObject() {
        // Pass an object that is not a Locale – the method casts without checks.
        LocaleConversions.toString("not a locale", dummyConverter);
    }

    /* --------------------------------------------------------------
     *  2.  Tests for {@code toMap(Object, Converter)}
     * -------------------------------------------------------------- */

    @Test
    public void testToMapLanguageOnly() {
        Locale locale = new Locale("fr"); // French, no country/script/variant
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(1, map.size());
        assertEquals("fr", map.get(LANGUAGE));
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
    }

    @Test
    public void testToMapLanguageAndCountry() {
        Locale locale = new Locale("en", "GB"); // English, United Kingdom
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(2, map.size());
        assertEquals("en", map.get(LANGUAGE));
        assertEquals("GB", map.get(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
    }

    @Test
    public void testToMapLanguageScript() {
        // Locale with language and script (no country)
        Locale locale = new Locale.Builder()
                .setLanguage("sr")
                .setScript("Latn")
                .build(); // Serbian written in Latin script
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(2, map.size());
        assertEquals("sr", map.get(LANGUAGE));
        assertEquals("Latn", map.get(SCRIPT));
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(VARIANT));
    }

    @Test
    public void testToMapLanguageVariant() {
        Locale locale = new Locale("nl", "", "WIN"); // Dutch with variant WIN
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(2, map.size());
        assertEquals("nl", map.get(LANGUAGE));
        assertEquals("WIN", map.get(VARIANT));
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
    }

    @Test
    public void testToMapAllFieldsPresent() {
        // Language + Country + Script + Variant
        Locale locale = new Locale.Builder()
                .setLanguage("en")
                .setRegion("US")
                .setScript("Latn")
                .setVariant("POSIX")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        assertEquals(4, map.size());
        assertEquals("en", map.get(LANGUAGE));
        assertEquals("US", map.get(COUNTRY));
        assertEquals("Latn", map.get(SCRIPT));
        assertEquals("POSIX", map.get(VARIANT));
    }

    @Test
    public void testToMapEmptyOptionalFieldsAreOmitted() {
        // Locale with empty country, script and variant strings.
        Locale locale = new Locale.Builder()
                .setLanguage("ja")
                .setRegion("")   // empty country
                .setScript("")   // empty script
                .setVariant("")  // empty variant
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter);

        // Only the language key should survive
        assertEquals(1, map.size());
        assertEquals("ja", map.get(LANGUAGE));
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullLocale() {
        LocaleConversions.toMap(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapInvalidObject() {
        LocaleConversions.toMap(Integer.valueOf(42), dummyConverter);
    }
}
