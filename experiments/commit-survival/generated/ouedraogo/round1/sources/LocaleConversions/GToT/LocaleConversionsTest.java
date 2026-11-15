package com.cedarsoftware.util.convert;

import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link LocaleConversions}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>basic functionality of {@code toString}</li>
 *   <li>basic functionality of {@code toMap}</li>
 *   <li>edge‑case handling (null arguments, empty language, script/variant presence, etc.)</li>
 *   <li>verification that the {@code Converter} argument is ignored (as per current implementation)</li>
 * </ul>
 * </p>
 *
 * <p>All tests are written in the same package as {@code LocaleConversions}
 * so that the package‑private static methods are directly accessible.</p>
 */
public class LocaleConversionsTest {

    /** Helper that supplies a dummy {@link Converter}. The implementation of LocaleConversions
     * does not use the converter, so a simple instance constructed with default options is enough. */
    private static Converter dummyConverter() {
        return new Converter(new ConverterOptions());
    }

    /* ----------------------------------------------------------------------
     *  Tests for toString(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToStringSimpleLocale() {
        Locale locale = Locale.US; // en_US
        String tag = LocaleConversions.toString(locale, dummyConverter());
        assertEquals("en-US", tag);
    }

    @Test
    public void testToStringLanguageOnly() {
        Locale locale = new Locale("fr"); // French, no country
        String tag = LocaleConversions.toString(locale, dummyConverter());
        assertEquals("fr", tag);
    }

    @Test
    public void testToStringFullLocaleWithScriptAndVariant() {
        Locale locale = new Locale.Builder()
                .setLanguage("en")
                .setScript("Latn")
                .setRegion("GB")
                .setVariant("POSIX")
                .build(); // en-Latn-GB-POSIX
        String tag = LocaleConversions.toString(locale, dummyConverter());
        assertEquals("en-Latn-GB-POSIX", tag);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullLocale() {
        LocaleConversions.toString(null, dummyConverter());
    }

    @Test
    public void testToStringIgnoresConverter() {
        Locale locale = Locale.CANADA_FRENCH; // fr_CA
        // Pass a converter that would throw if used (we never call it)
        Converter badConverter = new Converter(new ConverterOptions()) {
            @Override public <T> T convert(Object from, Class<T> toType) {
                throw new AssertionError("Converter should not be used");
            }
        };
        String tag = LocaleConversions.toString(locale, badConverter);
        assertEquals("fr-CA", tag);
    }

    /* ----------------------------------------------------------------------
     *  Tests for toMap(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMapAllFieldsPresent() {
        Locale locale = new Locale.Builder()
                .setLanguage("en")
                .setScript("Latn")
                .setRegion("US")
                .setVariant("POSIX")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter());

        assertEquals("en", map.get(LANGUAGE));
        assertEquals("US", map.get(COUNTRY));
        assertEquals("Latn", map.get(SCRIPT));
        assertEquals("POSIX", map.get(VARIANT));

        // No extra keys
        assertEquals(4, map.size());
    }

    @Test
    public void testToMapOnlyLanguageAndCountry() {
        Locale locale = new Locale("de", "DE"); // German (Germany)

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter());

        assertEquals("de", map.get(LANGUAGE));
        assertEquals("DE", map.get(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));

        assertEquals(2, map.size());
    }

    @Test
    public void testToMapLanguageOnly() {
        Locale locale = new Locale("ja"); // Japanese, no country, script or variant

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter());

        assertEquals("ja", map.get(LANGUAGE));
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));

        assertEquals(1, map.size());
    }

    @Test
    public void testToMapEmptyStrings() {
        // Locale with empty language, country, script, variant
        Locale locale = new Locale.Builder()
                .setLanguage("")
                .setRegion("")
                .setScript("")
                .setVariant("")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, dummyConverter());

        // Language is always put (even if empty)
        assertTrue(map.containsKey(LANGUAGE));
        assertEquals("", map.get(LANGUAGE));

        // The other fields are added only if StringUtilities.hasContent() returns true.
        // Assuming hasContent treats empty strings as false, they must be absent.
        assertFalse(map.containsKey(COUNTRY));
        assertFalse(map.containsKey(SCRIPT));
        assertFalse(map.containsKey(VARIANT));

        assertEquals(1, map.size());
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullLocale() {
        LocaleConversions.toMap(null, dummyConverter());
    }

    @Test
    public void testToMapIgnoresConverter() {
        Locale locale = Locale.ROOT; // language "", country "", etc.
        Converter badConverter = new Converter(new ConverterOptions()) {
            @Override public <T> T convert(Object from, Class<T> toType) {
                throw new AssertionError("Converter should not be used");
            }
        };
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, badConverter);

        // Only language key should be present (empty string)
        assertEquals("", map.get(LANGUAGE));
        assertEquals(1, map.size());
    }
}
