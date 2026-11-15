package com.cedarsoftware.util.convert;

import org.junit.Test;

import java.util.Locale;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.LOCALE;
import static org.junit.Assert.*;

public class LocaleConversionsTest {

    /** Helper that creates a dummy Converter instance. The Converter is not used by the
     *  methods under test, but the signature requires it. Passing {@code null} would also
     *  work, however constructing a real instance demonstrates that the API can be used
     *  without side‑effects. */
    private static Converter dummyConverter() {
        // The Converter constructor expects a ConverterOptions instance; the library permits
        // a null argument (options are optional). If the implementation changes, the test will
        // still compile because the argument type matches.
        return new Converter(null);
    }

    /* ----------------------------------------------------------------------
     * Tests for toString(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void toString_simpleLanguage() {
        Locale locale = Locale.ENGLISH; // language = "en"
        String result = LocaleConversions.toString(locale, dummyConverter());
        assertEquals("en", result);
    }

    @Test
    public void toString_languageAndCountry() {
        Locale locale = new Locale("en", "US"); // language = "en", country = "US"
        String result = LocaleConversions.toString(locale, dummyConverter());
        assertEquals("en-US", result);
    }

    @Test
    public void toString_languageCountryAndVariant() {
        // Variant is included after a third hyphen
        Locale locale = new Locale("en", "GB", "POSIX");
        String result = LocaleConversions.toString(locale, dummyConverter());
        assertEquals("en-GB-posix", result); // toLanguageTag lower‑cases variant
    }

    @Test
    public void toString_languageScriptCountry() {
        // Using the Builder to create a locale with a script subtag (e.g., "Hans")
        Locale locale = new Locale.Builder()
                .setLanguage("zh")
                .setScript("Hans")
                .setRegion("CN")
                .build();
        String result = LocaleConversions.toString(locale, dummyConverter());
        assertEquals("zh-Hans-CN", result);
    }

    @Test(expected = NullPointerException.class)
    public void toString_nullLocaleThrowsNPE() {
        LocaleConversions.toString(null, dummyConverter());
    }

    /* ----------------------------------------------------------------------
     * Tests for toMap(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void toMap_containsLocaleKeyAndStringValue() {
        Locale locale = new Locale("fr", "CA"); // French (Canada)
        Map<?, ?> map = LocaleConversions.toMap(locale, dummyConverter());

        // The map must contain exactly one entry
        assertNotNull(map);
        assertEquals(1, map.size());

        // Verify the key is the constant from MapConversions
        assertTrue(map.containsKey(LOCALE));

        // Verify the value matches LocaleConversions.toString(...)
        String expectedValue = LocaleConversions.toString(locale, dummyConverter());
        assertEquals(expectedValue, map.get(LOCALE));
    }

    @Test
    public void toMap_isLinkedHashMapPreservesInsertionOrder() {
        Locale locale = Locale.JAPAN; // language = "ja", country = "JP"
        Map<?, ?> map = LocaleConversions.toMap(locale, dummyConverter());

        // The concrete class should be LinkedHashMap (order‑preserving)
        assertTrue("Expected a LinkedHashMap implementation", map instanceof java.util.LinkedHashMap);
    }

    @Test(expected = NullPointerException.class)
    public void toMap_nullLocaleThrowsNPE() {
        LocaleConversions.toMap(null, dummyConverter());
    }

    /* ----------------------------------------------------------------------
     * Additional sanity checks
     * ---------------------------------------------------------------------- */

    @Test
    public void toMap_andToString_consistency() {
        Locale locale = new Locale("es", "MX"); // Spanish (Mexico)

        String asString = LocaleConversions.toString(locale, dummyConverter());
        Map<?, ?> asMap = LocaleConversions.toMap(locale, dummyConverter());

        assertEquals(asString, asMap.get(LOCALE));
    }
}
