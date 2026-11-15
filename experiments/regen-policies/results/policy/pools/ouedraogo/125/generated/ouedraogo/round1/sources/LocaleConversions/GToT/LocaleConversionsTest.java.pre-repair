package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;
import java.util.Iterator;

public class LocaleConversionsTest {

    private Converter converter;   // a real Converter instance (options can be default)

    @Before
    public void setUp() {
        // Converter requires a ConverterOptions object; using the no‑arg constructor of
        // ConverterOptions is not shown, but we can pass null because the methods under test
        // never use the converter. The public API permits a null options argument.
        converter = new Converter(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for toString                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_NormalLocale() {
        Locale locale = Locale.US; // language=en, country=US
        String result = LocaleConversions.toString(locale, converter);
        assertEquals("en-US", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        LocaleConversions.toString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        LocaleConversions.toString("not-a-locale", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for toMap                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMap_AllFieldsPresent() {
        Locale locale = new Locale.Builder()
                .setLanguage("en")
                .setRegion("GB")
                .setScript("Latn")
                .setVariant("POSIX")
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, converter);

        // Expected size = 4 (language, country, script, variant)
        assertEquals(4, map.size());

        // Verify insertion order – the builder creates an insertion‑ordered CompactMap
        Iterator<String> keyIter = map.keySet().iterator();
        assertEquals(MapConversions.LANGUAGE, keyIter.next());
        assertEquals(MapConversions.COUNTRY, keyIter.next());
        assertEquals(MapConversions.SCRIPT, keyIter.next());
        assertEquals(MapConversions.VARIANT, keyIter.next());

        assertEquals("en", map.get(MapConversions.LANGUAGE));
        assertEquals("GB", map.get(MapConversions.COUNTRY));
        assertEquals("Latn", map.get(MapConversions.SCRIPT));
        assertEquals("POSIX", map.get(MapConversions.VARIANT));
    }

    @Test
    public void testToMap_OnlyLanguage() {
        Locale locale = new Locale("fr"); // language only
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, converter);

        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.LANGUAGE));
        assertEquals("fr", map.get(MapConversions.LANGUAGE));

        // No optional entries
        assertFalse(map.containsKey(MapConversions.COUNTRY));
        assertFalse(map.containsKey(MapConversions.SCRIPT));
        assertFalse(map.containsKey(MapConversions.VARIANT));
    }

    @Test
    public void testToMap_EmptyOptionalFields() {
        // Locale with language but empty country/script/variant strings
        Locale locale = new Locale.Builder()
                .setLanguage("de")
                .setRegion("")   // empty country
                .setScript("")   // empty script
                .setVariant("")  // empty variant
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, converter);

        // Only language should be present
        assertEquals(1, map.size());
        assertEquals("de", map.get(MapConversions.LANGUAGE));
        assertFalse(map.containsKey(MapConversions.COUNTRY));
        assertFalse(map.containsKey(MapConversions.SCRIPT));
        assertFalse(map.containsKey(MapConversions.VARIANT));
    }

    @Test
    public void testToMap_EmptyLanguage() {
        // Locale with empty language (legal, though unusual)
        Locale locale = new Locale("");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, converter);

        // Language key is still added (value is empty string)
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.LANGUAGE));
        assertEquals("", map.get(MapConversions.LANGUAGE));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        LocaleConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        LocaleConversions.toMap(Integer.valueOf(42), converter);
    }
}
