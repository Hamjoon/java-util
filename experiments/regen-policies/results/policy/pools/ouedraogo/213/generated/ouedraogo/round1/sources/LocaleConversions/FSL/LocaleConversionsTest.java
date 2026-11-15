package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.convert.MapConversions.LOCALE;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link LocaleConversions}.
 *
 * These tests cover the conversion of {@link Locale} objects to their
 * BCP‑47 language‑tag string representation as well as the creation of a
 * {@link java.util.Map} that contains that string under the {@code LOCALE}
 * key.
 *
 * No external dependencies are required – the {@link Converter} argument is
 * deliberately passed as {@code null} because the implementation does not
 * make use of it.
 */
public class LocaleConversionsTest {

    // ----------------------------------------------------------------------
    // toString(Object, Converter) tests
    // ----------------------------------------------------------------------

    @Test
    public void toString_languageOnly() {
        Locale locale = Locale.ENGLISH;                 // "en"
        String tag = LocaleConversions.toString(locale, null);
        assertEquals("en", tag);
    }

    @Test
    public void toString_languageAndCountry() {
        Locale locale = Locale.US;                     // "en_US"
        String tag = LocaleConversions.toString(locale, null);
        assertEquals("en-US", tag);
    }

    @Test
    public void toString_languageCountryAndVariant() {
        Locale locale = new Locale("fr", "CA", "POSIX"); // "fr_CA_POSIX"
        String tag = LocaleConversions.toString(locale, null);
        assertEquals("fr-CA-posix", tag);
    }

    @Test
    public void toString_usingPredefinedLocale() {
        Locale locale = Locale.CANADA_FRENCH;           // "fr_CA"
        String tag = LocaleConversions.toString(locale, null);
        assertEquals("fr-CA", tag);
    }

    @Test
    public void toString_emptyLanguage() {
        Locale locale = new Locale("", "", "");
        String tag = LocaleConversions.toString(locale, null);
        assertEquals("", tag);
    }

    @Test(expected = NullPointerException.class)
    public void toString_nullLocaleThrowsNPE() {
        LocaleConversions.toString(null, null);
    }

    // ----------------------------------------------------------------------
    // toMap(Object, Converter) tests
    // ----------------------------------------------------------------------

    @Test
    public void toMap_containsLocaleKeyWithCorrectValue() {
        Locale locale = Locale.GERMANY;                // "de_DE"
        Map<?, ?> result = LocaleConversions.toMap(locale, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(LOCALE));

        String expectedTag = LocaleConversions.toString(locale, null);
        assertEquals(expectedTag, result.get(LOCALE));
    }

    @Test
    public void toMap_withVariantLocale() {
        Locale locale = new Locale("es", "MX", "traditional");
        Map<?, ?> result = LocaleConversions.toMap(locale, null);

        assertEquals(1, result.size());
        assertEquals(LocaleConversions.toString(locale, null), result.get(LOCALE));
    }

    @Test
    public void toMap_nullLocaleThrowsNPE() {
        try {
            LocaleConversions.toMap(null, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    // ----------------------------------------------------------------------
    // Miscellaneous sanity checks
    // ----------------------------------------------------------------------

    @Test
    public void toMap_isLinkedHashMapPreservingInsertionOrder() {
        // The implementation uses LinkedHashMap – we verify that the entry
        // order is the same as insertion order (only one entry, but the type
        // is still LinkedHashMap).
        Locale locale = Locale.ITALY;
        Map<?, ?> map = LocaleConversions.toMap(locale, null);
        assertTrue("Result should be a LinkedHashMap", map instanceof java.util.LinkedHashMap);
    }

    @Test
    public void toString_and_toMap_consistency() {
        Locale locale = new Locale("ja", "JP");
        String tag = LocaleConversions.toString(locale, null);
        Map<?, ?> map = LocaleConversions.toMap(locale, null);
        assertEquals(tag, map.get(LOCALE));
    }
}
