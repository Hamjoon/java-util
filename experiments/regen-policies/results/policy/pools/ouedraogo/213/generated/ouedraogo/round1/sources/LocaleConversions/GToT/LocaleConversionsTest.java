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
 *   • Normal (happy‑path) usage of {@code toString} and {@code toMap}
 *   • Edge‑cases such as null arguments, locales with missing components,
 *     and locales that contain a variant
 *   • Behaviour of the returned Map (correct key/value and mutability)
 *
 * All tests are placed in the same package as {@code LocaleConversions}
 * so that the package‑private static methods are directly accessible.
 */
public class LocaleConversionsTest {

    // ----------------------------------------------------------------------
    //  Happy‑path tests
    // ----------------------------------------------------------------------
    @Test
    public void testToStringBasic() {
        Locale locale = new Locale("en", "GB");
        String tag = LocaleConversions.toString(locale, null);
        assertEquals("en-GB", tag);
    }

    @Test
    public void testToMapBasic() {
        Locale locale = Locale.US; // language=en, country=US
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, null);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("en-US", map.get(LOCALE));
    }

    // ----------------------------------------------------------------------
    //  Edge‑case / exception tests for toString
    // ----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        LocaleConversions.toString(null, null);
    }

    @Test
    public void testToString_LanguageOnly() {
        Locale locale = new Locale("fr");
        assertEquals("fr", LocaleConversions.toString(locale, null));
    }

    @Test
    public void testToString_WithVariant() {
        Locale locale = new Locale("ja", "JP", "JP");
        // According to Locale.toLanguageTag() the expected tag is:
        // language-country-u-va-variant
        assertEquals("ja-JP-u-va-jp", LocaleConversions.toString(locale, null));
    }

    // ----------------------------------------------------------------------
    //  Edge‑case / exception tests for toMap
    // ----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        LocaleConversions.toMap(null, null);
    }

    @Test
    public void testToMap_EmptyLanguage() {
        Locale locale = new Locale("", "US");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, null);
        // An empty language results in a leading hyphen
        assertEquals("-US", map.get(LOCALE));
    }

    @Test
    public void testToMap_IsMutable() {
        Locale locale = Locale.CANADA_FRENCH;
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) LocaleConversions.toMap(locale, null);
        // Verify the map is a mutable LinkedHashMap
        map.put("extraKey", "extraValue");
        assertEquals("extraValue", map.get("extraKey"));
    }
}
